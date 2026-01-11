/**
 * ✅ 상태 확인 및 복원 (새로고침 대응)
 */
function checkAndRestoreState() {
  const token = localStorage.getItem('accessToken');
  
  fetch('/api/battle/' + SESSION_ID + '/rankings', {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? 'Bearer ' + token : ''
    }
  })
  .then(response => {
    if (!response.ok) throw new Error('API 호출 실패');
    return response.json();
  })
  .then(data => {
    console.log('✅ 상태 확인:', data);
    
    if (!data.data || data.data.length === 0) {
      console.warn('⚠️ 순위 데이터 없음 - 카운트다운 시작');
      showCountdown();
      return;
    }
    
    const myData = data.data.find(r => r.userId === myUserId);
    
    if (!myData) {
      console.warn('⚠️ 내 데이터 없음 - 카운트다운 시작');
      showCountdown();
      return;
    }
    
    // ✅ 배틀 시작 여부 확인
    // sessionStorage로 "카운트다운을 이미 봤는지" 확인
    const countdownShown = sessionStorage.getItem('battle_countdown_' + SESSION_ID);
    
    // 카운트다운을 이미 봤으면 복원 모드
    const alreadyStarted = countdownShown === 'true';
    
    if (alreadyStarted) {
      console.log('🔄 이미 진행 중 - 상태 복원 모드');
      
      // 상태 복원
      totalDistance = myData.totalDistance || 0;
      isFinished = myData.isFinished || false;
      
      if (myData.finishTime) {
        elapsedSeconds = Math.floor(myData.finishTime / 1000);
      }
      
      // ✅ 음수 체크 (카운트다운 중)
      if (elapsedSeconds < 0) {
        elapsedSeconds = 0;
        log.info('⚠️ 카운트다운 중 - elapsedSeconds = 0 으로 보정');
      }
      
      if (elapsedSeconds > 0) {
        startTime = new Date(Date.now() - (elapsedSeconds * 1000));
      }
      
      console.log('✅ 복원 완료:', {
        totalDistance: totalDistance.toFixed(2) + 'm',
        elapsedSeconds: elapsedSeconds + 's',
        isFinished: isFinished
      });
      
      // UI 업데이트
      updateMyProgress();
      
      // 타이머 시작
      if (!elapsedTimerInterval) {
        startElapsedTimer();
      }
      
      // GPS 추적 바로 시작
      startGPSTracking();
      
      // 완주 상태 처리
      if (isFinished) {
        showFinishMessage();
        
        // 완주 후 GPS 타이머
        if (!finishedGpsInterval) {
          finishedGpsInterval = setInterval(() => {
            if (lastPosition && lastPosition.lat && lastPosition.lng) {
              sendGpsData(lastPosition.lat, lastPosition.lng, 0);
            }
          }, 2000);
        }
        
        startResultPolling();
      }
      
    } else {
      console.log('🎮 처음 시작 - 카운트다운 모드');
      
      // ✅ 카운트다운 표시 후 sessionStorage에 표시
      showCountdown();
      
      // 카운트다운 끝나면 sessionStorage에 저장 (아래 showCountdown에서 처리)
    }
  })
  .catch(error => {
    console.error('❌ 상태 확인 실패:', error);
    // 에러 시 기본 동작 (카운트다운)
    showCountdown();
  });
}/**
 * Match Battle - 실시간 러닝 대결
 * WebSocket + GPS 추적 + 실시간 순위
 */

// 전역 변수
let stompClient = null;
let isConnected = false;
let SESSION_ID = null;
let myUserId = null;
let sessionData = null;

// GPS 추적
let watchId = null;
let lastPosition = null; // { lat, lng, time, lastSentTime }
let totalDistance = 0; // 미터 단위
let startTime = null;
let elapsedSeconds = 0;
let isFinished = false; // 내가 완주했는지 여부
let isGPSStarted = false; // GPS 추적 시작 여부

// 타이머
let elapsedTimerInterval = null;
let countdownInterval = null;
let timeoutCountdownInterval = null;  // ✅ 타임아웃 카운트다운 인터벌
let finishedGpsInterval = null;  // ✅ 완주 후 GPS 전송 인터벌
let resultPollingInterval = null;  // ✅ 결과 페이지 이동 폴링 인터벌

// 현재 순위 데이터
let currentRankings = [];

// ✅ 타임아웃 정보
let timeoutInfo = null;  // { startTime, timeoutSeconds }

// localStorage에서 userId 가져오기
const storedUserId = localStorage.getItem('userId');
if (storedUserId) {
  myUserId = parseInt(storedUserId);
  console.log('👤 현재 사용자 ID:', myUserId);
}

