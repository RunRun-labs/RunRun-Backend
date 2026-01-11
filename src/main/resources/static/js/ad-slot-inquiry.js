document.addEventListener("DOMContentLoaded", function () {
  let currentPage = 0;
  let totalPages = 1;
  const pageSize = 20;

  // 필터 요소들
  const statusCheckboxes = document.querySelectorAll(
    'input.filter-checkbox[value="ENABLED"], input.filter-checkbox[value="DISABLED"]'
  );
  const slotTypeCheckboxes = document.querySelectorAll(
    'input.filter-checkbox[value="FEED_LIST_ITEM"], input.filter-checkbox[value="RUN_END_BANNER"], input.filter-checkbox[value="HOME_TOP_BANNER"], input.filter-checkbox[value="COUPON_LIST_BANNER"]'
  );
  const keywordFilter = document.getElementById("keywordFilter");
  const applyFilterBtn = document.getElementById("applyFilterBtn");
  const resetFilterBtn = document.getElementById("resetFilterBtn");

  // 테이블 바디
  const tableBody = document.getElementById("adSlotTableBody");
  const paginationContainer = document.getElementById("paginationContainer");

  // 필터 상태
  let filters = {
    status: null,
    slotType: null,
    keyword: "",
  };

  // 광고 슬롯 목록 로드
  function loadAdSlots(page = 0) {
    currentPage = page;

    // 필터 파라미터 구성
    const params = new URLSearchParams();

    if (filters.status) {
      params.append("status", filters.status);
    }

    if (filters.slotType) {
      params.append("slotType", filters.slotType);
    }

    if (filters.keyword) {
      params.append("keyword", filters.keyword);
    }

    // 페이지네이션 파라미터
    params.append("page", page);
    params.append("size", pageSize);

    // API 호출
    fetch(`/api/admin/ad-slot?${params.toString()}`, {
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
          displayAdSlots(data.content || []);
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
            <td colspan="7" class="empty-message">
              데이터를 불러오는 중 오류가 발생했습니다.
            </td>
          </tr>
        `;
      });
  }

  // 광고 슬롯 목록 표시
  function displayAdSlots(slots) {
    if (slots.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="7" class="empty-message">
            조회된 광고 슬롯이 없습니다.
          </td>
        </tr>
      `;
      return;
    }

    const startIndex = currentPage * pageSize;
    tableBody.innerHTML = slots
      .map((slot, index) => {
        const rowNumber = startIndex + index + 1;
        const statusClass = slot.status
          ? slot.status.toLowerCase()
          : "";
        const statusText = getStatusText(slot.status);
        const slotTypeText = getSlotTypeText(slot.slotType);
        const dailyLimitText = slot.dailyLimit === 0 || slot.dailyLimit === null ? "무제한" : slot.dailyLimit;
        const allowPremiumText = slot.allowPremium ? "예" : "아니오";

        return `
          <tr>
            <td>${rowNumber}</td>
            <td>${escapeHtml(slot.name || "-")}</td>
            <td>${slotTypeText}</td>
            <td>${dailyLimitText}</td>
            <td>${allowPremiumText}</td>
            <td><span class="status-badge ${statusClass}">${statusText}</span></td>
            <td>
              <div style="display: flex; gap: 8px; justify-content: center;">
                <button 
                  type="button" 
                  class="action-btn edit-btn" 
                  onclick="window.location.href='/admin/ad-slot/update/${slot.slotId}'"
                >
                  수정
                </button>
                <label class="toggle-label-wrapper">
                  <input 
                    type="checkbox" 
                    class="toggle-input" 
                    ${slot.status === "ENABLED" ? "checked" : ""}
                    onchange="toggleStatus(${slot.slotId}, this.checked)"
                  />
                  <span class="toggle-label">${slot.status === "ENABLED" ? "활성" : "비활성"}</span>
                </label>
              </div>
            </td>
          </tr>
        `;
      })
      .join("");
  }

  // 상태 텍스트 변환
  function getStatusText(status) {
    const statusMap = {
      ENABLED: "활성",
      DISABLED: "비활성",
    };
    return statusMap[status] || status || "-";
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

  // HTML 이스케이프
  function escapeHtml(text) {
    if (!text) return "-";
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
  }

  // 상태 토글
  window.toggleStatus = function(slotId, enabled) {
    const endpoint = enabled ? "enable" : "disable";
    const message = enabled ? "활성화" : "비활성화";

    fetch(`/api/admin/ad-slot/${slotId}/${endpoint}`, {
      method: "PATCH",
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
          alert(`${message} 성공`);
          loadAdSlots(currentPage);
        } else {
          throw new Error(result.message || `${message} 실패`);
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        alert(error.message || `${message} 중 오류가 발생했습니다.`);
        loadAdSlots(currentPage);
      });
  };

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

    // 페이지 번호 생성 (최대 10개 표시)
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
      loadAdSlots(page);
    }
  };

  // 필터 적용
  applyFilterBtn.addEventListener("click", function () {
    // 상태 필터
    const checkedStatuses = Array.from(statusCheckboxes)
      .filter((cb) => cb.checked)
      .map((cb) => cb.value);
    filters.status = checkedStatuses.length === 1 ? checkedStatuses[0] : null;

    // 슬롯 타입 필터
    const checkedSlotTypes = Array.from(slotTypeCheckboxes)
      .filter((cb) => cb.checked)
      .map((cb) => cb.value);
    filters.slotType = checkedSlotTypes.length === 1 ? checkedSlotTypes[0] : null;

    // 검색어
    filters.keyword = keywordFilter.value.trim();

    loadAdSlots(0);
  });

  // 필터 초기화
  resetFilterBtn.addEventListener("click", function () {
    statusCheckboxes.forEach((cb) => (cb.checked = false));
    slotTypeCheckboxes.forEach((cb) => (cb.checked = false));
    keywordFilter.value = "";

    filters = {
      status: null,
      slotType: null,
      keyword: "",
    };

    loadAdSlots(0);
  });

  // 초기 로드
  loadAdSlots(0);
});
