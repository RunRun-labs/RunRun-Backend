// 전역 변수 (이벤트 리스너에서 접근 가능하도록)
let selectedDistance = null;
let selectedParticipant = null;
let isMatching = false;
let userProfileUrl = null;
let matchFoundTimeout = null;
let countdownInterval = null;
let currentSessionId = null; // 현재 매칭된 세션 ID 저장
let fallbackCheckDone = false; // 폴백 체크 완료 플래그 (중복 처리 방지)
let fallbackCheckInterval = null; // 주기적 폴백 체크 interval (SSE 연결이 끊겼을 때만 실행)
let sseCheckInterval = null; // SSE 연결 상태 확인 interval
let matchStartTime = null; // 매칭 시작 시간 (오래된 알림 필터링용)
let matchFoundHandled = false;
let fallbackTimeoutId = null; // 폴백 폴링 setTimeout ID
let fallbackChecking = false; // 폴백 체크 중복 방지 락
let fallbackStartTime = null; // 폴백 시작 시각

// DOM 요소 참조 (나중에 초기화)
let statusTitle = null;
let statusSubtitle = null;
let matchFoundSection = null;
let countdownText = null;
let opponentProfiles = null;
let connectionLines = null;
let cancelButton = null;
let matchingOverlay = null;
let startButton = null;

// ✅ clearAllFallbackTimers 함수 선언문 (호이스팅 안전)
function clearAllFallbackTimers() {
  if (fallbackCheckInterval) {
    clearInterval(fallbackCheckInterval);
    fallbackCheckInterval = null;
  }
  if (fallbackTimeoutId) {
    clearTimeout(fallbackTimeoutId);
    fallbackTimeoutId = null;
  }
  fallbackStartTime = null;
}

// SSE 알림 수신 리스너 등록 (DOMContentLoaded 전에 등록)
window.addEventListener('notification-received', async (event) => {
  const notification = event.detail;

  console.log('[online-match] notification-received 이벤트 수신:', notification);
  console.log('[online-match] isMatching 상태:', isMatching);
  console.log('[online-match] notificationType:',
      notification.notificationType);
  console.log('[online-match] relatedType:', notification.relatedType);
  console.log('[online-match] relatedId:', notification.relatedId);

  const isMatchFound = notification.notificationType === 'MATCH_FOUND'
      && notification.relatedId;

  // MATCH_FOUND가 아니고 매칭 중이 아니면 무시
  if (!isMatching && !isMatchFound) {
    console.log('[online-match] ⚠️ 매칭 중이 아니고 MATCH_FOUND도 아님 - 알림 무시');
    return;
  }

  // MATCH_FOUND 알림 처리
  if (isMatchFound) {
    const sessionId = notification.relatedId;

    // ✅ MATCH_FOUND 들어가자마자 matchFoundHandled = true
    matchFoundHandled = true;

    // ✅ 폴백 타이머 정리
    clearAllFallbackTimers();

    // ✅ sseCheckInterval 정리
    if (sseCheckInterval) {
      clearInterval(sseCheckInterval);
      sseCheckInterval = null;
    }

    // matchFoundHandled 체크 개선: 세션 ID별로 체크
    const handledKey = `matchFound_${sessionId}`;
    if (sessionStorage.getItem(handledKey)) {
      console.log('[online-match] MATCH_FOUND already handled for sessionId:',
          sessionId);
      return;
    }
    sessionStorage.setItem(handledKey, 'true');
    // 30초 후 자동 삭제 (메모리 정리)
    setTimeout(() => sessionStorage.removeItem(handledKey), 30000);

    if (fallbackCheckInterval) {
      clearInterval(fallbackCheckInterval);
      fallbackCheckInterval = null;
    }

    console.log('[online-match] ✅ MATCH_FOUND 알림 처리 시작 - sessionId:',
        sessionId);

    fallbackCheckDone = true;

    // 매칭 오버레이가 표시되어 있지 않으면 매칭 오버레이를 표시하고 처리
    const shouldShowOverlay = !matchingOverlay || matchingOverlay.style.display
        === 'none';

    if (shouldShowOverlay) {
      if (matchingOverlay) {
        matchingOverlay.style.display = "flex";
      }
      if (startButton) {
        startButton.style.display = "none";
      }
      const header = document.querySelector(".header");
      if (header) {
        header.style.display = "none";
      }
      resetMatchUI();
    }

    isMatching = false;
    matchStartTime = null;

    try {
      const token = localStorage.getItem("accessToken");
      const headers = {
        "Content-Type": "application/json",
      };
      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }

      try {
        await fetch("/api/match/online/status", {
          method: "GET",
          headers: headers,
        });
        console.log('[online-match] Redis 키 정리 완료 (status API 호출)');
      } catch (statusError) {
        console.warn('[online-match] status API 호출 실패 (무시하고 계속):', statusError);
      }

      await showMatchFound(sessionId);
      // matchFoundHandled는 이미 위에서 true로 설정됨

      console.log('[online-match] ✅ MATCH_FOUND 알림 처리 완료');

      try {
        sessionStorage.removeItem('pendingMatchFound');
      } catch (e) {
      }
    } catch (error) {
      console.error('[online-match] ❌ 매칭 완료 처리 오류:', error);
      matchFoundHandled = false; // ✅ 에러 시 false로 되돌림
      // 에러 시 handledKey도 삭제하여 재시도 가능하게
      sessionStorage.removeItem(handledKey);
      if (matchingOverlay) {
        hideMatchingOverlay();
      }
      alert('매칭 완료 처리 중 오류가 발생했습니다.');
    }
    return;
  }
});

