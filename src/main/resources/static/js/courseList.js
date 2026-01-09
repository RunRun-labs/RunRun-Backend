// ==========================
// DOM Elements
// ==========================
let courseListContainer;
let loadingMessage;
let emptyMessage;
let searchInput;
let searchButton;
let filterButton;
let sortButton;

function initDOMElements() {
  courseListContainer = document.getElementById("courseList");
  loadingMessage = document.getElementById("loadingMessage");
  emptyMessage = document.getElementById("emptyMessage");
  searchInput = document.querySelector(".search-input");
  searchButton = document.querySelector(".search-button");
  filterButton = document.querySelector(".filter-button");
  sortButton = document.querySelector(".sort-button");
}

// ==========================
// State
// ==========================
let currentCursor = null;
let hasNext = false;
let isLoading = false;
let currentFilters = {
  keyword: null,
  registerType: [], // 배열로 변경하여 여러개 선택 가능
  sortType: "LATEST", // default: 최신순
  nearby: false,
  lat: null,
  lng: null,
  radiusM: 1500, // default: 1.5km
};
const DEFAULT_PAGE_SIZE = 5;

// ==========================
// Get JWT Token
// ==========================
function getAccessToken() {
  // Try to get from localStorage
  let token = localStorage.getItem("accessToken");
  if (token) {
    return token.startsWith("Bearer ") ? token : `Bearer ${token}`;
  }

  // Try to get from cookies
  const cookies = document.cookie.split(";");
  for (let cookie of cookies) {
    const [name, value] = cookie.trim().split("=");
    if (name === "accessToken" || name === "token") {
      return value.startsWith("Bearer ") ? value : `Bearer ${value}`;
    }
  }

  return null;
}

// ==========================
// Load Course List
// ==========================
async function loadCourseList(reset = false) {
  if (isLoading) return;

  if (reset) {
    currentCursor = null;
    courseListContainer.innerHTML = "";
    // Create loading message element
    const loadingEl = document.createElement("div");
    loadingEl.className = "loading-message";
    loadingEl.id = "loadingMessage";
    loadingEl.innerHTML = "<p>코스를 불러오는 중...</p>";
    courseListContainer.appendChild(loadingEl);
    if (emptyMessage) emptyMessage.style.display = "none";
  } else if (loadingMessage) {
    loadingMessage.style.display = "block";
  }

  isLoading = true;

  try {
    const token = getAccessToken();
    const headers = {
      "Content-Type": "application/json",
    };

    if (token) {
      headers["Authorization"] = token;
    }

    // Build query parameters
    const params = new URLSearchParams();
    if (currentFilters.keyword) {
      params.append("keyword", currentFilters.keyword);
    }
    // registerType을 배열로 처리 (여러개 선택 가능)
    if (currentFilters.registerType && currentFilters.registerType.length > 0) {
      currentFilters.registerType.forEach((type) => {
        params.append("registerType", type);
      });
    }
    if (currentFilters.sortType) {
      params.append("sortType", currentFilters.sortType);
    }
    if (currentFilters.nearby) {
      params.append("nearby", "true");
      if (currentFilters.lat && currentFilters.lng) {
        params.append("lat", currentFilters.lat);
        params.append("lng", currentFilters.lng);
        params.append("radiusM", currentFilters.radiusM || 1500);
      }
    }
    if (currentCursor) {
      params.append("cursor", currentCursor);
    }
    params.append("size", String(DEFAULT_PAGE_SIZE));

    const url = `/api/courses${
      params.toString() ? "?" + params.toString() : ""
    }`;
    console.log("Fetching courses from:", url);

    const response = await fetch(url, {
      method: "GET",
      headers: headers,
    });

    console.log("Response status:", response.status);

    if (!response.ok) {
      const errorText = await response.text();
      console.error("Error response:", errorText);
      throw new Error(`코스 목록을 불러올 수 없습니다 (${response.status})`);
    }

    const result = await response.json();
    console.log("API Response:", result);

    const data = result.data;

    if (!data || !data.items) {
      console.error("Invalid data structure:", result);
      throw new Error("데이터 형식이 올바르지 않습니다");
    }

    // Hide loading message
    const loadingEl = courseListContainer.querySelector(".loading-message");
    if (loadingEl) {
      loadingEl.style.display = "none";
    }
    if (loadingMessage) {
      loadingMessage.style.display = "none";
    }

    if (reset) {
      courseListContainer.innerHTML = "";
    }

    if (data.items.length === 0 && reset) {
      if (emptyMessage) {
        emptyMessage.style.display = "block";
        courseListContainer.appendChild(emptyMessage);
      } else {
        const emptyEl = document.createElement("div");
        emptyEl.className = "empty-message";
        emptyEl.innerHTML = "<p>등록된 코스가 없습니다.</p>";
        courseListContainer.appendChild(emptyEl);
      }
    } else {
      if (emptyMessage) emptyMessage.style.display = "none";
      data.items.forEach((course) => {
        const card = createCourseCard(course);
        courseListContainer.appendChild(card);
      });
    }

    currentCursor = data.nextCursor;
    hasNext = data.hasNext;
  } catch (error) {
    console.error("Course list loading error:", error);

    // Hide loading message
    const loadingEl = courseListContainer.querySelector(".loading-message");
    if (loadingEl) {
      loadingEl.style.display = "none";
    }
    if (loadingMessage) {
      loadingMessage.style.display = "none";
    }

    // Show error message
    const errorEl = document.createElement("div");
    errorEl.className = "empty-message";
    errorEl.style.color = "#ff6b6b";
    errorEl.innerHTML = `<p>코스를 불러오는 중 오류가 발생했습니다.<br/>${error.message}</p>`;

    if (reset) {
      courseListContainer.innerHTML = "";
      courseListContainer.appendChild(errorEl);
    } else {
      courseListContainer.appendChild(errorEl);
    }
  } finally {
    isLoading = false;
  }
}

