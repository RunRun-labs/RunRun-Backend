/**
 * Match Waiting Room - WebSocket 실시간 연동
 * 백엔드 BattleWebSocketController와 연동
 */

let stompClient = null;
let isConnected = false;
let isReady = false;
let sessionData = null;  // API에서 가져온 세션 데이터
let myUserId = null;  // 현재 로그인한 사용자 ID

// localStorage에서 userId 가져오기
const storedUserId = localStorage.getItem('userId');
if (storedUserId) {
  myUserId = parseInt(storedUserId);
  console.log('👤 현재 사용자 ID:', myUserId);
}

document.addEventListener("DOMContentLoaded", () => {
  console.log("🎮 매칭 대기방 초기화 - sessionId:", SESSION_ID);
  
  // 세션 정보 먼저 로드
  loadSessionData();
  initEventListeners();
});

/**
 * 이벤트 리스너 초기화
 */
function initEventListeners() {
  // 뒤로가기 버튼
  const backButton = document.querySelector(".back-button");
  if (backButton) {
    backButton.addEventListener("click", handleBackButton);
  }

  // 준비완료 버튼
  const readyButton = document.querySelector(".ready-button");
  if (readyButton) {
    readyButton.addEventListener("click", handleReadyToggle);
  }

  // 대결 취소하기 버튼
  const cancelButton = document.querySelector(".cancel-button");
  if (cancelButton) {
    cancelButton.addEventListener("click", handleCancelBattle);
  }
}

/**
 * WebSocket 연결
 */
function connectWebSocket() {
  console.log("🔌 WebSocket 연결 시작...");
  
  const socket = new SockJS('/ws');
  stompClient = Stomp.over(socket);
  
  // 디버그 로그 비활성화 (프로덕션에서는 끄기)
  stompClient.debug = null;
  
  // JWT 토큰 가져오기 (쿠키 또는 localStorage)
  const token = getCookie('accessToken') || localStorage.getItem('accessToken');
  
  const headers = token ? {'Authorization': 'Bearer ' + token} : {};
  
  stompClient.connect(
    headers,
    onConnected,
    onConnectionError
  );
}

/**
 * WebSocket 연결 성공
 */
function onConnected(frame) {
  console.log('✅ WebSocket 연결 성공:', frame);
  isConnected = true;
  
  // 1. Ready 상태 업데이트 구독
  stompClient.subscribe('/sub/battle/' + SESSION_ID + '/ready', function(message) {
    const data = JSON.parse(message.body);
    console.log('📩 Ready 상태 수신:', data);
    handleReadyUpdate(data);
  });
  
  // 2. 배틀 시작 알림 구독
  stompClient.subscribe('/sub/battle/' + SESSION_ID + '/start', function(message) {
    const data = JSON.parse(message.body);
    console.log('🏁 배틀 시작 수신:', data);
    handleBattleStart(data);
  });
  
  // 3. 타임아웃 메시지 구독 (새로 추가)
  stompClient.subscribe('/sub/battle/' + SESSION_ID + '/timeout', function(message) {
    const data = JSON.parse(message.body);
    console.log('⏰ 타임아웃 메시지 수신:', data);
    handleTimeout(data);
  });
  
  // 4. 에러 메시지 구독
  stompClient.subscribe('/sub/battle/' + SESSION_ID + '/errors', function(message) {
    const data = JSON.parse(message.body);
    console.error('❌ 에러 수신:', data);
    console.error('❌ 에러 상세:', JSON.stringify(data, null, 2));
    alert('오류: ' + (data.message || data.error || JSON.stringify(data)));
  });
  
  // ✅ 5. 세션 취소 메시지 구독 (새로 추가)
  stompClient.subscribe('/sub/battle/' + SESSION_ID + '/cancel', function(message) {
    const data = JSON.parse(message.body);
    console.log('❌❌❌ 세션 취소 수신!!!:', data);
    console.log('❌ 구독 경로:', '/sub/battle/' + SESSION_ID + '/cancel');
    console.log('❌ 메시지 내용:', data.message);
    
    // ✅ 타임아웃 없이 바로 모달 표시
    handleSessionCancel(data);
  });
  console.log('✅ 세션 취소 구독 완료:', '/sub/battle/' + SESSION_ID + '/cancel');
  
  // ✅ 6. 참가자 나간 알림 구독 (새로 추가)
  stompClient.subscribe('/sub/battle/' + SESSION_ID + '/user-left', function(message) {
    const data = JSON.parse(message.body);
    console.log('🚪 참가자 이탈 수신:', data);
    console.log('🚪 구독 경로:', '/sub/battle/' + SESSION_ID + '/user-left');
    handleUserLeft(data);
  });
  console.log('✅ 참가자 이탈 구독 완료:', '/sub/battle/' + SESSION_ID + '/user-left');
}