// BFCache(뒤로가기 캐시) 대응
window.addEventListener('pageshow', async (e) => {
  if (!e.persisted) {
    return;
  }

  console.log('[online-match] pageshow(bfcache restore) - 상태 리셋');

  isMatching = false;
  currentSessionId = null;
  fallbackCheckDone = false;
  matchStartTime = null;
  matchFoundHandled = false;

  // ✅ pageshow에서도 타이머 정리
  if (sseCheckInterval) {
    clearInterval(sseCheckInterval);
    sseCheckInterval = null;
  }
  clearAllFallbackTimers();

  try {
    resetMatchUI();
    updateStartButton();
  } catch (err) {
    console.warn('[online-match] pageshow UI 리셋 중 오류:', err);
  }

  try {
    if (typeof window.ensureSseConnected === 'function') {
      await window.ensureSseConnected();
    }
  } catch (err) {
    console.error('[online-match] pageshow SSE 재연결 실패:', err);
  }
});

function updateStartButton() {
  const selectionSummary = document.getElementById("selectionSummary");
  const selectedDistanceEl = document.getElementById("selectedDistance");
  const selectedParticipantEl = document.getElementById("selectedParticipant");
  const matchContent = document.querySelector(".match-content");

  if (startButton && selectedDistance && selectedParticipant) {
    startButton.disabled = false;
    startButton.style.display = "block";

    if (matchContent) {
      matchContent.classList.add("selection-complete");
    }

    if (selectionSummary) {
      const distanceMap = {"KM_3": "3km", "KM_5": "5km", "KM_10": "10km"};
      if (selectedDistanceEl) {
        selectedDistanceEl.textContent = distanceMap[selectedDistance]
            || selectedDistance;
      }
      if (selectedParticipantEl) {
        selectedParticipantEl.textContent = `${selectedParticipant}명`;
      }

      setTimeout(() => {
        selectionSummary.style.display = "flex";
        requestAnimationFrame(() => selectionSummary.classList.add("show"));
      }, 300);

      fetchUserDistanceRating(selectedDistance);
    }
  } else if (startButton) {
    startButton.disabled = true;
    startButton.style.display = "none";
    if (matchContent) {
      matchContent.classList.remove("selection-complete");
    }
    if (selectionSummary) {
      selectionSummary.classList.remove("show");
      setTimeout(() => {
        selectionSummary.style.display = "none";
      }, 300);
    }
  }
}

async function handleCancel() {
  if (!isMatching) {
    return;
  }

  try {
    const token = localStorage.getItem("accessToken");
    const headers = {"Content-Type": "application/json"};
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    const response = await fetch("/api/match/online/join", {
      method: "DELETE",
      headers: headers,
    });

    isMatching = false;
    matchStartTime = null;

    // ✅ cancel 시에도 타이머 정리
    if (sseCheckInterval) {
      clearInterval(sseCheckInterval);
      sseCheckInterval = null;
    }
    clearAllFallbackTimers();

    hideMatchingOverlay();
    window.location.href = "/match/online";
  } catch (error) {
    console.error("매칭 취소 오류:", error);
    isMatching = false;
    // ✅ 에러 시에도 타이머 정리
    if (sseCheckInterval) {
      clearInterval(sseCheckInterval);
      sseCheckInterval = null;
    }
    clearAllFallbackTimers();
    hideMatchingOverlay();
    window.location.href = "/match/online";
  }
}

