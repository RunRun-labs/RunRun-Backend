// 채팅방 페이지 스크립트 - JWT 인증 적용

let stompClient = null;
let currentUser = null;
let currentSession = null;
let currentUserJoinedAt = null;
let isHost = false;
let hostId = null;
let latestRunningStats = null;
let runningStatsModalShown = false;
let chatSubscription = null;
let seededRunningStatsOnce = false; // fetch seed 1회
let seededRunningStatsFromStorageOnce = false; // localStorage seed 1회
let lastPaceText = "--:--";
let runningTracker = null; // ✅ RunningTracker 인스턴스(방장 GPS publish/매칭 진행도 전송용)

// ✅ GPS/WS stats가 끊겨도 "시간"은 계속 흐르게(로컬 타이머)
let runningStatsTimerBaseSec = 0;
let runningStatsTimerStartMs = null;
let runningStatsTimerIntervalId = null;
let runningStatsLivePollIntervalId = null;
let completedHandled = false; // ✅ 러닝 종료 후 TTS 비활성화용

// ✅ 러닝페이지(running.js)와 시간 공유용 localStorage 키 규칙
function runningStorageKey(key) {
  return `running:${currentSession?.id}:${key}`;
}

function getStartedAtMsFromStorage() {
  try {
    const v = Number(localStorage.getItem(runningStorageKey("startedAtMs")));
    return Number.isFinite(v) && v > 0 ? v : null;
  } catch (e) {
    return null;
  }
}

function getLastStatsAtMsFromStorage() {
  try {
    const v = Number(localStorage.getItem(runningStorageKey("lastStatsAtMs")));
    return Number.isFinite(v) && v > 0 ? v : 0;
  } catch (e) {
    return 0;
  }
}

function getLastTotalDistanceFromStorage() {
  try {
    const v = Number(
      localStorage.getItem(runningStorageKey("lastTotalDistance"))
    );
    return Number.isFinite(v) && v >= 0 ? v : null;
  } catch (e) {
    return null;
  }
}

function getLastTotalRunningTimeFromStorage() {
  try {
    const v = Number(
      localStorage.getItem(runningStorageKey("lastTotalRunningTime"))
    );
    return Number.isFinite(v) && v >= 0 ? v : null;
  } catch (e) {
    return null;
  }
}

function getLastHostMatchedDistMFromStorage() {
  try {
    const v = Number(
      localStorage.getItem(runningStorageKey("lastHostMatchedDistM"))
    );
    return Number.isFinite(v) && v >= 0 ? v : null;
  } catch (e) {
    return null;
  }
}

function ensureStartedAtMsInStorage(startedAtMs) {
  try {
    const existing = getStartedAtMsFromStorage();
    if (existing) {
      return existing;
    }
    const v = Number(startedAtMs);
    const ms = Number.isFinite(v) && v > 0 ? v : Date.now();
    localStorage.setItem(runningStorageKey("startedAtMs"), String(ms));
    return ms;
  } catch (e) {
    return null;
  }
}

// ============================================
// 인증 관련 함수
// ============================================

// Authorization 헤더 가져오기
function getAuthHeaders() {
  const token = localStorage.getItem("accessToken");
  return {
    "Content-Type": "application/json",
    Authorization: token ? "Bearer " + token : "",
  };
}

// 인증 포함 fetch 함수
async function fetchWithAuth(url, options = {}) {
  const headers = {
    ...getAuthHeaders(),
    ...options.headers,
  };

  const response = await fetch(url, {
    ...options,
    headers,
  });

  // 401 Unauthorized면 로그인 페이지로 이동
  if (response.status === 401) {
    alert("로그인이 필요합니다.");
    window.location.href = "/login";
    throw new Error("Unauthorized");
  }

  return response;
}

function safeJsonParse(raw, fallback = null) {
  try {
    return JSON.parse(raw);
  } catch (e) {
    return fallback;
  }
}

function safeStompSend(destination, bodyObj) {
  try {
    if (!stompClient || !stompClient.connected) {
      console.warn("[STOMP] send skipped (not connected):", destination);
      return false;
    }
    stompClient.send(destination, {}, JSON.stringify(bodyObj));
    return true;
  } catch (e) {
    console.error("[STOMP] send failed:", destination, e);
    return false;
  }
}

// ============================================
// TTS (채팅방) - 러닝 중에도 음성 안내가 필요할 수 있어 최소 훅 제공
// ============================================
let chatTtsReady = false;

async function ensureChatTtsOnce() {
  if (chatTtsReady) {
    return true;
  }
  if (!window.TtsManager) {
    return false;
  }
  try {
    await window.TtsManager.ensureLoaded({
      sessionId: currentSession?.id,
      mode: currentSession?.type || "OFFLINE",
    });
    chatTtsReady = true;
    return true;
  } catch (e) {
    console.warn("채팅방 TTS 로드 실패(무시):", e?.message || e);
    return false;
  }
}

// 현재 로그인한 사용자 정보 조회
async function getCurrentUser() {
  try {
    const response = await fetchWithAuth("/api/chat/me");
    const result = await response.json();

    if (result.success) {
      return {
        id: result.data.userId,
        loginId: result.data.loginId,
        name: result.data.name,
      };
    } else {
      throw new Error(result.message || "사용자 정보 조회 실패");
    }
  } catch (error) {
    console.error("사용자 정보 조회 실패:", error);
    alert("로그인이 필요합니다.");
    window.location.href = "/login";
    return null;
  }
}

// URL 파라미터에서 세션 ID 가져오기
function getUrlParams() {
  const params = new URLSearchParams(window.location.search);
  return {
    sessionId: params.get("sessionId"),
  };
}

// ============================================
// 페이지 초기화
// ============================================

// 페이지 로드 시 초기화
document.addEventListener("DOMContentLoaded", async function () {
  const params = getUrlParams();

  if (!params.sessionId) {
    alert("세션 ID가 필요합니다.\nURL 형식: /chat/chat1?sessionId=1");
    return;
  }

  // 1. 로그인한 사용자 정보 조회
  const user = await getCurrentUser();
  if (!user) {
    return;
  }

  currentUser = user;
  console.log("현재 사용자:", currentUser);

  // 2. 세션 정보 조회
  await loadSessionInfo(params.sessionId);

  // 3. 화면 업데이트
  updateChatRoomUI();

  // 4. WebSocket 연결
  connectWebSocket();

  // 5. 이벤트 리스너 설정
  setupEventListeners();

  // 6. 페이지 나갈 때 마지막 읽은 시간 업데이트
  window.addEventListener("beforeunload", function () {
    updateLastReadTime(params.sessionId);
  });

  // 러닝 페이지 갔다가 뒤로가기 등으로 돌아올 때(bfcache 포함) 상태 재조회해서 버튼 갱신
  async function refreshSessionStatusOnly() {
    try {
      await loadSessionInfo(params.sessionId);
      updateControlBar();
    } catch (e) {
      // ignore
    }
  }

  window.addEventListener("pageshow", function (event) {
    if (event.persisted) {
      refreshSessionStatusOnly();
    }
  });
  window.addEventListener("focus", function () {
    refreshSessionStatusOnly();
  });
});

// ============================================
// 세션 정보 조회
// ============================================

// 세션 정보 조회
async function loadSessionInfo(sessionId) {
  try {
    const response = await fetchWithAuth(`/api/chat/sessions/${sessionId}`);
    const result = await response.json();

    if (result.success) {
      currentSession = {
        id: parseInt(sessionId),
        type: result.data.type,
        distance: result.data.targetDistance,
        status: result.data.status,
        meetingTime: result.data.meetingTime,
        meetingPlace: result.data.meetingPlace || "장소 미정",
        title: result.data.title || "제목 없음",
        courseId: result.data.courseId || null,
      };

      hostId = result.data.hostId;
      isHost = hostId == currentUser.id;

      // 입장 시점 조회
      try {
        const joinedResponse = await fetchWithAuth(
          `/api/chat/sessions/${sessionId}/joined-at`
        );
        const joinedResult = await joinedResponse.json();
        if (joinedResult.success) {
          currentUserJoinedAt = joinedResult.data;
        }
      } catch (e) {
        console.warn("입장 시점 조회 실패:", e);
      }
    }
  } catch (error) {
    console.error("세션 정보 조회 실패:", error);
    alert("세션 정보를 불러올 수 없습니다.");
  }
}

// ============================================
// 채팅방 UI 업데이트
// ============================================

// 채팅방 UI 업데이트
function updateChatRoomUI() {
  if (!currentSession) {
    return;
  }

  // 그룹명 (제목 표시, 없으면 세션 ID)
  const title = currentSession.title || `세션 #${currentSession.id}`;
  document.getElementById("group-name").textContent = title;

  // 세션 타입
  document.getElementById(
    "session-type-badge"
  ).textContent = `🏃 ${currentSession.type}`;

  // 거리
  document.getElementById(
    "session-distance"
  ).textContent = `${currentSession.distance}km`;

  // 만남 시간
  const meetingTimeEl = document.getElementById("meeting-time");
  if (currentSession.meetingTime) {
    try {
      const date = new Date(currentSession.meetingTime);
      if (!isNaN(date.getTime())) {
        const formatted = formatDateTime(date);
        meetingTimeEl.textContent = formatted;
      } else {
        meetingTimeEl.textContent = "시간 미정";
      }
    } catch (error) {
      console.error("날짜 파싱 오류:", error);
      meetingTimeEl.textContent = "시간 미정";
    }
  } else {
    meetingTimeEl.textContent = "시간 미정";
  }

  // 만남 장소
  document.getElementById("meeting-place").textContent =
    currentSession.meetingPlace || "장소 미정";

  // 참여자 수 업데이트
  loadParticipants(currentSession.id);

  // 컨트롤 바 업데이트
  updateControlBar();
}

// 날짜/시간 포맷팅
function formatDateTime(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const weekdays = ["일", "월", "화", "수", "목", "금", "토"];
  const weekday = weekdays[date.getDay()];
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");

  return `${year}.${month}.${day} (${weekday}) ${hours}:${minutes}`;
}

// 마지막 읽은 시간 업데이트
function updateLastReadTime(sessionId) {
  const token = localStorage.getItem("accessToken");

  fetch(`/api/chat/sessions/${sessionId}/read`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    keepalive: true, // 페이지를 나가더라도 요청 유지
  })
    .then(() => {
      console.log("마지막 읽음 시간 업데이트 완료");
    })
    .catch((error) => {
      console.error("읽음 시간 업데이트 실패:", error);
    });
}

// ============================================
// 참여자 관련
// ============================================

let participantsList = [];

// 현재 사용자의 준비 상태 불러오기
function loadUserReadyStatus() {
  fetchWithAuth(`/api/chat/sessions/${currentSession.id}/users`)
    .then((response) => response.json())
    .then((result) => {
      if (result.success) {
        const currentUserData = result.data.find(
          (u) => u.userId == currentUser.id
        );
        if (currentUserData) {
          const isReady = currentUserData.isReady;
          const readyButton = document.getElementById("ready-button");
          if (readyButton) {
            if (isReady) {
              readyButton.classList.add("ready-active");
              readyButton.textContent = "준비완료 취소";
            } else {
              readyButton.classList.remove("ready-active");
              readyButton.textContent = "✓ 준비완료";
            }
          }
        }
      }
    })
    .catch((error) => console.error("준비 상태 조회 실패:", error));
}