// ==========================
// Create Course Card
// ==========================
function createCourseCard(course) {
  const card = document.createElement("div");
  card.className = "course-card";
  card.style.cursor = "pointer";
  card.addEventListener("click", () => {
    // 코스 선택 모드(모집글에서 진입)면 query param을 상세로 그대로 전달
    const currentUrl = new URL(window.location.href);
    const params = currentUrl.searchParams;
    const qs = params.toString();
    window.location.href = `/courseDetail/${course.id}${qs ? "?" + qs : ""}`;
  });

  // Format course distance (코스 길이)
  const courseDistance = course.distanceM
    ? `${(course.distanceM / 1000).toFixed(2)} km`
    : "";

  // Format distance from user (내 주변 검색 시 사용자로부터의 거리)
  const distanceFromUser = course.distM
    ? `${(course.distM / 1000).toFixed(2)} km`
    : "";

  // Format register type
  const registerTypeText = getRegisterTypeText(course.registerType);

  // 내 주변 필터가 활성화되어 있고 distM이 있으면 거리 표시, 아니면 등록타입 표시
  const showDistance =
    currentFilters.nearby &&
    course.distM !== null &&
    course.distM !== undefined;

  card.innerHTML = `
    <div class="course-info">
      <div class="info-field">
        <label>제목</label>
        <input type="text" value="${escapeHtml(course.title || "")}" readonly/>
      </div>
      <div class="info-field">
        <label>거리</label>
        <input type="text" value="${courseDistance}" readonly/>
      </div>
      <div class="info-field info-field-address">
        <label>주소</label>
        <input type="text" class="address-input-full" value="${escapeHtml(
          course.address || ""
        )}" readonly/>
      </div>
      <div class="info-field info-field-badge-only">
        <div class="badge-container">
          <span class="register-type-badge register-type-${(
            course.registerType || ""
          ).toLowerCase()}">
            ${registerTypeText}
          </span>
          ${
            showDistance
              ? `<span class="distance-badge">${distanceFromUser}</span>`
              : ""
          }
        </div>
      </div>
    </div>
    <div class="course-thumbnail-wrapper">
      <div class="course-actions">
        <button class="action-icon heart-icon ${course.isLiked ? "active" : ""}" type="button" aria-label="좋아요" onclick="event.stopPropagation();">
          <img
              src="http://localhost:3845/assets/9af0c1d4ec1d966c7ec0b9ad1664f0fb4dc60971.svg"
              alt="하트 아이콘"
          />
          <span class="action-count">${course.likeCount || 0}</span>
        </button>
        <button class="action-icon star-icon ${course.isFavorited ? "active" : ""}" type="button" aria-label="즐겨찾기" onclick="event.stopPropagation();">
          <img
              src="http://localhost:3845/assets/a153ec3dff46ec34044b8bce0977bd3c5e0e43d7.svg"
              alt="별 아이콘"
          />
          <span class="action-count">${course.favoriteCount || 0}</span>
        </button>
      </div>
      <div class="course-thumbnail">
        ${
          course.thumbnailUrl
            ? `<img src="${escapeHtml(
                course.thumbnailUrl
              )}" alt="코스 썸네일" class="thumbnail-image" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';"/>
               <span style="display:none;">썸네일</span>`
            : `<span>썸네일</span>`
        }
      </div>
    </div>
  `;

  return card;
}

