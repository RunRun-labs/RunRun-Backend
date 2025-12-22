const MAX_DISTANCE_M = 42195;
const STORAGE_KEY = "courseDraft";

const form = document.getElementById("courseForm");
const stickyBtn = document.getElementById("stickyUpdateBtn");
const autoRouteBtn = document.getElementById("autoRouteBtn");
const manualRouteBtn = document.getElementById("manualRouteBtn");
const summaryEl = document.getElementById("routeSummary");
const mapPreviewEl = document.getElementById("mapPreview");

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

let previewMap = null;
let previewPolyline = null;

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
  if (!el) return;
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

let previewStartMarker = null;
let previewEndMarker = null;

function initMapPreview() {
  if (previewMap || !mapPreviewEl || typeof kakao === "undefined") return;
  const center = new kakao.maps.LatLng(37.5665, 126.978);
  previewMap = new kakao.maps.Map(mapPreviewEl, {
    center,
    level: 6,
    draggable: true,
    scrollwheel: true,
  });

  // ⭐ 지도 초기화 직후 무조건 relayout (여러 번 호출하여 확실하게)
  setTimeout(() => {
    if (previewMap) {
      previewMap.relayout();
      previewMap.setDraggable(true);
      previewMap.setZoomable(true);
    }
  }, 0);

  setTimeout(() => {
    if (previewMap) {
      previewMap.relayout();
      previewMap.setDraggable(true);
      previewMap.setZoomable(true);
    }
  }, 100);

  setTimeout(() => {
    if (previewMap) {
      previewMap.relayout();
      previewMap.setDraggable(true);
      previewMap.setZoomable(true);
    }
  }, 300);

  setTimeout(() => {
    if (previewMap) {
      previewMap.relayout();
      previewMap.setDraggable(true);
      previewMap.setZoomable(true);
    }
  }, 500);
}

function clearPreviewMarkers() {
  if (previewStartMarker) {
    previewStartMarker.setMap(null);
    previewStartMarker = null;
  }
  if (previewEndMarker) {
    previewEndMarker.setMap(null);
    previewEndMarker = null;
  }
}

function addPreviewMarker(lat, lng, labelText, variant = "default") {
  const latlng = new kakao.maps.LatLng(lat, lng);
  const palette = {
    start: "#1e88e5",
    end: "#ff3d00",
    default: "#333333",
  };
  const color = palette[variant] || palette.default;

  const content = `
    <div style="transform:translate(-50%,-100%);text-align:center;">
      <div style="
        padding:4px 10px;
        border-radius:14px;
        background:${color};
        color:#fff;
        font-size:11px;
        font-weight:bold;
        white-space:nowrap;
        box-shadow:0 2px 4px rgba(0,0,0,0.3);
      ">${labelText}</div>
      <div style="
        width:0;
        height:0;
        border-left:6px solid transparent;
        border-right:6px solid transparent;
        border-top:8px solid ${color};
        margin:0 auto;
      "></div>
    </div>
  `;

  const marker = new kakao.maps.CustomOverlay({
    position: latlng,
    content: content,
    yAnchor: 1,
  });

  marker.setMap(previewMap);
  return marker;
}

