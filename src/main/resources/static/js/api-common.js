/**
 * 공통 API 호출 유틸리티
 * - 토큰 만료 처리 (refreshToken 재발급)
 * - 로그인/로그아웃 처리
 */

/**
 * localStorage에서 accessToken 가져오기
 */
function getAccessToken() {
  return localStorage.getItem('accessToken');
}

/**
 * localStorage에서 refreshToken 가져오기
 */
function getRefreshToken() {
  return localStorage.getItem('refreshToken');
}

/**
 * localStorage 정리 (로그아웃 시)
 */
function clearAuthData() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('userId');
}

/**
 * 로그인 페이지로 이동
 */
function redirectToLogin(message = '로그인이 필요합니다.') {
  clearAuthData();
  alert(message);
  window.location.href = '/login';
}

/**
 * refreshToken으로 accessToken 재발급
 */
async function refreshAccessToken() {
  const refreshToken = getRefreshToken();
  
  if (!refreshToken) {
    throw new Error('refreshToken이 없습니다.');
  }

  try {
    const response = await fetch('/auth/refresh', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${refreshToken}`
      }
    });

    const result = await response.json().catch(() => ({}));

    if (!response.ok || !result?.success) {
      // refreshToken도 만료된 경우
      throw new Error('refreshToken이 만료되었습니다.');
    }

    // 새로운 토큰 저장
    const tokenData = result.data;
    if (tokenData?.accessToken) {
      localStorage.setItem('accessToken', tokenData.accessToken);
    }
    if (tokenData?.refreshToken) {
      localStorage.setItem('refreshToken', tokenData.refreshToken);
    }

    return tokenData.accessToken;
  } catch (error) {
    console.error('토큰 재발급 실패:', error);
    throw error;
  }
}

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
    return fetch(url, options);
  }

  let accessToken = getAccessToken();
  
  // accessToken이 없으면 로그인 페이지로 이동
  if (!accessToken) {
    redirectToLogin();
    throw new Error('accessToken이 없습니다.');
  }

  // 첫 번째 요청 (accessToken 사용)
  let response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
      ...options.headers
    }
  });

  // 401 에러인 경우 refreshToken으로 재발급 시도
  if (response.status === 401) {
    try {
      // refreshToken으로 accessToken 재발급
      const newAccessToken = await refreshAccessToken();
      
      // 재발급된 accessToken으로 원래 요청 재시도
      response = await fetch(url, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${newAccessToken}`,
          ...options.headers
        }
      });

      // 재시도 후에도 401이면 refreshToken도 만료
      if (response.status === 401) {
        redirectToLogin();
        throw new Error('토큰이 만료되었습니다. 다시 로그인해주세요.');
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
 * API 응답 JSON 파싱 (에러 처리 포함)
 */
async function parseApiResponse(response) {
  const result = await response.json().catch(() => ({}));
  
  if (!response.ok || !result?.success) {
    throw new Error(result?.message || 'API 호출 실패');
  }
  
  return result.data;
}