// ==========================
// Get Register Type Text
// ==========================
function getRegisterTypeText(registerType) {
  if (!registerType) return "";

  switch (registerType.toUpperCase()) {
    case "MANUAL":
      return "수동 등록";
    case "AUTO":
      return "자동 등록";
    case "AI":
      return "AI 등록";
    default:
      return registerType;
  }
}

// ==========================
// Escape HTML
// ==========================
function escapeHtml(text) {
  if (!text) return "";
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}

// ==========================
// Event Listeners
// ==========================

function initSearchListeners() {
  if (!searchButton || !searchInput) {
    return;
  }

  searchButton.addEventListener("click", () => {
    currentFilters.keyword = searchInput.value.trim() || null;
    loadCourseList(true);
  });

  searchInput.addEventListener("keypress", (e) => {
    if (e.key === "Enter") {
      currentFilters.keyword = searchInput.value.trim() || null;
      loadCourseList(true);
    }
  });

  let searchDebounceTimer = null;
  searchInput.addEventListener("input", () => {
    const nextKeyword = searchInput.value.trim();
    if (searchDebounceTimer) {
      clearTimeout(searchDebounceTimer);
    }
    searchDebounceTimer = setTimeout(() => {
      currentFilters.keyword = nextKeyword || null;
      loadCourseList(true);
    }, 300);
  });
}

// ==========================
// Filter & Sort UI
// ==========================
const filterPanel = document.getElementById("filterPanel");
const sortPanel = document.getElementById("sortPanel");
const filterToggleBtn = document.getElementById("filterToggleBtn");
const sortToggleBtn = document.getElementById("sortToggleBtn");

// Get user location for nearby filter
function getUserLocation() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error("Geolocation is not supported"));
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve({
          lat: position.coords.latitude,
          lng: position.coords.longitude,
        });
      },
      (error) => {
        reject(error);
      }
    );
  });
}

// Close panels when clicking outside
document.addEventListener("click", (e) => {
  if (
    filterPanel &&
    !filterPanel.contains(e.target) &&
    !filterToggleBtn.contains(e.target)
  ) {
    filterPanel.style.display = "none";
  }
  if (
    sortPanel &&
    !sortPanel.contains(e.target) &&
    !sortToggleBtn.contains(e.target)
  ) {
    sortPanel.style.display = "none";
  }
});

// Toggle filter panel
if (filterToggleBtn) {
  filterToggleBtn.addEventListener("click", (e) => {
    e.stopPropagation();
    if (filterPanel) {
      const isVisible = filterPanel.style.display !== "none";
      filterPanel.style.display = isVisible ? "none" : "block";
      if (sortPanel) sortPanel.style.display = "none";
    }
  });
}

// Toggle sort panel
if (sortToggleBtn) {
  sortToggleBtn.addEventListener("click", (e) => {
    e.stopPropagation();
    if (sortPanel) {
      const isVisible = sortPanel.style.display !== "none";
      sortPanel.style.display = isVisible ? "none" : "block";
      if (filterPanel) filterPanel.style.display = "none";
    }
  });
}

// Update filter UI based on current filters
function updateFilterUI() {
  const filterOptions = document.querySelectorAll(".filter-option");
  filterOptions.forEach((option) => {
    const filterType = option.dataset.filter;
    const value = option.dataset.value;

    option.classList.remove("active");

    if (filterType === "registerType") {
      // 배열에 포함되어 있으면 active
      if (
        currentFilters.registerType &&
        currentFilters.registerType.includes(value)
      ) {
        option.classList.add("active");
      }
    } else if (filterType === "nearby" && currentFilters.nearby) {
      option.classList.add("active");
    }
  });

  // Update sort UI
  const sortOptions = document.querySelectorAll(".sort-option");
  sortOptions.forEach((option) => {
    option.classList.remove("active");
    if (option.dataset.sort === currentFilters.sortType) {
      option.classList.add("active");
    }
  });

  // 거리 정렬 옵션 표시/숨김 (내 주변 선택 시에만 표시)
  const distanceSortOption = document.querySelector(
    '.sort-option[data-sort="DISTANCE"]'
  );
  if (distanceSortOption) {
    if (currentFilters.nearby) {
      distanceSortOption.style.display = "block";
    } else {
      distanceSortOption.style.display = "none";
      // 거리 정렬이 선택되어 있는데 nearby가 false면 최신순으로 변경
      if (currentFilters.sortType === "DISTANCE") {
        currentFilters.sortType = "LATEST";
        updateFilterUI();
      }
    }
  }
}

