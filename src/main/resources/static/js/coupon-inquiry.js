document.addEventListener("DOMContentLoaded", function () {
  let currentPage = 0;
  let totalPages = 1;
  const pageSize = 10;

  // 필터 요소들
  const statusCheckboxes = document.querySelectorAll(
    'input.filter-checkbox[value="ACTIVE"], input.filter-checkbox[value="EXPIRED"], input.filter-checkbox[value="SOLD_OUT"], input.filter-checkbox[value="DRAFT"]'
  );
  const codeTypeCheckboxes = document.querySelectorAll(
    'input.filter-checkbox[value="SINGLE"], input.filter-checkbox[value="MULTI"]'
  );
  const channelCheckboxes = document.querySelectorAll(
    'input.filter-checkbox[value="EVENT"], input.filter-checkbox[value="SYSTEM"], input.filter-checkbox[value="PARTNER"], input.filter-checkbox[value="ADMIN"], input.filter-checkbox[value="PROMOTION"]'
  );
  const sortFilter = document.getElementById("sortFilter");
  const keywordFilter = document.getElementById("keywordFilter");
  const applyFilterBtn = document.getElementById("applyFilterBtn");
  const resetFilterBtn = document.getElementById("resetFilterBtn");

  // 테이블 바디
  const tableBody = document.getElementById("couponTableBody");
  const paginationContainer = document.getElementById("paginationContainer");

  // 필터 상태
  let filters = {
    statuses: [],
    codeTypes: [],
    channels: [],
    keyword: "",
    sort: "createdAt,DESC",
  };

  // 쿠폰 목록 로드
  function loadCoupons(page = 0) {
    currentPage = page;

    // 필터 파라미터 구성
    const params = new URLSearchParams();

    if (filters.statuses.length > 0) {
      filters.statuses.forEach((status) => {
        params.append("statuses", status);
      });
    }

    if (filters.codeTypes.length > 0) {
      filters.codeTypes.forEach((codeType) => {
        params.append("codeTypes", codeType);
      });
    }

    if (filters.channels.length > 0) {
      filters.channels.forEach((channel) => {
        params.append("channels", channel);
      });
    }

    if (filters.keyword) {
      params.append("keyword", filters.keyword);
    }

    // 정렬 파라미터 (Spring Data Pageable 형식)
    const [sortField, sortDirection] = filters.sort.split(",");
    params.append("sort", `${sortField},${sortDirection}`);

    // 페이지네이션 파라미터
    params.append("page", page);
    params.append("size", pageSize);

    // API 호출
    fetch(`/api/admin/coupons?${params.toString()}`, {
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
          displayCoupons(data.items || []);
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
            <td colspan="11" class="empty-message">
              데이터를 불러오는 중 오류가 발생했습니다.
            </td>
          </tr>
        `;
      });
  }

  // 쿠폰 목록 표시
  function displayCoupons(coupons) {
    if (coupons.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="11" class="empty-message">
            조회된 쿠폰이 없습니다.
          </td>
        </tr>
      `;
      return;
    }

    const startIndex = currentPage * pageSize;
    tableBody.innerHTML = coupons
      .map((coupon, index) => {
        const rowNumber = startIndex + index + 1;
        const statusClass = coupon.status
          ? coupon.status.toLowerCase().replace("_", "_")
          : "";
        const statusText = getStatusText(coupon.status);
        const createdAt = formatDate(coupon.createdAt);
        const startAt = formatDate(coupon.startAt);
        const endAt = formatDate(coupon.endAt);
        const benefitText = formatBenefit(
          coupon.benefitType,
          coupon.benefitValue
        );

        return `
          <tr>
            <td>${rowNumber}</td>
            <td>${escapeHtml(coupon.name || "-")}</td>
            <td>${coupon.quantity ?? "-"}</td>
            <td>${coupon.issuedCount ?? 0}</td>
            <td>${coupon.codeType || "-"}</td>
            <td>${coupon.channel || "-"}</td>
            <td>${benefitText}</td>
            <td><span class="status-badge ${statusClass}">${statusText}</span></td>
            <td>${startAt}</td>
            <td>${endAt}</td>
            <td>${createdAt}</td>
          </tr>
        `;
      })
      .join("");
  }

  // 상태 텍스트 변환
  function getStatusText(status) {
    const statusMap = {
      ACTIVE: "활성",
      EXPIRED: "만료",
      SOLD_OUT: "품절",
      DRAFT: "초안",
      DELETED: "삭제",
    };
    return statusMap[status] || status || "-";
  }

  // 혜택 포맷
  function formatBenefit(benefitType, benefitValue) {
    if (!benefitType || benefitValue === null || benefitValue === undefined) return "-";

    let unit = "";
    if (benefitType === "RATE_DISCOUNT") {
      unit = "%";
    } else if (benefitType === "FIXED_DISCOUNT") {
      unit = "원";
    } else if (benefitType === "EXPERIENCE") {
      unit = "일";
    }

    return `${benefitValue}${unit}`;
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
        <button type="button" class="pagination-number" onclick="goToPage(${
          total - 1
        })">
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

  // 페이지 이동 함수 (전역)
  window.goToPage = function (page) {
    if (page >= 0 && page < totalPages) {
      loadCoupons(page);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  // 필터 적용
  function applyFilters() {
    filters.statuses = Array.from(statusCheckboxes)
      .filter((cb) => cb.checked)
      .map((cb) => cb.value);
    filters.codeTypes = Array.from(codeTypeCheckboxes)
      .filter((cb) => cb.checked)
      .map((cb) => cb.value);
    filters.channels = Array.from(channelCheckboxes)
      .filter((cb) => cb.checked)
      .map((cb) => cb.value);
    filters.keyword = keywordFilter.value.trim();
    filters.sort = sortFilter.value;

    currentPage = 0;
    loadCoupons(0);
  }

  // 필터 초기화
  function resetFilters() {
    statusCheckboxes.forEach((cb) => (cb.checked = false));
    codeTypeCheckboxes.forEach((cb) => (cb.checked = false));
    channelCheckboxes.forEach((cb) => (cb.checked = false));
    keywordFilter.value = "";
    sortFilter.value = "createdAt,DESC";

    filters = {
      statuses: [],
      codeTypes: [],
      channels: [],
      keyword: "",
      sort: "createdAt,DESC",
    };

    currentPage = 0;
    loadCoupons(0);
  }

  // 이벤트 리스너
  if (applyFilterBtn) {
    applyFilterBtn.addEventListener("click", applyFilters);
  }

  if (resetFilterBtn) {
    resetFilterBtn.addEventListener("click", resetFilters);
  }

  // Enter 키로 필터 적용
  if (keywordFilter) {
    keywordFilter.addEventListener("keypress", function (e) {
      if (e.key === "Enter") {
        applyFilters();
      }
    });
  }

  // 초기 로드
  loadCoupons(0);
});