function renderRouteOnMap(coords) {
  if (!coords || coords.length < 2) {
    if (previewPolyline) {
      previewPolyline.setMap(null);
      previewPolyline = null;
    }
    clearPreviewMarkers();
    return;
  }
  initMapPreview();
  const latLngs = coords.map(([lng, lat]) => new kakao.maps.LatLng(lat, lng));
  if (!previewMap) return;

  // Clear existing polyline
  if (previewPolyline) {
    previewPolyline.setMap(null);
  }

  // Draw route
  previewPolyline = new kakao.maps.Polyline({
    path: latLngs,
    strokeWeight: 5,
    strokeColor: "#ff3d00",
    strokeOpacity: 0.9,
  });
  previewPolyline.setMap(previewMap);

  // ⭐ 경로 그린 뒤에도 relayout 필수
  setTimeout(() => {
    if (previewMap) {
      previewMap.relayout();
    }
  }, 0);

  // Clear existing markers
  clearPreviewMarkers();

  // Add start and end markers
  const startCoord = latLngs[0];
  const endCoord = latLngs[latLngs.length - 1];

  // Check if round trip (start and end are the same)
  const isRoundTrip =
    Math.abs(startCoord.getLat() - endCoord.getLat()) < 0.0001 &&
    Math.abs(startCoord.getLng() - endCoord.getLng()) < 0.0001;

  if (isRoundTrip) {
    // 루프 코스일 때는 출발점만 표시
    previewStartMarker = addPreviewMarker(
      startCoord.getLat(),
      startCoord.getLng(),
      "출발",
      "start"
    );
  } else {
    previewStartMarker = addPreviewMarker(
      startCoord.getLat(),
      startCoord.getLng(),
      "출발",
      "start"
    );
    previewEndMarker = addPreviewMarker(
      endCoord.getLat(),
      endCoord.getLng(),
      "도착",
      "end"
    );
  }

  const bounds = new kakao.maps.LatLngBounds();
  latLngs.forEach((p) => bounds.extend(p));
  previewMap.setBounds(bounds);

  // ⭐ 경로 그린 뒤 + setBounds 이후 relayout 필수
  setTimeout(() => {
    if (previewMap) {
      previewMap.relayout();
    }
  }, 0);

  setTimeout(() => {
    if (previewMap) {
      previewMap.relayout();
    }
  }, 300);

  // ⭐ 마커 추가 후 + 전체 작업 완료 후 최종 relayout
  setTimeout(() => {
    if (previewMap) {
      previewMap.relayout();
      // 드래그/줌 기능 명시적 재활성화
      previewMap.setDraggable(true);
      previewMap.setZoomable(true);
    }
  }, 500);
}

