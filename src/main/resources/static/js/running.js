// ==========================
// DOM Elements
// ==========================
const mapContainer = document.getElementById("map");
const backButton = document.getElementById("backButton");
const chatButton = document.getElementById("chatButton");
const startRunningButton = document.getElementById("startRunningButton");
const locateButton = document.getElementById("locateButton");
const pageTitleEl = document.querySelector(".page-title");
const distanceValue = document.getElementById("distanceValue");
const paceValue = document.getElementById("paceValue");
const timeValue = document.getElementById("timeValue");
const mapContainerEl = document.querySelector(".map-container");
const runningResultModal = document.getElementById(
  "running-result-modal-overlay"
);
const runningResultClose = document.getElementById("running-result-close");
const runningResultGoChat = document.getElementById("result-go-chat");
const resultDistanceEl = document.getElementById("result-distance");
const resultTimeEl = document.getElementById("result-time");
const resultPaceEl = document.getElementById("result-pace");
const resultSegmentsEl = document.getElementById("result-segments");
const resultLoadingEl = document.getElementById("result-loading");
const resultLoadingTextEl = document.getElementById("result-loading-text");
const globalLoadingOverlayEl = document.getElementById(
  "global-loading-overlay"
);
const globalLoadingTextEl = document.getElementById("global-loading-text");
const freeRunCourseModalEl = document.getElementById(
  "free-run-course-modal-overlay"
);
const freeRunCourseSaveBtn = document.getElementById(
  "free-run-course-save-btn"
);
const freeRunCourseTitleInput = document.getElementById(
  "free-run-course-title-input"
);
const freeRunCourseDescInput = document.getElementById(
  "free-run-course-desc-input"
);
const freeRunCourseAddressInput = document.getElementById(
  "free-run-course-address-input"
);
const freeRunCourseDistanceInput = document.getElementById(
  "free-run-course-distance-input"
);
const freeRunCourseRegisterTypeInput = document.getElementById(
  "free-run-course-registertype-input"
);
const freeRunCourseStartLatInput = document.getElementById(
  "free-run-course-startlat-input"
);
const freeRunCourseStartLngInput = document.getElementById(
  "free-run-course-startlng-input"
);
const freeRunCourseImageInput = document.getElementById(
  "free-run-course-image-input"
);

// ==========================
// Mobile viewport fix (100vh issue) + Kakao map relayout
// ==========================
let mapRelayoutRafId = null;

function setViewportHeightVar() {
  // visual viewport 기반으로 1vh를 px로 계산
  const h = window.innerHeight || document.documentElement.clientHeight || 0;
  const vh = h * 0.01;
  document.documentElement.style.setProperty("--vh", `${vh}px`);
}

function scheduleMapRelayout() {
  if (!map) return;
  if (mapRelayoutRafId != null) return;
  mapRelayoutRafId = requestAnimationFrame(() => {
    mapRelayoutRafId = null;
    try {
      map.relayout();
    } catch (e) {
      // ignore
    }
  });
}

// 초기 1회 세팅 (DOM 초기 렌더/주소창 상태 반영)
setViewportHeightVar();
window.addEventListener(
  "resize",
  () => {
    setViewportHeightVar();
    scheduleMapRelayout();
  },
  { passive: true }
);

// 모바일에서 주소창 show/hide 등으로 visualViewport가 변할 때 대응
if (window.visualViewport) {
  window.visualViewport.addEventListener(
    "resize",
    () => {
      setViewportHeightVar();
      scheduleMapRelayout();
    },
    { passive: true }
  );
  window.visualViewport.addEventListener(
    "scroll",
    () => {
      // 주소창 애니메이션 중에도 innerHeight가 변하는 경우가 있어 보정
      setViewportHeightVar();
      scheduleMapRelayout();
    },
    { passive: true }
  );
}

// ==========================
// WebSocket
// ==========================
let stompClient = null;
let sessionId = null;
let userId = null;
let runningTracker = null;
let gpsSubscription = null;

// WS reconnect
let wsManualDisconnect = false;
let wsReconnectAttempt = 0;
let wsReconnectTimerId = null;

// ==========================
// Get Session ID from URL
// ==========================
function getSessionIdFromUrl() {
  const path = window.location.pathname;
  const match = path.match(/\/running\/(\d+)/);
  return match ? parseInt(match[1]) : null;
}

// ==========================
// Get JWT Token
// ==========================
function getAccessToken() {
  let token = localStorage.getItem("accessToken");
  if (token) {
    return token.startsWith("Bearer ") ? token : `Bearer ${token}`;
  }
  return null;
}

// ==========================
// Get User ID
// ==========================
function getUserId() {
  const userIdStr = localStorage.getItem("userId");
  return userIdStr ? parseInt(userIdStr) : null;
}

// ==========================
// Kakao Map
// ==========================
let map = null;
let geocoder = null;
let resolvedStartAddress = null; // ✅ 시작 위치 주소 (러닝 시작 시 미리 변환해서 저장)
let coursePolyline = null;
let userMarker = null;
let startMarker = null;
let hostMarker = null;
let userHeadingOverlay = null;
let hostHeadingOverlay = null;
let coursePath = null; // 원본 코스 경로 좌표 배열
let remainingPath = null; // 남은 경로 좌표 배열
let courseCumDistM = null; // 각 포인트까지 누적거리(미터)
let courseTotalDistM = 0;
let isHost = false;
let courseSegLenM = null;
let sessionStatus = null;
let latestPosition = null;
let previewWatchId = null; // STANDBY 미리보기용(서버 전송 없음)
let isFollowing = false; // 사용자가 지도 움직이면 false, 버튼 눌렀을 때만 true
let finishRequested = false;
let chatRoomUrl = null;

// ==========================
// Result modal polling (avoid duplicates)
// ==========================
let resultPollingPromise = null;

function startResultModalPolling(loadingText) {
  const defaultLoadingText =
    sessionCourseId == null ? "코스 저장중입니다…" : "러닝 결과 저장중입니다…";

  // If modal is already opened/polling, do not start another polling loop.
  if (resultPollingPromise) {
    setResultLoadingText(loadingText || defaultLoadingText);
    openRunningResultModal();
    if (resultLoadingEl) resultLoadingEl.classList.add("show");
    return resultPollingPromise;
  }

  resultPollingPromise = showRunningResultModalWithRetry(
    loadingText || defaultLoadingText
  ).catch(() => null);
  return resultPollingPromise;
}

// start marker overlay (custom) - deprecated, use startMarker instead
let startMarkerOverlay = null;

// end marker
let endMarker = null;

// direction arrow overlay
let directionArrowOverlay = null;

// runner pin overlay (custom)
let userPinOverlay = null;
let hostPinOverlay = null;

// 코스 매칭(지도 선 따라갈 때만 지우기) 상태
let lastMatchedDistM = 0;
let lastMatchedSegIdx = 0;
let lastGpsAccuracyM = null;
let lastPositionTimestampMs = 0;
let lastPositionLatForProgress = null;
let lastPositionLngForProgress = null;
let latestStatsCache = null;
let lastAcceptedHostLat = null;
let lastAcceptedHostLng = null;

// 타이머(시간은 seed 후 계속 흐르게)
let timerStartMs = null;
let timerIntervalId = null;
let timerIsProvisional = false; // ✅ stats 시드 전 임시 타이머 여부(첫 stats에서 보정 가능)
let completedHandled = false;
let sessionCourseId = null; // null이면 자유러닝(코스 없음)
let freeRunPreview = null; // { path, distanceM, startLat, startLng }
let freeRunPreviewPolyline = null;
let isSoloRun = false; // 솔로런 여부
let sessionDataCache = null; // 세션 데이터 캐시 (코스 저장 모달용)

// ==========================
// Off-route detection (host only)
// ==========================
let offRouteCount = 0;
let offRouteActive = false;
let lastOffRouteToastAtMs = 0;

function computeOffRouteThresholdM() {
  const acc =
    lastGpsAccuracyM != null && Number.isFinite(lastGpsAccuracyM)
      ? lastGpsAccuracyM
      : 20;
  // 추천안: clamp(max(40m, accuracy*2), 40m, 120m)
  return clamp(Math.max(40, acc * 2.0), 40, 120);
}

function checkOffRouteByMatch(lat, lng) {
  // ✅ 경로 이탈 체크는 방장만 수행하지만, 참여자도 TTS를 듣도록 변경
  // 참여자는 GPS를 보내지 않으므로 경로 이탈 체크를 할 수 없음
  if (!coursePath || !courseCumDistM) return;

  // 방장의 경우에만 경로 이탈 체크
  if (isHost) {
    const m = matchProgressOnCourse(lat, lng);
    if (!m) return;

    const thresholdM = computeOffRouteThresholdM();
    const off = m.distM > thresholdM;
    if (off) {
      offRouteCount += 1;
      if (offRouteCount >= 3 && !offRouteActive) {
        console.log("경로를 이탈하였습니다");
        const now = Date.now();
        if (now - lastOffRouteToastAtMs > 15000) {
          showToast("경로를 이탈하였습니다", "warn", 3500);
          lastOffRouteToastAtMs = now;
        }
        // ✅ TTS: 방장과 참여자 모두 듣도록 (참여자는 방장의 경로 이탈을 알 필요가 있음)
        if (ttsReady && window.TtsManager && !completedHandled) {
          window.TtsManager.speak("OFF_ROUTE");
        }
        offRouteActive = true;
      }
      return;
    }

    // 다시 코스에 붙으면 리셋
    if (offRouteActive) {
      // ✅ TTS: 방장과 참여자 모두 듣도록
      if (ttsReady && window.TtsManager && !completedHandled) {
        window.TtsManager.speak("BACK_ON_ROUTE");
      }
    }
    offRouteCount = 0;
    offRouteActive = false;
  }
}

// ==========================
// GPS accuracy debug (host only)
// ==========================
let gpsAccuracyBadgeEl = null;
let lastAccuracyLogAtMs = 0;

function ensureGpsAccuracyBadge() {
  if (gpsAccuracyBadgeEl) return gpsAccuracyBadgeEl;
  if (!mapContainerEl) return null;

  const el = document.createElement("div");
  el.className = "gps-accuracy-badge";
  el.id = "gps-accuracy-badge";
  el.textContent = "GPS acc: --m";
  mapContainerEl.appendChild(el);
  gpsAccuracyBadgeEl = el;
  return el;
}

function updateGpsAccuracyBadge(accuracyM) {
  // 정확도는 내 GPS(방장)에서만 의미가 있으니 host만 표시
  if (!isHost) return;
  const el = ensureGpsAccuracyBadge();
  if (!el) return;
  if (accuracyM == null || !Number.isFinite(accuracyM)) {
    el.classList.remove("show");
    return;
  }
  el.textContent = `GPS acc: ${Math.round(accuracyM)}m`;
  el.classList.add("show");

  // 콘솔은 너무 시끄럽지 않게 5초에 1번만
  const now = Date.now();
  if (now - lastAccuracyLogAtMs > 5000) {
    console.log("GPS accuracy(m):", accuracyM);
    lastAccuracyLogAtMs = now;
  }
}

// ==========================
// Preview-only GPS (STANDBY)
// ==========================
function startPreviewOnlyTracking() {
  console.log("🔍 startPreviewOnlyTracking() 호출됨");
  if (!navigator.geolocation) {
    showToast("이 브라우저는 위치 기능을 지원하지 않습니다", "warn", 3500);
    return;
  }
  if (previewWatchId != null) {
    console.log("⚠️ previewWatchId가 이미 설정되어 있음:", previewWatchId);
    return;
  }
  console.log("📍 GPS 권한 요청 시작");

  // ✅ 미리보기 메시지 제거

  // ✅ GPS 권한 요청을 위해 getCurrentPosition을 먼저 호출
  navigator.geolocation.getCurrentPosition(
    (position) => {
      // 권한이 허용되면 첫 번째 위치도 마커로 표시
      if (position && position.coords) {
        latestPosition = position;
        lastGpsAccuracyM = position.coords.accuracy;
        updateGpsAccuracyBadge(lastGpsAccuracyM);
        // ✅ heading 전달 (미리보기에서는 heading이 없을 수 있음)
        const heading =
          position.coords.heading != null ? position.coords.heading : null;
        // ✅ map이 준비되었는지 확인 후 마커 표시
        if (map) {
          updateUserPosition(
            position.coords.latitude,
            position.coords.longitude,
            heading
          );
        } else {
          // map이 아직 준비되지 않았으면 잠시 후 재시도
          setTimeout(() => {
            if (map && latestPosition && latestPosition.coords) {
              const heading =
                latestPosition.coords.heading != null
                  ? latestPosition.coords.heading
                  : null;
              updateUserPosition(
                latestPosition.coords.latitude,
                latestPosition.coords.longitude,
                heading
              );
            }
          }, 100);
        }
      }

      // watchPosition 시작
      if (previewWatchId != null) return;

  previewWatchId = navigator.geolocation.watchPosition(
    (position) => {
      if (!position || !position.coords) return;
      latestPosition = position;
      lastGpsAccuracyM = position.coords.accuracy;
      updateGpsAccuracyBadge(lastGpsAccuracyM);
      // ✅ heading 전달 (미리보기에서는 heading이 없을 수 있음)
      const heading =
        position.coords.heading != null ? position.coords.heading : null;
          // ✅ map이 준비되었는지 확인 후 마커 표시
          if (map) {
      updateUserPosition(
        position.coords.latitude,
        position.coords.longitude,
        heading
      );
          }
    },
    (err) => {
      console.warn("미리보기 GPS 에러:", err?.message || err);
        },
        { enableHighAccuracy: true, timeout: 8000, maximumAge: 0 }
      );
    },
    (err) => {
      console.warn("GPS 권한 요청 실패:", err?.message || err);
      if (err.code === err.PERMISSION_DENIED) {
        showToast(
          "위치 권한이 필요합니다. 브라우저 설정에서 위치 권한을 허용해주세요.",
          "warn",
          5000
        );
      }
    },
    { enableHighAccuracy: true, timeout: 8000, maximumAge: 0 }
  );
}

/**
 * 시작 위치 주소를 비동기로 변환해서 저장
 */
async function convertStartAddressAsync(lat, lng) {
  if (resolvedStartAddress) {
    // 이미 변환된 주소가 있으면 스킵
    return;
  }

  try {
    // geocoder가 없으면 대기
    if (!geocoder) {
      for (let i = 0; i < 30; i++) {
        await new Promise((resolve) => setTimeout(resolve, 100));
        if (geocoder) break;
      }
    }

    if (geocoder) {
      console.log("✅ 시작 위치 주소 변환 시도:", { lat, lng });
      resolvedStartAddress = await new Promise((resolve) => {
        geocoder.coord2Address(lng, lat, (result, status) => {
          if (status === kakao.maps.services.Status.OK && result?.[0]) {
            const address =
              result[0].road_address?.address_name ??
              result[0].address?.address_name ??
              "";
            console.log("✅ 시작 위치 주소 변환 성공:", address);
            resolve(address);
          } else {
            console.warn("❌ 시작 위치 주소 변환 실패:", status);
            resolve(null);
          }
        });
      });
    } else {
      console.warn("❌ geocoder가 초기화되지 않음");
      resolvedStartAddress = null;
    }
  } catch (e) {
    console.warn("❌ 시작 위치 주소 변환 예외:", e);
    resolvedStartAddress = null;
  }
}

/**
 * 자유러닝(코스 없음) 저장 모달에서 사용할 주소를 최대한 안정적으로 확보한다.
 * - 모달은 이 함수가 끝난 뒤(성공/타임아웃/좌표 fallback) 표시된다.
 * - 요구사항: 주소는 자동 입력 + readOnly
 */
async function resolveFreeRunAddressWithRetry(preview, { timeoutMs = 5000 } = {}) {
  // 오프라인 자유러닝: meetingPlace가 있으면 우선 사용
  if (sessionDataCache?.meetingPlace) return sessionDataCache.meetingPlace;

  // 이미 변환된 시작 주소가 있으면 즉시 사용
  if (resolvedStartAddress) return resolvedStartAddress;

  // 좌표 후보 (preview 우선, 없으면 latestPosition)
  let targetLat = preview?.startLat ?? latestPosition?.coords?.latitude ?? null;
  let targetLng = preview?.startLng ?? latestPosition?.coords?.longitude ?? null;

  // 1) 기존 로직(geocoder 대기 포함)로 한번 채워보기
  if (targetLat != null && targetLng != null) {
    try {
      await convertStartAddressAsync(targetLat, targetLng);
    } catch (e) {
      // ignore
    }
    if (resolvedStartAddress) return resolvedStartAddress;
  }

  // 2) 직접 재시도 (geocoder/services가 늦게 붙는 케이스 대응)
  const deadline = Date.now() + Number(timeoutMs || 0);
  while (Date.now() < deadline) {
    // 최신 좌표 재확인
    targetLat = preview?.startLat ?? latestPosition?.coords?.latitude ?? null;
    targetLng = preview?.startLng ?? latestPosition?.coords?.longitude ?? null;

    if (!geocoder) {
      await new Promise((r) => setTimeout(r, 100));
      continue;
    }

    if (targetLat != null && targetLng != null) {
      const addr = await new Promise((resolve) => {
        try {
          geocoder.coord2Address(targetLng, targetLat, (result, status) => {
            if (status === kakao.maps.services.Status.OK && result?.[0]) {
              resolve(
                result[0].road_address?.address_name ??
                  result[0].address?.address_name ??
                  ""
              );
            } else {
              resolve("");
            }
          });
        } catch (e) {
          resolve("");
        }
      });

      if (addr) {
        resolvedStartAddress = addr;
        return addr;
      }
    }

    await new Promise((r) => setTimeout(r, 150));
  }

  // 3) 끝까지 안 되면 "빈 값"으로 두면 저장 자체가 막히므로 좌표로 fallback
  if (targetLat != null && targetLng != null) {
    const fallback = `위치정보(위도:${Number(targetLat).toFixed(
      5
    )}, 경도:${Number(targetLng).toFixed(5)})`;
    showToast("주소 변환에 실패했습니다. 좌표로 저장됩니다.", "warn", 3500);
    return fallback;
  }

  showToast("주소 변환에 실패했습니다. 잠시 후 다시 시도해주세요.", "warn", 3500);
  return "";
}

