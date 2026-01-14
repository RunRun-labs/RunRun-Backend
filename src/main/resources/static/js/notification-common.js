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
  let matchFoundHandledGlobally = false;

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

  // ============== EventSource Polyfill ==============
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
        signal: this.abortController.signal
      }).then(response => {
        if (!response.ok) {
          throw new Error(
              `HTTP error! status: ${response.status}`);
        }

        this.readyState = 1; // OPEN
        this.dispatchEvent({type: 'open'});

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        const readChunk = () => {
          reader.read().then(({done, value}) => {
            if (done) {
              this.readyState = 2;
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
            if (err.name !== 'AbortError') {
              console.error('[SSE] Read error:', err);
              this.readyState = 2;
              this.dispatchEvent({type: 'error', error: err});
            }
          });
        };
        readChunk();
      }).catch(err => {
        if (err.name !== 'AbortError') {
          console.error('[SSE] Connection error:', err);
          this.readyState = 2;
          this.dispatchEvent({type: 'error', error: err});
        }
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
          console.error('[SSE] Listener error:', err);
        }
      });
    }

    close() {
      this.readyState = 2;
      this.abortController.abort();
    }
  }

  // ============== 토스트 알림 ==============
  function showToastNotification(notification) {
    if (window.location.pathname === '/match/online') {
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
      </div>`;

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

  // ============== Heartbeat & SSE 로직 ==============
  function resetHeartbeatTimeout() {
    if (heartbeatTimeoutId) {
      clearTimeout(heartbeatTimeoutId);
    }
    if (heartbeatSupported) {
      heartbeatTimeoutId = setTimeout(() => {
        console.warn('[SSE] Heartbeat timeout - reconnecting...');
        reconnect();
      }, 45000);
    }
  }

  function reconnect() {
    if (globalEventSource) {
      try {
        globalEventSource.close();
      } catch (e) {
      }
      globalEventSource = null;
    }
    isConnected = false;
    heartbeatSupported = false;
    setTimeout(initNotificationSubscription, 3000);
  }

  function initNotificationSubscription() {
    const token = getToken();
    if (!token) {
      return null;
    }

    const isHomePage = window.location.pathname === '/home'
        || window.location.pathname === '/';
    if (!isHomePage && globalEventSource && globalEventSource.readyState === 1
        && isConnected) {
      return globalEventSource;
    }

    if (globalEventSource) {
      try {
        globalEventSource.close();
      } catch (e) {
      }
      globalEventSource = null;
      isConnected = false;
    }

    globalEventSource = new EventSourcePolyfill('/api/notifications/subscribe',
        {
          headers: {'Authorization': `Bearer ${token}`}
        });

    globalEventSource.addEventListener('open', () => {
      isConnected = true;
      connectionResolvers.forEach(resolve => resolve());
      connectionResolvers = [];
    });

    globalEventSource.addEventListener('heartbeat', () => {
      heartbeatSupported = true;
      resetHeartbeatTimeout();
    });

    const handleData = (dataStr) => {
      if (dataStr === 'ping') {
        resetHeartbeatTimeout();
        return;
      }
      try {
        const notification = JSON.parse(dataStr);
        resetHeartbeatTimeout();

        const isOnlineMatchPage = window.location.pathname === '/match/online';
        if (notification.notificationType === 'MATCH_FOUND'
            && notification.relatedType === 'ONLINE'
            && notification.relatedId) {
          if (!isOnlineMatchPage) {
            if (matchFoundHandledGlobally) {
              return;
            }
            matchFoundHandledGlobally = true;
            window.location.href = `/match/online?autoMatch=${notification.relatedId}`;
            return;
          } else {
            sessionStorage.setItem('pendingMatchFound',
                JSON.stringify({...notification, timestamp: Date.now()}));
          }
        }

        showToastNotification(notification);
        updateNotificationBadge();
        window.dispatchEvent(
            new CustomEvent('notification-received', {detail: notification}));
      } catch (e) {
        console.warn('[SSE] Data parse error', e);
      }
    };

    globalEventSource.addEventListener('message', (e) => handleData(e.data));
    globalEventSource.addEventListener('notification',
        (e) => handleData(e.data));
    globalEventSource.addEventListener('error', () => {
      isConnected = false;
      if (globalEventSource?.readyState === 2) {
        reconnect();
      }
    });

    return globalEventSource;
  }

  // ============== 전역 API ==============
  window.ensureSseConnected = function () {
    return new Promise((resolve) => {
      if (isConnected && globalEventSource?.readyState === 1) {
        resolve();
        return;
      }
      connectionResolvers.push(resolve);
      if (!globalEventSource) {
        initNotificationSubscription();
      }
      setTimeout(resolve, 5000);
    });
  };

  window.isSseConnected = () => isConnected && globalEventSource?.readyState
      === 1;

  async function updateNotificationBadge() {
    const token = getToken();
    if (!token) {
      return;
    }
    try {
      const response = await fetch('/api/notifications/unread-count', {
        headers: {'Authorization': `Bearer ${token}`}
      });
      if (response.ok) {
        const result = await response.json();
        const count = result.data;
        document.querySelectorAll('.notification-badge').forEach(badge => {
          if (count > 0) {
            badge.textContent = count > 99 ? '99+' : count;
            badge.removeAttribute('hidden');
            badge.style.display = 'inline-flex';
          } else {
            badge.setAttribute('hidden', 'hidden');
            badge.style.display = 'none';
          }
        });
      }
    } catch (e) {
    }
  }

  window.updateNotificationBadge = updateNotificationBadge;

  // ============== Lifecycle 관리 ==============
  window.addEventListener('pageshow', (event) => {
    if (event.persisted || !isConnected) {
      reconnect();
    }
  });

  window.addEventListener('pagehide', () => {
    isConnected = false;
    if (heartbeatTimeoutId) {
      clearTimeout(heartbeatTimeoutId);
    }
  });

  // ============== 하단 네비게이션 핸들러 ==============
  function initBottomNavHandler() {
    const matchLink = document.querySelector('.bottom-nav a[href*="/match"]');
    if (!matchLink || matchLink.dataset.handlerAttached === 'true') {
      return;
    }

    matchLink.addEventListener("click", async (e) => {
      e.preventDefault();
      const token = getToken();
      if (!token) {
        window.location.href = "/match/select";
        return;
      }

      try {
        const response = await fetch("/api/match/active-session", {
          headers: {"Authorization": `Bearer ${token}`}
        });
        if (response.ok) {
          const result = await response.json();
          if (result.success && result.data) {
            window.location.href = result.data.redirectUrl;
            return;
          }
        }
        window.location.href = "/match/select";
      } catch (error) {
        window.location.href = "/match/select";
      }
    });
    matchLink.dataset.handlerAttached = 'true';
  }

  // 초기화 실행
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      initNotificationSubscription();
      updateNotificationBadge();
      initBottomNavHandler();
    });
  } else {
    initNotificationSubscription();
    updateNotificationBadge();
    initBottomNavHandler();
  }

  // MutationObserver로 동적 요소 대응
  new MutationObserver(initBottomNavHandler).observe(document.body,
      {childList: true, subtree: true});

})();