document.addEventListener("DOMContentLoaded", () => {
  // 복구 로직
  try {
    const pending = sessionStorage.getItem('pendingMatchFound');
    if (pending) {
      const stored = JSON.parse(pending);
      const timeSinceStored = Date.now() - (stored.timestamp || 0);
      if (timeSinceStored < 30000 && stored.relatedId) {
        sessionStorage.removeItem('pendingMatchFound');
        window.dispatchEvent(
            new CustomEvent('notification-received', {detail: stored}));
      } else {
        sessionStorage.removeItem('pendingMatchFound');
      }
    }
  } catch (e) {
  }

  const backButton = document.querySelector(".back-button");
  const optionButtons = document.querySelectorAll(".option-card");
  startButton = document.getElementById("startButton");
  matchingOverlay = document.getElementById("matchingOverlay");
  cancelButton = document.getElementById("cancelButton");
  const userProfileImage = document.getElementById("userProfileImage");
  statusTitle = document.getElementById("statusTitle");
  statusSubtitle = document.getElementById("statusSubtitle");
  opponentProfiles = document.getElementById("opponentProfiles");
  connectionLines = document.getElementById("connectionLines");
  matchFoundSection = document.getElementById("matchFoundSection");
  countdownText = document.getElementById("countdownText");

  if (startButton) {
    startButton.style.display = "none";
  }

  if (backButton) {
    backButton.addEventListener("click", () => {
      if (isMatching) {
        handleCancel();
      } else {
        window.history.length > 1 ? window.history.back()
            : (window.location.href = "/match");
      }
    });
  }

  optionButtons.forEach((button) => {
    button.addEventListener("click", () => {
      const type = button.getAttribute("data-type");
      const value = button.getAttribute("data-value");

      if (type === "distance" && selectedDistance === value
          && button.classList.contains("active")) {
        button.classList.remove("active");
        selectedDistance = null;
        document.querySelector(".match-content")?.classList.remove(
            "has-distance-selected");
        const pGroup = document.querySelector(
            '.setting-group[data-type="participant"]');
        if (pGroup) {
          pGroup.classList.remove("show");
        }
        selectedParticipant = null;
        document.querySelectorAll(
            '.option-card[data-type="participant"]').forEach(
            btn => btn.classList.remove("active"));
        updateStartButton();
        return;
      }

      document.querySelectorAll(`.option-card[data-type="${type}"]`).forEach(
          (btn) => btn.classList.remove("active"));
      button.classList.add("active");

      if (type === "distance") {
        selectedDistance = value;
        document.querySelector(".match-content")?.classList.add(
            "has-distance-selected");
        const pGroup = document.querySelector(
            '.setting-group[data-type="participant"]');
        if (pGroup) {
          setTimeout(() => {
            pGroup.classList.add("show");
            // scrollIntoView 제거 - 위쪽이 잘리지 않도록
            // setTimeout(() => {
            //   pGroup.scrollIntoView({behavior: 'smooth', block: 'center'});
            // }, 400);
          }, 100);
        }
      } else if (type === "participant") {
        selectedParticipant = parseInt(value, 10);
      }
      updateStartButton();
    });
  });

  if (startButton) {
    startButton.addEventListener("click", async () => {
      if (!startButton.disabled && !isMatching) {
        await handleMatchStart();
      }
    });
  }

  if (cancelButton) {
    cancelButton.addEventListener("click", handleCancel);
  }

  // Auto Match 처리
  const urlParams = new URLSearchParams(window.location.search);
  const autoMatchSessionId = urlParams.get('autoMatch');
  if (autoMatchSessionId) {
    isMatching = true;
    if (matchingOverlay) {
      matchingOverlay.style.display = "flex";
    }
    if (startButton) {
      startButton.style.display = "none";
    }
    resetMatchUI();
    setTimeout(async () => {
      await showMatchFound(autoMatchSessionId);
      window.history.replaceState({}, '', window.location.pathname);
    }, 300);
  }

  loadUserProfile();
});

