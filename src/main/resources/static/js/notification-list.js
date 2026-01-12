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

  // RelatedType에 따른 라우팅 URL 반환
  function getRouteUrl(relatedType, relatedId) {
    if (!relatedType || !relatedId) {
      return null;
    }

    const routeMap = {
      OFF_CHAT_ROOM: `/chat/chat1?sessionId=${relatedId}`,
      RECRUIT: `/recruit/${relatedId}`,
      WAITING_ROOM: `/match/waiting?sessionId=${relatedId}`,
      ONLINE: `/match/waiting?sessionId=${relatedId}`,

      CREW_JOIN_REQUEST: `/crews/${relatedId}/join-requests`,
      CREW: `/crews/${relatedId}`,
      CREW_USERS: `/crews/${relatedId}/users`,
      CREW_MAIN: `/crews/main`,
      CREW_CHAT_ROOM: `/chat/crew?roomId=${relatedId}`,

      MEMBERSHIP: `/membership`,

      POINT_BALANCE: `/points/balance`,

      CHALLENGE: `/challenge`,
      CHALLENGE_END: `/challenge/end`,

      FEED_RECORD: `/feed/records`,

      FRIEND_LIST: `/friends/list`
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

  // 매치 세션 유효성 검증 (STANDBY 상태 및 참가자 확인)
  async function validateMatchSession(sessionId) {
    const token = getToken();
    if (!token) {
      return false;
    }

    // 현재 사용자 ID 가져오기
    const currentUserIdStr = localStorage.getItem("userId");
    if (!currentUserIdStr) {
      console.error("사용자 ID를 찾을 수 없습니다.");
      return false;
    }

    const currentUserId = parseInt(currentUserIdStr, 10);
    if (isNaN(currentUserId)) {
      console.error("유효하지 않은 사용자 ID:", currentUserIdStr);
      return false;
    }

    try {
      const response = await fetch(`/api/match/session/${sessionId}`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`
        }
      });

      // 200 OK면 응답 본문 확인
      if (response.ok) {
        const result = await response.json();

        // ApiResponse 구조: { success: true, data: { status: "STANDBY", participants: [...], ... } }
        if (result.success && result.data) {
          const sessionData = result.data;
          const sessionStatus = sessionData.status;
          const participants = sessionData.participants || [];

          // ✅ 1. STANDBY 상태인지 확인
          if (sessionStatus !== "STANDBY") {
            console.log(`매치 세션이 STANDBY 상태가 아님: ${sessionStatus}`);
            return false;
          }

          // ✅ 2. 현재 사용자가 참가자 목록에 있는지 확인
          const isParticipant = participants.some(
              participant => participant.userId === currentUserId
          );

          if (!isParticipant) {
            console.log(`현재 사용자(${currentUserId})가 세션(${sessionId})의 참가자가 아님`);
            return false;
          }

          // STANDBY 상태이고 참가자인 경우에만 유효
          return true;
        }

        return false;
      }

      // 404나 다른 에러면 유효하지 않은 세션
      return false;
    } catch (error) {
      console.error("매치 세션 유효성 확인 실패:", error);
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
      // ✅ 매치 세션 관련 알림인 경우 유효성 확인
      if (notification.relatedType === "WAITING_ROOM"
          || notification.relatedType === "ONLINE") {
        const isValid = await validateMatchSession(notification.relatedId);
        if (!isValid) {
          alert("이미 종료되거나 유효하지 않은 매치 세션입니다.");
          return; // 이동 중단
        }
      }

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

