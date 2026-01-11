document.addEventListener("DOMContentLoaded", function () {
  let currentPage = 0;
  let totalPages = 1;
  const pageSize = 20;

  const isActiveCheckboxes = document.querySelectorAll(
    'input.filter-checkbox[value="true"], input.filter-checkbox[value="false"]'
  );
  const slotTypeCheckboxes = document.querySelectorAll(
    'input.filter-checkbox[value="FEED_LIST_ITEM"], input.filter-checkbox[value="RUN_END_BANNER"], input.filter-checkbox[value="HOME_TOP_BANNER"], input.filter-checkbox[value="COUPON_LIST_BANNER"]'
  );
  const keywordFilter = document.getElementById("keywordFilter");
  const applyFilterBtn = document.getElementById("applyFilterBtn");
  const resetFilterBtn = document.getElementById("resetFilterBtn");

  const tableBody = document.getElementById("adPlacementTableBody");
  const paginationContainer = document.getElementById("paginationContainer");

  let filters = {
    isActive: null,
    slotType: null,
    keyword: "",
  };

  // 광고 배치 목록 로드
  function loadPlacements(page = 0) {
    currentPage = page;

    const params = new URLSearchParams();

    if (filters.isActive !== null) {
      params.append("isActive", filters.isActive);
    }

    if (filters.slotType) {
      params.append("slotType", filters.slotType);
    }

    if (filters.keyword) {
      params.append("keyword", filters.keyword);
    }

    params.append("page", page);
    params.append("size", pageSize);

    fetch(`/api/admin/ad-placements?${params.toString()}`, {
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
          displayPlacements(data.content || []);
          updatePagination(data.page || 0, data.totalPages || 1);
          currentPage = data.page || 0;
          totalPages = data.totalPages || 1;
        } else {
          throw new Error(result.message || "데이터를 불러올 수 없습니다.");
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        tableBody.innerHTML = `
          <tr>
            <td colspan="9" class="empty-message">
              데이터를 불러오는 중 오류가 발생했습니다.
            </td>
          </tr>
        `;
      });
  }

  // 광고 배치 목록 표시
  function displayPlacements(placements) {
    if (placements.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="9" class="empty-message">
            조회된 광고 배치가 없습니다.
          </td>
        </tr>
      `;
      return;
    }

    const startIndex = currentPage * pageSize;
    tableBody.innerHTML = placements
      .map((placement, index) => {
        const rowNumber = startIndex + index + 1;
        const statusClass = placement.isActive ? "active" : "inactive";
        const statusText = placement.isActive ? "활성" : "비활성";
        const startAt = formatDateTime(placement.startAt);
        const endAt = formatDateTime(placement.endAt);

        return `
          <tr>
            <td><a href="/admin/ad-placement/detail/${placement.placementId}" class="table-link">${rowNumber}</a></td>
            <td>${escapeHtml(placement.slotName || "-")}</td>
            <td>${escapeHtml(placement.adName || "-")}</td>
            <td>${placement.weight || 0}</td>
            <td>${startAt}</td>
            <td>${endAt}</td>
            <td>${placement.totalImpressions || 0}</td>
            <td>${placement.totalClicks || 0}</td>
            <td><span class="status-badge ${statusClass}">${statusText}</span></td>
          </tr>
        `;
      })
      .join("");
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

  // HTML 이스케이프
  function escapeHtml(text) {
    if (!text) return "-";
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
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
      loadPlacements(page);
    }
  };

  // 필터 적용
  applyFilterBtn.addEventListener("click", function () {
    // 활성 상태 필터
    const checkedIsActive = Array.from(isActiveCheckboxes)
      .filter((cb) => cb.checked)
      .map((cb) => cb.value === "true");
    filters.isActive = checkedIsActive.length === 1 ? checkedIsActive[0] : null;

    // 슬롯 타입 필터
    const checkedSlotTypes = Array.from(slotTypeCheckboxes)
      .filter((cb) => cb.checked)
      .map((cb) => cb.value);
    filters.slotType = checkedSlotTypes.length === 1 ? checkedSlotTypes[0] : null;

    // 검색어
    filters.keyword = keywordFilter.value.trim();

    loadPlacements(0);
  });

  // 필터 초기화
  resetFilterBtn.addEventListener("click", function () {
    isActiveCheckboxes.forEach((cb) => (cb.checked = false));
    slotTypeCheckboxes.forEach((cb) => (cb.checked = false));
    keywordFilter.value = "";

    filters = {
      isActive: null,
      slotType: null,
      keyword: "",
    };

    loadPlacements(0);
  });

  // 초기 로드
  loadPlacements(0);
});