async function loadUserProfile() {
  try {
    const token = localStorage.getItem("accessToken");
    const headers = {"Content-Type": "application/json"};
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    const response = await fetch("/users", {method: "GET", headers});
    const result = await response.json();
    const userProfileImage = document.getElementById("userProfileImage");

    if (response.ok && result?.success && result?.data && userProfileImage) {
      userProfileUrl = result.data.profileImageUrl;
      userProfileImage.src = userProfileUrl || "/img/default-profile.svg";
      userProfileImage.onerror = function () {
        this.src = "/img/default-profile.svg";
      };
    }
  } catch (error) {
    console.error("프로필 로드 오류:", error);
  }
}

async function handleMatchStart() {
  // ✅ 시작 시 기존 타이머 정리
  clearAllFallbackTimers();

  matchFoundHandled = false;
  fallbackCheckDone = false;
  matchStartTime = Date.now();
  isMatching = true;
  fallbackChecking = false;
  fallbackStartTime = null;

  if (startButton) {
    startButton.disabled = true;
  }

  try {
    if (typeof window.ensureSseConnected
        === 'function') {
      await window.ensureSseConnected();
    }

    const token = localStorage.getItem("accessToken");
    const headers = {"Content-Type": "application/json"};
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    const response = await fetch("/api/match/online/join", {
      method: "POST",
      headers: headers,
      body: JSON.stringify(
          {distance: selectedDistance, participantCount: selectedParticipant}),
    });

    const result = await response.json();
    if (!response.ok || !result?.success) {
      throw new Error(
          result?.message || "매칭 신청 실패");
    }

    if (result?.data) {
      const sessionId = result.data;
      matchingOverlay.style.display = "flex";
      if (startButton) {
        startButton.style.display = "none";
      }
      resetMatchUI();
      if (statusTitle) {
        statusTitle.textContent = "참여 중인 매칭이 존재합니다";
      }
      if (statusSubtitle) {
        statusSubtitle.textContent = "기존 배틀방으로 이동합니다...";
        statusSubtitle.style.display = "block";
      }
      setTimeout(() => {
        window.location.href = `/match/waiting?sessionId=${sessionId}`;
      }, 2000);
      return;
    }

    matchingOverlay.style.display = "flex";
    if (startButton) {
      startButton.style.display = "none";
    }
    // ✅ 파동 애니메이션 표시 시 상단바 숨김
    const header = document.querySelector(".header");
    if (header) {
      header.style.display = "none";
    }
    resetMatchUI();

    // ✅ 폴백 체크 함수 (락 추가)
    const performFallbackCheck = async () => {
      if (!isMatching || fallbackCheckDone || matchFoundHandled) {
        return;
      }
      if (fallbackChecking) {
        return; // ✅ 중복 실행 방지
      }

      fallbackChecking = true;
      try {
        const statusResponse = await fetch("/api/match/online/status",
            {method: "GET", headers});
        if (statusResponse.ok) {
          const sResult = await statusResponse.json();
          if (sResult?.success && sResult?.data?.status === "MATCHED"
              && sResult?.data?.sessionId) {
            fallbackCheckDone = true;
            isMatching = false;
            // ✅ 폴백 성공 시 sseCheckInterval도 정리
            if (sseCheckInterval) {
              clearInterval(sseCheckInterval);
              sseCheckInterval = null;
            }
            clearAllFallbackTimers();
            await showMatchFound(sResult.data.sessionId);
          }
        }
      } catch (e) {
        console.debug('[online-match] 폴백 체크 에러:', e);
      } finally {
        fallbackChecking = false; // ✅ 락 해제
      }
    };

    // SSE 연결 상태 확인 (SSE 끊김 감지용, 폴백 시작은 별도)
    sseCheckInterval = setInterval(() => {
      const isConnected = typeof window.isSseConnected === 'function'
          && window.isSseConnected();
      if (!isConnected) {
        console.debug('[online-match] SSE 연결 끊김 감지');
      }
    }, 1000);

    // ✅ scheduleNextFallback 함수 (null 체크 및 60초 종료 시 정리)
    const scheduleNextFallback = () => {
      if (!isMatching || fallbackCheckDone || matchFoundHandled) {
        return;
      }
      if (!fallbackStartTime) {
        fallbackStartTime = Date.now(); // ✅ 안전장치
      }

      const elapsed = Date.now() - fallbackStartTime;

      // ✅ 60초 넘으면 중지 및 타이머 정리
      if (elapsed >= 60000) {
        console.log('[online-match] 폴백 폴링 60초 경과 - 중지');
        // ✅ 60초 종료 시 sseCheckInterval도 정리
        if (sseCheckInterval) {
          clearInterval(sseCheckInterval);
          sseCheckInterval = null;
        }
        clearAllFallbackTimers();
        return;
      }

      // ✅ 주기 결정
      const nextInterval = elapsed < 20000 ? 3000 : 10000;

      // ✅ 다음 폴백 체크 스케줄
      fallbackTimeoutId = setTimeout(async () => {
        await performFallbackCheck();
        scheduleNextFallback();
      }, nextInterval);
    };

    // ✅ 7초 후 첫 폴백 체크 시작 (fallbackStartTime은 여기서 세팅)
    fallbackTimeoutId = setTimeout(() => {
      if (isMatching && !fallbackCheckDone && !matchFoundHandled) {
        fallbackStartTime = Date.now(); // ✅ 폴백 시작 시각에 세팅
        performFallbackCheck();
        scheduleNextFallback();
      }
    }, 7000);

  } catch (error) {
    alert(error.message);
    hideMatchingOverlay();
    isMatching = false;
  }
}

