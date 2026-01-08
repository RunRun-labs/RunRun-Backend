document.addEventListener("DOMContentLoaded", function () {
  let currentPage = 0;
  let totalPages = 1;
  const pageSize = 10;

  // 필터 요소들
  const isActiveTrueCheckbox = document.getElementById("isActiveTrue");
  const isActiveFalseCheckbox = document.getElementById("isActiveFalse");
  const triggerEventCheckboxes = document.querySelectorAll(".trigger-event");
  const sortFilter = document.getElementById("sortFilter");
  const keywordFilter = document.getElementById("keywordFilter");
  const applyFilterBtn = document.getElementById("applyFilterBtn");
  const resetFilterBtn = document.getElementById("resetFilterBtn");

  // 테이블 바디
  const tableBody = document.getElementById("couponRoleTableBody");
  const paginationContainer = document.getElementById("paginationContainer");

  // 필터 상태
  let filters = {
    isActive: null,
    triggerEvents: [],
    keyword: "",
    sort: "createdAt,DESC",
  };

  // 쿠폰 정책 목록 로드
  function loadCouponRoles(page = 0) {
    currentPage = page;

    // 필터 파라미터 구성
    const params = new URLSearchParams();

    if (filters.isActive !== null) {
      params.append("isActive", filters.isActive);
    }

    if (filters.triggerEvents.length > 0) {
      filters.triggerEvents.forEach((event) => {
        params.append("triggerEvents", event);
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
    fetch(`/api/admin/coupon-roles?${params.toString()}`, {
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
          displayCouponRoles(data.content || []);
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
            <td colspan="8" class="empty-message">
              데이터를 불러오는 중 오류가 발생했습니다.
            </td>
          </tr>
        `;
      });
  }

  // 쿠폰 정책 목록 표시
  function displayCouponRoles(roles) {
    if (roles.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="8" class="empty-message">
            조회된 쿠폰 정책이 없습니다.
          </td>
        </tr>
      `;
      return;
    }

    const startIndex = currentPage * pageSize;
    tableBody.innerHTML = roles
      .map((role, index) => {
        const rowNumber = startIndex + index + 1;
        const createdAt = formatDate(role.createdAt);
        const triggerEventText = getTriggerEventText(role.triggerEvent);
        const isActiveText = role.isActive ? "활성" : "비활성";
        const isActiveClass = role.isActive ? "active" : "expired";
        const isDisabled = role.isActive ? "disabled" : "";
        const disabledStyle = role.isActive
          ? 'style="opacity: 0.5; cursor: not-allowed;"'
          : "";

        return `
          <tr>
            <td>${rowNumber}</td>
            <td>${escapeHtml(role.name || "-")}</td>
            <td>${escapeHtml(role.couponName || "-")}</td>
            <td>${triggerEventText}</td>
            <td>${role.conditionValue ?? "-"}</td>
            <td>
              <div class="toggle-switch">
                <label class="toggle-label-wrapper">
                  <input 
                    type="checkbox" 
                    class="toggle-input" 
                    data-role-id="${role.id}"
                    ${role.isActive ? "checked" : ""}
                  />
                  <span class="toggle-label">${isActiveText}</span>
                </label>
              </div>
            </td>
            <td>${createdAt}</td>
            <td>
              <div style="display: flex; gap: 8px; justify-content: center;">
                <button 
                  type="button" 
                  class="action-btn edit-btn" 
                  data-role-id="${role.id}"
                  ${isDisabled}
                  ${disabledStyle}
                >
                  수정
                </button>
                <button 
                  type="button" 
                  class="action-btn delete-btn" 
                  data-role-id="${role.id}"
                  ${isDisabled}
                  ${disabledStyle}
                >
                  삭제
                </button>
              </div>
            </td>
          </tr>
        `;
      })
      .join("");

    // 이벤트 리스너 추가
    attachButtonListeners();
    attachToggleListeners();
  }

  // 버튼 이벤트 리스너 추가
  function attachButtonListeners() {
    // 수정 버튼
    const editButtons = tableBody.querySelectorAll(".edit-btn:not([disabled])");
    editButtons.forEach((btn) => {
      btn.addEventListener("click", function (e) {
        e.preventDefault();
        e.stopPropagation();
        const roleId = this.getAttribute("data-role-id");
        if (roleId) {
          editCouponRole(parseInt(roleId));
        }
      });
    });

    // 삭제 버튼
    const deleteButtons = tableBody.querySelectorAll(
      ".delete-btn:not([disabled])"
    );
    deleteButtons.forEach((btn) => {
      btn.addEventListener("click", function (e) {
        e.preventDefault();
        e.stopPropagation();
        const roleId = this.getAttribute("data-role-id");
        if (roleId) {
          deleteCouponRole(parseInt(roleId));
        }
      });
    });
  }

  // 토글 버튼 이벤트 리스너 추가
  function attachToggleListeners() {
    const toggleInputs = tableBody.querySelectorAll(".toggle-input");
    toggleInputs.forEach((toggle) => {
      toggle.addEventListener("change", function (e) {
        e.stopPropagation();
        const roleId = this.getAttribute("data-role-id");
        const isActive = this.checked;

        if (roleId) {
          toggleCouponRoleActive(parseInt(roleId), isActive, this);
        }
      });
    });
  }

  // 쿠폰 정책 활성화/비활성화 토글
  function toggleCouponRoleActive(roleId, isActive, toggleElement) {
    // 토글 비활성화 (중복 요청 방지)
    toggleElement.disabled = true;

    fetch(`/api/admin/coupon-roles/${roleId}`, {
      method: "PATCH",
      headers: getAuthHeaders(),
      body: JSON.stringify({ isActive: isActive }),
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
        if (result.success) {
          // 성공 시 목록 새로고침
          loadCouponRoles(currentPage);
        } else {
          // 실패 시 토글 원래 상태로 되돌리기
          toggleElement.checked = !isActive;
          alert(result.message || "활성 상태 변경에 실패했습니다.");
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        // 실패 시 토글 원래 상태로 되돌리기
        toggleElement.checked = !isActive;
        let errorMessage = "활성 상태 변경 중 오류가 발생했습니다.";
        if (error.message) {
          errorMessage = error.message;
        } else if (error.data && error.data.message) {
          errorMessage = error.data.message;
        }
        alert(errorMessage);
      })
      .finally(() => {
        // 토글 다시 활성화
        toggleElement.disabled = false;
      });
  }

  // 트리거 이벤트 텍스트 변환
  function getTriggerEventText(event) {
    const eventMap = {
      SIGNUP: "회원가입",
      FIRST_RUNNING: "첫 러닝",
      RUN_COUNT_REACHED: "러닝 횟수 달성",
      BIRTHDAY: "생일",
    };
    return eventMap[event] || event || "-";
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
      loadCouponRoles(page);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  // 수정 함수 (전역)
  window.editCouponRole = function (roleId) {
    window.location.href = `/admin/coupon-role/update/${roleId}`;
  };

  // 삭제 함수 (전역)
  window.deleteCouponRole = function (roleId) {
    if (!confirm("정말로 이 쿠폰 정책을 삭제하시겠습니까?")) {
      return;
    }

    fetch(`/api/admin/coupon-roles/${roleId}`, {
      method: "DELETE",
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
        if (result.success) {
          alert("쿠폰 정책이 삭제되었습니다.");
          loadCouponRoles(currentPage);
        } else {
          alert(result.message || "삭제에 실패했습니다.");
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        let errorMessage = "삭제 중 오류가 발생했습니다.";
        if (error.message) {
          errorMessage = error.message;
        } else if (error.data && error.data.message) {
          errorMessage = error.data.message;
        }
        alert(errorMessage);
      });
  };

  // 필터 적용
  function applyFilters() {
    // isActive 필터
    if (isActiveTrueCheckbox.checked && !isActiveFalseCheckbox.checked) {
      filters.isActive = true;
    } else if (!isActiveTrueCheckbox.checked && isActiveFalseCheckbox.checked) {
      filters.isActive = false;
    } else {
      filters.isActive = null;
    }

    // triggerEvents 필터
    filters.triggerEvents = Array.from(triggerEventCheckboxes)
      .filter((cb) => cb.checked)
      .map((cb) => cb.value);

    filters.keyword = keywordFilter.value.trim();
    filters.sort = sortFilter.value;

    currentPage = 0;
    loadCouponRoles(0);
  }

  // 필터 초기화
  function resetFilters() {
    isActiveTrueCheckbox.checked = false;
    isActiveFalseCheckbox.checked = false;
    triggerEventCheckboxes.forEach((cb) => (cb.checked = false));
    keywordFilter.value = "";
    sortFilter.value = "createdAt,DESC";

    filters = {
      isActive: null,
      triggerEvents: [],
      keyword: "",
      sort: "createdAt,DESC",
    };

    currentPage = 0;
    loadCouponRoles(0);
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

  // 체크박스 클릭 시 기본 동작 방지 (줄바꿈 방지)
  triggerEventCheckboxes.forEach((checkbox) => {
    checkbox.addEventListener("click", function (e) {
      e.stopPropagation();
    });
  });

  if (isActiveTrueCheckbox) {
    isActiveTrueCheckbox.addEventListener("click", function (e) {
      e.stopPropagation();
    });
  }

  if (isActiveFalseCheckbox) {
    isActiveFalseCheckbox.addEventListener("click", function (e) {
      e.stopPropagation();
    });
  }

  // 초기 로드
  loadCouponRoles(0);
});