function stopPreviewOnlyTracking() {
  try {
    if (previewWatchId != null && navigator.geolocation) {
      navigator.geolocation.clearWatch(previewWatchId);
    }
  } catch (e) {
    // ignore
  }
  previewWatchId = null;
}

// ==========================
// Toast (WS/속도/이탈 알림)
// ==========================
let toastEl = null;
let toastTimerId = null;

function ensureToast() {
  if (toastEl) return toastEl;
  const el = document.createElement("div");
  el.className = "toast";
  el.id = "toast";
  el.style.display = "none";
  document.body.appendChild(el);
  toastEl = el;
  return el;
}

function showToast(message, type = "info", durationMs = 3000) {
  const el = ensureToast();
  if (!el) return;
  el.textContent = message;
  el.className = `toast show ${type}`;
  el.style.display = "block";

  if (toastTimerId) clearTimeout(toastTimerId);
  toastTimerId = setTimeout(() => {
    el.classList.remove("show");
    el.style.display = "none";
  }, durationMs);
}

function setResultLoadingText(text) {
  if (resultLoadingTextEl && typeof text === "string" && text.trim()) {
    resultLoadingTextEl.textContent = text;
  }
}

function setGlobalLoading(show, text) {
  if (!globalLoadingOverlayEl) return;
  if (globalLoadingTextEl && typeof text === "string" && text.trim()) {
    globalLoadingTextEl.textContent = text;
  }
  if (show) globalLoadingOverlayEl.classList.add("show");
  else globalLoadingOverlayEl.classList.remove("show");
}