// Filter options
function initFilterOptions() {
  const filterOptions = document.querySelectorAll(".filter-option");
  filterOptions.forEach((option) => {
    option.addEventListener("click", () => {
      const filterType = option.dataset.filter;
      const value = option.dataset.value;

      // Toggle active state
      if (filterType === "registerType") {
        // 여러개 선택 가능하도록 배열로 처리
        if (!currentFilters.registerType) {
          currentFilters.registerType = [];
        }
        const index = currentFilters.registerType.indexOf(value);
        if (index > -1) {
          // 이미 선택되어 있으면 제거
          currentFilters.registerType.splice(index, 1);
        } else {
          // 선택되어 있지 않으면 추가
          currentFilters.registerType.push(value);
        }
        updateFilterUI();
      } else if (filterType === "nearby") {
        // Toggle nearby
        if (currentFilters.nearby) {
          currentFilters.nearby = false;
          currentFilters.lat = null;
          currentFilters.lng = null;
          updateFilterUI();
        } else {
          // Get user location
          getUserLocation()
            .then((location) => {
              currentFilters.nearby = true;
              currentFilters.lat = location.lat;
              currentFilters.lng = location.lng;
              updateFilterUI();
            })
            .catch((error) => {
              alert(
                "위치 정보를 가져올 수 없습니다. 위치 권한을 확인해주세요."
              );
              console.error("Location error:", error);
            });
        }
      }
    });
  });
}

// Filter apply button
const filterApplyBtn = document.querySelector(".filter-apply");
if (filterApplyBtn) {
  filterApplyBtn.addEventListener("click", () => {
    if (filterPanel) filterPanel.style.display = "none";
    loadCourseList(true);
  });
}

// Filter reset button
const filterResetBtn = document.querySelector(".filter-reset");
if (filterResetBtn) {
  filterResetBtn.addEventListener("click", () => {
    // Reset filters
    currentFilters.registerType = [];
    currentFilters.nearby = false;
    currentFilters.lat = null;
    currentFilters.lng = null;

    // Reset UI
    filterOptions.forEach((opt) => {
      opt.classList.remove("active");
    });

    if (filterPanel) filterPanel.style.display = "none";
    loadCourseList(true);
  });
}

// Sort options
function initSortOptions() {
  const sortOptions = document.querySelectorAll(".sort-option");
  sortOptions.forEach((option) => {
    option.addEventListener("click", async () => {
      const sortType = option.dataset.sort;

      // DISTANCE 정렬 선택 시 위치 정보 필요
      if (
        sortType === "DISTANCE" &&
        (!currentFilters.lat || !currentFilters.lng)
      ) {
        try {
          const location = await getUserLocation();
          currentFilters.lat = location.lat;
          currentFilters.lng = location.lng;
        } catch (error) {
          alert(
            "거리 정렬을 사용하려면 위치 정보가 필요합니다. 위치 권한을 확인해주세요."
          );
          return;
        }
      }

      currentFilters.sortType = sortType;

      updateFilterUI();

      if (sortPanel) sortPanel.style.display = "none";
      loadCourseList(true);
    });
  });
}

// Scroll to load more
const courseContent = document.querySelector(".course-content");
if (courseContent) {
  courseContent.addEventListener("scroll", () => {
    const scrollTop = courseContent.scrollTop;
    const scrollHeight = courseContent.scrollHeight;
    const clientHeight = courseContent.clientHeight;

    // Load more when near bottom
    if (
      scrollTop + clientHeight >= scrollHeight - 100 &&
      hasNext &&
      !isLoading
    ) {
      loadCourseList(false);
    }
  });
}

// ==========================
// Initialize
// ==========================
function init() {
  initDOMElements();
  initSearchListeners();

  if (!courseListContainer) {
    console.error("courseList container not found!");
    return;
  }

  // Initialize filter and sort options
  initFilterOptions();
  initSortOptions();
  updateFilterUI();

  loadCourseList(true);
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", init);
} else {
  init();
}