// 페이지 로드 시 초기화
document.addEventListener("DOMContentLoaded", () => {
  console.log("🎮 배틀 페이지 초기화");
  
  // URL에서 sessionId 가져오기
  const urlParams = new URLSearchParams(window.location.search);
  SESSION_ID = parseInt(urlParams.get('sessionId'));
  
  if (!SESSION_ID) {
    console.error('❌ SESSION_ID가 없습니다!');
    alert('잘못된 접근입니다. (URL에 sessionId가 없음)');
    // window.location.href = '/match/select';  // 임시로 주석 처리
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
  loadSessionData();
}

/**
 * 세션 데이터 로드
 */
function loadSessionData() {
  const token = localStorage.getItem('accessToken');
  
  fetch('/api/match/session/' + SESSION_ID, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? 'Bearer ' + token : ''
    }
  })
  .then(response => {
    if (!response.ok) throw new Error('API 호출 실패');
    return response.json();
  })
  .then(data => {
    console.log('✅ 세션 데이터 로드:', data);
    sessionData = data.data;
    
    // 세션 상태 확인
    if (sessionData.status !== 'IN_PROGRESS') {
      console.warn('⚠️ 세션이 아직 시작되지 않음: status=' + sessionData.status);
      alert('배틀이 아직 시작되지 않았습니다. 대기방으로 이동합니다.');
      window.location.href = '/match/waiting?sessionId=' + SESSION_ID;
      return;
    }
    
    // 목표 거리 표시 (이미 km 단위)
    const targetKm = sessionData.targetDistance.toFixed(1);
    document.getElementById('goal-distance').textContent = targetKm;
    document.querySelector('.battle-title').textContent = targetKm + 'km 스피드 배틀';
    
    // WebSocket 연결
    connectWebSocket();
    
    // ✅ 초기 순위를 먼저 로드해서 상태 확인
    checkAndRestoreState();
  })
  .catch(error => {
    console.error('❌ 세션 데이터 로드 실패:', error);
    console.error('❌ 에러 상세:', error.message);
    console.error('❌ 에러 스택:', error.stack);
    alert('세션 정보를 불러오지 못했습니다. (페이지는 유지됩니다 - 로그 확인용)');
    // window.location.href = '/match/select';  // 임시로 주석 처리 - 로그 확인용
  });
}

/**
 * WebSocket 연결
 */
function connectWebSocket() {
  console.log("🔌 WebSocket 연결 시작...");
  
  const socket = new SockJS('/ws');
  stompClient = Stomp.over(socket);
  stompClient.debug = null; // 디버그 로그 비활성화
  
  const token = localStorage.getItem('accessToken');
  const headers = token ? {'Authorization': 'Bearer ' + token} : {};
  
  stompClient.connect(headers, onConnected, onConnectionError);
}

/**
 * WebSocket 연결 성공
 */
function onConnected(frame) {
  console.log('✅ WebSocket 연결 성공:', frame);
  isConnected = true;
  
  // 실시간 순위 업데이트 구독
  stompClient.subscribe('/sub/battle/' + SESSION_ID + '/ranking', function(message) {
    const data = JSON.parse(message.body);  // BattleUpdateResponse 객체
    console.log('📊 순위 업데이트 수신:', data);
    handleRankingUpdate(data.rankings);  // rankings 배열 추출
  });
  
  // 배틀 종료 이벤트 구독
  stompClient.subscribe('/sub/battle/' + SESSION_ID + '/complete', function(message) {
    console.log('🏁 [WebSocket] 배틀 종료 메시지 수신!');
    console.log('📦 메시지 내용:', message.body);
    
    // 모든 타이머 정리
    if (resultPollingInterval) clearInterval(resultPollingInterval);
    if (finishedGpsInterval) clearInterval(finishedGpsInterval);
    if (elapsedTimerInterval) clearInterval(elapsedTimerInterval);
    if (timeoutCountdownInterval) clearInterval(timeoutCountdownInterval);
    
    // GPS 추적 중지
    if (watchId) {
      navigator.geolocation.clearWatch(watchId);
    }
    
    // 결과 페이지로 즉시 이동
    console.log('🚀 결과 페이지로 이동합니다...');
    window.location.href = '/match/result?sessionId=' + SESSION_ID;
  });
  
  // 포기 메시지 구독 (새로 추가)
  stompClient.subscribe('/sub/battle/' + SESSION_ID + '/quit', function(message) {
    const data = JSON.parse(message.body);
    console.log('🚪 포기 알림 수신:', data);
    handleUserQuit(data);
  });
  
  // ✅ 타임아웃 시작 메시지 구독
  stompClient.subscribe('/sub/battle/' + SESSION_ID + '/timeout-start', function(message) {
    const data = JSON.parse(message.body);
    console.log('⏰ 타임아웃 시작 수신:', data);
    handleTimeoutStart(data);
  });
  
  console.log('✅ 채널 구독 완료');
  
  // 초기 순위 로드 (REST API)
  loadInitialRankings();
}

/**
 * 초기 순위 로드 및 상태 복원
 */
function loadInitialRankings() {
  const token = localStorage.getItem('accessToken');
  
  fetch('/api/battle/' + SESSION_ID + '/rankings', {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? 'Bearer ' + token : ''
    }
  })
  .then(response => {
    if (!response.ok) throw new Error('API 호출 실패');
    return response.json();
  })
  .then(data => {
    console.log('✅ 초기 순위 로드:', data);
    if (data.data && data.data.length > 0) {
      handleRankingUpdate(data.data);
      
      // ✅ 새로고침 시 내 데이터로 상태 복원
      restoreMyState(data.data);
    } else {
      console.warn('⚠️ 초기 순위 데이터가 비어있음');
    }
  })
  .catch(error => {
    console.error('❌ 초기 순위 로드 실패:', error);
  });
}

/**
 * ✅ 내 상태 복원 (새로고침 대응)
 */
