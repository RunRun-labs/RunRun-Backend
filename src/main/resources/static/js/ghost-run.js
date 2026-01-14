/**
 * Ghost Run - 실시간 고스트런
 * WebSocket + GPS 추적 + 실시간 비교
 */

// 전역 변수
let stompClient = null;
let isConnected = false;
let SESSION_ID = null;
let ghostData = null;
let myUserId = null;  // 추가!

// ✅ 재연결 관리
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;
let reconnectTimeout = null;

// 마지막 비교 결과 (종료 시 사용)
let lastComparison = {
  status: 'EVEN',
  timeDiffSeconds: 0
};

// ==========================
// TTS hooks
// ==========================
let ttsReady = false;
let ghostStatusState = null; // "AHEAD" | "BEHIND" | null
let ghostStatusChangeTime = null;
let ghostStatusTimer = null;
let completedHandled = false; // 종료 TTS 후 TTS 중단 플래그
let lastKmSpoken = 0; // 마지막으로 재생한 km (DIST_DONE용)

// GPS 추적
let watchId = null;
let lastPosition = null;
let totalDistance = 0; // 미터 단위
let startTime = null;
let elapsedSeconds = 0;
let isRunning = false;
let isPaused = false;
let isFinished = false;

// 속도 제한(경고용)
let tooFastHardMps = 8.5; // 하드: 8.5m/s(30.6km/h) 이상은 거의 GPS 점프/차량 → 즉시 경고
let tooFastSoftMps = 6.0; // 소프트: 6.0m/s(21.6km/h) 이상이 3회 연속이면 경고
let tooFastSoftCount = 0;
let tooFastAlertCooldownMs = 15000;
let lastTooFastAlertAt = 0;

// 타이머
let elapsedTimerInterval = null;

// DOM 요소
const statusBadge = document.getElementById('statusBadge');
const statusText = statusBadge.querySelector('.status-text');
const comparisonStatus = document.getElementById('comparisonStatus');
const comparisonDistance = document.getElementById('comparisonDistance');
const currentDistanceEl = document.getElementById('currentDistance');
const elapsedTimeEl = document.getElementById('elapsedTime');
const currentPaceEl = document.getElementById('currentPace');
const ghostDateEl = document.getElementById('ghostDate');
const ghostTimeEl = document.getElementById('ghostTime');
const ghostPaceEl = document.getElementById('ghostPace');

const startButton = document.getElementById('startButton');
const pauseButton = document.getElementById('pauseButton');
const resumeButton = document.getElementById('resumeButton');
const quitButton = document.getElementById('quitButton');

// localStorage에서 userId 가져오기 (배틀과 동일)
const storedUserId = localStorage.getItem('userId');
if (storedUserId) {
  myUserId = parseInt(storedUserId);
  console.log('👤 현재 사용자 ID:', myUserId);
}

// 페이지 로드 시 초기화
document.addEventListener("DOMContentLoaded", () => {
  console.log("👻 고스트런 페이지 초기화");
  
  // URL에서 sessionId 가져오기
  const urlParams = new URLSearchParams(window.location.search);
  SESSION_ID = parseInt(urlParams.get('sessionId'));
  
  if (!SESSION_ID) {
    console.error('❌ SESSION_ID가 없습니다!');
    alert('잘못된 접근입니다.');
    window.location.href = '/match/ghost';
    return;
  }
  
  console.log('📍 Session ID:', SESSION_ID);
  
  // 초기화
  init();
});

/**
 * 초기화
 */
function init() {
  setupEventListeners();
  loadGhostData();
  connectWebSocket();
  
  // 초기 대기 상태 설정
  setWaitingState();
  
  // TTS 미리 로드
  ensureTtsOnce().catch(() => {
    console.warn("TTS 로드 실패 (무시)");
  });
}

/**
 * TTS 초기화
 */
