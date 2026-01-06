// Get JWT Token
function getAccessToken() {
  let token = localStorage.getItem("accessToken");
  if (token) {
    return token.startsWith("Bearer ") ? token : `Bearer ${token}`;
  }
  const cookies = document.cookie.split(";");
  for (let cookie of cookies) {
    const [name, value] = cookie.trim().split("=");
    if (name === "accessToken" || name === "token") {
      return value.startsWith("Bearer ") ? value : `Bearer ${value}`;
    }
  }
  return null;
}

// Get Auth Headers
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

document.addEventListener("DOMContentLoaded", function () {
  let cursor = null;
  let hasMore = true;
  const pageSize = 5;

  // DOM 요소
  const couponList = document.getElementById("couponList");
  const loadingMessage = document.getElementById("loadingMessage");
  const emptyMessage = document.getElementById("emptyMessage");
  const couponCountText = document.getElementById("couponCountText");
  const couponCodeInput = document.getElementById("couponCodeInput");
  const submitCouponBtn = document.getElementById("submitCouponBtn");

  let totalCount = 0;

  // 전체 쿠폰 개수 조회
  function loadCouponCount() {
    return fetch("/api/coupon-issues/count", {
      method: "GET",
      headers: getAuthHeaders(),
    })
      .then((response) => {
        if (!response.ok) {
          if (response.status === 401) {
            alert("로그인이 필요합니다.");
            window.location.href = "/login";
            return Promise.reject("Unauthorized");
          }
          return response.json().then((err) => Promise.reject(err));
        }
        return response.json();
      })
      .then((result) => {
        if (result.success && result.data !== undefined) {
          totalCount = result.data;
          updateCouponCount();
        }
      })
      .catch((error) => {
        console.error("Error loading coupon count:", error);
        // 개수 조회 실패해도 계속 진행
      });
  }

  // 로딩 인디케이터 추가/제거
  function showLoadingIndicator() {
    const existingLoader = couponList.querySelector(".scroll-loading");
    if (!existingLoader) {
      const loader = document.createElement("div");
      loader.className = "scroll-loading";
      loader.innerHTML = '<p style="text-align: center; padding: 20px; color: var(--text-muted);">불러오는 중...</p>';
      couponList.appendChild(loader);
    }
  }

  function hideLoadingIndicator() {
    const loader = couponList.querySelector(".scroll-loading");
    if (loader) {
      loader.remove();
    }
  }

  // 쿠폰 목록 로드
  function loadCoupons(reset = false) {
    if (reset) {
      cursor = null;
      hasMore = true;
      totalCount = 0;
      couponList.innerHTML = `
        <div class="loading-message" id="loadingMessage">
          <p>쿠폰을 불러오는 중...</p>
        </div>
      `;
      loadingMessage.style.display = "block";
      emptyMessage.style.display = "none";
    }

    if (!hasMore && !reset) {
      return Promise.resolve();
    }

    if (!reset) {
      showLoadingIndicator();
    }

    const params = new URLSearchParams();
    params.append("size", pageSize);
    if (cursor) {
      params.append("cursor", cursor);
    }

    return fetch(`/api/coupon-issues?${params.toString()}`, {
      method: "GET",
      headers: getAuthHeaders(),
    })
      .then((response) => {
        if (!response.ok) {
          if (response.status === 401) {
            alert("로그인이 필요합니다.");
            window.location.href = "/login";
            return Promise.reject("Unauthorized");
          }
          return response.json().then((err) => Promise.reject(err));
        }
        return response.json();
      })
      .then((result) => {
        if (result.success && result.data) {
          const data = result.data;
          const items = data.items || [];
          
          console.log("API Response:", {
            itemsCount: items.length,
            hasNext: data.hasNext,
            nextCursor: data.nextCursor,
            reset: reset
          });
          
          if (reset) {
            displayCoupons(items);
          } else {
            appendCoupons(items);
          }

          cursor = data.nextCursor || null;
          // hasNext가 명시적으로 false가 아니면 items.length로 판단
          if (data.hasNext !== undefined) {
            hasMore = data.hasNext;
          } else {
            // hasNext가 없으면 items.length가 pageSize 이상이면 더 있을 가능성이 있음
            hasMore = items.length >= pageSize;
          }
          
          console.log("State after load:", {
            cursor: cursor,
            hasMore: hasMore,
            totalCards: document.querySelectorAll(".coupon-card").length,
            totalCount: totalCount
          });
          
          // totalCount가 설정되어 있으면 그대로 사용, 없으면 업데이트
          if (totalCount === 0) {
            updateCouponCount();
          }
          
          loadingMessage.style.display = "none";
          hideLoadingIndicator();
          
          if (items.length === 0 && reset) {
            emptyMessage.style.display = "block";
          }
          
          // 더 이상 데이터가 없으면 로딩 인디케이터 제거
          if (!hasMore) {
            hideLoadingIndicator();
            console.log("No more data to load");
          }
        } else {
          throw new Error(result.message || "데이터를 불러올 수 없습니다.");
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        loadingMessage.style.display = "none";
        hideLoadingIndicator();
        if (reset) {
          emptyMessage.style.display = "block";
          emptyMessage.innerHTML = `
            <p>쿠폰을 불러오는 중 오류가 발생했습니다.</p>
          `;
        }
        throw error;
      });
  }

  // 쿠폰 목록 표시
  function displayCoupons(coupons) {
    if (coupons.length === 0) {
      couponList.innerHTML = "";
      emptyMessage.style.display = "block";
      return;
    }

    emptyMessage.style.display = "none";
    couponList.innerHTML = coupons
      .map((coupon) => createCouponCard(coupon))
      .join("");
  }

  // 쿠폰 추가 (무한 스크롤용)
  function appendCoupons(coupons) {
    if (coupons.length === 0) {
      return;
    }

    const existingCards = couponList.querySelectorAll(".coupon-card");
    if (existingCards.length === 0) {
      displayCoupons(coupons);
      return;
    }

    const newCards = coupons
      .map((coupon) => createCouponCard(coupon))
      .join("");
    couponList.insertAdjacentHTML("beforeend", newCards);
  }

  // 쿠폰 카드 생성
  function createCouponCard(coupon) {
    const startDate = formatDate(coupon.startAt);
    const endDate = formatDate(coupon.endAt);
    const benefitTypeText = getBenefitTypeText(coupon.benefitType);
    const channelText = getChannelText(coupon.couponChannel);
    const benefitValue = coupon.benefitValue || 0;
    const couponIssueId = coupon.id;

    const benefitDisplay = `${benefitTypeText} ${benefitValue}${getBenefitUnit(coupon.benefitType)}`;
    const channelIcon = getChannelIcon(coupon.couponChannel);
    
    return `
      <div class="coupon-card" data-coupon-id="${couponIssueId}">
        <div class="coupon-card-inner">
          <div class="coupon-header">
            <div class="coupon-title-section">
              <h3 class="coupon-name">${escapeHtml(coupon.name || "-")}</h3>
              <div class="coupon-channel-badge">
                ${channelIcon}
                <span class="coupon-channel-text">${channelText}</span>
              </div>
            </div>
            <div class="coupon-actions">
              <svg 
                class="coupon-star-icon"
                width="20" 
                height="20" 
                viewBox="0 0 16 15" 
                fill="none" 
                xmlns="http://www.w3.org/2000/svg"
              >
                <path 
                  d="M8 0L9.79811 5.52786L15.6085 5.52786L10.9052 8.94427L12.7033 14.4721L8 11.0557L3.29667 14.4721L5.09478 8.94427L0.391456 5.52786L6.20189 5.52786L8 0Z" 
                  fill="#FFD700"
                />
              </svg>
              <button 
                class="coupon-delete-btn" 
                type="button"
                data-coupon-issue-id="${couponIssueId}"
                onclick="deleteCoupon(this)"
              >
                삭제
              </button>
            </div>
          </div>
          
          <div class="coupon-benefit-section">
            <div class="coupon-benefit-value">${benefitValue}${getBenefitUnit(coupon.benefitType)}</div>
            <div class="coupon-benefit-type">${benefitTypeText}</div>
          </div>

          <div class="coupon-details">
            <div class="coupon-detail-item">
              <div class="coupon-detail-icon">📅</div>
              <div class="coupon-detail-content">
                <div class="coupon-detail-label">유효기간</div>
                <div class="coupon-detail-value">${startDate} ~ ${endDate}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  // 채널 아이콘 가져오기
  function getChannelIcon(channel) {
    const iconMap = {
      EVENT: "🎉",
      SYSTEM: "⚙️",
      PARTNER: "🤝",
      ADMIN: "👤",
      PROMOTION: "📢",
    };
    return iconMap[channel] || "🎫";
  }

  // 쿠폰 개수 업데이트
  function updateCouponCount() {
    // totalCount가 설정되어 있으면 그것을 사용, 없으면 실제 로드된 카드 개수 사용
    if (totalCount > 0) {
      couponCountText.textContent = `보유 쿠폰 ${totalCount}장`;
    } else {
      const couponCards = document.querySelectorAll(".coupon-card");
      const actualCount = couponCards.length;
      couponCountText.textContent = `보유 쿠폰 ${actualCount}장`;
    }
  }

  // 날짜 포맷
  function formatDate(dateString) {
    if (!dateString) return "-";
    try {
      const date = new Date(dateString);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, "0");
      const day = String(date.getDate()).padStart(2, "0");
      return `${year}.${month}.${day}`;
    } catch (e) {
      return "-";
    }
  }

  // 혜택 타입 텍스트 변환
  function getBenefitTypeText(type) {
    const typeMap = {
      DISCOUNT: "할인",
      EXPERIENCE: "체험",
      VOUCHER: "교환권",
    };
    return typeMap[type] || type || "-";
  }

  // 혜택 단위
  function getBenefitUnit(type) {
    if (type === "DISCOUNT") {
      return "%";
    }
    return "";
  }

  // 채널 텍스트 변환
  function getChannelText(channel) {
    const channelMap = {
      EVENT: "이벤트",
      SYSTEM: "시스템",
      PARTNER: "파트너",
      ADMIN: "관리자",
      PROMOTION: "프로모션",
    };
    return channelMap[channel] || channel || "-";
  }

  // HTML 이스케이프
  function escapeHtml(text) {
    if (!text) return "-";
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
  }

  // 쿠폰 코드 입력
  if (submitCouponBtn) {
    submitCouponBtn.addEventListener("click", function () {
      const code = couponCodeInput.value.trim();
      if (!code) {
        alert("쿠폰 코드를 입력해주세요.");
        return;
      }

      submitCouponBtn.disabled = true;
      submitCouponBtn.textContent = "처리 중...";

      fetch("/api/coupon-issues/redeem", {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify({ code: code }),
      })
        .then((response) => {
          return response.json().then((data) => {
            if (!response.ok) {
              return Promise.reject(data);
            }
            return data;
          });
        })
        .then((result) => {
          if (result.success) {
            alert("쿠폰이 발급되었습니다.");
            couponCodeInput.value = "";
            // 전체 개수 다시 조회 후 목록 새로고침
            Promise.all([
              loadCouponCount(),
              loadCoupons(true)
            ]).catch((error) => {
              console.error("Error reloading after redeem:", error);
            });
          } else {
            alert(result.message || "쿠폰 발급에 실패했습니다.");
          }
        })
        .catch((error) => {
          console.error("Error:", error);
          let errorMessage = "쿠폰 발급 중 오류가 발생했습니다.";
          if (error.message) {
            errorMessage = error.message;
          } else if (error.data && error.data.message) {
            errorMessage = error.data.message;
          }
          alert(errorMessage);
        })
        .finally(() => {
          submitCouponBtn.disabled = false;
          submitCouponBtn.textContent = "입력하기";
        });
    });
  }

  // Enter 키로 쿠폰 코드 입력
  if (couponCodeInput) {
    couponCodeInput.addEventListener("keypress", function (e) {
      if (e.key === "Enter") {
        submitCouponBtn.click();
      }
    });
  }

  // 무한 스크롤
  let isLoading = false;
  let scrollTimeout = null;
  const couponContent = document.querySelector(".coupon-content");

  function handleScroll() {
    if (isLoading || !hasMore) {
      if (!hasMore) {
        hideLoadingIndicator();
      }
      return;
    }

    // 스크롤 컨테이너 확인
    const scrollContainer = couponContent || window;
    const scrollTop = scrollContainer === window 
      ? window.pageYOffset || document.documentElement.scrollTop
      : scrollContainer.scrollTop;
    const scrollHeight = scrollContainer === window
      ? document.documentElement.scrollHeight
      : scrollContainer.scrollHeight;
    const clientHeight = scrollContainer === window
      ? window.innerHeight
      : scrollContainer.clientHeight;

    const distanceFromBottom = scrollHeight - (scrollTop + clientHeight);
    
    // 스크롤이 하단 200px 이내에 도달하면 로드
    if (distanceFromBottom <= 200) {
      console.log("Scroll triggered - loading more:", {
        scrollTop,
        scrollHeight,
        clientHeight,
        distanceFromBottom,
        hasMore,
        isLoading
      });
      
      isLoading = true;
      loadCoupons(false)
        .then(() => {
          isLoading = false;
        })
        .catch((error) => {
          console.error("Error loading more coupons:", error);
          isLoading = false;
        });
    }
  }

  // 스크롤 이벤트 최적화 (throttle)
  const scrollHandler = function () {
    if (scrollTimeout) {
      clearTimeout(scrollTimeout);
    }
    scrollTimeout = setTimeout(handleScroll, 150);
  };

  // coupon-content에 스크롤 이벤트 바인딩
  if (couponContent) {
    couponContent.addEventListener("scroll", scrollHandler);
  } else {
    window.addEventListener("scroll", scrollHandler);
  }

  // 초기 로드: 먼저 전체 개수 조회 후 쿠폰 목록 로드
  Promise.all([
    loadCouponCount(),
    loadCoupons(true)
  ]).catch((error) => {
    console.error("Error during initial load:", error);
  });
});

// 쿠폰 삭제 함수 (전역)
window.deleteCoupon = function (btn) {
  const couponIssueId = btn.getAttribute("data-coupon-issue-id");
  if (!couponIssueId) {
    alert("쿠폰 정보를 찾을 수 없습니다.");
    return;
  }

  if (!confirm("정말로 이 쿠폰을 삭제하시겠습니까?")) {
    return;
  }

  btn.disabled = true;
  btn.textContent = "삭제 중...";

  fetch(`/api/coupon-issues/${couponIssueId}`, {
    method: "DELETE",
    headers: getAuthHeaders(),
  })
    .then((response) => {
      return response.json().then((data) => {
        if (!response.ok) {
          return Promise.reject(data);
        }
        return data;
      });
    })
    .then((result) => {
      if (result.success) {
        alert("쿠폰이 삭제되었습니다.");
        // 쿠폰 카드 제거
        const couponCard = btn.closest(".coupon-card");
        if (couponCard) {
          couponCard.remove();
        }
        // 전체 개수 다시 조회
        loadCouponCount();
        
        // 카드가 없으면 빈 메시지 표시
        const remainingCards = document.querySelectorAll(".coupon-card");
        if (remainingCards.length === 0) {
          document.getElementById("emptyMessage").style.display = "block";
        }
      } else {
        alert(result.message || "삭제에 실패했습니다.");
        btn.disabled = false;
        btn.textContent = "삭제하기";
      }
    })
    .catch((error) => {
      console.error("Error:", error);
      let errorMessage = "삭제 중 오류가 발생했습니다.";
      if (error.message) {
        errorMessage = error.message;
      } else if (error.data && error.data.message) {
        errorMessage = error.data.message;
      }
      alert(errorMessage);
      btn.disabled = false;
      btn.textContent = "삭제하기";
    });
};

