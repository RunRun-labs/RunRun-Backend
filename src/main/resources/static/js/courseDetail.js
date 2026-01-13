// ==========================
// DOM Elements
// ==========================
const mapContainer = document.getElementById("map");
const deleteBtn = document.getElementById("deleteBtn");

// ==========================
// Get Course ID from URL
// ==========================
function getCourseIdFromUrl() {
  const path = window.location.pathname;
  // Try courseDetail/{course_id} pattern first
  let match = path.match(/\/courseDetail\/(\d+)/);
  if (match) {
    return match[1];
  }
  // Fallback to /course/{courseId} pattern
  match = path.match(/\/course\/(\d+)/);
  return match ? match[1] : null;
}

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
// Kakao Map
// ==========================
let map = null;
let mapModal = null; // 모달 지도
let polyline = null;
let startMarker = null;
let endMarker = null;
let loopMarker = null;
let startInfoWindow = null;
let endInfoWindow = null;
let loadedCourse = null;

// 모달 마커 및 경로
let modalPolyline = null;
let modalStartMarker = null;
let modalEndMarker = null;
let modalStartInfoWindow = null;
let modalEndInfoWindow = null;
let isModalMapInitialized = false; // 모달 지도 초기화 여부

// ==========================
// Select Mode (from recruit)
// ==========================
function getSelectModeParams() {
  const u = new URL(window.location.href);
  const selectMode = u.searchParams.get("selectMode");
  return {
    isRecruitSelect: selectMode === "recruit",
    isSoloSelect: selectMode === "solo",
    isSelectMode: selectMode === "recruit" || selectMode === "solo",
    returnTo: u.searchParams.get("returnTo"),
  };
}

function setupSelectConfirm() {
  const { isSelectMode, returnTo } = getSelectModeParams();
  const bar = document.getElementById("selectConfirmBar");
  const btn = document.getElementById("selectConfirmBtn");
  if (!bar || !btn) {
    return;
  }

  if (!isSelectMode || !returnTo) {
    bar.style.display = "none";
    const content = document.querySelector(".course-content");
    if (content) {
      content.classList.remove("has-select-confirm");
    }
    return;
  }

  bar.style.display = "block";
  const content = document.querySelector(".course-content");
  if (content) {
    content.classList.add("has-select-confirm");
  }

  btn.addEventListener("click", () => {
    if (!loadedCourse) {
      alert("코스 정보를 불러오는 중입니다. 잠시 후 다시 시도해주세요.");
      return;
    }

    const base = new URL(returnTo, window.location.origin);
    base.searchParams.set(
      "courseId",
      String(loadedCourse.id || getCourseIdFromUrl())
    );
    base.searchParams.set("courseName", loadedCourse.title || "코스 선택됨");
    if (loadedCourse.distanceM != null) {
      base.searchParams.set(
        "courseDistanceKm",
        (loadedCourse.distanceM / 1000).toFixed(2)
      );
    }
    window.location.href = base.pathname + base.search + base.hash;
  });
}