async function ensureTtsOnce() {
  if (ttsReady) return true;
  if (!window.TtsManager) return false;
  try {
    await window.TtsManager.ensureLoaded({ sessionId: SESSION_ID, mode: "GHOST" });
    ttsReady = true;
    return true;
  } catch (e) {
    console.warn("TTS 로드 실패(무시):", e?.message || e);
    return false;
  }
}

/**
 * 대기 상태 설정
 */
function setWaitingState() {
  comparisonStatus.textContent = '👻 고스트 대기중...';
  comparisonDistance.textContent = '';
  comparisonDistance.className = 'comparison-distance';
}

/**
 * 시작 상태 설정
 */
function setStartingState() {
  comparisonStatus.textContent = '👻 고스트 출발! 🏁';
  comparisonDistance.textContent = '0m';
  comparisonDistance.className = 'comparison-distance';
  
  // 애니메이션 효과
  comparisonStatus.classList.add('starting');
  setTimeout(() => {
    comparisonStatus.classList.remove('starting');
  }, 500);
}

/**
 * 이벤트 리스너 설정
 */
function setupEventListeners() {
  startButton.addEventListener('click', handleStart);
  pauseButton.addEventListener('click', handlePause);
  resumeButton.addEventListener('click', handleResume);
  quitButton.addEventListener('click', handleQuit);
}

/**
 * 고스트 기록 데이터 로드
 */
