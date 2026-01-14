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
  const couponId = 22; // 하드코딩된 쿠폰 ID
  const couponCard = document.getElementById("couponCard");
  const couponLoading = document.getElementById("couponLoading");
  const downloadBtn = document.getElementById("downloadBtn");
  const eventInfoList = document.getElementById("eventInfoList");

  // 쿠폰 정보 로드
  function loadCoupon() {
    fetch(`/api/admin/coupons/public/${couponId}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
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
          const coupon = result.data;
          displayCoupon(coupon);
          updateEventInfo(coupon);
          downloadBtn.disabled = false;
        } else {
          throw new Error(result.message || "쿠폰 정보를 불러올 수 없습니다.");
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        couponLoading.innerHTML = `
          <p style="color: #ff3b30;">쿠폰 정보를 불러오는 중 오류가 발생했습니다.</p>
        `;
      });
  }

  // 쿠폰 표시
  function displayCoupon(coupon) {
    const startDate = formatDate(coupon.startAt);
    const endDate = formatDate(coupon.endAt);
    const benefitTypeText = getBenefitTypeText(coupon.benefitType);
    const benefitValue = coupon.benefitValue || 0;
    const benefitUnit = getBenefitUnit(coupon.benefitType);
    const description = coupon.description || "";

    couponLoading.style.display = "none";
    couponCard.innerHTML = `
      <div class="coupon-content">
        <div class="coupon-header-section">
          <h2 class="coupon-name-large">${escapeHtml(coupon.name || "-")}</h2>
          ${description ? `<p class="coupon-description">${escapeHtml(description)}</p>` : ""}
        </div>
        
        <div class="coupon-benefit-large">
          <div class="benefit-value-large">${benefitValue}${benefitUnit}</div>
          <div class="benefit-type-large">${benefitTypeText}</div>
        </div>

        <div class="coupon-details-section">
          <div class="coupon-detail-row">
            <span class="detail-label">유효기간</span>
            <span class="detail-value">${startDate} ~ ${endDate}</span>
          </div>
          <div class="coupon-detail-row">
            <span class="detail-label">발급 채널</span>
            <span class="detail-value">${getChannelText(coupon.channel)}</span>
          </div>
          ${coupon.quantity ? `
          <div class="coupon-detail-row">
            <span class="detail-label">남은 수량</span>
            <span class="detail-value">${coupon.quantity - coupon.issuedCount}장</span>
          </div>
          ` : ""}
        </div>
      </div>
    `;
  }

  // 이벤트 안내 업데이트
  function updateEventInfo(coupon) {
    const startDate = formatDate(coupon.startAt);
    const endDate = formatDate(coupon.endAt);
    
    eventInfoList.innerHTML = `
      <li>이벤트 기간: ${startDate} ~ ${endDate}</li>
      <li>쿠폰은 발급 후 내 쿠폰함에서 확인할 수 있습니다</li>
      <li>쿠폰은 유효기간 내에만 사용 가능합니다</li>
      <li>중복 발급은 불가능합니다</li>
    `;
  }

  // 쿠폰 다운로드
  if (downloadBtn) {
    downloadBtn.addEventListener("click", function () {
      if (downloadBtn.disabled) return;

      downloadBtn.disabled = true;
      downloadBtn.innerHTML = `
        <span class="download-icon">⏳</span>
        <span class="download-text">발급 중...</span>
      `;

      fetch(`/api/coupon-issues/${couponId}/download`, {
        method: "POST",
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
            downloadBtn.innerHTML = `
              <span class="download-icon">✅</span>
              <span class="download-text">발급 완료!</span>
            `;
            downloadBtn.style.background = "linear-gradient(135deg, #4caf50 0%, #2e7d32 100%)";
            
            setTimeout(() => {
              alert("쿠폰이 발급되었습니다!\n내 쿠폰함에서 확인하세요.");
              window.location.href = "/coupon/my";
            }, 1000);
          } else {
            throw new Error(result.message || "쿠폰 발급에 실패했습니다.");
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
          
          downloadBtn.disabled = false;
          downloadBtn.innerHTML = `
            <span class="download-icon">⬇️</span>
            <span class="download-text">쿠폰 받기</span>
          `;
        });
    });
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
    if (type === "RATE_DISCOUNT") {
      return "%";
    } else if (type === "FIXED_DISCOUNT") {
      return "원";
    } else if (type === "EXPERIENCE") {
      return "일";
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

  // 초기 로드
  loadCoupon();
});

