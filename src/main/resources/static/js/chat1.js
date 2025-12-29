// 채팅방 페이지 스크립트 - JWT 인증 적용

let stompClient = null;
let currentUser = null;
let currentSession = null;
let currentUserJoinedAt = null;
let isHost = false;
let hostId = null;

// ============================================
// 인증 관련 함수
// ============================================

// Authorization 헤더 가져오기
function getAuthHeaders() {
  const token = localStorage.getItem('accessToken');
  return {
    'Content-Type': 'application/json',
    'Authorization': token ? 'Bearer ' + token : ''
  };
}

// 인증 포함 fetch 함수
async function fetchWithAuth(url, options = {}) {
  const headers = {
    ...getAuthHeaders(),
    ...options.headers
  };

  const response = await fetch(url, {
    ...options,
    headers
  });

  // 401 Unauthorized면 로그인 페이지로 이동
  if (response.status === 401) {
    alert('로그인이 필요합니다.');
    window.location.href = '/login';
    throw new Error('Unauthorized');
  }

  return response;
}

// 현재 로그인한 사용자 정보 조회
async function getCurrentUser() {
  try {
    const response = await fetchWithAuth('/api/chat/me');
    const result = await response.json();

    if (result.success) {
      return {
        id: result.data.userId,
        loginId: result.data.loginId,
        name: result.data.name
      };
    } else {
      throw new Error(result.message || '사용자 정보 조회 실패');
    }
  } catch (error) {
    console.error('사용자 정보 조회 실패:', error);
    alert('로그인이 필요합니다.');
    window.location.href = '/login';
    return null;
  }
}

// URL 파라미터에서 세션 ID 가져오기 (userId 제거!)
function getUrlParams() {
  const params = new URLSearchParams(window.location.search);
  return {
    sessionId: params.get('sessionId')
  };
}

// ============================================
// 페이지 초기화
// ============================================

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', async function () {
  const params = getUrlParams();

  if (!params.sessionId) {
    alert('세션 ID가 필요합니다.\nURL 형식: /chat/chat1?sessionId=1');
    return;
  }

  // 1. 로그인한 사용자 정보 조회
  const user = await getCurrentUser();
  if (!user) {
    return;
  }

  currentUser = user;
  console.log('현재 사용자:', currentUser);

  // 2. 세션 정보 조회
  await loadSessionInfo(params.sessionId);

  // 3. 화면 업데이트
  updateChatRoomUI();

  // 4. WebSocket 연결
  connectWebSocket();

  // 5. 이벤트 리스너 설정
  setupEventListeners();

  // 6. 페이지 나갈 때 마지막 읽은 시간 업데이트
  window.addEventListener('beforeunload', function () {
    updateLastReadTime(params.sessionId);
  });

  // ⭐ 7. 런닝 중이면 자동으로 재개
  if (currentSession.status === 'IN_PROGRESS') {
    console.log('🔄 런닝 진행 중 감지 - 자동 재개');

    // WebSocket 연결 대기 (1초)
    setTimeout(() => {
      resumeRunning();
    }, 1000);
  }
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
        meetingPlace: result.data.meetingPlace || '장소 미정',
        title: result.data.title || '제목 없음'
      };

      hostId = result.data.hostId;
      isHost = (hostId == currentUser.id);

      // 입장 시점 조회
      try {
        const joinedResponse = await fetchWithAuth(
            `/api/chat/sessions/${sessionId}/joined-at`);
        const joinedResult = await joinedResponse.json();
        if (joinedResult.success) {
          currentUserJoinedAt = joinedResult.data;
        }
      } catch (e) {
        console.warn('입장 시점 조회 실패:', e);
      }
    }
  } catch (error) {
    console.error('세션 정보 조회 실패:', error);
    alert('세션 정보를 불러올 수 없습니다.');
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
  document.getElementById('group-name').textContent = title;

  // 세션 타입
  document.getElementById(
      'session-type-badge').textContent = `🏃 ${currentSession.type}`;

  // 거리
  document.getElementById(
      'session-distance').textContent = `${currentSession.distance}km`;

  // 만남 시간
  const meetingTimeEl = document.getElementById('meeting-time');
  if (currentSession.meetingTime) {
    try {
      const date = new Date(currentSession.meetingTime);
      if (!isNaN(date.getTime())) {
        const formatted = formatDateTime(date);
        meetingTimeEl.textContent = formatted;
      } else {
        meetingTimeEl.textContent = '시간 미정';
      }
    } catch (error) {
      console.error('날짜 파싱 오류:', error);
      meetingTimeEl.textContent = '시간 미정';
    }
  } else {
    meetingTimeEl.textContent = '시간 미정';
  }

  // 만남 장소
  document.getElementById(
      'meeting-place').textContent = currentSession.meetingPlace || '장소 미정';

  // 참여자 수 업데이트
  loadParticipants(currentSession.id);

  // 컨트롤 바 업데이트
  updateControlBar();
}

