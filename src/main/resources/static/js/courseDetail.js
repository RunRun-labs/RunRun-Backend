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
let polyline = null;
let startMarker = null;
let endMarker = null;
let loopMarker = null;

// Initialize map
function initMap() {
  if (!mapContainer) {
    console.warn("map container not found; skipping map init");
    return;
  }
  if (typeof kakao === "undefined" || !kakao.maps) {
    console.error("Kakao Maps SDK not loaded");
    return;
  }

  const center = new kakao.maps.LatLng(37.5665, 126.978);
  map = new kakao.maps.Map(mapContainer, {
    center: center,
    level: 5,
    draggable: true,
    scrollwheel: true,
  });

  // ⭐ 지도 초기화 직후 무조건 relayout (여러 번 호출하여 확실하게)
  setTimeout(() => {
    if (map) {
      map.relayout();
      map.setDraggable(true);
      map.setZoomable(true);
    }
  }, 0);

  setTimeout(() => {
    if (map) {
      map.relayout();
      map.setDraggable(true);
      map.setZoomable(true);
    }
  }, 100);

  setTimeout(() => {
    if (map) {
      map.relayout();
      map.setDraggable(true);
      map.setZoomable(true);
    }
  }, 300);

  setTimeout(() => {
    if (map) {
      map.relayout();
      map.setDraggable(true);
      map.setZoomable(true);
    }
  }, 500);
}

// Add marker
function addMarker(lat, lng, labelText, variant = "default") {
  const latlng = new kakao.maps.LatLng(lat, lng);
  const palette = {
    start: "#1e88e5",
    end: "#ff3d00",
    loop: "#8e24aa",
    default: "#333333",
  };

  let color = palette[variant] || palette.default;
  if (typeof variant === "string" && variant.startsWith("#")) {
    color = variant;
  }

  const content = `
    <div style="transform:translate(-50%,-100%);text-align:center;">
      <div style="
        padding:4px 10px;
        border-radius:14px;
        background:${color};
        color:#fff;
        font-size:11px;
        font-weight:bold;
        box-shadow:0 2px 6px rgba(0,0,0,0.35);
      ">
        ${labelText}
      </div>
      <div style="
        width:0;
        height:0;
        border-left:6px solid transparent;
        border-right:6px solid transparent;
        border-top:10px solid ${color};
        margin:0 auto;
      "></div>
    </div>
  `;

  const overlay = new kakao.maps.CustomOverlay({
    position: latlng,
    content,
    yAnchor: 1,
  });
  overlay.setMap(map);
  return overlay;
}

// Clear marker
function clearMarker(markerRef) {
  if (markerRef) {
    markerRef.setMap(null);
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

    const response = await fetch(`/api/routes/${courseId}`, {
      method: "GET",
      headers: headers,
    });

    if (!response.ok) {
      throw new Error("코스 데이터를 불러올 수 없습니다");
    }

    const result = await response.json();
    console.log("API Response:", result);

    const course = result.data;

    if (!course) {
      throw new Error("코스 데이터가 없습니다");
    }

    console.log("Course data:", course);
    console.log("Course path:", course.path);
    console.log("Path type:", typeof course.path);

    // Fill form fields with course data
    fillCourseData(course);

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
  if (!dateString) return "";

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
  const courseImageField = document.getElementById("courseImageField");
  const courseImagePreview = document.getElementById("courseImagePreview");

  if (course.imageUrl) {
    if (courseImage) {
      courseImage.src = course.imageUrl;
    }
    if (courseImagePreview) {
      courseImagePreview.src = course.imageUrl;
    }
    if (courseImageContainer) {
      courseImageContainer.style.display = "block";
    }
    if (courseImageField) {
      courseImageField.style.display = "block";
    }
  }
}

// Display course on map
function displayCourseOnMap(course) {
  if (!map) {
    console.error("Map not initialized");
    return;
  }

  console.log("Displaying course on map:", course);

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
        "출발",
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

  if (isRoundTrip) {
    // 루프 코스일 때는 출발점만 표시
    startMarker = addMarker(displayStartLat, displayStartLng, "출발", "start");
  } else {
    startMarker = addMarker(displayStartLat, displayStartLng, "출발", "start");
    endMarker = addMarker(endLat, endLng, "도착", "end");
  }

  // Draw route
  drawRoute(pathCoords);

  // Fit bounds to show entire route
  const latLngs = pathCoords.map(
    ([lng, lat]) => new kakao.maps.LatLng(lat, lng)
  );
  const bounds = new kakao.maps.LatLngBounds();
  latLngs.forEach((p) => bounds.extend(p));
  map.setBounds(bounds);

  // ⭐ setBounds 이후에도 relayout 필수
  setTimeout(() => {
    if (map) {
      map.relayout();
    }
  }, 0);

  setTimeout(() => {
    if (map) {
      map.relayout();
    }
  }, 300);

  // ⭐ 마커 추가 후 + 전체 작업 완료 후 최종 relayout
  setTimeout(() => {
    if (map) {
      map.relayout();
      // 드래그/줌 기능 명시적 재활성화
      map.setDraggable(true);
      map.setZoomable(true);
    }
  }, 500);
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

    const response = await fetch(`/api/routes/${courseId}`, {
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
// Event Listeners
// ==========================

// Delete button
if (deleteBtn) {
  deleteBtn.addEventListener("click", deleteCourse);
}

// Initialize map and load course data when page loads
function bootstrapMap() {
  if (!mapContainer) {
    console.warn("Map container not found");
    return;
  }

  // Wait for Kakao Maps SDK to load
  if (typeof kakao === "undefined" || !kakao.maps) {
    console.log("Waiting for Kakao Maps SDK to load...");
    setTimeout(bootstrapMap, 100);
    return;
  }

  console.log("Initializing map...");
  initMap();

  // 맵이 완전히 초기화된 후에 course data 로드
  // 경로를 그리기 전에도 맵이 상호작용 가능하도록 보장
  setTimeout(() => {
    if (map) {
      map.relayout();
      map.setDraggable(true);
      map.setZoomable(true);
    }
    loadCourseData();
  }, 600);
}

// Wait for both DOM and Kakao Maps SDK
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", () => {
    if (typeof kakao !== "undefined" && kakao.maps) {
      bootstrapMap();
    } else {
      // Wait for Kakao Maps SDK
      window.addEventListener("load", bootstrapMap);
    }
  });
} else {
  if (typeof kakao !== "undefined" && kakao.maps) {
    bootstrapMap();
  } else {
    window.addEventListener("load", bootstrapMap);
  }
}