function restoreMyState(rankings) {
  const myData = rankings.find(r => r.userId === myUserId);
  
  if (!myData) {
    console.warn('⚠️ 내 데이터를 찾을 수 없음');
    return;
  }
  
  // 거리 복원 (미터 단위)
  totalDistance = myData.totalDistance || 0;
  
  // 완주 여부 복원
  isFinished = myData.isFinished || false;
  
  // 경과 시간 복원 (finishTime이 밀리초 단위)
  if (myData.finishTime) {
    elapsedSeconds = Math.floor(myData.finishTime / 1000);
  }
  
  // ✅ startTime 추정 (현재 시각 - 경과 시간)
  if (elapsedSeconds > 0) {
    startTime = new Date(Date.now() - (elapsedSeconds * 1000));
  } else if (elapsedSeconds < 0) {
    // ✅ 음수면 카운트다운 중 (startTime이 미래)
    startTime = new Date(Date.now() - (elapsedSeconds * 1000));
    elapsedSeconds = 0;  // 0으로 보정
  }
  
  console.log('🔄 상태 복원 완료:', {
    totalDistance: totalDistance.toFixed(2) + 'm',
    elapsedSeconds: elapsedSeconds + 's',
    isFinished: isFinished,
    startTime: startTime
  });
  
  // UI 업데이트
  updateMyProgress();
  
  // ✅ 타이머 복원
  if (elapsedSeconds > 0 && !elapsedTimerInterval) {
    startElapsedTimer();
  }
  
  // ✅ 완주 상태면 메시지 표시
  if (isFinished) {
    showFinishMessage();
    
    // 완주 후 GPS 타이머 시작
    if (!finishedGpsInterval && lastPosition) {
      finishedGpsInterval = setInterval(() => {
        if (lastPosition && lastPosition.lat && lastPosition.lng) {
          sendGpsData(lastPosition.lat, lastPosition.lng, 0);
          console.log('🔄 완주 후 GPS 전송 (복원)');
        }
      }, 2000);
    }
    
    // 결과 페이지 폴링 시작
    startResultPolling();
  }
}

/**
 * WebSocket 연결 실패
 */
function onConnectionError(error) {
  console.error('❌ WebSocket 연결 실패:', error);
  isConnected = false;
  
  // 3초 후 재연결 시도
  setTimeout(() => {
    console.log('🔄 WebSocket 재연결 시도...');
    connectWebSocket();
  }, 3000);
}

/**
 * 10초 카운트다운 표시
 */