// 참여자 목록 조회
function loadParticipants(sessionId) {
  fetchWithAuth(`/api/chat/sessions/${sessionId}/users`)
    .then((response) => response.json())
    .then((result) => {
      if (result.success) {
        participantsList = result.data;
        const count = result.data.length;
        document.getElementById(
          "participant-count"
        ).textContent = `${count}명 참여중`;

        // 준비 완료 수 업데이트
        const readyCount = result.data.filter((p) => p.isReady).length;
        document.getElementById(
          "ready-subtitle"
        ).textContent = `${readyCount}/${count}명 준비 완료`;

        // 모달이 열려있으면 목록 업데이트
        const modal = document.getElementById("participant-modal-overlay");
        if (modal && modal.classList.contains("show")) {
          renderParticipantList();
        }
      }
    })
    .catch((error) => console.error("참여자 목록 조회 실패:", error));
}

// ============================================
// 컨트롤 바
// ============================================

// 컨트롤 바 업데이트
function updateControlBar() {
  const hostSection = document.getElementById("host-control-section");
  const userSection = document.getElementById("user-control-section");
  const readySection = document.getElementById("ready-section");
  const goRunningBtn = document.getElementById("go-running-page-btn");
  const startBtn = document.getElementById("start-running-btn");
  const hostLabel = document.getElementById("host-control-label");

  if (currentSession.status === "COMPLETED") {
    // 종료됨: "러닝 결과 보기" 버튼 노출
    if (hostSection) {
      hostSection.classList.remove("hidden");
    }
    if (userSection) {
      userSection.classList.add("hidden");
    }
    if (readySection) {
      readySection.classList.add("hidden");
    }

    if (startBtn) {
      startBtn.disabled = false;
      startBtn.style.opacity = "1";
      startBtn.textContent = "🏁 러닝 결과 보기";
    }

    if (hostLabel) {
      hostLabel.textContent = isHost
        ? "👑 방장입니다"
        : "🏁 러닝이 종료되었습니다";
    }

    if (goRunningBtn) {
      goRunningBtn.classList.remove("hidden");
    }
  } else if (currentSession.status === "IN_PROGRESS") {
    // 진행 중일 때: "러닝 페이지로 가기" 버튼을 확실히 노출
    if (hostSection) {
      hostSection.classList.remove("hidden");
    }
    if (userSection) {
      userSection.classList.add("hidden");
    }
    if (readySection) {
      readySection.classList.add("hidden");
    }

    if (startBtn) {
      startBtn.disabled = false;
      startBtn.style.opacity = "1";
      startBtn.textContent = "🏃 러닝 페이지로 가기";
    }

    // 방장 문구가 비방장에게 보이는 문제 방지
    if (hostLabel) {
      hostLabel.textContent = isHost
        ? "👑 방장입니다"
        : "🏃 러닝이 진행중입니다";
    }

    // 상단 아이콘 버튼도 함께 노출
    if (goRunningBtn) {
      goRunningBtn.classList.remove("hidden");
    }
  } else {
    // ✅ STANDBY(시작 전)에도 러닝페이지 "미리보기"를 위해 항상 노출
    if (goRunningBtn) {
      goRunningBtn.classList.remove("hidden");
    }
    // 대기 중일 때
    if (isHost) {
      if (hostSection) {
        hostSection.classList.remove("hidden");
      }
      if (userSection) {
        userSection.classList.add("hidden");
      }
      if (readySection) {
        readySection.classList.remove("hidden");
      }
      checkAllReadyAndUpdateButton();

      if (hostLabel) {
        hostLabel.textContent = "👑 방장입니다";
      }
    } else {
      if (hostSection) {
        hostSection.classList.add("hidden");
      }
      if (userSection) {
        userSection.classList.add("hidden");
      }
      if (readySection) {
        readySection.classList.remove("hidden");
      }
    }
  }
}

function openRunningStatsModal() {
  const overlay = document.getElementById("running-stats-modal-overlay");
  if (!overlay) {
    return;
  }
  overlay.classList.add("show");
  runningStatsModalShown = true;
  if (latestRunningStats) {
    updateRunningStatsModal(latestRunningStats);
  }
  // ✅ 모달 오픈 시: (1) WS 구독 시도, (2) fetch 폴링(참여자 환경에서 WS 미수신 보완)
  if (currentSession?.status === "IN_PROGRESS" && stompClient?.connected) {
    subscribeToRunningStats();
  }
  ensureRunningStatsLive();
}

function closeRunningStatsModal() {
  const overlay = document.getElementById("running-stats-modal-overlay");
  if (!overlay) {
    return;
  }
  overlay.classList.remove("show");
  if (runningStatsLivePollIntervalId) {
    clearInterval(runningStatsLivePollIntervalId);
    runningStatsLivePollIntervalId = null;
  }
}

function toggleRunningStatsModal() {
  const overlay = document.getElementById("running-stats-modal-overlay");
  if (!overlay) {
    return;
  }

  const isOpen = overlay.classList.contains("show");
  if (isOpen) {
    closeRunningStatsModal();
    return;
  }

  // 러닝 중이면 통계 구독이 없을 때 자동으로 붙인다
  if (
    currentSession?.status === "IN_PROGRESS" &&
    (!gpsSubscription || !stompClient?.connected)
  ) {
    if (stompClient?.connected) {
      subscribeToRunningStats();
    }
  }

  // ✅ 모달 열 때: 우선 localStorage 스냅샷으로 즉시 표시
  seedLatestRunningStatsFromStorageOnce();

  // ✅ 그 다음 최신값은 fetch로 동기화(WS 미수신 케이스 보완)
  seedLatestRunningStatsOnce();
  ensureRunningStatsLive();

  // ✅ seed/WS가 실패해도 러닝 중이면 시간은 0초부터라도 흐르게
  if (
    currentSession?.status === "IN_PROGRESS" &&
    !runningStatsTimerIntervalId
  ) {
    const startedAtMs = getStartedAtMsFromStorage();
    if (startedAtMs) {
      const elapsedSec = Math.max(
        0,
        Math.floor((Date.now() - startedAtMs) / 1000)
      );
      startRunningStatsLocalTimer(elapsedSec);
    } else {
      startRunningStatsLocalTimer(0);
    }
  }

  openRunningStatsModal();
}

async function fetchLatestRunningStats() {
  if (!currentSession?.id) {
    return null;
  }
  try {
    const res = await fetchWithAuth(
      `/api/running/sessions/${currentSession.id}/stats`,
      { method: "GET" }
    );
    if (!res.ok) {
      return null;
    }
    const body = await res.json().catch(() => null);
    // { success, data }
    return body?.data ?? null;
  } catch (e) {
    return null;
  }
}

function ensureRunningStatsLive() {
  if (runningStatsLivePollIntervalId) {
    return;
  }
  if (!currentSession?.id) {
    return;
  }

  runningStatsLivePollIntervalId = setInterval(async () => {
    const overlay = document.getElementById("running-stats-modal-overlay");
    if (!overlay || !overlay.classList.contains("show")) {
      clearInterval(runningStatsLivePollIntervalId);
      runningStatsLivePollIntervalId = null;
      return;
    }
    if (currentSession?.status !== "IN_PROGRESS") {
      return;
    }

    const stats = await fetchLatestRunningStats();
    if (stats) {
      updateRunningUI(stats);
    }
  }, 1200);
}

async function seedLatestRunningStatsOnce() {
  if (seededRunningStatsOnce) {
    return;
  }
  const stats = await fetchLatestRunningStats();
  if (!stats) {
    return;
  }
  updateRunningUI(stats);
  seededRunningStatsOnce = true;
}

function seedLatestRunningStatsFromStorageOnce() {
  if (seededRunningStatsFromStorageOnce) {
    return;
  }
  try {
    const d = Number(getLastTotalDistanceFromStorage());
    const t = Number(getLastTotalRunningTimeFromStorage());
    const along = Number(getLastHostMatchedDistMFromStorage());

    if (!Number.isFinite(d) && !Number.isFinite(t) && !Number.isFinite(along)) {
      return;
    }

    const snapshot = {
      totalDistance: Number.isFinite(d) ? d : 0,
      totalRunningTime: Number.isFinite(t) ? Math.max(0, t) : 0,
      hostMatchedDistM: Number.isFinite(along) ? Math.max(0, along) : 0,
      // 모달 표시용으로만 사용하므로 pace/remaining은 미표시(--/0) 처리
      remainingDistance: 0,
      teamAveragePace: null,
      segmentPaces: null,
    };
    updateRunningUI(snapshot);
    seededRunningStatsFromStorageOnce = true;
  } catch (e) {
    // ignore
  }
}

