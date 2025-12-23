const MAX_DISTANCE_M = 42195;
const STORAGE_KEY = "courseDraft";

const form = document.getElementById("courseForm");
const stickyBtn = document.getElementById("stickyCreateBtn");
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

let previewStartMarker = null;
let previewEndMarker = null;

function initMapPreview() {
  if (previewMap || !mapPreviewEl || typeof kakao === "undefined") {
    return;
  }
  const center = new kakao.maps.LatLng(37.5665, 126.978);
  previewMap = new kakao.maps.Map(mapPreviewEl, {
    center,
    level: 6,
    draggable: true, // 지도 드래그 가능
    scrollwheel: true, // 마우스 휠로 확대/축소 가능
  });
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
  if (!previewMap) {
    return;
  }

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
  summaryEl.innerHTML = `
    <div class="summary-row"><span>등록 방식</span><strong>${
      draft.courseRegisterType ?? "-"
    }</strong></div>
    <div class="summary-row"><span>거리</span><strong>${distanceKm} km</strong></div>
    <div class="summary-row"><span>출발지</span><strong>${
      draft.address ?? "-"
    }</strong></div>
  `;

  renderRouteOnMap(draft.path.coordinates);
  validateRoute();
  anyInvalid();
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
  } finally {
    sessionStorage.removeItem(STORAGE_KEY);
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
  if (anyInvalid()) {
    e.preventDefault();
    e.stopPropagation();
    return;
  }

  e.preventDefault();

  // Get form values
  const titleValue = titleInput.value.trim();
  const descriptionValue = descInput.value.trim();
  const addressValue = addressInput.value.trim();
  const registerTypeValue = courseTypeInput.value || "AUTO";

  // Get path - must be GeoJSON format: { type: "LineString", coordinates: [...] }
  let pathObj = null;
  if (pathInput.value) {
    const pathValue = pathInput.value.trim();

    // Check if it's already a WKT string (starts with "LINESTRING")
    if (
      pathValue.startsWith("LINESTRING") ||
      pathValue.startsWith("linestring")
    ) {
      console.error(
        "ERROR: pathInput contains WKT string, not GeoJSON:",
        pathValue.substring(0, 100)
      );
      alert(
        "코스 경로 형식 오류: WKT 형식이 감지되었습니다. 코스를 다시 생성해주세요."
      );
      return;
    }

    try {
      const parsed = JSON.parse(pathValue);

      // Ensure it's a GeoJSON object, not a string
      if (typeof parsed === "string") {
        console.error(
          "Path is a string, not GeoJSON:",
          parsed.substring(0, 100)
        );
        alert("코스 경로 형식이 올바르지 않습니다");
        return;
      }

      pathObj = parsed;
    } catch (e) {
      console.error(
        "Error parsing path:",
        e,
        "pathInput.value (first 200 chars):",
        pathValue.substring(0, 200)
      );
      alert("코스 경로 형식이 올바르지 않습니다. 코스를 다시 생성해주세요.");
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

  // Ensure pathObj has type field and is proper GeoJSON
  if (!pathObj.type) {
    pathObj.type = "LineString";
  }

  // Validate GeoJSON structure
  if (pathObj.type !== "LineString") {
    console.error("Invalid path type:", pathObj.type);
    alert("코스 경로 형식이 올바르지 않습니다");
    return;
  }

  // Final validation: ensure coordinates is an array of [lng, lat] pairs
  if (!Array.isArray(pathObj.coordinates) || pathObj.coordinates.length < 2) {
    console.error("Invalid coordinates:", pathObj.coordinates);
    alert("코스 경로에 충분한 좌표가 없습니다");
    return;
  }

  // Log for debugging
  console.log("Sending path object (GeoJSON):", {
    type: pathObj.type,
    coordinatesCount: pathObj.coordinates.length,
    firstCoord: pathObj.coordinates[0],
    lastCoord: pathObj.coordinates[pathObj.coordinates.length - 1],
  });

  // Get distance
  const distanceM = distanceMInput.value
    ? Math.round(Number(distanceMInput.value))
    : null;

  // Get coordinates
  const startLat = parseFloat(startLatInput.value) || null;
  const startLng = parseFloat(startLngInput.value) || null;

  // Final validation: ensure pathObj is a proper GeoJSON object
  if (typeof pathObj !== "object" || pathObj === null) {
    console.error("ERROR: pathObj is not an object:", typeof pathObj, pathObj);
    alert("코스 경로 형식 오류가 발생했습니다.");
    return;
  }

  if (typeof pathObj === "string") {
    console.error(
      "ERROR: pathObj is a string (WKT):",
      pathObj.substring(0, 100)
    );
    alert("코스 경로가 WKT 형식입니다. 코스를 다시 생성해주세요.");
    return;
  }

  // Ensure path is a GeoJSON object, not a string
  if (!pathObj.type || pathObj.type !== "LineString") {
    console.error("ERROR: pathObj.type is invalid:", pathObj.type);
    alert("코스 경로 형식이 올바르지 않습니다.");
    return;
  }

  if (!Array.isArray(pathObj.coordinates) || pathObj.coordinates.length < 2) {
    console.error(
      "ERROR: pathObj.coordinates is invalid:",
      pathObj.coordinates
    );
    alert("코스 경로에 충분한 좌표가 없습니다.");
    return;
  }

  // Build course object with path as stringified JSON
  const course = {
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

  // Add course as JSON blob
  formData.append(
    "course",
    new Blob([JSON.stringify(course)], { type: "application/json" })
  );

  // Add image file if exists
  if (imageInput && imageInput.files && imageInput.files.length > 0) {
    formData.append("imageFile", imageInput.files[0]);
  }

  try {
    const token = localStorage.getItem("accessToken");
    if (!token) {
      alert("로그인이 필요합니다");
      window.location.href = "/login";
      return;
    }

    // Use XMLHttpRequest instead of fetch to ensure proper Content-Type handling
    const xhr = new XMLHttpRequest();
    xhr.open("POST", "/api/courses", true);
    xhr.setRequestHeader(
      "Authorization",
      token.startsWith("Bearer ") ? token : `Bearer ${token}`
    );

    // Don't set Content-Type header - let browser set it with boundary for multipart/form-data
    // xhr.setRequestHeader("Content-Type", "multipart/form-data"); // ❌ Don't do this

    xhr.onload = function () {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const result = JSON.parse(xhr.responseText);
          console.log("Create success:", result);
          alert("코스가 생성되었습니다");
          window.location.href = "/course";
        } catch (e) {
          console.error("Parse error:", e);
          alert("코스 생성 중 오류가 발생했습니다");
        }
      } else {
        let errorMessage = "알 수 없는 오류";
        try {
          const errorData = JSON.parse(xhr.responseText);
          // 여러 필드에서 에러 메시지 추출 시도
          errorMessage =
            errorData.message ||
            errorData.error ||
            errorData.code ||
            errorData.detail ||
            errorMessage;
          console.error("Error response:", errorData);
        } catch (e) {
          console.error("Error response (raw):", xhr.responseText);
          // JSON이 아닌 경우 raw 텍스트 사용
          errorMessage =
            xhr.responseText ||
            `HTTP ${xhr.status}: ${xhr.statusText}` ||
            errorMessage;
        }
        alert("코스 생성 실패: " + errorMessage);
      }
    };

    xhr.onerror = function () {
      console.error("Network error");
      alert("코스 생성 중 네트워크 오류가 발생했습니다");
    };

    xhr.send(formData);
  } catch (error) {
    console.error("Course create error:", error);
    alert("코스 생성 중 오류가 발생했습니다: " + error.message);
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
    imageUrl: null, // 이미지는 복원 불가능하므로 URL만 저장
  };

  // 이미지가 있으면 미리보기 URL 저장 (복원은 불가능하지만 참고용)
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
  if (!raw) {
    return;
  }

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

    // 이미지는 복원 불가능 (파일 객체는 저장할 수 없음)
    // 하지만 사용자가 다시 선택할 수 있도록 안내는 것은 나중에 추가 가능

    // 복원 후 저장 데이터 삭제 (한 번만 복원)
    sessionStorage.removeItem(FORM_DATA_KEY);
  } catch (err) {
    console.error("Failed to restore form data:", err);
    sessionStorage.removeItem(FORM_DATA_KEY);
  }
}

autoRouteBtn?.addEventListener("click", () => {
  saveFormData(); // 현재 입력값 저장
  sessionStorage.removeItem(STORAGE_KEY);
  // 현재 페이지 정보 저장 (코스 적용 후 돌아올 페이지)
  sessionStorage.setItem("returnPage", "/courseCreate");
  window.location.href = "/course_auto";
});

manualRouteBtn?.addEventListener("click", () => {
  saveFormData(); // 현재 입력값 저장
  sessionStorage.removeItem(STORAGE_KEY);
  // 현재 페이지 정보 저장 (코스 적용 후 돌아올 페이지)
  sessionStorage.setItem("returnPage", "/courseCreate");
  window.location.href = "/course_manual";
});

imageUploadArea?.addEventListener("click", () => imageInput?.click());
imageInput?.addEventListener("change", handleImageUpload);

// 페이지 로드 시: 먼저 저장된 입력값 복원, 그 다음 경로 정보 적용
restoreFormData();
hydrateFromStorage();
anyInvalid();