/**
 * WebSocket 연결 실패
 */
function onConnectionError(error) {
  console.error('❌ WebSocket 연결 실패:', error);
  isConnected = false;
  
  // 3초 후 재연결 시도
  setTimeout(() => {
    console.log('🔄 재연결 시도...');
    connectWebSocket();
  }, 3000);
}

/**
 * 준비완료 버튼 토글
 */
function handleReadyToggle() {
  if (!isConnected) {
    alert('서버와 연결되지 않았습니다. 잠시 후 다시 시도해주세요.');
    return;
  }
  
  if (!myUserId) {
    alert('사용자 정보를 불러올 수 없습니다.');
    return;
  }
  
  const readyButton = document.querySelector(".ready-button");
  isReady = !isReady;
  
  console.log('🎯 Ready 토글:', isReady);
  
  // 서버로 Ready 상태 전송
  stompClient.send('/pub/battle/ready', {}, JSON.stringify({
    sessionId: SESSION_ID,
    userId: myUserId,  // 추가
    isReady: isReady
  }));
  
  // UI 즉시 업데이트
  updateMyReadyUI(isReady);
}

/**
 * 내 Ready 상태 UI 업데이트
 */
function updateMyReadyUI(ready) {
  const readyButton = document.querySelector(".ready-button");
  
  if (ready) {
    readyButton.classList.add('ready-active');
    readyButton.innerHTML = `
      <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M16.6667 5L7.5 14.1667L3.33334 10" stroke="#10b981" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <span>준비완료 ✓</span>
    `;
    readyButton.style.backgroundColor = 'rgba(16, 185, 129, 0.2)';
    readyButton.style.color = '#10b981';
  } else {
    readyButton.classList.remove('ready-active');
    readyButton.innerHTML = `
      <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M16.6667 5L7.5 14.1667L3.33334 10" stroke="#10b981" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <span>준비완료</span>
    `;
    readyButton.style.backgroundColor = '';
    readyButton.style.color = '';
  }
}

/**
 * 다른 참가자 Ready 상태 업데이트 수신
 */
function handleReadyUpdate(data) {
  console.log('📊 참가자 Ready 업데이트:', data);
  
  // 해당 userId의 참가자 카드 찾기
  const userId = data.userId;
  const isReady = data.isReady;
  const allReady = data.allReady;  // 서버에서 보낸 allReady 플래그
  
  // sessionData에서 해당 참가자 찾아서 업데이트
  if (sessionData && sessionData.participants) {
    const participant = sessionData.participants.find(p => p.userId === userId);
    if (participant) {
      participant.isReady = isReady;
      
      // 화면 재렌더링
      renderParticipants();
      
      // Ready 카운트 업데이트
      updateReadyCount();
    }
  }
  
  // 모두 Ready면 알림
  if (allReady) {
    console.log('🎉 모두 준비 완료! 잠시 후 자동 시작됩니다.');
    // 서버에서 자동으로 START 메시지를 보낼 것임
  }
}

/**
 * Ready 카운트 업데이트
 */
function updateReadyCount() {
  if (!sessionData || !sessionData.participants) return;
  
  // 실제로 Ready 상태인 참가자 수 계산
  const readyCount = sessionData.participants.filter(p => p.isReady).length;
  const totalCount = sessionData.participants.length;
  
  const readyStatus = document.querySelector(".ready-text");
  if (readyStatus) {
    readyStatus.textContent = `${readyCount}/${totalCount} 준비완료`;
    
    // 모두 준비 완료 시
    if (readyCount === totalCount && totalCount > 0) {
      console.log('✅ 모든 참가자 준비 완료!');
      // TODO: 호스트 여부 확인 후 자동 시작
    }
  }
}

/**
 * 배틀 시작 처리
 */
function handleBattleStart(data) {
  console.log('🚀 배틀 시작!', data);
  
  // 배틀 페이지로 이동
  window.location.href = '/match/battle?sessionId=' + SESSION_ID;
}