async function openFreeRunCourseModal(preview) {
  if (!freeRunCourseModalEl) return;
  freeRunPreview = preview;

  // 기본값 세팅
  try {
    if (freeRunCourseTitleInput) {
      const d = new Date();
      const pad = (n) => String(n).padStart(2, "0");
      freeRunCourseTitleInput.value = `자유러닝 코스 ${pad(
        d.getMonth() + 1
      )}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    }
    if (freeRunCourseDescInput) freeRunCourseDescInput.value = "";

    if (freeRunCourseDistanceInput && preview?.distanceM != null) {
      freeRunCourseDistanceInput.value = String(preview.distanceM);
      // ✅ 거리 입력 필드를 readonly로 설정 (본인이 뛴 거리 고정)
      freeRunCourseDistanceInput.readOnly = true;
    }
    if (freeRunCourseRegisterTypeInput) {
      // ✅ 오프라인이든 솔로런이든 둘다 MANUAL로 고정
      freeRunCourseRegisterTypeInput.value = "MANUAL";
      // ✅ 등록 타입을 disabled로 설정 (변경 불가)
      freeRunCourseRegisterTypeInput.disabled = true;
    }
    if (freeRunCourseStartLatInput && preview?.startLat != null) {
      freeRunCourseStartLatInput.value = String(preview.startLat);
      // ✅ 시작 위도를 readonly로 설정 (변경 불가)
      freeRunCourseStartLatInput.readOnly = true;
      freeRunCourseStartLatInput.style.backgroundColor = "#f3f4f6";
      freeRunCourseStartLatInput.style.cursor = "not-allowed";
    }
    if (freeRunCourseStartLngInput && preview?.startLng != null) {
      freeRunCourseStartLngInput.value = String(preview.startLng);
      // ✅ 시작 경도를 readonly로 설정 (변경 불가)
      freeRunCourseStartLngInput.readOnly = true;
      freeRunCourseStartLngInput.style.backgroundColor = "#f3f4f6";
      freeRunCourseStartLngInput.style.cursor = "not-allowed";
    }
    if (freeRunCourseImageInput) freeRunCourseImageInput.value = "";
  } catch (e) {
    // ignore
  }

  // ✅ 주소 자동 입력: resolvedStartAddress 우선 사용 (첫 GPS 주소 변환 결과)
  let resolvedAddress = "";
  if (freeRunCourseAddressInput) {
    // ✅ 주소가 들어올 때까지 재시도 후 모달 표시 (최대 5초)
    resolvedAddress = await resolveFreeRunAddressWithRetry(preview, {
      timeoutMs: 5000,
    });
    if (resolvedAddress && resolvedStartAddress == null) {
      resolvedStartAddress = resolvedAddress;
    }
  }

  // ✅ 주소 설정 (요구사항: 무조건 readOnly)
  if (freeRunCourseAddressInput) {
    freeRunCourseAddressInput.value = resolvedAddress || "";
    freeRunCourseAddressInput.readOnly = true;
    freeRunCourseAddressInput.style.backgroundColor = "#f3f4f6";
    freeRunCourseAddressInput.style.cursor = "not-allowed";
  }

  // 지도에 프리뷰 경로 표시 (수정 불가)
  try {
    if (map && preview?.path) {
      const geo = JSON.parse(preview.path);
      const coords = geo?.coordinates;
      if (Array.isArray(coords) && coords.length >= 2) {
        const latLngs = coords.map(
          (c) => new kakao.maps.LatLng(Number(c[1]), Number(c[0]))
        );
        if (freeRunPreviewPolyline) freeRunPreviewPolyline.setMap(null);
        freeRunPreviewPolyline = new kakao.maps.Polyline({
          path: latLngs,
          strokeWeight: 6,
          strokeColor: "#111827",
          strokeOpacity: 0.9,
          strokeStyle: "solid",
        });
        freeRunPreviewPolyline.setMap(map);
        map.setCenter(latLngs[0]);
      }
    }
  } catch (e) {
    console.warn("프리뷰 경로 렌더 실패:", e?.message || e);
  }

  if (freeRunCourseSaveBtn) freeRunCourseSaveBtn.disabled = false;
  // ✅ 주소 확보(또는 timeout/fallback) 이후에만 모달 표시
  freeRunCourseModalEl.classList.add("show");

  // ✅ 코스 제목/설명 validation 초기화
  clearCourseFormErrors();
}

function closeFreeRunCourseModal() {
  if (!freeRunCourseModalEl) return;
  freeRunCourseModalEl.classList.remove("show");
  // ✅ 모달 닫을 때 에러 메시지 초기화
  clearCourseFormErrors();
}

// ✅ 코스 폼 validation 함수
function validateCourseForm() {
  let isValid = true;

  // 코스 제목 validation
  const title = freeRunCourseTitleInput?.value?.trim();
  const titleError = document.getElementById("free-run-course-title-error");
  if (!title) {
    isValid = false;
    if (freeRunCourseTitleInput) {
      freeRunCourseTitleInput.classList.add("error");
    }
    if (titleError) {
      titleError.textContent = "코스 제목은 필수입니다.";
      titleError.style.display = "block";
    }
  } else if (title.length > 50) {
    isValid = false;
    if (freeRunCourseTitleInput) {
      freeRunCourseTitleInput.classList.add("error");
    }
    if (titleError) {
      titleError.textContent = "코스 제목은 50자 이내여야 합니다.";
      titleError.style.display = "block";
    }
  } else {
    if (freeRunCourseTitleInput) {
      freeRunCourseTitleInput.classList.remove("error");
    }
    if (titleError) {
      titleError.style.display = "none";
    }
  }

  // 코스 설명 validation (선택, 500자 제한)
  const description = freeRunCourseDescInput?.value?.trim() || "";
  const descError = document.getElementById("free-run-course-desc-error");
  if (description.length > 500) {
    isValid = false;
    if (freeRunCourseDescInput) {
      freeRunCourseDescInput.classList.add("error");
    }
    if (descError) {
      descError.textContent = "코스 설명은 500자 이내여야 합니다.";
      descError.style.display = "block";
    }
  } else {
    if (freeRunCourseDescInput) {
      freeRunCourseDescInput.classList.remove("error");
    }
    if (descError) {
      descError.style.display = "none";
    }
  }

  return isValid;
}

// ✅ 코스 폼 에러 메시지 초기화
function clearCourseFormErrors() {
  if (freeRunCourseTitleInput) {
    freeRunCourseTitleInput.classList.remove("error");
  }
  const titleError = document.getElementById("free-run-course-title-error");
  if (titleError) {
    titleError.style.display = "none";
  }

  if (freeRunCourseDescInput) {
    freeRunCourseDescInput.classList.remove("error");
  }
  const descError = document.getElementById("free-run-course-desc-error");
  if (descError) {
    descError.style.display = "none";
  }
}

async function fetchFreeRunCoursePreview() {
  const token = getAccessToken();
  if (!token) throw new Error("로그인이 필요합니다.");
  const res = await fetch(
    `/api/running/sessions/${sessionId}/free-run/course/preview`,
    {
      method: "POST",
      headers: {
        Authorization: token,
        "Content-Type": "application/json",
      },
    }
  );
  const body = await res.json().catch(() => null);
  if (!res.ok || !body?.success) {
    throw new Error(body?.message || "코스 프리뷰 생성에 실패했습니다.");
  }
  return body.data;
}

async function saveCourseFromFreeRunPreview(preview) {
  const title = freeRunCourseTitleInput?.value?.trim();
  const description = freeRunCourseDescInput?.value?.trim() || "";
  const address = freeRunCourseAddressInput?.value?.trim();
  const distanceM = Number(freeRunCourseDistanceInput?.value);
  const courseRegisterType = freeRunCourseRegisterTypeInput?.value;

  // ✅ 코스 생성 페이지와 동일한 validation 적용
  // 제목 validation
  if (!title) {
    throw new Error("코스 제목은 필수입니다.");
  }
  if (title.length > 50) {
    throw new Error("코스 제목은 50자 이내여야 합니다.");
  }

  // 설명 validation (선택, 500자 제한)
  if (description.length > 500) {
    throw new Error("코스 설명은 500자 이내여야 합니다.");
  }

  // 주소 validation
  if (!address) {
    throw new Error("주소는 필수입니다.");
  }
  if (address.length > 100) {
    throw new Error("주소는 100자 이내여야 합니다.");
  }

  // 거리 validation
  if (!Number.isFinite(distanceM) || distanceM < 100) {
    throw new Error("코스 거리는 최소 100m 이상이어야 합니다.");
  }
  if (distanceM > 100000) {
    throw new Error("코스 거리는 최대 100km를 초과할 수 없습니다.");
  }

  // 등록 타입 validation
  if (!courseRegisterType) {
    throw new Error("코스 등록 타입을 선택해주세요.");
  }

  // 경로 validation
  if (!preview?.path) {
    throw new Error("코스 경로가 없습니다.");
  }

  // 시작 좌표 validation
  const startLat = Number(preview?.startLat);
  const startLng = Number(preview?.startLng);
  if (!Number.isFinite(startLat) || startLat < -90 || startLat > 90) {
    throw new Error("시작 위도가 올바르지 않습니다.");
  }
  if (!Number.isFinite(startLng) || startLng < -180 || startLng > 180) {
    throw new Error("시작 경도가 올바르지 않습니다.");
  }

  const token = getAccessToken();
  if (!token) throw new Error("로그인이 필요합니다.");

  const courseDto = {
    title,
    description,
    path: preview.path,
    distanceM: Math.round(distanceM),
    startLat,
    startLng,
    address,
    courseRegisterType,
  };

  const fd = new FormData();
  fd.append(
    "course",
    new Blob([JSON.stringify(courseDto)], { type: "application/json" })
  );
  const imageFile = freeRunCourseImageInput?.files?.[0];
  if (imageFile) {
    fd.append("imageFile", imageFile);
  }

  const res = await fetch(`/api/courses`, {
    method: "POST",
    headers: {
      Authorization: token,
    },
    body: fd,
  });
  const body = await res.json().catch(() => null);
  if (!res.ok || !body?.success) {
    throw new Error(body?.message || "코스 저장에 실패했습니다.");
  }
  return body.data; // { id }
}

// RunningTracker의 속도 제한 경고 이벤트 수신
window.addEventListener("running:tooFast", (evt) => {
  try {
    const speedMps = evt?.detail?.speedMps;
    const hard = evt?.detail?.hard === true;
    const kmh =
      speedMps != null && Number.isFinite(speedMps) ? speedMps * 3.6 : null;
    const label = kmh != null ? `${kmh.toFixed(1)}km/h` : "";
    showToast(
      `속도가 너무 빠릅니다${label ? ` (${label})` : ""}`,
      hard ? "warn" : "info",
      3500
    );
    // TTS (속도 경고)
    try {
      if (window.TtsManager && !completedHandled) {
        window.TtsManager.speak("SPEED_TOO_FAST");
      }
    } catch (e) {
      // ignore
    }
  } catch (e) {
    // ignore
  }
});

// ==========================
// TTS hooks (policy-based)
// ==========================
let ttsReady = false;
let ttsMode = "OFFLINE";
let ttsMotivateTimerId = null;
let ttsPaceTimerId = null;
let lastStatsAtMs = 0;
let hostSignalLost = false;

async function ensureTtsOnce() {
  if (ttsReady) return true;
  if (!window.TtsManager) return false;
  try {
    await window.TtsManager.ensureLoaded({ sessionId, mode: ttsMode });
    ttsReady = true;
    return true;
  } catch (e) {
    console.warn("TTS 로드 실패(무시):", e?.message || e);
    return false;
  }
}

function maybeSpeakStartOnce() {
  // 시작 직후(채팅방에서 시작 후 바로 러닝페이지로 온 경우)만 1회
  if (!ttsReady || !window.TtsManager) return;
  // ✅ 참여자도 START_RUN TTS 들리도록 isHost 체크 제거
  if (sessionStatus !== "IN_PROGRESS") return;
  const key = storageKey("ttsStartSpoken");
  if (localStorage.getItem(key) === "1") return;

  const startedAtMsRaw = localStorage.getItem(storageKey("startedAtMs"));
  const startedAtMs = startedAtMsRaw ? Number(startedAtMsRaw) : null;
  if (!Number.isFinite(startedAtMs) || startedAtMs <= 0) return;

  // 20초 이내면 "방금 시작"으로 간주
  if (Date.now() - startedAtMs <= 20000) {
    window.TtsManager.speak("START_RUN", { priority: 2, cooldownMs: 0 });
    localStorage.setItem(key, "1");
  }
}

function startMotivationSchedule() {
  if (ttsMotivateTimerId) return;
  // 시작 3분 후부터 5분마다 (참여자도 들리도록 isHost 체크 제거)
  setTimeout(() => {
    if (ttsReady && sessionStatus === "IN_PROGRESS" && !completedHandled) {
      window.TtsManager.speak("MOTIVATE_GOOD_JOB");
    }
  }, 3 * 60 * 1000);
  ttsMotivateTimerId = setInterval(() => {
    if (!ttsReady || sessionStatus !== "IN_PROGRESS" || completedHandled)
      return;
    window.TtsManager.speak("MOTIVATE_GOOD_JOB");
  }, 5 * 60 * 1000);
}

function startPaceSchedule() {
  // ✅ 페이스는 "1km split 기반"으로 말하므로 시간 기반 스케줄은 사용하지 않는다.
  return;
}

function startHostSignalWatchdog() {
  // stats가 일정 시간 끊기면(방장 신호 끊김) 안내
  // ✅ 방장과 참여자 모두 듣도록 isHost 체크 제거
  // ✅ 솔로런에서는 "GPS 신호"로 표시
  setInterval(() => {
    if (sessionStatus !== "IN_PROGRESS") return;
    // ✅ 러닝 종료 후에는 TTS/토스트 안 나오게
    if (completedHandled) return;
    // ✅ localStorage에서 읽기
    const lastMs = getLastStatsAtMs();
    const now = Date.now();
    if (lastMs > 0 && now - lastMs > 5000) {
      if (!hostSignalLost) {
        hostSignalLost = true;
        // ✅ 솔로런에서는 "GPS 신호", 오프라인에서는 "방장 신호"
        const signalText = isSoloRun
          ? "GPS 신호가 끊겼습니다"
          : "방장 신호가 끊겼습니다";
        showToast(signalText, "warn", 3500);
        // ✅ TTS: 방장과 참여자 모두 듣도록
        if (ttsReady && window.TtsManager && !completedHandled) {
          window.TtsManager.speak("HOST_SIGNAL_LOST");
        }
      }
    }
  }, 1000);
}

// ==========================
// Running Statistics
// ==========================
let isRunning = false;
let lastHostAlongM = 0;
let hasHostAlongMOnce = false;
let lastHostAlongTimeSec = 0;
let lastPaceText = "0'00''";

// ==========================
// Solo Run Layout Adjustment
// ==========================
function adjustSoloRunLayout(isInProgress) {
  if (!isSoloRun) return;

  const statsOverlay = document.querySelector(".stats-overlay");

  if (isInProgress) {
    // IN_PROGRESS: 통계 오버레이 아래로
    if (statsOverlay) {
      statsOverlay.style.bottom = "120px"; // 채팅방 버튼 위치
    }
  } else {
    // STANDBY: 통계 오버레이 위로
    if (statsOverlay) {
      statsOverlay.style.bottom = "200px"; // 기본 위치
    }
  }
}

// ==========================
// Initialize
// ==========================
document.addEventListener("DOMContentLoaded", async () => {
  // ✅ 데이터 로딩 전까지 UI 요소 숨김 (플래시 방지)
  if (pageTitleEl) {
    pageTitleEl.style.display = "none";
  }
  if (chatButton) {
    chatButton.style.display = "none";
  }
  if (backButton) {
    backButton.style.display = "none";
  }
  if (startRunningButton) {
    startRunningButton.style.display = "none";
  }

  sessionId = getSessionIdFromUrl();
  userId = getUserId();
  chatRoomUrl = `/chat/chat1?sessionId=${sessionId}`;

  if (!sessionId) {
    alert("세션 ID가 없습니다.");
    window.location.href = "/";
    return;
  }

  if (!userId) {
    alert("로그인이 필요합니다.");
    window.location.href = "/login";
    return;
  }

  // ✅ 다른 세션으로 진입할 때만 이전 세션의 localStorage 데이터 초기화
  try {
    const lastSessionId = localStorage.getItem("running:lastSessionId");
    if (lastSessionId && lastSessionId !== String(sessionId)) {
      // 이전 세션의 데이터 삭제
    const keys = Object.keys(localStorage);
    keys.forEach((key) => {
        if (key.startsWith(`running:${lastSessionId}:`)) {
        localStorage.removeItem(key);
      }
    });
      console.log(
        "✅ 이전 세션 localStorage 데이터 초기화 완료: sessionId=" +
          lastSessionId
      );
    }
    // 현재 세션 ID 저장
    localStorage.setItem("running:lastSessionId", String(sessionId));
  } catch (e) {
    console.warn("localStorage 초기화 실패:", e);
  }

  // 뒤로가기 버튼
  backButton.addEventListener("click", () => {
    if (isRunning) {
      if (confirm("러닝을 중단하시겠습니까?")) {
        stopRunning();
        disconnectWebSocket();
        stopPreviewOnlyTracking();
        if (isSoloRun) {
          window.location.href = "/match/select";
        } else {
          window.location.href = chatRoomUrl;
        }
      }
    } else {
      disconnectWebSocket();
      stopPreviewOnlyTracking();
      if (isSoloRun) {
        window.location.href = "/match/select";
      } else {
        window.location.href = chatRoomUrl;
      }
    }
  });

  // 채팅방 버튼 (솔로런이 아닐 때만)
  if (chatButton) {
    chatButton.addEventListener("click", () => {
      disconnectWebSocket();
      stopPreviewOnlyTracking();
      window.location.href = chatRoomUrl;
    });
  }

  // 시작 버튼 (솔로런/오프라인용)
  if (startRunningButton) {
    startRunningButton.addEventListener("click", async () => {
      if (isSoloRun) {
        await startSoloRunning();
      } else if (!isSoloRun && sessionStatus === "STANDBY") {
        // ✅ STANDBY 상태일 때: 시작하기 버튼 클릭 시 실제 러닝 시작
        if (isHost) {
          await startOfflineRunning();
        } else {
          // 참여자는 러닝을 시작할 수 없음
          showToast("방장만 러닝을 시작할 수 있습니다.", "warn", 3000);
        }
      }
    });
  }
  if (locateButton) {
    locateButton.addEventListener("click", async () => {
      isFollowing = true;
      await centerToRunner();
    });
  }

  // 결과 모달 닫기
  if (runningResultClose) {
    runningResultClose.addEventListener("click", () => {
      closeRunningResultModal();
      goToChatRoom();
    });
  }
  if (runningResultGoChat) {
    runningResultGoChat.addEventListener("click", () => {
      closeRunningResultModal();
      goToChatRoom();
    });
  }

  // ✅ 로딩 오버레이 클릭 시 로딩 해제 (사용자가 다른 작업 가능하도록)
  if (globalLoadingOverlayEl) {
    globalLoadingOverlayEl.addEventListener("click", (e) => {
      // 로딩 오버레이 자체를 클릭했을 때만 해제 (내부 요소 클릭은 무시)
      if (e.target === globalLoadingOverlayEl) {
        setGlobalLoading(false);
      }
    });
  }

  // 자유러닝 코스 저장 버튼
  if (freeRunCourseSaveBtn) {
    freeRunCourseSaveBtn.addEventListener("click", async () => {
      if (!isHost) return;
      if (!freeRunPreview) {
        showToast("코스 프리뷰가 없습니다. 다시 시도해주세요.", "warn", 3500);
        return;
      }

      // ✅ validation 체크
      if (!validateCourseForm()) {
        return;
      }

      try {
        freeRunCourseSaveBtn.disabled = true;
        setGlobalLoading(true, "코스 저장중입니다…");
        const saved = await saveCourseFromFreeRunPreview(freeRunPreview);
        const courseId = saved?.id;
        if (!courseId) {
          throw new Error("코스 저장에 실패했습니다.");
        }
        // 저장된 코스를 세션에 연결하고 결과 저장
        closeFreeRunCourseModal();
        sessionCourseId = courseId;
        setGlobalLoading(true, "러닝 결과 저장중입니다…");
        await requestFinishOnce(courseId);
        setGlobalLoading(false);

        // ✅ 솔로런일 경우 시스템 메시지를 기다리지 않고 바로 결과 모달 표시
        if (isSoloRun) {
          await showRunningResultModalWithRetry("러닝 결과 저장중입니다…");
        } else {
          // 오프라인은 시스템 메시지 수신 시 결과 모달 표시 (기존 로직 유지)
          await showRunningResultModalWithRetry("러닝 결과 저장중입니다…");
        }
      } catch (e) {
        setGlobalLoading(false);
        console.error("자유러닝 코스 저장/결과 저장 실패:", e);
        showToast(e?.message || "처리에 실패했습니다.", "warn", 3500);
      } finally {
        if (freeRunCourseSaveBtn) freeRunCourseSaveBtn.disabled = false;
      }
    });
  }

  // ✅ 코스 제목/설명 입력 시 실시간 validation
  if (freeRunCourseTitleInput) {
    freeRunCourseTitleInput.addEventListener("input", () => {
      validateCourseForm();
    });
    freeRunCourseTitleInput.addEventListener("blur", () => {
      validateCourseForm();
    });
  }

  if (freeRunCourseDescInput) {
    freeRunCourseDescInput.addEventListener("input", () => {
      validateCourseForm();
    });
    freeRunCourseDescInput.addEventListener("blur", () => {
      validateCourseForm();
    });
  }

  try {
    // 1. 세션 정보 조회
    const sessionData = await loadSessionData(sessionId);
    sessionDataCache = sessionData; // ✅ 캐시 저장 (코스 저장 모달용)
    isHost = sessionData?.hostId != null && sessionData.hostId === userId;
    sessionStatus = sessionData?.status || null;
    ttsMode = sessionData?.type || "OFFLINE";
    sessionCourseId = sessionData?.courseId ?? null;
    isSoloRun = sessionData?.type === "SOLO"; // ✅ 솔로런 여부 확인

    // ✅ 데이터 로딩 완료 후 UI 표시
    if (pageTitleEl) {
      pageTitleEl.style.display = ""; // 표시
    }

    // ✅ 솔로런일 때 UI 조정
    if (isSoloRun) {
      // 솔로런은 항상 호스트
      isHost = true;

      // 페이지 타이틀 변경
      if (pageTitleEl) {
        pageTitleEl.textContent = "솔로런";
      }

      // ✅ 뒤로가기 버튼 숨기기 (솔로런)
      if (backButton) {
        backButton.style.display = "none";
      }

      // 채팅방 버튼 완전히 숨김
      if (chatButton) {
        chatButton.style.display = "none";
        chatButton.classList.add("solo-hidden");
      }

      // ✅ 결과 모달 버튼 텍스트 변경
      if (runningResultGoChat) {
        runningResultGoChat.textContent = "홈으로";
      }

      // 시작 버튼 표시 (STANDBY 상태일 때만)
      if (startRunningButton) {
        if (sessionStatus === "STANDBY") {
          startRunningButton.style.display = "block";
          // STANDBY일 때 통계 오버레이를 위로, GPS 버튼을 아래로
          adjustSoloRunLayout(false);
        } else {
          startRunningButton.style.display = "none";
          // IN_PROGRESS일 때 통계 오버레이를 아래로, GPS 버튼을 위로
          adjustSoloRunLayout(true);
        }
      }
    } else {
      // 오프라인 러닝일 때
      if (pageTitleEl) {
        pageTitleEl.textContent = "오프라인";
      }

      // ✅ 뒤로가기 버튼 숨기기 (오프라인 - 채팅방 버튼이 있으므로)
      if (backButton) {
        backButton.style.display = "none";
      }

      // ✅ STANDBY 상태일 때: 채팅방 버튼 숨기고 시작하기 버튼만 표시
      if (sessionStatus === "STANDBY") {
        if (chatButton) {
          chatButton.style.display = "none";
        }
        if (startRunningButton) {
          startRunningButton.style.display = "flex";
          startRunningButton.textContent = "시작하기";
          adjustSoloRunLayout(false);
        }
      } else {
        // ✅ STANDBY가 아닐 때: 채팅방 버튼 표시
        if (chatButton) {
          chatButton.style.display = "flex";
          chatButton.classList.remove("solo-hidden");
        }
        if (startRunningButton) {
          startRunningButton.style.display = "none";
        }
      }
    }

    // ✅ 러닝이 이미 시작된 상태라면(재입장/새로고침) GPS가 없어도 시간은 즉시 흐르게
    if (sessionStatus === "IN_PROGRESS") {
      ensureTimerRunningForInProgress();
      await ensureTtsOnce();
      startMotivationSchedule();
      maybeSpeakStartOnce();
    }

    // ✅ 솔로런 STANDBY 상태에서도 TTS 미리 로드 (시작 버튼 클릭 시 즉시 재생)
    if (isSoloRun && sessionStatus === "STANDBY") {
      ensureTtsOnce().catch(() => {
        // TTS 로드 실패해도 무시 (시작 시 다시 시도)
      });
    }

    // 2. 카카오맵 초기화 (먼저)
    await initKakaoMap();

    // 3. 코스가 있으면 경로 로드 및 표시 (웹소켓 전에)
    if (sessionCourseId) {
      try {
        await loadCoursePath(sessionId);
      } catch (error) {
        console.error("코스 경로 로드 실패:", error);
        // 경로 로드 실패해도 계속 진행
      }
    } else if (sessionData.startLat != null && sessionData.startLng != null) {
      // ✅ 자유러닝(코스 없음): 모집글 출발지 마커 표시
      // 단, 솔로런 코스 없이 뛸 때는 출발점 표시 안 함
      if (!isSoloRun) {
        const startLat = Number(sessionData.startLat);
        const startLng = Number(sessionData.startLng);
        if (Number.isFinite(startLat) && Number.isFinite(startLng)) {
          renderStartMarker(startLat, startLng);
          const startPoint = new kakao.maps.LatLng(startLat, startLng);
          map.setCenter(startPoint);
          map.setLevel(5);
        }
      }
    }

    // 3.5 재진입 복원: 최신 stats 1회 조회 후 경로/스탯 즉시 반영
    try {
      const latestStats = await loadLatestRunningStats(sessionId);
      if (latestStats) {
        latestStatsCache = latestStats;

        // ✅ TTS Manager의 Set 초기화 (재진입 시 중복 방지) - handleRunningStats 전에 먼저 호출
        if (
          window.TtsManager &&
          typeof window.TtsManager.resetDistanceState === "function"
        ) {
          let remainingDistance = latestStats.remainingDistance;
          const targetDistance = sessionDataCache?.targetDistance; // km
          const totalDistance = latestStats.totalDistance; // km

          // ✅ 솔로런 코스 없이 뛸 때: remainingDistance가 없으면 targetDistance와 totalDistance로 계산
          if (remainingDistance == null || remainingDistance === undefined) {
            if (
              Number.isFinite(targetDistance) &&
              Number.isFinite(totalDistance)
            ) {
              remainingDistance = Math.max(0, targetDistance - totalDistance);
            }
          }
          
          // ✅ 목표 거리를 초과하는 남은 거리는 목표 거리로 제한
          if (Number.isFinite(targetDistance) && Number.isFinite(totalDistance) && remainingDistance != null) {
            const maxRemainingDistance = Math.max(0, targetDistance - totalDistance);
            remainingDistance = Math.min(remainingDistance, maxRemainingDistance);
          }

          if (remainingDistance != null) {
            window.TtsManager.resetDistanceState(remainingDistance);
          } else {
            window.TtsManager.resetDistanceState();
          }
        }

        handleRunningStats(latestStats); // 거리/페이스 등 초기 반영
        // COMPLETED면 시간은 멈춰야 함
        const completed =
          (latestStats.isCompleted ?? latestStats.completed) === true;
        if (sessionStatus === "COMPLETED" || completed) {
          stopTimerAndFreeze(latestStats.totalRunningTime);
        } else {
          seedTimerOnce(latestStats.totalRunningTime);
        }
      }
    } catch (e) {
      // stats가 없을 수 있으니 무시
      console.warn("최신 러닝 통계 조회 실패(무시):", e?.message || e);
    }

    // ✅ 이미 종료된 세션이면 결과 모달을 바로 띄우고 WS 연결/추적은 하지 않는다.
    if (sessionStatus === "COMPLETED") {
      await showRunningResultModalWithRetry("러닝 결과 저장중입니다…");
      return;
    }

    // ✅ 시작 전(STANDBY)에는 러닝페이지를 "미리보기"로만 사용:
    // - WS 연결 X
    // - 서버로 GPS 전송 X
    // - 프론트에서만 내 위치 표시(출발점 찾기)
    // ✅ 솔로런일 때는 시작 버튼이 표시되므로 미리보기 추적 시작
    // ✅ 지도 초기화 후에 GPS 시작 (map이 null이면 updateUserPosition이 작동하지 않음)
    if (sessionStatus !== "IN_PROGRESS") {
      // 지도가 초기화된 후에만 GPS 시작
      if (map) {
      startPreviewOnlyTracking();
      }
      // ✅ 오프라인 STANDBY 상태에서도 러닝 페이지로 직접 올 수 있음
      // (시작은 러닝 페이지에서 방장이 시작 버튼으로 시작)
      if (!isSoloRun && sessionStatus === "STANDBY") {
        // 오프라인 STANDBY: 시작 버튼 표시하지 않고, 채팅방으로 돌아갈 수 있도록
        // 러닝은 러닝 페이지에서 시작하도록 변경
        return;
      }
      // 솔로런이 아니면 여기서 종료
      if (!isSoloRun) {
        return;
      }
      // 솔로런이면 시작 버튼이 표시된 상태로 대기 (웹소켓 연결하지 않음)
      return;
    }

    // 4. 웹소켓 연결 (IN_PROGRESS만, 솔로런 포함)
    if (typeof Stomp !== "undefined") {
      connectWebSocket();
      if (!isSoloRun) {
        startHostSignalWatchdog(); // 솔로런은 참여자가 없으므로 watchdog 불필요
      }
    } else {
      console.error("Stomp 라이브러리가 로드되지 않았습니다.");
      alert(
        "WebSocket 라이브러리를 로드할 수 없습니다. 페이지를 새로고침해주세요."
      );
    }
  } catch (error) {
    console.error("초기화 오류:", error);
    alert("페이지 로드 중 오류가 발생했습니다: " + error.message);
  }
});

function closeRunningResultModal() {
  if (runningResultModal) {
    runningResultModal.classList.remove("show");
  }
  if (resultLoadingEl) {
    resultLoadingEl.classList.remove("show");
  }
  // Allow polling to be restarted if user re-opens the modal later.
  resultPollingPromise = null;
}

function openRunningResultModal() {
  if (runningResultModal) {
    runningResultModal.classList.add("show");
  }
}

function stopTimerAndFreeze(finalSec) {
  try {
    if (timerIntervalId) {
      clearInterval(timerIntervalId);
      timerIntervalId = null;
    }
  } catch (e) {
    // ignore
  }
  timerStartMs = null;
  if (finalSec != null) {
    timeValue.textContent = formatElapsed(finalSec);
  }
}

async function fetchRunningResult() {
  const token = getAccessToken();
  if (!token) {
    throw new Error("로그인이 필요합니다.");
  }
  const res = await fetch(`/api/running/sessions/${sessionId}/result`, {
    method: "GET",
    headers: {
      Authorization: token,
      "Content-Type": "application/json",
    },
  });
  const body = await res.json().catch(() => null);
  if (!res.ok || !body?.success) {
    throw new Error(body?.message || "러닝 결과를 불러올 수 없습니다.");
  }
  return body.data;
}

function renderRunningResult(data) {
  if (!data) return;

  // 거리
  if (resultDistanceEl) {
    resultDistanceEl.textContent =
      data.totalDistance != null
        ? Number(data.totalDistance).toFixed(2)
        : "0.00";
  }

  // 시간
  if (resultTimeEl) {
    const total = Number(data.totalTime) || 0;
    const min = Math.floor(total / 60);
    const sec = total % 60;
    resultTimeEl.textContent = `${min}:${String(sec).padStart(2, "0")}`;
  }

  // 평균 페이스
  if (resultPaceEl) {
    if (data.avgPace != null) {
      const pace = Number(data.avgPace);
      const m = Math.floor(pace);
      const s = Math.round((pace - m) * 60);
      resultPaceEl.textContent = `${m}:${String(s).padStart(2, "0")}`;
    } else {
      resultPaceEl.textContent = "--:--";
    }
  }

  // 구간 페이스
  if (resultSegmentsEl) {
    resultSegmentsEl.innerHTML = "";

    const split = data.splitPace;
    if (Array.isArray(split) && split.length > 0) {
      split.forEach((seg) => {
        const row = document.createElement("div");
        row.className = "segment-item";

        const km = document.createElement("span");
        km.className = "segment-km";
        km.textContent = `${seg.km}km`;

        const pace = document.createElement("span");
        pace.className = "segment-pace";
        const p = Number(seg.pace) || 0;
        const m = Math.floor(p);
        const s = Math.round((p - m) * 60);
        pace.textContent = `${m}:${String(s).padStart(2, "0")}/km`;

        row.appendChild(km);
        row.appendChild(pace);
        resultSegmentsEl.appendChild(row);
      });
    } else {
      const empty = document.createElement("div");
      empty.style.cssText =
        "text-align:center;color:#9CA3AF;padding:16px;font-size:12px;font-weight:800;";
      empty.textContent = "구간 데이터 없음";
      resultSegmentsEl.appendChild(empty);
    }
  }
}

async function showRunningResultModalWithRetry(loadingText) {
  openRunningResultModal();
  // ✅ 로딩 텍스트 설정 (코스 없이 뛸 때는 "코스 저장중", 코스 있이 뛸 때는 "러닝 결과 저장중")
  const defaultLoadingText = sessionCourseId == null 
    ? "코스 저장중입니다…" 
    : "러닝 결과 저장중입니다…";
  setResultLoadingText(loadingText || defaultLoadingText);
  if (resultLoadingEl) resultLoadingEl.classList.add("show");
  if (resultSegmentsEl) resultSegmentsEl.innerHTML = "";
  
  // ✅ 결과 요약 영역도 로딩 중에는 숨김
  const resultSummaryEl = document.querySelector(".result-summary");
  const resultSegmentsTitleEl = document.querySelector(".result-segments");
  if (resultSummaryEl) resultSummaryEl.style.display = "none";
  if (resultSegmentsTitleEl) resultSegmentsTitleEl.style.display = "none";

  // ✅ 광고 미리 로드 시작 (비동기, 결과 로드와 병렬 진행)
  let adLoadPromise = Promise.resolve(null);
  if (typeof loadAd === "function" && typeof createAdPopup === "function") {
    adLoadPromise = loadAd("RUN_END_BANNER").catch((e) => {
      console.warn("광고 미리 로드 실패 (무시):", e);
      return null;
    });
  }

  let lastErr = null;
  let resultData = null;
  // ✅ 재시도(폴링): 코스 없는 오프라인은 방장 코스 저장까지 시간이 길 수 있어 더 오래 기다린다.
  // - 모달을 닫으면 즉시 중단한다.
  const maxRetries = sessionCourseId == null ? 600 : 120; // ~20min / ~3min
  const retryDelay = sessionCourseId == null ? 1500 : 800;
  
  for (let i = 0; i < maxRetries; i++) {
    // 사용자가 모달을 닫으면 더 이상 폴링하지 않는다.
    if (
      runningResultModal &&
      !runningResultModal.classList.contains("show")
    ) {
      return;
    }
    try {
      resultData = await fetchRunningResult();
      
      // ✅ 결과 데이터가 있는 경우에만 렌더링 및 로딩 숨김
      if (resultData) {
        renderRunningResult(resultData);
        
        // ✅ 결과 영역 표시 (결과 요약과 구간 페이스)
        if (resultSummaryEl) resultSummaryEl.style.display = "grid"; // CSS에서 grid로 정의됨
        if (resultSegmentsTitleEl) resultSegmentsTitleEl.style.display = "flex"; // CSS에서 flex로 정의됨
        
        // ✅ 로딩 숨김 (결과가 로드된 후에만)
        if (resultLoadingEl) resultLoadingEl.classList.remove("show");
        
        // ✅ 참여자: global-loading 해제 (결과 모달이 표시되면 로딩 완료)
        if (!isHost) {
          console.log("✅ 참여자 결과 로드 완료: global-loading 해제");
          setGlobalLoading(false);
        }

        // ✅ 결과 로드 완료 후 광고 표시 (이미 로드된 광고가 있으면 즉시 표시)
        const adData = await adLoadPromise;
        if (adData) {
          const adPopup = createAdPopup(adData);
          document.body.appendChild(adPopup);
        }

        return;
      } else {
        // 결과가 없으면 계속 시도 (로딩은 계속 표시)
        console.log(`[${i + 1}/${maxRetries}] 러닝 결과 대기 중...`);
        await new Promise((r) => setTimeout(r, retryDelay));
      }
    } catch (e) {
      lastErr = e;
      // ✅ 404 에러(결과가 아직 없음)도 계속 재시도
      if (e?.message?.includes("404") || e?.message?.includes("불러올 수 없습니다")) {
        console.log(`[${i + 1}/${maxRetries}] 러닝 결과 대기 중... (404)`);
        await new Promise((r) => setTimeout(r, retryDelay));
      } else {
        // 다른 에러는 짧은 지연 후 재시도
        await new Promise((r) => setTimeout(r, retryDelay));
      }
    }
  }

  // ✅ 최종 실패 시에도 로딩은 계속 표시하고, 사용자에게 메시지 표시
  console.warn("러닝 결과 조회 실패:", lastErr?.message || lastErr);
  setResultLoadingText("러닝 결과를 불러올 수 없습니다. 잠시 후 다시 시도해주세요.");
  
  // 결과 로드 실패해도 광고 표시
  const adData = await adLoadPromise;
  if (adData) {
    const adPopup = createAdPopup(adData);
    document.body.appendChild(adPopup);
  }
}

function goToChatRoom() {
  if (isSoloRun) {
    window.location.href = "/home";
    return;
  }
  if (chatRoomUrl) {
    window.location.href = chatRoomUrl;
    return;
  }
  if (sessionId != null) {
    window.location.href = `/chat/chat1?sessionId=${sessionId}`;
    return;
  }
  window.location.href = "/chat";
}

async function centerToRunner() {
  if (!map) return;

  // 방장은 내 위치, 참가자는 방장 위치를 기본으로 센터링
  let lat = null;
  let lng = null;
  // ✅ 미리보기(STANDBY)에서는 참가자도 "내 위치"로 센터링 가능해야 한다
  if ((isHost || sessionStatus !== "IN_PROGRESS") && latestPosition?.coords) {
    lat = latestPosition.coords.latitude;
    lng = latestPosition.coords.longitude;
  } else if (
    latestStatsCache?.hostLatitude != null &&
    latestStatsCache?.hostLongitude != null
  ) {
    lat = latestStatsCache.hostLatitude;
    lng = latestStatsCache.hostLongitude;
  } else if (coursePath && coursePath.length > 0) {
    lat = coursePath[0].lat;
    lng = coursePath[0].lng;
  }

  // ✅ STANDBY 상태에서 latestPosition이 없으면 getCurrentPosition으로 현재 위치 가져오기
  if (
    (isHost || sessionStatus !== "IN_PROGRESS") &&
    (lat == null || lng == null)
  ) {
    try {
      const getCurrentPositionOnce = (options = {}) =>
        new Promise((resolve, reject) => {
          if (!navigator.geolocation) {
            reject(new Error("이 브라우저에서 위치 기능을 지원하지 않습니다."));
            return;
          }
          navigator.geolocation.getCurrentPosition(resolve, reject, options);
        });

      const position = await getCurrentPositionOnce({
        enableHighAccuracy: true,
        timeout: 8000,
        maximumAge: 0,
      });

      if (position?.coords) {
        lat = position.coords.latitude;
        lng = position.coords.longitude;
        // latestPosition도 업데이트
        latestPosition = position;
      }
    } catch (e) {
      console.warn("현재 위치 조회 실패:", e?.message || e);
    }
  }

  // 참가자인데 방장 위치가 아직 없으면 최신 stats 1회 재조회 시도
  if (!isHost && (lat == null || lng == null)) {
    try {
      const latest = await loadLatestRunningStats(sessionId);
      if (latest) {
        latestStatsCache = latest;
        if (latest.hostLatitude != null && latest.hostLongitude != null) {
          lat = latest.hostLatitude;
          lng = latest.hostLongitude;
        }
      }
    } catch (e) {
      // ignore
    }
  }

  if (lat == null || lng == null) {
    alert("위치정보를 불러올 수 없습니다.");
    return;
  }
  map.setCenter(new kakao.maps.LatLng(lat, lng));
}

function setHeadingOverlay(overlayRefName, lat, lng, headingDeg, isHostArrow) {
  if (!map || lat == null || lng == null || headingDeg == null) return;

  const el = document.createElement("div");
  el.className = `runner-heading-cone${isHostArrow ? " host" : ""}`;

  const overlay = new kakao.maps.CustomOverlay({
    position: new kakao.maps.LatLng(lat, lng),
    content: el,
    yAnchor: 0.9,
    xAnchor: 0.5,
  });
  overlay.setMap(map);

  const setRotation = (deg) => {
    // 화살표는 위(북쪽)를 향하므로 heading 그대로 회전
    el.style.transform = `rotate(${deg}deg)`;
  };
  setRotation(headingDeg);

  if (overlayRefName === "user") {
    if (userHeadingOverlay) userHeadingOverlay.setMap(null);
    userHeadingOverlay = overlay;
    userHeadingOverlay.__el = el;
  } else {
    if (hostHeadingOverlay) hostHeadingOverlay.setMap(null);
    hostHeadingOverlay = overlay;
    hostHeadingOverlay.__el = el;
  }
}

function updateHeadingOverlay(overlay, lat, lng, headingDeg) {
  if (!overlay || lat == null || lng == null) return;
  overlay.setPosition(new kakao.maps.LatLng(lat, lng));
  const el = overlay.__el;
  if (el && headingDeg != null) {
    el.style.transform = `rotate(${headingDeg}deg)`;
  }
}

function storageKey(key) {
  return `running:${sessionId}:${key}`;
}

function getStoredHostAlongM() {
  try {
    const v = Number(localStorage.getItem(storageKey("lastHostMatchedDistM")));
    return Number.isFinite(v) && v >= 0 ? v : 0;
  } catch (e) {
    return 0;
  }
}

function storeHostAlongM(meters) {
  try {
    const v = Number(meters);
    if (!Number.isFinite(v) || v < 0) return;
    localStorage.setItem(storageKey("lastHostMatchedDistM"), String(v));
  } catch (e) {
    // ignore
  }
}

function updateLastStatsAtMs() {
  try {
    localStorage.setItem(storageKey("lastStatsAtMs"), String(Date.now()));
  } catch (e) {
    // ignore
  }
}

function cacheLatestStatsSnapshot(stats) {
  try {
    if (!stats) return;
    if (stats.totalDistance != null) {
      localStorage.setItem(
        storageKey("lastTotalDistance"),
        String(Number(stats.totalDistance) || 0)
      );
    }
    if (stats.totalRunningTime != null) {
      localStorage.setItem(
        storageKey("lastTotalRunningTime"),
        String(Math.max(0, Number(stats.totalRunningTime) || 0))
      );
    }
  } catch (e) {
    // ignore
  }
}

function getLastStatsAtMs() {
  try {
    const v = Number(localStorage.getItem(storageKey("lastStatsAtMs")));
    return Number.isFinite(v) && v > 0 ? v : 0;
  } catch (e) {
    return 0;
  }
}

function seedTimerOnce(seedRunningTimeSec) {
  // 우선 localStorage에 시작시각이 있으면 그걸 사용 (재진입에도 동일)
  const stored = localStorage.getItem(storageKey("startedAtMs"));
  if (stored) {
    const v = Number(stored);
    if (Number.isFinite(v) && v > 0) {
      timerStartMs = v;
      timerIsProvisional = false;
      if (!timerIntervalId) startTimerTick();
      return;
    }
  }

  // 없으면 stats 시간으로 seed (그 뒤로는 계속 흐르게)
  const seed = Number(seedRunningTimeSec);
  if (Number.isFinite(seed) && seed >= 0) {
    const candidateStartMs = Date.now() - seed * 1000;
    // 이미 임시 타이머가 돌아가고 있으면(참여자 즉시 진입 등) 첫 stats에서 보정한다.
    if (timerIntervalId) {
      // seed가 0이면(아직 첫 GPS/통계 전파 전) 보정하지 않고 임시 상태를 유지한다.
      if (timerIsProvisional && seed > 0 && timerStartMs !== candidateStartMs) {
        timerStartMs = candidateStartMs;
        timerIsProvisional = false;
        try {
          localStorage.setItem(storageKey("startedAtMs"), String(timerStartMs));
        } catch (e) {
          // ignore
        }
      }
      return;
    }

    if (seed > 0) {
      timerStartMs = candidateStartMs;
      timerIsProvisional = false;
      try {
        localStorage.setItem(storageKey("startedAtMs"), String(timerStartMs));
      } catch (e) {
        // ignore
      }
      startTimerTick();
      return;
    }
  }
}

// ✅ GPS/WS stats가 안 와도 IN_PROGRESS면 시간은 무조건 흐르게 한다.
function ensureTimerRunningForInProgress() {
  if (sessionStatus !== "IN_PROGRESS") return;
  if (timerIntervalId) return;

  const stored = localStorage.getItem(storageKey("startedAtMs"));
  if (stored) {
    const v = Number(stored);
    if (Number.isFinite(v) && v > 0) {
      timerStartMs = v;
      timerIsProvisional = false;
      startTimerTick();
      return;
    }
  }

  // ✅ startedAtMs를 모르는데(참여자 즉시 진입 등) 바로 localStorage에 박아버리면
  // 방장 시간과 어긋난 값이 고정될 수 있다.
  // -> 우선 임시로만 돌리고, 첫 stats(totalRunningTime)가 오면 seedTimerOnce에서 보정한다.
  timerStartMs = Date.now();
  timerIsProvisional = true;
  startTimerTick();
}

function startTimerTick() {
  if (!timerStartMs || timerIntervalId) return;

  const render = () => {
    const sec = Math.max(0, Math.floor((Date.now() - timerStartMs) / 1000));
    timeValue.textContent = formatElapsed(sec);
  };
  render();
  timerIntervalId = setInterval(render, 1000);
}

function formatElapsed(totalSec) {
  const s = Math.max(0, Number(totalSec) || 0);
  const hours = Math.floor(s / 3600);
  const minutes = Math.floor((s % 3600) / 60);
  const seconds = s % 60;
  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, "0")}:${String(
      seconds
    ).padStart(2, "0")}`;
  }
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(
    2,
    "0"
  )}`;
}

// NOTE: 예전에는 matched 진행도를 localStorage에 저장/복원했지만,
// 이제는 서버(Redis latest hostMatchedDistM) + 세션 코스경로 API가 복원을 책임진다.

function beginGpsTrackingWithResume() {
  // ✅ 솔로런도 재연결 시 GPS 추적 재개 필요 (startRunning이 이미 솔로런 지원)
  // ✅ 러닝 시작은 채팅방에서만: IN_PROGRESS가 아니면 서버 전송/추적 시작 금지
  if (sessionStatus !== "IN_PROGRESS") {
    if (!isSoloRun) {
      showToast("러닝 시작은 채팅방에서만 가능합니다.", "warn", 3500);
    }
    return;
  }
  if (!stompClient || !stompClient.connected) {
    alert("WebSocket 연결이 필요합니다. 잠시 후 다시 시도해주세요.");
    return;
  }
  startRunning();
}

// ==========================
// Load Latest Running Stats (Redis latest)
// ==========================
async function loadLatestRunningStats(sessionId) {
  const token = getAccessToken();
  if (!token) {
    throw new Error("로그인이 필요합니다.");
  }

  const response = await fetch(`/api/running/sessions/${sessionId}/stats`, {
    method: "GET",
    headers: {
      Authorization: token,
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    throw new Error("최신 러닝 통계를 불러올 수 없습니다.");
  }

  const result = await response.json();
  return result.data; // null 가능
}

// ==========================
// Load Session Data
// ==========================
async function loadSessionData(sessionId) {
  const token = getAccessToken();
  if (!token) {
    throw new Error("로그인이 필요합니다.");
  }

  const response = await fetch(`/api/match/sessions/${sessionId}`, {
    method: "GET",
    headers: {
      Authorization: token,
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    throw new Error("세션 정보를 불러올 수 없습니다.");
  }

  const result = await response.json();
  return result.data;
}

// ==========================
// Load Course Path
// ==========================
async function loadCoursePath(sessionId) {
  const token = getAccessToken();
  if (!token) {
    throw new Error("로그인이 필요합니다.");
  }

  const response = await fetch(
    `/api/running/sessions/${sessionId}/course-path`,
    {
      method: "GET",
      headers: {
        Authorization: token,
        "Content-Type": "application/json",
      },
    }
  );

  if (!response.ok) {
    throw new Error("세션 코스 경로를 불러올 수 없습니다.");
  }

  const result = await response.json();
  const pathData = result.data;

  // fullPath(원본) 파싱: 이후 stats 수신 시 trim 계산에 필요
  if (pathData.fullPath && pathData.fullPath.coordinates) {
    coursePath = pathData.fullPath.coordinates.map((coord) => ({
      lat: coord[1], // GeoJSON은 [lng, lat] 순서
      lng: coord[0],
    }));
    computeCourseCumulativeDistances();
  }

  // remainingPath(재진입 복원 결과) 파싱: 처음 렌더링은 서버가 잘라준 그대로
  if (pathData.remainingPath && pathData.remainingPath.coordinates) {
    remainingPath = pathData.remainingPath.coordinates.map((coord) => ({
      lat: coord[1],
      lng: coord[0],
    }));
  } else if (!remainingPath && coursePath && coursePath.length) {
    // ✅ 남은 경로가 이미 화면에 존재하는데(=지금까지 지운 코스가 있음)
    // 네트워크/방장 신호 끊김 등으로 remainingPath를 못 받았다고 해서
    // 전체 코스로 되돌리면 UX가 깨진다. 최초 로드 시에만 fullPath를 사용한다.
    remainingPath = [...coursePath];
  }

  // 서버가 내려준 진행도(있으면) 로컬 상태에 반영해 trim 일관성 유지
  if (pathData.hostMatchedDistM != null) {
    lastMatchedDistM = Number(pathData.hostMatchedDistM) || 0;
    lastHostAlongM = Math.max(0, lastMatchedDistM);
    if (lastHostAlongM > 0) {
      hasHostAlongMOnce = true;
      storeHostAlongM(lastHostAlongM);

      // ✅ 서버가 remainingPath를 잘라줬으면 그것을 그대로 사용
      // trimCourseByMatchedProgress를 호출하지 않음 (서버가 이미 잘라줬으므로)
      // 단, remainingPath가 없거나 잘못된 경우에만 trimCourseByMatchedProgress 호출
      if (!remainingPath || remainingPath.length === 0) {
        if (coursePath && courseCumDistM && lastHostAlongM > 0) {
          const completed = false; // 재진입 시에는 완주 상태가 아님
          trimCourseByMatchedProgress(lastHostAlongM, completed);
        }
      }
    }
  }

  // 카카오맵에 코스 경로 표시
  // ✅ remainingPath가 있으면 그것을 우선 사용 (서버가 잘라준 경로 또는 trimCourseByMatchedProgress가 업데이트한 경로)
  // remainingPath가 없으면 전체 코스(coursePath) 표시
  // ✅ map이 준비되었는지 확인 후 코스 표시
  if (map) {
  drawCoursePath();
  } else {
    // map이 아직 준비되지 않았으면 잠시 후 재시도
    setTimeout(() => {
      if (map && coursePath && coursePath.length >= 2) {
        drawCoursePath();
      }
    }, 100);
  }

  // 지도 중심을 코스 시작점으로 이동
  // ✅ 출발점 마커 표시 (서버 startLat/startLng 우선)
  const startLat =
    pathData.startLat != null
      ? Number(pathData.startLat)
      : coursePath?.[0]?.lat;
  const startLng =
    pathData.startLng != null
      ? Number(pathData.startLng)
      : coursePath?.[0]?.lng;

  // 출발점 마커 표시 (코스가 있을 때만)
  if (
    startLat != null &&
    startLng != null &&
    coursePath &&
    coursePath.length > 0
  ) {
    renderStartMarker(startLat, startLng);

    // 도착점 마커 표시 (출발점과 도착점이 다를 때만)
    const endPoint = coursePath[coursePath.length - 1];
    const isRoundTrip =
      Math.abs(startLat - endPoint.lat) < 0.0001 &&
      Math.abs(startLng - endPoint.lng) < 0.0001;

    if (!isRoundTrip) {
      renderEndMarker(endPoint.lat, endPoint.lng);
    } else {
      clearEndMarker();
    }
  }

  if (startLat != null && startLng != null) {
    const startPoint = new kakao.maps.LatLng(startLat, startLng);
    map.setCenter(startPoint);
    map.setLevel(5);
  }
}

// ==========================
// Course distance helpers (meters)
// ==========================
function computeCourseCumulativeDistances() {
  if (!coursePath || coursePath.length < 2) {
    courseCumDistM = null;
    courseSegLenM = null;
    courseTotalDistM = 0;
    return;
  }

  courseCumDistM = new Array(coursePath.length).fill(0);
  courseSegLenM = new Array(coursePath.length - 1).fill(0);
  let acc = 0;
  for (let i = 1; i < coursePath.length; i++) {
    const prev = coursePath[i - 1];
    const cur = coursePath[i];
    const seg = getDistance(prev.lat, prev.lng, cur.lat, cur.lng); // meters
    courseSegLenM[i - 1] = seg;
    acc += seg;
    courseCumDistM[i] = acc;
  }
  courseTotalDistM = acc;
}

function findCourseSegIdxByAlongM(alongM) {
  if (!courseCumDistM || !coursePath || coursePath.length < 2) return 0;
  const traveledM = Math.max(
    0,
    Math.min(courseTotalDistM, Number(alongM) || 0)
  );
  if (traveledM <= 0) return 0;
  if (traveledM >= courseTotalDistM) return Math.max(0, coursePath.length - 2);

  let lo = 0;
  let hi = courseCumDistM.length - 1;
  while (lo < hi) {
    const mid = Math.floor((lo + hi + 1) / 2);
    if (courseCumDistM[mid] <= traveledM) lo = mid;
    else hi = mid - 1;
  }
  return Math.min(lo, coursePath.length - 2);
}

function trimCourseByDistance(distanceKm) {
  if (!coursePath || !courseCumDistM || coursePath.length < 2) {
    return;
  }

  const traveledM = Math.max(
    0,
    Math.min(courseTotalDistM, (distanceKm || 0) * 1000)
  );

  // 시작점
  if (traveledM <= 0) {
    remainingPath = [...coursePath];
    if (coursePolyline) {
      const latLngs = remainingPath.map(
        (p) => new kakao.maps.LatLng(p.lat, p.lng)
      );
      coursePolyline.setPath(latLngs);
    }
    return;
  }

  // 완주
  if (traveledM >= courseTotalDistM) {
    remainingPath = [];
    if (coursePolyline) {
      coursePolyline.setMap(null);
      coursePolyline = null;
    }
    return;
  }

  // 이진 탐색: courseCumDistM[i] <= traveledM < courseCumDistM[i+1]
  let lo = 0;
  let hi = courseCumDistM.length - 1;
  while (lo < hi) {
    const mid = Math.floor((lo + hi + 1) / 2);
    if (courseCumDistM[mid] <= traveledM) lo = mid;
    else hi = mid - 1;
  }
  const i = Math.min(lo, coursePath.length - 2);
  const a = coursePath[i];
  const b = coursePath[i + 1];
  const segLen = Math.max(1, courseCumDistM[i + 1] - courseCumDistM[i]);
  const t = (traveledM - courseCumDistM[i]) / segLen;
  const interp = {
    lat: a.lat + (b.lat - a.lat) * t,
    lng: a.lng + (b.lng - a.lng) * t,
  };

  remainingPath = [interp, ...coursePath.slice(i + 1)];

  if (coursePolyline) {
    const latLngs = remainingPath.map(
      (p) => new kakao.maps.LatLng(p.lat, p.lng)
    );
    coursePolyline.setPath(latLngs);
  }
}

// ==========================
// Map-match progress: "선 위를 지나갈 때만" 경로가 사라지도록
// ==========================
function projectLngLatToXY(originLat, originLng, lat, lng) {
  // meters (근사)
  const cosLat = Math.cos((originLat * Math.PI) / 180);
  const x = (lng - originLng) * 111320.0 * cosLat;
  const y = (lat - originLat) * 110540.0;
  return { x, y };
}

function clamp(v, min, max) {
  return Math.max(min, Math.min(max, v));
}

function renderStartMarker(lat, lng) {
  if (!map || lat == null || lng == null) return;

  // 기존 마커 제거
  if (startMarker) {
    startMarker.setMap(null);
  }
  if (startMarkerOverlay) {
    startMarkerOverlay.setMap(null);
    startMarkerOverlay = null;
  }

  // 카카오 기본 파란색 마커 사용
  startMarker = new kakao.maps.Marker({
    position: new kakao.maps.LatLng(lat, lng),
  });
  startMarker.setMap(map);
}

function renderEndMarker(lat, lng) {
  if (!map || lat == null || lng == null) return;

  // 기존 마커 제거
  if (endMarker) {
    endMarker.setMap(null);
  }

  // 카카오 기본 파란색 마커 사용
  endMarker = new kakao.maps.Marker({
    position: new kakao.maps.LatLng(lat, lng),
  });
  endMarker.setMap(map);
}

function clearEndMarker() {
  if (endMarker) {
    endMarker.setMap(null);
    endMarker = null;
  }
}

function renderDirectionArrow(startLat, startLng, nextLat, nextLng) {
  if (
    !map ||
    startLat == null ||
    startLng == null ||
    nextLat == null ||
    nextLng == null
  )
    return;

  // 기존 화살표 제거
  if (directionArrowOverlay) {
    directionArrowOverlay.setMap(null);
    directionArrowOverlay = null;
  }

  // 방향 각도 계산 (도 단위, 동쪽이 0도, 시계방향)
  // 카카오맵에서 화살표는 동쪽(오른쪽)이 0도
  const dLng = nextLng - startLng;
  const dLat = nextLat - startLat;
  // atan2(y, x) = atan2(dLat, dLng) → 동쪽이 0도
  const angle = (Math.atan2(dLat, dLng) * 180) / Math.PI;

  // 화살표 HTML 생성 (SVG 사용)
  const el = document.createElement("div");
  el.innerHTML = `
    <svg width="40" height="40" viewBox="0 0 40 40" style="filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));">
      <defs>
        <marker id="arrowhead" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
          <polygon points="0 0, 10 3, 0 6" fill="#1e88e5" />
        </marker>
      </defs>
      <line x1="20" y1="20" x2="20" y2="5" 
            stroke="#1e88e5" 
            stroke-width="3" 
            marker-end="url(#arrowhead)"
            transform="rotate(${angle} 20 20)" />
    </svg>
  `;
  el.style.cssText = `
    width: 40px;
    height: 40px;
    pointer-events: none;
  `;

  directionArrowOverlay = new kakao.maps.CustomOverlay({
    position: new kakao.maps.LatLng(startLat, startLng),
    content: el,
    yAnchor: 0.5,
    xAnchor: 0.5,
  });
  directionArrowOverlay.setMap(map);
}

function clearDirectionArrow() {
  if (directionArrowOverlay) {
    directionArrowOverlay.setMap(null);
    directionArrowOverlay = null;
  }
}

function setRunnerPinOverlay(which, lat, lng) {
  if (!map || lat == null || lng == null) return;

  const el = document.createElement("div");
  el.className = `runner-pin${which === "host" ? " host" : ""}`;
  el.innerHTML = `<div class="runner-pin-inner"></div>`;

  const overlay = new kakao.maps.CustomOverlay({
    position: new kakao.maps.LatLng(lat, lng),
    content: el,
    yAnchor: 0.5,
    xAnchor: 0.5,
  });
  overlay.setMap(map);

  overlay.__el = el;

  if (which === "host") {
    if (hostPinOverlay) hostPinOverlay.setMap(null);
    hostPinOverlay = overlay;
  } else {
    if (userPinOverlay) userPinOverlay.setMap(null);
    userPinOverlay = overlay;
  }
}

function updateRunnerPinOverlay(which, lat, lng) {
  const overlay = which === "host" ? hostPinOverlay : userPinOverlay;
  if (!overlay || lat == null || lng == null) return;
  overlay.setPosition(new kakao.maps.LatLng(lat, lng));
}

function matchProgressOnCourse(lat, lng) {
  if (
    !coursePath ||
    !courseCumDistM ||
    !courseSegLenM ||
    coursePath.length < 2
  ) {
    return null;
  }

  // 정확도 기반 허용 오차: 너무 빡세면 GPS 오차로 진행이 안 됨
  // - 체감상 선 지우기 "조금 더" 잘 되도록 허용 범위를 소폭 확대
  const baseTol = 12; // m
  // ✅ 실환경 accuracy가 30~100m로 흔들리는 경우가 많아, 진행 매칭 허용치를 조금 넉넉히 잡는다.
  // - 너무 넓으면 "선이 튀어서 지워짐"이 생길 수 있어 상한을 둔다.
  const tol = clamp(
    lastGpsAccuracyM != null && Number.isFinite(lastGpsAccuracyM)
      ? lastGpsAccuracyM * 1.2
      : baseTol,
    baseTol,
    80
  );

  // 루프/교차에서 멀리 점프하지 않게, 이전 매칭 세그먼트 주변만 탐색
  const from = clamp(lastMatchedSegIdx - 5, 0, coursePath.length - 2);
  const to = clamp(lastMatchedSegIdx + 60, 0, coursePath.length - 2);

  const originLat = lat;
  const originLng = lng;
  const p = projectLngLatToXY(originLat, originLng, lat, lng);

  let best = {
    segIdx: from,
    t: 0,
    distM: Number.POSITIVE_INFINITY,
    alongM: 0,
  };

  for (let i = from; i <= to; i++) {
    const aLL = coursePath[i];
    const bLL = coursePath[i + 1];

    const a = projectLngLatToXY(originLat, originLng, aLL.lat, aLL.lng);
    const b = projectLngLatToXY(originLat, originLng, bLL.lat, bLL.lng);

    const vx = b.x - a.x;
    const vy = b.y - a.y;
    const wx = p.x - a.x;
    const wy = p.y - a.y;

    const vv = vx * vx + vy * vy;
    if (vv <= 0.000001) {
      continue;
    }

    const t = clamp((wx * vx + wy * vy) / vv, 0, 1);
    const cx = a.x + t * vx;
    const cy = a.y + t * vy;
    const dx = p.x - cx;
    const dy = p.y - cy;
    const d = Math.sqrt(dx * dx + dy * dy); // meters

    if (d < best.distM) {
      const segLen = courseSegLenM[i] || 0;
      const alongM = (courseCumDistM[i] || 0) + t * segLen;
      best = { segIdx: i, t, distM: d, alongM };
    }
  }

  if (!Number.isFinite(best.distM)) {
    return null;
  }

  // ✅ 진행 인정 여부는 matched로 분리 (이탈 체크는 distM 기반으로 판단)
  return {
    ...best,
    tolM: tol,
    matched: best.distM <= tol,
  };
}

function trimCourseByMatchedProgress(alongM, isCompleted = false) {
  if (!coursePath || !courseCumDistM || coursePath.length < 2) {
    return;
  }

  const traveledM = Math.max(0, Math.min(courseTotalDistM, alongM || 0));

  if (traveledM <= 0) {
    remainingPath = [...coursePath];
    if (coursePolyline) {
      const latLngs = remainingPath.map(
        (p) => new kakao.maps.LatLng(p.lat, p.lng)
      );
      coursePolyline.setPath(latLngs);
    }
    return;
  }

  // ✅ 완주 판정이 확실할 때만 코스를 완전히 제거
  if (isCompleted && traveledM >= courseTotalDistM) {
    remainingPath = [];
    if (coursePolyline) {
      coursePolyline.setMap(null);
      coursePolyline = null;
    }
    return;
  }

  // ✅ 완주 전에는 코스를 완전히 제거하지 않고, 최대 courseTotalDistM - 0.1까지만 트리밍
  const safeTraveledM = isCompleted
    ? traveledM
    : Math.min(traveledM, Math.max(0, courseTotalDistM - 0.1));

  // 이진 탐색: courseCumDistM[i] <= safeTraveledM < courseCumDistM[i+1]
  let lo = 0;
  let hi = courseCumDistM.length - 1;
  while (lo < hi) {
    const mid = Math.floor((lo + hi + 1) / 2);
    if (courseCumDistM[mid] <= safeTraveledM) lo = mid;
    else hi = mid - 1;
  }
  const i = Math.min(lo, coursePath.length - 2);
  const a = coursePath[i];
  const b = coursePath[i + 1];
  const segLen = Math.max(
    1,
    (courseCumDistM[i + 1] || 0) - (courseCumDistM[i] || 0)
  );
  const t = (safeTraveledM - (courseCumDistM[i] || 0)) / segLen;
  const interp = {
    lat: a.lat + (b.lat - a.lat) * t,
    lng: a.lng + (b.lng - a.lng) * t,
  };

  remainingPath = [interp, ...coursePath.slice(i + 1)];

  if (coursePolyline) {
    const latLngs = remainingPath.map(
      (p) => new kakao.maps.LatLng(p.lat, p.lng)
    );
    coursePolyline.setPath(latLngs);
  }
}

// ==========================
// Initialize Kakao Map
// ==========================
function initKakaoMap() {
  return new Promise((resolve, reject) => {
    // ✅ SDK 로드 실패(도메인 미등록/키 오류/네트워크 차단)일 때 여기서 명확히 잡힘
    if (!window.kakao || !window.kakao.maps) {
      reject(
        new Error(
          "Kakao Maps SDK가 로드되지 않았습니다. (카카오 콘솔 Web 도메인 등록 / JS 키 / 네트워크를 확인하세요)"
        )
      );
      return;
    }

    kakao.maps.load(() => {
      const defaultPosition = new kakao.maps.LatLng(37.5665, 126.978); // 서울시청

      const mapOption = {
        center: defaultPosition,
        level: 3,
      };

      map = new kakao.maps.Map(mapContainer, mapOption);
      
      // ✅ 뷰포트 높이 재계산 및 지도 relayout (카카오맵 잘림 방지)
      const relayoutMap = () => {
        setViewportHeightVar();
        if (map) {
          try {
            // ✅ 지도 컨테이너 크기 확인
            const container = mapContainer;
            if (container) {
              const height = container.offsetHeight;
              const width = container.offsetWidth;
              // ✅ 지도 크기가 0이면 재시도 스케줄링
              if (height === 0 || width === 0) {
                console.warn("지도 컨테이너 크기가 0입니다. 재시도합니다.");
                return;
              }
            }
            map.relayout();
          } catch (e) {
            console.warn("지도 relayout 실패:", e);
          }
        }
      };
      
      // ✅ 즉시 relayout
      relayoutMap();
      scheduleMapRelayout();
      
      // ✅ 지도 초기화 후 여러 번 relayout 호출 (카카오맵 잘림 방지)
      setTimeout(relayoutMap, 100);
      setTimeout(relayoutMap, 300);
      setTimeout(relayoutMap, 600);
      setTimeout(relayoutMap, 1000);
      setTimeout(relayoutMap, 1500);
      
      // ✅ resize 이벤트 핸들러에 추가 relayout (이미 있는 핸들러와 중복되지만 안전장치)
      const existingRelayout = () => {
        setViewportHeightVar();
        scheduleMapRelayout();
      };
      
      // ✅ 기존 핸들러가 있어도 추가로 relayoutMap 호출
      window.addEventListener('resize', () => {
        existingRelayout();
        relayoutMap();
      }, { passive: true });
      
      if (window.visualViewport) {
        window.visualViewport.addEventListener('resize', () => {
          existingRelayout();
          relayoutMap();
        }, { passive: true });
      }

      // ✅ Geocoder는 services가 로드된 후에 초기화
      try {
        if (kakao.maps.services && kakao.maps.services.Geocoder) {
      geocoder = new kakao.maps.services.Geocoder();
        } else {
          // services가 아직 로드되지 않았으면 지연 초기화
          setTimeout(() => {
            if (kakao.maps.services && kakao.maps.services.Geocoder) {
              geocoder = new kakao.maps.services.Geocoder();
            }
          }, 100);
        }
      } catch (e) {
        console.warn("Geocoder 초기화 실패 (나중에 재시도):", e);
        // 지연 초기화 시도
        setTimeout(() => {
          try {
            if (kakao.maps.services && kakao.maps.services.Geocoder) {
              geocoder = new kakao.maps.services.Geocoder();
            }
          } catch (e2) {
            console.warn("Geocoder 초기화 재시도 실패:", e2);
          }
        }, 200);
      }

      // 사용자가 지도를 움직이면 자동 따라가기(follow) 해제
      try {
        kakao.maps.event.addListener(map, "dragstart", () => {
          isFollowing = false;
        });
        kakao.maps.event.addListener(map, "zoom_changed", () => {
          isFollowing = false;
        });
      } catch (e) {
        // ignore
      }

      // 사용자 위치 마커 생성
      const markerImageSrc =
        "https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/marker_red.png";
      const markerImageSize = new kakao.maps.Size(24, 35);
      const markerImage = new kakao.maps.MarkerImage(
        markerImageSrc,
        markerImageSize
      );

      userMarker = new kakao.maps.Marker({
        position: defaultPosition,
        image: markerImage,
      });

      // 기본 마커는 유지하되(호환), 러닝앱 스타일 핀(CustomOverlay)을 우선 사용한다.
      // - 실제 GPS 업데이트가 오기 전에는 마커를 숨김
      userMarker.setMap(null);

      // 기본은 follow OFF (사용자가 보고 싶은대로 움직일 수 있게)
      isFollowing = false;

      resolve();
    });
  });
}

// ==========================
// Draw Course Path
// ==========================
function drawCoursePath() {
  if (!coursePath || coursePath.length < 2) {
    console.warn("코스 경로가 없거나 충분하지 않습니다.");
    return;
  }

  if (!map) {
    console.warn("카카오맵이 초기화되지 않았습니다.");
    return;
  }

  // 기존 폴리라인 제거
  if (coursePolyline) {
    coursePolyline.setMap(null);
  }

  // 카카오맵 좌표로 변환
  // ✅ 재진입 복원: 서버가 내려준 remainingPath가 있으면 그걸 우선 렌더링
  const displayPath =
    remainingPath && Array.isArray(remainingPath) && remainingPath.length >= 2
      ? remainingPath
      : coursePath;

  const latLngs = displayPath.map(
    (point) => new kakao.maps.LatLng(point.lat, point.lng)
  );

  // 폴리라인 생성
  coursePolyline = new kakao.maps.Polyline({
    path: latLngs,
    strokeWeight: 5,
    strokeColor: "#ff3d00",
    strokeOpacity: 0.8,
    strokeStyle: "solid",
  });

  coursePolyline.setMap(map);
  console.log("코스 경로 표시 완료:", displayPath.length, "개 포인트");
}

// ==========================
// Update Remaining Path
// ==========================
function updateRemainingPath(userLat, userLng) {
  if (!remainingPath || remainingPath.length === 0) {
    return;
  }

  // 사용자 위치와 가장 가까운 경로상의 점 찾기
  let closestIndex = 0;
  let minDistance = Infinity;

  remainingPath.forEach((point, index) => {
    const distance = getDistance(userLat, userLng, point.lat, point.lng); // ✅ meters
    if (distance < minDistance) {
      minDistance = distance;
      closestIndex = index;
    }
  });

  // ✅ getDistance는 "미터"인데 기존엔 0.0005로 비교하고 있었음(버그)
  // 사용자가 경로에 가까이 있으면 (50m 이내) 지나온 부분 제거
  if (minDistance < 50) {
    remainingPath = remainingPath.slice(closestIndex + 1);

    if (remainingPath.length > 1) {
      const latLngs = remainingPath.map(
        (point) => new kakao.maps.LatLng(point.lat, point.lng)
      );
      if (coursePolyline) {
        coursePolyline.setPath(latLngs);
      }
    } else {
      // 경로 완주
      if (coursePolyline) {
        coursePolyline.setMap(null);
        coursePolyline = null;
      }
    }
  }
}

// ==========================
// Calculate Distance (Haversine formula)
// ==========================
function getDistance(lat1, lng1, lat2, lng2) {
  const R = 6371000; // Earth radius in meters
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

// ==========================
// Connect WebSocket
// ==========================
function connectWebSocket() {
  if (typeof Stomp === "undefined") {
    console.error("Stomp 라이브러리가 로드되지 않았습니다.");
    return;
  }

  if (typeof SockJS === "undefined") {
    console.error("SockJS 라이브러리가 로드되지 않았습니다.");
    return;
  }

  wsManualDisconnect = false;

  const socket = new SockJS("/ws");
  stompClient = Stomp.over(socket);
  stompClient.debug = null;

  const token = localStorage.getItem("accessToken");
  const headers = token ? { Authorization: "Bearer " + token } : {};

  // SockJS close/reconnect 알림
  try {
    socket.onclose = () => {
      if (wsManualDisconnect) return;
      // ✅ 러닝 종료 시 웹소켓 연결 종료 TTS 비활성화
      // ✅ 오프라인과 솔로런 모두 WebSocket TTS 작동
      if (!completedHandled) {
        showToast("WebSocket 연결이 종료되었습니다", "warn", 3500);
        if (ttsReady && window.TtsManager && !completedHandled) {
          window.TtsManager.speak("WEB_SOCKET_DISCONNECTED");
        }
        scheduleWebSocketReconnect();
      }
    };
  } catch (e) {
    // ignore
  }

  stompClient.connect(
    headers,
    () => {
      console.log("WebSocket 연결 성공");
      if (wsReconnectAttempt > 0) {
        // ✅ 러닝 종료 후에는 TTS/토스트 안 나오게
        // ✅ 오프라인과 솔로런 모두 WebSocket 재연결 TTS 작동
        if (!completedHandled) {
          showToast("WebSocket이 다시 연결되었습니다", "success", 2500);
          if (ttsReady && window.TtsManager && !completedHandled) {
            window.TtsManager.speak("WEB_SOCKET_RECONNECTED");
          }
        }
        wsReconnectAttempt = 0;
      }
      if (wsReconnectTimerId) {
        clearTimeout(wsReconnectTimerId);
        wsReconnectTimerId = null;
      }
      subscribeToRunningStats();
      subscribeToRunningErrors();
      subscribeToChatMessages();

      // ✅ 재진입 시 경로 다시 로드 (코스가 있을 때만)
      if (sessionCourseId) {
        loadCoursePath(sessionId).catch((e) => {
          console.warn("경로 재로드 실패:", e);
        });
      }

      // ✅ TTS Manager의 Set 초기화 (재진입 시 중복 방지)
      // WebSocket 연결 후 첫 stats 수신 전에 resetDistanceState를 호출하여 이미 지나간 거리 TTS 방지
      if (
        window.TtsManager &&
        typeof window.TtsManager.resetDistanceState === "function" &&
        latestStatsCache
      ) {
        // ✅ 재진입 시 현재 남은 거리를 전달하여 이미 지나간 거리 TTS만 스킵
        let remainingDistance = latestStatsCache.remainingDistance;
        const targetDistance = sessionDataCache?.targetDistance; // km
        const totalDistance = latestStatsCache.totalDistance; // km

        // ✅ 솔로런 코스 없이 뛸 때: remainingDistance가 없으면 targetDistance와 totalDistance로 계산
        if (remainingDistance == null || remainingDistance === undefined) {
          if (
            Number.isFinite(targetDistance) &&
            Number.isFinite(totalDistance)
          ) {
            remainingDistance = Math.max(0, targetDistance - totalDistance);
          }
        }
        
        // ✅ 목표 거리를 초과하는 남은 거리는 목표 거리로 제한
        if (Number.isFinite(targetDistance) && Number.isFinite(totalDistance) && remainingDistance != null) {
          const maxRemainingDistance = Math.max(0, targetDistance - totalDistance);
          remainingDistance = Math.min(remainingDistance, maxRemainingDistance);
        }

        if (remainingDistance != null) {
          // 현재 남은 거리를 전달하여 이미 지나간 거리 TTS 스킵
          window.TtsManager.resetDistanceState(remainingDistance);
        } else {
          // 거리 정보가 없으면 기존 방식으로 리셋
          window.TtsManager.resetDistanceState();
        }
      }

      // ✅ stats가 아직 안 와도 시간은 흐르도록 보장
      ensureTimerRunningForInProgress();

      // 러닝 페이지는 채팅방에서 시작 버튼을 눌러 들어오는 흐름이므로,
      // 세션이 이미 IN_PROGRESS면(러닝 중) 방장은 자동으로 추적을 재개한다.
      if (isHost && sessionStatus === "IN_PROGRESS" && !isRunning) {
        try {
          beginGpsTrackingWithResume();
        } catch (e) {
          console.warn("자동 재개 실패:", e);
        }
      }
    },
    (error) => {
      console.error("WebSocket 연결 실패:", error);
      // ✅ 오프라인과 솔로런 모두 WebSocket 연결 실패 TTS 작동
      if (!wsManualDisconnect && !completedHandled) {
        showToast(
          "WebSocket 연결에 실패했습니다. 재연결 시도 중...",
          "warn",
          3500
        );
        if (ttsReady && window.TtsManager && !completedHandled) {
          window.TtsManager.speak("WEB_SOCKET_DISCONNECTED");
        }
        scheduleWebSocketReconnect();
      }
    }
  );
}

function scheduleWebSocketReconnect() {
  if (wsManualDisconnect) return;
  if (wsReconnectTimerId) return;
  // ✅ 러닝 종료 후에는 재연결 시도 안 함
  // ✅ 오프라인과 솔로런 모두 WebSocket 재연결 중 TTS 작동
  if (completedHandled) return;
  wsReconnectAttempt += 1;
  if (ttsReady && window.TtsManager && !completedHandled) {
    window.TtsManager.speak("WEB_SOCKET_RECONNECTING");
  }
  const delay = clamp(
    Math.floor(900 * Math.pow(1.6, wsReconnectAttempt)),
    1200,
    8000
  );
  wsReconnectTimerId = setTimeout(() => {
    wsReconnectTimerId = null;
    if (wsManualDisconnect) return;
    try {
      // 기존 stompClient가 남아있을 수 있으니 정리
      if (stompClient && stompClient.connected) {
        stompClient.disconnect();
      }
    } catch (e) {
      // ignore
    }
    stompClient = null;
    gpsSubscription = null;
    connectWebSocket();
  }, delay);
}

// ==========================
// Subscribe to Running Stats
// ==========================
function subscribeToRunningStats() {
  if (!stompClient || !stompClient.connected) {
    console.error("❌ WebSocket 연결 없음");
    return;
  }

  if (gpsSubscription) {
    console.log("⚠️ 이미 런닝 통계를 구독 중입니다");
    return;
  }

  gpsSubscription = stompClient.subscribe(
    `/sub/running/${sessionId}`,
    (message) => {
      const stats = JSON.parse(message.body);
      handleRunningStats(stats);
    }
  );

  console.log("✅ 런닝 통계 구독 완료:", `/sub/running/${sessionId}`);
}

// ==========================
// Subscribe to Running Errors
// ==========================
function subscribeToRunningErrors() {
  if (!stompClient || !stompClient.connected) {
    console.error("❌ WebSocket 연결 없음 (에러 구독)");
    return;
  }

  stompClient.subscribe(`/sub/running/${sessionId}/errors`, (message) => {
    const error = JSON.parse(message.body);
    console.error("러닝 에러:", error);
    alert(`러닝 오류: ${error.message}`);
  });

  console.log("✅ 런닝 에러 구독 완료:", `/sub/running/${sessionId}/errors`);
}

// ==========================
// Subscribe to Chat Messages (for running end system message)
// ==========================
function subscribeToChatMessages() {
  if (!stompClient || !stompClient.connected) {
    console.error("❌ WebSocket 연결 없음 (채팅 구독)");
    return;
  }

  // 채팅 메시지 구독 (러닝 종료 시스템 메시지 수신용)
  stompClient.subscribe(`/sub/chat/${sessionId}`, (message) => {
    try {
      const data = JSON.parse(message.body);
      // ✅ 러닝 종료 시스템 메시지 수신 시 결과 모달 표시 (참여자)
      if (
        data.messageType === "SYSTEM" &&
        (data.content.includes("런닝이 종료") ||
          data.content.includes("러닝이 종료"))
      ) {
        console.log("🏁 런닝 종료 시스템 메시지 수신 - 결과 모달 표시");

        // ✅ 종료 이벤트 처리: TTS 즉시 중단, 큐 비우기, 종료 멘트만 재생, 이후 Lock
        if (ttsReady && window.TtsManager) {
          // 1. 현재 재생 중인 TTS 즉시 중단
          if (typeof window.TtsManager.stopAll === "function") {
            window.TtsManager.stopAll();
          } else if (typeof window.TtsManager.stop === "function") {
            window.TtsManager.stop();
          }

          // 2. 재생 대기 큐 비우기
          if (typeof window.TtsManager.clearQueue === "function") {
            window.TtsManager.clearQueue();
          } else if (typeof window.TtsManager.clear === "function") {
            window.TtsManager.clear();
          }

          // 3. 종료 멘트('러닝이 종료되었습니다')만 1회 재생
          window.TtsManager.speak("END_RUN", {
            priority: 2,
            cooldownMs: 0,
          })
            .then(() => {
              // 4. 재생이 끝나면 TTS Lock(이후 어떤 TTS 요청도 무시)
              completedHandled = true;
            })
            .catch(() => {
              // 에러가 나도 Lock 설정
              completedHandled = true;
            });

          // Promise를 지원하지 않는 경우를 대비한 fallback
          setTimeout(() => {
            completedHandled = true;
          }, 3000);
        } else {
          // TTS가 없으면 즉시 설정
          completedHandled = true;
        }

        // ✅ 오프라인 종료 시 결과 모달 정책:
        // - 코스 있음: 방장/참여자 모두 즉시 결과 모달(로딩→결과)
        // - 코스 없음: 참여자는 즉시 결과 모달(계속 대기), 방장은 코스 저장 후 결과 모달
        const loadingText =
          sessionCourseId == null ? "코스 저장중입니다…" : "러닝 결과 저장중입니다…";

        // 방장 + 코스 없음은 코스 저장 플로우가 우선이므로 여기서는 모달을 띄우지 않는다.
        if (isHost && sessionCourseId == null) {
          return;
        }

        startResultModalPolling(loadingText);
      }
    } catch (e) {
      console.warn("채팅 메시지 파싱 실패:", e);
    }
  });

  console.log("✅ 채팅 메시지 구독 완료:", `/sub/chat/${sessionId}`);
}

// ==========================
// Disconnect WebSocket
// ==========================
function disconnectWebSocket() {
  wsManualDisconnect = true;
  if (wsReconnectTimerId) {
    clearTimeout(wsReconnectTimerId);
    wsReconnectTimerId = null;
  }

  if (runningTracker) {
    runningTracker.stopTracking();
    runningTracker = null;
  }

  if (gpsSubscription) {
    gpsSubscription.unsubscribe();
    gpsSubscription = null;
  }

  if (stompClient && stompClient.connected) {
    stompClient.disconnect();
    console.log("WebSocket 연결 종료");
  }
}

// ==========================
// Handle Running Stats
// ==========================
function handleRunningStats(stats) {
  console.log("📊 통계 수신:", stats);
  lastStatsAtMs = Date.now();
  // ✅ localStorage에 lastStatsAtMs 업데이트
  updateLastStatsAtMs();
  // ✅ 페이지 전환(러닝↔채팅) 시 누적거리/시간이 리셋되지 않도록 스냅샷 저장
  cacheLatestStatsSnapshot(stats);
  if (hostSignalLost) {
    hostSignalLost = false;
    // ✅ 솔로런에서는 "GPS 신호", 오프라인에서는 "방장 신호"
    const signalText = isSoloRun
      ? "GPS 신호가 다시 잡혔습니다"
      : "방장 신호가 다시 잡혔습니다";
    showToast(signalText, "success", 2500);
    // ✅ TTS: 방장과 참여자 모두 듣도록
    if (ttsReady && window.TtsManager && !completedHandled) {
      window.TtsManager.speak("HOST_SIGNAL_RESUMED");
    }
  }

  // 최신 stats 캐시 (센터링/호스트 위치 표시에 사용)
  latestStatsCache = stats;
  const completed = (stats.isCompleted ?? stats.completed) === true;

  if (stats.totalDistance !== undefined) {
    distanceValue.textContent = `${stats.totalDistance.toFixed(2)}km`;
    // TTS: 거리/남은거리 (참여자도 들리도록 isHost 체크 제거)
    // ✅ 솔로런(코스 없음)도 오프라인과 동일하게 TTS 작동: remainingDistance가 없으면 targetDistance로 계산
    if (ttsReady && window.TtsManager && !completedHandled) {
      let remainingDistance = stats.remainingDistance;
      const targetDistance = sessionDataCache?.targetDistance; // km
      const totalDistance = stats.totalDistance; // km
      
      // remainingDistance가 없으면 세션의 targetDistance로 계산
      if (remainingDistance == null || remainingDistance === undefined) {
        if (Number.isFinite(targetDistance) && Number.isFinite(totalDistance)) {
          remainingDistance = Math.max(0, targetDistance - totalDistance);
        }
      }
      
      // ✅ 목표 거리를 초과하는 남은 거리 TTS는 재생하지 않음
      // 예: 목표 0.2km인데 코스의 remainingDistance가 0.3km면, 목표 기준으로 0.2km만 사용
      if (Number.isFinite(targetDistance) && Number.isFinite(totalDistance) && remainingDistance != null) {
        const maxRemainingDistance = Math.max(0, targetDistance - totalDistance);
        remainingDistance = Math.min(remainingDistance, maxRemainingDistance);
      }

      // ✅ 재진입/재연결 시 첫 stats 수신 시 resetDistanceState 호출하여 이미 지나간 거리 TTS 방지
      // latestStatsCache가 있고, 현재 stats의 totalDistance가 이전과 같거나 작으면 재진입으로 간주
      if (
        latestStatsCache &&
        latestStatsCache.totalDistance != null &&
        stats.totalDistance != null
      ) {
        const prevTotalDistance = latestStatsCache.totalDistance;
        const currentTotalDistance = stats.totalDistance;
        // 재진입/재연결 시: 거리가 증가하지 않았거나 같으면 resetDistanceState 호출
        if (
          currentTotalDistance <= prevTotalDistance ||
          (prevTotalDistance > 0 &&
            Math.abs(currentTotalDistance - prevTotalDistance) < 0.001)
        ) {
          if (typeof window.TtsManager.resetDistanceState === "function") {
            let resetRemainingDistance = remainingDistance;
            const resetTargetDistance = sessionDataCache?.targetDistance;
            const resetTotalDistance = stats.totalDistance;
            
            if (
              resetRemainingDistance == null ||
              resetRemainingDistance === undefined
            ) {
              if (
                Number.isFinite(resetTargetDistance) &&
                Number.isFinite(resetTotalDistance)
              ) {
                resetRemainingDistance = Math.max(
                  0,
                  resetTargetDistance - resetTotalDistance
                );
              }
            }
            
            // ✅ 목표 거리를 초과하는 남은 거리는 목표 거리로 제한
            if (Number.isFinite(resetTargetDistance) && Number.isFinite(resetTotalDistance) && resetRemainingDistance != null) {
              const maxResetRemainingDistance = Math.max(0, resetTargetDistance - resetTotalDistance);
              resetRemainingDistance = Math.min(resetRemainingDistance, maxResetRemainingDistance);
            }
            
            if (resetRemainingDistance != null) {
              window.TtsManager.resetDistanceState(resetRemainingDistance);
            } else {
              window.TtsManager.resetDistanceState();
            }
          }
        }
      }

      // ✅ 시작 직후(stats.totalDistance가 0일 때)는 거리 TTS 재생하지 않음
      // ✅ 시작 시점에 resetDistanceState 호출하여 목표 거리 초과 TTS 방지
      if (stats.totalDistance === 0 && remainingDistance != null) {
        // 시작 시점에 목표 거리를 초과하는 남은 거리가 있으면 resetDistanceState 호출
        const targetDistance = sessionDataCache?.targetDistance;
        if (Number.isFinite(targetDistance)) {
          // 목표 거리로 제한하여 resetDistanceState 호출
          const limitedRemainingDistance = Math.min(remainingDistance, targetDistance);
          if (typeof window.TtsManager.resetDistanceState === "function") {
            window.TtsManager.resetDistanceState(limitedRemainingDistance);
          }
        } else {
          // 목표 거리가 없으면 그대로 resetDistanceState 호출
          if (typeof window.TtsManager.resetDistanceState === "function") {
            window.TtsManager.resetDistanceState(remainingDistance);
          }
        }
        // ✅ 시작 시점에는 TTS 재생하지 않음
        return;
      }
      
      // ✅ 실제로 뛰기 시작한 후에만 거리 TTS 재생
      // ✅ 목표 거리를 초과하는 남은 거리가 있으면 TTS 재생하지 않음
      if (stats.totalDistance > 0) {
        // 목표 거리 확인
        const targetDistance = sessionDataCache?.targetDistance;
        if (Number.isFinite(targetDistance)) {
          const maxRemainingDistance = Math.max(0, targetDistance - stats.totalDistance);
          // remainingDistance가 목표 거리를 초과하면 TTS 재생하지 않음
          if (remainingDistance != null && remainingDistance > maxRemainingDistance) {
            // 목표 거리로 제한하여 TTS 재생
            const limitedRemainingDistance = maxRemainingDistance;
            window.TtsManager.onDistance(stats.totalDistance, limitedRemainingDistance);
          } else {
            window.TtsManager.onDistance(stats.totalDistance, remainingDistance);
          }
        } else {
          // 목표 거리가 없으면 그대로 TTS 재생
          window.TtsManager.onDistance(stats.totalDistance, remainingDistance);
        }
      }
    }
    // ✅ 방장/참여자 모두: 서버가 브로드캐스트하는 hostMatchedDistM(코스 위 진행도) 기준으로 트리밍
    // - 러닝 시작(IN_PROGRESS) 이후에만 선이 사라지게 한다
    if (sessionStatus === "IN_PROGRESS" && coursePath && courseCumDistM) {
      const hostAlongM =
        stats.hostMatchedDistM != null &&
        Number.isFinite(stats.hostMatchedDistM)
          ? stats.hostMatchedDistM
          : null;

      if (hostAlongM != null && hostAlongM >= 0) {
        const candidate = Math.max(lastHostAlongM, hostAlongM);

        // ✅ 시작 직후(20m 미만, 5초 미만)에는 트리밍하지 않음 (백엔드와 동일한 로직)
        const totalDistKm = stats.totalDistance || 0;
        const totalTimeSec = stats.totalRunningTime || 0;
        const isJustStarted = totalDistKm < 0.02 && totalTimeSec < 5;

        if (!isJustStarted) {
          // ✅ 완주 전에는 코스를 완전히 제거하지 않고, 최대 courseTotalDistM - 0.1까지만 트리밍
          let safeAlongM = candidate;
          if (!completed && Number.isFinite(courseTotalDistM)) {
            safeAlongM = Math.min(
              safeAlongM,
              Math.max(0, courseTotalDistM - 0.1)
            );
          }

          lastHostAlongM = candidate;
          hasHostAlongMOnce = hasHostAlongMOnce || lastHostAlongM > 0;
          storeHostAlongM(lastHostAlongM);

          trimCourseByMatchedProgress(safeAlongM, completed);
        }
      } else if (!hasHostAlongMOnce) {
        // ✅ 서버에서 값을 받지 못한 경우에만 localStorage fallback 사용
        // (재진입 시에는 loadCoursePath에서 서버로부터 받아오므로 이 경우는 드뭅니다)
        const stored = getStoredHostAlongM();
        if (stored > 0) {
          lastHostAlongM = Math.max(lastHostAlongM, stored);
          hasHostAlongMOnce = true;
        }
      } else if (!isHost) {
        // 참가자 fallback: 거리(km) 기반(정밀도 낮음)
        trimCourseByDistance(stats.totalDistance);
      }
    }
  }

  const serverPace = Number(stats.teamAveragePace);
  if (Number.isFinite(serverPace) && serverPace > 0) {
    const minutes = Math.floor(serverPace);
    const seconds = Math.floor((serverPace - minutes) * 60);
    lastPaceText = `${minutes}'${seconds.toString().padStart(2, "0")}''`;
    paceValue.textContent = lastPaceText;
  } else {
    // ✅ 서버가 pace를 안 내려주거나 이상값이면 직전 표시를 유지
    paceValue.textContent = lastPaceText;
  }

  // TTS: 1km split 기반 페이스(방장 기준) - DIST_DONE + PACE 함께
  if (
    ttsReady &&
    window.TtsManager &&
    stats.segmentPaces &&
    !completedHandled
  ) {
    window.TtsManager.onSplitPaces(stats.segmentPaces);
  }

  // 시간은 "seed 후 계속 흐르게" (stats로 매번 덮어쓰지 않음)
  if (stats.totalRunningTime !== undefined) {
    // 완주 상태면 시간은 멈춰야 한다
    if (completed || sessionStatus === "COMPLETED") {
      stopTimerAndFreeze(stats.totalRunningTime);
    } else {
      seedTimerOnce(stats.totalRunningTime);
    }
  }

  if (completed) {
    // 코스가 있다면 선을 완전히 제거 (남은 경로 0)
    if (coursePath && courseCumDistM) {
      trimCourseByMatchedProgress(courseTotalDistM, true);
    }

    // ✅ 종료 이벤트 처리: TTS 즉시 중단, 큐 비우기, 종료 멘트만 재생, 이후 Lock
    if (ttsReady && window.TtsManager && !completedHandled) {
      // 1. 현재 재생 중인 TTS 즉시 중단
      if (typeof window.TtsManager.stopAll === "function") {
        window.TtsManager.stopAll();
      } else if (typeof window.TtsManager.stop === "function") {
        window.TtsManager.stop();
      }

      // 2. 재생 대기 큐 비우기
      if (typeof window.TtsManager.clearQueue === "function") {
        window.TtsManager.clearQueue();
      } else if (typeof window.TtsManager.clear === "function") {
        window.TtsManager.clear();
      }

      // 3. 종료 멘트('러닝이 종료되었습니다')만 1회 재생
      const endRunPromise = window.TtsManager.speak("END_RUN", {
        priority: 2,
        cooldownMs: 0,
      });

      if (endRunPromise && typeof endRunPromise.then === "function") {
        endRunPromise
          .then(() => {
            // 4. 재생이 끝나면 TTS Lock(이후 어떤 TTS 요청도 무시)
            completedHandled = true;
          })
          .catch(() => {
            // 에러가 나도 Lock 설정
            completedHandled = true;
          });
      } else {
        // Promise를 지원하지 않는 경우를 대비한 fallback
        setTimeout(() => {
          completedHandled = true;
        }, 3000);
      }
    }

    handleCompletedOnce(stats);
  }

  // ✅ 참가자 화면: 방장 GPS 위치/방향 표시 (솔로런이 아닐 때만)
  if (
    !isSoloRun &&
    !isHost &&
    stats.hostLatitude != null &&
    stats.hostLongitude != null
  ) {
    if (!hostPinOverlay) {
      setRunnerPinOverlay("host", stats.hostLatitude, stats.hostLongitude);
    } else {
      updateRunnerPinOverlay("host", stats.hostLatitude, stats.hostLongitude);
    }

    // 참가자는 내 marker는 의미 없으니 숨김
    if (userMarker) userMarker.setMap(null);

    if (hostHeadingOverlay) {
      updateHeadingOverlay(
        hostHeadingOverlay,
        stats.hostLatitude,
        stats.hostLongitude,
        stats.hostHeading
      );
    } else if (stats.hostHeading != null) {
      setHeadingOverlay(
        "host",
        stats.hostLatitude,
        stats.hostLongitude,
        stats.hostHeading,
        true
      );
    }

    // follow 중이면 방장 위치로 센터링
    if (isFollowing) {
      map.setCenter(
        new kakao.maps.LatLng(stats.hostLatitude, stats.hostLongitude)
      );
    }
  }
}

