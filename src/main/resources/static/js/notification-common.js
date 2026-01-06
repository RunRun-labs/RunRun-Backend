/**
 * 공통 알림 수신 스크립트
 * EventSource를 사용하여 실시간 알림을 수신하고 토스트 알림을 표시합니다.
 */

(function () {
  'use strict';

  // 토큰 가져오기
  function getToken() {
    return localStorage.getItem("accessToken") || getCookie("accessToken");
  }

  // 쿠키에서 값 가져오기
  function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
      return parts.pop().split(";").shift();
    }
    return null;
  }

  // EventSource polyfill (Authorization 헤더 지원)
  class EventSourcePolyfill {
    constructor(url, options = {}) {
      this.url = url;
      this.headers = options.headers || {};
      this.eventSource = null;
      this.listeners = {};
      this.readyState = 0; // CONNECTING
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
        console.error('No token available for SSE connection');
        this.readyState = 2;
        return;
      }

      // Fetch API를 사용하여 스트림 읽기 (Authorization 헤더 지원)
      fetch(this.url, {
        method: 'GET',
        headers: {
          ...this.headers,
          'Accept': 'text/event-stream',
          'Cache-Control': 'no-cache'
        },
        credentials: 'include'
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
              line = line.replace(/\r$/, '');     // CRLF 대응

              if (line.startsWith('event:')) {
                eventType = line.substring(6).trim();
              } else if (line.startsWith('data:')) {
                data += line.substring(5).trim() + '\n';
              } else if (line.trim() === '') {    // 여기 핵심: '' 대신 trim()
                if (data) {
                  this.dispatchEvent({type: eventType, data: data.trim()});
                  data = '';
                  eventType = 'message';
                }
              }
            }

            readChunk();
          }).catch(err => {
            console.error('SSE read error:', err);
            this.readyState = 2; // CLOSED
            this.dispatchEvent({type: 'error', error: err});
          });
        };

        readChunk();
      }).catch(err => {
        console.error('SSE connection error:', err);
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
          console.error('Event listener error:', err);
        }
      });
    }

    close() {
      this.readyState = 2; // CLOSED
      if (this.eventSource) {
        this.eventSource.abort();
      }
    }
  }

  // 토스트 알림 표시
  function showToastNotification(notification) {
    // 기존 토스트 제거
    const existingToast = document.getElementById('notification-toast');
    if (existingToast) {
      existingToast.remove();
    }

    // 토스트 요소 생성
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

    // 클릭 이벤트: 알림 페이지로 이동
    toast.addEventListener('click', () => {
      window.location.href = '/notification';
    });

    // 문서에 추가
    document.body.appendChild(toast);

    // 애니메이션: 나타나기
    setTimeout(() => {
      toast.classList.add('show');
    }, 10);

    // 5초 후 자동 제거
    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => {
        if (toast.parentNode) {
          toast.remove();
        }
      }, 300);
    }, 5000);
  }

  // HTML 이스케이프
  function escapeHtml(text) {
    if (!text) {
      return '';
    }
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  // SSE 연결 초기화
  function initNotificationSubscription() {
    const token = getToken();
    if (!token) {
      console.log('No token found, skipping notification subscription');
      return null;
    }

    try {
      const eventSource = new EventSourcePolyfill(
          '/api/notifications/subscribe', {
            headers: {
              'Authorization': `Bearer ${token}`
            }
          });

      eventSource.addEventListener('open', () => {
        console.log('Notification SSE connection opened');
      });

      eventSource.addEventListener('message', (event) => {
        try {
          const notification = JSON.parse(event.data);
          showToastNotification(notification);
        } catch (err) {
          console.error('Failed to parse notification:', err);
        }
      });

      // notification 이벤트 타입도 처리
      eventSource.addEventListener('notification', (event) => {
        try {
          const notification = JSON.parse(event.data);
          showToastNotification(notification);
        } catch (err) {
          console.error('Failed to parse notification:', err);
        }
      });

      eventSource.addEventListener('error', (event) => {
        console.error('Notification SSE error:', event.error);
        // 연결이 끊어지면 재연결 시도 (3초 후)
        setTimeout(() => {
          if (eventSource.readyState === 2) { // CLOSED
            initNotificationSubscription();
          }
        }, 3000);
      });

      // 페이지 언로드 시 연결 종료
      window.addEventListener('beforeunload', () => {
        eventSource.close();
      });

      return eventSource;
    } catch (err) {
      console.error('Failed to initialize notification subscription:', err);
      return null;
    }
  }

  // DOM 로드 완료 후 초기화
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initNotificationSubscription);
  } else {
    initNotificationSubscription();
  }

})();

