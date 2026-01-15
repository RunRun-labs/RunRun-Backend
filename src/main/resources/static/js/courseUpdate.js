const MAX_DISTANCE_M = 42195;
const STORAGE_KEY = "courseDraft";

const form = document.getElementById("courseForm");
const stickyBtn = document.getElementById("stickyUpdateBtn");
const autoRouteBtn = document.getElementById("autoRouteBtn");
const manualRouteBtn = document.getElementById("manualRouteBtn");
const summaryEl = document.getElementById("routeSummary");
const mapContainer = document.getElementById("map");

const titleInput = form.querySelector('input[name="title"]');
const descInput = form.querySelector('textarea[name="description"]');
const pathInput = document.getElementById("pathInput");
const distanceMInput = document.getElementById("distanceMInput");
const startLatInput = document.getElementById("startLatInput");
const startLngInput = document.getElementById("startLngInput");
const addressInput = document.getElementById("addressInput");
const courseTypeInput = document.getElementById("courseRegisterTypeInput");

const imageUploadArea = document.getElementById("imageUploadArea");
const imageInput = document.getElementById("imageInput");
const imagePreview = document.getElementById("imagePreview");

const errorEls = {
  title: document.querySelector('[data-error-for="title"]'),
  description: document.querySelector('[data-error-for="description"]'),
  path: document.querySelector('[data-error-for="path"]'),
};

let map = null;
let mapModal = null; // 모달 지도
let polyline = null;
let startMarker = null;
let endMarker = null;
let loopMarker = null;
let startInfoWindow = null;
let endInfoWindow = null;

// 모달 마커 및 경로
let modalPolyline = null;
let modalStartMarker = null;
let modalEndMarker = null;
let modalStartInfoWindow = null;
let modalEndInfoWindow = null;
let isModalMapInitialized = false; // 모달 지도 초기화 여부

// Get Course ID from Thymeleaf or URL
function getCourseId() {
  // Thymeleaf에서 선언된 courseId 변수 사용
  if (typeof courseId !== "undefined" && courseId !== null) {
    return courseId;
  }
  // Fallback: URL에서 파싱
  const path = window.location.pathname;
  const match = path.match(/\/courseUpdate\/(\d+)/);
  return match ? match[1] : null;
}

// Get JWT Token
function getAccessToken() {
  let token = localStorage.getItem("accessToken");
  if (token) {
    return token.startsWith("Bearer ") ? token : `Bearer ${token}`;
  }
  return null;
}

function setFieldState(el, message) {
  if (!el) {
    return;
  }
  const field = el.closest(".form-field");
  if (message) {
    field?.classList.add("has-error");
    field?.classList.remove("has-valid");
  } else {
    field?.classList.remove("has-error");
    field?.classList.add("has-valid");
  }
  return field;
}

function showError(name, message) {
  if (errorEls[name]) {
    errorEls[name].textContent = message || "";
    const field =
        name === "title"
            ? titleInput
            : name === "description"
                ? descInput
                : pathInput;
    setFieldState(field, message);
  }
}

function validateTitle() {
  const value = titleInput.value.trim();
  if (!value) {
    showError("title", "제목을 입력해 주세요");
    return false;
  }
  if (value.length > 50) {
    showError("title", "제목은 50자 이내로 입력해 주세요");
    return false;
  }
  showError("title", "");
  return true;
}

function validateDescription() {
  const value = descInput.value.trim();
  if (value.length > 500) {
    showError("description", "설명은 500자 이내로 입력해 주세요");
    return false;
  }
  showError("description", "");
  return true;
}

function validateRoute() {
  if (!pathInput.value || !distanceMInput.value) {
    showError("path", "코스를 먼저 생성해 주세요");
    return false;
  }
  const distance = Number(distanceMInput.value);
  if (Number.isFinite(distance) && distance > MAX_DISTANCE_M) {
    showError(
        "path",
        "마라톤(42.195km)보다 긴 코스는 등록할 수 없습니다. 다시 작성해 주세요."
    );
    return false;
  }
  showError("path", "");
  return true;
}

function anyInvalid() {
  const checks = [validateTitle(), validateDescription(), validateRoute()];
  const ok = checks.every(Boolean);
  if (stickyBtn) {
    stickyBtn.disabled = !ok;
    stickyBtn.style.opacity = ok ? "1" : "0.5";
  }
  return !ok;
}