// ✅ resetMatchUI()에서 타이머 정리 제거 (UI만 담당)
function resetMatchUI() {
  if (statusTitle) {
    statusTitle.textContent = "SEARCHING FOR PLAYERS...";
  }
  if (statusSubtitle) {
    statusSubtitle.textContent = "Finding users with similar skill and latency.";
  }
  if (matchFoundSection) {
    matchFoundSection.style.display = "none";
  }
  if (countdownText) {
    countdownText.textContent = "";
  }
  if (opponentProfiles) {
    opponentProfiles.innerHTML = "";
  }
  if (connectionLines) {
    connectionLines.style.display = "none";
  }
  if (cancelButton) {
    cancelButton.style.display = "block";
  }

  // ✅ 타이머 정리는 여기서 하지 않음 (UI만 담당)
  fallbackStartTime = null;
  fallbackChecking = false;
}

async function fetchMatchInfo(sessionId) {
  const token = localStorage.getItem("accessToken");
  const headers = {"Content-Type": "application/json"};
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(`/api/match/session/${sessionId}`,
      {method: "GET", headers});
  const result = await response.json();
  return result?.data;
}

async function showMatchFound(sessionId) {
  if (countdownInterval) {
    clearInterval(countdownInterval);
  }

  let matchData;
  try {
    matchData = await fetchMatchInfo(sessionId);
  } catch (e) {
    matchData = {sessionId, participants: []};
  }

  const finalSessionId = matchData?.sessionId || sessionId;
  currentSessionId = finalSessionId;

  // ✅ 매칭 잡히면 바로 TTS batch 준비
  if (window.TtsManager) {
    window.TtsManager.ensureLoaded(
        {sessionId: finalSessionId, mode: "ONLINE"}).catch(() => {
      console.warn("온라인 런 TTS 로드 실패 (무시)");
    });
  }

  if (statusTitle) {
    statusTitle.textContent = "";
  }
  if (statusSubtitle) {
    statusSubtitle.style.display = "none";
  }
  if (matchFoundSection) {
    matchFoundSection.style.display = "block";
  }
  if (cancelButton) {
    cancelButton.style.display = "none";
  }

  const participants = matchData?.participants || [];
  const opponentCount = Math.max(1,
      participants.length > 0 ? participants.length - 1 : (selectedParticipant
          || 2) - 1);

  if (opponentProfiles) {
    opponentProfiles.innerHTML = "";
    const angleStep = (360 / opponentCount) * (Math.PI / 180);
    const startAngle = -90 * (Math.PI / 180);
    const radius = 255;
    const containerSize = 300;
    const currentUserId = localStorage.getItem("userId") ? parseInt(
        localStorage.getItem("userId")) : null;

    let opponentIndex = 0;
    participants.forEach((p, i) => {
      if (currentUserId && p.userId === currentUserId) {
        document.getElementById("userTier").textContent = p.tier || "토끼";
        return;
      }

      const angle = startAngle + angleStep * opponentIndex;
      const x = 50 + (radius / containerSize) * 50 * Math.cos(angle);
      const y = 50 + (radius / containerSize) * 50 * Math.sin(angle);

      const div = document.createElement("div");
      div.className = "opponent-profile";
      div.style.left = `${x}%`;
      div.style.top = `${y}%`;
      div.innerHTML = `<img src="${p.profileImage || '/img/default-profile.svg'}"
      alt="상대방" onerror="this.src='/img/default-profile.svg'"><div class="opponent-label"><span class="opponent-name">${p.name
      || 'Player'}</span><span class="opponent-tier">${p.tier
      || '토끼'}</span></div>`;
      opponentProfiles.appendChild(div);
      opponentIndex++;
    });
  }

  if (connectionLines) {
    connectionLines.style.display = "block";
    connectionLines.innerHTML = "";
    const angleStep = (360 / opponentCount) * (Math.PI / 180);
    for (let i = 0; i < opponentCount; i++) {
      const angle = (-90 * (Math.PI / 180)) + angleStep * i;
      const x2 = 50 + (255 / 300) * 50 * Math.cos(angle);
      const y2 = 50 + (255 / 300) * 50 * Math.sin(angle);
      const line = document.createElementNS("http://www.w3.org/2000/svg",
          "line");
      line.setAttribute("class", "connection-line");
      line.setAttribute("x1", "50%");
      line.setAttribute("y1", "50%");
      line.setAttribute("x2", `${x2}%`);
      line.setAttribute("y2", `${y2}%`);
      connectionLines.appendChild(line);
    }
  }

  setTimeout(startProfilePullAnimation, 1000);

  let countdown = 10;
  if (countdownText) {
    countdownText.textContent = countdown;
  }

  countdownInterval = setInterval(() => {
    countdown--;
    if (countdownText) {
      countdownText.textContent = countdown > 0 ? countdown
          : "";
    }
    if (countdown <= 0) {
      clearInterval(countdownInterval);
      if (currentSessionId) {
        window.location.href = `/match/waiting?sessionId=${currentSessionId}`;
      }
    }
  }, 1000);
}

