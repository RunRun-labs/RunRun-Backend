document.addEventListener("DOMContentLoaded", function () {
  let couponId = null;
  let dailyTrendChart = null;
  let statusChart = null;

  // URL에서 쿠폰 ID 가져오기
  const pathParts = window.location.pathname.split("/");
  const detailIndex = pathParts.indexOf("detail");
  if (detailIndex !== -1 && pathParts[detailIndex + 1]) {
    couponId = pathParts[detailIndex + 1];
  }

  if (!couponId) {
    alert("쿠폰 ID를 찾을 수 없습니다.");
    window.location.href = "/admin/coupon/inquiry";
    return;
  }

  const rangeFilter = document.getElementById("rangeFilter");

  // 쿠폰 기본 정보 로드
  function loadCouponDetail() {
    fetch(`/api/admin/coupons/${couponId}`, {
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
          const coupon = result.data;
          
          document.getElementById("couponName").textContent = coupon.name || "-";
          document.getElementById("couponCode").textContent = coupon.code || "-";
          document.getElementById("quantity").textContent = coupon.quantity === 0 ? "무제한" : (coupon.quantity || "-");
          document.getElementById("issuedCount").textContent = coupon.issuedCount || 0;
          document.getElementById("benefitType").textContent = getBenefitTypeText(coupon.benefitType);
          document.getElementById("benefitValue").textContent = formatBenefitValue(coupon.benefitType, coupon.benefitValue);
          document.getElementById("channel").textContent = getChannelText(coupon.channel);
          document.getElementById("status").textContent = getStatusText(coupon.status);
          document.getElementById("description").textContent = coupon.description || "-";
          document.getElementById("startAt").textContent = formatDateTime(coupon.startAt);
          document.getElementById("endAt").textContent = formatDateTime(coupon.endAt);
        } else {
          throw new Error(result.message || "쿠폰 데이터를 불러올 수 없습니다.");
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        alert("쿠폰 데이터를 불러오는 중 오류가 발생했습니다.");
      });
  }

  // 통계 로드
  function loadStats(range = "D30") {
    fetch(`/api/admin/coupons/${couponId}/stats?range=${range}`, {
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
          
          // 통계 요약
          document.getElementById("totalIssued").textContent = stats.totalIssued || 0;
          document.getElementById("totalUsed").textContent = stats.totalUsed || 0;
          document.getElementById("usageRate").textContent = (stats.usageRate || 0).toFixed(2) + "%";

          // 날짜별 추이 차트
          updateDailyTrendChart(stats.dailyTrend || []);

          // 상태별 분포 차트
          updateStatusChart(stats.statusBreakdown);
        } else {
          throw new Error(result.message || "통계 데이터를 불러올 수 없습니다.");
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        alert("통계 데이터를 불러오는 중 오류가 발생했습니다.");
      });
  }

  // 날짜별 추이 차트 업데이트
  function updateDailyTrendChart(trend) {
    const ctx = document.getElementById("dailyTrendChart").getContext("2d");

    if (dailyTrendChart) {
      dailyTrendChart.destroy();
    }

    const dates = trend.map((t) => {
      const date = new Date(t.date);
      return `${date.getMonth() + 1}/${date.getDate()}`;
    });
    const issued = trend.map((t) => t.issued || 0);
    const used = trend.map((t) => t.used || 0);

    dailyTrendChart = new Chart(ctx, {
      type: "line",
      data: {
        labels: dates,
        datasets: [
          {
            label: "발급 수",
            data: issued,
            borderColor: "#baff29",
            backgroundColor: "rgba(186, 255, 41, 0.1)",
            tension: 0.4,
          },
          {
            label: "사용 수",
            data: used,
            borderColor: "#4d81e7",
            backgroundColor: "rgba(77, 129, 231, 0.1)",
            tension: 0.4,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            labels: {
              color: "#ffffff",
            },
          },
        },
        scales: {
          x: {
            ticks: {
              color: "#87888c",
            },
            grid: {
              color: "rgba(255, 255, 255, 0.1)",
            },
          },
          y: {
            ticks: {
              color: "#87888c",
            },
            grid: {
              color: "rgba(255, 255, 255, 0.1)",
            },
          },
        },
      },
    });
  }

  // 상태별 분포 차트 업데이트
  function updateStatusChart(breakdown) {
    const ctx = document.getElementById("statusChart").getContext("2d");

    if (statusChart) {
      statusChart.destroy();
    }

    const available = breakdown?.available || 0;
    const used = breakdown?.used || 0;
    const expired = breakdown?.expired || 0;

    statusChart = new Chart(ctx, {
      type: "doughnut",
      data: {
        labels: ["사용 가능", "사용됨", "만료됨"],
        datasets: [
          {
            data: [available, used, expired],
            backgroundColor: [
              "#baff29",
              "#4d81e7",
              "#87888c",
            ],
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: "bottom",
            labels: {
              color: "#ffffff",
            },
          },
        },
      },
    });
  }

  // 헬퍼 함수들
  function getBenefitTypeText(type) {
    const typeMap = {
      FIXED_DISCOUNT: "정액 할인",
      RATE_DISCOUNT: "정률 할인",
      EXPERIENCE: "체험",
      VOUCHER: "교환권",
    };
    return typeMap[type] || type || "-";
  }

  function formatBenefitValue(type, value) {
    if (!value) return "-";
    let unit = "";
    if (type === "FIXED_DISCOUNT") unit = "원";
    else if (type === "RATE_DISCOUNT") unit = "%";
    else if (type === "EXPERIENCE") unit = "일";
    return `${value}${unit}`;
  }

  function getChannelText(channel) {
    const channelMap = {
      EVENT: "이벤트",
      SYSTEM: "시스템",
      PARTNER: "제휴사",
      ADMIN: "관리자",
      PROMOTION: "프로모션",
    };
    return channelMap[channel] || channel || "-";
  }

  function getStatusText(status) {
    const statusMap = {
      ACTIVE: "활성",
      EXPIRED: "만료",
      SOLD_OUT: "품절",
      DRAFT: "초안",
      DELETED: "삭제됨",
    };
    return statusMap[status] || status || "-";
  }

  function formatDateTime(dateString) {
    if (!dateString) return "-";
    try {
      const date = new Date(dateString);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, "0");
      const day = String(date.getDate()).padStart(2, "0");
      const hours = String(date.getHours()).padStart(2, "0");
      const minutes = String(date.getMinutes()).padStart(2, "0");
      return `${year}.${month}.${day} ${hours}:${minutes}`;
    } catch (e) {
      return "-";
    }
  }

  // 기간 필터 변경
  rangeFilter.addEventListener("change", function () {
    loadStats(rangeFilter.value);
  });

  // 초기 로드
  loadCouponDetail();
  loadStats("D30");
});