/**
 * 배틀 시작 버튼 (호스트만)
 * TODO: HTML에 버튼 추가 필요
 */
function startBattle() {
  if (!isConnected) {
    alert('서버와 연결되지 않았습니다.');
    return;
  }
  
  console.log('🎬 배틀 시작 요청');
  
  stompClient.send('/pub/battle/start', {}, JSON.stringify({
    sessionId: SESSION_ID
  }));
}

/**
 * 뒤로가기
 */
function handleBackButton() {
  if (confirm('대기방을 나가시겠습니까?')) {
    // WebSocket 연결 종료
    if (stompClient && isConnected) {
      stompClient.disconnect();
    }
    
    if (window.history.length > 1) {
      window.history.back();
    } else {
      window.location.href = '/match/select';
    }
  }
}

/**
 * 대결 취소
 */
function handleCancelBattle() {
  if (confirm("정말 대결을 취소하시겠습니까?")) {
    console.log("대결 취소");
    
    // ✅ WebSocket 연결 종료
    if (stompClient && isConnected) {
      stompClient.disconnect();
    }
    
    // ✅ 대기방 나가기 API 호출
    const token = localStorage.getItem('accessToken');
    
    fetch('/api/match/session/' + SESSION_ID + '/leave', {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : ''
      }
    })
    .then(response => {
      if (!response.ok) {
        throw new Error('취소 요청 실패');
      }
      return response.json();
    })
    .then(data => {
      console.log('✅ 취소 성공:', data);
    })
    .catch(error => {
      console.error('❌ 취소 실패:', error);
      alert('취소 요청에 실패했습니다.');
    })
    .finally(() => {
      // ✅ 성공하든 실패하든 본인은 바로 페이지 이동
      console.log('>>> 본인 페이지 이동: /match/select');
      window.location.href = "/match/select";
    });
  }
}

/**
 * 카운트다운 타이머 (서버 시간 기준)
 */
