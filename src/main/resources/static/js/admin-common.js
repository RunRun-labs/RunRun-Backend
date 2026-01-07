// Admin 공통 JavaScript 함수

/**
 * localStorage에서 엑세스 토큰 가져오기
 * @returns {string|null} Bearer 토큰 또는 null
 */
function getAccessToken() {
  const token = localStorage.getItem("accessToken");
  return token ? `Bearer ${token}` : null;
}

/**
 * API 요청 헤더 생성 (엑세스 토큰 포함)
 * @param {Object} additionalHeaders - 추가 헤더
 * @returns {Object} 헤더 객체
 */
function getAuthHeaders(additionalHeaders = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...additionalHeaders,
  };

  const token = getAccessToken();
  if (token) {
    headers["Authorization"] = token;
  }

  return headers;
}

/**
 * 로그아웃 처리
 */
async function handleLogout() {
  const token = getAccessToken();

  if (!token) {
    // 토큰이 없으면 바로 로그인 페이지로 이동
    localStorage.clear();
    window.location.href = "/login";
    return;
  }

  try {
    const response = await fetch("/auth/logout", {
      method: "GET",
      headers: {
        Authorization: token,
      },
    });

    const result = await response.json().catch(() => ({}));

    // 성공 여부와 관계없이 localStorage 클리어 및 로그인 페이지로 이동
    localStorage.clear();

    if (response.ok && result.success) {
      alert("로그아웃되었습니다.");
    }

    window.location.href = "/login";
  } catch (error) {
    console.error("로그아웃 오류:", error);
    // 오류가 발생해도 localStorage 클리어 및 로그인 페이지로 이동
    localStorage.clear();
    window.location.href = "/login";
  }
}

/**
 * 사이드바 활성 메뉴 항목 설정
 * 현재 페이지 URL에 따라 해당 메뉴 항목에 active 클래스 추가
 */
function setActiveSidebarItem() {
  const currentPath = window.location.pathname;
  const sidebarItems = document.querySelectorAll(".sidebar-item");

  sidebarItems.forEach((item) => {
    const href = item.getAttribute("href");

    // 로그아웃 버튼은 제외
    if (href === "#" || item.id === "logoutBtn") {
      return;
    }

    // 현재 경로와 일치하는 메뉴 항목 찾기
    if (href && currentPath.startsWith(href)) {
      item.classList.add("active");
    } else {
      item.classList.remove("active");
    }
  });
}

// DOM 로드 완료 시 사이드바 활성 메뉴 설정
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", setActiveSidebarItem);
} else {
  setActiveSidebarItem();
}
