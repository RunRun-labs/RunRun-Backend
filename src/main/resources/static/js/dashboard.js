document.addEventListener("DOMContentLoaded", function () {
  // 대시보드 통계 로드
  function loadDashboardStats() {
    fetch("/api/admin/dashboard/stats", {
      method: "GET",
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
        if (result.success && result.data) {
          const stats = result.data;
          
          // 오늘의 광고
          document.getElementById("todayAdClicks").textContent = stats.todayAdClicks || 0;
          document.getElementById("activeAdPercentage").textContent = (stats.activeAdPercentage || 0).toFixed(2) + "%";
          document.getElementById("todayAdImpressions").textContent = stats.todayAdImpressions || 0;
          document.getElementById("newMembershipUsers").textContent = stats.newMembershipUsers || 0;
          
          // 오늘의 쿠폰
          document.getElementById("todayCouponIssued").textContent = stats.todayCouponIssued || 0;
          document.getElementById("todayCouponUsageRate").textContent = (stats.todayCouponUsageRate || 0).toFixed(2) + "%";
          document.getElementById("todayCouponUsed").textContent = stats.todayCouponUsed || 0;
          document.getElementById("newUsers").textContent = stats.newUsers || 0;
          
          // Top 5 광고
          displayTopAds(stats.topAds || []);
          
          // Top 5 쿠폰
          displayTopCoupons(stats.topCoupons || []);
        } else {
          throw new Error(result.message || "대시보드 데이터를 불러올 수 없습니다.");
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        alert("대시보드 데이터를 불러오는 중 오류가 발생했습니다.");
      });
  }

  // Top 5 광고 표시
  function displayTopAds(topAds) {
    const listElement = document.getElementById("topAdsList");
    
    if (topAds.length === 0) {
      listElement.innerHTML = '<div class="empty-message">데이터가 없습니다.</div>';
      return;
    }
    
    listElement.innerHTML = topAds
      .map((ad, index) => {
        return `
          <div class="top-item">
            <div class="top-rank">${index + 1}</div>
            <div class="top-info">
              <div class="top-name">${escapeHtml(ad.adName || "-")}</div>
              <div class="top-value">클릭수: ${ad.clicks}</div>
            </div>
          </div>
        `;
      })
      .join("");
  }

  // Top 5 쿠폰 표시
  function displayTopCoupons(topCoupons) {
    const listElement = document.getElementById("topCouponsList");
    
    if (topCoupons.length === 0) {
      listElement.innerHTML = '<div class="empty-message">데이터가 없습니다.</div>';
      return;
    }
    
    listElement.innerHTML = topCoupons
      .map((coupon, index) => {
        return `
          <div class="top-item">
            <div class="top-rank">${index + 1}</div>
            <div class="top-info">
              <div class="top-name">${escapeHtml(coupon.couponName || "-")}</div>
              <div class="top-value">발급 수: ${coupon.issuedCount}</div>
            </div>
          </div>
        `;
      })
      .join("");
  }

  // HTML 이스케이프
  function escapeHtml(text) {
    if (!text) return "-";
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
  }

  // 초기 로드
  loadDashboardStats();
});