async function handleCompletedOnce(stats) {
  if (completedHandled) return;
  completedHandled = true;

  // UI/추적 중지
  try {
    stopRunning();
  } catch (e) {
    // ignore
  }

  // ✅ 자유러닝일 때도 웹소켓 disconnect (TTS 방지)
  try {
    disconnectWebSocket();
  } catch (e) {
    // ignore
  }
  stopTimerAndFreeze(stats?.totalRunningTime);

  // ✅ 오프라인: 러닝이 종료되면 즉시 결과 모달을 띄우고(스피너), 저장 완료되면 자동으로 결과 표시
  // - 코스 있음: 방장/참여자 모두 대상
  // - 코스 없음: 참여자만 즉시, 방장은 코스 저장 후 표시
  if (!isSoloRun) {
    const loadingText =
      sessionCourseId == null ? "코스 저장중입니다…" : "러닝 결과 저장중입니다…";
    if (!isHost || sessionCourseId != null) {
      startResultModalPolling(loadingText);
    }
  }

  // ✅ 자유러닝(코스 없음) 방장/솔로런 플로우:
  // 1) 코스 프리뷰 생성중 → 2) 코스 저장(필수 입력, MANUAL 고정) → 3) 결과 저장(finish(courseId))
  // 오프라인과 솔로런 모두 코스 없으면 코스 저장 후 종료
  if (isHost && sessionCourseId == null) {
    try {
      setGlobalLoading(true, "코스 생성중입니다…");
      const preview = await fetchFreeRunCoursePreview();
      setGlobalLoading(false);
      await openFreeRunCourseModal(preview);
      return; // 결과 모달은 "코스 저장 + finish" 이후에 띄운다 (시스템 메시지 수신 시)
    } catch (e) {
      setGlobalLoading(false);
      console.error("자유러닝 코스 프리뷰 실패:", e);
      showToast(
        e?.message || "코스 생성에 실패했습니다. 다시 시도해주세요.",
        "warn",
        3500
      );
      // 실패해도 결과 저장은 못 하므로 여기서 종료
      return;
    }
  }

  // 코스가 있는 경우: 방장 finish 호출(한 번만) - await로 완료 대기
  if (isHost) {
    await requestFinishOnce(null);
  }

  // ✅ 오프라인 (코스 있음/없음 모두): 방장/참여자 모두 시스템 메시지 수신 시 결과 모달 표시
  // - 코스 없을 때: 방장이 코스 생성 후 finish 호출할 때까지 기다려야 함
  // - 코스 있을 때: 방장이 finish 호출 후 결과가 저장될 때까지 기다려야 함
  if (!isSoloRun) {
    // 시스템 메시지를 기다림 (subscribeToChatMessages에서 처리)
    setGlobalLoading(false);
    // 결과 모달은 시스템 메시지 수신 시 표시됨 (subscribeToChatMessages에서 처리)
    return;
  }

  // ✅ 솔로런 코스 러닝 방장: finish 완료 후 바로 결과 모달 표시
  if (isHost && isSoloRun && sessionCourseId != null) {
    setGlobalLoading(false);
    await showRunningResultModalWithRetry("러닝 결과 저장중입니다…");
  }

  // ✅ TTS 제거: 러닝 종료 후에는 어떠한 TTS도 나오면 안 됨
  // (completedHandled로 모든 TTS가 차단됨)
}

