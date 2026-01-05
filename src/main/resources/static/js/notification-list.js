document.addEventListener("DOMContentLoaded", () => {
  const backButton = document.getElementById("backBtn");
  const notificationList = document.getElementById("notificationList");
  const loadingSpinner = document.getElementById("loadingSpinner");
  const emptyState = document.getElementById("emptyState");
  const sentinel = document.getElementById("sentinel");
  const container = document.querySelector(".page");

  let currentPage = 0;
  let hasNext = true;
  let isLoading = false;

  // 뒤로가기 버튼
  if (backButton) {
    backButton.addEventListener("click", () => {
      window.history.length > 1
          ? window.history.back()
          : (window.location.href = "/");
    });
  }

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

  // 날짜 포맷팅 (MM/DD HH:mm 형식)
  function formatDate(dateString) {
    if (!dateString) {
      return "";
    }

    const date = new Date(dateString);
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");

    return `${month}/${day} ${hours}:${minutes}`;
  }

  // 알림 타입에 따른 아이콘 반환
  function getNotificationIcon(notificationType) {
    const iconMap = {
      MATCH: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M9 11l3 3L22 4"/>
        <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/>
      </svg>`,
      CREW: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
        <circle cx="9" cy="7" r="4"/>
        <path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/>
      </svg>`,
      CHAT: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>
      </svg>`,
      RECRUIT: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 2L2 7l10 5 10-5-10-5z"/>
        <path d="M2 17l10 5 10-5M2 12l10 5 10-5"/>
      </svg>`,
      RUNNING: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
      </svg>`,
      CHALLENGE: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 2L2 7l10 5 10-5-10-5z"/>
        <path d="M2 17l10 5 10-5M2 12l10 5 10-5"/>
      </svg>`,
      FRIEND: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
        <circle cx="8.5" cy="7" r="4"/>
        <path d="M20 8v6M23 11h-6"/>
      </svg>`,
      ADVERTISEMENT: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
        <path d="M9 9h6v6H9z"/>
      </svg>`,
      COUPON: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M21 12c0 1.66-1.34 3-3 3H6c-1.66 0-3-1.34-3-3s1.34-3 3-3h12c1.66 0 3 1.34 3 3z"/>
        <path d="M9 12h6"/>
      </svg>`,
      default: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"/>
        <path d="M12 16v-4M12 8h.01"/>
      </svg>`
    };

    return iconMap[notificationType] || iconMap.default;
  }

  // RelatedType에 따른 라우팅 URL 반환
  function getRouteUrl(relatedType, relatedId) {
    if (!relatedType || !relatedId) {
      return null;
    }

    const routeMap = {
      CREW_CHAT_ROOM: `/chat`,
      OFFLINE_CHAT_ROOM: `/chat`,
      RECRUIT: `/recruit/${relatedId}`,
      CREW: `/crews/${relatedId}`,
      CREW_JOIN_REQUEST: `/crews/${relatedId}/join-requests`,
      CREW_USER: `/crews/${relatedId}/users`,
      CREW_ACTIVITY: `/crews/${relatedId}`,
      CREW_ACTIVITY_USER: `/crews/${relatedId}`,
      ONLINE_BATTLE: `/match/online`,
      SOLORUN: `/match/solo`,
      GHOSTRUN: `/match/ghost`,
      BATTLE_RESULT: `/match/battleDetail/${relatedId}`,
      RUNNING_RESULT: `/match/result/${relatedId}`,
      CHALLENGE: `/challenge/${relatedId}`,
      FEED_POST: `/feed/${relatedId}`,
      MEMBERSHIP: `/membership`,
      PAYMENT: `/payment/${relatedId}`,
      USER_POINT: `/my`,
      POINT_HISTORY: `/my`,
      POINT_EXPIRATION: `/my`,
      POINT_PRODUCT: `/my`,
      RATING: `/my`,
      AD: `/`,
      COUPON: `/my`,
      COURSE: `/course/${relatedId}`,
      USERS: `/user/${relatedId}`
    };

    return routeMap[relatedType] || null;
  }

  // 알림 읽음 처리
  async function markAsRead(notificationId) {
    const token = getToken();
    if (!token) {
      return false;
    }

    try {
      const response = await fetch(`/api/notifications/${notificationId}/read`,
          {
            method: "PATCH",
            headers: {
              "Content-Type": "application/json",
              "Authorization": `Bearer ${token}`
            }
          });

      if (!response.ok) {
        throw new Error("읽음 처리 실패");
      }

      return true;
    } catch (error) {
      console.error("알림 읽음 처리 실패:", error);
      return false;
    }
  }

  // 알림 클릭 처리
  async function handleNotificationClick(notification) {
    // 읽지 않은 알림이면 읽음 처리
    if (!notification.read) {
      const success = await markAsRead(notification.id);
      if (success) {
        // notification 객체 상태 업데이트
        notification.read = true;

        // UI 업데이트: unread 클래스 제거, read 클래스 추가
        const card = document.querySelector(
            `[data-notification-id="${notification.id}"]`);
        if (card) {
          card.classList.remove("unread");
          card.classList.add("read");
        }
      }
    }

    // 라우팅 처리
    if (notification.relatedType && notification.relatedId) {
      const routeUrl = getRouteUrl(notification.relatedType,
          notification.relatedId);
      if (routeUrl) {
        window.location.href = routeUrl;
      }
    }
  }

  // 알림 아이템 렌더링 (카드형)
  function renderNotificationItem(notification) {
    const card = document.createElement("div");
    const isRead = notification.read === true || notification.read
        === "true" || notification.read === 1;
    card.className = `notification-card ${isRead ? "read" : "unread"}`;
    card.dataset.notificationId = notification.id;

    // createdAt이 NotificationResDto에 없을 수 있음 (백엔드에서 추가 필요)
    // 일단 createdAt이 있다고 가정하고 처리
    const dateString = notification.createdAt || null;
    const dateFormatted = dateString ? formatDate(dateString) : "";

    card.innerHTML = `
      <div class="notification-card-header">
        <div class="notification-content">
          <h3 class="notification-title">${escapeHtml(notification.title)}</h3>
          <p class="notification-message">${escapeHtml(notification.message)}</p>
          <div class="notification-date">${dateFormatted}</div>
        </div>
      </div>
    `;

    card.addEventListener("click", () => {
      handleNotificationClick(notification);
    });

    return card;
  }

  // HTML 이스케이프
  function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
  }

  // 알림 목록 로드
  async function loadNotifications(reset = false) {
    if (isLoading) {
      return;
    }
    if (!hasNext && !reset) {
      return;
    }

    isLoading = true;

    if (reset) {
      notificationList.innerHTML = "";
      currentPage = 0;
      hasNext = true;
    }

    loadingSpinner.style.display = "flex";
    emptyState.style.display = "none";

    try {
      const token = getToken();
      const headers = {
        "Content-Type": "application/json"
      };

      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }

      const params = new URLSearchParams({
        page: currentPage.toString(),
        size: "10"
      });

      const response = await fetch(`/api/notifications/remaining?${params}`, {
        method: "GET",
        headers: headers
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      if (result.success && result.data) {
        const sliceData = result.data;
        const notifications = Array.isArray(sliceData) ? sliceData
            : (sliceData.content || []);

        // Slice의 last 필드 확인 (last가 false면 다음 페이지 있음)
        // Slice 구조: { content: [...], last: boolean, ... }
        hasNext = sliceData.last !== undefined ? !sliceData.last
            : (notifications.length >= 10);

        if (notifications.length === 0 && reset) {
          emptyState.style.display = "flex";
        } else {
          notifications.forEach((notification) => {
            const item = renderNotificationItem(notification);
            notificationList.appendChild(item);
          });
          currentPage++;
        }
      } else {
        throw new Error(result.message || "알림을 불러오는데 실패했습니다.");
      }
    } catch (error) {
      console.error("알림 로드 실패:", error);
      if (reset) {
        emptyState.style.display = "flex";
        emptyState.innerHTML = `<p>알림을 불러오는데 실패했습니다.<br>${error.message}</p>`;
      }
    } finally {
      loadingSpinner.style.display = "none";
      isLoading = false;
      setupInfiniteScroll();
    }
  }

  // Intersection Observer를 사용한 무한 스크롤
  let observer = null;

  function setupInfiniteScroll() {
    if (observer) {
      observer.disconnect();
    }

    if (!sentinel || !hasNext) {
      return;
    }

    observer = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting && !isLoading && hasNext) {
              loadNotifications(false);
            }
          });
        },
        {
          root: container,
          rootMargin: "100px",
          threshold: 0.1
        }
    );

    observer.observe(sentinel);
  }

  // 초기화
  loadNotifications(true);
  setupInfiniteScroll();
});