// Initialize map
function initMap() {
  console.log("[MAP INIT] Starting map initialization...");
  
  if (!mapContainer) {
    console.error("[MAP INIT ERROR] Map container not found!");
    return;
  }
  console.log("[MAP INIT] Map container found:", mapContainer);
  
  if (typeof kakao === "undefined" || !kakao.maps) {
    console.error("[MAP INIT ERROR] Kakao Maps SDK not loaded!");
    return;
  }
  console.log("[MAP INIT] Kakao Maps SDK loaded successfully");

  const center = new kakao.maps.LatLng(37.5665, 126.978);
  console.log("[MAP INIT] Center position:", center);
  
  // 지도 생성 - 드래그와 줌 확실하게 활성화
  const mapOptions = {
    center: center,
    level: 5,
    draggable: true,
    scrollwheel: true,
    disableDoubleClick: false,
    disableDoubleClickZoom: false,
    keyboardShortcuts: true
  };
  console.log("[MAP INIT] Map options:", mapOptions);
  
  map = new kakao.maps.Map(mapContainer, mapOptions);
  console.log("[MAP INIT] Map object created:", map);
  console.log("[MAP INIT] Map draggable status:", map.getDraggable());
  console.log("[MAP INIT] Map zoomable status:", map.getZoomable());

  // 지도에 마우스 이벤트 리스너 추가 (드래그 테스트)
  kakao.maps.event.addListener(map, 'dragstart', function() {
    console.log("[MAP EVENT] Drag started!");
  });
  
  kakao.maps.event.addListener(map, 'drag', function() {
    console.log("[MAP EVENT] Dragging...");
  });
  
  kakao.maps.event.addListener(map, 'dragend', function() {
    console.log("[MAP EVENT] Drag ended!");
  });
  
  kakao.maps.event.addListener(map, 'click', function(mouseEvent) {
    console.log("[MAP EVENT] Map clicked at:", mouseEvent.latLng);
  });

  // 지도 초기화 직후 여러 번 relayout 및 드래그 활성화
  const enableMapInteraction = () => {
    if (map) {
      console.log("[MAP ENABLE] Enabling map interaction...");
      map.relayout();
      map.setDraggable(true);
      map.setZoomable(true);
      console.log("[MAP ENABLE] Map draggable:", map.getDraggable());
      console.log("[MAP ENABLE] Map zoomable:", map.getZoomable());
      
      // DOM 요소 확인 및 touch-action 강제 변경
      // 오버레이를 제외하고 실제 Kakao 지도 div 찾기
      const allDivsInContainer = mapContainer.querySelectorAll('div');
      let mapDiv = null;
      
      // map-expand-overlay가 아닌 div 찾기
      for (let i = 0; i < allDivsInContainer.length; i++) {
        const div = allDivsInContainer[i];
        if (!div.classList.contains('map-expand-overlay') && 
            !div.classList.contains('map-expand-hint')) {
          mapDiv = div;
          break;
        }
      }
      
      console.log("[MAP ENABLE] Map div element:", mapDiv);
      if (mapDiv) {
        // Kakao Maps가 설정한 touch-action을 강제로 변경
        mapDiv.style.removeProperty('touch-action');
        mapDiv.style.touchAction = 'auto';
        mapDiv.style.setProperty('touch-action', 'auto', 'important');
        
        // 하위 모든 요소도 변경
        const allDivs = mapDiv.querySelectorAll('div');
        allDivs.forEach(div => {
          div.style.removeProperty('touch-action');
          div.style.touchAction = 'auto';
          div.style.setProperty('touch-action', 'auto', 'important');
        });
        
        // MutationObserver로 Kakao가 덮어쓰는 것 감지하고 다시 변경
        if (!mapDiv.hasAttribute('data-observer-attached')) {
          const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
              if (mutation.type === 'attributes' && mutation.attributeName === 'style') {
                const target = mutation.target;
                if (target.style.touchAction !== 'auto') {
                  console.log("[MAP OBSERVER] Touch-action changed, fixing...");
                  target.style.removeProperty('touch-action');
                  target.style.touchAction = 'auto';
                  target.style.setProperty('touch-action', 'auto', 'important');
                }
              }
            });
          });
          
          observer.observe(mapDiv, {
            attributes: true,
            attributeFilter: ['style'],
            subtree: true
          });
          
          mapDiv.setAttribute('data-observer-attached', 'true');
          console.log("[MAP ENABLE] MutationObserver attached to map div");
        }
        
        const computedStyle = window.getComputedStyle(mapDiv);
        console.log("[MAP ENABLE] Map div pointer-events:", computedStyle.pointerEvents);
        console.log("[MAP ENABLE] Map div cursor:", computedStyle.cursor);
        console.log("[MAP ENABLE] Map div position:", computedStyle.position);
        console.log("[MAP ENABLE] Map div z-index:", computedStyle.zIndex);
        console.log("[MAP ENABLE] Map div touch-action AFTER fix:", computedStyle.touchAction);
      }
      
      // 컨테이너 스타일 확인
      const containerStyle = window.getComputedStyle(mapContainer);
      console.log("[MAP ENABLE] Container pointer-events:", containerStyle.pointerEvents);
      console.log("[MAP ENABLE] Container cursor:", containerStyle.cursor);
      console.log("[MAP ENABLE] Container overflow:", containerStyle.overflow);
    }
  };

  // 여러 타이밍에 활성화 호출
  console.log("[MAP INIT] Setting up delayed enable calls...");
  enableMapInteraction();
  setTimeout(() => {
    console.log("[MAP ENABLE] Timeout 0ms");
    enableMapInteraction();
  }, 0);
  setTimeout(() => {
    console.log("[MAP ENABLE] Timeout 100ms");
    enableMapInteraction();
  }, 100);
  setTimeout(() => {
    console.log("[MAP ENABLE] Timeout 300ms");
    enableMapInteraction();
  }, 300);
  setTimeout(() => {
    console.log("[MAP ENABLE] Timeout 500ms");
    enableMapInteraction();
  }, 500);
  setTimeout(() => {
    console.log("[MAP ENABLE] Timeout 1000ms");
    enableMapInteraction();
  }, 1000);
  
  console.log("[MAP INIT] Map initialization complete!");
}

// Add marker (기본 마커 사용)
function addMarker(lat, lng, labelText, variant = "default") {
  const latlng = new kakao.maps.LatLng(lat, lng);

  // 기본 마커 생성
  const marker = new kakao.maps.Marker({
    position: latlng,
    clickable: true, // 클릭 가능하도록 명시
  });
  marker.setMap(map);

  // InfoWindow 생성
  const infoContent = `<div style="padding:8px;font-size:13px;font-weight:bold;">${labelText}</div>`;
  const infoWindow = new kakao.maps.InfoWindow({
    content: infoContent,
    removable: true, // 닫기 버튼 표시
  });

  // variant에 따라 InfoWindow 저장 (이벤트 리스너 등록 전에 저장)
  if (variant === "start") {
    startInfoWindow = infoWindow;
  } else if (variant === "end") {
    endInfoWindow = infoWindow;
  }

  // 마커 클릭 시 InfoWindow 표시
  kakao.maps.event.addListener(marker, "click", function () {
    // 다른 InfoWindow 닫기
    if (startInfoWindow && startInfoWindow !== infoWindow) {
      startInfoWindow.close();
    }
    if (endInfoWindow && endInfoWindow !== infoWindow) {
      endInfoWindow.close();
    }

    // 현재 InfoWindow 열기
    infoWindow.open(map, marker);
  });

  return marker;
}

// Clear marker
function clearMarker(markerRef) {
  if (markerRef) {
    markerRef.setMap(null);
  }
}

// Clear InfoWindows
function clearInfoWindows() {
  if (startInfoWindow) {
    startInfoWindow.close();
    startInfoWindow = null;
  }
  if (endInfoWindow) {
    endInfoWindow.close();
    endInfoWindow = null;
  }
}

// Draw route
function drawRoute(coords) {
  if (polyline) {
    polyline.setMap(null);
  }

  const path = coords.map((c) => new kakao.maps.LatLng(c[1], c[0])); // [lng,lat] -> LatLng(lat,lng)

  polyline = new kakao.maps.Polyline({
    path: path,
    strokeWeight: 5,
    strokeColor: "#ff3d00",
    strokeOpacity: 0.8,
    strokeStyle: "solid",
  });

  polyline.setMap(map);

  // ⭐ 경로 그린 뒤에도 relayout 필수
  setTimeout(() => {
    if (map) {
      map.relayout();
    }
  }, 0);
}

