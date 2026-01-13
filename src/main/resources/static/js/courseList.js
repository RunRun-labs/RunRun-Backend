// ==========================
// DOM Elements
// ==========================
let courseListContainer;
let loadingSpinner;
let searchInput;
let filterButton;
let sortButton;

function initDOMElements() {
  courseListContainer = document.getElementById("courseList");
  loadingSpinner = document.getElementById("loadingSpinner");
  searchInput =
    document.getElementById("keywordInput") ||
    document.querySelector(".search-input");
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
  myCourses: false,
  myLikedCourses: false,
  myFavoritedCourses: false,
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
  if (isLoading) {
    return;
  }

  if (reset) {
    currentCursor = null;
    courseListContainer.innerHTML = "";
  }

  // Show loading spinner
  if (loadingSpinner) {
    loadingSpinner.style.display = 'flex';
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
    if (currentFilters.myCourses) {
      params.append("myCourses", "true");
    }
    if (currentFilters.myLikedCourses) {
      params.append("myLikedCourses", "true");
    }
    if (currentFilters.myFavoritedCourses) {
      params.append("myFavoritedCourses", "true");
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

    // Hide loading spinner
    if (loadingSpinner) {
      loadingSpinner.style.display = 'none';
    }

    if (reset) {
      courseListContainer.innerHTML = "";
    }

    if (data.items.length === 0 && reset) {
      const emptyEl = document.createElement("div");
      emptyEl.style.textAlign = 'center';
      emptyEl.style.padding = '40px 20px';
      emptyEl.style.color = 'var(--text-muted)';
      emptyEl.style.fontSize = '14px';
      emptyEl.innerHTML = "<p>등록된 코스가 없습니다.</p>";
      courseListContainer.appendChild(emptyEl);
    } else {
      data.items.forEach((course) => {
        const card = createCourseCard(course);
        courseListContainer.appendChild(card);
      });
    }

    currentCursor = data.nextCursor;
    hasNext = data.hasNext;
  } catch (error) {
    console.error("Course list loading error:", error);

    // Hide loading spinner
    if (loadingSpinner) {
      loadingSpinner.style.display = 'none';
    }

    // Show error message
    const errorEl = document.createElement("div");
    errorEl.style.textAlign = 'center';
    errorEl.style.padding = '40px 20px';
    errorEl.style.color = '#ff6b6b';
    errorEl.style.fontSize = '14px';
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
  card.className = "recruit-card";
  card.style.cursor = "pointer";
  card.setAttribute("data-course-id", course.id);
  card.addEventListener("click", () => {
    window.location.href = `/courseDetail/${course.id}`;
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

  // 내 주변 필터가 활성화되어 있고 distM이 있으면 거리 표시
  const showDistanceFromUser =
    currentFilters.nearby &&
    course.distM !== null &&
    course.distM !== undefined;

  // Register type badge class
  const registerTypeClass = (course.registerType || "").toLowerCase();

  // 썸네일 URL
  const thumbnailUrl = course.thumbnailUrl
    ? escapeHtml(course.thumbnailUrl)
    : null;

  card.innerHTML = `
    <div class="course-card-content">
      <!-- 1행: 뱃지 (등록 타입) -->
      <div class="card-badge-wrapper">
        <span class="card-badge register-type-${registerTypeClass}">${registerTypeText}</span>
      </div>
      
      <!-- 2행: 제목 -->
      <h3 class="card-title">${escapeHtml(course.title || "제목 없음")}</h3>
      
      <!-- 3행: 거리 정보 (내 위치에서 거리 - 내 주변 필터 활성화시만) -->
      ${
        showDistanceFromUser
          ? `
      <div class="card-distance">
        <svg class="card-distance-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M21 10C21 17 12 23 12 23C12 23 3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.364 3.63604C20.0518 5.32387 21 7.61305 21 10Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M12 13C13.6569 13 15 11.6569 15 10C15 8.34315 13.6569 7 12 7C10.3431 7 9 8.34315 9 10C9 11.6569 10.3431 13 12 13Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span>내 위치에서 ${distanceFromUser}</span>
      </div>
      `
          : ""
      }
      
      <!-- 4행: 상세 스펙 (코스 거리 | 주소) -->
      <div class="card-specs">
        ${
          courseDistance
            ? `
        <div class="card-spec-item">
          <svg class="card-spec-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M3 12L5 10M5 10L12 3L19 10M5 10V20C5 20.5523 5.44772 21 6 21H9M19 10L21 12M19 10V20C19 20.5523 18.5523 21 18 21H15M9 21C9.55228 21 10 20.5523 10 20V16C10 15.4477 10.4477 15 11 15H13C13.5523 15 14 15.4477 14 16V20C14 20.5523 14.4477 21 15 21M9 21H15" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>${courseDistance}</span>
        </div>
        `
            : ""
        }
        ${
          courseDistance && course.address
            ? `<span class="card-spec-divider">·</span>`
            : ""
        }
        ${
          course.address
            ? `
        <div class="card-spec-item">
          <svg class="card-spec-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M21 10C21 17 12 23 12 23C12 23 3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.364 3.63604C20.0518 5.32387 21 7.61305 21 10Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M12 13C13.6569 13 15 11.6569 15 10C15 8.34315 13.6569 7 12 7C10.3431 7 9 8.34315 9 10C9 11.6569 10.3431 13 12 13Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>${escapeHtml(course.address)}</span>
        </div>
        `
            : ""
        }
      </div>
    </div>
    
    <!-- 썸네일 이미지 (오른쪽) -->
    ${
      thumbnailUrl
        ? `<div class="course-card-thumbnail">
            <img src="${thumbnailUrl}" alt="코스 썸네일" class="course-card-thumbnail-image" onerror="this.style.display='none';" />
          </div>`
        : ""
    }
  `;

  return card;
}

// ==========================
// Get Register Type Text
// ==========================
function getRegisterTypeText(registerType) {
  if (!registerType) {
    return "";
  }

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
  if (!text) {
    return "";
  }
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}

// ==========================
// Event Listeners
// ==========================

function initSearchListeners() {
  if (!searchInput) {
    return;
  }

  // Enter 키로 검색
  searchInput.addEventListener("keypress", (e) => {
    if (e.key === "Enter") {
      currentFilters.keyword = searchInput.value.trim() || null;
      loadCourseList(true);
    }
  });

  // 실시간 검색 (한글자씩 입력할 때마다)
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
      if (sortPanel) {
        sortPanel.style.display = "none";
      }
    }
  });
}

// Toggle sort panel
if (sortToggleBtn) {
  sortToggleBtn.addEventListener("click", (e) => {
    e.stopPropagation();
    // ✅ Sort By 열 때마다 현재 필터 상태를 UI에 먼저 반영(거리순 disabled 동기화)
    try {
      updateFilterUI();
    } catch (e) {
      // ignore
    }
    if (sortPanel) {
      const isVisible = sortPanel.style.display !== "none";
      sortPanel.style.display = isVisible ? "none" : "block";
      if (filterPanel) {
        filterPanel.style.display = "none";
      }
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

  // ✅ 거리 정렬 옵션: "내 주변" 필터가 활성화된 경우에만 클릭 가능
  const distanceSortOption = document.querySelector(
    '.sort-option[data-sort="DISTANCE"]'
  );
  if (distanceSortOption) {
    distanceSortOption.disabled = !currentFilters.nearby;
    if (!currentFilters.nearby) {
      distanceSortOption.style.opacity = "0.45";
      distanceSortOption.style.cursor = "not-allowed";
    } else {
      distanceSortOption.style.opacity = "1";
      distanceSortOption.style.cursor = "pointer";
    }
  }

  // ✅ 거리순 정렬이 선택된 상태에서는 "내 주변" 필터 옵션 비활성화
  const nearbyFilterOption = document.querySelector(
    '.filter-option[data-filter="nearby"]'
  );
  if (nearbyFilterOption) {
    if (currentFilters.sortType === "DISTANCE" && currentFilters.nearby) {
      // 거리순 정렬이 선택되고 "내 주변" 필터가 활성화된 경우 비활성화
      nearbyFilterOption.disabled = true;
      nearbyFilterOption.style.opacity = "0.6";
      nearbyFilterOption.style.cursor = "not-allowed";
    } else {
      nearbyFilterOption.disabled = false;
      nearbyFilterOption.style.opacity = "1";
      nearbyFilterOption.style.cursor = "pointer";
    }
  }

  // 새 필터 옵션 UI 업데이트
  const myCoursesOption = document.querySelector(
    '.filter-option[data-filter="myCourses"]'
  );
  const myLikedCoursesOption = document.querySelector(
    '.filter-option[data-filter="myLikedCourses"]'
  );
  const myFavoritedCoursesOption = document.querySelector(
    '.filter-option[data-filter="myFavoritedCourses"]'
  );

  if (myCoursesOption) {
    myCoursesOption.classList.toggle("active", currentFilters.myCourses);
  }
  if (myLikedCoursesOption) {
    myLikedCoursesOption.classList.toggle(
      "active",
      currentFilters.myLikedCourses
    );
  }
  if (myFavoritedCoursesOption) {
    myFavoritedCoursesOption.classList.toggle(
      "active",
      currentFilters.myFavoritedCourses
    );
  }

  // ✅ "내 코스 보기" 필터가 활성화되면 좋아요/즐겨찾기 필터 옵션 숨기기
  // ✅ 좋아요/즐겨찾기 필터가 활성화되면 "내 코스 보기" 필터 옵션 숨기기 (양방향 배타)
  if (myLikedCoursesOption) {
    if (currentFilters.myCourses) {
      myLikedCoursesOption.style.display = "none";
    } else {
      myLikedCoursesOption.style.display = "block";
    }
  }
  if (myFavoritedCoursesOption) {
    if (currentFilters.myCourses) {
      myFavoritedCoursesOption.style.display = "none";
    } else {
      myFavoritedCoursesOption.style.display = "block";
    }
  }
  if (myCoursesOption) {
    if (currentFilters.myLikedCourses || currentFilters.myFavoritedCourses) {
      myCoursesOption.style.display = "none";
    } else {
      myCoursesOption.style.display = "block";
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
        // ✅ 거리순 정렬이 선택된 상태에서는 "내 주변" 필터 해제 불가
        if (currentFilters.nearby && currentFilters.sortType === "DISTANCE") {
          alert(
            "거리순 정렬이 선택된 상태에서는 '내 주변' 필터를 해제할 수 없습니다. 다른 정렬을 선택한 후 해제해주세요."
          );
          return;
        }

        // Toggle nearby
        if (currentFilters.nearby) {
          currentFilters.nearby = false;
          currentFilters.lat = null;
          currentFilters.lng = null;
          // ✅ "내 주변" 필터 해제 시 거리순 정렬도 해제
          if (currentFilters.sortType === "DISTANCE") {
            currentFilters.sortType = "LATEST";
          }
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
      } else if (filterType === "myCourses") {
        // ✅ "내 코스 보기" 클릭 시 좋아요/즐겨찾기 필터 비활성화
        currentFilters.myCourses = !currentFilters.myCourses;
        if (currentFilters.myCourses) {
          // 내 코스 보기가 활성화되면 좋아요/즐겨찾기 필터 비활성화
          currentFilters.myLikedCourses = false;
          currentFilters.myFavoritedCourses = false;
        }
        updateFilterUI();
      } else if (filterType === "myLikedCourses") {
        // ✅ 좋아요 필터 클릭 시 "내 코스 보기" 필터 비활성화
        if (currentFilters.myCourses) {
          currentFilters.myCourses = false;
        }
        currentFilters.myLikedCourses = !currentFilters.myLikedCourses;
        updateFilterUI();
      } else if (filterType === "myFavoritedCourses") {
        // ✅ 즐겨찾기 필터 클릭 시 "내 코스 보기" 필터 비활성화
        if (currentFilters.myCourses) {
          currentFilters.myCourses = false;
        }
        currentFilters.myFavoritedCourses = !currentFilters.myFavoritedCourses;
        updateFilterUI();
      }
    });
  });
}

// Filter apply button
const filterApplyBtn = document.querySelector(".filter-apply");
if (filterApplyBtn) {
  filterApplyBtn.addEventListener("click", () => {
    if (filterPanel) {
      filterPanel.style.display = "none";
    }
    // ✅ 적용 시점에 UI 먼저 동기화(거리순 disabled 동기화)
    try {
      updateFilterUI();
    } catch (e) {
      // ignore
    }
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
    currentFilters.myCourses = false;
    currentFilters.myLikedCourses = false;
    currentFilters.myFavoritedCourses = false;

    // Reset UI
    filterOptions.forEach((opt) => {
      opt.classList.remove("active");
    });

    if (filterPanel) {
      filterPanel.style.display = "none";
    }
    // ✅ 리셋 시점에 UI 동기화(거리순 disabled 동기화)
    try {
      updateFilterUI();
    } catch (e) {
      // ignore
    }
    loadCourseList(true);
  });
}

// Sort options
function initSortOptions() {
  const sortOptions = document.querySelectorAll(".sort-option");
  sortOptions.forEach((option) => {
    option.addEventListener("click", async () => {
      const sortType = option.dataset.sort;

      // ✅ 거리순 정렬 선택 시 자동으로 "내 주변" 필터 활성화
      if (sortType === "DISTANCE") {
        // "내 주변" 필터가 비활성화되어 있으면 자동으로 활성화
        if (!currentFilters.nearby) {
          currentFilters.nearby = true;
        }

        // 위치 정보가 없으면 사용자 위치 가져오기
        if (!currentFilters.lat || !currentFilters.lng) {
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
      }

      currentFilters.sortType = sortType;

      updateFilterUI();

      if (sortPanel) {
        sortPanel.style.display = "none";
      }
      loadCourseList(true);
    });
  });
}

// Scroll to load more
const courseMain = document.querySelector(".course-main");
if (courseMain) {
  courseMain.addEventListener("scroll", () => {
    const scrollTop = courseMain.scrollTop;
    const scrollHeight = courseMain.scrollHeight;
    const clientHeight = courseMain.clientHeight;

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
// FAB Button
// ==========================
function initCreateButton() {
  const createBtn = document.getElementById("createBtn");
  if (createBtn) {
    createBtn.addEventListener("click", () => {
      window.location.href = "/courseCreate";
    });
  }
}

// ==========================
// Back Button
// ==========================
function initBackButton() {
  const backBtn = document.getElementById('backBtn');
  if (backBtn) {
    backBtn.addEventListener('click', () => {
      window.history.back();
    });
  }
}

// ==========================
// Initialize
// ==========================
function init() {
  initDOMElements();
  initBackButton();
  initSearchListeners();
  initCreateButton();

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
