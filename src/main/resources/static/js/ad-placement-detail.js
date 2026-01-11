document.addEventListener("DOMContentLoaded", function () {
  let placementId = null;
  let currentPage = 0;
  let totalPages = 1;
  const pageSize = 30;

  // URL에서 배치 ID 가져오기
  const pathParts = window.location.pathname.split("/");
  const detailIndex = pathParts.indexOf("detail");
  if (detailIndex !== -1 && pathParts[detailIndex + 1]) {
    placementId = pathParts[detailIndex + 1];
  }

  if (!placementId) {
    alert("배치 ID를 찾을 수 없습니다.");
    window.location.href = "/admin/ad-placement/inquiry";
    return;
  }

  // 필터 요소들
  const fromDate = document.getElementById("fromDate");
  const toDate = document.getElementById("toDate");
  const sortFilter = document.getElementById("sortFilter");
  const dirFilter = document.getElementById("dirFilter");
  const applyStatsFilterBtn = document.getElementById("applyStatsFilterBtn");
  const statsTableBody = document.getElementById("statsTableBody");
  const paginationContainer = document.getElementById("paginationContainer");

  // 배치 기본 정보 로드
  async function loadPlacementDetail() {
    try {
      const response = await fetch(`/api/admin/ad-placements?page=0&size=1000`, {
        method: "GET",
        headers: getAuthHeaders(),
      });
      const result = await response.json();
      if (result.success && result.data && result.data.content) {
        const placement = result.data.content.find(
          (p) => p.placementId === parseInt(placementId)
        );

        if (!placement) {
          throw new Error("배치를 찾을 수 없습니다.");
        }

        // 기본 정보 표시
        document.getElementById("slotName").textContent = placement.slotName || "-";
        document.getElementById("slotType").textContent = getSlotTypeText(placement.slotType);
        document.getElementById("adName").textContent = placement.adName || "-";
        document.getElementById("weight").textContent = placement.weight || 0;
        document.getElementById("startAt").textContent = formatDateTime(placement.startAt);
        document.getElementById("endAt").textContent = formatDateTime(placement.endAt);
        document.getElementById("totalImpressions").textContent = placement.totalImpressions || 0;
        document.getElementById("totalClicks").textContent = placement.totalClicks || 0;
        document.getElementById("isActive").textContent = placement.isActive ? "활성" : "비활성";
      } else {
        throw new Error(result.message || "배치 데이터를 불러올 수 없습니다.");
      }
    } catch (error) {
      console.error("Error:", error);
      alert("배치 데이터를 불러오는 중 오류가 발생했습니다.");
    }
  }

  // 슬롯 타입 텍스트 변환
  function getSlotTypeText(slotType) {
    const typeMap = {
      FEED_LIST_ITEM: "피드 리스트",
      RUN_END_BANNER: "러닝 종료 배너",
      HOME_TOP_BANNER: "홈 상단 배너",
      COUPON_LIST_BANNER: "쿠폰 리스트 배너",
    };
    return typeMap[slotType] || slotType || "-";
  }

  // 날짜/시간 포맷
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

  // 날짜 포맷
  function formatDate(dateString) {
    if (!dateString) return "";
    try {
      const date = new Date(dateString);
      return date.toISOString().split("T")[0];
    } catch (e) {
      return "";
    }
  }

  // 데일리 스텟 로드
  function loadDailyStats(page = 0) {
    currentPage = page;

    const params = new URLSearchParams();

    if (fromDate.value) {
      params.append("from", fromDate.value);
    }

    if (toDate.value) {
      params.append("to", toDate.value);
    }

    if (sortFilter.value) {
      params.append("sort", sortFilter.value);
    }

    if (dirFilter.value) {
      params.append("dir", dirFilter.value);
    }

    params.append("page", page);
    params.append("size", pageSize);

    fetch(`/api/admin/ad-placements/${placementId}/daily-stats?${params.toString()}`, {
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
          const data = result.data;
          displayDailyStats(data.content || []);
          updatePagination(data.page || 0, data.totalPages || 1);
          currentPage = data.page || 0;
          totalPages = data.totalPages || 1;
        } else {
          throw new Error(result.message || "통계 데이터를 불러올 수 없습니다.");
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        statsTableBody.innerHTML = `
          <tr>
            <td colspan="4" class="empty-message">
              통계 데이터를 불러오는 중 오류가 발생했습니다.
            </td>
          </tr>
        `;
      });
  }

  // 데일리 스텟 표시
  function displayDailyStats(stats) {
    if (stats.length === 0) {
      statsTableBody.innerHTML = `
        <tr>
          <td colspan="4" class="empty-message">
            조회된 통계가 없습니다.
          </td>
        </tr>
      `;
      return;
    }

    statsTableBody.innerHTML = stats
      .map((stat) => {
        const date = formatDate(stat.statDate);
        const ctr = stat.impressions > 0 
          ? ((stat.clicks / stat.impressions) * 100).toFixed(2) 
          : "0.00";

        return `
          <tr>
            <td>${date}</td>
            <td>${stat.impressions || 0}</td>
            <td>${stat.clicks || 0}</td>
            <td>${ctr}%</td>
          </tr>
        `;
      })
      .join("");
  }

  // 페이지네이션 업데이트
  function updatePagination(page, total) {
    currentPage = page;
    totalPages = total;

    if (total <= 1) {
      paginationContainer.innerHTML = "";
      return;
    }

    let paginationHTML = `
      <div class="pagination">
        <button 
          type="button" 
          class="pagination-btn pagination-prev" 
          ${page === 0 ? "disabled" : ""}
          onclick="goToPage(${page - 1})"
        >
          이전
        </button>
        <div class="pagination-numbers">
    `;

    const maxVisible = 10;
    let startPage = Math.max(0, page - Math.floor(maxVisible / 2));
    let endPage = Math.min(total - 1, startPage + maxVisible - 1);

    if (endPage - startPage < maxVisible - 1) {
      startPage = Math.max(0, endPage - maxVisible + 1);
    }

    if (startPage > 0) {
      paginationHTML += `
        <button type="button" class="pagination-number" onclick="goToPage(0)">
          <span>1</span>
        </button>
      `;
      if (startPage > 1) {
        paginationHTML += `<span class="pagination-ellipsis">...</span>`;
      }
    }

    for (let i = startPage; i <= endPage; i++) {
      paginationHTML += `
        <button 
          type="button" 
          class="pagination-number ${i === page ? "active" : ""}" 
          onclick="goToPage(${i})"
        >
          <span>${i + 1}</span>
        </button>
      `;
    }

    if (endPage < total - 1) {
      if (endPage < total - 2) {
        paginationHTML += `<span class="pagination-ellipsis">...</span>`;
      }
      paginationHTML += `
        <button type="button" class="pagination-number" onclick="goToPage(${total - 1})">
          <span>${total}</span>
        </button>
      `;
    }

    paginationHTML += `
        </div>
        <button 
          type="button" 
          class="pagination-btn pagination-next" 
          ${page >= total - 1 ? "disabled" : ""}
          onclick="goToPage(${page + 1})"
        >
          다음
        </button>
      </div>
    `;

    paginationContainer.innerHTML = paginationHTML;
  }

  // 페이지 이동
  window.goToPage = function(page) {
    if (page >= 0 && page < totalPages) {
      loadDailyStats(page);
    }
  };

  // 필터 적용
  applyStatsFilterBtn.addEventListener("click", function () {
    loadDailyStats(0);
  });

  // 초기 로드
  loadPlacementDetail();
  loadDailyStats(0);
});