async function requestFinishOnce(courseIdOrNull) {
  if (finishRequested) return;
  finishRequested = true;

  try {
    // finish API (러닝 결과 저장)
    const token = getAccessToken();
    if (!token) {
      throw new Error("로그인이 필요합니다.");
    }

    const res = await fetch(`/api/running/sessions/${sessionId}/finish`, {
      method: "POST",
      headers: {
        Authorization: token,
        "Content-Type": "application/json",
      },
      body:
        courseIdOrNull != null
          ? JSON.stringify({ courseId: courseIdOrNull })
          : null,
    });
    const body = await res.json().catch(() => null);
    if (!res.ok || !body?.success) {
      throw new Error(body?.message || "러닝 종료(결과 저장)에 실패했습니다.");
    }
  } catch (e) {
    console.error("finish 실패:", e);
    // 실패해도 참가자/방장 모두 결과 조회 retry가 돌기 때문에 여기서는 UX를 깨지 않는다.
  }
}

// ==========================
// Start Offline Running
// ==========================
async function startOfflineRunning() {
  if (isSoloRun || !isHost) {
    return;
  }

  if (sessionStatus === "IN_PROGRESS") {
    showToast("이미 러닝이 시작되었습니다.", "info", 3000);
    return;
  }

  try {
    // ✅ 출발점 게이트: 출발점 20m 이내 + GPS 정확도(<=30m) 충족 시에만 시작 가능
    // (코스가 있는 경우만)
    const START_GATE_RADIUS_M = 20;
    const START_GATE_MAX_ACCURACY_M = 30;

    const haversineMeters = (lat1, lng1, lat2, lng2) => {
      const R = 6371000;
      const dLat = ((lat2 - lat1) * Math.PI) / 180;
      const dLng = ((lng2 - lng1) * Math.PI) / 180;
      const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos((lat1 * Math.PI) / 180) *
          Math.cos((lat2 * Math.PI) / 180) *
          Math.sin(dLng / 2) *
          Math.sin(dLng / 2);
      const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
      return R * c;
    };

    const getCurrentPositionOnce = (options = {}) =>
      new Promise((resolve, reject) => {
        if (!navigator.geolocation) {
          reject(new Error("이 브라우저에서 위치 기능을 지원하지 않습니다."));
          return;
        }
        navigator.geolocation.getCurrentPosition(resolve, reject, options);
      });

    let startLat = null;
    let startLng = null;

    // 코스가 있으면 출발점 정보 조회
    if (sessionCourseId) {
      try {
        const token = getAccessToken();
        const headers = {
          "Content-Type": "application/json",
        };
        if (token) {
          headers["Authorization"] = token;
        }

        const spRes = await fetch(
          `/api/running/sessions/${sessionId}/course-path`,
          { method: "GET", headers }
        );
        const spBody = await spRes.json().catch(() => null);
        if (spRes.ok && spBody?.success) {
          if (
            spBody?.data?.startLat != null &&
            spBody?.data?.startLng != null
          ) {
            startLat = Number(spBody.data.startLat);
            startLng = Number(spBody.data.startLng);
          }
        } else {
          throw new Error(
            spBody?.message || "출발점 정보를 불러올 수 없습니다."
          );
        }
      } catch (e) {
        throw new Error(e?.message || "출발점 정보를 불러올 수 없습니다.");
      }
    }

    // 코스가 있는 세션이면 반드시 출발점 게이트 적용
    let pos = null;
    let payload = null;
    if (startLat != null && startLng != null) {
      pos = await getCurrentPositionOnce({
        enableHighAccuracy: true,
        timeout: 8000,
        maximumAge: 0,
      });

      const acc = pos?.coords?.accuracy;
      if (
        acc == null ||
        !Number.isFinite(acc) ||
        acc > START_GATE_MAX_ACCURACY_M
      ) {
        showToast(
          `GPS 정확도가 낮습니다(약 ${
            acc != null ? Math.round(acc) : "?"
          }m). 출발점 근처에서 잠시 대기 후 다시 시도해주세요.`,
          "warn",
          5000
        );
        return;
      }

      const distM = haversineMeters(
        pos.coords.latitude,
        pos.coords.longitude,
        startLat,
        startLng
      );
      if (distM > START_GATE_RADIUS_M) {
        showToast(
          `출발점 ${START_GATE_RADIUS_M}m 이내에서만 시작할 수 있습니다. 현재 출발점까지 약 ${Math.round(
            distM
          )}m 입니다.`,
          "warn",
          5000
        );
        return;
      }

      // ✅ 백엔드에서도 검증할 수 있도록 위치/정확도 전달
      payload = {
        latitude: pos.coords.latitude,
        longitude: pos.coords.longitude,
        accuracyM: acc,
      };
    }

    if (!confirm("런닝을 시작하시겠습니까?")) {
      return;
    }

    // ✅ 시작 상태를 서버에 반영 (IN_PROGRESS)
    const token = getAccessToken();
    if (!token) {
      throw new Error("로그인이 필요합니다.");
    }

    const response = await fetch(`/api/chat/sessions/${sessionId}/start`, {
      method: "POST",
      headers: {
        Authorization: token,
        "Content-Type": "application/json",
      },
      body: payload ? JSON.stringify(payload) : null,
    });

    const result = await response.json();

    if (!response.ok || !result.success) {
      throw new Error(result.message || "런닝 시작에 실패했습니다.");
    }

    console.log("✅ 오프라인 러닝 시작 API 호출 완료");

    // 세션 상태 업데이트
    sessionStatus = "IN_PROGRESS";

    // ✅ 시간은 서버 stats(totalRunningTime) 기준으로 보정해서 맞춘다.
    // -> 우선 즉시 타이머는 임시로 흐르게 하고, 첫 stats가 오면 seedTimerOnce에서 동기화됨
    ensureTimerRunningForInProgress();
    // TTS 시작 말하기 플래그 리셋
    localStorage.removeItem(storageKey("ttsStartSpoken"));

    // ✅ 시작 버튼을 채팅방 버튼으로 변경
    if (startRunningButton) {
      startRunningButton.style.display = "none";
    }
    if (chatButton) {
      chatButton.style.display = "flex";
      chatButton.classList.remove("solo-hidden");
    }

    // ✅ IN_PROGRESS일 때 UI 레이아웃 조정
    if (!isSoloRun) {
      adjustSoloRunLayout(true);
    }

    // ✅ 웹소켓 연결 및 러닝 시작 (페이지 새로고침 없이)
    if (typeof Stomp !== "undefined") {
      connectWebSocket();
    }

    // ✅ TTS 준비 및 "러닝을 시작합니다" 재생
    await ensureTtsOnce();
    startMotivationSchedule();
    maybeSpeakStartOnce();

    // ✅ 오프라인 러닝 시작 시 TTS 로드 완료 후 즉시 재생
    if (ttsReady && window.TtsManager && sessionStatus === "IN_PROGRESS") {
      const key = storageKey("ttsStartSpoken");
      if (localStorage.getItem(key) !== "1") {
        window.TtsManager.speak("START_RUN", { priority: 2, cooldownMs: 0 });
        localStorage.setItem(key, "1");
      }
    }

    // ✅ GPS 추적 시작 (오프라인 러닝은 직접 호출)
    if (isHost && sessionStatus === "IN_PROGRESS") {
      // 미리보기 추적 중지
      stopPreviewOnlyTracking();

      // 웹소켓 연결 대기 후 GPS 추적 시작
      setTimeout(() => {
        if (stompClient && stompClient.connected) {
          startRunning();
        } else {
          // 웹소켓 연결 대기
          const checkConnection = setInterval(() => {
            if (stompClient && stompClient.connected) {
              clearInterval(checkConnection);
              startRunning();
            }
          }, 100);

          // 5초 후 타임아웃
          setTimeout(() => {
            clearInterval(checkConnection);
          }, 5000);
        }
      }, 500);
    }

    showToast("러닝이 시작되었습니다!", "success", 3000);
  } catch (e) {
    console.error("오프라인 러닝 시작 실패:", e);
    showToast(e?.message || "러닝 시작에 실패했습니다.", "warn", 5000);
  }
}