function formatTimeHMS(totalSec) {
  if (totalSec == null) {
    return "00:00:00";
  }
  const sec = Math.max(0, Math.floor(totalSec));
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}:${String(
    s
  ).padStart(2, "0")}`;
}

function formatPaceMMSS(pace) {
  if (pace == null || Number.isNaN(pace)) {
    return "--:--";
  }
  const min = Math.floor(pace);
  const sec = Math.round((pace - min) * 60);
  return `${min}:${String(sec).padStart(2, "0")}`;
}

function startRunningStatsLocalTimer(seedSec) {
  if (runningStatsTimerIntervalId) {
    return;
  }
  runningStatsTimerBaseSec = Math.max(0, Math.floor(Number(seedSec) || 0));
  runningStatsTimerStartMs = Date.now();

  runningStatsTimerIntervalId = setInterval(() => {
    // 모달이 열려있을 때만 렌더링
    const overlay = document.getElementById("running-stats-modal-overlay");
    if (!overlay || !overlay.classList.contains("show")) {
      return;
    }
    if (!latestRunningStats) {
      return;
    }
    updateRunningStatsModal(latestRunningStats);
  }, 1000);
}

function getRunningStatsLocalTimeSec(stats) {
  // stats가 주는 시간이 있으면 그걸 seed로 쓰되, 이후엔 로컬로 흐르게
  const serverSec = Number(stats?.totalRunningTime);
  if (!runningStatsTimerIntervalId && Number.isFinite(serverSec)) {
    startRunningStatsLocalTimer(serverSec);
  }

  if (!runningStatsTimerStartMs) {
    // ✅ 러닝페이지와 시간 공유: startedAtMs가 있으면 그 기준으로 seed
    const startedAtMs = getStartedAtMsFromStorage();
    if (startedAtMs && !runningStatsTimerIntervalId) {
      const elapsedSec = Math.max(
        0,
        Math.floor((Date.now() - startedAtMs) / 1000)
      );
      startRunningStatsLocalTimer(elapsedSec);
    }

    // seed가 없는데 러닝 중이면 0부터라도 흐르게
    if (
      currentSession?.status === "IN_PROGRESS" &&
      !runningStatsTimerIntervalId
    ) {
      startRunningStatsLocalTimer(0);
    }
  }

  if (!runningStatsTimerStartMs) {
    return Number.isFinite(serverSec) ? Math.max(0, Math.floor(serverSec)) : 0;
  }

  const elapsed = Math.floor((Date.now() - runningStatsTimerStartMs) / 1000);
  return Math.max(0, runningStatsTimerBaseSec + elapsed);
}

function updateRunningStatsModal(stats) {
  const distEl = document.getElementById("live-distance");
  const remainingEl = document.getElementById("live-remaining");
  const paceEl = document.getElementById("live-pace");
  const timeEl = document.getElementById("live-time");
  if (distEl && stats.totalDistance != null) {
    // ✅ 소수점 두자리까지 표시
    distEl.textContent = Number(stats.totalDistance).toFixed(2);
  }
  if (remainingEl && stats.remainingDistance != null) {
    // ✅ 소수점 두자리까지 표시
    remainingEl.textContent = Number(
      Math.max(0, stats.remainingDistance)
    ).toFixed(2);
  }

  const localTimeSec = getRunningStatsLocalTimeSec(stats);
  if (timeEl) {
    timeEl.textContent = formatTimeHMS(localTimeSec);
  }

  // ✅ 페이스는 서버 값(팀 평균)을 우선 사용한다.
  // - 로컬 계산은 미세한 오차로 깜빡임이 생길 수 있어 표시하지 않는다.
  if (paceEl) {
    const serverPace = Number(stats?.teamAveragePace);
    if (Number.isFinite(serverPace) && serverPace > 0) {
      lastPaceText = formatPaceMMSS(serverPace);
      paceEl.textContent = lastPaceText;
    } else {
      paceEl.textContent = lastPaceText;
    }
  }
}

// ============================================
// 이벤트 리스너
// ============================================

function setupEventListeners() {
  // 뒤로가기 버튼 - 채팅방 목록으로 이동
  const backButton = document.querySelector(".back-button");
  if (backButton) {
    backButton.addEventListener("click", function () {
      // 나가기 전에 마지막 읽은 시간 업데이트
      updateLastReadTime(currentSession.id);
      window.location.href = "/chat";
    });
  }

  // 스크롤 버튼
  const scrollButton = document.getElementById("scroll-to-bottom");
  const chatContainer = document.querySelector(".chat-container");

  if (scrollButton && chatContainer) {
    function toggleScrollButton() {
      const isScrolledToBottom =
        chatContainer.scrollHeight - chatContainer.scrollTop <=
        chatContainer.clientHeight + 50;
      if (isScrolledToBottom) {
        scrollButton.classList.remove("show");
      } else {
        scrollButton.classList.add("show");
      }
    }

    chatContainer.addEventListener("scroll", toggleScrollButton);
    toggleScrollButton();

    scrollButton.addEventListener("click", function () {
      chatContainer.scrollTo({
        top: chatContainer.scrollHeight,
        behavior: "smooth",
      });
    });
  }

  // 메시지 전송
  const messageInput = document.getElementById("message-input");
  const sendButton = document.getElementById("send-button");

  if (messageInput && sendButton) {
    sendButton.addEventListener("click", sendMessage);

    messageInput.addEventListener("keypress", function (e) {
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
      }
    });
  }

  // 러닝 통계 토글 (+ 버튼)
  const runningStatsToggleBtn = document.getElementById(
    "running-stats-toggle-btn"
  );
  if (runningStatsToggleBtn) {
    runningStatsToggleBtn.addEventListener("click", () => {
      toggleRunningStatsModal();
    });
  }

  // 하단(방장 영역) 버튼: 대기 중이면 시작, 진행 중이면 러닝 페이지로 이동
  const startBtn = document.getElementById("start-running-btn");
  if (startBtn) {
    startBtn.addEventListener("click", () => {
      if (currentSession?.status === "IN_PROGRESS") {
        window.location.href = `/running/${currentSession.id}`;
        return;
      }
      if (currentSession?.status === "COMPLETED") {
        window.location.href = `/running/${currentSession.id}`;
        return;
      }
      startRunning();
    });
  }

  // 러닝 페이지 이동 버튼 (수동)
  const goRunningBtn = document.getElementById("go-running-page-btn");
  if (goRunningBtn) {
    goRunningBtn.addEventListener("click", () => {
      if (currentSession?.id) {
        window.location.href = `/running/${currentSession.id}`;
      }
    });
  }

  // 런닝 취소 버튼
  const cancelBtn = document.getElementById("cancel-running-btn");
  if (cancelBtn) {
    cancelBtn.addEventListener("click", cancelRunning);
  }

  // 준비완료 버튼
  const readyButton = document.getElementById("ready-button");
  if (readyButton) {
    readyButton.addEventListener("click", toggleReadyStatus);
  }

  // 참여자 목록 모달 열기
  const moreMenuBtn = document.getElementById("more-menu-btn");
  if (moreMenuBtn) {
    moreMenuBtn.addEventListener("click", function () {
      openParticipantModal();
    });
  }

  // 참여자 목록 모달 닫기
  const modalOverlay = document.getElementById("participant-modal-overlay");
  const modalClose = document.getElementById("participant-modal-close");
  if (modalOverlay && modalClose) {
    modalClose.addEventListener("click", closeParticipantModal);
    modalOverlay.addEventListener("click", function (e) {
      if (e.target === modalOverlay) {
        closeParticipantModal();
      }
    });
  }

  // 채팅방 나가기 버튼
  const leaveChatBtn = document.getElementById("leave-chat-btn");
  if (leaveChatBtn) {
    leaveChatBtn.addEventListener("click", function () {
      if (confirm("채팅방을 나가시겠습니까?")) {
        closeParticipantModal();
        leaveChatRoom();
      }
    });
  }

  // 런닝 결과 모달 닫기 버튼
  const resultCloseBtn = document.getElementById("running-result-modal-close");
  if (resultCloseBtn) {
    resultCloseBtn.addEventListener("click", closeRunningResultModal);
  }

  // 런닝 결과 모달 오버레이 클릭 시 닫기
  const resultOverlay = document.getElementById("running-result-modal-overlay");
  if (resultOverlay) {
    resultOverlay.addEventListener("click", function (e) {
      if (e.target === resultOverlay) {
        closeRunningResultModal();
      }
    });
  }

  // 러닝 통계 모달 닫기/이동
  const runningStatsCloseBtn = document.getElementById(
    "running-stats-modal-close"
  );
  if (runningStatsCloseBtn) {
    runningStatsCloseBtn.addEventListener("click", closeRunningStatsModal);
  }
  const runningStatsOverlay = document.getElementById(
    "running-stats-modal-overlay"
  );
  if (runningStatsOverlay) {
    runningStatsOverlay.addEventListener("click", function (e) {
      if (e.target === runningStatsOverlay) {
        closeRunningStatsModal();
      }
    });
  }
  const goRunningModalBtn = document.getElementById(
    "go-running-page-btn-modal"
  );
  if (goRunningModalBtn) {
    goRunningModalBtn.addEventListener("click", () => {
      if (currentSession?.id) {
        window.location.href = `/running/${currentSession.id}`;
      }
    });
  }
}

// ============================================
// WebSocket 연결
// ============================================

function connectWebSocket() {
  if (typeof SockJS === "undefined" || typeof Stomp === "undefined") {
    console.error("WebSocket 라이브러리 로드 실패: SockJS/Stomp");
    alert(
      "WebSocket 라이브러리를 로드할 수 없습니다. 새로고침 후 다시 시도해주세요."
    );
    return;
  }

  const socket = new SockJS("/ws");
  stompClient = Stomp.over(socket);
  stompClient.debug = null;

  // JWT 토큰을 WebSocket 헤더에 포함
  const token = localStorage.getItem("accessToken");
  const headers = token ? { Authorization: "Bearer " + token } : {};

  stompClient.connect(
    headers,
    function (frame) {
      console.log("WebSocket 연결 성공");

      // 현재 사용자의 준비 상태 불러오기
      loadUserReadyStatus();

      // 과거 메시지 불러오기
      loadPreviousMessages();

      // 구독 (중복 방지)
      try {
        if (chatSubscription) {
          chatSubscription.unsubscribe();
          chatSubscription = null;
        }
      } catch (e) {
        // ignore
      }

      chatSubscription = stompClient.subscribe(
        "/sub/chat/" + currentSession.id,
        function (response) {
          const message = safeJsonParse(response.body, null);
          if (!message) {
            console.warn("메시지 파싱 실패:", response.body);
            return;
          }
          displayMessage(message);
          if (
            message.messageType === "SYSTEM" &&
            message.content &&
            message.content.includes("런닝이 시작되었습니다")
          ) {
            console.log("🏃 런닝 시작 감지 - 통계 구독 + 모달 표시");

            // 방장/참여자 모두: 통계 구독 (중복 구독 방지 로직 있음)
            subscribeToRunningStats();
            // ✅ 최신 stats 1회 seed (WS 수신 전에도 즉시 표시)
            // - session.status를 IN_PROGRESS로 올린 뒤에 방장 GPS publish를 시작해야 한다.
            //   (maybeStartHostGpsTrackingInChat 내부에서 status 체크)
            const startHostGpsAfterStatus = () => {
              // WS-only 정책: 서버 fetch seed 대신 localStorage 스냅샷으로 1회 표시
              seedLatestRunningStatsFromStorageOnce();
              maybeStartHostGpsTrackingInChat();
            };

            // ✅ TTS batch 로드(모드 기준) + 시작 안내 (참여자도 들리도록 isHost 체크 제거)
            ensureChatTtsOnce().then(() => {
              if (chatTtsReady && window.TtsManager) {
                window.TtsManager.speak("START_RUN", {
                  priority: 2,
                  cooldownMs: 0,
                });
              }
            });

            // ✅ 시작 시각 공유(러닝페이지/채팅방 시간 동기화)
            ensureStartedAtMsInStorage(Date.now());

            // 세션 상태 업데이트
            currentSession.status = "IN_PROGRESS";
            updateControlBar();
            startHostGpsAfterStatus();

            // 러닝 통계 모달 즉시 표시 (한 번만)
            // ✅ 모달 자동 오픈은 채팅 입력을 막을 수 있어 사용자가 직접 열도록 한다.
          }

          // KICK 메시지 처리
          if (message.messageType === "KICK") {
            // 내가 강퇴당한 경우
            if (message.senderId == currentUser.id) {
              alert("방장에 의해 강퇴되었습니다.");

              // 채팅방 목록으로 이동
              if (stompClient) {
                stompClient.disconnect();
              }
              window.location.href = "/chat";
              return;
            }

            // 다른 사람이 강퇴당한 경우 - 참여자 목록 갱신
            setTimeout(() => {
              loadParticipants(currentSession.id);

              if (isHost) {
                checkAllReadyAndUpdateButton();
              }
            }, 300);
          }

          // 시스템 메시지 수신 시 참여자 목록 자동 갱신
          if (message.messageType === "SYSTEM") {
            // 입장, 퇴장, 준비완료 메시지일 때 참여자 정보 업데이트
            setTimeout(() => {
              loadParticipants(currentSession.id);

              // 준비완료 메시지면 방장의 시작 버튼도 업데이트
              if (
                isHost &&
                (message.content.includes("준비완료") ||
                  message.content.includes("준비를 취소"))
              ) {
                checkAllReadyAndUpdateButton();
              }
            }, 300);
          }

          // 런닝 시작 메시지면 상태 업데이트
          if (
            message.messageType === "SYSTEM" &&
            message.content.includes("런닝이 시작되었습니다")
          ) {
            currentSession.status = "IN_PROGRESS";
            updateControlBar();
          }

          // 런닝 종료 메시지면 모든 참여자에게 결과 모달 표시
          if (
            message.messageType === "SYSTEM" &&
            message.content === "🏁 런닝이 종료되었습니다! 수고하셨습니다!"
          ) {
            console.log("🏁 런닝 종료 감지 - 결과 모달 표시");

            // ✅ completedHandled는 TTS 재생 후에 설정 (END_RUN TTS는 재생되도록)

            // 세션 상태 업데이트
            currentSession.status = "COMPLETED";
            updateControlBar();

            // 테스트 패널 숨기기
            const testPanel = document.getElementById("running-test-panel");
            if (testPanel) {
              testPanel.style.display = "none";
            }

            // GPS 구독 해제 (참여자)
            if (gpsSubscription) {
              gpsSubscription.unsubscribe();
              gpsSubscription = null;
              console.log("🛑 GPS 구독 해제됨 (참여자)");
            }

            // 결과 모달 표시 (모든 참여자)
            setTimeout(() => {
              showRunningResultModal();
            }, 500);

            // ✅ "러닝이 종료되었습니다" TTS는 재생 (이후 TTS는 차단)
            ensureChatTtsOnce().then(() => {
              if (chatTtsReady && window.TtsManager) {
                window.TtsManager.speak("END_RUN", {
                  priority: 2,
                  cooldownMs: 0,
                });
                // ✅ TTS 재생 후 completedHandled 설정 (이후 TTS 차단)
                setTimeout(() => {
                  completedHandled = true;
                }, 1000);
              } else {
                // TTS가 없으면 즉시 설정
                completedHandled = true;
              }
            });
          }
        }
      );

      // ✅ 이미 러닝 중인 상태로 들어온 경우(새로고침/재입장): 즉시 통계 구독 + 모달 표시
      if (currentSession?.status === "IN_PROGRESS") {
        subscribeToRunningStats();
        // ✅ 방장: 재입장 직후에도 GPS publish를 최대한 빨리 재개
        // (seed API가 지연/실패해도 참여자 실시간 수신이 끊기지 않게)
        maybeStartHostGpsTrackingInChat();
        // ✅ WS-only 정책: 서버 fetch seed 대신 localStorage 스냅샷으로 1회 표시
        seedLatestRunningStatsFromStorageOnce();
        // ✅ 방장: 채팅방에서도 GPS publish 유지 (러닝페이지 진입 전에도 참여자 진행 반영)
        maybeStartHostGpsTrackingInChat();
        // ✅ 채팅방 TTS batch 로드(재입장)
        ensureChatTtsOnce();
        // ✅ seed/WS가 실패해도 시간은 0초부터라도 흐르게
        if (!runningStatsTimerIntervalId) {
          const startedAtMs = getStartedAtMsFromStorage();
          if (startedAtMs) {
            const elapsedSec = Math.max(
              0,
              Math.floor((Date.now() - startedAtMs) / 1000)
            );
            startRunningStatsLocalTimer(elapsedSec);
          } else {
            startRunningStatsLocalTimer(0);
          }
        }
        // ✅ 모달 자동 오픈은 사용자가 직접 토글로 열도록 한다.
      }

      // 입장 메시지 전송 제거 (채팅방 생성 시에만 백엔드에서 자동 전송)
      // stompClient.send('/pub/chat/enter', {}, JSON.stringify({
      //   sessionId: currentSession.id,
      //   senderId: currentUser.id,
      //   senderName: currentUser.name
      // }));
    },
    function (error) {
      console.error("WebSocket 연결 실패:", error);
    }
  );
}

// ============================================
// 메시지 관련
// ============================================

// 과거 메시지 불러오기
function loadPreviousMessages() {
  let url = `/api/chat/${currentSession.id}/messages`;
  if (currentUserJoinedAt) {
    url += `?joinedAt=${encodeURIComponent(currentUserJoinedAt)}`;
  }

  fetchWithAuth(url)
    .then((response) => response.json())
    .then((result) => {
      if (result.success && result.data.length > 0) {
        result.data.forEach((message) => {
          displayMessage(message, true);
        });

        // 스크롤을 맨 아래로
        setTimeout(() => {
          const chatContainer = document.querySelector(".chat-container");
          if (chatContainer) {
            chatContainer.scrollTo({
              top: chatContainer.scrollHeight,
              behavior: "auto",
            });
          }
        }, 100);
      }
    })
    .catch((error) => console.error("메시지 로드 실패:", error));
}

// 메시지 전송
function sendMessage() {
  const input = document.getElementById("message-input");
  const content = input.value.trim();

  if (!content) {
    return;
  }

  const ok = safeStompSend("/pub/chat/message", {
    sessionId: currentSession.id,
    senderId: currentUser.id,
    senderName: currentUser.name,
    content: content,
    messageType: "TEXT",
  });

  if (!ok) {
    alert("메시지를 전송할 수 없습니다. 잠시 후 다시 시도해주세요.");
    return;
  }

  input.value = "";

  // 메시지 전송 후 즉시 스크롤을 맨 아래로
  setTimeout(() => {
    const chatContainer = document.querySelector(".chat-container");
    if (chatContainer) {
      chatContainer.scrollTo({
        top: chatContainer.scrollHeight,
        behavior: "smooth",
      });
    }
  }, 100);
}

// 메시지 표시
function displayMessage(message, isPrevious = false) {
  const messagesDiv = document.getElementById("chat-messages");

  // 시스템 메시지 (SYSTEM, KICK 포함)
  if (message.messageType === "SYSTEM" || message.messageType === "KICK") {
    const systemDiv = document.createElement("div");
    systemDiv.className = "system-message";
    const p = document.createElement("p");
    p.textContent = message.content;
    systemDiv.appendChild(p);
    messagesDiv.appendChild(systemDiv);

    // 참여자 목록 업데이트
    loadParticipants(currentSession.id);
  } else {
    const isMyMessage = message.senderId == currentUser.id;

    const messageItem = document.createElement("div");
    messageItem.className = `message-item ${
      isMyMessage ? "message-right" : "message-left"
    }`;
    
    // 메시지 데이터 저장 (시간 표시 판단용)
    messageItem.dataset.senderId = message.senderId;
    messageItem.dataset.createdAt = message.createdAt;

    if (!isMyMessage) {
      // 아바타
      const avatar = document.createElement("div");
      avatar.className = "message-avatar";
      
      // ✅ participantsList에서 프로필 이미지 찾기
      const participant = participantsList.find(p => p.userId == message.senderId);
      const profileImage = participant?.profileImage;
      
      if (profileImage) {
        const img = document.createElement('img');
        img.src = profileImage;
        img.alt = message.senderName;
        img.style.cssText = 'width: 100%; height: 100%; object-fit: cover; border-radius: 50%;';
        
        // 이미지 로드 실패 시 기본 아이콘으로 대체
        img.onerror = function() {
          avatar.innerHTML = '<svg width="18" height="21" viewBox="0 0 18 21" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M9 0C4.02944 0 0 4.02944 0 9C0 13.9706 4.02944 18 9 18C13.9706 18 18 13.9706 18 9C18 4.02944 13.9706 0 9 0Z" fill="#E5E7EB"/></svg>';
        };
        
        avatar.appendChild(img);
      } else {
        avatar.innerHTML =
          '<svg width="18" height="21" viewBox="0 0 18 21" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M9 0C4.02944 0 0 4.02944 0 9C0 13.9706 4.02944 18 9 18C13.9706 18 18 13.9706 18 9C18 4.02944 13.9706 0 9 0Z" fill="#E5E7EB"/></svg>';
      }
      
      messageItem.appendChild(avatar);
    }

    const contentWrapper = document.createElement("div");
    contentWrapper.className = "message-content-wrapper";

    if (!isMyMessage) {
      const sender = document.createElement("p");
      sender.className = "message-sender";
      sender.textContent = message.senderName;
      contentWrapper.appendChild(sender);
    }

    const bubble = document.createElement("div");
    bubble.className = `message-bubble ${
      isMyMessage ? "message-bubble-right" : "message-bubble-left"
    }`;
    const text = document.createElement("p");
    text.className = "message-text";
    text.textContent = message.content;
    bubble.appendChild(text);
    contentWrapper.appendChild(bubble);

    if (message.createdAt) {
      const time = document.createElement("p");
      time.className = "message-time";
      const date = new Date(message.createdAt);
      time.textContent = `${String(date.getHours()).padStart(2, "0")}:${String(
        date.getMinutes()
      ).padStart(2, "0")}`;
      contentWrapper.appendChild(time);
    }

    messageItem.appendChild(contentWrapper);
    messagesDiv.appendChild(messageItem);
    
    // 메시지 추가 후 시간 표시 여부 업데이트
    updateMessageTimeVisibility();
  }

  // 스크롤을 맨 아래로
  if (!isPrevious) {
    setTimeout(() => {
      const chatContainer = document.querySelector(".chat-container");
      if (chatContainer) {
        chatContainer.scrollTo({
          top: chatContainer.scrollHeight,
          behavior: "smooth",
        });
      }
    }, 100);
  }
}

/**
 * 메시지 시간 표시 여부 업데이트 (카톡 스타일)
 * 같은 사람이 같은 분에 보낸 연속된 메시지는 마지막에만 시간 표시
 */
function updateMessageTimeVisibility() {
  const messagesDiv = document.getElementById('chat-messages');
  const messageItems = messagesDiv.querySelectorAll('.message-item:not(.system-message)');
  
  for (let i = 0; i < messageItems.length; i++) {
    const currentMsg = messageItems[i];
    const nextMsg = messageItems[i + 1];
    const timeElement = currentMsg.querySelector('.message-time');
    
    if (!timeElement) continue;
    
    // 마지막 메시지면 항상 시간 표시
    if (!nextMsg) {
      timeElement.style.display = '';
      continue;
    }
    
    const currentSenderId = currentMsg.dataset.senderId;
    const nextSenderId = nextMsg.dataset.senderId;
    const currentCreatedAt = currentMsg.dataset.createdAt;
    const nextCreatedAt = nextMsg.dataset.createdAt;
    
    // 보낸 사람이 다르면 시간 표시
    if (currentSenderId !== nextSenderId) {
      timeElement.style.display = '';
      continue;
    }
    
    // 시간(분 단위) 비교
    if (currentCreatedAt && nextCreatedAt) {
      const currentTime = new Date(currentCreatedAt);
      const nextTime = new Date(nextCreatedAt);
      
      const isSameMinute = 
        currentTime.getFullYear() === nextTime.getFullYear() &&
        currentTime.getMonth() === nextTime.getMonth() &&
        currentTime.getDate() === nextTime.getDate() &&
        currentTime.getHours() === nextTime.getHours() &&
        currentTime.getMinutes() === nextTime.getMinutes();
      
      // 같은 분에 보낸 메시지면 시간 숨김
      if (isSameMinute) {
        timeElement.style.display = 'none';
      } else {
        timeElement.style.display = '';
      }
    }
  }
}

// ============================================
// 준비완료 / 런닝 시작
// ============================================

// 준비완료 토글 (userId 제거 - 서버에서 자동 처리)
function toggleReadyStatus() {
  fetchWithAuth(`/api/chat/sessions/${currentSession.id}/ready`, {
    method: "POST",
  })
    .then((response) => response.json())
    .then((result) => {
      if (result.success) {
        const isReady = result.data.isReady;

        // 버튼 UI 업데이트
        const readyButton = document.getElementById("ready-button");
        if (readyButton) {
          if (isReady) {
            readyButton.classList.add("ready-active");
            readyButton.textContent = "준비완료 취소";
          } else {
            readyButton.classList.remove("ready-active");
            readyButton.textContent = "✓ 준비완료";
          }
        }

        // 시스템 메시지 전송
        const message = isReady
          ? `${currentUser.name}님이 준비완료했습니다.`
          : `${currentUser.name}님이 준비를 취소했습니다.`;

        safeStompSend("/pub/chat/message", {
          sessionId: currentSession.id,
          senderId: null,
          senderName: "SYSTEM",
          content: message,
          messageType: "SYSTEM",
        });

        // 런닝 시작 버튼 상태 업데이트 (방장이면)
        if (isHost) {
          checkAllReadyAndUpdateButton();
        }
      } else {
        alert(result.message || "준비 상태 변경 실패");
      }
    })
    .catch((error) => {
      console.error("준비 상태 변경 실패:", error);
      alert("준비 상태 변경에 실패했습니다.");
    });
}

// 모두 준비완료 확인 및 런닝 시작 버튼 업데이트
function checkAllReadyAndUpdateButton() {
  fetchWithAuth(`/api/chat/sessions/${currentSession.id}/all-ready`)
    .then((response) => response.json())
    .then((result) => {
      if (result.success) {
        const allReady = result.data.allReady;
        const startBtn = document.getElementById("start-running-btn");

        if (startBtn) {
          if (allReady) {
            startBtn.disabled = false;
            startBtn.textContent = "🏃 런닝 시작";
            startBtn.style.opacity = "1";
          } else {
            startBtn.disabled = true;
            startBtn.textContent = `🏃 런닝 시작 (${result.data.readyCount}/${result.data.totalCount} 준비완료)`;
            startBtn.style.opacity = "0.5";
          }
        }
      }
    })
    .catch((error) => console.error("준비 상태 확인 실패:", error));
}

// 런닝 시작 (방장만, 모두 준비완료 시) - userId 제거
async function startRunning() {
  if (!isHost) {
    alert("방장만 런닝을 시작할 수 있습니다.");
    return;
  }

  if (!stompClient || !stompClient.connected) {
    alert("WebSocket 연결 중입니다. 잠시 후 다시 시도해주세요.");
    return;
  }

  try {
    // ============================================
    // ✅ 출발점 게이트: 출발점 20m 이내 + GPS 정확도(<=30m) 충족 시에만 시작 가능
    // - 코스가 없는 세션이면 게이트 생략
    // ============================================
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
    try {
      const spRes = await fetchWithAuth(
        `/api/running/sessions/${currentSession.id}/course-path`,
        { method: "GET" }
      );
      const spBody = await spRes.json().catch(() => null);
      if (spRes.ok && spBody?.success) {
        if (spBody?.data?.startLat != null && spBody?.data?.startLng != null) {
          startLat = Number(spBody.data.startLat);
          startLng = Number(spBody.data.startLng);
        }
      } else {
        // 코스 조회 실패는 시작을 막아야(치팅 방지) 하므로 에러로 처리
        throw new Error(spBody?.message || "출발점 정보를 불러올 수 없습니다.");
      }
    } catch (e) {
      throw new Error(e?.message || "출발점 정보를 불러올 수 없습니다.");
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

    if (!confirm("런닝을 시작하시겠습니까?\n러닝 페이지로 이동합니다.")) {
      return;
    }

    // ✅ 시작 상태를 서버에 반영 (IN_PROGRESS)
    const response = await fetchWithAuth(
      `/api/chat/sessions/${currentSession.id}/start`,
      {
        method: "POST",
        body: payload ? JSON.stringify(payload) : null,
      }
    );
    const result = await response.json();

    if (!result.success) {
      throw new Error(result.message || "런닝 시작에 실패했습니다.");
    }

    console.log("✅ 런닝 시작 API 호출 완료");

    // 세션 상태 업데이트
    currentSession.status = "IN_PROGRESS";
    updateControlBar();

    // ✅ 시작 시각 공유(러닝페이지/채팅방 시간 동기화)
    ensureStartedAtMsInStorage(Date.now());

    // 런닝 시작 시스템 메시지 전송 (pub/sub 기반 안내)
    safeStompSend("/pub/chat/message", {
      sessionId: currentSession.id,
      senderId: null,
      senderName: "SYSTEM",
      content: "🏃 런닝이 시작되었습니다! 모두 화이팅!",
      messageType: "SYSTEM",
    });

    // 방장은 러닝 페이지로 이동 (참여자는 채팅에서 모달로 안내)
    window.location.href = `/running/${currentSession.id}`;
  } catch (error) {
    console.error("런닝 시작 에러:", error);
    alert(error?.message || "런닝 시작에 실패했습니다.");
  }
}

// 런닝 취소 (세션 퇴장) - userId 제거
function cancelRunning() {
  if (currentSession.status === "IN_PROGRESS") {
    alert("런닝이 진행중이라 취소할 수 없습니다.");
    return;
  }

  if (!confirm("정말 런닝을 취소하시겠습니까?\n채팅방에서 나가게 됩니다.")) {
    return;
  }

  // 퇴장 메시지 전송
  safeStompSend("/pub/chat/message", {
    sessionId: currentSession.id,
    senderId: null,
    senderName: "SYSTEM",
    content: `${currentUser.name}님이 런닝을 취소했습니다.`,
    messageType: "SYSTEM",
  });

  // 세션에서 퇴장 (새 API 사용)
  fetchWithAuth(`/api/chat/sessions/${currentSession.id}/leave`, {
    method: "DELETE",
  })
    .then((response) => response.json())
    .then((result) => {
      setTimeout(() => {
        if (stompClient) {
          stompClient.disconnect();
        }
        alert("런닝을 취소했습니다.");
        window.history.back();
      }, 300);
    })
    .catch((error) => {
      console.error("퇴장 실패:", error);
    });
}

// ============================================
// 채팅방 퇴장
// ============================================

function leaveChatRoom() {
  if (stompClient !== null) {
    // 1. 퇴장 메시지 전송
    safeStompSend("/pub/chat/leave", {
      sessionId: currentSession.id,
      senderId: currentUser.id,
      senderName: currentUser.name,
    });

    // 2. DB에서 참가자 삭제 (새 API 사용)
    fetchWithAuth(`/api/chat/sessions/${currentSession.id}/leave`, {
      method: "DELETE",
    })
      .then((response) => response.json())
      .then((result) => {
        console.log("퇴장 완료:", result);
      })
      .catch((error) => {
        console.error("퇴장 API 실패:", error);
      })
      .finally(() => {
        // 3. WebSocket 연결 끊고 뒤로가기
        setTimeout(() => {
          stompClient.disconnect();
          window.history.back();
        }, 300);
      });
  } else {
    window.history.back();
  }
}

// ============================================
// 참여자 목록 모달
// ============================================

function openParticipantModal() {
  const modal = document.getElementById("participant-modal-overlay");
  if (modal) {
    modal.classList.add("show");
    renderParticipantList();
    loadParticipants(currentSession.id);
  }
}

function closeParticipantModal() {
  const modal = document.getElementById("participant-modal-overlay");
  if (modal) {
    modal.classList.remove("show");
  }
}

function renderParticipantList() {
  const listContainer = document.getElementById("participant-list");
  const readyCountEl = document.getElementById("participant-ready-count");
  const readyTotalEl = document.getElementById("participant-ready-total");

  if (!listContainer || !participantsList.length) {
    return;
  }

  // 준비 완료 수 및 전체 인원 업데이트
  const readyCount = participantsList.filter((p) => p.isReady).length;
  const totalCount = participantsList.length;

  if (readyCountEl) {
    readyCountEl.textContent = readyCount;
  }

  if (readyTotalEl) {
    readyTotalEl.textContent = `/${totalCount}명 준비완료`;
  }

  // 목록 초기화
  listContainer.innerHTML = "";

  // 참여자 목록 렌더링
  participantsList.forEach((participant) => {
    const isCurrentUser = participant.userId == currentUser.id;
    const isHostUser = participant.userId == hostId;
    const isReady = participant.isReady;

    const item = document.createElement("div");
    item.className = "participant-item";

    // 아바타 래퍼
    const avatarWrapper = document.createElement("div");
    avatarWrapper.className = "participant-avatar-wrapper";

    const avatar = document.createElement("div");
    avatar.className = "participant-avatar";
    if (isHostUser) {
      avatar.classList.add("avatar-yellow");
    }

    // 아바타 아이콘
    // ✅ 프로필 이미지가 있으면 표시, 없으면 기본 SVG 아이콘
    if (participant.profileImage) {
      avatar.innerHTML = `<img src="${participant.profileImage}" alt="${participant.name}" 
                               style="width: 100%; height: 100%; object-fit: cover; border-radius: 50%;" 
                               onerror="this.style.display='none'; this.nextElementSibling.style.display='block';">
                          <svg class="participant-avatar-icon" width="22" height="26" viewBox="0 0 22 26" fill="none" xmlns="http://www.w3.org/2000/svg" style="display: none;">
                            <path d="M11 0C4.925 0 0 4.925 0 11C0 17.075 4.925 22 11 22C17.075 22 22 17.075 22 11C22 4.925 17.075 0 11 0Z" fill="#E5E7EB"/>
                          </svg>`;
    } else {
      avatar.innerHTML =
        '<svg class="participant-avatar-icon" width="22" height="26" viewBox="0 0 22 26" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M11 0C4.925 0 0 4.925 0 11C0 17.075 4.925 22 11 22C17.075 22 22 17.075 22 11C22 4.925 17.075 0 11 0Z" fill="#E5E7EB"/></svg>';
    }

    // 준비 상태 배지 (러닝 시작 전에만 표시)
    if (currentSession?.status !== "IN_PROGRESS") {
      const statusBadge = document.createElement("div");
      statusBadge.className = "participant-status-badge";
      if (!isReady) {
        statusBadge.classList.add("waiting");
      } else {
        statusBadge.innerHTML =
          '<svg width="10" height="10" viewBox="0 0 10 10" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M8.33333 2.5L3.75 7.08333L1.66667 5" stroke="white" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>';
      }
      avatarWrapper.appendChild(statusBadge);
    }

    avatarWrapper.appendChild(avatar);

    // 참여자 정보
    const info = document.createElement("div");
    info.className = "participant-info";

    const nameRow = document.createElement("div");
    nameRow.className = "participant-name-row";

    const name = document.createElement("span");
    name.className = "participant-name";
    if (isCurrentUser) {
      name.classList.add("current-user");
      name.textContent = "나";
    } else {
      name.textContent = participant.name || "이름 없음";
    }

    nameRow.appendChild(name);

    // 역할 배지
    if (isHostUser) {
      const hostBadge = document.createElement("span");
      hostBadge.className = "participant-role-badge host";
      hostBadge.textContent = "방장";
      nameRow.appendChild(hostBadge);
    } else if (isCurrentUser) {
      const meBadge = document.createElement("span");
      meBadge.className = "participant-role-badge me";
      meBadge.textContent = "나";
      nameRow.appendChild(meBadge);
    }

    info.appendChild(nameRow);

    // 평균 페이스
    const pace = document.createElement("div");
    pace.className = "participant-pace";
    pace.textContent = `평균 페이스 ${participant.averagePace || "5:30"} /km`;
    info.appendChild(pace);

    // 준비 상태 + 강퇴 버튼
    const rightSection = document.createElement("div");
    rightSection.className = "participant-right-section";

    // 준비 상태 표시 (러닝 시작 전에만 표시)
    if (currentSession?.status !== "IN_PROGRESS") {
      const readyStatus = document.createElement("div");
      readyStatus.className = "participant-ready-status";
      const readyText = document.createElement("span");
      readyText.className = "participant-ready-text";
      if (!isReady) {
        readyText.classList.add("waiting");
        readyText.textContent = "대기중";
      } else {
        readyText.textContent = "준비완료";
      }
      readyStatus.appendChild(readyText);
      rightSection.appendChild(readyStatus);
    }

    // 강퇴 버튼 (방장이고, 자기 자신이 아닌 경우만)
    if (isHost && !isCurrentUser) {
      const kickBtn = document.createElement("button");
      kickBtn.className = "kick-btn";
      kickBtn.textContent = "강퇴";
      kickBtn.onclick = () =>
        kickParticipant(participant.userId, participant.name);
      rightSection.appendChild(kickBtn);
    }

    item.appendChild(avatarWrapper);
    item.appendChild(info);
    item.appendChild(rightSection);

    listContainer.appendChild(item);
  });
}

// ============================================
// 강퇴 기능
// ============================================

async function kickParticipant(userId, userName) {
  if (!confirm(`${userName}님을 강퇴하시겠습니까?`)) {
    return;
  }

  try {
    const response = await fetchWithAuth(
      `/api/chat/sessions/${currentSession.id}/kick/${userId}`,
      {
        method: "DELETE",
      }
    );

    if (!response.ok) {
      const error = await response.json();
      alert(error.message || "강퇴에 실패했습니다.");
      return;
    }

    console.log(`${userName}님을 강퇴했습니다.`);

    // 시스템 메시지가 WebSocket으로 전달되므로
    // 참여자 목록은 자동으로 갱신됨
  } catch (error) {
    console.error("강퇴 에러:", error);
    alert("강퇴 중 오류가 발생했습니다.");
  }
}

// ============================================
// 런닝 통계 구독 및 UI
// ============================================

let gpsSubscription = null;

/**
 * 실시간 런닝 통계 구독
 */
function subscribeToRunningStats() {
  if (!stompClient || !stompClient.connected) {
    console.error("❌ WebSocket 연결 없음");
    return;
  }

  // 이미 구독 중이면 중복 구독 방지
  if (gpsSubscription) {
    console.log("⚠️ 이미 런닝 통계를 구독 중입니다");
    return;
  }

  gpsSubscription = stompClient.subscribe(
    `/sub/running/${currentSession.id}`,
    function (message) {
      const stats = JSON.parse(message.body);
      console.log("📊 통계 수신:", stats);

      // ✅ localStorage에 lastStatsAtMs 업데이트
      try {
        localStorage.setItem(
          runningStorageKey("lastStatsAtMs"),
          String(Date.now())
        );
      } catch (e) {}

      updateRunningUI(stats);

      // ✅ 완주 시 자동 종료(정책 통일: 코스+거리 조건은 서버 stats.completed로 판정)
      const completed = (stats.isCompleted ?? stats.completed) === true;
      if (completed && runningTracker && runningTracker.isTracking) {
        console.log("🏁 완주 감지! GPS 추적 중지 + 종료 처리");
        // ✅ 러닝 종료 플래그 설정 (TTS 비활성화용)
        completedHandled = true;
        finishRunning(true); // 자동 종료
      }
    }
  );

  console.log("✅ 런닝 통계 구독 완료:", `/sub/running/${currentSession.id}`);
  subscribeToRunningErrors();
}

// ============================================
// 채팅방에서 방장 GPS publish 시, 코스 매칭 진행도(matchedDistanceM)도 같이 전송하기 위한 최소 매칭 로직
// - 러닝페이지처럼 선이 지워지려면 stats.hostMatchedDistM가 계속 갱신되어야 한다.
// - 방장이 채팅방에만 있어도 참가자 러닝페이지가 즉시 따라오게 만들기 위해 필요.
// ============================================
let hostCoursePath = null; // [{lat,lng}, ...]
let hostCourseCumDistM = null; // 누적거리(m)
let hostCourseSegLenM = null;
let hostLastMatchedDistM = 0;
let hostLastMatchedSegIdx = 0;
let hostLastMatchedAtMs = 0;
// ✅ 진행도 판단 baseline은 "accept된 좌표"만 사용 (드리프트로 baseline 오염 방지)
let hostLastAcceptedProgressLat = null;
let hostLastAcceptedProgressLng = null;

function clampNum(v, min, max) {
  return Math.max(min, Math.min(max, v));
}

function findHostCourseSegIdxByAlongM(alongM) {
  if (!hostCourseCumDistM || hostCourseCumDistM.length < 2) {
    return 0;
  }
  const target = Math.max(0, Number(alongM) || 0);
  let lo = 0;
  let hi = hostCourseCumDistM.length - 1;
  while (lo < hi) {
    const mid = Math.floor((lo + hi + 1) / 2);
    if ((hostCourseCumDistM[mid] || 0) <= target) {
      lo = mid;
    } else {
      hi = mid - 1;
    }
  }
  return Math.min(lo, hostCourseCumDistM.length - 2);
}

function haversineMeters(lat1, lng1, lat2, lng2) {
  const R = 6371000.0;
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

function projectLngLatToXY(originLat, originLng, lat, lng) {
  const cosLat = Math.cos((originLat * Math.PI) / 180);
  const x = (lng - originLng) * 111320.0 * cosLat;
  const y = (lat - originLat) * 110540.0;
  return { x, y };
}

async function ensureHostCourseLoadedForChat() {
  if (hostCoursePath && hostCoursePath.length > 1) {
    return true;
  }
  if (!currentSession?.id) {
    return false;
  }

  try {
    const res = await fetchWithAuth(
      `/api/running/sessions/${currentSession.id}/course-path`,
      { method: "GET" }
    );
    const body = await res.json().catch(() => null);
    if (!res.ok || !body?.success) {
      return false;
    }

    const full = body?.data?.fullPath;
    if (!full?.coordinates || !Array.isArray(full.coordinates)) {
      return false;
    }

    hostCoursePath = full.coordinates.map((coord) => ({
      lng: coord[0],
      lat: coord[1],
    }));

    if (hostCoursePath.length < 2) {
      return false;
    }

    hostCourseCumDistM = new Array(hostCoursePath.length).fill(0);
    hostCourseSegLenM = new Array(hostCoursePath.length - 1).fill(0);
    let acc = 0;
    for (let i = 1; i < hostCoursePath.length; i++) {
      const a = hostCoursePath[i - 1];
      const b = hostCoursePath[i];
      const seg = haversineMeters(a.lat, a.lng, b.lat, b.lng);
      hostCourseSegLenM[i - 1] = seg;
      acc += seg;
      hostCourseCumDistM[i] = acc;
    }
    return true;
  } catch (e) {
    return false;
  }
}

function matchProgressOnCourseForChat(lat, lng, accuracyM) {
  if (!hostCoursePath || !hostCourseCumDistM || !hostCourseSegLenM) {
    return null;
  }
  if (hostCoursePath.length < 2) {
    return null;
  }

  // ✅ 백엔드와 동일한 허용 오차 로직: clamp(accuracy * 2.0, 50.0, 150.0)
  const baseTol = 50; // 백엔드 최소값과 동일
  const defaultAccuracyM = 30.0;
  const accM = Number.isFinite(accuracyM) ? accuracyM : defaultAccuracyM;
  const tol = clampNum(accM * 2.0, 50.0, 150.0); // 백엔드와 동일한 계산식

  // ✅ 지워진 경로의 시작점(현재 진행도 지점) 근처를 지나가면 지난 것으로 처리
  // - 이 지점은 remainingPath의 시작점이 됨
  // - 이 지점 근처를 지나가면 경로를 지워야 함
  const hostCourseTotalDistM =
    hostCourseCumDistM && hostCourseCumDistM.length > 0
      ? hostCourseCumDistM[hostCourseCumDistM.length - 1]
      : 0;

  if (hostLastMatchedDistM > 0 && hostLastMatchedDistM < hostCourseTotalDistM) {
    // 현재 진행도 지점의 좌표 찾기
    const baseIdx = findHostCourseSegIdxByAlongM(hostLastMatchedDistM);
    if (baseIdx >= 0 && baseIdx < hostCoursePath.length - 1) {
      const a = hostCoursePath[baseIdx];
      const b = hostCoursePath[baseIdx + 1];
      const segLen = Math.max(1, hostCourseSegLenM[baseIdx] || 0);
      const segStartDist = hostCourseCumDistM[baseIdx] || 0;
      const t =
        hostLastMatchedDistM > segStartDist
          ? Math.min(1, (hostLastMatchedDistM - segStartDist) / segLen)
          : 0;

      // 진행도 지점의 좌표
      const progressPoint = {
        lat: a.lat + (b.lat - a.lat) * t,
        lng: a.lng + (b.lng - a.lng) * t,
      };

      // GPS 위치가 진행도 지점 근처에 있는지 확인
      const distToProgressPoint = haversineMeters(
        lat,
        lng,
        progressPoint.lat,
        progressPoint.lng
      );

      // ✅ 진행도 지점 근처를 지나가면 지난 것으로 처리 (가로지르기 방지: 앞으로만)
      // - tol 이내에 있으면 진행도를 그 지점으로 설정
      // - 단, 뒤로 가는 것은 허용하지 않음 (가로지르기 방지)
      if (distToProgressPoint <= tol) {
        // 진행도 지점 근처를 지나감 → 경로를 지워야 함
        return {
          segIdx: baseIdx,
          t: t,
          distM: distToProgressPoint,
          alongM: hostLastMatchedDistM,
          tolM: tol,
          matched: true, // 근처를 지나갔으므로 지난 것으로 처리
        };
      }
    }
  }

  // ✅ "남아있는 코스 위에서만" 진행되도록:
  // - 탐색 시작점을 현재 진행도 기준으로 앞으로만 제한(backtracking 금지)
  const baseIdx = findHostCourseSegIdxByAlongM(hostLastMatchedDistM);
  const from = clampNum(
    Math.max(baseIdx, hostLastMatchedSegIdx),
    0,
    hostCoursePath.length - 2
  );
  // 너무 멀리 앞 세그먼트를 탐색하면 루프/근접 구간에서 "먼 세그먼트 점프"가 발생할 수 있음
  const to = clampNum(from + 20, 0, hostCoursePath.length - 2);

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
    const aLL = hostCoursePath[i];
    const bLL = hostCoursePath[i + 1];
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

    const t = clampNum((wx * vx + wy * vy) / vv, 0, 1);
    const cx = a.x + t * vx;
    const cy = a.y + t * vy;
    const dx = p.x - cx;
    const dy = p.y - cy;
    const d = Math.sqrt(dx * dx + dy * dy);

    if (d < best.distM) {
      const segLen = hostCourseSegLenM[i] || 0;
      const alongM = (hostCourseCumDistM[i] || 0) + t * segLen;
      best = { segIdx: i, t, distM: d, alongM };
    }
  }

  if (!Number.isFinite(best.distM)) {
    return null;
  }

  // ✅ 가로지르기 방지: 진행 방향 확인
  // - alongM이 이전 값보다 앞으로 진행하는지 확인 (최소 5m 이상 앞으로)
  // - 뒤로 가거나 가로지르는 경우는 매칭하지 않음
  const minForwardProgressM = 5.0; // 최소 진행 거리 (가로지르기 방지)
  if (
    hostLastMatchedDistM > 0 &&
    best.alongM < hostLastMatchedDistM + minForwardProgressM
  ) {
    // 뒤로 가거나 가로지르는 경우
    // 단, tol 이내에서 약간의 후퇴는 허용 (GPS 오차 고려)
    const backwardM = hostLastMatchedDistM - best.alongM;
    if (backwardM > tol * 0.3) {
      // 30% 이상 뒤로 가면 무시
      return null;
    }
  }

  return { ...best, tolM: tol, matched: best.distM <= tol };
}

function maybeStartHostGpsTrackingInChat() {
  if (!isHost) {
    return;
  }
  if (currentSession?.status !== "IN_PROGRESS") {
    return;
  }
  if (!stompClient || !stompClient.connected) {
    return;
  }
  if (typeof RunningTracker === "undefined") {
    return;
  }

  // 이미 추적 중이면 중복 방지
  if (runningTracker && runningTracker.isTracking) {
    return;
  }

  try {
    runningTracker = new RunningTracker(
      currentSession.id,
      currentUser.id,
      stompClient,
      false
    );

    // ✅ 채팅방에서 GPS 추적 시작 시 초기 필터링 강화
    // - 채팅방에서 시작하는 경우는 이미 러닝이 진행 중일 수 있으므로
    //   첫 GPS 수신 시점부터 10초간 강한 필터링 적용
    let chatGpsStartTime = Date.now();
    let chatGpsFirstPosition = null;

    // ✅ 방장만 matchedDistanceM을 전송 (백엔드의 자체 매칭을 피하고, 참가자가 덮어쓰는 사고 방지)
    try {
      runningTracker.includeMatchedDistanceM = true;
    } catch (e) {
      // ignore
    }

    // 러닝페이지와 동일하게: 정확도/점프 필터를 통과한 GPS만 전송
    // (기본값은 RunningTracker 내부 기본값을 사용)

    // ✅ 채팅방에서도 matchedDistanceM 전송(호스트 기준 선 지우기/참가자 실시간 동기화)
    // - 재입장 시 이미 진행된 alongM이 있으면 그 값으로 시드
    try {
      const seeded =
        Number(latestRunningStats?.hostMatchedDistM) ||
        Number(getLastHostMatchedDistMFromStorage()) ||
        0;
      if (Number.isFinite(seeded) && seeded > 0) {
        hostLastMatchedDistM = seeded;
      }
    } catch (e) {
      // ignore
    }
    ensureHostCourseLoadedForChat().then((ok) => {
      if (!ok) {
        return;
      }
      try {
        const original = runningTracker.onGPSUpdate.bind(runningTracker);
        runningTracker.onGPSUpdate = (position) => {
          // ✅ 채팅방에서 GPS 추적 시작 직후 필터링 강화
          const now = Date.now();
          const timeSinceChatStart = (now - chatGpsStartTime) / 1000;
          const isJustStartedInChat = timeSinceChatStart < 10; // 채팅방 시작 후 10초 이내

          if (isJustStartedInChat && position?.coords) {
            const coords = position.coords;

            // 첫 GPS는 기준점으로 저장
            if (!chatGpsFirstPosition) {
              // 정확도가 좋은 GPS만 기준점으로 사용
              if (coords.accuracy == null || coords.accuracy > 20) {
                return; // 첫 GPS도 정확도 체크
              }
              chatGpsFirstPosition = {
                lat: coords.latitude,
                lng: coords.longitude,
                timestamp: now,
              };
            } else {
              // 시작점에서 50m 이상 튀는 GPS는 제외
              const distFromStart = haversineMeters(
                chatGpsFirstPosition.lat,
                chatGpsFirstPosition.lng,
                coords.latitude,
                coords.longitude
              );
              if (distFromStart > 50) {
                return; // 튀는 GPS 제외
              }

              // 이전 GPS와의 거리 체크 (30m 이상 점프는 제외)
              if (runningTracker.lastPosition) {
                const prev = runningTracker.lastPosition.coords;
                const distKm = runningTracker.calculateDistance(
                  prev.latitude,
                  prev.longitude,
                  coords.latitude,
                  coords.longitude
                );
                if (Number.isFinite(distKm) && distKm > 0.03) {
                  return; // 30m 이상 점프는 제외
                }
              }
            }
          }

          // ✅ 첫 전송부터 matchedDistanceM이 null이면 백엔드가 자체 매칭으로 큰 alongM을 잡을 수 있음
          // 호스트는 항상 "현재 값(초기 0/시드)"을 먼저 실어서 서버 자체 매칭을 끈다.
          try {
            if (runningTracker.matchedDistanceM == null) {
              runningTracker.matchedDistanceM = hostLastMatchedDistM;
            }
          } catch (e) {
            // ignore
          }

          original(position);
          const c = position?.coords;
          if (!c) {
            return;
          }

          const matched = matchProgressOnCourseForChat(
            c.latitude,
            c.longitude,
            c.accuracy
          );
          if (
            matched &&
            matched.matched &&
            matched.alongM >= hostLastMatchedDistM
          ) {
            // ✅ 정확한 GPS만: 정확도가 나쁘면 진행도 갱신 자체를 하지 않는다.
            const acc = Number(c.accuracy);
            if (!Number.isFinite(acc) || acc > 20) {
              return;
            }

            let nextAlongM = Number(matched.alongM) || 0;

            // ✅ 튀는 값은 캡으로 올리지 말고 그냥 버린다.
            // - 이번 위치 변화(movedM) 대비 along 증가가 과도하면 무시
            try {
              const lat = Number(c.latitude);
              const lng = Number(c.longitude);
              if (Number.isFinite(lat) && Number.isFinite(lng)) {
                const hasPrev =
                  hostLastAcceptedProgressLat != null &&
                  hostLastAcceptedProgressLng != null;
                const movedM =
                  hasPrev &&
                  Number.isFinite(Number(hostLastAcceptedProgressLat)) &&
                  Number.isFinite(Number(hostLastAcceptedProgressLng))
                    ? haversineMeters(
                        hostLastAcceptedProgressLat,
                        hostLastAcceptedProgressLng,
                        lat,
                        lng
                      )
                    : 0;

                const speed = Number(c.speed);
                const isStationary =
                  speed == null || !Number.isFinite(speed)
                    ? movedM < 2.0
                    : speed < 0.5 && movedM < 2.0;
                if (isStationary) {
                  return;
                }

                const deltaM = nextAlongM - hostLastMatchedDistM;
                // 첫 기준점이 없을 때는(채팅방에서 트래킹 시작 직후) 작은 범위만 허용
                const maxDeltaM = hasPrev ? Math.max(5, movedM + 5) : 20;
                if (deltaM > maxDeltaM) {
                  return;
                }
              }
            } catch (e) {
              return;
            }
            try {
              const totalM =
                hostCourseCumDistM &&
                hostCourseCumDistM.length > 0 &&
                Number.isFinite(
                  hostCourseCumDistM[hostCourseCumDistM.length - 1]
                )
                  ? hostCourseCumDistM[hostCourseCumDistM.length - 1]
                  : null;
              if (totalM != null) {
                nextAlongM = Math.max(0, Math.min(nextAlongM, totalM));
              }
            } catch (e) {
              // ignore
            }

            hostLastMatchedDistM = nextAlongM;
            hostLastMatchedSegIdx = matched.segIdx;
            // ✅ baseline(이동 기준점)은 "accept"될 때만 갱신한다.
            try {
              const lat = Number(c.latitude);
              const lng = Number(c.longitude);
              if (Number.isFinite(lat) && Number.isFinite(lng)) {
                hostLastAcceptedProgressLat = lat;
                hostLastAcceptedProgressLng = lng;
              }
            } catch (e) {
              // ignore
            }
            runningTracker.matchedDistanceM = hostLastMatchedDistM;
          }
        };
      } catch (e) {
        // ignore
      }
    });

    // 러닝페이지/채팅방 시간 공유값이 있으면 그 기준으로 러닝타임을 맞춘다
    const startedAtMs = getStartedAtMsFromStorage();
    if (startedAtMs && typeof runningTracker.bootstrap === "function") {
      const elapsedSec = Math.max(
        0,
        Math.floor((Date.now() - startedAtMs) / 1000)
      );
      const d0 =
        latestRunningStats?.totalDistance ??
        getLastTotalDistanceFromStorage() ??
        0;
      runningTracker.bootstrap(d0, elapsedSec);
    } else if (
      latestRunningStats &&
      typeof runningTracker.bootstrap === "function"
    ) {
      runningTracker.bootstrap(
        latestRunningStats.totalDistance ?? 0,
        latestRunningStats.totalRunningTime ?? 0
      );
    } else if (typeof runningTracker.bootstrap === "function") {
      const d0 = getLastTotalDistanceFromStorage();
      const t0 = getLastTotalRunningTimeFromStorage();
      if (d0 != null && t0 != null) {
        runningTracker.bootstrap(d0, t0);
      }
    }

    runningTracker.startTracking();
    console.log("✅ 채팅방에서 방장 GPS publish 시작됨");
  } catch (e) {
    console.warn("채팅방 GPS 추적 시작 실패(무시):", e?.message || e);
  }
}

/**
 * 런닝 UI 업데이트
 */
function updateRunningUI(stats) {
  // 모달/토글용 최신값 캐시
  latestRunningStats = stats;

  // ✅ 러닝↔채팅 페이지 전환 시 누적거리/시간이 리셋되지 않도록 스냅샷 저장
  try {
    if (stats && stats.totalDistance != null) {
      localStorage.setItem(
        runningStorageKey("lastTotalDistance"),
        String(Number(stats.totalDistance) || 0)
      );
    }
    if (stats && stats.totalRunningTime != null) {
      localStorage.setItem(
        runningStorageKey("lastTotalRunningTime"),
        String(Math.max(0, Number(stats.totalRunningTime) || 0))
      );
    }
  } catch (e) {}

  // ✅ 코스 진행도도 스냅샷 저장(페이지 전환 시 선 트리밍 일관성)
  try {
    if (
      stats &&
      stats.hostMatchedDistM != null &&
      Number.isFinite(stats.hostMatchedDistM)
    ) {
      localStorage.setItem(
        runningStorageKey("lastHostMatchedDistM"),
        String(Math.max(0, Number(stats.hostMatchedDistM) || 0))
      );
    }
  } catch (e) {}

  // 테스트 패널 요소들
  const teamPaceEl = document.getElementById("test-pace");
  const currentDistanceEl = document.getElementById("test-distance");
  const remainingDistanceEl = document.getElementById("test-remaining");
  const runningTimeEl = document.getElementById("test-time");
  const segmentsEl = document.getElementById("test-segments");

  // 팀 평균 페이스 (서버 값) - 서버가 없으면 직전 값 유지
  if (teamPaceEl) {
    const serverPace = Number(stats?.teamAveragePace);
    if (Number.isFinite(serverPace) && serverPace > 0) {
      const paceMin = Math.floor(serverPace);
      const paceSec = Math.round((serverPace - paceMin) * 60);
      lastPaceText = `${paceMin}:${String(paceSec).padStart(2, "0")}/km`;
      teamPaceEl.textContent = lastPaceText;
    } else {
      teamPaceEl.textContent = lastPaceText;
    }
  }

  // 현재 거리
  if (currentDistanceEl && stats.totalDistance !== undefined) {
    currentDistanceEl.textContent = `${Number(stats.totalDistance).toFixed(
      1
    )}km`;
  }

  // 남은 거리
  if (remainingDistanceEl && stats.remainingDistance !== undefined) {
    const remaining = Math.max(0, stats.remainingDistance);
    remainingDistanceEl.textContent = `${Number(remaining).toFixed(1)}km`;
  }

  // 런닝 시간
  if (runningTimeEl && stats.totalRunningTime !== undefined) {
    const hours = Math.floor(stats.totalRunningTime / 3600);
    const minutes = Math.floor((stats.totalRunningTime % 3600) / 60);
    const seconds = stats.totalRunningTime % 60;
    runningTimeEl.textContent = `${String(hours).padStart(2, "0")}:${String(
      minutes
    ).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
  }

  // km별 페이스 업데이트
  if (
    segmentsEl &&
    stats.segmentPaces &&
    Object.keys(stats.segmentPaces).length > 0
  ) {
    segmentsEl.innerHTML = ""; // 초기화

    // Map → 배열 변환 후 표시
    Object.entries(stats.segmentPaces).forEach(([km, pace]) => {
      const segmentDiv = document.createElement("div");
      segmentDiv.style.cssText =
        "margin-bottom: 4px; font-size: 12px; color: #1F2937;";

      const paceMin = Math.floor(pace);
      const paceSec = Math.round((pace - paceMin) * 60);

      segmentDiv.textContent = `${km}km: ${paceMin}:${String(paceSec).padStart(
        2,
        "0"
      )}/km`;
      segmentsEl.appendChild(segmentDiv);
    });
  } else if (segmentsEl) {
    segmentsEl.innerHTML =
      '<div style="font-size: 12px; color: #9CA3AF;">데이터 수신 대기 중...</div>';
  }

  console.log("📈 UI 업데이트:", {
    pace: stats.teamAveragePace,
    distance: stats.totalDistance,
    remaining: stats.remainingDistance,
    time: stats.totalRunningTime,
    segments: stats.segmentPaces ? Object.keys(stats.segmentPaces).length : 0,
  });

  // 러닝 통계 모달이 열려있으면 즉시 갱신
  const overlay = document.getElementById("running-stats-modal-overlay");
  if (overlay && overlay.classList.contains("show")) {
    updateRunningStatsModal(stats);
  }
}

