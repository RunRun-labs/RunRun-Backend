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
    if (!text) return '';
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
      if (this.readyState === 2) return; // CLOSED

      this.readyState = 0; // CONNECTING

      const token = this.headers['Authorization']?.replace('Bearer ', '') || getToken();
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
        this.dispatchEvent({ type: 'open' });

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        const readChunk = () => {
          reader.read().then(({ done, value }) => {
            if (done) {
              this.readyState = 2; // CLOSED
              this.dispatchEvent({ type: 'error' });
              return;
            }

            buffer += decoder.decode(value, { stream: true });
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
                  this.dispatchEvent({ type: eventType, data: data.trim() });
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
            this.dispatchEvent({ type: 'error', error: err });
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
        this.dispatchEvent({ type: 'error', error: err });
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
          <div class="notification-toast-title">${escapeHtml(notification.title || '알림')}</div>
          <div class="notification-toast-message">${escapeHtml(notification.message || '')}</div>
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
        if (toast.parentNode) toast.remove();
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

    // 기존 연결이 있으면 재사용
    if (globalEventSource && isConnected) {
      console.log('[SSE] Reusing existing connection');
      return globalEventSource;
    }

    try {
      console.log('[SSE] Initializing new connection...');
      
      globalEventSource = new EventSourcePolyfill('/api/notifications/subscribe', {
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
          
          // ✅ "ping" (heartbeat) 데이터는 무시
          if (event.data === 'ping' || event.data.trim() === 'ping') {
            console.log('[SSE] 💓 Heartbeat received via message event, ignoring');
            resetHeartbeatTimeout();
            return;
          }
          
          // ✅ JSON 유효성 검사
          let notification;
          try {
            notification = JSON.parse(event.data);
          } catch (parseErr) {
            console.warn('[SSE] Message is not valid JSON, ignoring:', event.data);
            return;
          }
          
          // ✅ 메시지 수신 시에도 타임아웃 리셋 (연결이 살아있음을 확인)
          resetHeartbeatTimeout();
          
          // 토스트 표시
          showToastNotification(notification);
          
          // CustomEvent 발생 (다른 스크립트에서 감지 가능)
          window.dispatchEvent(new CustomEvent('notification-received', { 
            detail: notification 
          }));
          
          console.log('[SSE] ✅ Custom event dispatched:', notification.notificationType);
        } catch (err) {
          console.error('[SSE] Failed to process message notification:', err);
        }
      });

      // NOTIFICATION 이벤트 (명시적)
      globalEventSource.addEventListener('notification', (event) => {
        try {
          console.log('[SSE] 🔔 Notification event received:', event.data);
          const notification = JSON.parse(event.data);
          
          // ✅ 알림 수신 시에도 타임아웃 리셋 (연결이 살아있음을 확인)
          resetHeartbeatTimeout();
          
          showToastNotification(notification);
          
          window.dispatchEvent(new CustomEvent('notification-received', { 
            detail: notification 
          }));
          
          console.log('[SSE] ✅ Custom event dispatched:', notification.notificationType);
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
  window.ensureSseConnected = function() {
    return new Promise((resolve) => {
      if (isConnected && globalEventSource && globalEventSource.readyState === 1) {
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

  // ============== BFCache 대응 ==============
  window.addEventListener('pageshow', (event) => {
    if (event.persisted) {
      console.log('[SSE] 🔄 Page restored from BFCache - reconnecting...');
      isConnected = false;
      reconnect();
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
    if (globalEventSource) {
      console.log('[SSE] 🚪 Page unloading - closing connection');
      globalEventSource.close();
    }
  });

  // ============== 초기 연결 ==============
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initNotificationSubscription);
  } else {
    initNotificationSubscription();
  }

})();