// ==========================
// Start Solo Running
// ==========================
async function startSoloRunning() {
  if (!isSoloRun) {
    return;
  }

  if (sessionStatus === "IN_PROGRESS") {
    showToast("이미 러닝이 시작되었습니다.", "info", 3000);
    return;
  }

  try {
    // ✅ 출발점 게이트: 출발점 20m 이내 + GPS 정확도(<=30m) 충족 시에만 시작 가능
    const START_GATE_RADIUS_M = 20;
    const START_GATE_MAX_ACCURACY_M = 30;

    const haversineMeters = (lat1, lng1, lat2, lng2) => {
      const R = 6371000;
      const dLat = ((lat2 - lat1) * Math.PI) / 180;
      const dLng = ((lng2 - lng1) * Math.PI) / 180;
      const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos((lat1 * Math.PI) / 180) *
          Math.cos((lat2 * Math.PI) / 180) *
          Math.sin(dLng / 2) *
          Math.sin(dLng / 2);
      const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
      return R * c;
    };

    const getCurrentPositionOnce = (options = {}) =>
      new Promise((resolve, reject) => {
        if (!navigator.geolocation) {
          reject(new Error("이 브라우저에서 위치 기능을 지원하지 않습니다."));
          return;
        }
        navigator.geolocation.getCurrentPosition(resolve, reject, options);
      });

    let startLat = null;
    let startLng = null;

    // 코스가 있으면 출발점 정보 조회
    if (sessionCourseId) {
      try {
        const token = getAccessToken();
        const headers = {
          "Content-Type": "application/json",
        };
        if (token) {
          headers["Authorization"] = token;
        }

        const spRes = await fetch(
          `/api/running/sessions/${sessionId}/course-path`,
          { method: "GET", headers }
        );
        const spBody = await spRes.json().catch(() => null);
        if (spRes.ok && spBody?.success) {
          if (
            spBody?.data?.startLat != null &&
            spBody?.data?.startLng != null
          ) {
            startLat = Number(spBody.data.startLat);
            startLng = Number(spBody.data.startLng);
          }
        } else {
          // 코스가 있지만 출발점 정보를 못 가져온 경우는 에러
          throw new Error(
            spBody?.message || "출발점 정보를 불러올 수 없습니다."
          );
        }
      } catch (e) {
        throw new Error(e?.message || "출발점 정보를 불러올 수 없습니다.");
      }
    }
    // 코스가 없는 자유러닝은 출발점 게이트 적용하지 않음
    // ✅ 코스가 없을 때는 현재 위치를 가져와서 주소 변환에 사용
    if (!sessionCourseId) {
      try {
        const pos = await getCurrentPositionOnce({
          enableHighAccuracy: true,
          timeout: 8000,
          maximumAge: 0,
        });
        if (pos?.coords) {
          latestPosition = pos;
          // ✅ 시작 위치 주소 변환
          convertStartAddressAsync(pos.coords.latitude, pos.coords.longitude);
        }
      } catch (e) {
        console.warn("자유러닝 시작 위치 가져오기 실패:", e);
      }
    }

    // 코스가 있는 세션이면 반드시 출발점 게이트 적용
    let pos = null;
    let payload = null;
    if (startLat != null && startLng != null) {
      pos = await getCurrentPositionOnce({
        enableHighAccuracy: true,
        timeout: 8000,
        maximumAge: 0,
      });

      const acc = pos?.coords?.accuracy;
      if (
        acc == null ||
        !Number.isFinite(acc) ||
        acc > START_GATE_MAX_ACCURACY_M
      ) {
        alert(
          `GPS 정확도가 낮습니다(약 ${
            acc != null ? Math.round(acc) : "?"
          }m).\n출발점 근처에서 잠시 대기 후 다시 시도해주세요.`
        );
        return;
      }

      const distM = haversineMeters(
        pos.coords.latitude,
        pos.coords.longitude,
        startLat,
        startLng
      );
      if (distM > START_GATE_RADIUS_M) {
        alert(
          `출발점 ${START_GATE_RADIUS_M}m 이내에서만 시작할 수 있습니다.\n현재 출발점까지 약 ${Math.round(
            distM
          )}m 입니다.`
        );
        return;
      }

      // ✅ 백엔드에서도 검증할 수 있도록 위치/정확도 전달
      payload = {
        latitude: pos.coords.latitude,
        longitude: pos.coords.longitude,
        accuracyM: acc,
      };
    }

    if (!confirm("런닝을 시작하시겠습니까?")) {
      return;
    }

    // ✅ 시작 상태를 서버에 반영 (IN_PROGRESS)
    const token = getAccessToken();
    if (!token) {
      throw new Error("로그인이 필요합니다.");
    }

    // 솔로런은 채팅방이 없으므로 /api/chat/sessions 대신 다른 API 사용
    // 일단 /api/chat/sessions를 사용하되, 백엔드에서 솔로런도 지원하는지 확인 필요
    const response = await fetch(`/api/chat/sessions/${sessionId}/start`, {
      method: "POST",
      headers: {
        Authorization: token,
        "Content-Type": "application/json",
      },
      body: payload ? JSON.stringify(payload) : null,
    });

    const result = await response.json();

    if (!response.ok || !result.success) {
      throw new Error(result.message || "런닝 시작에 실패했습니다.");
    }

    console.log("✅ 솔로런 시작 API 호출 완료");

    // 세션 상태 업데이트
    sessionStatus = "IN_PROGRESS";

    // ✅ 시간은 서버 stats(totalRunningTime) 기준으로 보정해서 맞춘다.
    // -> 우선 즉시 타이머는 임시로 흐르게 하고, 첫 stats가 오면 seedTimerOnce에서 동기화됨
    ensureTimerRunningForInProgress();
    // TTS 시작 말하기 플래그 리셋
    localStorage.removeItem(storageKey("ttsStartSpoken"));

    // ✅ 시작 버튼 즉시 숨김
    if (startRunningButton) {
      startRunningButton.style.display = "none";
    }

    // ✅ IN_PROGRESS일 때 UI 레이아웃 조정 (통계 오버레이 아래로, GPS 버튼 위로)
    adjustSoloRunLayout(true);

    // ✅ 웹소켓 연결 및 러닝 시작 (페이지 새로고침 없이)
    if (typeof Stomp !== "undefined") {
      connectWebSocket();
    }

    // ✅ TTS 준비 및 "러닝을 시작합니다" 재생 (오프라인과 동일하게)
    await ensureTtsOnce();
    startMotivationSchedule();

    // ✅ 솔로런 시작 시 TTS 로드 완료 후 즉시 재생 (오프라인과 동일)
    if (ttsReady && window.TtsManager && sessionStatus === "IN_PROGRESS") {
      const key = storageKey("ttsStartSpoken");
      if (localStorage.getItem(key) !== "1") {
        window.TtsManager.speak("START_RUN", { priority: 2, cooldownMs: 0 });
        localStorage.setItem(key, "1");
      }
    }

    // ✅ GPS 추적 시작 (솔로런은 직접 호출)
    if (isHost && sessionStatus === "IN_PROGRESS") {
      // 미리보기 추적 중지
      stopPreviewOnlyTracking();

      // 웹소켓 연결 대기 후 GPS 추적 시작
      setTimeout(() => {
        if (stompClient && stompClient.connected) {
          startRunning();
        } else {
          // 웹소켓 연결 대기
          const checkConnection = setInterval(() => {
            if (stompClient && stompClient.connected) {
              clearInterval(checkConnection);
              startRunning();
            }
          }, 100);

          // 5초 후 타임아웃
          setTimeout(() => {
            clearInterval(checkConnection);
          }, 5000);
        }
      }, 500);
    }
  } catch (error) {
    console.error("솔로런 시작 에러:", error);
    alert(error?.message || "런닝 시작에 실패했습니다.");
  }
}