/**
 * 런닝 재개 (채팅방 다시 입장 시)
 */
function resumeRunning() {
  if (!stompClient || !stompClient.connected) {
    console.error("❌ WebSocket 연결 없음");
    return;
  }

  console.log("🔄 런닝 재개 시작...");

  // 1. 통계 구독 (모든 참여자)
  subscribeToRunningStats();

  // 2. UI 표시 (모든 참여자)
  showRunningUI();

  // 3. 방장이면 GPS 추적 재시작
  if (isHost) {
    console.log("🎯 방장 - GPS 추적 재시작 (채팅방 publish 유지)");
    maybeStartHostGpsTrackingInChat();
  }

  console.log("✅ 런닝 재개 완료");
}

/**
 * 런닝 UI 표시
 */
function showRunningUI() {
  const testPanel = document.getElementById("running-test-panel");
  if (testPanel) {
    testPanel.style.display = "block";
    console.log("✅ 테스트 패널 표시");
  }
}

/**
 * 런닝 종료
 */
async function finishRunning(isAuto = false) {
  if (!isHost && !isAuto) {
    alert("방장만 런닝을 종료할 수 있습니다.");
    return;
  }

  // ✅ 자유러닝(코스 없음)이면 러닝페이지로 이동 유도
  if (currentSession.courseId == null) {
    const confirmMove = confirm(
      "자유러닝은 코스 저장이 필요합니다.\n러닝 페이지로 이동하여 코스를 저장하시겠습니까?"
    );
    if (confirmMove) {
      window.location.href = `/running/running?sessionId=${currentSession.id}`;
      return;
    } else {
      return; // 취소하면 종료 안 함
    }
  }

  const confirmMessage = isAuto
    ? "목표 거리에 도달했습니다! 런닝을 종료하시겠습니까?"
    : "런닝을 종료하시겠습니까?";

  if (!confirm(confirmMessage)) {
    return;
  }

  // ✅ 러닝 종료 플래그 설정 (TTS 비활성화용)
  completedHandled = true;

  try {
    // 1. GPS 추적 중지
    if (runningTracker) {
      runningTracker.stopTracking();
      console.log("🛑 GPS 추적 완전히 중지됨");
      runningTracker = null;
    }

    // 2. GPS 구독 해제
    if (gpsSubscription) {
      gpsSubscription.unsubscribe();
      gpsSubscription = null;
      console.log("🛑 GPS 구독 해제됨");
    }

    // 3. API 호출 - 런닝 종료 (running_result 테이블에 저장)
    const response = await fetchWithAuth(
      `/api/running/sessions/${currentSession.id}/finish`,
      {
        method: "POST",
      }
    );

    if (!response.ok) {
      const error = await response.json();
      alert(error.message || "런닝 종료 실패");
      return;
    }

    // 4. 세션 상태 업데이트
    currentSession.status = "COMPLETED";
    updateControlBar();

    // 5. 테스트 패널 숨기기
    const testPanel = document.getElementById("running-test-panel");
    if (testPanel) {
      testPanel.style.display = "none";
    }

    // 6. 종료 시스템 메시지
    safeStompSend("/pub/chat/message", {
      sessionId: currentSession.id,
      senderId: null,
      senderName: "SYSTEM",
      content: "🏁 런닝이 종료되었습니다! 수고하셨습니다!",
      messageType: "SYSTEM",
    });

    // 7. 런닝 결과 모달 표시
    const result = await response.json();
    if (result.success) {
      showRunningResultModal();
    }
  } catch (error) {
    console.error("런닝 종료 에러:", error);
    alert("런닝 종료 중 오류가 발생했습니다.");
  }
}