function applyDraft(draft) {
  if (!draft || !draft.path) {
    // 경로가 없을 때는 경로 관련 필드만 초기화 (제목, 설명, 이미지는 유지)
    summaryEl.textContent = "아직 선택된 코스가 없습니다.";
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

  // Validate and normalize path
  let pathToStore = draft.path;

  // If path is a string (WKT), reject it
  if (typeof pathToStore === "string") {
    console.error(
      "ERROR: draft.path is a string (WKT), not GeoJSON:",
      pathToStore.substring(0, 100)
    );
    alert("코스 경로 형식 오류가 감지되었습니다. 코스를 다시 생성해주세요.");
    applyDraft(null);
    return;
  }

  // Ensure it's a GeoJSON object with coordinates
  if (
    !pathToStore.coordinates ||
    !Array.isArray(pathToStore.coordinates) ||
    pathToStore.coordinates.length === 0
  ) {
    console.error("ERROR: draft.path has no valid coordinates:", pathToStore);
    applyDraft(null);
    return;
  }

  // Ensure type field exists
  if (!pathToStore.type) {
    pathToStore.type = "LineString";
  }

  // 경로 관련 필드만 업데이트 (제목, 설명, 이미지는 유지)
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
    draft.distanceM != null ? (Number(draft.distanceM) / 1000).toFixed(2) : "-";
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
  anyInvalid();
}

// Load course data from API
async function loadCourseData() {
  const id = getCourseId();
  if (!id) {
    console.error("Course ID not found");
    // 경로 수정 후 돌아온 경우 저장된 제목/설명 복원
    restoreFormData();
    hydrateFromStorage();
    anyInvalid();
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

    const response = await fetch(`/api/routes/${id}`, {
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

    // Fill form fields
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

    // Load path data
    if (course.path) {
      // path는 이미 Map<String, Object> (GeoJSON) 형태로 올 것
      let pathObj = course.path;

      // registerType은 enum이므로 문자열로 올 수 있음 ("AUTO", "MANUAL", "AI")
      let registerTypeValue = course.registerType;
      if (typeof registerTypeValue === "string") {
        // 이미 문자열
      } else if (registerTypeValue && typeof registerTypeValue === "object") {
        // 객체인 경우 name() 메서드나 toString() 사용
        registerTypeValue = registerTypeValue.name
          ? registerTypeValue.name()
          : String(registerTypeValue);
      } else {
        registerTypeValue = "AUTO";
      }

      // path.coordinates 확인 및 GeoJSON 형식 보장
      if (
        pathObj &&
        pathObj.coordinates &&
        Array.isArray(pathObj.coordinates) &&
        pathObj.coordinates.length > 0
      ) {
        // Ensure GeoJSON format: { type: "LineString", coordinates: [...] }
        if (!pathObj.type) {
          pathObj = { type: "LineString", coordinates: pathObj.coordinates };
        } else if (pathObj.type !== "LineString") {
          pathObj.type = "LineString";
        }

        // ⭐ sessionStorage에 새로운 경로가 있으면 우선 적용, 없으면 API 데이터 적용
        const hasNewRoute = sessionStorage.getItem(STORAGE_KEY);
        if (hasNewRoute) {
          // 새로운 경로가 있으면 나중에 hydrateFromStorage()에서 적용
          // 여기서는 제목/설명/이미지만 설정
        } else {
          // 새로운 경로가 없으면 API 데이터 적용
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
        // ⭐ sessionStorage에 새로운 경로가 있으면 우선 적용
        const hasNewRoute = sessionStorage.getItem(STORAGE_KEY);
        if (!hasNewRoute) {
          applyDraft(null);
        }
      }
    } else {
      console.warn("No path data in course");
      // ⭐ sessionStorage에 새로운 경로가 있으면 우선 적용
      const hasNewRoute = sessionStorage.getItem(STORAGE_KEY);
      if (!hasNewRoute) {
        applyDraft(null);
      }
    }

    // 경로 수정 후 돌아온 경우 저장된 제목/설명 복원
    restoreFormData();

    // ⭐ 경로 수정 후 돌아온 경우 sessionStorage의 새로운 경로 데이터 우선 적용
    // loadCourseData() 완료 후 hydrateFromStorage() 호출하여 새로운 경로 적용
    const hasNewRoute = sessionStorage.getItem(STORAGE_KEY);
    if (hasNewRoute) {
      // sessionStorage에 새로운 경로가 있으면 우선 적용 (API 데이터를 덮어씀)
      hydrateFromStorage();
    }

    // Trigger validation after data is loaded
    setTimeout(() => {
      validateTitle();
      validateDescription();
      anyInvalid();
    }, 100);
  } catch (error) {
    console.error("Course loading error:", error);
    alert("코스 정보를 불러오는 중 오류가 발생했습니다: " + error.message);
    hydrateFromStorage();
    anyInvalid();
  }
}

function hydrateFromStorage() {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  if (!raw) {
    // 경로가 없으면 아무것도 하지 않음 (기존 입력값 유지)
    return;
  }
  try {
    const parsed = JSON.parse(raw);

    // Validate that path is GeoJSON, not WKT string
    if (parsed.path && typeof parsed.path === "string") {
      console.error("ERROR: Stored path is WKT string, clearing storage");
      sessionStorage.removeItem(STORAGE_KEY);
      // 경로만 초기화 (제목, 설명, 이미지는 유지)
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

    // 경로 정보만 적용 (제목, 설명, 이미지는 유지)
    applyDraft(parsed);

    // ⭐ 경로 적용 성공 후 sessionStorage 삭제
    sessionStorage.removeItem(STORAGE_KEY);
  } catch (err) {
    console.error("Failed to parse stored route", err);
    sessionStorage.removeItem(STORAGE_KEY);
    // 경로만 초기화 (제목, 설명, 이미지는 유지)
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

  if (anyInvalid()) {
    return;
  }

  const id = getCourseId();
  if (!id) {
    alert("코스 ID를 찾을 수 없습니다");
    return;
  }

  // Get form values
  const titleValue = titleInput.value.trim();
  const descriptionValue = descInput.value.trim();
  const addressValue = addressInput.value.trim();
  const registerTypeValue = courseTypeInput.value || "AUTO";

  // Get path - must be GeoJSON format: { type: "LineString", coordinates: [...] }
  let pathObj = null;
  if (pathInput.value) {
    try {
      pathObj =
        typeof pathInput.value === "string"
          ? JSON.parse(pathInput.value)
          : pathInput.value;

      // Ensure it's in GeoJSON format
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

  // Ensure pathObj has type field
  if (!pathObj.type) {
    pathObj.type = "LineString";
  }

  // Get distance
  const distanceM = distanceMInput.value
    ? Math.round(Number(distanceMInput.value))
    : null;

  // Get coordinates
  const startLat = parseFloat(startLatInput.value) || null;
  const startLng = parseFloat(startLngInput.value) || null;

  // Build DTO object with path as stringified JSON
  const dto = {
    title: titleValue,
    description: descriptionValue,
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

  // Create FormData
  const formData = new FormData();
  formData.append(
    "dto",
    new Blob([JSON.stringify(dto)], { type: "application/json" })
  );

  // Add image file if selected
  if (imageInput && imageInput.files && imageInput.files.length > 0) {
    formData.append("imageFile", imageInput.files[0]);
  }

  try {
    const token = getAccessToken();
    if (!token) {
      alert("로그인이 필요합니다");
      window.location.href = "/login";
      return;
    }

    const response = await fetch(`/api/routes/${id}`, {
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

        // Try to parse as JSON
        try {
          const errorData = JSON.parse(errorText);
          errorMessage =
            errorData.message ||
            errorData.code ||
            errorData.error ||
            errorMessage;
          console.error("Error response (parsed):", errorData);
        } catch (parseError) {
          // If not JSON, use the raw text
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
    // Network error or other errors
    const errorMessage = error.message || "알 수 없는 오류가 발생했습니다";
    alert("코스 수정 중 오류가 발생했습니다: " + errorMessage);
  }
});

// 제목/설명/이미지 저장 키
const FORM_DATA_KEY = "courseFormData";

// 현재 입력값 저장
function saveFormData() {
  const formData = {
    title: titleInput.value.trim(),
    description: descInput.value.trim(),
    hasImage: imageInput && imageInput.files && imageInput.files.length > 0,
    imageUrl: null,
  };

  // 이미지가 있으면 미리보기 URL 저장
  if (formData.hasImage && imagePreview) {
    const img = imagePreview.querySelector("img");
    if (img && img.src) {
      formData.imageUrl = img.src;
    }
  }

  sessionStorage.setItem(FORM_DATA_KEY, JSON.stringify(formData));
}

// 저장된 입력값 복원
function restoreFormData() {
  const raw = sessionStorage.getItem(FORM_DATA_KEY);
  if (!raw) return;

  try {
    const formData = JSON.parse(raw);

    // 제목 복원
    if (formData.title && titleInput) {
      titleInput.value = formData.title;
      validateTitle();
    }

    // 설명 복원
    if (formData.description !== undefined && descInput) {
      descInput.value = formData.description;
      validateDescription();
    }

    // 복원 후 저장 데이터 삭제
    sessionStorage.removeItem(FORM_DATA_KEY);
  } catch (err) {
    console.error("Failed to restore form data:", err);
    sessionStorage.removeItem(FORM_DATA_KEY);
  }
}

autoRouteBtn?.addEventListener("click", () => {
  saveFormData(); // 현재 입력값 저장
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
  // 현재 페이지 정보 저장 (코스 적용 후 돌아올 페이지)
  const courseId = getCourseId();
  if (courseId) {
    sessionStorage.setItem("returnPage", `/courseUpdate/${courseId}`);
  } else {
    sessionStorage.setItem("returnPage", "/courseUpdate");
  }
  window.location.href = "/test";
});

manualRouteBtn?.addEventListener("click", () => {
  saveFormData(); // 현재 입력값 저장
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
  // 현재 페이지 정보 저장 (코스 적용 후 돌아올 페이지)
  const courseId = getCourseId();
  if (courseId) {
    sessionStorage.setItem("returnPage", `/courseUpdate/${courseId}`);
  } else {
    sessionStorage.setItem("returnPage", "/courseUpdate");
  }
  window.location.href = "/test2";
});

imageUploadArea?.addEventListener("click", () => imageInput?.click());
imageInput?.addEventListener("change", handleImageUpload);

// Initialize on page load
// 맵을 먼저 초기화하여 경로를 그리기 전에도 상호작용 가능하도록
if (typeof kakao !== "undefined" && kakao.maps) {
  initMapPreview();
} else {
  window.addEventListener("load", () => {
    if (typeof kakao !== "undefined" && kakao.maps) {
      initMapPreview();
    }
  });
}

// 기존 코스 데이터 로드 (완료 후 저장된 제목/설명 복원)
setTimeout(() => {
  loadCourseData();
}, 600);
anyInvalid();