// ==========================
// Kakao Map (courseDetail.js와 동일)
// ==========================

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
  kakao.maps.event.addListener(map, 'dragstart', function () {
    console.log("[MAP EVENT] Drag started!");
  });

  kakao.maps.event.addListener(map, 'drag', function () {
    console.log("[MAP EVENT] Dragging...");
  });

  kakao.maps.event.addListener(map, 'dragend', function () {
    console.log("[MAP EVENT] Drag ended!");
  });

  kakao.maps.event.addListener(map, 'click', function (mouseEvent) {
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
      const allDivsInContainer = mapContainer.querySelectorAll('div');
      let mapDiv = null;

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
        mapDiv.style.removeProperty('touch-action');
        mapDiv.style.touchAction = 'auto';
        mapDiv.style.setProperty('touch-action', 'auto', 'important');

        const allDivs = mapDiv.querySelectorAll('div');
        allDivs.forEach(div => {
          div.style.removeProperty('touch-action');
          div.style.touchAction = 'auto';
          div.style.setProperty('touch-action', 'auto', 'important');
        });

        if (!mapDiv.hasAttribute('data-observer-attached')) {
          const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
              if (mutation.type === 'attributes' && mutation.attributeName
                  === 'style') {
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
        console.log("[MAP ENABLE] Map div touch-action AFTER fix:",
            computedStyle.touchAction);
      }
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

// Add marker
function addMarker(lat, lng, labelText, variant = "default") {
  const latlng = new kakao.maps.LatLng(lat, lng);

  const marker = new kakao.maps.Marker({
    position: latlng,
    clickable: true,
  });
  marker.setMap(map);

  const infoContent = `<div style="padding:8px;font-size:13px;font-weight:bold;">${labelText}</div>`;
  const infoWindow = new kakao.maps.InfoWindow({
    content: infoContent,
    removable: true,
  });

  if (variant === "start") {
    startInfoWindow = infoWindow;
  } else if (variant === "end") {
    endInfoWindow = infoWindow;
  }

  kakao.maps.event.addListener(marker, "click", function () {
    if (startInfoWindow && startInfoWindow !== infoWindow) {
      startInfoWindow.close();
    }
    if (endInfoWindow && endInfoWindow !== infoWindow) {
      endInfoWindow.close();
    }
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

  const path = coords.map((c) => new kakao.maps.LatLng(c[1], c[0]));

  polyline = new kakao.maps.Polyline({
    path: path,
    strokeWeight: 5,
    strokeColor: "#ff3d00",
    strokeOpacity: 0.8,
    strokeStyle: "solid",
  });

  polyline.setMap(map);

  setTimeout(() => {
    if (map) {
      map.relayout();
    }
  }, 0);
}

function renderRouteOnMap(coords) {
  if (!coords || coords.length < 2) {
    if (polyline) {
      polyline.setMap(null);
      polyline = null;
    }
    clearMarker(startMarker);
    clearMarker(endMarker);
    clearInfoWindows();
    return;
  }

  if (!map) {
    return;
  }

  const latLngs = coords.map(([lng, lat]) => new kakao.maps.LatLng(lat, lng));

  // Clear existing polyline
  if (polyline) {
    polyline.setMap(null);
  }

  // Draw route
  polyline = new kakao.maps.Polyline({
    path: latLngs,
    strokeWeight: 5,
    strokeColor: "#ff3d00",
    strokeOpacity: 0.9,
  });
  polyline.setMap(map);

  setTimeout(() => {
    if (map) {
      map.relayout();
    }
  }, 0);

  // Clear existing markers
  clearMarker(startMarker);
  clearMarker(endMarker);
  clearInfoWindows();

  // Add start and end markers
  const startCoord = latLngs[0];
  const endCoord = latLngs[latLngs.length - 1];

  const isRoundTrip =
      Math.abs(startCoord.getLat() - endCoord.getLat()) < 0.0001 &&
      Math.abs(startCoord.getLng() - endCoord.getLng()) < 0.0001;

  if (isRoundTrip) {
    startMarker = addMarker(
        startCoord.getLat(),
        startCoord.getLng(),
        "📍 출발점",
        "start"
    );
  } else {
    startMarker = addMarker(
        startCoord.getLat(),
        startCoord.getLng(),
        "📍 출발점",
        "start"
    );
    endMarker = addMarker(
        endCoord.getLat(),
        endCoord.getLng(),
        "🏁 도착점",
        "end"
    );
  }

  const bounds = new kakao.maps.LatLngBounds();
  latLngs.forEach((p) => bounds.extend(p));

  const sw = bounds.getSouthWest();
  const ne = bounds.getNorthEast();
  const centerLat = (sw.getLat() + ne.getLat()) / 2;
  const centerLng = (sw.getLng() + ne.getLng()) / 2;

  const latDiff = ne.getLat() - sw.getLat();
  const lngDiff = ne.getLng() - sw.getLng();
  const maxDiff = Math.max(latDiff, lngDiff);

  let level = 6;
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

  map.setCenter(new kakao.maps.LatLng(centerLat, centerLng));
  map.setLevel(level);

  map.setDraggable(true);
  map.setZoomable(true);
}

function applyDraft(draft) {
  if (!draft || !draft.path) {
    summaryEl.textContent = "아직 선택된 코스가 없습니다.";
    pathInput.value = "";
    distanceMInput.value = "";
    startLatInput.value = "";
    startLngInput.value = "";
    addressInput.value = "";
    courseTypeInput.value = "";
    renderRouteOnMap(null);
    validateRoute();
    // anyInvalid() 호출은 loadCourseData 완료 후에 실행됨
    return;
  }

  let pathToStore = draft.path;

  if (typeof pathToStore === "string") {
    console.error(
        "ERROR: draft.path is a string (WKT), not GeoJSON:",
        pathToStore.substring(0, 100)
    );
    alert("코스 경로 형식 오류가 감지되었습니다. 코스를 다시 생성해주세요.");
    applyDraft(null);
    return;
  }

  if (
      !pathToStore.coordinates ||
      !Array.isArray(pathToStore.coordinates) ||
      pathToStore.coordinates.length === 0
  ) {
    console.error("ERROR: draft.path has no valid coordinates:", pathToStore);
    applyDraft(null);
    return;
  }

  if (!pathToStore.type) {
    pathToStore.type = "LineString";
  }

  pathInput.value = JSON.stringify(pathToStore);
  if (draft.distanceM != null) {
    distanceMInput.value = draft.distanceM;
  }
  if (draft.startLat != null) {
    startLatInput.value = draft.startLat;
  }
  if (draft.startLng != null) {
    startLngInput.value = draft.startLng;
  }
  if (draft.address != null) {
    addressInput.value = draft.address;
  }
  if (draft.courseRegisterType != null) {
    courseTypeInput.value = draft.courseRegisterType;
  }

  const distanceKm =
      draft.distanceM != null ? (Number(draft.distanceM) / 1000).toFixed(2)
          : "-";
  const registerTypeText =
      draft.courseRegisterType === "AUTO"
          ? "자동 등록"
          : draft.courseRegisterType === "MANUAL"
              ? "수동 등록"
              : draft.courseRegisterType === "AI"
                  ? "AI 등록"
                  : draft.courseRegisterType ?? "-";
  summaryEl.innerHTML = `
    <div class="summary-row"><span>등록 방식</span><strong>${registerTypeText}</strong></div>
    <div class="summary-row"><span>거리</span><strong>${distanceKm} km</strong></div>
    <div class="summary-row"><span>출발지</span><strong>${
      draft.address ?? "-"
  }</strong></div>
  `;

  renderRouteOnMap(pathToStore.coordinates);
  validateRoute();
  // anyInvalid() 호출은 loadCourseData 완료 후에 실행됨 (bootstrapMap에서 호출)
}

// Load course data from API
async function loadCourseData() {
  const id = getCourseId();
  if (!id) {
    console.error("Course ID not found");
    restoreFormData();
    hydrateFromStorage();
    anyInvalid();
    return;
  }

  // 데이터 로딩 중 버튼 비활성화
  if (stickyBtn) {
    stickyBtn.disabled = true;
    stickyBtn.style.opacity = "0.5";
  }

  try {
    const token = getAccessToken();
    const headers = {
      "Content-Type": "application/json",
    };

    if (token) {
      headers["Authorization"] = token;
    }

    const response = await fetch(`/api/courses/${id}`, {
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
    console.log(
        "Register type:",
        course.registerType,
        typeof course.registerType
    );

    if (titleInput && course.title) {
      titleInput.value = course.title;
    }
    if (descInput && course.description) {
      descInput.value = course.description;
    }
    if (imagePreview && course.imageUrl) {
      const existingImg = imagePreview.querySelector("img");
      if (!existingImg) {
        const img = document.createElement("img");
        img.src = course.imageUrl;
        img.alt = "기존 코스 이미지";
        img.className = "preview-image";
        imagePreview.appendChild(img);
        imageUploadArea?.classList.add("has-preview");
      }
    }

    if (course.path) {
      let pathObj = course.path;

      let registerTypeValue = course.registerType;
      if (typeof registerTypeValue === "string") {
      } else if (registerTypeValue && typeof registerTypeValue === "object") {
        registerTypeValue = registerTypeValue.name
            ? registerTypeValue.name()
            : String(registerTypeValue);
      } else {
        registerTypeValue = "AUTO";
      }

      if (
          pathObj &&
          pathObj.coordinates &&
          Array.isArray(pathObj.coordinates) &&
          pathObj.coordinates.length > 0
      ) {
        if (!pathObj.type) {
          pathObj = {type: "LineString", coordinates: pathObj.coordinates};
        } else if (pathObj.type !== "LineString") {
          pathObj.type = "LineString";
        }

        const hasNewRoute = sessionStorage.getItem(STORAGE_KEY);
        if (hasNewRoute) {
        } else {
          applyDraft({
            path: pathObj,
            distanceM: course.distanceM,
            startLat: course.startLat,
            startLng: course.startLng,
            address: course.address,
            courseRegisterType: registerTypeValue || "AUTO",
          });
        }
      } else {
        console.warn("Path coordinates are invalid or empty");
        const hasNewRoute = sessionStorage.getItem(STORAGE_KEY);
        if (!hasNewRoute) {
          applyDraft(null);
        }
      }
    } else {
      console.warn("No path data in course");
      const hasNewRoute = sessionStorage.getItem(STORAGE_KEY);
      if (!hasNewRoute) {
        applyDraft(null);
      }
    }

    restoreFormData();

    const hasNewRoute = sessionStorage.getItem(STORAGE_KEY);
    if (hasNewRoute) {
      hydrateFromStorage();
    }

    setTimeout(() => {
      validateTitle();
      validateDescription();
      validateRoute();
      anyInvalid(); // 이제 모든 데이터가 로드되었으므로 validation 실행
    }, 100);
  } catch (error) {
    console.error("Course loading error:", error);
    alert("코스 정보를 불러오는 중 오류가 발생했습니다: " + error.message);
    hydrateFromStorage();
    anyInvalid();
  } finally {
    // 데이터 로딩 완료 후 버튼 상태 업데이트
    setTimeout(() => {
      anyInvalid();
    }, 200);
  }
}

function hydrateFromStorage() {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return;
  }
  try {
    const parsed = JSON.parse(raw);

    if (parsed.path && typeof parsed.path === "string") {
      console.error("ERROR: Stored path is WKT string, clearing storage");
      sessionStorage.removeItem(STORAGE_KEY);
      pathInput.value = "";
      distanceMInput.value = "";
      startLatInput.value = "";
      startLngInput.value = "";
      addressInput.value = "";
      courseTypeInput.value = "";
      renderRouteOnMap(null);
      validateRoute();
      anyInvalid();
      return;
    }

    applyDraft(parsed);

    sessionStorage.removeItem(STORAGE_KEY);
  } catch (err) {
    console.error("Failed to parse stored route", err);
    sessionStorage.removeItem(STORAGE_KEY);
    pathInput.value = "";
    distanceMInput.value = "";
    startLatInput.value = "";
    startLngInput.value = "";
    addressInput.value = "";
    courseTypeInput.value = "";
    renderRouteOnMap(null);
    validateRoute();
    anyInvalid();
  }
}

function handleImageUpload() {
  if (!imageInput?.files?.length) {
    imagePreview.innerHTML = "";
    imageUploadArea?.classList.remove("has-preview");
    return;
  }
  const [file] = imageInput.files;
  const frag = document.createDocumentFragment();
  const url = URL.createObjectURL(file);
  const img = document.createElement("img");
  img.src = url;
  img.alt = file.name;
  img.onload = () => URL.revokeObjectURL(url);
  frag.appendChild(img);
  imagePreview.innerHTML = "";
  imagePreview.appendChild(frag);
  imageUploadArea?.classList.add("has-preview");
}

titleInput.addEventListener("input", () => {
  validateTitle();
  anyInvalid();
});

descInput.addEventListener("input", () => {
  validateDescription();
  anyInvalid();
});

form.addEventListener("submit", async (e) => {
  e.preventDefault();

  console.log("[SUBMIT] Form submitted");
  console.log("[SUBMIT] pathInput.value:",
      pathInput.value ? "exists" : "empty");
  console.log("[SUBMIT] distanceMInput.value:", distanceMInput.value);
  console.log("[SUBMIT] courseTypeInput.value:", courseTypeInput.value);
  console.log("[SUBMIT] addressInput.value:", addressInput.value);

  if (anyInvalid()) {
    console.error("[SUBMIT] Validation failed");
    return;
  }

  const id = getCourseId();
  if (!id) {
    alert("코스 ID를 찾을 수 없습니다");
    return;
  }

  const titleValue = titleInput.value.trim();
  const descriptionValue = descInput.value.trim();
  const addressValue = addressInput.value.trim();
  const registerTypeValue = courseTypeInput.value || "AUTO";

  let pathObj = null;
  if (pathInput.value) {
    try {
      pathObj =
          typeof pathInput.value === "string"
              ? JSON.parse(pathInput.value)
              : pathInput.value;

      if (!pathObj.type) {
        pathObj = {
          type: "LineString",
          coordinates: pathObj.coordinates || pathObj,
        };
      }
      if (pathObj.type !== "LineString") {
        pathObj.type = "LineString";
      }
    } catch (e) {
      console.error("Error parsing path:", e);
      alert("코스 경로 형식이 올바르지 않습니다");
      return;
    }
  }

  if (
      !pathObj ||
      !pathObj.coordinates ||
      !Array.isArray(pathObj.coordinates) ||
      pathObj.coordinates.length === 0
  ) {
    alert("코스 경로를 생성해주세요");
    return;
  }

  if (!pathObj.type) {
    pathObj.type = "LineString";
  }

  const distanceM = distanceMInput.value
      ? Math.round(Number(distanceMInput.value))
      : null;

  const startLat = parseFloat(startLatInput.value) || null;
  const startLng = parseFloat(startLngInput.value) || null;

  const dto = {
    title: titleValue,
    description: descriptionValue || "", // 빈 문자열 허용
    path: JSON.stringify({
      type: "LineString",
      coordinates: pathObj.coordinates,
    }),
    distanceM: distanceM,
    startLat: startLat,
    startLng: startLng,
    address: addressValue,
    courseRegisterType: registerTypeValue,
  };

  console.log("[SUBMIT] Sending DTO:", dto);
  console.log("[SUBMIT] courseRegisterType value:", registerTypeValue);

  const formData = new FormData();
  // File 객체로 변환하여 파일명 명시 (Spring이 @RequestPart로 제대로 파싱하도록)
  formData.append(
      "dto",
      new File([JSON.stringify(dto)], "dto.json", {type: "application/json"})
  );

  if (imageInput && imageInput.files && imageInput.files.length > 0) {
    formData.append("imageFile", imageInput.files[0]);
    console.log("[SUBMIT] Image file attached:", imageInput.files[0].name);
  } else {
    console.log("[SUBMIT] No image file attached");
  }

  try {
    const token = getAccessToken();
    if (!token) {
      alert("로그인이 필요합니다");
      window.location.href = "/login";
      return;
    }

    const response = await fetch(`/api/courses/${id}`, {
      method: "PUT",
      headers: {
        Authorization: token,
      },
      body: formData,
    });

    if (!response.ok) {
      let errorMessage = "알 수 없는 오류";
      try {
        const errorText = await response.text();
        console.error("Error response text:", errorText);

        try {
          const errorData = JSON.parse(errorText);
          errorMessage =
              errorData.message ||
              errorData.code ||
              errorData.error ||
              errorMessage;
          console.error("Error response (parsed):", errorData);
        } catch (parseError) {
          errorMessage = errorText || errorMessage;
          console.error("Error response (raw):", errorText);
        }
      } catch (e) {
        console.error("Error parsing error response:", e);
        errorMessage = `HTTP ${response.status}: ${response.statusText}`;
      }
      alert("코스 수정 실패: " + errorMessage);
      return;
    }

    const result = await response.json();
    console.log("Update success:", result);
    alert("코스가 수정되었습니다");
    window.location.href = `/courseDetail/${id}`;
  } catch (error) {
    console.error("Course update error:", error);
    const errorMessage = error.message || "알 수 없는 오류가 발생했습니다";
    alert("코스 수정 중 오류가 발생했습니다: " + errorMessage);
  }
});

const FORM_DATA_KEY = "courseFormData";

function saveFormData() {
  const formData = {
    title: titleInput.value.trim(),
    description: descInput.value.trim(),
    hasImage: imageInput && imageInput.files && imageInput.files.length > 0,
    imageUrl: null,
  };

  if (formData.hasImage && imagePreview) {
    const img = imagePreview.querySelector("img");
    if (img && img.src) {
      formData.imageUrl = img.src;
    }
  }

  sessionStorage.setItem(FORM_DATA_KEY, JSON.stringify(formData));
}

function restoreFormData() {
  const raw = sessionStorage.getItem(FORM_DATA_KEY);
  if (!raw) {
    return;
  }

  try {
    const formData = JSON.parse(raw);

    if (formData.title && titleInput) {
      titleInput.value = formData.title;
      validateTitle();
    }

    if (formData.description !== undefined && descInput) {
      descInput.value = formData.description;
      validateDescription();
    }

    sessionStorage.removeItem(FORM_DATA_KEY);
  } catch (err) {
    console.error("Failed to restore form data:", err);
    sessionStorage.removeItem(FORM_DATA_KEY);
  }
}

autoRouteBtn?.addEventListener("click", () => {
  saveFormData();
  const currentDraft = {
    path: pathInput.value ? JSON.parse(pathInput.value) : null,
    distanceM: distanceMInput.value || null,
    startLat: startLatInput.value || null,
    startLng: startLngInput.value || null,
    address: addressInput.value || null,
    courseRegisterType: courseTypeInput.value || null,
  };
  if (currentDraft.path) {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(currentDraft));
  }
  const courseId = getCourseId();
  if (courseId) {
    sessionStorage.setItem("returnPage", `/courseUpdate/${courseId}`);
  } else {
    sessionStorage.setItem("returnPage", "/courseUpdate");
  }
  window.location.href = "/course_auto";
});

manualRouteBtn?.addEventListener("click", () => {
  saveFormData();
  const currentDraft = {
    path: pathInput.value ? JSON.parse(pathInput.value) : null,
    distanceM: distanceMInput.value || null,
    startLat: startLatInput.value || null,
    startLng: startLngInput.value || null,
    address: addressInput.value || null,
    courseRegisterType: courseTypeInput.value || null,
  };
  if (currentDraft.path) {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(currentDraft));
  }
  const courseId = getCourseId();
  if (courseId) {
    sessionStorage.setItem("returnPage", `/courseUpdate/${courseId}`);
  } else {
    sessionStorage.setItem("returnPage", "/courseUpdate");
  }
  window.location.href = "/course_manual";
});

// 이미지 업로드 처리 - 미리보기가 있으면 클릭 방지
const uploadPlaceholder = document.getElementById("uploadPlaceholder");

uploadPlaceholder?.addEventListener("click", () => {
  imageInput?.click();
});

imageInput?.addEventListener("change", handleImageUpload);

// ==========================
// Map Modal Functions
// ==========================

function openMapModal() {
  console.log("[MAP MODAL] Opening map modal...");
  const modal = document.getElementById("mapModal");
  if (modal) {
    modal.style.visibility = "visible";
    modal.style.opacity = "1";

    if (!isModalMapInitialized) {
      console.log("[MAP MODAL] First time opening, initializing...");
      setTimeout(() => {
        initModalMap();
        const pathValue = pathInput.value;
        if (pathValue) {
          try {
            const pathObj = typeof pathValue === "string" ? JSON.parse(
                pathValue) : pathValue;
            if (pathObj && pathObj.coordinates) {
              displayCourseOnModalMap(pathObj.coordinates);
            }
          } catch (e) {
            console.error("[MAP MODAL] Error parsing path:", e);
          }
        }
        isModalMapInitialized = true;
      }, 100);
    } else {
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

function closeMapModal() {
  console.log("[MAP MODAL] Closing map modal...");
  const modal = document.getElementById("mapModal");
  if (modal) {
    modal.style.visibility = "hidden";
    modal.style.opacity = "0";
  }
}

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

function displayCourseOnModalMap(pathCoords) {
  console.log("[MAP MODAL] Displaying course on modal map...");

  if (!mapModal) {
    console.error("[MAP MODAL ERROR] Modal map not initialized!");
    return;
  }

  if (!pathCoords || pathCoords.length === 0) {
    console.warn("[MAP MODAL] No path coordinates found");
    return;
  }

  const startCoord = pathCoords[0];
  const endCoord = pathCoords[pathCoords.length - 1];
  const startLat = startCoord[1];
  const startLng = startCoord[0];
  const endLat = endCoord[1];
  const endLng = endCoord[0];

  const isRoundTrip =
      Math.abs(startLat - endLat) < 0.0001 &&
      Math.abs(startLng - endLng) < 0.0001;

  const displayStartLat = parseFloat(startLatInput.value) || startLat;
  const displayStartLng = parseFloat(startLngInput.value) || startLng;

  if (modalStartMarker) {
    modalStartMarker.setMap(null);
  }
  if (modalEndMarker) {
    modalEndMarker.setMap(null);
  }
  if (modalPolyline) {
    modalPolyline.setMap(null);
  }
  if (modalStartInfoWindow) {
    modalStartInfoWindow.close();
  }
  if (modalEndInfoWindow) {
    modalEndInfoWindow.close();
  }

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
    if (modalEndInfoWindow) {
      modalEndInfoWindow.close();
    }
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
      if (modalStartInfoWindow) {
        modalStartInfoWindow.close();
      }
      modalEndInfoWindow.open(mapModal, modalEndMarker);
    });
  }

  const path = pathCoords.map((c) => new kakao.maps.LatLng(c[1], c[0]));
  modalPolyline = new kakao.maps.Polyline({
    path: path,
    strokeWeight: 5,
    strokeColor: "#ff3d00",
    strokeOpacity: 0.8,
    strokeStyle: "solid",
  });
  modalPolyline.setMap(mapModal);

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

// Initialize on page load
function bootstrapMap() {
  console.log("[BOOTSTRAP] Starting map bootstrap...");

  if (!mapContainer) {
    console.error("[BOOTSTRAP ERROR] Map container not found!");
    return;
  }
  console.log("[BOOTSTRAP] Map container found:", mapContainer);

  if (typeof kakao === "undefined" || !kakao.maps) {
    console.log("[BOOTSTRAP] Waiting for Kakao Maps SDK to load...");
    setTimeout(bootstrapMap, 100);
    return;
  }
  console.log("[BOOTSTRAP] Kakao Maps SDK loaded!");

  console.log("[BOOTSTRAP] Initializing map...");
  initMap();

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

const closeMapModalBtn = document.getElementById("closeMapModal");
const mapModalOverlay = document.getElementById("mapModal");

if (closeMapModalBtn) {
  closeMapModalBtn.addEventListener("click", closeMapModal);
}

if (mapModalOverlay) {
  mapModalOverlay.addEventListener("click", (e) => {
    if (e.target === mapModalOverlay) {
      closeMapModal();
    }
  });
}

if (mapContainer) {
  mapContainer.addEventListener("click", () => {
    if (pathInput.value) {
      openMapModal();
    }
  });
  mapContainer.style.cursor = "pointer";
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", () => {
    if (typeof kakao !== "undefined" && kakao.maps) {
      bootstrapMap();
    } else {
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

anyInvalid();