// ============================================
// 런닝 결과 모달
// ============================================

/**
 * 런닝 결과 모달 표시
 */
function showRunningResultModal() {
  const modal = document.getElementById("running-result-modal-overlay");
  const segmentsDiv = document.getElementById("result-segments");
  const resultLoadingEl = document.getElementById("result-loading");
  const resultLoadingTextEl = document.getElementById("result-loading-text");

  // 먼저 모달을 띄우고 "처리중" 안내를 표시
  if (segmentsDiv) {
    segmentsDiv.innerHTML = "";
  }
  if (resultLoadingTextEl) {
    resultLoadingTextEl.textContent = "러닝 결과 저장중입니다…";
  }
  if (resultLoadingEl) {
    resultLoadingEl.classList.add("show");
  }
  if (modal) {
    modal.classList.add("show");
  }

  (async () => {
    let lastErr = null;

    for (let i = 0; i < 10; i++) {
      try {
        const response = await fetchWithAuth(
          `/api/running/sessions/${currentSession.id}/result`,
          { method: "GET" }
        );
        const result = await response.json().catch(() => null);

        if (!result?.success || !result?.data) {
          lastErr = new Error(result?.message || "러닝 결과 처리중");
          await new Promise((r) => setTimeout(r, 1200));
          continue;
        }

        const data = result.data;

        // 로딩 숨김
        if (resultLoadingEl) {
          resultLoadingEl.classList.remove("show");
        }

        // 총 거리
        document.getElementById("result-distance").textContent =
          data.totalDistance ? data.totalDistance.toFixed(2) : "0.00";

        // 소요 시간 (초 → 분:초)
        const totalMinutes = Math.floor(data.totalTime / 60);
        const totalSeconds = data.totalTime % 60;
        document.getElementById(
          "result-time"
        ).textContent = `${totalMinutes}:${String(totalSeconds).padStart(
          2,
          "0"
        )}`;

        // 평균 페이스
        if (data.avgPace) {
          const paceMin = Math.floor(data.avgPace);
          const paceSec = Math.round((data.avgPace - paceMin) * 60);
          document.getElementById(
            "result-pace"
          ).textContent = `${paceMin}:${String(paceSec).padStart(2, "0")}`;
        } else {
          document.getElementById("result-pace").textContent = "--:--";
        }

        // 구간별 페이스
        const segmentsDiv2 = document.getElementById("result-segments");
        if (segmentsDiv2) {
          segmentsDiv2.innerHTML = "";
        }

        if (segmentsDiv2) {
          if (data.splitPace && data.splitPace.length > 0) {
            data.splitPace.forEach((segment) => {
              const segmentDiv = document.createElement("div");
              segmentDiv.className = "segment-item";

              const kmLabel = document.createElement("span");
              kmLabel.className = "segment-km";
              kmLabel.textContent = `${segment.km}km`;

              const paceValue = document.createElement("span");
              paceValue.className = "segment-pace";
              const min = Math.floor(segment.pace);
              const sec = Math.round((segment.pace - min) * 60);
              paceValue.textContent = `${min}:${String(sec).padStart(
                2,
                "0"
              )}/km`;

              segmentDiv.appendChild(kmLabel);
              segmentDiv.appendChild(paceValue);
              segmentsDiv2.appendChild(segmentDiv);
            });
          } else {
            const emptyDiv = document.createElement("div");
            emptyDiv.style.cssText =
              "text-align: center; color: #9CA3AF; padding: 20px; font-size: 12px;";
            emptyDiv.textContent = "구간 데이터 없음";
            segmentsDiv2.appendChild(emptyDiv);
          }
        }

        return;
      } catch (e) {
        lastErr = e;
        await new Promise((r) => setTimeout(r, 1200));
      }
    }

    console.warn("런닝 결과 조회 실패:", lastErr?.message || lastErr);
    if (resultLoadingEl) {
      resultLoadingEl.classList.remove("show");
    }
    const segmentsDiv3 = document.getElementById("result-segments");
    if (segmentsDiv3) {
      segmentsDiv3.innerHTML =
        '<div style="text-align:center;color:#ef4444;padding:20px;font-size:12px;font-weight:900;">런닝 결과를 불러올 수 없습니다.</div>';
    }
  })();
}