async function loadGhostData() {
  const token = getToken();
  
  try {
    // 고스트 세션 정보 조회 API 호출
    const response = await fetch(`/api/match/ghost/session/${SESSION_ID}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (!response.ok) throw new Error('세션 조회 실패');
    
    const result = await response.json();
    ghostData = result.data;
    
    console.log('✅ 고스트 데이터 로드:', ghostData);
    
    // 고스트 정보 표시
    if (ghostData.ghostRecord) {
      const record = ghostData.ghostRecord;
      ghostDateEl.textContent = formatDate(record.startedAt);
      ghostTimeEl.textContent = formatTime(record.totalTime);
      ghostPaceEl.textContent = formatPace(record.avgPace);
      
      console.log('👻 고스트 정보 표시 완료:', {
        date: formatDate(record.startedAt),
        time: formatTime(record.totalTime),
        pace: formatPace(record.avgPace),
        splitPace: record.splitPace ? `${record.splitPace.length}구간` : '없음'
      });
    }
    
  } catch (error) {
    console.error('❌ 고스트 데이터 로드 실패:', error);
    alert('고스트 데이터를 불러올 수 없습니다.');
  }
}

/**
 * WebSocket 연결
 */
function connectWebSocket() {
  const socket = new SockJS('/ws');
  stompClient = Stomp.over(socket);
  
  // 디버그 모드 끄기
  stompClient.debug = null;
  
  stompClient.connect({}, onConnected, onError);
}

/**
 * WebSocket 연결 성공
 */
function onConnected() {
  console.log('✅ WebSocket 연결 성공');
  isConnected = true;
  
  // ✅ 재연결 성공 메시지 (초기화 전에 체크)
  const wasReconnecting = reconnectAttempts > 0;
  
  // ✅ 재연결 카운터 초기화
  reconnectAttempts = 0;
  if (reconnectTimeout) {
    clearTimeout(reconnectTimeout);
    reconnectTimeout = null;
  }
  
  if (wasReconnecting) {
    showToast('✅ 연결 복구 성공!', 'success');
  }
  
  // 고스트런 비교 결과 구독
  stompClient.subscribe(`/sub/ghost-run/${SESSION_ID}`, onGhostComparison);
  console.log(`✅ 구독: /sub/ghost-run/${SESSION_ID}`);
  
  // 에러 메시지 구독
  stompClient.subscribe(`/sub/ghost-run/${SESSION_ID}/error`, onError);
  console.log(`✅ 구독: /sub/ghost-run/${SESSION_ID}/error`);
  
  // 완료 메시지 구독
  stompClient.subscribe(`/sub/ghost-run/${SESSION_ID}/complete`, onComplete);
  console.log(`✅ 구독: /sub/ghost-run/${SESSION_ID}/complete`);
  
  console.log('✅ 고스트런 구독 완료');
}

/**
 * WebSocket 에러
 */
function onError(error) {
  console.error('❌ WebSocket 에러:', error);
  isConnected = false;
  
  if (error.body) {
    const errorData = JSON.parse(error.body);
    console.error('에러 메시지:', errorData.error);
  }
  
  // ✅ 재연결 시도
  attemptReconnect();
}

/**
 * 고스트 비교 결과 수신
 */
function onGhostComparison(message) {
  const comparison = JSON.parse(message.body);
  console.log('📊 고스트 비교:', comparison);
  
  // 마지막 비교 결과 저장 (종료 시 사용)
  lastComparison = {
    status: comparison.status || 'EVEN',
    timeDiffSeconds: comparison.timeDiffSeconds || 0
  };
  
  updateComparisonUI(comparison);
}

/**
 * 비교 UI 업데이트
 */
function updateComparisonUI(comparison) {
  // 시작 전이면 대기 메시지 유지
  if (!isRunning) {
    setWaitingState();
    return;
  }
  
  const { status, distanceDiffMeters, timeDiffSeconds, compareMethod } = comparison;
  
  console.log('📊 비교 결과:', {
    status,
    distance: `${distanceDiffMeters}m`,
    time: `${timeDiffSeconds}s`,
    method: compareMethod === 'KM_BASED' ? '정밀비교' : '평균페이스'
  });
  
  // 시작 직후 (0초, 0m)
  if (status === 'EVEN' && distanceDiffMeters === 0) {
    comparisonStatus.textContent = '👻 고스트 출발! 🏁';
    comparisonDistance.textContent = '0m';
    comparisonDistance.className = 'comparison-distance';
    ghostStatusState = null;
    if (ghostStatusTimer) {
      clearTimeout(ghostStatusTimer);
      ghostStatusTimer = null;
    }
  }
  // 앞섬
  else if (status === 'AHEAD') {
    comparisonStatus.textContent = '고스트보다 앞서고 있어요! 🔥';
    comparisonDistance.textContent = `+${distanceDiffMeters}m`;
    comparisonDistance.className = 'comparison-distance ahead';
    
    // 상태 변경 감지 및 TTS 처리
    if (ghostStatusState !== 'AHEAD') {
      // 상태가 변경됨 (BEHIND → AHEAD 또는 null → AHEAD)
      if (ghostStatusState === 'BEHIND') {
        // 뒤→앞으로 변경: 5초 타이머 시작
        if (ghostStatusTimer) {
          clearTimeout(ghostStatusTimer);
        }
        ghostStatusChangeTime = Date.now();
        ghostStatusTimer = setTimeout(() => {
          // 5초 후에도 AHEAD 상태이면 TTS 재생
          if (ghostStatusState === 'AHEAD' && ttsReady && window.TtsManager && !completedHandled) {
            window.TtsManager.speak("GHOST_AHEAD");
          }
        }, 5000);
      }
      ghostStatusState = 'AHEAD';
    }
  }
  // 뒤처짐
  else if (status === 'BEHIND') {
    comparisonStatus.textContent = '고스트를 따라잡아요! 💪';
    comparisonDistance.textContent = `-${distanceDiffMeters}m`;
    comparisonDistance.className = 'comparison-distance behind';
    
    // 상태 변경 감지 및 TTS 처리
    if (ghostStatusState !== 'BEHIND') {
      // 상태가 변경됨 (AHEAD → BEHIND 또는 null → BEHIND)
      if (ghostStatusState === 'AHEAD') {
        // 앞→뒤로 변경: 5초 타이머 시작
        if (ghostStatusTimer) {
          clearTimeout(ghostStatusTimer);
        }
        ghostStatusChangeTime = Date.now();
        ghostStatusTimer = setTimeout(() => {
          // 5초 후에도 BEHIND 상태이면 TTS 재생
          if (ghostStatusState === 'BEHIND' && ttsReady && window.TtsManager && !completedHandled) {
            window.TtsManager.speak("GHOST_BEHIND");
          }
        }, 5000);
      }
      ghostStatusState = 'BEHIND';
    }
  }
  // 동률 (1초 이후)
  else {
    comparisonStatus.textContent = '고스트 기록과 동률! ⚡';
    comparisonDistance.textContent = '';
    comparisonDistance.className = 'comparison-distance';
    ghostStatusState = null;
    if (ghostStatusTimer) {
      clearTimeout(ghostStatusTimer);
      ghostStatusTimer = null;
    }
  }
}

/**
 * 완료 메시지 수신
 */
function onComplete(message) {
  console.log('🏁 고스트런 완료');
  stopRunning();
  
  // 결과 페이지로 이동 (TODO: 결과 페이지 구현)
  setTimeout(() => {
    alert('고스트런을 완주했습니다!');
    window.location.href = '/match/select';
  }, 1000);
}

/**
 * 시작 버튼
 */
function handleStart() {
  console.log('▶️ 고스트런 시작');
  
  startGPSTracking();
  startRunning();
  
  // 버튼 변경
  startButton.style.display = 'none';
  pauseButton.style.display = 'flex';
  
  // 상태 변경
  statusBadge.classList.add('running');
  statusText.textContent = '러닝 중';
  
  // 시작 메시지 표시
  setStartingState();
  
  // START_RUN TTS
  if (ttsReady && window.TtsManager) {
    window.TtsManager.speak("START_RUN", { priority: 2, cooldownMs: 0 });
  }
}

/**
 * 일시정지 버튼
 */
function handlePause() {
  console.log('⏸ 일시정지');
  
  isPaused = true;
  stopGPSTracking();
  
  // 타이머 정지
  if (elapsedTimerInterval) {
    clearInterval(elapsedTimerInterval);
    elapsedTimerInterval = null;
  }
  
  // 버튼 변경
  pauseButton.style.display = 'none';
  resumeButton.style.display = 'flex';
  
  // 상태 변경
  statusBadge.classList.remove('running');
  statusBadge.classList.add('paused');
  statusText.textContent = '일시정지';
}

/**
 * 재개 버튼
 */
function handleResume() {
  console.log('▶️ 재개');
  
  isPaused = false;
  startGPSTracking();
  
  // 타이머 재시작
  startElapsedTimer();
  
  // 버튼 변경
  resumeButton.style.display = 'none';
  pauseButton.style.display = 'flex';
  
  // 상태 변경
  statusBadge.classList.remove('paused');
  statusBadge.classList.add('running');
  statusText.textContent = '러닝 중';
}

/**
 * 자동 종료 (목표 거리 도달 시)
 */
function autoFinish() {
  console.log('🏁 목표 거리 도달 - 자동 종료');
  
  // 러닝 종료 처리
  isFinished = true;
  
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
  
  console.log('✅ 완료 요청');
  
  // 러닝 결과 데이터 계산
  const totalDistanceKm = totalDistance / 1000;  // km
  const avgPaceMinPerKm = elapsedSeconds / 60 / totalDistanceKm;  // 분/km
  
  // userId 확인 (배틀과 동일)
  if (!myUserId) {
    alert('오류: 사용자 정보를 찾을 수 없습니다. 다시 로그인해주세요.');
    window.location.href = '/login';
    return;
  }
  
  const finishData = {
    userId: myUserId,  // 배틀과 동일!
    totalDistance: parseFloat(totalDistanceKm.toFixed(2)),
    totalTime: elapsedSeconds,
    avgPace: parseFloat(avgPaceMinPerKm.toFixed(2))
  };
  
  console.log('🏁 완료 데이터:', finishData);
  console.log('📡 WebSocket 연결 상태:', isConnected);
  console.log('📡 STOMP 클라이언트:', stompClient ? '존재' : '없음');
  console.log('📡 전송 경로:', `/pub/ghost-run/${SESSION_ID}/finish`);
  
  // WebSocket으로 완료 알림 (러닝 결과 포함)
  if (isConnected && stompClient) {
    try {
      stompClient.send(
        `/pub/ghost-run/${SESSION_ID}/finish`,
        {},
        JSON.stringify(finishData)
      );
      console.log('✅ 완료 데이터 전송 성공');
      console.log('📤 전송한 데이터:', JSON.stringify(finishData));
    } catch (error) {
      console.error('❌ 전송 실패:', error);
      alert('오류: 데이터 전송 실패!');
      return;
    }
  } else {
    console.error('❌ WebSocket 연결 안 됨!');
    alert('오류: WebSocket 연결이 끊기셨습니다!');
    return;
  }
  
  stopRunning();
  
  // 내 기록을 localStorage에 저장 (결과 페이지에서 사용)
  localStorage.setItem('ghost_my_distance', totalDistanceKm.toFixed(2));
  localStorage.setItem('ghost_my_time', elapsedSeconds.toString());
  localStorage.setItem('ghost_time_diff', lastComparison.timeDiffSeconds.toString());
  localStorage.setItem('ghost_status', lastComparison.status);
  
  // 결과 페이지로 이동
  setTimeout(() => {
    window.location.href = `/match/ghost-result?sessionId=${SESSION_ID}`;
  }, 500);
}

/**
 * 포기 버튼
 */
async function handleQuit() {
  if (!confirm('정말로 포기하시겠습니까?')) return;
  
  console.log('❌ 포기');
  
  isRunning = false;
  stopRunning();
  
  // 세션 종료 API 호출
  const token = getToken();
  try {
    await fetch(`/api/ghost-run/${SESSION_ID}/end`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
  } catch (error) {
    console.error('세션 종료 실패:', error);
  }
  
  window.location.href = '/match/ghost';
}

/**
 * 러닝 시작
 */
function startRunning() {
  isRunning = true;
  isPaused = false;
  startTime = Date.now();
  elapsedSeconds = 0;
  
  startElapsedTimer();
}

/**
 * 러닝 정지
 */
function stopRunning() {
  isRunning = false;
  isPaused = false;
  
  stopGPSTracking();
  
  if (elapsedTimerInterval) {
    clearInterval(elapsedTimerInterval);
    elapsedTimerInterval = null;
  }
  
  // 고스트 상태 타이머 정리
  if (ghostStatusTimer) {
    clearTimeout(ghostStatusTimer);
    ghostStatusTimer = null;
  }
}

/**
 * 경과 시간 타이머 시작
 */
function startElapsedTimer() {
  elapsedTimerInterval = setInterval(() => {
    if (!isPaused) {
      elapsedSeconds++;
      updateElapsedTimeUI();
      updatePaceUI();
      // ✅ 거리/남은거리 TTS는 GPS 업데이트에서 호출하되, 타이머에서도 호출하여 안정성 확보
      updateDistanceUI();
    }
  }, 1000);
}

/**
 * GPS 추적 시작
 */
function startGPSTracking() {
  if (!navigator.geolocation) {
    alert('GPS를 사용할 수 없습니다.');
    return;
  }
  
  const options = {
    enableHighAccuracy: true,
    timeout: 5000,
    maximumAge: 0
  };
  
  watchId = navigator.geolocation.watchPosition(
    onGPSSuccess,
    onGPSError,
    options
  );
  
  console.log('📍 GPS 추적 시작');
}

/**
 * GPS 추적 정지
 */
function stopGPSTracking() {
  if (watchId !== null) {
    navigator.geolocation.clearWatch(watchId);
    watchId = null;
    console.log('📍 GPS 추적 정지');
  }
}

/**
 * GPS 성공
 */
function onGPSSuccess(position) {
  const { latitude, longitude } = position.coords;
  const accuracy = position.coords.accuracy;  // 정확도 (m)
  const speed = position.coords.speed;
  const currentTime = Date.now();
  
  console.log('📍 GPS 업데이트:', latitude, longitude, 'accuracy:', accuracy, 'm');
  
  // ✅ 1. 정확도 필터링 (20m 이하만 사용)
  if (accuracy > 20) {
    console.warn('⚠️ GPS 정확도 낮음:', accuracy, 'm - 무시');
    return;
  }
  
  // 첫 위치 저장
  if (!lastPosition) {
    lastPosition = { lat: latitude, lng: longitude, time: currentTime };
    console.log('📍 첫 위치 저장');
    return;  // ✅ 첫 위치는 거리 계산 안 함!
  }
  
  // ✅ 2. 거리 계산 (Haversine formula)
  const distance = calculateDistance(
    lastPosition.lat,
    lastPosition.lng,
    latitude,
    longitude
  );
  
  // ✅ 3. GPS 점프 감지 (50m 이상 = 오류)
  if (distance > 50) {
    console.warn('⚠️ GPS 점프 감지:', distance.toFixed(2), 'm - 무시');
    lastPosition = { lat: latitude, lng: longitude, time: currentTime };  // 위치만 업데이트
    return;
  }
  
  // ✅ 4. 최소 이동 거리 필터 (3m 이상만 인정)
  if (distance >= 3) {
    // ✅ 속도 제한(경고/로그) - lastPosition 업데이트 전에 계산
    try {
      let speedMps = null;
      if (speed != null && Number.isFinite(speed) && speed > 0) {
        speedMps = speed; // m/s
      } else {
        const prevTime = lastPosition.time;
        const dtSec = (currentTime - prevTime) / 1000;
        if (dtSec > 0) {
          speedMps = distance / dtSec;
        }
      }

      if (speedMps != null && Number.isFinite(speedMps)) {
        const canAlert = currentTime - lastTooFastAlertAt > tooFastAlertCooldownMs;

        if (speedMps >= tooFastHardMps) {
          if (canAlert) {
            console.warn("속도가 너무 빠릅니다(hard):", speedMps, "m/s");
            lastTooFastAlertAt = currentTime;
          }
        } else if (speedMps >= tooFastSoftMps) {
          tooFastSoftCount += 1;
          if (tooFastSoftCount >= 3 && canAlert) {
            console.warn("속도가 너무 빠릅니다(soft):", speedMps, "m/s");
            lastTooFastAlertAt = currentTime;
            tooFastSoftCount = 0;
          }
        } else {
          tooFastSoftCount = 0;
        }
      }
    } catch (e) {
      // ignore
    }
    
    // 완주했으면 거리 누적 안 함
    if (!isFinished) {
      totalDistance += distance;
    }
    lastPosition = { lat: latitude, lng: longitude, time: currentTime };

      if (speedMps != null && Number.isFinite(speedMps)) {
        const canAlert = currentTime - lastTooFastAlertAt > tooFastAlertCooldownMs;

        if (speedMps >= tooFastHardMps) {
          if (canAlert) {
            console.warn("속도가 너무 빠릅니다(hard):", speedMps, "m/s");
            lastTooFastAlertAt = currentTime;
          }
        } else if (speedMps >= tooFastSoftMps) {
          tooFastSoftCount += 1;
          if (tooFastSoftCount >= 3 && canAlert) {
            console.warn("속도가 너무 빠릅니다(soft):", speedMps, "m/s");
            lastTooFastAlertAt = currentTime;
            tooFastSoftCount = 0;
          }
        } else {
          tooFastSoftCount = 0;
        }
      }
    } catch (e) {
      // ignore
    }
    
    // UI 업데이트
    updateDistanceUI();
    
    // WebSocket으로 GPS 데이터 전송
    sendGPSData();
    
    // ⭐ 목표 거리 도달 시 자동 종료
    if (ghostData && ghostData.targetDistance) {
      const targetMeters = ghostData.targetDistance * 1000; // km -> m
      if (totalDistance >= targetMeters && !isFinished) {
        console.log('🏁 목표 거리 도달! 자동 종료');
        autoFinish();
      }
    }
  }
}

/**
 * GPS 에러
 */
function onGPSError(error) {
  console.error('❌ GPS 에러:', error);
  
  if (error.code === error.PERMISSION_DENIED) {
    alert('위치 권한이 필요합니다.');
  }
}

/**
 * Haversine 공식으로 거리 계산 (미터)
 */
function calculateDistance(lat1, lon1, lat2, lon2) {
  const R = 6371e3; // 지구 반지름 (미터)
  const φ1 = (lat1 * Math.PI) / 180;
  const φ2 = (lat2 * Math.PI) / 180;
  const Δφ = ((lat2 - lat1) * Math.PI) / 180;
  const Δλ = ((lon2 - lon1) * Math.PI) / 180;

  const a =
    Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
    Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return R * c;
}

/**
 * WebSocket으로 GPS 데이터 전송
 * 
 * 흐름:
 * 1. 매 1초마다 GPS 위치 수신
 * 2. 총 이동 거리 계산 (Haversine 공식)
 * 3. WebSocket으로 서버에 전송: { distance: km, elapsedTime: sec }
 * 4. 서버에서 고스트와 비교 계산
 * 5. 결과 수신: { status: 'AHEAD'/'BEHIND', distanceDiffMeters }
 * 6. UI 업데이트
 */
function sendGPSData() {
  if (!isConnected) {
    console.warn('⚠️ WebSocket 연결 안 됨 - GPS 전송 실패');
    return;
  }
  if (!stompClient) {
    console.warn('⚠️ STOMP 클라이언트 없음 - GPS 전송 실패');
    return;
  }
  if (!isRunning || isPaused) {
    return;
  }
  
  const data = {
    distance: totalDistance / 1000, // km
    elapsedTime: elapsedSeconds     // 초
  };
  
  const destination = `/pub/ghost-run/${SESSION_ID}/gps`;
  
  console.log(`📤 GPS 전송 -> ${destination}`, {
    distance: `${data.distance.toFixed(3)}km`,
    time: `${data.elapsedTime}s`,
    targetDistance: ghostData?.targetDistance ? `${ghostData.targetDistance}km` : 'unknown'
  });
  
  try {
    stompClient.send(destination, {}, JSON.stringify(data));
    console.log('✅ GPS 전송 성공');
  } catch (error) {
    console.error('❌ GPS 전송 실패:', error);
  }
}

/**
 * UI 업데이트
 */
function updateDistanceUI() {
  const km = (totalDistance / 1000).toFixed(2);
  currentDistanceEl.textContent = km;
  
  // ✅ TTS: 거리/남은거리
  if (ttsReady && window.TtsManager && !completedHandled && !isFinished && ghostData && ghostData.targetDistance) {
    const totalDistanceKm = totalDistance / 1000; // 미터 -> km
    const remainingDistanceKm = Math.max(0, ghostData.targetDistance - totalDistanceKm);
    
    // DIST_DONE: km 단위 체크 (1km, 2km, 3km...)
    const currentKm = Math.floor(totalDistanceKm);
    if (currentKm > lastKmSpoken && currentKm >= 1 && currentKm <= 10) {
      lastKmSpoken = currentKm;
      window.TtsManager.speak(`DIST_DONE_${currentKm}KM`, { priority: 2, cooldownMs: 0 });
    }
    
    // DIST_REMAIN: 남은 거리
    window.TtsManager.onDistance(totalDistanceKm, remainingDistanceKm);
  }
}

function updateElapsedTimeUI() {
  elapsedTimeEl.textContent = formatTime(elapsedSeconds);
}

function updatePaceUI() {
  if (totalDistance > 0 && elapsedSeconds > 0) {
    const km = totalDistance / 1000;
    const paceMinutes = elapsedSeconds / 60 / km; // 분/km
    currentPaceEl.textContent = formatPace(paceMinutes);
    
    // ✅ 페이스 TTS
    if (ttsReady && window.TtsManager && !completedHandled && !isFinished) {
      window.TtsManager.maybeSpeakPace(paceMinutes);
    }
  }
}

/**
 * 유틸리티 함수
 */
function getToken() {
  return localStorage.getItem('accessToken') || getCookie('accessToken');
}

function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
  return null;
}

function formatTime(seconds) {
  if (!seconds) return '00:00';
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

function formatPace(paceMinutes) {
  if (!paceMinutes || paceMinutes === Infinity) return '0:00';
  const minutes = Math.floor(paceMinutes);
  const seconds = Math.round((paceMinutes - minutes) * 60);
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

function formatDate(dateString) {
  if (!dateString) return '-';
  const date = new Date(dateString);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}.${month}.${day}`;
}

/**
 * ✅ WebSocket 재연결 시도
 */
function attemptReconnect() {
  // 이미 재연결 중이면 중복 방지
  if (reconnectTimeout) {
    console.log('⚠️ 이미 재연결 중...');
    return;
  }
  
  reconnectAttempts++;
  
  if (reconnectAttempts > MAX_RECONNECT_ATTEMPTS) {
    console.error('❌ 최대 재연결 시도 초과 (5회)');
    showToast('❌ 연결 실패. 페이지를 새로고침 해주세요.', 'error');
    return;
  }
  
  console.log(`🔄 WebSocket 재연결 시도 (${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS})`);
  showToast(`🔄 연결 회복 중... (${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS})`, 'info');
  
  // ✅ 1초 후 재연결 (즉시 재연결하면 서버 부하 가능)
  reconnectTimeout = setTimeout(() => {
    reconnectTimeout = null;
    
    // WebSocket 연결
    try {
      connectWebSocket();
    } catch (error) {
      console.error('❌ 재연결 실패:', error);
      // 다음 재연결 시도
      attemptReconnect();
    }
  }, 1000);  // 1초 후
}

/**
 * ✅ 토스트 메시지 표시
 */
function showToast(message, type = 'info') {
  // 기존 토스트 제거
  const existingToast = document.getElementById('toast-message');
  if (existingToast) {
    document.body.removeChild(existingToast);
  }
  
  const toast = document.createElement('div');
  toast.id = 'toast-message';
  
  // 타입별 색상
  let bgColor;
  switch(type) {
    case 'success':
      bgColor = 'rgba(34, 197, 94, 0.95)';  // 초록
      break;
    case 'error':
      bgColor = 'rgba(239, 68, 68, 0.95)';  // 빨강
      break;
    case 'info':
    default:
      bgColor = 'rgba(59, 130, 246, 0.95)';  // 파랑
      break;
  }
  
  toast.style.cssText = `
    position: fixed;
    top: 100px;
    left: 50%;
    transform: translateX(-50%);
    background: ${bgColor};
    color: white;
    padding: 16px 24px;
    border-radius: 12px;
    font-size: 16px;
    font-weight: 600;
    z-index: 10000;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    animation: slideDown 0.3s ease-out;
    max-width: 90%;
    text-align: center;
  `;
  toast.textContent = message;
  
  // 애니메이션 정의
  if (!document.getElementById('toast-animation-style')) {
    const style = document.createElement('style');
    style.id = 'toast-animation-style';
    style.textContent = `
      @keyframes slideDown {
        from {
          transform: translateX(-50%) translateY(-100%);
          opacity: 0;
        }
        to {
          transform: translateX(-50%) translateY(0);
          opacity: 1;
        }
      }
    `;
    document.head.appendChild(style);
  }
  
  document.body.appendChild(toast);
  
  // 3초 후 제거
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transition = 'opacity 0.3s';
    setTimeout(() => {
      if (document.body.contains(toast)) {
        document.body.removeChild(toast);
      }
    }, 300);
  }, 3000);
}
