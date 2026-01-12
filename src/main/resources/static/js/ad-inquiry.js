document.addEventListener("DOMContentLoaded", function () {
  let currentPage = 0;
  let totalPages = 1;
  const pageSize = 20;

  const keywordFilter = document.getElementById("keywordFilter");
  const applyFilterBtn = document.getElementById("applyFilterBtn");
  const resetFilterBtn = document.getElementById("resetFilterBtn");

  const tableBody = document.getElementById("adTableBody");
  const paginationContainer = document.getElementById("paginationContainer");

  let filters = {
    keyword: "",
  };

  // 광고 목록 로드
  function loadAds(page = 0) {
    currentPage = page;

    const params = new URLSearchParams();

    if (filters.keyword) {
      params.append("keyword", filters.keyword);
    }

    params.append("page", page);
    params.append("size", pageSize);

    fetch(`/api/admin/ads?${params.toString()}`, {
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
          displayAds(data.content || []);
          updatePagination(data.number || 0, data.totalPages || 1);
          currentPage = data.number || 0;
          totalPages = data.totalPages || 1;
        } else {
          throw new Error(result.message || "데이터를 불러올 수 없습니다.");
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        tableBody.innerHTML = `
          <tr>
            <td colspan="3" class="empty-message">
              데이터를 불러오는 중 오류가 발생했습니다.
            </td>
          </tr>
        `;
      });
  }

  // 광고 목록 표시
  function displayAds(ads) {
    if (ads.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="3" class="empty-message">
            조회된 광고가 없습니다.
          </td>
        </tr>
      `;
      return;
    }

    const startIndex = currentPage * pageSize;
    tableBody.innerHTML = ads
      .map((ad, index) => {
        const rowNumber = startIndex + index + 1;
        const redirectUrl = ad.redirectUrl || "-";

        return `
          <tr>
            <td><a href="/admin/ad/detail/${ad.id}" class="table-link">${rowNumber}</a></td>
            <td>${escapeHtml(ad.name || "-")}</td>
            <td style="max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${escapeHtml(redirectUrl)}">${escapeHtml(redirectUrl)}</td>
          </tr>
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
      loadAds(page);
    }
  };

  // 필터 적용
  applyFilterBtn.addEventListener("click", function () {
    filters.keyword = keywordFilter.value.trim();
    loadAds(0);
  });

  // 필터 초기화
  resetFilterBtn.addEventListener("click", function () {
    keywordFilter.value = "";
    filters.keyword = "";
    loadAds(0);
  });

  // 초기 로드
  loadAds(0);
});