function showCountdown() {
  // 카운트다운 오버레이 생성
  const overlay = document.createElement('div');
  overlay.id = 'countdown-overlay';
  overlay.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.9);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    z-index: 9999;
    color: white;
  `;
  
  const title = document.createElement('div');
  title.textContent = '잠시 후 시작합니다!';
  title.style.cssText = `
    font-size: 24px;
    font-weight: 600;
    margin-bottom: 20px;
  `;
  
  const countdownNumber = document.createElement('div');
  countdownNumber.id = 'countdown-number';
  countdownNumber.style.cssText = `
    font-size: 120px;
    font-weight: 800;
    color: #00ff88;
  `;
  
  overlay.appendChild(title);
  overlay.appendChild(countdownNumber);
  document.body.appendChild(overlay);
  
  let count = 10;
  countdownNumber.textContent = count;
  
  countdownInterval = setInterval(() => {
    count--;
    
    if (count > 0) {
      countdownNumber.textContent = count;
      // 애니메이션 효과
      countdownNumber.style.transform = 'scale(1.2)';
      setTimeout(() => {
        countdownNumber.style.transform = 'scale(1)';
      }, 100);
    } else {
      clearInterval(countdownInterval);
      countdownNumber.textContent = 'START!';
      countdownNumber.style.color = '#ff4444';
      
      // ✅ 카운트다운 완료 - sessionStorage에 저장
      sessionStorage.setItem('battle_countdown_' + SESSION_ID, 'true');
      
      setTimeout(() => {
        document.body.removeChild(overlay);
        startGPSTracking();
        startElapsedTimer();
      }, 1000);
    }
  }, 1000);
}

/**
 * GPS 추적 시작
 */
function startGPSTracking() {
  if (!navigator.geolocation) {
    alert('GPS를 지원하지 않는 브라우저입니다.');
    return;
  }
  
  console.log('📍 GPS 추적 시작');
  isGPSStarted = true;
  
  // 위치 권한 요청 및 추적 시작
  watchId = navigator.geolocation.watchPosition(
    onLocationUpdate,
    onLocationError,
    {
      enableHighAccuracy: true,
      timeout: 5000,
      maximumAge: 0
    }
  );
}

/**
 * 위치 업데이트 처리
 */
function onLocationUpdate(position) {
  const lat = position.coords.latitude;
  const lng = position.coords.longitude;
  const speed = position.coords.speed; // m/s
  const accuracy = position.coords.accuracy; // 정확도 (미터)
  const now = Date.now();
  
  console.log('📍 GPS 업데이트:', lat, lng, 'accuracy:', accuracy, 'm');
  
  // ✅ 1. 정확도 필터링 (20m 이하만 사용)
  if (accuracy > 20) {
    console.warn('⚠️ GPS 정확도 낮음:', accuracy, 'm - 무시');
    return;
  }
  
  // 첫 위치 저장
  if (!lastPosition) {
    lastPosition = { lat, lng, time: now };
    startTime = new Date();
    return;
  }
  
  // ✅ 2. 거리 계산 (Haversine formula)
  const distance = calculateDistance(lastPosition, { lat, lng });
  
  // ✅ 3. GPS 점프 감지 (100m 이상 = 오류)
  if (distance > 100) {
    console.warn('⚠️ GPS 점프 감지:', distance.toFixed(2), 'm - 무시');
    lastPosition = { lat, lng, time: now }; // 위치만 업데이트
    return;
  }
  
  // ✅ 4. 최소 이동 거리 필터 (3m 이상만 인정)
  if (distance >= 3) {
    // 완주했으면 거리 누적 안 함
    if (!isFinished) {
      totalDistance += distance;
    }
    lastPosition = { lat, lng, time: now };
    
    // ✅ 5. 1초 간격 제어로 서버 전송
    if (!lastPosition.lastSentTime || (now - lastPosition.lastSentTime) >= 1000) {
      sendGpsData(lat, lng, speed);
      lastPosition.lastSentTime = now;
    }
    
    // UI 업데이트
    updateMyProgress();
  } else if (isFinished) {
    // ✅ 완주 후에는 이동 거리 관계없이 주기적으로 GPS 전송
    // (타임아웃 체크를 위해 필수!)
    if (!lastPosition.lastSentTime || (now - lastPosition.lastSentTime) >= 2000) {
      sendGpsData(lat, lng, speed);
      lastPosition.lastSentTime = now;
      console.log('🏁 완주 후 GPS 전송 (타임아웃 체크용)');
    }
  }
}

/**
 * 위치 오류 처리
 */
function onLocationError(error) {
  console.error('❌ GPS 오류:', error);
  
  switch(error.code) {
    case error.PERMISSION_DENIED:
      alert("위치 권한을 허용해주세요.");
      break;
    case error.POSITION_UNAVAILABLE:
      console.warn("위치 정보를 사용할 수 없습니다.");
      break;
    case error.TIMEOUT:
      console.warn("위치 요청 시간 초과");
      break;
  }
}

/**
 * 두 GPS 좌표 간 거리 계산 (Haversine formula)
 * @returns 거리 (미터)
 */
function calculateDistance(pos1, pos2) {
  const R = 6371e3; // 지구 반지름 (미터)
  const φ1 = pos1.lat * Math.PI / 180;
  const φ2 = pos2.lat * Math.PI / 180;
  const Δφ = (pos2.lat - pos1.lat) * Math.PI / 180;
  const Δλ = (pos2.lng - pos1.lng) * Math.PI / 180;
  
  const a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
            Math.cos(φ1) * Math.cos(φ2) *
            Math.sin(Δλ/2) * Math.sin(Δλ/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  
  return R * c; // 미터
}

/**
 * GPS 데이터 서버로 전송
 */
function sendGpsData(lat, lng, speed) {
  if (!isConnected || !stompClient) {
    console.warn('⚠️ WebSocket 미연결');
    return;
  }
  
  const gpsData = {
    sessionId: SESSION_ID,
    userId: myUserId,
    gps: {
      lat: lat,
      lng: lng,
      speed: speed || 0
    },
    totalDistance: totalDistance
  };
  
  stompClient.send('/pub/battle/gps', {}, JSON.stringify(gpsData));
  console.log('📤 GPS 데이터 전송:', totalDistance.toFixed(2) + 'm');
  
  // 목표 거리 도달 체크 (아직 완주 안 했을 때만)
  // targetDistance는 km, totalDistance는 m이므로 변환 필요
  const targetDistanceInMeters = sessionData.targetDistance * 1000;
  if (!isFinished && totalDistance >= targetDistanceInMeters) {
    handleFinish();
  }
}

/**
 * 내 진행 상황 UI 업데이트
 */
function updateMyProgress() {
  const distanceKm = totalDistance / 1000;
  const targetKm = sessionData.targetDistance; // 이미 km 단위
  const progressPercent = (distanceKm / targetKm) * 100;
  
  document.getElementById('my-distance').textContent = distanceKm.toFixed(2);
  document.getElementById('progress-percent').textContent = progressPercent.toFixed(1);
}

/**
 * 순위 업데이트 처리
 */
function handleRankingUpdate(rankings) {
  currentRankings = rankings;
  
  // 참가자 수 업데이트
  document.getElementById('participants-count').textContent = rankings.length;
  
  // 순위 리스트 렌더링
  renderRankings(rankings);
  
  // 페이스 비교 렌더링
  renderPaceComparison(rankings);
  
  // ✅ 모든 참가자 완주 확인 (매 순위 업데이트마다)
  checkAllFinishedAndRedirect(rankings);
}

/**
 * ✅ 모든 참가자 완주 확인 및 결과 페이지 이동
 */
function checkAllFinishedAndRedirect(rankings) {
  if (!rankings || rankings.length === 0) return;
  
  const allFinished = rankings.every(participant => participant.isFinished);
  
  if (allFinished) {
    console.log('🎉🎉🎉 모든 참가자 완주 감지! 결과 페이지로 이동');
    
    // 모든 타이머 정리
    if (resultPollingInterval) clearInterval(resultPollingInterval);
    if (finishedGpsInterval) clearInterval(finishedGpsInterval);
    if (elapsedTimerInterval) clearInterval(elapsedTimerInterval);
    if (timeoutCountdownInterval) clearInterval(timeoutCountdownInterval);
    
    // GPS 추적 중지
    if (watchId) {
      navigator.geolocation.clearWatch(watchId);
    }
    
    // WebSocket 연결 종료
    if (stompClient && isConnected) {
      stompClient.disconnect();
    }
    
    // 결과 페이지로 이동
    console.log('🚀 결과 페이지로 이동!');
    window.location.href = '/match/result?sessionId=' + SESSION_ID;
  }
}

/**
 * 순위 리스트 렌더링
 */
function renderRankings(rankings) {
  const rankingList = document.getElementById('ranking-list');
  if (!rankingList) return;
  
  rankingList.innerHTML = '';
  
  rankings.forEach((participant, index) => {
    const isMe = participant.userId === myUserId;
    const rankingItem = createRankingItem(participant, isMe, rankings);
    rankingList.appendChild(rankingItem);
  });
}

/**
 * 순위 아이템 생성
 */
function createRankingItem(participant, isMe, allRankings) {
  const item = document.createElement('div');
  item.className = `ranking-item rank-${participant.rank}`;
  if (isMe) item.classList.add('my-rank');
  
  // ✅ 포기한 참가자 스타일 적용 (백엔드는 GIVE_UP 사용)
  const isQuit = participant.status === 'GIVE_UP' || participant.rank === 0;
  if (isQuit) {
    item.classList.add('quit');
  }
  
  // 왼쪽: 순위 + 아바타 + 이름
  const leftArea = document.createElement('div');
  leftArea.className = 'ranking-item-left';
  
  const rankNumber = document.createElement('div');
  rankNumber.className = 'rank-number';
  // ✅ rank가 0이면 "포기" 표시
  rankNumber.textContent = participant.rank === 0 ? '포기' : participant.rank;
  
  const avatar = document.createElement('div');
  avatar.className = 'participant-avatar';
  avatar.innerHTML = `
    <svg width="18" height="21" viewBox="0 0 18 21" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M9 0L0 4.5V9C0 14.25 3.825 19.125 9 21C14.175 19.125 18 14.25 18 9V4.5L9 0Z" fill="white"/>
    </svg>
  `;
  
  const participantInfo = document.createElement('div');
  participantInfo.className = 'participant-info';
  
  const name = document.createElement('div');
  name.className = 'participant-name';
  name.textContent = isMe ? '나' : participant.username;
  
  // ✅ 포기 여부 표시 - 비활성화 (순위 칸에만 표시)
  if (isQuit) {
    // name.innerHTML += ' <span class="quit-badge">✕ 포기</span>';  // 제거
  } else if (participant.isFinished) {
    name.textContent += ' 🏁';
  }
  
  const pace = document.createElement('div');
  pace.className = 'participant-pace';
  // ✅ 포기한 경우 간단하게 표시
  if (isQuit) {
    pace.textContent = '포기함';
    pace.style.color = '#ef4444';
    pace.style.fontWeight = '600';
  } else {
    pace.textContent = participant.isFinished ? '완주!' : `페이스 ${participant.currentPace} /km`;
  }
  
  participantInfo.appendChild(name);
  participantInfo.appendChild(pace);
  
  leftArea.appendChild(rankNumber);
  leftArea.appendChild(avatar);
  leftArea.appendChild(participantInfo);
  
  // 오른쪽: 거리 + 위치
  const rightArea = document.createElement('div');
  rightArea.className = 'ranking-item-right';
  
  const distanceDisplay = document.createElement('div');
  distanceDisplay.className = 'distance-display';
  const distanceKm = (participant.totalDistance / 1000).toFixed(2);
  distanceDisplay.innerHTML = `
    <span class="distance-value">${distanceKm}</span>
    <span class="distance-unit">km</span>
  `;
  
  const positionIndicator = document.createElement('div');
  positionIndicator.className = 'position-indicator';
  
  // ✅ 포기한 경우 위치 표시 안 함
  if (isQuit) {
    positionIndicator.textContent = '포기';
    positionIndicator.classList.add('quit-status');
  } else if (isMe && participant.rank > 1) {
    // 내가 1등이 아닐 때 - 1등과의 거리차
    const firstPlace = allRankings.find(r => r.rank === 1);
    const gap = firstPlace.totalDistance - participant.totalDistance;
    positionIndicator.textContent = `-${gap.toFixed(0)}m (${participant.rank}위)`;
    positionIndicator.classList.add('behind');
  } else if (!isMe && allRankings.length > 0) {
    // 다른 참가자 - 나와의 거리차
    const myData = allRankings.find(r => r.userId === myUserId);
    if (myData) {
      const gap = Math.abs(participant.totalDistance - myData.totalDistance);
      if (participant.totalDistance > myData.totalDistance) {
        positionIndicator.textContent = `+${gap.toFixed(0)}m 앞`;
        positionIndicator.classList.add('ahead');
      } else {
        positionIndicator.textContent = `+${gap.toFixed(0)}m 뒤`;
        positionIndicator.classList.add('behind');
      }
    }
  } else {
    positionIndicator.textContent = '1위';
    positionIndicator.classList.add('ahead');
  }
  
  rightArea.appendChild(distanceDisplay);
  rightArea.appendChild(positionIndicator);
  
  // 콘텐츠
  const content = document.createElement('div');
  content.className = 'ranking-item-content';
  content.appendChild(leftArea);
  content.appendChild(rightArea);
  
  // 진행률 바
  const progressBarContainer = document.createElement('div');
  progressBarContainer.className = 'progress-bar-container';
  
  const progressBar = document.createElement('div');
  progressBar.className = 'progress-bar';
  const progressPercent = Math.min(100, participant.progressPercent); // 100% 제한
  progressBar.style.width = `${progressPercent}%`;
  
  // ✅ 진행률 퍼센트 텍스트 추가
  const progressPercentText = document.createElement('div');
  progressPercentText.className = 'progress-percent-text';
  progressPercentText.textContent = `${progressPercent.toFixed(1)}%`;
  
  progressBarContainer.appendChild(progressBar);
  progressBarContainer.appendChild(progressPercentText);
  
  item.appendChild(content);
  item.appendChild(progressBarContainer);
  
  return item;
}

/**
 * 페이스 비교 렌더링
 */
function renderPaceComparison(rankings) {
  const paceGrid = document.getElementById('pace-grid');
  if (!paceGrid) return;
  
  paceGrid.innerHTML = '';
  
  // 내 데이터 찾기
  const myData = rankings.find(r => r.userId === myUserId);
  if (!myData) return;
  
  // 내 카드 (기준)
  const myCard = createPaceCard({
    name: '나',
    pace: myData.currentPace,
    comparison: { type: 'reference' },
    className: 'user'
  });
  paceGrid.appendChild(myCard);
  
  // 다른 참가자들 (최대 3명)
  const others = rankings.filter(r => r.userId !== myUserId).slice(0, 3);
  others.forEach((participant, index) => {
    const card = createPaceCard({
      name: participant.username,
      pace: participant.currentPace,
      comparison: calculatePaceComparison(myData.currentPace, participant.currentPace),
      className: `opponent-${index + 1}`
    });
    paceGrid.appendChild(card);
  });
}

/**
 * 페이스 비교 계산
 */
function calculatePaceComparison(myPace, otherPace) {
  // "5:30" -> 330초로 변환
  const mySeconds = paceToSeconds(myPace);
  const otherSeconds = paceToSeconds(otherPace);
  
  const diff = Math.abs(mySeconds - otherSeconds);
  
  if (otherSeconds < mySeconds) {
    return { type: 'faster', value: diff };
  } else if (otherSeconds > mySeconds) {
    return { type: 'slower', value: diff };
  } else {
    return { type: 'same', value: 0 };
  }
}

/**
 * 페이스 문자열을 초로 변환
 */
function paceToSeconds(pace) {
  const parts = pace.split(':');
  return parseInt(parts[0]) * 60 + parseInt(parts[1]);
}

/**
 * 페이스 비교 카드 생성
 */
function createPaceCard(data) {
  const card = document.createElement('div');
  card.className = `pace-card ${data.className}`;
  
  card.innerHTML = `
    <div class="pace-card-header">
      <div class="pace-card-avatar">
        <svg width="10" height="12" viewBox="0 0 10 12" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M5 0L0 2.5V5C0 7.92 2.125 10.625 5 12C7.875 10.625 10 7.92 10 5V2.5L5 0Z" fill="white"/>
        </svg>
      </div>
      <div class="pace-card-name">${data.name}</div>
    </div>
    <div class="pace-value-wrapper">
      <span class="pace-value">${data.pace}</span>
      <span class="pace-unit">/km</span>
    </div>
    <div class="pace-comparison">
      <div class="pace-comparison-label ${data.comparison.type}">
        ${getPaceComparisonText(data.comparison)}
      </div>
    </div>
  `;
  
  return card;
}

/**
 * 페이스 비교 텍스트
 */
function getPaceComparisonText(comparison) {
  if (comparison.type === 'reference') {
    return '기준';
  } else if (comparison.type === 'faster') {
    return `${comparison.value}초 빠름 🔥`;
  } else if (comparison.type === 'slower') {
    return `${comparison.value}초 느림`;
  } else {
    return '동일';
  }
}

/**
 * 경과 시간 타이머
 */
function startElapsedTimer() {
  elapsedTimerInterval = setInterval(() => {
    elapsedSeconds++;
    
    // ✅ 음수면 0으로 표시 (카운트다운 중)
    const displaySeconds = Math.max(0, elapsedSeconds);
    
    const minutes = Math.floor(displaySeconds / 60);
    const seconds = displaySeconds % 60;
    
    document.getElementById('elapsed-minutes').textContent = String(minutes).padStart(2, '0');
    document.getElementById('elapsed-seconds').textContent = String(seconds).padStart(2, '0');
  }, 1000);
}

/**
 * 완주 처리
 */
function handleFinish() {
  console.log('🏁 완주!');
  isFinished = true;
  
  // 서버에 완주 알림
  const token = localStorage.getItem('accessToken');
  fetch('/api/battle/' + SESSION_ID + '/finish', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? 'Bearer ' + token : ''
    }
  })
  .then(response => response.json())
  .then(data => {
    console.log('✅ 완주 알림 성공:', data);
  })
  .catch(error => {
    console.error('❌ 완주 알림 실패:', error);
  });
  
  // ✅ GPS 추적은 계속 (다른 참가자 기다림)
  // ✅ 타이머도 계속 진행
  
  // ✅ 완주 후 2초마다 강제로 GPS 전송 (타임아웃 체크용)
  if (!finishedGpsInterval) {
    finishedGpsInterval = setInterval(() => {
      if (lastPosition && lastPosition.lat && lastPosition.lng) {
        sendGpsData(lastPosition.lat, lastPosition.lng, 0);
        console.log('🔄 완주 후 주기적 GPS 전송 (타임아웃 체크용)');
      }
    }, 2000);  // 2초마다
    console.log('⏰ 완주 후 GPS 타이머 시작');
  }
  
  // 완주 메시지 표시
  showFinishMessage();
  
  // ✅ 백업: 5초마다 배틀 상태 폴링 (WebSocket 실패 대비)
  startResultPolling();
}

/**
 * 완주 메시지 표시
 */
function showFinishMessage() {
  const messageDiv = document.createElement('div');
  messageDiv.id = 'finish-message';
  messageDiv.style.cssText = `
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    background: rgba(0, 255, 136, 0.95);
    color: white;
    
    /* ✅ 반응형 패딩 */
    padding: clamp(20px, 5vw, 30px) clamp(30px, 8vw, 50px);
    
    /* ✅ 최대 너비 */
    max-width: 90%;
    min-width: 280px;
    box-sizing: border-box;
    
    border-radius: 20px;
    
    /* ✅ 반응형 폰트 */
    font-size: clamp(20px, 5vw, 32px);
    font-weight: 800;
    line-height: 1.4;
    
    /* ✅ 텍스트 처리 */
    text-align: center;
    word-break: keep-all;
    white-space: pre-line;
    
    z-index: 9998;
    box-shadow: 0 10px 40px rgba(0, 255, 136, 0.5);
    
    /* ✅ 애니메이션 */
    animation: bounceIn 0.5s ease-out;
  `;
  
  /* ✅ 짧고 명확한 메시지 */
  messageDiv.textContent = '🏁 완주!\n잠시만 기다려주세요';
  
  // 애니메이션 정의
  if (!document.getElementById('finish-message-animation')) {
    const style = document.createElement('style');
    style.id = 'finish-message-animation';
    style.textContent = `
      @keyframes bounceIn {
        0% { 
          transform: translate(-50%, -50%) scale(0.5); 
          opacity: 0; 
        }
        60% { 
          transform: translate(-50%, -50%) scale(1.1); 
        }
        100% { 
          transform: translate(-50%, -50%) scale(1); 
          opacity: 1; 
        }
      }
    `;
    document.head.appendChild(style);
  }
  
  document.body.appendChild(messageDiv);
  
  // 3초 후 메시지 제거
  setTimeout(() => {
    messageDiv.style.opacity = '0';
    messageDiv.style.transition = 'opacity 0.5s';
    setTimeout(() => {
      if (document.body.contains(messageDiv)) {
        document.body.removeChild(messageDiv);
      }
    }, 500);
  }, 3000);
}

/**
 * 포기 메시지 처리
 */
function handleUserQuit(data) {
  console.log('🚨 포기 처리:', data);
  
  // 토스트 메시지 표시
  showToast(data.message || data.quitUserName + '님이 포기하셨습니다.');
  
  // 순위 자동 갱신 (포기한 사람 제거됨)
  loadInitialRankings();
}

/**
 * 토스트 메시지 표시
 */
function showToast(message) {
  // 기존 토스트 제거
  const existingToast = document.getElementById('toast-message');
  if (existingToast) {
    document.body.removeChild(existingToast);
  }
  
  const toast = document.createElement('div');
  toast.id = 'toast-message';
  toast.style.cssText = `
    position: fixed;
    top: 100px;
    left: 50%;
    transform: translateX(-50%);
    background: rgba(255, 107, 107, 0.95);
    color: white;
    padding: 16px 24px;
    border-radius: 12px;
    font-size: 16px;
    font-weight: 600;
    z-index: 10000;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    animation: slideDown 0.3s ease-out;
  `;
  toast.textContent = message;
  
  // 애니메이션 정의
  const style = document.createElement('style');
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

/**
 * 이벤트 리스너 설정
 */
function setupEventListeners() {
  // 뒤로가기
  const backButton = document.getElementById('back-button');
  if (backButton) {
    backButton.addEventListener('click', () => {
      if (confirm('러닝을 중단하시겠습니까?')) {
        handleQuit();
      }
    });
  }
  
  // 포기하기
  const giveupButton = document.getElementById('giveup-button');
  if (giveupButton) {
    giveupButton.addEventListener('click', () => {
      if (confirm('정말 포기하시겠습니까?')) {
        handleQuit();
      }
    });
  }
}

/**
 * 포기 처리
 */
function handleQuit() {
  // 서버에 포기 알림
  const token = localStorage.getItem('accessToken');
  fetch('/api/battle/' + SESSION_ID + '/quit', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? 'Bearer ' + token : ''
    }
  })
  .then(response => response.json())
  .then(data => {
    console.log('✅ 포기 알림 성공:', data);
  })
  .catch(error => {
    console.error('❌ 포기 알림 실패:', error);
  });
  
  // GPS 추적 중지
  if (watchId) {
    navigator.geolocation.clearWatch(watchId);
  }
  
  // 타이머 중지
  if (elapsedTimerInterval) {
    clearInterval(elapsedTimerInterval);
  }
  
  if (finishedGpsInterval) {
    clearInterval(finishedGpsInterval);
  }
  
  // WebSocket 연결 종료
  if (stompClient && isConnected) {
    stompClient.disconnect();
  }
  
  // 매칭 선택 페이지로 이동
  window.location.href = '/match/select';
}

/**
 * 페이지 언로드 시 정리
 */
window.addEventListener('beforeunload', () => {
  if (watchId) {
    navigator.geolocation.clearWatch(watchId);
  }
  if (elapsedTimerInterval) {
    clearInterval(elapsedTimerInterval);
  }
  if (timeoutCountdownInterval) {
    clearInterval(timeoutCountdownInterval);
  }
  if (finishedGpsInterval) {
    clearInterval(finishedGpsInterval);
  }
  if (resultPollingInterval) {
    clearInterval(resultPollingInterval);
  }
  if (stompClient && isConnected) {
    stompClient.disconnect();
  }
});

/**
 * ✅ 타임아웃 시작 처리
 */
function handleTimeoutStart(data) {
  console.log('⏰ 타임아웃 시작:', data);
  
  timeoutInfo = {
    startTime: new Date(),
    timeoutSeconds: data.timeoutSeconds
  };
  
  showTimeoutCountdown();
}

/**
 * ✅ 타임아웃 카운트다운 표시
 */
function showTimeoutCountdown() {
  // 기존 카운트다운 제거
  const existing = document.getElementById('timeout-countdown');
  if (existing) {
    existing.remove();
  }
  
  const countdown = document.createElement('div');
  countdown.id = 'timeout-countdown';
  countdown.style.cssText = `
    position: fixed;
    top: 80px;
    left: 50%;
    transform: translateX(-50%);
    background: linear-gradient(135deg, rgba(255, 68, 68, 0.95), rgba(255, 107, 107, 0.95));
    color: white;
    padding: clamp(12px, 3vw, 16px) clamp(20px, 5vw, 30px);
    border-radius: 30px;
    font-size: clamp(16px, 4vw, 20px);
    font-weight: 700;
    z-index: 10000;
    box-shadow: 0 8px 24px rgba(255, 68, 68, 0.5);
    animation: pulse 1.5s infinite, slideDown 0.5s ease-out;
    text-align: center;
    min-width: 200px;
    max-width: 90%;
    box-sizing: border-box;
  `;
  
  // 애니메이션 정의
  if (!document.getElementById('timeout-countdown-animation')) {
    const style = document.createElement('style');
    style.id = 'timeout-countdown-animation';
    style.textContent = `
      @keyframes pulse {
        0%, 100% { 
          transform: translateX(-50%) scale(1); 
          box-shadow: 0 8px 24px rgba(255, 68, 68, 0.5);
        }
        50% { 
          transform: translateX(-50%) scale(1.05); 
          box-shadow: 0 12px 32px rgba(255, 68, 68, 0.7);
        }
      }
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
  
  document.body.appendChild(countdown);
  
  // 1초마다 업데이트
  function updateCountdown() {
    if (!timeoutInfo) {
      if (timeoutCountdownInterval) {
        clearInterval(timeoutCountdownInterval);
      }
      return;
    }
    
    const now = new Date();
    const elapsed = Math.floor((now - timeoutInfo.startTime) / 1000);
    const remaining = timeoutInfo.timeoutSeconds - elapsed;
    
    if (remaining > 0) {
      countdown.innerHTML = `
        <div style="font-size: clamp(14px, 3.5vw, 16px); margin-bottom: 4px;">
          🏆 1등 완주! 제한 시간
        </div>
        <div style="font-size: clamp(24px, 6vw, 32px); font-weight: 900;">
          ⏰ ${remaining}초
        </div>
      `;
      
      // 마지막 10초은 빨간색 강조
      if (remaining <= 10) {
        countdown.style.background = 'linear-gradient(135deg, rgba(220, 38, 38, 0.95), rgba(239, 68, 68, 0.95))';
        countdown.style.animation = 'pulse 0.5s infinite, slideDown 0.5s ease-out';
      }
    } else {
      // ✅ 타임아웃 만료!
      console.log('⏰⏰⏰ 타임아웃 만료! 결과 페이지로 이동 준비');
      
      countdown.innerHTML = `
        <div style="font-size: clamp(18px, 4.5vw, 24px); font-weight: 900;">
          ⏰ 시간 만료!<br>결과 확인 중...
        </div>
      `;
      countdown.style.background = 'linear-gradient(135deg, rgba(153, 27, 27, 0.95), rgba(185, 28, 28, 0.95))';
      
      // 인터벌 중지
      if (timeoutCountdownInterval) {
        clearInterval(timeoutCountdownInterval);
        timeoutCountdownInterval = null;
      }
      
      // ✅ 3초 후 강제로 결과 페이지 확인
      setTimeout(() => {
        console.log('🔍 타임아웃 만료 - 결과 페이지 이동 강제 실행');
        
        // 모든 타이머 정리
        if (finishedGpsInterval) clearInterval(finishedGpsInterval);
        if (elapsedTimerInterval) clearInterval(elapsedTimerInterval);
        if (resultPollingInterval) clearInterval(resultPollingInterval);
        
        // GPS 추적 중지
        if (watchId) {
          navigator.geolocation.clearWatch(watchId);
        }
        
        // WebSocket 연결 종료
        if (stompClient && isConnected) {
          stompClient.disconnect();
        }
        
        // 결과 페이지로 강제 이동
        console.log('🚀 결과 페이지로 이동!');
        window.location.href = '/match/result?sessionId=' + SESSION_ID;
      }, 3000);
    }
  }
  
  // 즉시 한 번 업데이트
  updateCountdown();
  
  // 1초마다 업데이트
  if (timeoutCountdownInterval) {
    clearInterval(timeoutCountdownInterval);
  }
  timeoutCountdownInterval = setInterval(updateCountdown, 1000);
}