// 날짜/시간 포맷팅
function formatDateTime(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const weekdays = ['일', '월', '화', '수', '목', '금', '토'];
  const weekday = weekdays[date.getDay()];
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${year}.${month}.${day} (${weekday}) ${hours}:${minutes}`;
}

// 마지막 읽은 시간 업데이트
function updateLastReadTime(sessionId) {
  const token = localStorage.getItem('accessToken');

  fetch(`/api/chat/sessions/${sessionId}/read`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    keepalive: true // 페이지를 나가더라도 요청 유지
  }).then(() => {
    console.log('마지막 읽음 시간 업데이트 완료');
  }).catch(error => {
    console.error('읽음 시간 업데이트 실패:', error);
  });
}

// ============================================
// 참여자 관련
// ============================================

let participantsList = [];

// 현재 사용자의 준비 상태 불러오기
function loadUserReadyStatus() {
  fetchWithAuth(`/api/chat/sessions/${currentSession.id}/users`)
  .then(response => response.json())
  .then(result => {
    if (result.success) {
      const currentUserData = result.data.find(u => u.userId == currentUser.id);
      if (currentUserData) {
        const isReady = currentUserData.isReady;
        const readyButton = document.getElementById('ready-button');
        if (readyButton) {
          if (isReady) {
            readyButton.classList.add('ready-active');
            readyButton.textContent = '준비완료 취소';
          } else {
            readyButton.classList.remove('ready-active');
            readyButton.textContent = '✓ 준비완료';
          }
        }
      }
    }
  })
  .catch(error => console.error('준비 상태 조회 실패:', error));
}

// 참여자 목록 조회
function loadParticipants(sessionId) {
  fetchWithAuth(`/api/chat/sessions/${sessionId}/users`)
  .then(response => response.json())
  .then(result => {
    if (result.success) {
      participantsList = result.data;
      const count = result.data.length;
      document.getElementById(
          'participant-count').textContent = `${count}명 참여중`;

      // 준비 완료 수 업데이트
      const readyCount = result.data.filter(p => p.isReady).length;
      document.getElementById(
          'ready-subtitle').textContent = `${readyCount}/${count}명 준비 완료`;

      // 모달이 열려있으면 목록 업데이트
      const modal = document.getElementById('participant-modal-overlay');
      if (modal && modal.classList.contains('show')) {
        renderParticipantList();
      }
    }
  })
  .catch(error => console.error('참여자 목록 조회 실패:', error));
}

// ============================================
// 컨트롤 바
// ============================================

// 컨트롤 바 업데이트
function updateControlBar() {
  const hostSection = document.getElementById('host-control-section');
  const userSection = document.getElementById('user-control-section');
  const readySection = document.getElementById('ready-section');

  if (currentSession.status === 'IN_PROGRESS') {
    // 진행 중일 때는 컨트롤 숨김
    hostSection.classList.add('hidden');
    userSection.classList.add('hidden');
    readySection.classList.add('hidden');
  } else {
    // 대기 중일 때
    if (isHost) {
      hostSection.classList.remove('hidden');
      userSection.classList.add('hidden');
      readySection.classList.remove('hidden');
      checkAllReadyAndUpdateButton();
    } else {
      hostSection.classList.add('hidden');
      userSection.classList.add('hidden');
      readySection.classList.remove('hidden');
    }
  }
}

// ============================================
// 이벤트 리스너
// ============================================

function setupEventListeners() {
  // 뒤로가기 버튼 - 채팅방 목록으로 이동
  const backButton = document.querySelector('.back-button');
  if (backButton) {
    backButton.addEventListener('click', function () {
      // 나가기 전에 마지막 읽은 시간 업데이트
      updateLastReadTime(currentSession.id);
      window.location.href = '/chat';
    });
  }

  // 스크롤 버튼
  const scrollButton = document.getElementById('scroll-to-bottom');
  const chatContainer = document.querySelector('.chat-container');

  if (scrollButton && chatContainer) {
    function toggleScrollButton() {
      const isScrolledToBottom =
          chatContainer.scrollHeight - chatContainer.scrollTop
          <= chatContainer.clientHeight + 50;
      if (isScrolledToBottom) {
        scrollButton.classList.remove('show');
      } else {
        scrollButton.classList.add('show');
      }
    }

    chatContainer.addEventListener('scroll', toggleScrollButton);
    toggleScrollButton();

    scrollButton.addEventListener('click', function () {
      chatContainer.scrollTo({
        top: chatContainer.scrollHeight,
        behavior: 'smooth'
      });
    });
  }

  // 메시지 전송
  const messageInput = document.getElementById('message-input');
  const sendButton = document.getElementById('send-button');

  if (messageInput && sendButton) {
    sendButton.addEventListener('click', sendMessage);

    messageInput.addEventListener('keypress', function (e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
      }
    });
  }

  // 런닝 시작 버튼
  const startBtn = document.getElementById('start-running-btn');
  if (startBtn) {
    startBtn.addEventListener('click', startRunning);
  }

  // 런닝 취소 버튼
  const cancelBtn = document.getElementById('cancel-running-btn');
  if (cancelBtn) {
    cancelBtn.addEventListener('click', cancelRunning);
  }

  // 준비완료 버튼
  const readyButton = document.getElementById('ready-button');
  if (readyButton) {
    readyButton.addEventListener('click', toggleReadyStatus);
  }

  // 참여자 목록 모달 열기
  const moreMenuBtn = document.getElementById('more-menu-btn');
  if (moreMenuBtn) {
    moreMenuBtn.addEventListener('click', function () {
      openParticipantModal();
    });
  }

  // 참여자 목록 모달 닫기
  const modalOverlay = document.getElementById('participant-modal-overlay');
  const modalClose = document.getElementById('participant-modal-close');
  if (modalOverlay && modalClose) {
    modalClose.addEventListener('click', closeParticipantModal);
    modalOverlay.addEventListener('click', function (e) {
      if (e.target === modalOverlay) {
        closeParticipantModal();
      }
    });
  }

  // 채팅방 나가기 버튼
  const leaveChatBtn = document.getElementById('leave-chat-btn');
  if (leaveChatBtn) {
    leaveChatBtn.addEventListener('click', function () {
      if (confirm('채팅방을 나가시겠습니까?')) {
        closeParticipantModal();
        leaveChatRoom();
      }
    });
  }

  // 런닝 결과 모달 닫기 버튼
  const resultCloseBtn = document.getElementById('running-result-modal-close');
  if (resultCloseBtn) {
    resultCloseBtn.addEventListener('click', closeRunningResultModal);
  }

  // 런닝 결과 모달 오버레이 클릭 시 닫기
  const resultOverlay = document.getElementById('running-result-modal-overlay');
  if (resultOverlay) {
    resultOverlay.addEventListener('click', function (e) {
      if (e.target === resultOverlay) {
        closeRunningResultModal();
      }
    });
  }
}

// ============================================
// WebSocket 연결
// ============================================

function connectWebSocket() {
  const socket = new SockJS('/ws');
  stompClient = Stomp.over(socket);
  stompClient.debug = null;

  // JWT 토큰을 WebSocket 헤더에 포함
  const token = localStorage.getItem('accessToken');
  const headers = token ? {'Authorization': 'Bearer ' + token} : {};

  stompClient.connect(headers, function (frame) {
    console.log('WebSocket 연결 성공');

    // 현재 사용자의 준비 상태 불러오기
    loadUserReadyStatus();

    // 과거 메시지 불러오기
    loadPreviousMessages();

    // 구독
    stompClient.subscribe('/sub/chat/' + currentSession.id,
        function (response) {
          const message = JSON.parse(response.body);
          displayMessage(message);
          if (message.messageType === 'SYSTEM' &&
              message.content &&
              message.content.includes('런닝이 시작되었습니다')) {

            console.log('🏃 런닝 시작 감지 - 통계 구독 시작');

            // 모든 참여자: 실시간 통계 구독
            if (!isHost) {  // 방장은 이미 구독했으므로 제외
              subscribeToRunningStats();
              showRunningUI();

              // 세션 상태 업데이트
              currentSession.status = 'IN_PROGRESS';
              updateControlBar();
            }
          }

          // KICK 메시지 처리
          if (message.messageType === 'KICK') {
            // 내가 강퇴당한 경우
            if (message.senderId == currentUser.id) {
              alert('방장에 의해 강퇴되었습니다.');

              // 채팅방 목록으로 이동
              if (stompClient) {
                stompClient.disconnect();
              }
              window.location.href = '/chat';
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
          if (message.messageType === 'SYSTEM') {
            // 입장, 퇴장, 준비완료 메시지일 때 참여자 정보 업데이트
            setTimeout(() => {
              loadParticipants(currentSession.id);

              // 준비완료 메시지면 방장의 시작 버튼도 업데이트
              if (isHost && (message.content.includes('준비완료')
                  || message.content.includes('준비를 취소'))) {
                checkAllReadyAndUpdateButton();
              }
            }, 300);
          }

          // 런닝 시작 메시지면 상태 업데이트
          if (message.messageType === 'SYSTEM' && message.content.includes(
              '런닝이 시작되었습니다')) {
            currentSession.status = 'IN_PROGRESS';
            updateControlBar();
          }

          // 런닝 종료 메시지면 모든 참여자에게 결과 모달 표시
          if (message.messageType === 'SYSTEM' && message.content.includes(
              '런닝이 종료되었습니다')) {
            console.log('🏁 런닝 종료 감지 - 결과 모달 표시');

            // 세션 상태 업데이트
            currentSession.status = 'COMPLETED';
            updateControlBar();

            // 테스트 패널 숨기기
            const testPanel = document.getElementById('running-test-panel');
            if (testPanel) {
              testPanel.style.display = 'none';
            }

            // GPS 구독 해제 (참여자)
            if (gpsSubscription) {
              gpsSubscription.unsubscribe();
              gpsSubscription = null;
              console.log('🛑 GPS 구독 해제됨 (참여자)');
            }

            // 결과 모달 표시 (모든 참여자)
            setTimeout(() => {
              showRunningResultModal();
            }, 500);
          }
        });

    // 입장 메시지 전송 제거 (채팅방 생성 시에만 백엔드에서 자동 전송)
    // stompClient.send('/pub/chat/enter', {}, JSON.stringify({
    //   sessionId: currentSession.id,
    //   senderId: currentUser.id,
    //   senderName: currentUser.name
    // }));
  }, function (error) {
    console.error('WebSocket 연결 실패:', error);
  });
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
  .then(response => response.json())
  .then(result => {
    if (result.success && result.data.length > 0) {
      result.data.forEach(message => {
        displayMessage(message, true);
      });

      // 스크롤을 맨 아래로
      setTimeout(() => {
        const chatContainer = document.querySelector('.chat-container');
        if (chatContainer) {
          chatContainer.scrollTo({
            top: chatContainer.scrollHeight,
            behavior: 'auto'
          });
        }
      }, 100);
    }
  })
  .catch(error => console.error('메시지 로드 실패:', error));
}

// 메시지 전송
function sendMessage() {
  const input = document.getElementById('message-input');
  const content = input.value.trim();

  if (!content || !stompClient) {
    return;
  }

  stompClient.send('/pub/chat/message', {}, JSON.stringify({
    sessionId: currentSession.id,
    senderId: currentUser.id,
    senderName: currentUser.name,
    content: content,
    messageType: 'TEXT'
  }));

  input.value = '';

  // 메시지 전송 후 즉시 스크롤을 맨 아래로
  setTimeout(() => {
    const chatContainer = document.querySelector('.chat-container');
    if (chatContainer) {
      chatContainer.scrollTo({
        top: chatContainer.scrollHeight,
        behavior: 'smooth'
      });
    }
  }, 100);
}

// 메시지 표시
function displayMessage(message, isPrevious = false) {
  const messagesDiv = document.getElementById('chat-messages');

  // 시스템 메시지 (SYSTEM, KICK 포함)
  if (message.messageType === 'SYSTEM' || message.messageType === 'KICK') {
    const systemDiv = document.createElement('div');
    systemDiv.className = 'system-message';
    const p = document.createElement('p');
    p.textContent = message.content;
    systemDiv.appendChild(p);
    messagesDiv.appendChild(systemDiv);

    // 참여자 목록 업데이트
    loadParticipants(currentSession.id);
  } else {
    const isMyMessage = message.senderId == currentUser.id;

    const messageItem = document.createElement('div');
    messageItem.className = `message-item ${isMyMessage ? 'message-right'
        : 'message-left'}`;

    if (!isMyMessage) {
      // 아바타
      const avatar = document.createElement('div');
      avatar.className = 'message-avatar';
      avatar.innerHTML = '<svg width="18" height="21" viewBox="0 0 18 21" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M9 0C4.02944 0 0 4.02944 0 9C0 13.9706 4.02944 18 9 18C13.9706 18 18 13.9706 18 9C18 4.02944 13.9706 0 9 0Z" fill="#E5E7EB"/></svg>';
      messageItem.appendChild(avatar);
    }

    const contentWrapper = document.createElement('div');
    contentWrapper.className = 'message-content-wrapper';

    if (!isMyMessage) {
      const sender = document.createElement('p');
      sender.className = 'message-sender';
      sender.textContent = message.senderName;
      contentWrapper.appendChild(sender);
    }

    const bubble = document.createElement('div');
    bubble.className = `message-bubble ${isMyMessage ? 'message-bubble-right'
        : 'message-bubble-left'}`;
    const text = document.createElement('p');
    text.className = 'message-text';
    text.textContent = message.content;
    bubble.appendChild(text);
    contentWrapper.appendChild(bubble);

    if (message.createdAt) {
      const time = document.createElement('p');
      time.className = 'message-time';
      const date = new Date(message.createdAt);
      time.textContent = `${String(date.getHours()).padStart(2, '0')}:${String(
          date.getMinutes()).padStart(2, '0')}`;
      contentWrapper.appendChild(time);
    }

    messageItem.appendChild(contentWrapper);
    messagesDiv.appendChild(messageItem);
  }

  // 스크롤을 맨 아래로
  if (!isPrevious) {
    setTimeout(() => {
      const chatContainer = document.querySelector('.chat-container');
      if (chatContainer) {
        chatContainer.scrollTo({
          top: chatContainer.scrollHeight,
          behavior: 'smooth'
        });
      }
    }, 100);
  }
}

// ============================================
// 준비완료 / 런닝 시작
// ============================================

// 준비완료 토글 (userId 제거 - 서버에서 자동 처리)
function toggleReadyStatus() {
  fetchWithAuth(`/api/chat/sessions/${currentSession.id}/ready`, {
    method: 'POST'
  })
  .then(response => response.json())
  .then(result => {
    if (result.success) {
      const isReady = result.data.isReady;

      // 버튼 UI 업데이트
      const readyButton = document.getElementById('ready-button');
      if (readyButton) {
        if (isReady) {
          readyButton.classList.add('ready-active');
          readyButton.textContent = '준비완료 취소';
        } else {
          readyButton.classList.remove('ready-active');
          readyButton.textContent = '✓ 준비완료';
        }
      }

      // 시스템 메시지 전송
      const message = isReady
          ? `${currentUser.name}님이 준비완료했습니다.`
          : `${currentUser.name}님이 준비를 취소했습니다.`;

      stompClient.send('/pub/chat/message', {}, JSON.stringify({
        sessionId: currentSession.id,
        senderId: null,
        senderName: 'SYSTEM',
        content: message,
        messageType: 'SYSTEM'
      }));

      // 런닝 시작 버튼 상태 업데이트 (방장이면)
      if (isHost) {
        checkAllReadyAndUpdateButton();
      }
    } else {
      alert(result.message || '준비 상태 변경 실패');
    }
  })
  .catch(error => {
    console.error('준비 상태 변경 실패:', error);
    alert('준비 상태 변경에 실패했습니다.');
  });
}

// 모두 준비완료 확인 및 런닝 시작 버튼 업데이트
function checkAllReadyAndUpdateButton() {
  fetchWithAuth(`/api/chat/sessions/${currentSession.id}/all-ready`)
  .then(response => response.json())
  .then(result => {
    if (result.success) {
      const allReady = result.data.allReady;
      const startBtn = document.getElementById('start-running-btn');

      if (startBtn) {
        if (allReady) {
          startBtn.disabled = false;
          startBtn.textContent = '🏃 런닝 시작';
          startBtn.style.opacity = '1';
        } else {
          startBtn.disabled = true;
          startBtn.textContent = `🏃 런닝 시작 (${result.data.readyCount}/${result.data.totalCount} 준비완료)`;
          startBtn.style.opacity = '0.5';
        }
      }
    }
  })
  .catch(error => console.error('준비 상태 확인 실패:', error));
}

// ============================================
// 런닝 추적 기능
// ============================================

let runningTracker = null;

// 런닝 시작 (방장만, 모두 준비완료 시) - GPS 추적 포함
async function startRunning() {
  if (!isHost) {
    alert('방장만 런닝을 시작할 수 있습니다.');
    return;
  }

  if (!confirm('런닝을 시작하시겠습니까?')) {
    return;
  }

  try {
    // 1. API 호출 - 런닝 상태 변경
    const response = await fetchWithAuth(
        `/api/chat/sessions/${currentSession.id}/start`, {
          method: 'POST'
        });

    if (!response.ok) {
      const error = await response.json();
      alert(error.message || '런닝 시작 실패');
      return;
    }

    console.log('✅ 런닝 시작 API 호출 완료');

    // 2. 세션 상태 업데이트
    currentSession.status = 'IN_PROGRESS';
    updateControlBar();

    // 3. 방장만 GPS 추적 시작 (실제 GPS)
    console.log('🎯 방장 - GPS 추적 시작 (실제 GPS)');
    runningTracker = new RunningTracker(currentSession.id, currentUser.id,
        stompClient, false);  // ⭐ false = 실제 GPS!
    runningTracker.startTracking();

    // 4. 모든 참여자: 실시간 통계 구독
    subscribeToRunningStats();

    // 5. 테스트 UI 표시
    showRunningUI();

    // 6. 런닝 시작 시스템 메시지 전송
    stompClient.send('/pub/chat/message', {}, JSON.stringify({
      sessionId: currentSession.id,
      senderId: null,
      senderName: 'SYSTEM',
      content: '🏃 런닝이 시작되었습니다! 모두 화이팅!',
      messageType: 'SYSTEM'
    }));

    alert('런닝이 시작되었습니다! 🏃\n📍 실제 GPS 추적 시작!');

  } catch (error) {
    console.error('런닝 시작 에러:', error);
    alert('런닝 시작에 실패했습니다.');
  }
}

// 런닝 취소 (세션 퇴장) - userId 제거
function cancelRunning() {
  if (currentSession.status === 'IN_PROGRESS') {
    alert('런닝이 진행중이라 취소할 수 없습니다.');
    return;
  }

  if (!confirm('정말 런닝을 취소하시겠습니까?\n채팅방에서 나가게 됩니다.')) {
    return;
  }

  // 퇴장 메시지 전송
  stompClient.send('/pub/chat/message', {}, JSON.stringify({
    sessionId: currentSession.id,
    senderId: null,
    senderName: 'SYSTEM',
    content: `${currentUser.name}님이 런닝을 취소했습니다.`,
    messageType: 'SYSTEM'
  }));

  // 세션에서 퇴장 (새 API 사용)
  fetchWithAuth(`/api/chat/sessions/${currentSession.id}/leave`, {
    method: 'DELETE'
  })
  .then(response => response.json())
  .then(result => {
    setTimeout(() => {
      if (stompClient) {
        stompClient.disconnect();
      }
      alert('런닝을 취소했습니다.');
      window.history.back();
    }, 300);
  })
  .catch(error => {
    console.error('퇴장 실패:', error);
  });
}

// ============================================
// 채팅방 퇴장
// ============================================

function leaveChatRoom() {
  if (stompClient !== null) {
    // 1. 퇴장 메시지 전송
    stompClient.send('/pub/chat/leave', {}, JSON.stringify({
      sessionId: currentSession.id,
      senderId: currentUser.id,
      senderName: currentUser.name
    }));

    // 2. DB에서 참가자 삭제 (새 API 사용)
    fetchWithAuth(`/api/chat/sessions/${currentSession.id}/leave`, {
      method: 'DELETE'
    })
    .then(response => response.json())
    .then(result => {
      console.log('퇴장 완료:', result);
    })
    .catch(error => {
      console.error('퇴장 API 실패:', error);
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
  const modal = document.getElementById('participant-modal-overlay');
  if (modal) {
    modal.classList.add('show');
    renderParticipantList();
    loadParticipants(currentSession.id);
  }
}

function closeParticipantModal() {
  const modal = document.getElementById('participant-modal-overlay');
  if (modal) {
    modal.classList.remove('show');
  }
}

function renderParticipantList() {
  const listContainer = document.getElementById('participant-list');
  const readyCountEl = document.getElementById('participant-ready-count');
  const readyTotalEl = document.getElementById('participant-ready-total');

  if (!listContainer || !participantsList.length) {
    return;
  }

  // 준비 완료 수 및 전체 인원 업데이트
  const readyCount = participantsList.filter(p => p.isReady).length;
  const totalCount = participantsList.length;

  if (readyCountEl) {
    readyCountEl.textContent = readyCount;
  }

  if (readyTotalEl) {
    readyTotalEl.textContent = `/${totalCount}명 준비완료`;
  }

  // 목록 초기화
  listContainer.innerHTML = '';

  // 참여자 목록 렌더링
  participantsList.forEach(participant => {
    const isCurrentUser = participant.userId == currentUser.id;
    const isHostUser = participant.userId == hostId;
    const isReady = participant.isReady;

    const item = document.createElement('div');
    item.className = 'participant-item';

    // 아바타 래퍼
    const avatarWrapper = document.createElement('div');
    avatarWrapper.className = 'participant-avatar-wrapper';

    const avatar = document.createElement('div');
    avatar.className = 'participant-avatar';
    if (isHostUser) {
      avatar.classList.add('avatar-yellow');
    }

    // 아바타 아이콘
    avatar.innerHTML = '<svg class="participant-avatar-icon" width="22" height="26" viewBox="0 0 22 26" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M11 0C4.925 0 0 4.925 0 11C0 17.075 4.925 22 11 22C17.075 22 22 17.075 22 11C22 4.925 17.075 0 11 0Z" fill="#E5E7EB"/></svg>';

    // 준비 상태 배지
    const statusBadge = document.createElement('div');
    statusBadge.className = 'participant-status-badge';
    if (!isReady) {
      statusBadge.classList.add('waiting');
    } else {
      statusBadge.innerHTML = '<svg width="10" height="10" viewBox="0 0 10 10" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M8.33333 2.5L3.75 7.08333L1.66667 5" stroke="white" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>';
    }

    avatarWrapper.appendChild(avatar);
    avatarWrapper.appendChild(statusBadge);

    // 참여자 정보
    const info = document.createElement('div');
    info.className = 'participant-info';

    const nameRow = document.createElement('div');
    nameRow.className = 'participant-name-row';

    const name = document.createElement('span');
    name.className = 'participant-name';
    if (isCurrentUser) {
      name.classList.add('current-user');
      name.textContent = '나';
    } else {
      name.textContent = participant.name || '이름 없음';
    }

    nameRow.appendChild(name);

    // 역할 배지
    if (isHostUser) {
      const hostBadge = document.createElement('span');
      hostBadge.className = 'participant-role-badge host';
      hostBadge.textContent = '방장';
      nameRow.appendChild(hostBadge);
    } else if (isCurrentUser) {
      const meBadge = document.createElement('span');
      meBadge.className = 'participant-role-badge me';
      meBadge.textContent = '나';
      nameRow.appendChild(meBadge);
    }

    info.appendChild(nameRow);

    // 평균 페이스
    const pace = document.createElement('div');
    pace.className = 'participant-pace';
    pace.textContent = `평균 페이스 ${participant.averagePace || '5:30'} /km`;
    info.appendChild(pace);

    // 준비 상태 + 강퇴 버튼
    const rightSection = document.createElement('div');
    rightSection.className = 'participant-right-section';

    const readyStatus = document.createElement('div');
    readyStatus.className = 'participant-ready-status';
    const readyText = document.createElement('span');
    readyText.className = 'participant-ready-text';
    if (!isReady) {
      readyText.classList.add('waiting');
      readyText.textContent = '대기중';
    } else {
      readyText.textContent = '준비완료';
    }
    readyStatus.appendChild(readyText);
    rightSection.appendChild(readyStatus);

    // 강퇴 버튼 (방장이고, 자기 자신이 아닌 경우만)
    if (isHost && !isCurrentUser) {
      const kickBtn = document.createElement('button');
      kickBtn.className = 'kick-btn';
      kickBtn.textContent = '강퇴';
      kickBtn.onclick = () => kickParticipant(participant.userId,
          participant.name);
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
        `/api/chat/sessions/${currentSession.id}/kick/${userId}`, {
          method: 'DELETE'
        });

    if (!response.ok) {
      const error = await response.json();
      alert(error.message || '강퇴에 실패했습니다.');
      return;
    }

    console.log(`${userName}님을 강퇴했습니다.`);

    // 시스템 메시지가 WebSocket으로 전달되므로
    // 참여자 목록은 자동으로 갱신됨

  } catch (error) {
    console.error('강퇴 에러:', error);
    alert('강퇴 중 오류가 발생했습니다.');
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
    console.error('❌ WebSocket 연결 없음');
    return;
  }

  // 이미 구독 중이면 중복 구독 방지
  if (gpsSubscription) {
    console.log('⚠️ 이미 런닝 통계를 구독 중입니다');
    return;
  }

  gpsSubscription = stompClient.subscribe(
      `/sub/running/${currentSession.id}`,
      function (message) {
        const stats = JSON.parse(message.body);
        console.log('📊 통계 수신:', stats);

        updateRunningUI(stats);

        // 목표 거리 도달 시 GPS 자동 중지
        if (stats.remainingDistance <= 0 && runningTracker
            && runningTracker.isTracking) {
          console.log('🎯 목표 거리 도달! GPS 추적 중지');
          finishRunning(true); // 자동 종료
        }
      });

  console.log('✅ 런닝 통계 구독 완료:', `/sub/running/${currentSession.id}`);
  subscribeToRunningErrors();
}

/**
 * 런닝 UI 업데이트
 */
function updateRunningUI(stats) {
  // 테스트 패널 요소들
  const teamPaceEl = document.getElementById('test-pace');
  const currentDistanceEl = document.getElementById('test-distance');
  const remainingDistanceEl = document.getElementById('test-remaining');
  const runningTimeEl = document.getElementById('test-time');
  const segmentsEl = document.getElementById('test-segments');

  // 팀 평균 페이스
  if (teamPaceEl && stats.teamAveragePace) {
    const paceMin = Math.floor(stats.teamAveragePace);
    const paceSec = Math.round((stats.teamAveragePace - paceMin) * 60);
    teamPaceEl.textContent = `${paceMin}:${String(paceSec).padStart(2,
        '0')}/km`;
  }

  // 현재 거리
  if (currentDistanceEl && stats.totalDistance !== undefined) {
    currentDistanceEl.textContent = `${stats.totalDistance.toFixed(2)}km`;
  }

  // 남은 거리
  if (remainingDistanceEl && stats.remainingDistance !== undefined) {
    const remaining = Math.max(0, stats.remainingDistance);
    remainingDistanceEl.textContent = `${remaining.toFixed(2)}km`;
  }

  // 런닝 시간
  if (runningTimeEl && stats.totalRunningTime !== undefined) {
    const hours = Math.floor(stats.totalRunningTime / 3600);
    const minutes = Math.floor((stats.totalRunningTime % 3600) / 60);
    const seconds = stats.totalRunningTime % 60;
    runningTimeEl.textContent = `${String(hours).padStart(2, '0')}:${String(
        minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
  }

  // km별 페이스 업데이트
  if (segmentsEl && stats.segmentPaces && Object.keys(stats.segmentPaces).length
      > 0) {
    segmentsEl.innerHTML = ''; // 초기화

    // Map → 배열 변환 후 표시
    Object.entries(stats.segmentPaces).forEach(([km, pace]) => {
      const segmentDiv = document.createElement('div');
      segmentDiv.style.cssText = 'margin-bottom: 4px; font-size: 12px; color: #1F2937;';

      const paceMin = Math.floor(pace);
      const paceSec = Math.round((pace - paceMin) * 60);

      segmentDiv.textContent = `${km}km: ${paceMin}:${String(paceSec).padStart(
          2, '0')}/km`;
      segmentsEl.appendChild(segmentDiv);
    });
  } else if (segmentsEl) {
    segmentsEl.innerHTML = '<div style="font-size: 12px; color: #9CA3AF;">데이터 수신 대기 중...</div>';
  }

  console.log('📈 UI 업데이트:', {
    pace: stats.teamAveragePace,
    distance: stats.totalDistance,
    remaining: stats.remainingDistance,
    time: stats.totalRunningTime,
    segments: stats.segmentPaces ? Object.keys(stats.segmentPaces).length : 0
  });
}

/**
 * 런닝 재개 (채팅방 다시 입장 시)
 */
function resumeRunning() {
  if (!stompClient || !stompClient.connected) {
    console.error('❌ WebSocket 연결 없음');
    return;
  }

  console.log('🔄 런닝 재개 시작...');

  // 1. 통계 구독 (모든 참여자)
  subscribeToRunningStats();

  // 2. UI 표시 (모든 참여자)
  showRunningUI();

  // 3. 방장이면 GPS 추적 재시작
  if (isHost && !runningTracker) {
    console.log('🎯 방장 - GPS 추적 재시작 (실제 GPS)');
    runningTracker = new RunningTracker(currentSession.id, currentUser.id,
        stompClient, false);  // ⭐ false = 실제 GPS!
    runningTracker.startTracking();
  }

  console.log('✅ 런닝 재개 완료');
}

/**
 * 런닝 UI 표시
 */
function showRunningUI() {
  const testPanel = document.getElementById('running-test-panel');
  if (testPanel) {
    testPanel.style.display = 'block';
    console.log('✅ 테스트 패널 표시');
  }
}

/**
 * 런닝 종료
 */
async function finishRunning(isAuto = false) {
  if (!isHost && !isAuto) {
    alert('방장만 런닝을 종료할 수 있습니다.');
    return;
  }

  const confirmMessage = isAuto
      ? '목표 거리에 도달했습니다! 런닝을 종료하시겠습니까?'
      : '런닝을 종료하시겠습니까?';

  if (!confirm(confirmMessage)) {
    return;
  }

  try {
    // 1. GPS 추적 중지
    if (runningTracker) {
      runningTracker.stopTracking();
      console.log('🛑 GPS 추적 완전히 중지됨');
      runningTracker = null;
    }

    // 2. GPS 구독 해제
    if (gpsSubscription) {
      gpsSubscription.unsubscribe();
      gpsSubscription = null;
      console.log('🛑 GPS 구독 해제됨');
    }

    // 3. API 호출 - 런닝 종료 (running_result 테이블에 저장)
    const response = await fetchWithAuth(
        `/api/running/sessions/${currentSession.id}/finish`, {
          method: 'POST'
        });

    if (!response.ok) {
      const error = await response.json();
      alert(error.message || '런닝 종료 실패');
      return;
    }

    // 4. 세션 상태 업데이트
    currentSession.status = 'COMPLETED';
    updateControlBar();

    // 5. 테스트 패널 숨기기
    const testPanel = document.getElementById('running-test-panel');
    if (testPanel) {
      testPanel.style.display = 'none';
    }

    // 6. 종료 시스템 메시지
    stompClient.send('/pub/chat/message', {}, JSON.stringify({
      sessionId: currentSession.id,
      senderId: null,
      senderName: 'SYSTEM',
      content: '🏁 런닝이 종료되었습니다! 수고하셨습니다!',
      messageType: 'SYSTEM'
    }));

    // 7. 런닝 결과 모달 표시
    const result = await response.json();
    if (result.success) {
      showRunningResultModal();
    }

  } catch (error) {
    console.error('런닝 종료 에러:', error);
    alert('런닝 종료 중 오류가 발생했습니다.');
  }
}

// ============================================
// 런닝 결과 모달
// ============================================

/**
 * 런닝 결과 모달 표시
 */
function showRunningResultModal() {
  // 최종 런닝 결과 API 조회
  fetchWithAuth(`/api/running/sessions/${currentSession.id}/result`)
  .then(response => response.json())
  .then(result => {
    if (result.success && result.data) {
      const data = result.data;

      // 총 거리
      document.getElementById('result-distance').textContent =
          data.totalDistance ? data.totalDistance.toFixed(2) : '0.00';

      // 소요 시간 (초 → 분:초)
      const totalMinutes = Math.floor(data.totalTime / 60);
      const totalSeconds = data.totalTime % 60;
      document.getElementById('result-time').textContent =
          `${totalMinutes}:${String(totalSeconds).padStart(2, '0')}`;

      // 평균 페이스
      if (data.avgPace) {
        const paceMin = Math.floor(data.avgPace);
        const paceSec = Math.round((data.avgPace - paceMin) * 60);
        document.getElementById('result-pace').textContent =
            `${paceMin}:${String(paceSec).padStart(2, '0')}`;
      } else {
        document.getElementById('result-pace').textContent = '--:--';
      }

      // 구간별 페이스
      const segmentsDiv = document.getElementById('result-segments');
      segmentsDiv.innerHTML = '';

      if (data.splitPace && data.splitPace.length > 0) {
        data.splitPace.forEach(segment => {
          const segmentDiv = document.createElement('div');
          segmentDiv.className = 'segment-item';

          const kmLabel = document.createElement('span');
          kmLabel.className = 'segment-km';
          kmLabel.textContent = `${segment.km}km`;

          const paceValue = document.createElement('span');
          paceValue.className = 'segment-pace';
          const min = Math.floor(segment.pace);
          const sec = Math.round((segment.pace - min) * 60);
          paceValue.textContent = `${min}:${String(sec).padStart(2, '0')}/km`;

          segmentDiv.appendChild(kmLabel);
          segmentDiv.appendChild(paceValue);
          segmentsDiv.appendChild(segmentDiv);
        });
      } else {
        const emptyDiv = document.createElement('div');
        emptyDiv.style.cssText = 'text-align: center; color: #9CA3AF; padding: 20px; font-size: 12px;';
        emptyDiv.textContent = '구간 데이터 없음';
        segmentsDiv.appendChild(emptyDiv);
      }

      // 모달 표시
      const modal = document.getElementById('running-result-modal-overlay');
      if (modal) {
        modal.classList.add('show');
      }

    } else {
      console.error('런닝 결과 조회 실패:', result.message);
      alert('런닝 결과를 불러올 수 없습니다.');
    }
  })
  .catch(error => {
    console.error('런닝 결과 조회 에러:', error);
    alert('런닝 결과를 불러오는 중 오류가 발생했습니다.');
  });
}

/**
 * 런닝 결과 모달 닫기
 */
function closeRunningResultModal() {
  const modal = document.getElementById('running-result-modal-overlay');
  if (modal) {
    modal.classList.remove('show');
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
  console.error('❌ 서버 에러 수신:', error);
  
  // 에러 메시지 표시
  let errorMessage = error.message || '알 수 없는 오류가 발생했습니다.';
  
  // 에러 코드에 따른 추가 처리
  switch (error.errorCode) {
    case 'SESSION_NOT_FOUND':
      errorMessage += '\n세션을 찾을 수 없습니다. 페이지를 새로고침해주세요.';
      break;
    case 'USER_NOT_FOUND':
      errorMessage += '\n사용자 정보를 찾을 수 없습니다.';
      break;
    case 'INVALID_REQUEST':
      errorMessage += '\n잘못된 요청입니다.';
      break;
    case 'INTERNAL_SERVER_ERROR':
      errorMessage += '\n서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
      break;
  }
  
  alert('⚠️ GPS 추적 오류\n\n' + errorMessage);
  
  // 심각한 에러인 경우 GPS 추적 중지
  if (error.errorCode === 'SESSION_NOT_FOUND' || error.errorCode === 'INTERNAL_SERVER_ERROR') {
    if (runningTracker && runningTracker.isTracking) {
      console.log('🛑 심각한 에러로 인한 GPS 추적 중지');
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
    console.error('❌ WebSocket 연결 없음 (에러 구독)');
    return;
  }
  
  // 이미 구독 중이면 중복 구독 방지
  if (errorSubscription) {
    console.log('⚠️ 이미 런닝 에러를 구독 중입니다');
    return;
  }
  
  errorSubscription = stompClient.subscribe(
      `/sub/running/${currentSession.id}/errors`,
      function (message) {
        const error = JSON.parse(message.body);
        handleRunningError(error);
      });
  
  console.log('✅ 런닝 에러 구독 완료:', `/sub/running/${currentSession.id}/errors`);
}
