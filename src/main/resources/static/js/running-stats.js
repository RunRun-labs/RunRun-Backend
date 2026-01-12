document.addEventListener("DOMContentLoaded", function () {
  let tierStatsChart = null;
  let typeBreakdownChart = null;
  let hourlyStatsChart = null;
  let tierPaceChart = null;
  let weeklyTrendChart = null;
  let monthlyTrendChart = null;
  let paceDistributionChart = null;
  let courseStatsChart = null;

  // 러닝 통계 로드
  function loadRunningStats() {
    fetch("/api/admin/running/stats", {
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
          
          // 기본 통계 표시
          displayBasicStats(stats);
          
          // 차트 업데이트
          updateTierStatsChart(stats.tierStats || []);
          updateTypeBreakdownChart(stats.typeBreakdown || []);
          updateHourlyStatsChart(stats.hourlyStats || []);
          updateTierPaceChart(stats.tierPaceStats || []);
          updateWeeklyTrendChart(stats.weeklyTrend || []);
          updateMonthlyTrendChart(stats.monthlyTrend || []);
          updatePaceDistributionChart(stats.paceDistribution || []);
          updateCourseStatsChart(stats.courseStats || []);
        } else {
          throw new Error(result.message || "통계 데이터를 불러올 수 없습니다.");
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        alert("통계 데이터를 불러오는 중 오류가 발생했습니다.");
      });
  }

  // 기본 통계 표시
  function displayBasicStats(stats) {
    document.getElementById("matchSuccessRate").textContent = (stats.matchSuccessRate || 0).toFixed(2) + "%";
    document.getElementById("matchCancelRate").textContent = (stats.matchCancelRate || 0).toFixed(2) + "%";
    document.getElementById("runningDropoutRate").textContent = (stats.runningDropoutRate || 0).toFixed(2) + "%";
    document.getElementById("avgMatchDuration").textContent = formatSeconds(stats.avgMatchDuration || 0);
    document.getElementById("avgRunningDistance").textContent = formatDistance(stats.avgRunningDistance || 0);
    document.getElementById("matchParticipationRate").textContent = (stats.matchParticipationRate || 0).toFixed(2) + "%";
    
    document.getElementById("totalRunningDistance").textContent = formatDistance(stats.totalRunningDistance || 0);
    document.getElementById("totalRunningCount").textContent = formatNumber(stats.totalRunningCount || 0);
    document.getElementById("totalRunningTime").textContent = formatTime(stats.totalRunningTime || 0);
    document.getElementById("maxConsecutiveDays").textContent = formatNumber(stats.maxConsecutiveDays || 0) + "일";
  }

  // 티어별 러닝량 차트
  function updateTierStatsChart(tierStats) {
    const ctx = document.getElementById("tierStatsChart").getContext("2d");

    if (tierStatsChart) {
      tierStatsChart.destroy();
    }

    if (!tierStats || tierStats.length === 0) {
      return;
    }

    const labels = tierStats.map(t => getTierLabel(t.tier));
    const counts = tierStats.map(t => t.totalCount || 0);
    const distances = tierStats.map(t => parseFloat(t.totalDistance || 0));

    tierStatsChart = new Chart(ctx, {
      type: "bar",
      data: {
        labels: labels,
        datasets: [
          {
            label: "러닝 횟수",
            data: counts,
            backgroundColor: "#baff29",
            yAxisID: "y",
          },
          {
            label: "총 거리 (km)",
            data: distances,
            backgroundColor: "#4d81e7",
            yAxisID: "y1",
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
            type: "linear",
            display: true,
            position: "left",
            ticks: {
              color: "#87888c",
            },
            grid: {
              color: "rgba(255, 255, 255, 0.1)",
            },
          },
          y1: {
            type: "linear",
            display: true,
            position: "right",
            ticks: {
              color: "#87888c",
            },
            grid: {
              drawOnChartArea: false,
            },
          },
        },
      },
    });
  }

  // 러닝 타입별 분포 차트
  function updateTypeBreakdownChart(typeBreakdown) {
    const ctx = document.getElementById("typeBreakdownChart").getContext("2d");

    if (typeBreakdownChart) {
      typeBreakdownChart.destroy();
    }

    if (!typeBreakdown || typeBreakdown.length === 0) {
      return;
    }

    const labels = typeBreakdown.map(t => {
      const typeLabel = getRunningTypeLabel(t.type);
      const percentage = (t.percentage || 0).toFixed(1);
      return `${typeLabel} (${percentage}%)`;
    });
    const data = typeBreakdown.map(t => t.count || 0);
    const backgroundColor = [
      "#baff29",
      "#4d81e7",
      "#87888c",
      "#ff6b6b",
      "#ffd93d",
    ];

    typeBreakdownChart = new Chart(ctx, {
      type: "doughnut",
      data: {
        labels: labels,
        datasets: [
          {
            data: data,
            backgroundColor: backgroundColor.slice(0, data.length),
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
          tooltip: {
            callbacks: {
              label: function(context) {
                const label = context.label || '';
                const value = context.parsed || 0;
                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0;
                return `${label}: ${value.toLocaleString()}건 (${percentage}%)`;
              }
            }
          },
        },
      },
    });
  }

  // 시간대별 러닝량 차트
  function updateHourlyStatsChart(hourlyStats) {
    const ctx = document.getElementById("hourlyStatsChart").getContext("2d");

    if (hourlyStatsChart) {
      hourlyStatsChart.destroy();
    }

    if (!hourlyStats || hourlyStats.length === 0) {
      return;
    }

    const labels = hourlyStats.map(h => h.hour + "시");
    const counts = hourlyStats.map(h => h.count || 0);
    const distances = hourlyStats.map(h => parseFloat(h.totalDistance || 0));

    hourlyStatsChart = new Chart(ctx, {
      type: "line",
      data: {
        labels: labels,
        datasets: [
          {
            label: "러닝 횟수",
            data: counts,
            borderColor: "#baff29",
            backgroundColor: "rgba(186, 255, 41, 0.1)",
            tension: 0.4,
            yAxisID: "y",
          },
          {
            label: "총 거리 (km)",
            data: distances,
            borderColor: "#4d81e7",
            backgroundColor: "rgba(77, 129, 231, 0.1)",
            tension: 0.4,
            yAxisID: "y1",
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
            type: "linear",
            display: true,
            position: "left",
            ticks: {
              color: "#87888c",
            },
            grid: {
              color: "rgba(255, 255, 255, 0.1)",
            },
          },
          y1: {
            type: "linear",
            display: true,
            position: "right",
            ticks: {
              color: "#87888c",
            },
            grid: {
              drawOnChartArea: false,
            },
          },
        },
      },
    });
  }

  // 티어별 평균 페이스 차트
  function updateTierPaceChart(tierPaceStats) {
    const ctx = document.getElementById("tierPaceChart").getContext("2d");

    if (tierPaceChart) {
      tierPaceChart.destroy();
    }

    if (!tierPaceStats || tierPaceStats.length === 0) {
      return;
    }

    const labels = tierPaceStats.map(t => getTierLabel(t.tier));
    const paces = tierPaceStats.map(t => parseFloat(t.avgPace || 0));

    tierPaceChart = new Chart(ctx, {
      type: "bar",
      data: {
        labels: labels,
        datasets: [
          {
            label: "평균 페이스 (분/km)",
            data: paces,
            backgroundColor: "#baff29",
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

  // 주별 추이 차트
  function updateWeeklyTrendChart(weeklyTrend) {
    const ctx = document.getElementById("weeklyTrendChart").getContext("2d");

    if (weeklyTrendChart) {
      weeklyTrendChart.destroy();
    }

    if (!weeklyTrend || weeklyTrend.length === 0) {
      return;
    }

    const labels = weeklyTrend.map(w => {
      const date = new Date(w.weekStart);
      return `${date.getMonth() + 1}/${date.getDate()}`;
    });
    const counts = weeklyTrend.map(w => w.count || 0);
    const distances = weeklyTrend.map(w => parseFloat(w.totalDistance || 0));
    const times = weeklyTrend.map(w => w.totalTime || 0);

    weeklyTrendChart = new Chart(ctx, {
      type: "line",
      data: {
        labels: labels,
        datasets: [
          {
            label: "러닝 횟수",
            data: counts,
            borderColor: "#baff29",
            backgroundColor: "rgba(186, 255, 41, 0.1)",
            tension: 0.4,
            yAxisID: "y",
          },
          {
            label: "총 거리 (km)",
            data: distances,
            borderColor: "#4d81e7",
            backgroundColor: "rgba(77, 129, 231, 0.1)",
            tension: 0.4,
            yAxisID: "y1",
          },
          {
            label: "총 시간 (초)",
            data: times,
            borderColor: "#ff6b6b",
            backgroundColor: "rgba(255, 107, 107, 0.1)",
            tension: 0.4,
            yAxisID: "y2",
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
            type: "linear",
            display: true,
            position: "left",
            ticks: {
              color: "#87888c",
            },
            grid: {
              color: "rgba(255, 255, 255, 0.1)",
            },
          },
          y1: {
            type: "linear",
            display: true,
            position: "right",
            ticks: {
              color: "#87888c",
            },
            grid: {
              drawOnChartArea: false,
            },
          },
          y2: {
            type: "linear",
            display: false,
          },
        },
      },
    });
  }

  // 월별 추이 차트
  function updateMonthlyTrendChart(monthlyTrend) {
    const ctx = document.getElementById("monthlyTrendChart").getContext("2d");

    if (monthlyTrendChart) {
      monthlyTrendChart.destroy();
    }

    if (!monthlyTrend || monthlyTrend.length === 0) {
      return;
    }

    const labels = monthlyTrend.map(m => {
      if (typeof m.month === 'string') {
        // "2026-01" 형식인 경우
        const parts = m.month.split('-');
        return `${parts[0]}년 ${parseInt(parts[1])}월`;
      } else if (m.month && m.month.year && m.month.monthValue) {
        // 객체 형식인 경우
        return `${m.month.year}년 ${m.month.monthValue}월`;
      }
      return m.month || "";
    });
    const counts = monthlyTrend.map(m => m.count || 0);
    const distances = monthlyTrend.map(m => parseFloat(m.totalDistance || 0));
    const times = monthlyTrend.map(m => m.totalTime || 0);

    monthlyTrendChart = new Chart(ctx, {
      type: "line",
      data: {
        labels: labels,
        datasets: [
          {
            label: "러닝 횟수",
            data: counts,
            borderColor: "#baff29",
            backgroundColor: "rgba(186, 255, 41, 0.1)",
            tension: 0.4,
            yAxisID: "y",
          },
          {
            label: "총 거리 (km)",
            data: distances,
            borderColor: "#4d81e7",
            backgroundColor: "rgba(77, 129, 231, 0.1)",
            tension: 0.4,
            yAxisID: "y1",
          },
          {
            label: "총 시간 (초)",
            data: times,
            borderColor: "#ff6b6b",
            backgroundColor: "rgba(255, 107, 107, 0.1)",
            tension: 0.4,
            yAxisID: "y2",
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
            type: "linear",
            display: true,
            position: "left",
            ticks: {
              color: "#87888c",
            },
            grid: {
              color: "rgba(255, 255, 255, 0.1)",
            },
          },
          y1: {
            type: "linear",
            display: true,
            position: "right",
            ticks: {
              color: "#87888c",
            },
            grid: {
              drawOnChartArea: false,
            },
          },
          y2: {
            type: "linear",
            display: false,
          },
        },
      },
    });
  }

  // 페이스 분포 차트
  function updatePaceDistributionChart(paceDistribution) {
    const ctx = document.getElementById("paceDistributionChart").getContext("2d");

    if (paceDistributionChart) {
      paceDistributionChart.destroy();
    }

    if (!paceDistribution || paceDistribution.length === 0) {
      return;
    }

    const labels = paceDistribution.map(p => p.paceRange || "");
    const counts = paceDistribution.map(p => p.userCount || 0);

    paceDistributionChart = new Chart(ctx, {
      type: "bar",
      data: {
        labels: labels,
        datasets: [
          {
            label: "유저 수",
            data: counts,
            backgroundColor: "#baff29",
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

  // 코스별 통계 차트
  function updateCourseStatsChart(courseStats) {
    const ctx = document.getElementById("courseStatsChart").getContext("2d");

    if (courseStatsChart) {
      courseStatsChart.destroy();
    }

    if (!courseStats || courseStats.length === 0) {
      return;
    }

    // 상위 10개만 표시 (너무 많으면 차트가 복잡해짐)
    const topCourses = courseStats.slice(0, 10);
    const labels = topCourses.map(c => c.courseName || "코스명 없음");
    const counts = topCourses.map(c => c.usageCount || 0);

    courseStatsChart = new Chart(ctx, {
      type: "bar",
      data: {
        labels: labels,
        datasets: [
          {
            label: "사용 횟수",
            data: counts,
            backgroundColor: "#baff29",
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
              maxRotation: 45,
              minRotation: 45,
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

  // 헬퍼 함수들
  function getTierLabel(tier) {
    const tierMap = {
      거북이: "거북이",
      토끼: "토끼",
      사슴: "사슴",
      표범: "표범",
      호랑이: "호랑이",
      장산범: "장산범",
    };
    return tierMap[tier] || tier || "-";
  }

  function getRunningTypeLabel(type) {
    const typeMap = {
      SOLO: "솔로",
      OFFLINE: "오프라인",
      ONLINEBATTLE: "온라인 배틀",
      GHOST: "고스트",
    };
    return typeMap[type] || type || "-";
  }

  function formatDistance(km) {
    if (!km && km !== 0) return "0km";
    return parseFloat(km).toFixed(2) + "km";
  }

  function formatTime(seconds) {
    if (!seconds && seconds !== 0) return "0초";
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    if (hours > 0) {
      return `${hours}시간 ${minutes}분 ${secs}초`;
    } else if (minutes > 0) {
      return `${minutes}분 ${secs}초`;
    } else {
      return `${secs}초`;
    }
  }

  function formatSeconds(seconds) {
    if (!seconds && seconds !== 0) return "0초";
    return parseFloat(seconds).toFixed(1) + "초";
  }

  function formatNumber(num) {
    if (!num && num !== 0) return "0";
    return num.toLocaleString();
  }

  // 초기 로드
  loadRunningStats();
});