function startProfilePullAnimation() {
  document.querySelectorAll(".opponent-profile").forEach((profile, index) => {
    setTimeout(() => {
      const initialX = parseFloat(profile.style.left);
      const initialY = parseFloat(profile.style.top);
      profile.style.transition = "transform 1.5s ease-in, opacity 1.5s ease-in";
      profile.style.transform = `translate(calc(50% - ${initialX}%), calc(50% - ${initialY}%)) scale(0.2)`;
      profile.style.opacity = "0";
    }, index * 100);
  });
}

function hideMatchingOverlay() {
  if (matchingOverlay) {
    matchingOverlay.style.display = "none";
  }
  if (startButton) {
    startButton.style.display = "block";
  }
  const header = document.querySelector(".header");
  if (header) {
    header.style.display = "flex";
  }
  isMatching = false;
  matchStartTime = null;
  // ✅ hideMatchingOverlay에서도 타이머 정리
  if (sseCheckInterval) {
    clearInterval(sseCheckInterval);
    sseCheckInterval = null;
  }
  clearAllFallbackTimers();
  resetMatchUI();
}

async function fetchUserDistanceRating(distanceType) {
  try {
    const token = localStorage.getItem("accessToken");
    if (!token) {
      setDefaultRatingValues();
      return;
    }

    const response = await fetch(
        `/api/rating/distance?distanceType=${distanceType}`, {
          headers: {"Authorization": `Bearer ${token}`}
        });

    if (response.ok) {
      const result = await response.json();
      document.getElementById(
          "selectedRating").textContent = `${result?.data?.currentRating
      || 1000} LP`;
      document.getElementById(
          "selectedTier").textContent = result?.data?.currentTier || "거북이";
    } else {
      setDefaultRatingValues();
    }
  } catch (e) {
    setDefaultRatingValues();
  }
}

function setDefaultRatingValues() {
  const rEl = document.getElementById("selectedRating");
  const tEl = document.getElementById("selectedTier");
  if (rEl) {
    rEl.textContent = "1000 LP";
  }
  if (tEl) {
    tEl.textContent = "거북이";
  }
}