/**
 * ✅ 결과 페이지 이동 폴링 시작 (WebSocket 백업)
 */
function startResultPolling() {
  console.log('📡 결과 페이지 폴링 시작 (5초 간격)');
  
  if (resultPollingInterval) {
    clearInterval(resultPollingInterval);
  }
  
  resultPollingInterval = setInterval(() => {
    const token = localStorage.getItem('accessToken');
    
    fetch('/api/match/session/' + SESSION_ID, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : ''
      }
    })
    .then(response => response.json())
    .then(data => {
      console.log('📊 세션 상태 폴링:', data.data.status);
      
      // ✅ 배틀 종료되면 결과 페이지로 이동
      if (data.data.status === 'COMPLETED') {
        console.log('🎉 배틀 종료 감지! 결과 페이지로 이동');
        clearInterval(resultPollingInterval);
        
        // 모든 타이머 정리
        if (finishedGpsInterval) clearInterval(finishedGpsInterval);
        if (elapsedTimerInterval) clearInterval(elapsedTimerInterval);
        if (timeoutCountdownInterval) clearInterval(timeoutCountdownInterval);
        
        // GPS 추적 중지
        if (watchId) {
          navigator.geolocation.clearWatch(watchId);
        }
        
        // 결과 페이지로 이동
        window.location.href = '/match/result?sessionId=' + SESSION_ID;
      }
    })
    .catch(error => {
      console.error('❌ 세션 상태 폴링 실패:', error);
    });
  }, 5000);  // 5초마다 체크
}
