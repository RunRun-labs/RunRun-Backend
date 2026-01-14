/**
 * 광고 공통 유틸리티
 * - 광고 로드, 표시, 클릭 처리
 */

/**
 * 광고 로드
 * @param {string} slotType - AdSlotType enum 값 (예: 'RUN_END_BANNER', 'HOME_TOP_BANNER')
 * @returns {Promise<Object|null>} 광고 데이터 또는 null
 */
async function loadAd(slotType) {
  try {
    // localStorage에서 userId 가져오기
    const userId = localStorage.getItem('userId');
    let url = `/api/ads/serve?slotType=${encodeURIComponent(slotType)}`;
    if (userId) {
      url += `&userId=${encodeURIComponent(userId)}`;
    }
    
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      // 광고가 없으면 404 등이 올 수 있음
      if (response.status === 404) {
        return null;
      }
      throw new Error(`광고 로드 실패: ${response.status}`);
    }

    const result = await response.json();
    if (result?.success && result?.data) {
      return result.data;
    }
    return null;
  } catch (error) {
    console.warn('광고 로드 실패:', error);
    return null;
  }
}

/**
 * 광고 클릭 처리
 * @param {number} placementId - 광고 배치 ID
 * @param {string} redirectUrl - 리다이렉트 URL
 */
async function handleAdClick(placementId, redirectUrl) {
  try {
    // 클릭 카운트 증가
    await fetch('/api/ads/click', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ placementId })
    });
  } catch (error) {
    console.warn('광고 클릭 카운트 실패:', error);
  }

  // 리다이렉트
  if (redirectUrl) {
    window.open(redirectUrl, '_blank');
  }
}

/**
 * 광고 배너 생성
 * @param {Object} adData - 광고 데이터
 * @param {string} className - 추가 CSS 클래스
 * @returns {HTMLElement} 광고 배너 요소
 */
function createAdBanner(adData, className = '') {
  const banner = document.createElement('div');
  banner.className = `ad-banner ${className}`.trim();
  banner.style.cssText = `
    width: 100%;
    margin: 0;
    border-radius: 0;
    overflow: hidden;
    cursor: pointer;
    transition: opacity 0.2s;
    background: #f9fafb;
  `;

  banner.addEventListener('mouseenter', () => {
    banner.style.opacity = '0.9';
  });

  banner.addEventListener('mouseleave', () => {
    banner.style.opacity = '1';
  });

  const img = document.createElement('img');
  img.src = adData.imageUrl;
  img.alt = adData.name || '광고';
  img.style.cssText = `
    width: 100%;
    min-width: 100%;
    height: 120px;
    display: block;
    object-fit: cover;
    object-position: center;
    margin: 0;
    padding: 0;
  `;
  img.onerror = function() {
    banner.style.display = 'none';
  };

  banner.appendChild(img);

  banner.addEventListener('click', () => {
    handleAdClick(adData.placementId, adData.redirectUrl);
  });

  return banner;
}

/**
 * 광고 팝업 생성 (러닝 결과용)
 * @param {Object} adData - 광고 데이터
 * @returns {HTMLElement} 광고 팝업 요소
 */
function createAdPopup(adData) {
  const overlay = document.createElement('div');
  overlay.className = 'ad-popup-overlay';
  overlay.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 10000;
    animation: fadeIn 0.3s ease;
  `;

  const popup = document.createElement('div');
  popup.className = 'ad-popup';
  popup.style.cssText = `
    position: relative;
    width: 90%;
    max-width: 400px;
    background: white;
    border-radius: 20px;
    overflow: hidden;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
    animation: slideUp 0.3s ease;
  `;

  const closeBtn = document.createElement('button');
  closeBtn.className = 'ad-popup-close';
  closeBtn.innerHTML = '✕';
  closeBtn.style.cssText = `
    position: absolute;
    top: 12px;
    right: 12px;
    width: 32px;
    height: 32px;
    border: none;
    background: rgba(0, 0, 0, 0.5);
    color: white;
    border-radius: 50%;
    font-size: 20px;
    cursor: pointer;
    z-index: 10;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background 0.2s;
  `;

  closeBtn.addEventListener('mouseenter', () => {
    closeBtn.style.background = 'rgba(0, 0, 0, 0.7)';
  });

  closeBtn.addEventListener('mouseleave', () => {
    closeBtn.style.background = 'rgba(0, 0, 0, 0.5)';
  });

  closeBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    overlay.remove();
  });

  const img = document.createElement('img');
  img.src = adData.imageUrl;
  img.alt = adData.name || '광고';
  img.style.cssText = `
    width: 100%;
    height: auto;
    display: block;
  `;
  img.onerror = function() {
    overlay.remove();
  };

  popup.addEventListener('click', () => {
    handleAdClick(adData.placementId, adData.redirectUrl);
    overlay.remove();
  });

  popup.appendChild(closeBtn);
  popup.appendChild(img);
  overlay.appendChild(popup);

  // 오버레이 클릭 시 닫기
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) {
      overlay.remove();
    }
  });

  // CSS 애니메이션 추가
  if (!document.getElementById('ad-popup-styles')) {
    const style = document.createElement('style');
    style.id = 'ad-popup-styles';
    style.textContent = `
      @keyframes fadeIn {
        from { opacity: 0; }
        to { opacity: 1; }
      }
      @keyframes slideUp {
        from {
          transform: translateY(20px);
          opacity: 0;
        }
        to {
          transform: translateY(0);
          opacity: 1;
        }
      }
    `;
    document.head.appendChild(style);
  }

  return overlay;
}