function startCountdown() {
  const countdownTimer = document.querySelector(".countdown-timer");
  if (!countdownTimer) return;
  
  if (!sessionData || !sessionData.createdAt) {
    console.warn('⚠️ createdAt이 없습니다.');
    return;
  }
  
  // 서버에서 받은 세션 생성 시각
  const createdAt = new Date(sessionData.createdAt);
  const limitTime = new Date(createdAt.getTime() + 5 * 60 * 1000);  // +5분
  
  console.log('⏱️ 세션 생성:', createdAt.toLocaleTimeString());
  console.log('⏱️ 제한 시각:', limitTime.toLocaleTimeString());
  
  // 1초마다 남은 시간 계산
  const timerInterval = setInterval(() => {
    const now = new Date();
    const remainingMs = limitTime - now;
    
    if (remainingMs <= 0) {
      clearInterval(timerInterval);
      updateTimerDisplay(0, 0);
      handleCountdownEnd();
      return;
    }
    
    const totalSeconds = Math.floor(remainingMs / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    
    updateTimerDisplay(minutes, seconds);
  }, 100);  // 100ms마다 업데이트 (더 부드럽게)
}

/**
 * 타이머 화면 업데이트
 */
function updateTimerDisplay(minutes, seconds) {
  const countdownTimer = document.querySelector(".countdown-timer");
  if (!countdownTimer) return;
  
  const minutesElement = countdownTimer.querySelector(".timer-minutes");
  const secondsElement = countdownTimer.querySelector(".timer-seconds");
  
  if (minutesElement && secondsElement) {
    minutesElement.textContent = String(minutes).padStart(2, "0");
    secondsElement.textContent = String(seconds).padStart(2, "0");
  }
}

/**
 * 카운트다운 종료 - 타임아웃 API 호출
 */
function handleCountdownEnd() {
  console.log('⏰ 카운트다운 종료 - 타임아웃 처리 시작');
  
  const countdownTimer = document.querySelector(".countdown-timer");
  if (countdownTimer) {
    const minutesElement = countdownTimer.querySelector(".timer-minutes");
    const secondsElement = countdownTimer.querySelector(".timer-seconds");
    
    if (minutesElement && secondsElement) {
      minutesElement.textContent = "00";
      secondsElement.textContent = "00";
    }
  }
  
  // 타임아웃 API 호출
  callTimeoutAPI();
}

/**
 * 타임아웃 API 호출
 */
function callTimeoutAPI() {
  console.log('📡 타임아웃 API 호출: /api/match/session/' + SESSION_ID + '/timeout');
  
  const token = localStorage.getItem('accessToken');
  
  fetch('/api/match/session/' + SESSION_ID + '/timeout', {
    method: 'POST',
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
    console.log('✅ 타임아웃 API 응답:', data);
    // WebSocket으로 메시지가 오므로 여기서는 특별히 처리 안 함
  })
  .catch(error => {
    console.error('❌ 타임아웃 API 실패:', error);
    alert('타임아웃 처리 중 오류가 발생했습니다.');
  });
}

/**
 * 타임아웃 메시지 처리
 */
function handleTimeout(data) {
  console.log('🚨 타임아웃 처리:', data);
  
  if (data.type === 'TIMEOUT_START') {
    // 강퇴 후 시작
    alert(data.message || '일부 참가자가 강퇴되었습니다. 배틀을 시작합니다.');
    
    // 잠시 후 배틀 페이지로 이동 (배틀 시작 메시지가 올 것임)
    
  } else if (data.type === 'TIMEOUT_CANCEL') {
    // 취소
    alert(data.message || '참가자가 부족하여 매치가 취소되었습니다.');
    
    // 메인으로 이동
    window.location.href = '/match/select';
  }
}

/**
 * ✅ 세션 취소 메시지 처리 (새로 추가) - 상대방용
 */
function handleSessionCancel(data) {
  console.log('>>>>>>>>> handleSessionCancel 함수 실행 시작!');
  console.log('>>>>>>>>> data:', data);
  console.log('>>>>>>>>> message:', data.message);
  
  // WebSocket 연결 종료
  if (stompClient && isConnected) {
    stompClient.disconnect();
    console.log('>>>>>>>>> WebSocket 연결 종료');
  }
  
  console.log('>>>>>>>>> 화면에 모달 표시!!!');
  
  // ✅ 화면에 큰 모달 창 표시
  const message = data.message || '매칭이 취소되었습니다.';
  showCancelModal(message);
}

/**
 * ✅ 참가자 나간 알림 처리 (새로 추가)
 */
function handleUserLeft(data) {
  console.log('💬 참가자 이탈:', data);
  
  // ✅ 세션 데이터 다시 로드 (참가자 목록 갱신)
  loadSessionData();
  
  // 토스트 메시지 표시 (선택적)
  // showToast(data.message);
}

/**
 * 쿠키 가져오기
 */
function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
  return null;
}

/**
 * 세션 데이터 로드 (API 호출)
 */
function loadSessionData() {
  console.log('📡 API 호출: /api/match/session/' + SESSION_ID);
  
  // localStorage에서 accessToken 가져오기
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
    console.log('✅ 세션 데이터 로드 성공:', data);
    sessionData = data.data;
    
    // UI 업데이트
    updateSessionInfo();
    renderParticipants();
    
    // WebSocket 연결 및 카운트다운 시작
    connectWebSocket();
    startCountdown();
  })
  .catch(error => {
    console.error('❌ API 호출 실패:', error);
    alert('세션 정보를 불러오지 못했습니다.');
  });
}

/**
 * 세션 정보 UI 업데이트
 */
function updateSessionInfo() {
  // 목표 거리 (이미 km 단위)
  const targetKm = sessionData.targetDistance.toFixed(1);
  document.getElementById('target-distance-km').textContent = targetKm;
  
  // 참가자 수
  document.getElementById('participant-count').textContent = sessionData.totalCount;
  
  // Ready 카운트
  document.getElementById('ready-count').textContent = 
      sessionData.readyCount + '/' + sessionData.totalCount + ' 준비완료';
}

/**
 * 참가자 카드 렌더링
 */
function renderParticipants() {
  const grid = document.getElementById('participants-grid');
  grid.innerHTML = '';  // 기존 내용 제거
  
  sessionData.participants.forEach(participant => {
    const card = createParticipantCard(participant);
    grid.appendChild(card);
    
    // 현재 사용자의 Ready 상태 확인
    if (myUserId && participant.userId === myUserId) {
      isReady = participant.isReady;
      updateMyReadyUI(isReady);
    }
  });
}

/**
 * 참가자 카드 생성
 */