// Load course data and display on map
async function loadCourseData() {
  const courseId = getCourseIdFromUrl();
  if (!courseId) {
    console.warn("Course ID not found in URL");
    // Try to use data from Thymeleaf
    if (courseData && courseData.path) {
      displayCourseOnMap(courseData);
    }
    return;
  }

  try {
    const token = getAccessToken();
    const headers = {
      "Content-Type": "application/json",
    };

    if (token) {
      headers["Authorization"] = token;
    }

    const response = await fetch(`/api/courses/${courseId}`, {
      method: "GET",
      headers: headers,
    });

    if (!response.ok) {
      throw new Error("코스 데이터를 불러올 수 없습니다");
    }

    const result = await response.json();
    console.log("API Response:", result);

    const course = result.data;
    loadedCourse = course;

    if (!course) {
      throw new Error("코스 데이터가 없습니다");
    }

    console.log("Course data:", course);
    console.log("Course path:", course.path);
    console.log("Path type:", typeof course.path);

    // Fill form fields with course data
    fillCourseData(course);

    // Initialize like and favorite buttons
    initLikeButton(courseId, course);
    initFavoriteButton(courseId, course);

    // Show/hide edit/delete buttons based on isOwner
    const courseActions = document.getElementById("courseActions");
    const editBtn = document.getElementById("editBtn");

    if (course.isOwner && courseActions && editBtn) {
      courseActions.style.display = "flex";
      editBtn.onclick = () => {
        window.location.href = `/courseUpdate/${courseId}`;
      };
    } else if (courseActions) {
      courseActions.style.display = "none";
    }

    // Display on map - path가 있으면 표시
    if (course.path) {
      displayCourseOnMap(course);
    } else {
      console.warn("No path data in course:", course);
    }
  } catch (error) {
    console.error("Course loading error:", error);
    alert("코스 정보를 불러오는 중 오류가 발생했습니다: " + error.message);
  }
}

// Legacy function - no longer used but kept for compatibility
async function checkOwnership(course) {
  try {
    const token = getAccessToken();
    if (!token) {
      return;
    }

    // Decode JWT to get user info
    const payload = JSON.parse(atob(token.split(".")[1]));
    const currentUserId = payload.memberNo || payload.sub;

    // Note: CourseDetailResDto needs userId field to compare
    // For now, we'll check via API response or add userId to DTO
    // This is a placeholder - actual implementation depends on backend
  } catch (error) {
    console.error("Ownership check error:", error);
  }
}

