// 전역 변수로 adId 선언 (HTML의 onclick에서 접근 가능하도록)
let adId = null;

document.addEventListener("DOMContentLoaded", function () {
  let dailyChart = null;
  let slotChart = null;

  // URL에서 광고 ID 가져오기
  const pathParts = window.location.pathname.split("/");
  const detailIndex = pathParts.indexOf("detail");
  if (detailIndex !== -1 && pathParts[detailIndex + 1]) {
    adId = pathParts[detailIndex + 1];
  }

  if (!adId) {
    alert("광고 ID를 찾을 수 없습니다.");
    window.location.href = "/admin/ad/inquiry";
    return;
  }

  // 통계 필터
  const rangeFilter = document.getElementById("rangeFilter");

  // 광고 기본 정보 로드
  function loadAdDetail() {
    fetch(`/api/admin/ads/${adId}`, {
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
          const ad = result.data;
          
          document.getElementById("adName").textContent = ad.name || "-";
          document.getElementById("adRedirectUrl").textContent = ad.redirectUrl || "-";
          if (ad.imageUrl) {
            document.getElementById("adImage").src = ad.imageUrl;
          }

          // Placement 통계
          document.getElementById("totalPlacementCount").textContent = ad.totalPlacementCount || 0;
          document.getElementById("totalPlacementImpressions").textContent = ad.totalPlacementImpressions || 0;
          document.getElementById("totalPlacementClicks").textContent = ad.totalPlacementClicks || 0;
          document.getElementById("totalPlacementCtr").textContent = (ad.totalPlacementCtr || 0).toFixed(2) + "%";
        } else {
          throw new Error(result.message || "광고 데이터를 불러올 수 없습니다.");
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        alert("광고 데이터를 불러오는 중 오류가 발생했습니다.");
      });
  }

  // 통계 로드
  function loadStats(range = "D30") {
    fetch(`/api/admin/ads/${adId}/stats?range=${range}`, {
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
          document.getElementById("totalImpressions").textContent = stats.impressions || 0;
          document.getElementById("totalClicks").textContent = stats.clicks || 0;
          document.getElementById("totalCtr").textContent = (stats.ctr || 0).toFixed(2) + "%";

          // 날짜별 추이 차트
          updateDailyChart(stats.dailySeries || []);

          // 슬롯별 집계 차트
          updateSlotChart(stats.slotBreakdown || []);
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
  function updateDailyChart(series) {
    const ctx = document.getElementById("dailyChart").getContext("2d");

    if (dailyChart) {
      dailyChart.destroy();
    }

    const dates = series.map((s) => {
      const date = new Date(s.date);
      return `${date.getMonth() + 1}/${date.getDate()}`;
    });
    const impressions = series.map((s) => s.impressions || 0);
    const clicks = series.map((s) => s.clicks || 0);

    dailyChart = new Chart(ctx, {
      type: "line",
      data: {
        labels: dates,
        datasets: [
          {
            label: "노출 수",
            data: impressions,
            borderColor: "#baff29",
            backgroundColor: "rgba(186, 255, 41, 0.1)",
            tension: 0.4,
          },
          {
            label: "클릭 수",
            data: clicks,
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

  // 슬롯별 집계 차트 업데이트
  function updateSlotChart(breakdown) {
    const ctx = document.getElementById("slotChart").getContext("2d");

    if (slotChart) {
      slotChart.destroy();
    }

    const slotTypes = breakdown.map((b) => {
      const typeMap = {
        FEED_LIST_ITEM: "피드 리스트",
        RUN_END_BANNER: "러닝 종료 배너",
        HOME_TOP_BANNER: "홈 상단 배너",
        COUPON_LIST_BANNER: "쿠폰 리스트 배너",
      };
      return typeMap[b.slotType] || b.slotType;
    });
    const impressions = breakdown.map((b) => b.impressions || 0);
    const clicks = breakdown.map((b) => b.clicks || 0);

    slotChart = new Chart(ctx, {
      type: "bar",
      data: {
        labels: slotTypes,
        datasets: [
          {
            label: "노출 수",
            data: impressions,
            backgroundColor: "#baff29",
          },
          {
            label: "클릭 수",
            data: clicks,
            backgroundColor: "#4d81e7",
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

  // 기간 필터 변경
  rangeFilter.addEventListener("change", function () {
    loadStats(rangeFilter.value);
  });

  // 광고 삭제
  window.deleteAd = function() {
    if (!confirm("정말 삭제하시겠습니까?")) {
      return;
    }

    fetch(`/api/admin/ads/${adId}`, {
      method: "DELETE",
      headers: getAuthHeaders(),
    })
      .then((response) => {
        if (!response.ok) {
          return response.json().then((err) => Promise.reject(err));
        }
        return response.json();
      })
      .then((result) => {
        if (result.success) {
          alert("광고가 삭제되었습니다.");
          window.location.href = "/admin/ad/inquiry";
        } else {
          throw new Error(result.message || "삭제 실패");
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        alert(error.message || "삭제 중 오류가 발생했습니다.");
      });
  };

  // 초기 로드
  loadAdDetail();
  loadStats("D30");
});