// ==========================
// Start Running
// ==========================
function startRunning() {
  // ✅ 솔로런/오프라인 러닝 모두 IN_PROGRESS 상태면 GPS 추적 시작 가능
  if ((isSoloRun || !isSoloRun) && sessionStatus !== "IN_PROGRESS") {
    return;
  }
  // ✅ 러닝 시작은 러닝 페이지에서: IN_PROGRESS가 아니면 서버 전송/추적 시작 금지
  if (sessionStatus !== "IN_PROGRESS") {
    if (isSoloRun) {
      showToast("러닝을 시작해주세요.", "warn", 3500);
    } else {
      showToast("러닝을 시작해주세요.", "warn", 3500);
    }
    return;
  }
  // ✅ 방장만 GPS 전송: 참가자는 GPS 추적을 시작하지 않음
  if (!isHost) {
    return;
  }
  if (isRunning) {
    return;
  }

  // ✅ 미리보기(STANDBY)에서 돌던 watchPosition이 남아있으면,
  // 튄 좌표/정확도 상태가 러닝 시작 직후 흐름에 영향을 줄 수 있으니 반드시 끊고 초기화한다.
  if (previewWatchId != null) {
    stopPreviewOnlyTracking();
    latestPosition = null;
    lastGpsAccuracyM = null;
  }
  // ✅ 러닝 추적 시작 시점에는 "진행도 계산용 기준점"을 항상 리셋한다.
  // - 기준점이 남아있으면 movedM이 과대 계산되어 큰 along 점프가 통과할 수 있다.
  lastPositionTimestampMs = 0;
  lastPositionLatForProgress = null;
  lastPositionLngForProgress = null;

  // ✅ 재연결 시 GPS가 튀는 문제 해결: 재진입이 아닌 경우에만 진행도 초기화
  // 재진입(이미 IN_PROGRESS)인 경우는 loadCoursePath에서 받은 값 사용
  if (sessionStatus !== "IN_PROGRESS" || lastHostAlongM === 0) {
    lastMatchedDistM = 0;
    lastMatchedSegIdx = 0;
    // lastHostAlongM은 loadCoursePath에서 설정되므로 여기서는 초기화하지 않음
  }

  if (!stompClient || !stompClient.connected) {
    console.error("WebSocket이 연결되지 않았습니다.");
    return;
  }

  // ✅ 재진입/새로고침(이미 IN_PROGRESS)에서는 진행도(코스 위 매칭 거리)를 0으로 리셋하면 안 된다.
  // - 서버/Redis latest hostMatchedDistM(또는 로컬 백업)을 시드로 잡아야 '앞에서 지운 코스'가 다시 지워지지 않는다.
  let seedAlongM = 0;
  try {
    const a = Number(lastHostAlongM);
    if (Number.isFinite(a) && a > 0) seedAlongM = a;
  } catch (e) {
    // ignore
  }
  if (seedAlongM <= 0) {
    try {
      const stored = getStoredHostAlongM();
      if (Number.isFinite(stored) && stored > 0) seedAlongM = stored;
    } catch (e) {
      // ignore
    }
  }
  lastMatchedDistM = Math.max(0, seedAlongM);
  lastMatchedSegIdx = seedAlongM > 0 ? findCourseSegIdxByAlongM(seedAlongM) : 0;

  isRunning = true;

  if (typeof RunningTracker === "undefined") {
    console.error("RunningTracker 클래스가 로드되지 않았습니다.");
    alert("GPS 추적을 시작할 수 없습니다.");
    return;
  }

  console.log("🎯 GPS 추적 시작 (실제 GPS)");
  runningTracker = new RunningTracker(sessionId, userId, stompClient, false);
  // 재진입 시 이어달리기: 최신 stats로 tracker 시드
  if (latestStatsCache && typeof runningTracker.bootstrap === "function") {
    try {
      if (
        latestStatsCache.totalDistance != null &&
        latestStatsCache.totalRunningTime != null
      ) {
        runningTracker.bootstrap(
          latestStatsCache.totalDistance,
          latestStatsCache.totalRunningTime
        );
      }
    } catch (e) {
      console.warn("RunningTracker bootstrap 실패(무시):", e?.message || e);
    }
  }

  const originalOnGPSUpdate = runningTracker.onGPSUpdate.bind(runningTracker);
  runningTracker.onGPSUpdate = (position) => {
    // ✅ 첫 전송부터 matchedDistanceM이 null이면 백엔드가 자체 매칭(resolveHostMatchedDistM)으로
    // 임의의 큰 alongM을 잡아버릴 수 있다(루프/교차/근접 구간).
    // 호스트는 항상 "현재 값(초기 0)"을 먼저 실어 보내서 서버 매칭을 끄고, 이후 로컬 매칭으로 갱신한다.
    if (isHost && coursePath && courseCumDistM && runningTracker) {
      runningTracker.includeMatchedDistanceM = true;
      if (runningTracker.matchedDistanceM == null) {
        runningTracker.matchedDistanceM = lastMatchedDistM; // 초기 0
      }
    }

    originalOnGPSUpdate(position);

    if (position && position.coords) {
      lastGpsAccuracyM = position.coords.accuracy;
      updateGpsAccuracyBadge(lastGpsAccuracyM);
      latestPosition = position;

      // ✅ 첫 GPS 위치에서 주소 변환 (아직 변환되지 않았을 때만)
      if (!resolvedStartAddress) {
        convertStartAddressAsync(
          position.coords.latitude,
          position.coords.longitude
        );
      }

      // ✅ 내 방향 표시 (heading 우선, 없으면 트래커가 계산한 heading을 payload에 포함)
      const heading =
        position.coords.heading != null
          ? position.coords.heading
          : runningTracker?.heading;

      // ✅ updateUserPosition에 heading 전달
      updateUserPosition(
        position.coords.latitude,
        position.coords.longitude,
        heading
      );
      checkOffRouteByMatch(position.coords.latitude, position.coords.longitude);

      // ✅ 방장: 로컬 매칭으로 "코스 위 진행도"만 계산해서 서버로 전송(payload에만 반영)
      // (로컬에서 trimCourseByMatchedProgress 호출은 하지 않음)
      if (isHost && coursePath && courseCumDistM && runningTracker) {
        const matched = matchProgressOnCourse(
          position.coords.latitude,
          position.coords.longitude
        );

        if (matched && matched.matched && matched.alongM >= lastMatchedDistM) {
          lastMatchedDistM = matched.alongM;
          lastMatchedSegIdx = matched.segIdx;

          // 서버로 보내는 GPS payload에만 반영 → stats.hostMatchedDistM로 브로드캐스트됨
          runningTracker.matchedDistanceM = lastMatchedDistM;
        }
      }
      if (heading != null) {
        if (!userHeadingOverlay) {
          setHeadingOverlay(
            "user",
            position.coords.latitude,
            position.coords.longitude,
            heading,
            false
          );
        } else {
          updateHeadingOverlay(
            userHeadingOverlay,
            position.coords.latitude,
            position.coords.longitude,
            heading
          );
        }
      }

      // NOTE: 방장은 코스 트리밍을 로컬 GPS 매칭으로 즉시 처리하지 않는다.
      // 서버(Redis latest hostMatchedDistM) 기준으로 "통계(stats)"를 통해서만 동일하게 트리밍한다.
    }
  };

  // ✅ 러닝 시작 시 첫 GPS 위치에서 주소 변환
  if (latestPosition?.coords) {
    convertStartAddressAsync(
      latestPosition.coords.latitude,
      latestPosition.coords.longitude
    );
  }

  runningTracker.startTracking();
}

