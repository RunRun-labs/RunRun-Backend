// ad-inquiry.js 기반 - 포인트 상품 목록 조회

document.addEventListener("DOMContentLoaded", function () {
    let currentPage = 0;
    let totalPages = 1;
    const pageSize = 10;
    let selectedProducts = new Set();

    const keywordFilter = document.getElementById("keywordFilter");
    const applyFilterBtn = document.getElementById("applyFilterBtn");
    const resetFilterBtn = document.getElementById("resetFilterBtn");
    const sortFilter = document.getElementById("sortFilter");
    const isAvailableTrue = document.getElementById("isAvailableTrue");
    const isAvailableFalse = document.getElementById("isAvailableFalse");
    const selectAll = document.getElementById("selectAll");
    const btnDelete = document.getElementById("btnDelete");

    const tableBody = document.getElementById("productTableBody");
    const paginationContainer = document.getElementById("paginationContainer");

    let filters = {
        keyword: "",
        isAvailable: "",
        sort: "createdAt,DESC",
    };

    // 포인트 상품 목록 로드
    function loadProducts(page = 0) {
        currentPage = page;

        const params = new URLSearchParams();

        if (filters.keyword) {
            params.append("keyword", filters.keyword);
        }

        if (filters.isAvailable) {
            params.append("isAvailable", filters.isAvailable);
        }

        params.append("page", page);
        params.append("size", pageSize);
        params.append("sort", filters.sort);

        fetch(`/api/admin/points/products?${params.toString()}`, {
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
                    displayProducts(data.items || []);
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
            <td colspan="8" class="empty-message">
              데이터를 불러오는 중 오류가 발생했습니다.
            </td>
          </tr>
        `;
            });
    }

    // 포인트 상품 목록 표시
    function displayProducts(products) {
        // 전체 선택 체크박스 초기화
        if (selectAll) {
            selectAll.checked = false;
        }
        selectedProducts.clear();

        if (products.length === 0) {
            tableBody.innerHTML = `
        <tr>
          <td colspan="8" class="empty-message">
            등록된 포인트 상품이 없습니다.
          </td>
        </tr>
      `;
            return;
        }

        const startIndex = currentPage * pageSize;
        tableBody.innerHTML = products
            .map((product, index) => {
                const rowNumber = startIndex + index + 1;
                const imageUrl =
                    product.productImageUrl && product.productImageUrl.startsWith("http")
                        ? product.productImageUrl
                        : product.productImageUrl
                            ? `https://runrun-uploads-bucket.s3.mx-central-1.amazonaws.com/${product.productImageUrl}`
                            : "/images/no-image.png";

                return `
          <tr>
            <td>
              <input type="checkbox" class="product-checkbox" data-id="${product.productId}" />
            </td>
            <td>${rowNumber}</td>
            <td>
              <img src="${imageUrl}" alt="${escapeHtml(product.productName)}" class="product-image" />
            </td>
            <td>${escapeHtml(product.productName || "-")}</td>
            <td>${product.requiredPoint ? product.requiredPoint.toLocaleString() + " P" : "-"}</td>
            <td>
              <span class="status-badge ${product.isAvailable ? "status-active" : "status-inactive"}">
                ${product.isAvailable ? "판매중" : "판매중지"}
              </span>
            </td>
            <td>${formatDate(product.createdAt)}</td>
            <td>
              <div class="action-buttons">
                <button class="btn-edit" onclick="goToUpdate(${product.productId})">수정</button>
              </div>
            </td>
          </tr>
        `;
            })
            .join("");

        // 체크박스 이벤트 리스너 추가
        document.querySelectorAll(".product-checkbox").forEach((checkbox) => {
            checkbox.addEventListener("change", function () {
                const id = parseInt(this.dataset.id);
                if (this.checked) {
                    selectedProducts.add(id);
                } else {
                    selectedProducts.delete(id);
                    if (selectAll) selectAll.checked = false;
                }
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
    window.goToPage = function (page) {
        if (page >= 0 && page < totalPages) {
            loadProducts(page);
            window.scrollTo({top: 0, behavior: "smooth"});
        }
    };

    // 수정 페이지로 이동
    window.goToUpdate = function (productId) {
        window.location.href = `/admin/points/products/update/${productId}`;
    };

    // 필터 적용
    if (applyFilterBtn) {
        applyFilterBtn.addEventListener("click", function () {
            filters.keyword = keywordFilter.value.trim();

            // 판매 상태 필터
            const availableFilters = [];
            if (isAvailableTrue && isAvailableTrue.checked) {
                availableFilters.push("true");
            }
            if (isAvailableFalse && isAvailableFalse.checked) {
                availableFilters.push("false");
            }

            // 필터가 선택되지 않았거나 모두 선택되었으면 전체
            if (availableFilters.length === 0 || availableFilters.length === 2) {
                filters.isAvailable = "";
            } else {
                filters.isAvailable = availableFilters[0];
            }

            // 정렬
            if (sortFilter) {
                filters.sort = sortFilter.value;
            }

            loadProducts(0);
        });
    }

    // 필터 초기화
    if (resetFilterBtn) {
        resetFilterBtn.addEventListener("click", function () {
            if (keywordFilter) keywordFilter.value = "";
            if (isAvailableTrue) isAvailableTrue.checked = false;
            if (isAvailableFalse) isAvailableFalse.checked = false;
            if (sortFilter) sortFilter.value = "createdAt,DESC";

            filters = {
                keyword: "",
                isAvailable: "",
                sort: "createdAt,DESC",
            };

            loadProducts(0);
        });
    }

    // 전체 선택
    if (selectAll) {
        selectAll.addEventListener("change", function () {
            const checkboxes = document.querySelectorAll(".product-checkbox");
            checkboxes.forEach((checkbox) => {
                checkbox.checked = selectAll.checked;
                const id = parseInt(checkbox.dataset.id);
                if (selectAll.checked) {
                    selectedProducts.add(id);
                } else {
                    selectedProducts.delete(id);
                }
            });
        });
    }

    // 선택 삭제
    if (btnDelete) {
        btnDelete.addEventListener("click", async function () {
            if (selectedProducts.size === 0) {
                alert("삭제할 상품을 선택해주세요.");
                return;
            }

            if (!confirm(`선택한 ${selectedProducts.size}개의 상품을 삭제하시겠습니까?`)) {
                return;
            }

            try {
                const deletePromises = Array.from(selectedProducts).map((productId) =>
                    fetch(`/api/admin/points/products/${productId}`, {
                        method: "DELETE",
                        headers: getAuthHeaders(),
                    })
                );

                await Promise.all(deletePromises);

                alert("삭제되었습니다.");
                selectedProducts.clear();
                if (selectAll) selectAll.checked = false;
                loadProducts(currentPage);
            } catch (error) {
                console.error("Error:", error);
                alert("삭제 중 오류가 발생했습니다.");
            }
        });
    }

    // 초기 로드
    loadProducts(0);
});