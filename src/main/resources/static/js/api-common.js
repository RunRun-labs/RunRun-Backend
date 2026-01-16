/**
 * 공통 API 호출 유틸리티
 * - 토큰 만료 처리 (refreshToken 재발급)
 * - 로그인/로그아웃 처리
 */

/**
 * localStorage에서 accessToken 가져오기
 */
function getAccessToken() {
  return localStorage.getItem("accessToken");
}

/**
 * localStorage에서 refreshToken 가져오기
 */
function getRefreshToken() {
  return localStorage.getItem("refreshToken");
}

/**
 * localStorage 정리 (로그아웃 시)
 */
function clearAuthData() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("userId");
}

/**
 * 로그인 페이지로 이동
 */
function redirectToLogin(message = "로그인이 필요합니다.") {
  clearAuthData();
  alert(message);
  window.location.href = "/login";
}

/**
 * refreshToken으로 accessToken 재발급
 */
async function refreshAccessToken() {
  const refreshToken = getRefreshToken();

  if (!refreshToken) {
    throw new Error("refreshToken이 없습니다.");
  }

  try {
    // ✅ refresh 요청은 원본 fetch 사용 (무한 루프 방지)
    const response = await originalFetch("/auth/refresh", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${refreshToken}`,
      },
    });

    const result = await response.json().catch(() => ({}));

    if (!response.ok || !result?.success) {
      // refreshToken도 만료된 경우
      throw new Error("refreshToken이 만료되었습니다.");
    }

    // 새로운 토큰 저장
    const tokenData = result.data;
    if (tokenData?.accessToken) {
      localStorage.setItem("accessToken", tokenData.accessToken);
    }
    if (tokenData?.refreshToken) {
      localStorage.setItem("refreshToken", tokenData.refreshToken);
    }

    return tokenData.accessToken;
  } catch (error) {
    console.error("토큰 재발급 실패:", error);
    throw error;
  }
}

// ✅ 전역 fetch 함수 원본 저장
const originalFetch = window.fetch;

/**
 * 공통 fetch 함수 (토큰 만료 처리 포함)
 * @param {string} url - API URL
 * @param {object} options - fetch options
 * @param {boolean} requireAuth - 인증 필수 여부 (기본값: true)
 * @returns {Promise<Response>}
 */
async function fetchWithAuth(url, options = {}, requireAuth = true) {
  // 인증이 필요 없는 경우 (permitAll 경로)
  if (!requireAuth) {
    return originalFetch(url, options);
  }

  let accessToken = getAccessToken();

  // accessToken이 없으면 로그인 페이지로 이동
  if (!accessToken) {
    redirectToLogin();
    throw new Error("accessToken이 없습니다.");
  }

  // 첫 번째 요청 (accessToken 사용)
  let response = await originalFetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
      ...options.headers,
    },
  });

  // 401 에러인 경우 refreshToken으로 재발급 시도
  if (response.status === 401) {
    try {
      // refreshToken으로 accessToken 재발급
      const newAccessToken = await refreshAccessToken();

      // 재발급된 accessToken으로 원래 요청 재시도
      response = await originalFetch(url, {
        ...options,
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${newAccessToken}`,
          ...options.headers,
        },
      });

      // 재시도 후에도 401이면 refreshToken도 만료
      if (response.status === 401) {
        redirectToLogin();
        throw new Error("토큰이 만료되었습니다. 다시 로그인해주세요.");
      }
    } catch (error) {
      // refreshToken 재발급 실패 시 로그인 페이지로 이동
      redirectToLogin();
      throw error;
    }
  }

  return response;
}

/**
 * ✅ 전역 fetch 함수 래핑 (모든 fetch 호출에 자동 refresh 로직 적용)
 * Authorization 헤더가 있는 요청만 감지하여 401 시 자동으로 refresh 후 재시도
 */
window.fetch = async function (url, options = {}) {
  // ✅ /auth/refresh 요청은 원본 fetch 사용 (무한 루프 방지)
  const urlString = typeof url === "string" ? url : url.toString();
  const isRefreshRequest = urlString.includes("/auth/refresh");

  if (isRefreshRequest) {
    return originalFetch(url, options);
  }

  // ✅ Authorization 헤더 확인
  let hasAuthHeader = false;
  let authValue = null;

  if (options.headers) {
    if (options.headers instanceof Headers) {
      authValue = options.headers.get("Authorization");
      hasAuthHeader = !!authValue;
    } else if (typeof options.headers === "object") {
      authValue =
        options.headers.Authorization ||
        options.headers.authorization ||
        options.headers["Authorization"];
      hasAuthHeader = !!authValue;
    }
  }

  // ✅ Authorization 헤더가 없으면 원본 fetch 사용
  if (!hasAuthHeader) {
    return originalFetch(url, options);
  }

  // ✅ 첫 번째 요청 (원본 fetch 사용)
  let response = await originalFetch(url, options);

  // ✅ 401 에러이고 Authorization 헤더가 있는 경우에만 refresh 시도
  if (response.status === 401) {
    try {
      const refreshToken = getRefreshToken();
      if (!refreshToken) {
        // refreshToken이 없으면 원본 응답 반환
        return response;
      }

      // ✅ refreshToken으로 accessToken 재발급
      const newAccessToken = await refreshAccessToken();

      // ✅ 재발급된 accessToken으로 원래 요청 재시도
      const newOptions = { ...options };
      if (newOptions.headers instanceof Headers) {
        newOptions.headers.set("Authorization", `Bearer ${newAccessToken}`);
      } else if (typeof newOptions.headers === "object") {
        newOptions.headers = {
          ...newOptions.headers,
          Authorization: `Bearer ${newAccessToken}`,
        };
      } else {
        newOptions.headers = {
          "Content-Type": "application/json",
          Authorization: `Bearer ${newAccessToken}`,
        };
      }

      response = await originalFetch(url, newOptions);

      // ✅ 재시도 후에도 401이면 refreshToken도 만료
      if (response.status === 401) {
        redirectToLogin();
      }
    } catch (error) {
      // ✅ refresh 실패 시 원본 응답 반환
      console.error("토큰 재발급 실패:", error);
      if (
        error.message &&
        error.message.includes("refreshToken이 만료되었습니다")
      ) {
        redirectToLogin();
      }
      return response;
    }
  }

  return response;
};

/**
 * API 응답 JSON 파싱 (에러 처리 포함)
 */
async function parseApiResponse(response) {
  const result = await response.json().catch(() => ({}));

  if (!response.ok || !result?.success) {
    throw new Error(result?.message || "API 호출 실패");
  }

  return result.data;
}

/**
 * 페이지 로드 시 인증 체크 (자동 실행)
 * - 로그인/회원가입 페이지는 제외
 */
(function checkAuthOnPageLoad() {
  // 현재 경로 확인
  const currentPath = window.location.pathname;

  // 인증 불필요한 경로 (로그인, 회원가입 등)
  const publicPaths = ["/login", "/signup", "/auth"];

  // public 경로가 아니고 토큰이 없으면 로그인 요청
  const isPublicPath = publicPaths.some(
    (path) => currentPath === path || currentPath.startsWith(path)
  );

  if (!isPublicPath) {
    const accessToken = getAccessToken();
    if (!accessToken) {
      // 로그인 알람 후 리다이렉트
      redirectToLogin("로그인이 필요합니다. 로그인 후 이용해주세요.");
      return;
    }
  }
})();