function createParticipantCard(participant) {
  const card = document.createElement('div');
  
  // 카드 클래스 결정
  let cardClass = 'participant-card';
  if (participant.isHost && myUserId && participant.userId === myUserId) {
    cardClass += ' me-card host-card';
  } else if (myUserId && participant.userId === myUserId) {
    cardClass += ' me-card';
  } else if (participant.isHost) {
    cardClass += ' host-card';
  } else if (participant.isReady) {
    cardClass += ' ready-card';
  } else {
    cardClass += ' waiting-card';
  }
  
  card.className = cardClass;
  
  // 카드 HTML 생성
  card.innerHTML = `
    <div class="card-status-icon ${participant.isReady ? 'ready-icon' : 'waiting-icon'}">
      ${participant.isReady ? `
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M11.6667 3.5L5.25 9.91667L2.33334 7" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      ` : ''}
    </div>
    <div class="card-avatar speed-icon">
      ${participant.profileImage ? `
        <img src="${participant.profileImage}" alt="${participant.name}" 
             style="width: 100%; height: 100%; object-fit: cover; border-radius: 50%;" 
             onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
        <svg width="28" height="34" viewBox="0 0 28 34" fill="none" xmlns="http://www.w3.org/2000/svg" style="display: none;">
          <path d="M2 2L14 14L26 2" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M2 12L14 24L26 12" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M2 22L14 34L26 22" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      ` : `
        <svg width="28" height="34" viewBox="0 0 28 34" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M2 2L14 14L26 2" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M2 12L14 24L26 12" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M2 22L14 34L26 22" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      `}
    </div>
    <div class="card-name">${myUserId && participant.userId === myUserId ? '나' : participant.name}</div>
    ${myUserId && participant.userId === myUserId ? '<div class="card-badge me-badge">ME</div>' : ''}
    ${participant.isHost && !(myUserId && participant.userId === myUserId) ? '<div class="card-badge host-badge">HOST</div>' : ''}
    <div class="card-stats">
      <div class="stat-value">평균 페이스 ${participant.avgPace}</div>
    </div>
    <div class="${participant.isReady ? 'card-ready-badge' : 'card-waiting-badge'}">
      <span>${participant.isReady ? '✓ 준비완료' : '대기중...'}</span>
    </div>
  `;
  
  return card;
}

/**
 * ✅ 취소 모달 창 표시
 */
function showCancelModal(message) {
  console.log('>>> showCancelModal 호출:', message);
  
  // 모달 HTML 생성
  const modalHtml = `
    <div id="cancel-modal" style="
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.8);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 10000;
    ">
      <div style="
        background: white;
        padding: 40px;
        border-radius: 20px;
        text-align: center;
        max-width: 400px;
        width: 90%;
        box-shadow: 0 20px 60px rgba(0,0,0,0.5);
      ">
        <div style="
          font-size: 48px;
          margin-bottom: 20px;
        ">⚠️</div>
        <h2 style="
          font-size: 24px;
          font-weight: bold;
          color: #1f2937;
          margin-bottom: 16px;
        ">매칭 취소</h2>
        <p style="
          font-size: 16px;
          color: #6b7280;
          margin-bottom: 32px;
          line-height: 1.6;
        ">${message}</p>
        <button onclick="confirmCancelModal()" style="
          background: #ef4444;
          color: white;
          border: none;
          padding: 16px 48px;
          border-radius: 12px;
          font-size: 18px;
          font-weight: bold;
          cursor: pointer;
          width: 100%;
          transition: all 0.2s;
        " onmouseover="this.style.background='#dc2626'" onmouseout="this.style.background='#ef4444'">확인</button>
      </div>
    </div>
  `;
  
  // body에 모달 추가
  document.body.insertAdjacentHTML('beforeend', modalHtml);
  console.log('>>> 모달 추가 완료');
}

/**
 * ✅ 모달 확인 버튼 클릭
 */
function confirmCancelModal() {
  console.log('>>> 확인 버튼 클릭 - 페이지 이동');
  
  // 모달 제거
  const modal = document.getElementById('cancel-modal');
  if (modal) {
    modal.remove();
  }
  
  // 페이지 이동
  window.location.href = '/match/select';
}

/**
 * 파일 끝
 */
window.addEventListener('beforeunload', () => {
  if (stompClient && isConnected) {
    stompClient.disconnect();
  }
});