/**
 * 런닝 결과 모달 닫기
 */
function closeRunningResultModal() {
  const modal = document.getElementById("running-result-modal-overlay");
  if (modal) {
    modal.classList.remove("show");
  }
}

// ============================================
// 런닝 에러 처리
// ============================================

let errorSubscription = null;

/**
 * 런닝 에러 메시지 처리
 */
function handleRunningError(error) {
  console.error("❌ 서버 에러 수신:", error);

  // 에러 메시지 표시
  let errorMessage = error.message || "알 수 없는 오류가 발생했습니다.";

  // 에러 코드에 따른 추가 처리
  switch (error.errorCode) {
    case "SESSION_NOT_FOUND":
      errorMessage += "\n세션을 찾을 수 없습니다. 페이지를 새로고침해주세요.";
      break;
    case "USER_NOT_FOUND":
      errorMessage += "\n사용자 정보를 찾을 수 없습니다.";
      break;
    case "INVALID_REQUEST":
      errorMessage += "\n잘못된 요청입니다.";
      break;
    case "INTERNAL_SERVER_ERROR":
      errorMessage += "\n서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
      break;
  }

  alert("⚠️ GPS 추적 오류\n\n" + errorMessage);

  // 심각한 에러인 경우 GPS 추적 중지
  if (
    error.errorCode === "SESSION_NOT_FOUND" ||
    error.errorCode === "INTERNAL_SERVER_ERROR"
  ) {
    if (runningTracker && runningTracker.isTracking) {
      console.log("🛑 심각한 에러로 인한 GPS 추적 중지");
      runningTracker.stopTracking();
      runningTracker = null;
    }
  }
}

/**
 * 런닝 에러 구독
 */
function subscribeToRunningErrors() {
  if (!stompClient || !stompClient.connected) {
    console.error("❌ WebSocket 연결 없음 (에러 구독)");
    return;
  }

  // 이미 구독 중이면 중복 구독 방지
  if (errorSubscription) {
    console.log("⚠️ 이미 런닝 에러를 구독 중입니다");
    return;
  }

  errorSubscription = stompClient.subscribe(
    `/sub/running/${currentSession.id}/errors`,
    function (message) {
      const error = JSON.parse(message.body);
      handleRunningError(error);
    }
  );

  console.log(
    "✅ 런닝 에러 구독 완료:",
    `/sub/running/${currentSession.id}/errors`
  );
}
