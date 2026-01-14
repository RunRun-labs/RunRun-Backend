/**
 * 공통 알림 수신 스크립트 (SSE)
 * 싱글톤 패턴으로 전역에서 하나의 연결만 유지
 */

(function () {
  'use strict';

  // ============== 전역 변수 ==============
  let globalEventSource = null; // 싱글톤 EventSource
  let isConnected = false;
  let heartbeatSupported = false; // 서버가 heartbeat를 보내는지 여부
  let heartbeatTimeoutId = null;
  let connectionResolvers = []; // 연결 대기 중인 Promise들

  // ============== 유틸리티 함수 ==============
  function getToken() {
    return localStorage.getItem("accessToken") || getCookie("accessToken");
  }

  function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
      return parts.pop().split(";").shift();
    }
    return null;
  }

  function escapeHtml(text) {
    if (!text) {
      return '';
    }
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  // ============== EventSource Polyfill (AbortController 지원) ==============
  class EventSourcePolyfill {
    constructor(url, options = {}) {
      this.url = url;
      this.headers = options.headers || {};
      this.listeners = {};
      this.readyState = 0; // CONNECTING
      this.abortController = new AbortController();
      this.connect();
    }

    connect() {
      if (this.readyState === 2) {
        return;
      } // CLOSED

      this.readyState = 0; // CONNECTING

      const token = this.headers['Authorization']?.replace('Bearer ', '')
          || getToken();
      if (!token) {
        console.error('[SSE] No token available for SSE connection');
        this.readyState = 2;
        return;
      }

      fetch(this.url, {
        method: 'GET',
        headers: {
          ...this.headers,
          'Accept': 'text/event-stream',
          'Cache-Control': 'no-cache'
        },
        credentials: 'include',
        signal: this.abortController.signal // AbortController 추가
      }).then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        this.readyState = 1; // OPEN
        this.dispatchEvent({type: 'open'});

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        const readChunk = () => {
          reader.read().then(({done, value}) => {
            if (done) {
              this.readyState = 2; // CLOSED
              this.dispatchEvent({type: 'error'});
              return;
            }

            buffer += decoder.decode(value, {stream: true});
            const lines = buffer.split('\n');
            buffer = lines.pop() || '';

            let eventType = 'message';
            let data = '';

            for (let line of lines) {
              line = line.replace(/\r$/, '');

              if (line.startsWith('event:')) {
                eventType = line.substring(6).trim();
              } else if (line.startsWith('data:')) {
                data += line.substring(5).trim() + '\n';
              } else if (line.trim() === '') {
                if (data) {
                  this.dispatchEvent({type: eventType, data: data.trim()});
                  data = '';
                  eventType = 'message';
                }
              }
            }

            readChunk();
          }).catch(err => {
            if (err.name === 'AbortError') {
              console.log('[SSE] Connection aborted intentionally');
            } else {
              console.error('[SSE] Read error:', err);
            }
            this.readyState = 2; // CLOSED
            this.dispatchEvent({type: 'error', error: err});
          });
        };

        readChunk();
      }).catch(err => {
        if (err.name === 'AbortError') {
          console.log('[SSE] Connection aborted intentionally');
        } else {
          console.error('[SSE] Connection error:', err);
        }
        this.readyState = 2; // CLOSED
        this.dispatchEvent({type: 'error', error: err});
      });
    }

    addEventListener(type, listener) {
      if (!this.listeners[type]) {
        this.listeners[type] = [];
      }
      this.listeners[type].push(listener);
    }

    removeEventListener(type, listener) {
      if (this.listeners[type]) {
        this.listeners[type] = this.listeners[type].filter(l => l !== listener);
      }
    }

    dispatchEvent(event) {
      const listeners = this.listeners[event.type] || [];
      listeners.forEach(listener => {
        try {
          listener(event);
        } catch (err) {
          console.error('[SSE] Event listener error:', err);
        }
      });
    }

    close() {
      console.log('[SSE] Closing connection...');
      this.readyState = 2; // CLOSED
      this.abortController.abort(); // Fetch 스트림 중단
    }
  }

  // ============== 토스트 알림 ==============
  function showToastNotification(notification) {
    // ✅ /match/online 페이지에서는 토스트 표시하지 않음
    const isOnlineMatchPage = window.location.pathname === '/match/online';
    if (isOnlineMatchPage) {
      console.log('[SSE] 온라인 매칭 페이지에서는 토스트 표시 안함');
      return;
    }

    const existingToast = document.getElementById('notification-toast');
    if (existingToast) {
      existingToast.remove();
    }

    const toast = document.createElement('div');
    toast.id = 'notification-toast';
    toast.className = 'notification-toast';
    toast.innerHTML = `
      <div class="notification-toast-content">
        <div class="notification-toast-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
          </svg>
        </div>
        <div class="notification-toast-text">
          <div class="notification-toast-title">${escapeHtml(
        notification.title || '알림')}</div>
          <div class="notification-toast-message">${escapeHtml(
        notification.message || '')}</div>
        </div>
      </div>
    `;

    toast.addEventListener('click', () => {
      window.location.href = '/notification';
    });

    document.body.appendChild(toast);

    setTimeout(() => toast.classList.add('show'), 10);

    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => {
        if (toast.parentNode) {
          toast.remove();
        }
      }, 300);
    }, 5000);
  }

  // ============== Heartbeat 타임아웃 관리 ==============
  function resetHeartbeatTimeout() {
    if (heartbeatTimeoutId) {
      clearTimeout(heartbeatTimeoutId);
      heartbeatTimeoutId = null;
    }

    // heartbeat를 한 번이라도 받았을 때만 타임아웃 체크
    if (heartbeatSupported) {
      heartbeatTimeoutId = setTimeout(() => {
        console.warn('[SSE] Heartbeat timeout detected - reconnecting...');
        reconnect();
      }, 45000); // 45초 (서버 20초 간격 + 여유)
    }
  }

  // ============== 재연결 ==============
  function reconnect() {
    console.log('[SSE] Reconnecting...');
    if (globalEventSource) {
      try {
        globalEventSource.close();
      } catch (err) {
        console.warn('[SSE] Error closing old connection:', err);
      }
      globalEventSource = null;
    }
    isConnected = false;
    heartbeatSupported = false;

    // 3초 후 재연결
    setTimeout(() => {
      initNotificationSubscription();
    }, 3000);
  }

  // ============== SSE 연결 초기화 ==============
  function initNotificationSubscription() {
    const token = getToken();
    if (!token) {
      console.log('[SSE] No token found, skipping notification subscription');
      return null;
    }

    // ✅ /home 페이지에서는 기존 연결 재사용하지 않음 (항상 새로 생성하여 이벤트 리스너 보장)
    const isHomePage = window.location.pathname === '/home'
        || window.location.pathname === '/';

    if (!isHomePage && globalEventSource && globalEventSource.readyState === 1
        && isConnected) {
      console.log('[SSE] ✅ Reusing existing valid connection');
      return globalEventSource;
    }

    // ✅ 기존 연결이 있지만 유효하지 않거나 /home 페이지면 정리
    if (globalEventSource) {
      console.log(
          '[SSE] ⚠️ Existing connection is invalid or home page - closing...');
      console.log('[SSE]   - readyState:', globalEventSource.readyState);
      console.log('[SSE]   - isConnected:', isConnected);
      console.log('[SSE]   - isHomePage:', isHomePage);
      try {
        if (globalEventSource.readyState !== 2
            && globalEventSource.abortController
            && !globalEventSource.abortController.signal.aborted) {
          globalEventSource.abortController.abort();
        }
        if (globalEventSource.readyState !== 2) {
          globalEventSource.close();
        }
      } catch (e) {
        console.warn('[SSE] Error closing invalid connection:', e);
      }
      globalEventSource = null;
      isConnected = false;
    }

    try {
      console.log('[SSE] 🔌 Initializing new SSE connection...');
      console.log('[SSE]   - Current pathname:', window.location.pathname);

      globalEventSource = new EventSourcePolyfill(
          '/api/notifications/subscribe', {
            headers: {
              'Authorization': `Bearer ${token}`
            }
          });

      // OPEN 이벤트
      globalEventSource.addEventListener('open', () => {
        console.log('[SSE] ✅ Connection opened');
        isConnected = true;

        // 연결 대기 중인 Promise들 해결
        connectionResolvers.forEach(resolve => resolve());
        connectionResolvers = [];
      });

      // HEARTBEAT 이벤트 (서버가 보내는 keep-alive)
      globalEventSource.addEventListener('heartbeat', (event) => {
        console.log('[SSE] 💓 Heartbeat received:', event.data);
        heartbeatSupported = true; // 첫 heartbeat 수신 시 플래그 설정
        resetHeartbeatTimeout();
      });

      // MESSAGE 이벤트 (기본)
      globalEventSource.addEventListener('message', (event) => {
        try {
          console.log('[SSE] 📩 Message received:', event.data);
          console.log('[SSE]   - Current pathname:', window.location.pathname);

          // ✅ "ping" (heartbeat) 데이터는 무시
          if (event.data === 'ping' || event.data.trim() === 'ping') {
            console.log(
                '[SSE] 💓 Heartbeat received via message event, ignoring');
            resetHeartbeatTimeout();
            return;
          }

          // ✅ JSON 유효성 검사
          let notification;
          try {
            notification = JSON.parse(event.data);
          } catch (parseErr) {
            console.warn('[SSE] Message is not valid JSON, ignoring:',
                event.data);
            return;
          }

          // ✅ 메시지 수신 시에도 타임아웃 리셋 (연결이 살아있음을 확인)
          resetHeartbeatTimeout();

          // ✅ MATCH_FOUND + ONLINE 알림 처리
          const isOnlineMatchPage = window.location.pathname
              === '/match/online';
          const isMatchFoundOnline = notification.notificationType
              === 'MATCH_FOUND' &&
              notification.relatedType === 'ONLINE' &&
              notification.relatedId;

          if (isMatchFoundOnline) {
            if (!isOnlineMatchPage) {
              // 다른 페이지에서 받은 경우: 온라인 매칭 페이지로 이동
              console.log('[SSE] 🔔 MATCH_FOUND + ONLINE 알림 감지 (다른 페이지)');
              console.log('[SSE]   - Current pathname:',
                  window.location.pathname);
              console.log('[SSE]   - SessionId:', notification.relatedId);
              console.log('[SSE] 🚀 리다이렉트: /match/online?autoMatch='
                  + notification.relatedId);

              // ✅ 즉시 리다이렉트 (비동기 작업 방해 방지)
              window.location.href = `/match/online?autoMatch=${notification.relatedId}`;
              return; // 토스트 표시하지 않고 바로 리턴
            } else {
              // 온라인 매칭 페이지에서 직접 받은 경우: 토스트 표시 안 함 (레이더 애니메이션 실행 중)
              console.log(
                  '[SSE] 🔔 MATCH_FOUND + ONLINE 알림 감지 (온라인 매칭 페이지) - 토스트 표시 안 함, 레이더 애니메이션 사용');
              
              // ✅ 스티키 저장 (이벤트 유실 방지)
              try {
                sessionStorage.setItem('pendingMatchFound', JSON.stringify({
                  notificationType: notification.notificationType,
                  relatedType: notification.relatedType,
                  relatedId: notification.relatedId,
                  timestamp: Date.now()
                }));
              } catch (e) {
                console.warn('[SSE] sessionStorage 저장 실패:', e);
              }
              
              // CustomEvent는 발생시켜서 online-match.js에서 처리할 수 있도록 함
              window.dispatchEvent(new CustomEvent('notification-received', {
                detail: notification
              }));
              return; // 토스트 표시하지 않고 리턴
            }
          }

          // 다른 알림은 기존 로직대로 처리 (토스트 표시)
          showToastNotification(notification);

          // 알림 수신 시 뱃지 개수 업데이트
          updateNotificationBadge();

          // CustomEvent 발생 (다른 스크립트에서 감지 가능)
          window.dispatchEvent(new CustomEvent('notification-received', {
            detail: notification
          }));

          console.log('[SSE] ✅ Custom event dispatched:',
              notification.notificationType);
        } catch (err) {
          console.error('[SSE] Failed to process message notification:', err);
        }
      });

      // NOTIFICATION 이벤트 (명시적)
      globalEventSource.addEventListener('notification', (event) => {
        try {
          console.log('[SSE] 🔔 Notification event received:', event.data);
          console.log('[SSE]   - Current pathname:', window.location.pathname);

          const notification = JSON.parse(event.data);

          // ✅ 알림 수신 시에도 타임아웃 리셋 (연결이 살아있음을 확인)
          resetHeartbeatTimeout();

          // ✅ MATCH_FOUND + ONLINE 알림 처리
          const isOnlineMatchPage = window.location.pathname
              === '/match/online';
          const isMatchFoundOnline = notification.notificationType
              === 'MATCH_FOUND' &&
              notification.relatedType === 'ONLINE' &&
              notification.relatedId;

          if (isMatchFoundOnline) {
            if (!isOnlineMatchPage) {
              // 다른 페이지에서 받은 경우: 온라인 매칭 페이지로 이동
              console.log('[SSE] 🔔 MATCH_FOUND + ONLINE 알림 감지 (다른 페이지)');
              console.log('[SSE]   - Current pathname:',
                  window.location.pathname);
              console.log('[SSE]   - SessionId:', notification.relatedId);
              console.log('[SSE] 🚀 리다이렉트: /match/online?autoMatch='
                  + notification.relatedId);

              // ✅ 즉시 리다이렉트 (비동기 작업 방해 방지)
              window.location.href = `/match/online?autoMatch=${notification.relatedId}`;
              return; // 토스트 표시하지 않고 바로 리턴
            } else {
              // 온라인 매칭 페이지에서 직접 받은 경우: 토스트 표시 안 함 (레이더 애니메이션 실행 중)
              console.log(
                  '[SSE] 🔔 MATCH_FOUND + ONLINE 알림 감지 (온라인 매칭 페이지) - 토스트 표시 안 함, 레이더 애니메이션 사용');
              
              // ✅ 스티키 저장 (이벤트 유실 방지)
              try {
                sessionStorage.setItem('pendingMatchFound', JSON.stringify({
                  notificationType: notification.notificationType,
                  relatedType: notification.relatedType,
                  relatedId: notification.relatedId,
                  timestamp: Date.now()
                }));
              } catch (e) {
                console.warn('[SSE] sessionStorage 저장 실패:', e);
              }
              
              // CustomEvent는 발생시켜서 online-match.js에서 처리할 수 있도록 함
              window.dispatchEvent(new CustomEvent('notification-received', {
                detail: notification
              }));
              return; // 토스트 표시하지 않고 리턴
            }
          }

          // 다른 알림은 기존 로직대로 처리 (토스트 표시)
          showToastNotification(notification);

          // 알림 수신 시 뱃지 개수 업데이트
          updateNotificationBadge();

          window.dispatchEvent(new CustomEvent('notification-received', {
            detail: notification
          }));

          console.log('[SSE] ✅ Custom event dispatched:',
              notification.notificationType);
        } catch (err) {
          console.error('[SSE] Failed to parse notification event:', err);
        }
      });

      // ERROR 이벤트
      globalEventSource.addEventListener('error', (event) => {
        console.error('[SSE] ❌ Error:', event.error);
        isConnected = false;

        if (heartbeatTimeoutId) {
          clearTimeout(heartbeatTimeoutId);
          heartbeatTimeoutId = null;
        }

        // 연결이 끊어지면 재연결 시도
        if (globalEventSource && globalEventSource.readyState === 2) {
          reconnect();
        }
      });

      return globalEventSource;
    } catch (err) {
      console.error('[SSE] Failed to initialize:', err);
      isConnected = false;
      return null;
    }
  }

  // ============== 전역 함수: SSE 연결 보장 ==============
  window.ensureSseConnected = function () {
    return new Promise((resolve) => {
      if (isConnected && globalEventSource && globalEventSource.readyState
          === 1) {
        console.log('[SSE] Already connected');
        resolve();
        return;
      }

      console.log('[SSE] Waiting for connection...');
      connectionResolvers.push(resolve);

      if (!globalEventSource) {
        initNotificationSubscription();
      }

      // 5초 타임아웃 (연결 실패 대비)
      setTimeout(() => {
        console.warn('[SSE] Connection timeout (5s)');
        resolve(); // 실패해도 resolve (차단하지 않음)
      }, 5000);
    });
  };

  // ✅ SSE 연결 상태 확인 함수
  window.isSseConnected = function () {
    return isConnected && globalEventSource && globalEventSource.readyState === 1;
  };

  // ============== BFCache 대응 ==============
  window.addEventListener('pageshow', (event) => {
    console.log('[SSE] 🔄 pageshow 이벤트 발생 - persisted:', event.persisted,
        ', pathname:', window.location.pathname);

    if (event.persisted) {
      // BFCache에서 복원된 경우
      console.log('[SSE] 🔄 Page restored from BFCache - reconnecting...');
      isConnected = false;
      if (globalEventSource) {
        try {
          if (globalEventSource.readyState !== 2
              && globalEventSource.abortController) {
            globalEventSource.abortController.abort();
          }
          globalEventSource.close();
        } catch (e) {
          console.warn('[SSE] Error closing old connection on BFCache restore:',
              e);
        }
      }
      globalEventSource = null;
      reconnect();
    } else {
      // ✅ /home 또는 / 페이지에서는 항상 새 연결 생성 (이벤트 리스너 보장)
      const isHomePage = window.location.pathname === '/home'
          || window.location.pathname === '/';

      if (isHomePage) {
        console.log(
            '[SSE] 🏠 Home page detected - forcing new connection for event listener guarantee...');
        isConnected = false;
        if (globalEventSource) {
          try {
            console.log('[SSE]   - Closing existing connection (readyState:',
                globalEventSource.readyState + ')');
            if (globalEventSource.readyState !== 2
                && globalEventSource.abortController
                && !globalEventSource.abortController.signal.aborted) {
              globalEventSource.abortController.abort();
            }
            if (globalEventSource.readyState !== 2) {
              globalEventSource.close();
            }
          } catch (e) {
            console.warn('[SSE] Error closing connection on home page:', e);
          }
        }
        globalEventSource = null;

        setTimeout(() => {
          console.log('[SSE] 🔌 Creating new SSE connection for home page...');
          initNotificationSubscription();
        }, 150);
      } else {
        // 다른 페이지는 기존 로직 유지
        console.log('[SSE] 🔄 Page loaded - ensuring SSE connection...');

        // ✅ 더 엄격한 체크: readyState가 1(OPEN)이고 isConnected가 true여야 함
        if (!globalEventSource || globalEventSource.readyState !== 1
            || !isConnected) {
          console.log(
              '[SSE] Connection not active or invalid - reinitializing...');
          console.log('[SSE]   - globalEventSource 존재:', !!globalEventSource);
          console.log('[SSE]   - readyState:', globalEventSource?.readyState);
          console.log('[SSE]   - isConnected:', isConnected);

          isConnected = false;
          if (globalEventSource) {
            try {
              if (globalEventSource.readyState !== 2
                  && globalEventSource.abortController
                  && !globalEventSource.abortController.signal.aborted) {
                globalEventSource.abortController.abort();
              }
              if (globalEventSource.readyState !== 2) {
                globalEventSource.close();
              }
            } catch (e) {
              console.warn('[SSE] Error closing old connection:', e);
            }
          }
          globalEventSource = null;

          setTimeout(() => {
            console.log('[SSE] 🔌 Reinitializing connection...');
            initNotificationSubscription();
          }, 100);
        } else {
          console.log('[SSE] ✅ Connection already active and valid');
        }
      }
    }
  });

  window.addEventListener('pagehide', () => {
    console.log('[SSE] 📴 Page hidden - marking as disconnected');
    isConnected = false;
    if (heartbeatTimeoutId) {
      clearTimeout(heartbeatTimeoutId);
      heartbeatTimeoutId = null;
    }
  });

  // ============== 페이지 언로드 시 정리 ==============
  window.addEventListener('beforeunload', () => {
    // ✅ 연결을 닫지 않고 플래그만 설정
    // 새 페이지에서 자동으로 재연결되므로 알림 누락 방지
    console.log(
        '[SSE] 🚪 Page unloading - marking as disconnected (will reconnect on new page)');
    isConnected = false;

    // heartbeat 타임아웃 정리
    if (heartbeatTimeoutId) {
      clearTimeout(heartbeatTimeoutId);
      heartbeatTimeoutId = null;
    }

    // 연결은 닫지 않음 (브라우저가 자동으로 닫음)
    // 새 페이지에서 자동으로 재연결되므로 알림 누락 방지
    // globalEventSource.close(); // 제거 - 새 페이지에서 재연결 보장
  });

  // ============== 초기 연결 ==============
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initNotificationSubscription);
  } else {
    initNotificationSubscription();
  }

  // ============== 알림 뱃지 업데이트 ==============

  /**
   * 서버에서 읽지 않은 알림 개수를 가져와 뱃지를 업데이트합니다.
   */
  async function updateNotificationBadge() {
    const token = getToken();
    if (!token) {
      return;
    }

    try {
      const response = await fetch('/api/notifications/unread-count', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const result = await response.json();
        // ApiResponse.data가 count
        const count = result.data;
        renderNotificationBadge(count);
      } else {
        console.warn('알림 개수 로드 실패:', response.status);
      }
    } catch (error) {
      console.error('알림 개수 조회 에러:', error);
    }
  }

  /**
   * 알림 뱃지를 렌더링합니다.
   * 홈 화면 등의 네비게이션 바에 있는 .notification-badge 요소를 찾아서 업데이트
   * @param {number} count 읽지 않은 알림 수
   */
  function renderNotificationBadge(count) {
    // 모든 뱃지 요소를 찾아서 업데이트 (헤더 등 여러 곳에 있을 수 있음)
    const badges = document.querySelectorAll('.notification-badge');

    badges.forEach(badge => {
      if (count > 0) {
        badge.textContent = count > 99 ? '99+' : count;
        // hidden 속성 제거 및 block 처리
        badge.removeAttribute('hidden');
        badge.style.display = 'inline-flex'; // 혹은 flex, block 등 CSS 디자인에 맞게
      } else {
        // 0이면 숨김
        badge.textContent = '';
        badge.setAttribute('hidden', 'hidden');
        badge.style.display = 'none';
      }
    });

    // 타이틀에도 표시 (선택사항)
    /*
    if (count > 0) {
      document.title = `(${count}) RunRun`;
    } else {
      document.title = 'RunRun';
    }
    */
  }

  // 초기화 시 실행 및 SSE 이벤트 발생 시 실행되도록 노출
  // 전역 메서드로 노출하여 다른 곳에서도 호출 가능하게 함
  window.updateNotificationBadge = updateNotificationBadge;

  // DOM 로드 시 1회 실행
  document.addEventListener('DOMContentLoaded', updateNotificationBadge);

  // ============== BFCache 대응 ==============
  window.addEventListener('pageshow', (event) => {
    console.log('[SSE] 🔄 pageshow 이벤트 발생 - persisted:', event.persisted,
        ', pathname:', window.location.pathname);

    if (event.persisted) {
      // BFCache에서 복원된 경우
      console.log('[SSE] 🔄 Page restored from BFCache - reconnecting...');
      isConnected = false;
      if (globalEventSource) {
        try {
          if (globalEventSource.readyState !== 2
              && globalEventSource.abortController) {
            globalEventSource.abortController.abort();
          }
          globalEventSource.close();
        } catch (e) {
          console.warn('[SSE] Error closing old connection on BFCache restore:',
              e);
        }
      }
      globalEventSource = null;
      reconnect();
    } else {
      // ✅ /home 또는 / 페이지에서는 항상 새 연결 생성 (이벤트 리스너 보장)
      const isHomePage = window.location.pathname === '/home'
          || window.location.pathname === '/';

      if (isHomePage) {
        console.log(
            '[SSE] 🏠 Home page detected - forcing new connection for event listener guarantee...');
        isConnected = false;
        if (globalEventSource) {
          try {
            console.log('[SSE]   - Closing existing connection (readyState:',
                globalEventSource.readyState + ')');
            if (globalEventSource.readyState !== 2
                && globalEventSource.abortController
                && !globalEventSource.abortController.signal.aborted) {
              globalEventSource.abortController.abort();
            }
            if (globalEventSource.readyState !== 2) {
              globalEventSource.close();
            }
          } catch (e) {
            console.warn('[SSE] Error closing connection on home page:', e);
          }
        }
        globalEventSource = null;

        setTimeout(() => {
          console.log('[SSE] 🔌 Creating new SSE connection for home page...');
          initNotificationSubscription();
        }, 150);
      } else {
        // 다른 페이지는 기존 로직 유지
        console.log('[SSE] 🔄 Page loaded - ensuring SSE connection...');

        // ✅ 더 엄격한 체크: readyState가 1(OPEN)이고 isConnected가 true여야 함
        if (!globalEventSource || globalEventSource.readyState !== 1
            || !isConnected) {
          console.log(
              '[SSE] Connection not active or invalid - reinitializing...');
          console.log('[SSE]   - globalEventSource 존재:', !!globalEventSource);
          console.log('[SSE]   - readyState:', globalEventSource?.readyState);
          console.log('[SSE]   - isConnected:', isConnected);

          isConnected = false;
          if (globalEventSource) {
            try {
              if (globalEventSource.readyState !== 2
                  && globalEventSource.abortController
                  && !globalEventSource.abortController.signal.aborted) {
                globalEventSource.abortController.abort();
              }
              if (globalEventSource.readyState !== 2) {
                globalEventSource.close();
              }
            } catch (e) {
              console.warn('[SSE] Error closing old connection:', e);
            }
          }
          globalEventSource = null;

          setTimeout(() => {
            console.log('[SSE] 🔌 Reinitializing connection...');
            initNotificationSubscription();
          }, 100);
        } else {
          console.log('[SSE] ✅ Connection already active and valid');
        }
      }
    }
  });

  window.addEventListener('pagehide', () => {
    console.log('[SSE] 📴 Page hidden - marking as disconnected');
    isConnected = false;
    if (heartbeatTimeoutId) {
      clearTimeout(heartbeatTimeoutId);
      heartbeatTimeoutId = null;
    }
  });

  // ============== 페이지 언로드 시 정리 ==============
  window.addEventListener('beforeunload', () => {
    // ✅ 연결을 닫지 않고 플래그만 설정
    // 새 페이지에서 자동으로 재연결되므로 알림 누락 방지
    console.log(
        '[SSE] 🚪 Page unloading - marking as disconnected (will reconnect on new page)');
    isConnected = false;

    // heartbeat 타임아웃 정리
    if (heartbeatTimeoutId) {
      clearTimeout(heartbeatTimeoutId);
      heartbeatTimeoutId = null;
    }

    // 연결은 닫지 않음 (브라우저가 자동으로 닫음)
    // 새 페이지에서 자동으로 재연결되므로 알림 누락 방지
    // globalEventSource.close(); // 제거 - 새 페이지에서 재연결 보장
  });

  // ============== 초기 연결 ==============
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initNotificationSubscription);
  } else {
    initNotificationSubscription();
  }

  // ============== 하단 내비게이션 바 매치 버튼 클릭 처리 ==============
  function initBottomNavHandler() {
    const bottomNav = document.querySelector(".bottom-nav");

    if (!bottomNav) {
      console.warn('[BottomNav] ⚠️ .bottom-nav 요소를 찾을 수 없습니다.');
      return false;
    }

    // 매치 버튼 찾기
    const matchLink = bottomNav.querySelector('a[href*="/match"]');

    if (!matchLink) {
      console.warn('[BottomNav] ⚠️ 매치 링크를 찾을 수 없습니다.');
      return false;
    }

    // 이미 이벤트가 등록되어 있는지 확인 (data 속성 사용)
    if (matchLink.dataset.handlerAttached === 'true') {
      console.log('[BottomNav] ℹ️ 이미 이벤트 리스너가 등록되어 있습니다.');
      return true;
    }

    // 클릭 이벤트 가로채기
    matchLink.addEventListener("click", async (e) => {
      e.preventDefault();
      e.stopPropagation();
      console.log('[BottomNav] 🖱️ 매치 버튼 클릭됨');

      const token = getToken();

      if (!token) {
        console.log('[BottomNav] ℹ️ 토큰이 없음, 매치 선택 페이지로 이동');
        window.location.href = "/match/select";
        return;
      }

      try {
        console.log('[BottomNav] 📡 API 호출: /api/match/active-session');
        const response = await fetch("/api/match/active-session", {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`
          }
        });

        console.log('[BottomNav] 📡 API 응답 상태:', response.status);

        if (response.ok) {
          const result = await response.json();
          console.log('[BottomNav] 📡 API 응답:', result);

          if (result.success && result.data) {
            console.log('[BottomNav] ✅ 활성 세션 발견:', result.data);
            console.log('[BottomNav] 🚀 리다이렉트:', result.data.redirectUrl);
            window.location.href = result.data.redirectUrl;
            return;
          }
        }

        // 활성 세션이 없으면 매치 선택 페이지로 이동
        console.log('[BottomNav] ℹ️ 활성 세션 없음, 매치 선택 페이지로 이동');
        window.location.href = "/match/select";
      } catch (error) {
        console.error('[BottomNav] ❌ 활성 세션 확인 실패:', error);
        window.location.href = "/match/select";
      }
    });

    // 이벤트 등록 완료 표시
    matchLink.dataset.handlerAttached = 'true';
    console.log('[BottomNav] ✅ 매치 버튼 이벤트 리스너 등록 완료');
    return true;
  }

  // 하단 내비게이션 바 초기화
  function initBottomNav() {
    // DOM이 로드되었는지 확인
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', () => {
        initBottomNavHandler();
      });
    } else {
      // DOM이 이미 로드된 경우
      initBottomNavHandler();
    }

    // 추가 안전장치: MutationObserver로 동적 추가된 bottom-nav 감지
    const observer = new MutationObserver(() => {
      if (document.querySelector(".bottom-nav") &&
          !document.querySelector(
              '.bottom-nav a[href*="/match"][data-handler-attached="true"]')) {
        initBottomNavHandler();
      }
    });

    observer.observe(document.body, {
      childList: true,
      subtree: true
    });
  }

  // 하단 내비게이션 바 초기화 실행
  initBottomNav();

})();