// ==========================
// Stop Running
// ==========================
function stopRunning() {
  if (!isRunning) {
    return;
  }

  isRunning = false;

  if (runningTracker) {
    runningTracker.stopTracking();
    runningTracker = null;
    console.log("🛑 GPS 추적 완전히 중지됨");
  }

  if (gpsSubscription) {
    gpsSubscription.unsubscribe();
    gpsSubscription = null;
    console.log("🛑 GPS 구독 해제됨");
  }
}

// ==========================
// Update User Position
// ==========================
function updateUserPosition(lat, lng, heading = null) {
  if (!map) {
    return;
  }

  const position = new kakao.maps.LatLng(lat, lng);

  // 러닝앱 스타일 핀(CustomOverlay)
  if (!userPinOverlay) {
    setRunnerPinOverlay("user", lat, lng);
  } else {
    updateRunnerPinOverlay("user", lat, lng);
  }

  // ✅ GPS가 움직일 때 heading도 업데이트
  if (heading != null && Number.isFinite(heading)) {
    if (!userHeadingOverlay) {
      setHeadingOverlay("user", lat, lng, heading, false);
    } else {
      updateHeadingOverlay(userHeadingOverlay, lat, lng, heading);
    }
  }

  // 기본 마커는 숨김(중복 방지)
  if (userMarker) userMarker.setMap(null);

  // ✅ 자동 센터링 제거: 사용자가 지도를 움직일 수 있게
  if (isFollowing) {
    map.setCenter(position);
  }
}