// Format date to YYYY-MM-DD
function formatDate(dateString) {
  if (!dateString) {
    return "";
  }

  try {
    // Handle ISO 8601 format (e.g., "2025-12-20T15:30:00" or "2025-12-20T15:30:00.000Z")
    const date = new Date(dateString);
    if (isNaN(date.getTime())) {
      // Try parsing as LocalDateTime format (YYYY-MM-DDTHH:mm:ss)
      const parts = dateString.split("T");
      if (parts.length > 0) {
        return parts[0]; // Return YYYY-MM-DD part
      }
      return dateString;
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  } catch (error) {
    console.error("Date formatting error:", error);
    // Fallback: try to extract YYYY-MM-DD from string
    const match = dateString.match(/(\d{4}-\d{2}-\d{2})/);
    return match ? match[1] : dateString;
  }
}

// Fill course data in form fields
function fillCourseData(course) {
  // Title
  const titleInput = document.getElementById("courseTitle");
  if (titleInput && course.title) {
    titleInput.value = course.title;
  }

  // Description
  const descInput = document.getElementById("courseDescription");
  if (descInput && course.description) {
    descInput.value = course.description;
  }

  // Distance
  const distanceInput = document.getElementById("courseDistance");
  if (distanceInput && course.distanceM) {
    distanceInput.value = (course.distanceM / 1000).toFixed(2) + " km";
  }

  // Address
  const addressInput = document.getElementById("courseAddress");
  if (addressInput && course.address) {
    addressInput.value = course.address;
  }

  // Author
  const authorInput = document.getElementById("courseAuthor");
  if (authorInput && course.userName) {
    authorInput.value = course.userName;
  }

  // Created Date (format: YYYY-MM-DD)
  const createdAtInput = document.getElementById("courseCreatedAt");
  if (createdAtInput && course.createdAt) {
    createdAtInput.value = formatDate(course.createdAt);
  }

  // Image
  const courseImageContainer = document.getElementById("courseImageContainer");
  const courseImage = document.getElementById("courseImage");

  if (course.imageUrl) {
    if (courseImage) {
      courseImage.src = course.imageUrl;
    }
    if (courseImageContainer) {
      courseImageContainer.style.display = "block";
    }
  }
}

// Display course on map
function displayCourseOnMap(course) {
  console.log("[DISPLAY COURSE] Starting to display course on map...");
  
  if (!map) {
    console.error("[DISPLAY COURSE ERROR] Map not initialized!");
    return;
  }
  console.log("[DISPLAY COURSE] Map object exists:", map);
  console.log("[DISPLAY COURSE] Course data:", course);

  // Parse path (GeoJSON format)
  let pathCoords = [];

  // Handle different path formats
  if (course.path) {
    console.log("Processing path:", course.path);

    if (typeof course.path === "string") {
      // Try to parse as JSON string
      try {
        const parsed = JSON.parse(course.path);
        console.log("Parsed JSON string:", parsed);
        if (parsed.coordinates) {
          pathCoords = parsed.coordinates;
        } else if (Array.isArray(parsed)) {
          pathCoords = parsed;
        }
      } catch (e) {
        console.warn("Failed to parse path as JSON:", e);
      }
    } else if (course.path.coordinates) {
      // GeoJSON LineString format: { type: "LineString", coordinates: [[lng, lat], ...] }
      console.log("Using GeoJSON coordinates");
      pathCoords = course.path.coordinates;
    } else if (Array.isArray(course.path)) {
      // Direct array format: [[lng, lat], ...]
      console.log("Using direct array format");
      pathCoords = course.path;
    } else {
      console.warn("Unknown path format:", course.path);
    }
  } else {
    console.warn("No path in course object");
  }

  console.log("Parsed path coordinates:", pathCoords);

  if (pathCoords.length === 0) {
    console.warn("No path coordinates found", course);
    // Still show start marker if we have start coordinates
    if (course.startLat && course.startLng) {
      const displayStartLat = course.startLat;
      const displayStartLng = course.startLng;
      clearMarker(startMarker);
      clearMarker(endMarker);
      clearMarker(loopMarker);
      startMarker = addMarker(
        displayStartLat,
        displayStartLng,
        "📍 출발점",
        "start"
      );
      map.setCenter(new kakao.maps.LatLng(displayStartLat, displayStartLng));
    }
    return;
  }

  // Get start and end coordinates
  const startCoord = pathCoords[0];
  const endCoord = pathCoords[pathCoords.length - 1];

  // GeoJSON format is [lng, lat], but we need [lat, lng] for Kakao Maps
  const startLat = startCoord[1]; // GeoJSON is [lng, lat]
  const startLng = startCoord[0];
  const endLat = endCoord[1];
  const endLng = endCoord[0];

  // Check if round trip (start and end are the same)
  const isRoundTrip =
    Math.abs(startLat - endLat) < 0.0001 &&
    Math.abs(startLng - endLng) < 0.0001;

  // Use course.startLat and startLng if available, otherwise use first coordinate
  const displayStartLat = course.startLat || startLat;
  const displayStartLng = course.startLng || startLng;

  clearMarker(startMarker);
  clearMarker(endMarker);
  clearMarker(loopMarker);
  clearInfoWindows();

  if (isRoundTrip) {
    // 루프 코스일 때는 출발점만 표시
    startMarker = addMarker(
      displayStartLat,
      displayStartLng,
      "📍 출발점",
      "start"
    );
  } else {
    startMarker = addMarker(
      displayStartLat,
      displayStartLng,
      "📍 출발점",
      "start"
    );
    endMarker = addMarker(endLat, endLng, "🏁 도착점", "end");
  }

  // Draw route
  drawRoute(pathCoords);

  // Fit bounds to show entire route (setBounds 대신 setCenter 사용)
  const latLngs = pathCoords.map(
    ([lng, lat]) => new kakao.maps.LatLng(lat, lng)
  );
  const bounds = new kakao.maps.LatLngBounds();
  latLngs.forEach((p) => bounds.extend(p));

  // ⭐ setBounds는 드래그/줌을 비활성화할 수 있으므로, 대신 center와 level 계산
  const sw = bounds.getSouthWest();
  const ne = bounds.getNorthEast();
  const centerLat = (sw.getLat() + ne.getLat()) / 2;
  const centerLng = (sw.getLng() + ne.getLng()) / 2;

  // 거리에 따라 적절한 level 계산
  const latDiff = ne.getLat() - sw.getLat();
  const lngDiff = ne.getLng() - sw.getLng();
  const maxDiff = Math.max(latDiff, lngDiff);

  let level = 5; // 기본값
  if (maxDiff > 0.1) {
    level = 4;
  } else if (maxDiff > 0.05) {
    level = 5;
  } else if (maxDiff > 0.02) {
    level = 6;
  } else if (maxDiff > 0.01) {
    level = 7;
  } else {
    level = 8;
  }

  // center와 level 설정 (setBounds 대신)
  console.log("[DISPLAY COURSE] Setting map center and level...");
  map.setCenter(new kakao.maps.LatLng(centerLat, centerLng));
  map.setLevel(level);
  console.log("[DISPLAY COURSE] Map center set to:", centerLat, centerLng);
  console.log("[DISPLAY COURSE] Map level set to:", level);

  // setLevel 호출 직후 드래그/줌 재활성화 (setLevel이 드래그/줌을 비활성화할 수 있음)
  setTimeout(() => {
    if (map) {
      console.log("[DISPLAY COURSE] Re-enabling map after 100ms...");
      map.relayout();
      map.setDraggable(true);
      map.setZoomable(true);
      console.log("[DISPLAY COURSE] Map re-enabled - draggable:", map.getDraggable());
      console.log("[DISPLAY COURSE] Map re-enabled - zoomable:", map.getZoomable());
    }
  }, 100);
  
  setTimeout(() => {
    if (map) {
      console.log("[DISPLAY COURSE] Re-enabling map after 300ms...");
      map.setDraggable(true);
      map.setZoomable(true);
      console.log("[DISPLAY COURSE] Final draggable status:", map.getDraggable());
      console.log("[DISPLAY COURSE] Final zoomable status:", map.getZoomable());
    }
  }, 300);
  
  console.log("[DISPLAY COURSE] Course display complete!");
}

// Delete course
async function deleteCourse() {
  const courseId = getCourseIdFromUrl();
  if (!courseId) {
    alert("코스 ID를 찾을 수 없습니다");
    return;
  }

  if (!confirm("정말로 이 코스를 삭제하시겠습니까?")) {
    return;
  }

  try {
    const token = getAccessToken();
    if (!token) {
      alert("로그인이 필요합니다");
      return;
    }

    const response = await fetch(`/api/courses/${courseId}`, {
      method: "DELETE",
      headers: {
        "Content-Type": "application/json",
        Authorization: token,
      },
    });

    if (!response.ok) {
      throw new Error("코스 삭제에 실패했습니다");
    }

    alert("코스가 삭제되었습니다");
    window.location.href = "/course";
  } catch (error) {
    console.error("Delete course error:", error);
    alert("코스 삭제 중 오류가 발생했습니다");
  }
}

// ==========================
// Like/Favorite Functions
// ==========================

let isLiked = false;
let isFavorited = false;
let likeButtonInitialized = false;
let favoriteButtonInitialized = false;

// Initialize like button
function initLikeButton(courseId, course) {
  const likeBtn = document.getElementById("likeBtn");
  const likeCountEl = document.getElementById("likeCount");
  const likeSvg = likeBtn ? likeBtn.querySelector("svg") : null;

  if (!likeBtn || !likeCountEl) {
    console.warn("Like button elements not found");
    return;
  }

  // Set initial state
  isLiked = course.isLiked || false;
  if (likeCountEl) {
    likeCountEl.textContent = course.likeCount || 0;
  }

  // Update button appearance and SVG color
  if (isLiked) {
    likeBtn.classList.add("active");
    if (likeSvg) {
      likeSvg.style.color = "#ff0000"; // 빨간색
    }
  } else {
    likeBtn.classList.remove("active");
    if (likeSvg) {
      likeSvg.style.color = "#999999"; // 회색
    }
  }

  // Add event listener only once
  if (!likeButtonInitialized) {
    likeBtn.addEventListener("click", async () => {
      await handleLikeClick(courseId);
    });
    likeButtonInitialized = true;
  }
}

// Initialize favorite button
function initFavoriteButton(courseId, course) {
  const favoriteBtn = document.getElementById("favoriteBtn");
  const favoriteCountEl = document.getElementById("favoriteCount");
  const favoriteSvg = favoriteBtn ? favoriteBtn.querySelector("svg") : null;

  if (!favoriteBtn || !favoriteCountEl) {
    console.warn("Favorite button elements not found");
    return;
  }

  // Set initial state
  isFavorited = course.isFavorited || false;
  if (favoriteCountEl) {
    favoriteCountEl.textContent = course.favoriteCount || 0;
  }

  // Update button appearance and SVG color
  if (isFavorited) {
    favoriteBtn.classList.add("active");
    if (favoriteSvg) {
      favoriteSvg.style.color = "#ffd700"; // 노란색
    }
  } else {
    favoriteBtn.classList.remove("active");
    if (favoriteSvg) {
      favoriteSvg.style.color = "#999999"; // 회색
    }
  }

  // Add event listener only once
  if (!favoriteButtonInitialized) {
    favoriteBtn.addEventListener("click", async () => {
      await handleFavoriteClick(courseId);
    });
    favoriteButtonInitialized = true;
  }
}

// Handle like click
async function handleLikeClick(courseId) {
  const token = getAccessToken();
  if (!token) {
    alert("로그인이 필요합니다");
    window.location.href = "/login";
    return;
  }

  const likeBtn = document.getElementById("likeBtn");
  const likeCountEl = document.getElementById("likeCount");

  try {
    if (isLiked) {
      // Unlike
      const response = await fetch(`/api/courses/like/${courseId}`, {
        method: "DELETE",
        headers: {
          Authorization: token,
          "Content-Type": "application/json",
        },
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        const errorMessage =
          errorData.message || errorData.error || "좋아요 취소에 실패했습니다";
        alert(errorMessage);
        return;
      }

      isLiked = false;
      likeBtn.classList.remove("active");
      // Update SVG color
      const likeSvg = likeBtn.querySelector("svg");
      if (likeSvg) {
        likeSvg.style.color = "#999999"; // 회색
      }
      // Refresh course data to update counts
      await refreshCourseData(courseId);
    } else {
      // Like
      const response = await fetch(`/api/courses/like/${courseId}`, {
        method: "POST",
        headers: {
          Authorization: token,
          "Content-Type": "application/json",
        },
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        const errorMessage =
          errorData.message || errorData.error || "좋아요에 실패했습니다";
        alert(errorMessage);
        return;
      }

      isLiked = true;
      likeBtn.classList.add("active");
      // Update SVG color
      const likeSvg = likeBtn.querySelector("svg");
      if (likeSvg) {
        likeSvg.style.color = "#ff0000"; // 빨간색
      }
      // Refresh course data to update counts
      await refreshCourseData(courseId);
    }
  } catch (error) {
    console.error("Like error:", error);
    alert("좋아요 처리 중 오류가 발생했습니다: " + error.message);
  }
}

// Handle favorite click
async function handleFavoriteClick(courseId) {
  const token = getAccessToken();
  if (!token) {
    alert("로그인이 필요합니다");
    window.location.href = "/login";
    return;
  }

  const favoriteBtn = document.getElementById("favoriteBtn");
  const favoriteCountEl = document.getElementById("favoriteCount");

  try {
    if (isFavorited) {
      // Unfavorite
      const response = await fetch(`/api/courses/favorite/${courseId}`, {
        method: "DELETE",
        headers: {
          Authorization: token,
          "Content-Type": "application/json",
        },
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        const errorMessage =
          errorData.message ||
          errorData.error ||
          "즐겨찾기 취소에 실패했습니다";
        alert(errorMessage);
        return;
      }

      isFavorited = false;
      favoriteBtn.classList.remove("active");
      // Update SVG color
      const favoriteSvg = favoriteBtn.querySelector("svg");
      if (favoriteSvg) {
        favoriteSvg.style.color = "#999999"; // 회색
      }
      // Refresh course data to update counts
      await refreshCourseData(courseId);
    } else {
      // Favorite
      const response = await fetch(`/api/courses/favorite/${courseId}`, {
        method: "POST",
        headers: {
          Authorization: token,
          "Content-Type": "application/json",
        },
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        const errorMessage =
          errorData.message || errorData.error || "즐겨찾기에 실패했습니다";
        alert(errorMessage);
        return;
      }

      isFavorited = true;
      favoriteBtn.classList.add("active");
      // Update SVG color
      const favoriteSvg = favoriteBtn.querySelector("svg");
      if (favoriteSvg) {
        favoriteSvg.style.color = "#ffd700"; // 노란색
      }
      // Refresh course data to update counts
      await refreshCourseData(courseId);
    }
  } catch (error) {
    console.error("Favorite error:", error);
    alert("즐겨찾기 처리 중 오류가 발생했습니다: " + error.message);
  }
}

// Refresh course data after like/favorite action
async function refreshCourseData(courseId) {
  try {
    const token = getAccessToken();
    const headers = {
      "Content-Type": "application/json",
    };

    if (token) {
      headers["Authorization"] = token;
    }

    const response = await fetch(`/api/courses/${courseId}`, {
      method: "GET",
      headers: headers,
    });

    if (!response.ok) {
      throw new Error("코스 데이터를 불러올 수 없습니다");
    }

    const result = await response.json();
    const course = result.data;

    if (course) {
      // Update counts
      const likeCountEl = document.getElementById("likeCount");
      const favoriteCountEl = document.getElementById("favoriteCount");

      if (likeCountEl) {
        likeCountEl.textContent = course.likeCount || 0;
      }
      if (favoriteCountEl) {
        favoriteCountEl.textContent = course.favoriteCount || 0;
      }

      // Update state
      isLiked = course.isLiked || false;
      isFavorited = course.isFavorited || false;

      // Update button appearance
      const likeBtn = document.getElementById("likeBtn");
      const favoriteBtn = document.getElementById("favoriteBtn");
      const likeSvg = likeBtn ? likeBtn.querySelector("svg") : null;
      const favoriteSvg = favoriteBtn ? favoriteBtn.querySelector("svg") : null;

      if (likeBtn) {
        if (isLiked) {
          likeBtn.classList.add("active");
          if (likeSvg) {
            likeSvg.style.color = "#ff0000";
          }
        } else {
          likeBtn.classList.remove("active");
          if (likeSvg) {
            likeSvg.style.color = "#999999";
          }
        }
      }

      if (favoriteBtn) {
        if (isFavorited) {
          favoriteBtn.classList.add("active");
          if (favoriteSvg) {
            favoriteSvg.style.color = "#ffd700";
          }
        } else {
          favoriteBtn.classList.remove("active");
          if (favoriteSvg) {
            favoriteSvg.style.color = "#999999";
          }
        }
      }
    }
  } catch (error) {
    console.error("Refresh course data error:", error);
  }
}

// ==========================
// Map Modal Functions
// ==========================

// Open map modal
function openMapModal() {
  console.log("[MAP MODAL] Opening map modal...");
  const modal = document.getElementById("mapModal");
  if (modal) {
    modal.style.visibility = "visible";
    modal.style.opacity = "1";
    
    // 모달 지도가 아직 초기화되지 않았다면 초기화
    if (!isModalMapInitialized) {
      console.log("[MAP MODAL] First time opening, initializing...");
      setTimeout(() => {
        initModalMap();
        if (loadedCourse && loadedCourse.path) {
          displayCourseOnModalMap(loadedCourse);
        }
        isModalMapInitialized = true;
      }, 100);
    } else {
      // 이미 초기화되어 있으면 relayout만 호출
      console.log("[MAP MODAL] Already initialized, just relayout...");
      setTimeout(() => {
        if (mapModal) {
          mapModal.relayout();
          mapModal.setDraggable(true);
          mapModal.setZoomable(true);
        }
      }, 100);
    }
  }
}

// Close map modal
function closeMapModal() {
  console.log("[MAP MODAL] Closing map modal...");
  const modal = document.getElementById("mapModal");
  if (modal) {
    modal.style.visibility = "hidden";
    modal.style.opacity = "0";
  }
}

// Initialize modal map
function initModalMap() {
  console.log("[MAP MODAL] Initializing modal map...");
  
  const modalMapContainer = document.getElementById("mapModal-map");
  if (!modalMapContainer) {
    console.error("[MAP MODAL ERROR] Modal map container not found!");
    return;
  }
  
  if (typeof kakao === "undefined" || !kakao.maps) {
    console.error("[MAP MODAL ERROR] Kakao Maps SDK not loaded!");
    return;
  }

  const center = new kakao.maps.LatLng(37.5665, 126.978);
  
  const mapOptions = {
    center: center,
    level: 5,
    draggable: true,
    scrollwheel: true,
    disableDoubleClick: false,
    disableDoubleClickZoom: false,
    keyboardShortcuts: true
  };
  
  mapModal = new kakao.maps.Map(modalMapContainer, mapOptions);
  console.log("[MAP MODAL] Modal map created successfully");
  console.log("[MAP MODAL] Modal map draggable:", mapModal.getDraggable());
  console.log("[MAP MODAL] Modal map zoomable:", mapModal.getZoomable());
  
  // 여러 번 relayout 호출
  setTimeout(() => {
    if (mapModal) {
      mapModal.relayout();
      mapModal.setDraggable(true);
      mapModal.setZoomable(true);
      console.log("[MAP MODAL] Modal map re-enabled (100ms)");
    }
  }, 100);
  
  setTimeout(() => {
    if (mapModal) {
      mapModal.relayout();
      mapModal.setDraggable(true);
      mapModal.setZoomable(true);
      console.log("[MAP MODAL] Modal map re-enabled (300ms)");
    }
  }, 300);
  
  setTimeout(() => {
    if (mapModal) {
      mapModal.relayout();
      mapModal.setDraggable(true);
      mapModal.setZoomable(true);
      console.log("[MAP MODAL] Modal map re-enabled (500ms)");
    }
  }, 500);
}

// Display course on modal map
function displayCourseOnModalMap(course) {
  console.log("[MAP MODAL] Displaying course on modal map...");
  
  if (!mapModal) {
    console.error("[MAP MODAL ERROR] Modal map not initialized!");
    return;
  }

  // Parse path
  let pathCoords = [];

  if (course.path) {
    if (typeof course.path === "string") {
      try {
        const parsed = JSON.parse(course.path);
        if (parsed.coordinates) {
          pathCoords = parsed.coordinates;
        } else if (Array.isArray(parsed)) {
          pathCoords = parsed;
        }
      } catch (e) {
        console.warn("[MAP MODAL] Failed to parse path as JSON:", e);
      }
    } else if (course.path.coordinates) {
      pathCoords = course.path.coordinates;
    } else if (Array.isArray(course.path)) {
      pathCoords = course.path;
    }
  }

  if (pathCoords.length === 0) {
    console.warn("[MAP MODAL] No path coordinates found");
    return;
  }

  // Get start and end coordinates
  const startCoord = pathCoords[0];
  const endCoord = pathCoords[pathCoords.length - 1];
  const startLat = startCoord[1];
  const startLng = startCoord[0];
  const endLat = endCoord[1];
  const endLng = endCoord[0];

  const isRoundTrip =
    Math.abs(startLat - endLat) < 0.0001 &&
    Math.abs(startLng - endLng) < 0.0001;

  const displayStartLat = course.startLat || startLat;
  const displayStartLng = course.startLng || startLng;

  // Clear previous markers
  if (modalStartMarker) modalStartMarker.setMap(null);
  if (modalEndMarker) modalEndMarker.setMap(null);
  if (modalPolyline) modalPolyline.setMap(null);
  if (modalStartInfoWindow) modalStartInfoWindow.close();
  if (modalEndInfoWindow) modalEndInfoWindow.close();

  // Add markers
  const startLatLng = new kakao.maps.LatLng(displayStartLat, displayStartLng);
  modalStartMarker = new kakao.maps.Marker({
    position: startLatLng,
    clickable: true,
  });
  modalStartMarker.setMap(mapModal);

  modalStartInfoWindow = new kakao.maps.InfoWindow({
    content: '<div style="padding:8px;font-size:13px;font-weight:bold;">📍 출발점</div>',
    removable: true,
  });

  kakao.maps.event.addListener(modalStartMarker, "click", function () {
    if (modalEndInfoWindow) modalEndInfoWindow.close();
    modalStartInfoWindow.open(mapModal, modalStartMarker);
  });

  if (!isRoundTrip) {
    const endLatLng = new kakao.maps.LatLng(endLat, endLng);
    modalEndMarker = new kakao.maps.Marker({
      position: endLatLng,
      clickable: true,
    });
    modalEndMarker.setMap(mapModal);

    modalEndInfoWindow = new kakao.maps.InfoWindow({
      content: '<div style="padding:8px;font-size:13px;font-weight:bold;">🏁 도착점</div>',
      removable: true,
    });

    kakao.maps.event.addListener(modalEndMarker, "click", function () {
      if (modalStartInfoWindow) modalStartInfoWindow.close();
      modalEndInfoWindow.open(mapModal, modalEndMarker);
    });
  }

  // Draw route
  const path = pathCoords.map((c) => new kakao.maps.LatLng(c[1], c[0]));
  modalPolyline = new kakao.maps.Polyline({
    path: path,
    strokeWeight: 5,
    strokeColor: "#ff3d00",
    strokeOpacity: 0.8,
    strokeStyle: "solid",
  });
  modalPolyline.setMap(mapModal);

  // Fit bounds
  const bounds = new kakao.maps.LatLngBounds();
  path.forEach((p) => bounds.extend(p));

  const sw = bounds.getSouthWest();
  const ne = bounds.getNorthEast();
  const centerLat = (sw.getLat() + ne.getLat()) / 2;
  const centerLng = (sw.getLng() + ne.getLng()) / 2;

  const latDiff = ne.getLat() - sw.getLat();
  const lngDiff = ne.getLng() - sw.getLng();
  const maxDiff = Math.max(latDiff, lngDiff);

  let level = 5;
  if (maxDiff > 0.1) level = 4;
  else if (maxDiff > 0.05) level = 5;
  else if (maxDiff > 0.02) level = 6;
  else if (maxDiff > 0.01) level = 7;
  else level = 8;

  mapModal.setCenter(new kakao.maps.LatLng(centerLat, centerLng));
  mapModal.setLevel(level);
  
  setTimeout(() => {
    if (mapModal) {
      mapModal.relayout();
      mapModal.setDraggable(true);
      mapModal.setZoomable(true);
      console.log("[MAP MODAL] Modal map finalized");
    }
  }, 100);
  
  console.log("[MAP MODAL] Course displayed on modal map");
}

// ==========================
// Event Listeners
// ==========================

// Delete button
if (deleteBtn) {
  deleteBtn.addEventListener("click", deleteCourse);
}

// Initialize map and load course data when page loads
function bootstrapMap() {
  console.log("[BOOTSTRAP] Starting map bootstrap...");
  
  if (!mapContainer) {
    console.error("[BOOTSTRAP ERROR] Map container not found!");
    return;
  }
  console.log("[BOOTSTRAP] Map container found:", mapContainer);

  // Wait for Kakao Maps SDK to load
  if (typeof kakao === "undefined" || !kakao.maps) {
    console.log("[BOOTSTRAP] Waiting for Kakao Maps SDK to load...");
    setTimeout(bootstrapMap, 100);
    return;
  }
  console.log("[BOOTSTRAP] Kakao Maps SDK loaded!");

  console.log("[BOOTSTRAP] Initializing map...");
  initMap();

  // 맵이 완전히 초기화된 후에 course data 로드
  // 경로를 그리기 전에도 맵이 상호작용 가능하도록 보장
  setTimeout(() => {
    console.log("[BOOTSTRAP] Loading course data after 100ms...");
    if (map) {
      map.relayout();
      map.setDraggable(true);
      map.setZoomable(true);
      console.log("[BOOTSTRAP] Map re-enabled before loading course");
    }
    loadCourseData();
  }, 100);
  
  console.log("[BOOTSTRAP] Bootstrap complete!");
}

// ==========================
// Siren (Report) Functions
// ==========================

// Open siren modal
function openSirenModal() {
  const modal = document.getElementById("sirenModal");
  const descriptionTextarea = document.getElementById("sirenDescription");
  const errorEl = document.getElementById("sirenDescriptionError");

  if (modal) {
    modal.style.display = "flex";
    if (descriptionTextarea) {
      descriptionTextarea.value = "";
    }
    if (errorEl) {
      errorEl.style.display = "none";
      errorEl.textContent = "";
    }
  }
}

// Close siren modal
function closeSirenModal() {
  const modal = document.getElementById("sirenModal");
  const descriptionTextarea = document.getElementById("sirenDescription");
  const errorEl = document.getElementById("sirenDescriptionError");

  if (modal) {
    modal.style.display = "none";
  }
  if (descriptionTextarea) {
    descriptionTextarea.value = "";
  }
  if (errorEl) {
    errorEl.style.display = "none";
    errorEl.textContent = "";
  }
}

// Validate siren description
function validateSirenDescription(description) {
  if (!description || description.trim().length === 0) {
    return "신고 사유를 반드시 입력해주세요.";
  }
  if (description.trim().length < 10) {
    return "신고 사유는 10자 이상 입력해주세요.";
  }
  if (description.trim().length > 500) {
    return "신고 사유는 500자 이하로 입력해주세요.";
  }
  return null;
}

// Submit siren report
async function submitSirenReport() {
  const courseId = getCourseIdFromUrl();
  if (!courseId) {
    alert("코스 ID를 찾을 수 없습니다");
    return;
  }

  const descriptionTextarea = document.getElementById("sirenDescription");
  const errorEl = document.getElementById("sirenDescriptionError");

  if (!descriptionTextarea) {
    console.error("Siren description textarea not found");
    return;
  }

  const description = descriptionTextarea.value.trim();

  // Validate
  const validationError = validateSirenDescription(description);
  if (validationError) {
    if (errorEl) {
      errorEl.textContent = validationError;
      errorEl.style.display = "block";
    }
    return;
  }

  // Hide error if validation passes
  if (errorEl) {
    errorEl.style.display = "none";
    errorEl.textContent = "";
  }

  const token = getAccessToken();
  if (!token) {
    alert("로그인이 필요합니다");
    window.location.href = "/login";
    return;
  }

  try {
    const response = await fetch(`/api/courses/siren/${courseId}`, {
      method: "POST",
      headers: {
        Authorization: token,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        description: description,
      }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      const errorMessage =
        errorData.message ||
        errorData.error ||
        errorData.detail ||
        "신고하기에 실패했습니다";
      alert(errorMessage);
      return;
    }

    alert("신고가 접수되었습니다.");
    closeSirenModal();
    // 신고 완료 후 목록 페이지로 이동
    window.location.href = "/course";
  } catch (error) {
    console.error("Siren report error:", error);
    alert("신고 처리 중 오류가 발생했습니다: " + error.message);
  }
}

// Initialize siren button
function initSirenButton() {
  const sirenBtn = document.getElementById("sirenBtn");
  const closeBtn = document.getElementById("closeSirenModal");
  const cancelBtn = document.getElementById("cancelSirenBtn");
  const submitBtn = document.getElementById("submitSirenBtn");
  const modal = document.getElementById("sirenModal");

  if (sirenBtn) {
    sirenBtn.addEventListener("click", openSirenModal);
  }

  if (closeBtn) {
    closeBtn.addEventListener("click", closeSirenModal);
  }

  if (cancelBtn) {
    cancelBtn.addEventListener("click", closeSirenModal);
  }

  if (submitBtn) {
    submitBtn.addEventListener("click", submitSirenReport);
  }

  // Close modal when clicking outside
  if (modal) {
    modal.addEventListener("click", (e) => {
      if (e.target === modal) {
        closeSirenModal();
      }
    });
  }

  // Close modal on Escape key
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && modal && modal.style.display === "flex") {
      closeSirenModal();
    }
  });
}

// Wait for both DOM and Kakao Maps SDK
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", () => {
    initSirenButton();
    setupSelectConfirm();
    if (typeof kakao !== "undefined" && kakao.maps) {
      bootstrapMap();
    } else {
      // Wait for Kakao Maps SDK
      window.addEventListener("load", bootstrapMap);
    }
  });
} else {
  initSirenButton();
  setupSelectConfirm();
  if (typeof kakao !== "undefined" && kakao.maps) {
    bootstrapMap();
  } else {
    window.addEventListener("load", bootstrapMap);
  }
}
