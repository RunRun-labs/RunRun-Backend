// 크루 채팅방 페이지 스크립트

let stompClient = null;
let currentUser = null;
let currentRoom = null;

// ============================================
// 인증 관련 함수
// ============================================

function getAuthHeaders() {
  const token = localStorage.getItem('accessToken');
  return {
    'Content-Type': 'application/json',
    'Authorization': token ? 'Bearer ' + token : ''
  };
}

async function fetchWithAuth(url, options = {}) {
  const headers = {
    ...getAuthHeaders(),
    ...options.headers
  };

  const response = await fetch(url, {
    ...options,
    headers
  });

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
    const response = await fetchWithAuth('/api/crew-chat/me');
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

// URL 파라미터에서 roomId 가져오기
function getUrlParams() {
  const params = new URLSearchParams(window.location.search);
  return {
    roomId: params.get('roomId')
  };
}

// ============================================
// 페이지 초기화
// ============================================

document.addEventListener('DOMContentLoaded', async function () {
  const params = getUrlParams();

  if (!params.roomId) {
    alert('채팅방 ID가 필요합니다.');
    window.location.href = '/chat';
    return;
  }

  // 1. 로그인한 사용자 정보 조회
  const user = await getCurrentUser();
  if (!user) {
    return;
  }

  currentUser = user;
  console.log('현재 사용자:', currentUser);

  // 2. 채팅방 정보 조회
  await loadRoomInfo(params.roomId);

  // 3. 화면 업데이트
  updateChatRoomUI();

  // 4. 사용자 역할 로드 및 공지사항 로드
  loadCurrentUserRole();
  loadNotices();

  // 5. WebSocket 연결
  connectWebSocket();

  // 6. 이벤트 리스너 설정
  setupEventListeners();
});

// ============================================
// 채팅방 정보 조회
// ============================================

async function loadRoomInfo(roomId) {
  try {
    const response = await fetchWithAuth(`/api/crew-chat/rooms/${roomId}`);
    const result = await response.json();

    console.log('⭐ API 응답 전체:', result);  // 디버깅

    if (result.success) {
      currentRoom = {
        id: parseInt(roomId),
        roomName: result.data.roomName,
        crewId: result.data.crewId,
        crewName: result.data.crewName,
        crewDescription: result.data.crewDescription  // ⭐ 추가
      };

      console.log('⭐ currentRoom 저장:', currentRoom);  // 디버깅
      console.log('⭐ crewDescription:', currentRoom.crewDescription);  // 디버깅
    }
  } catch (error) {
    console.error('채팅방 정보 조회 실패:', error);
    alert('채팅방 정보를 불러올 수 없습니다.');
    window.location.href = '/chat';
  }
}

// ============================================
// 채팅방 UI 업데이트
// ============================================

function updateChatRoomUI() {
  if (!currentRoom) {
    return;
  }

  // 채팅방 이름
  document.getElementById('group-name').textContent = currentRoom.roomName
      || '크루 채팅';

  // 크루 뱃지
  document.getElementById('crew-badge').textContent = `🏃 ${currentRoom.crewName
  || '크루'}`;

  // ⭐ 크루 설명 표시
  const descriptionEl = document.getElementById('crew-description');
  if (descriptionEl) {
    descriptionEl.textContent = currentRoom.crewDescription || '크루 설명 없음';
  }

  // 참여자 수 업데이트
  loadParticipants(currentRoom.id);
}

// ============================================
// 참여자 관련
// ============================================

let participantsList = [];

function loadParticipants(roomId) {
  fetchWithAuth(`/api/crew-chat/rooms/${roomId}/users`)
  .then(response => response.json())
  .then(result => {
    if (result.success) {
      participantsList = result.data;
      const count = result.data.length;
      document.getElementById(
          'participant-count').textContent = `${count}명 참여중`;

      // 크루 멤버 수 표시
      document.getElementById('crew-member-count').textContent = `${count}명`;

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
// 이벤트 리스너
// ============================================

function setupEventListeners() {
  // 뒤로가기 버튼
  const backButton = document.querySelector('.back-button');
  if (backButton) {
    backButton.addEventListener('click', function () {
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

  // ==== 공지사항 관련 ====

  // 공지사항 추가 버튼
  const noticeAddBtn = document.getElementById('notice-add-btn');
  if (noticeAddBtn) {
    noticeAddBtn.addEventListener('click', function (e) {
      e.stopPropagation();
      openCreateNoticeModal();
    });
  }

  // 공지사항 접기/펼치기 버튼
  const noticeToggleBtn = document.getElementById('notice-toggle-btn');
  if (noticeToggleBtn) {
    noticeToggleBtn.addEventListener('click', function (e) {
      e.stopPropagation();
      toggleNoticeList();
    });
  }

  // 공지사항 모달 닫기
  const noticeModalOverlay = document.getElementById('notice-modal-overlay');
  const noticeModalClose = document.getElementById('notice-modal-close');
  const noticeCancelBtn = document.getElementById('notice-cancel-btn');

  if (noticeModalClose) {
    noticeModalClose.addEventListener('click', closeNoticeModal);
  }

  if (noticeCancelBtn) {
    noticeCancelBtn.addEventListener('click', closeNoticeModal);
  }

  if (noticeModalOverlay) {
    noticeModalOverlay.addEventListener('click', function (e) {
      if (e.target === noticeModalOverlay) {
        closeNoticeModal();
      }
    });
  }

  // 공지사항 제출
  const noticeSubmitBtn = document.getElementById('notice-submit-btn');
  if (noticeSubmitBtn) {
    noticeSubmitBtn.addEventListener('click', submitNotice);
  }

  // 공지사항 textarea 글자수 카운트
  const noticeTextarea = document.getElementById('notice-textarea');
  const noticeCharCurrent = document.getElementById('notice-char-current');
  if (noticeTextarea && noticeCharCurrent) {
    noticeTextarea.addEventListener('input', function () {
      noticeCharCurrent.textContent = this.value.length;
    });
  }
}

// ============================================
// WebSocket 연결
// ============================================

function connectWebSocket() {
  console.log('⭐ connectWebSocket 시작, currentRoom:', currentRoom);  // 디버깅

  if (!currentRoom) {
    console.error('❌ currentRoom이 없습니다!');
    return;
  }

  const socket = new SockJS('/ws');
  stompClient = Stomp.over(socket);
  stompClient.debug = null;

  const token = localStorage.getItem('accessToken');
  const headers = token ? {'Authorization': 'Bearer ' + token} : {};

  stompClient.connect(headers, function (frame) {
    console.log('✅ WebSocket 연결 성공');

    // 과거 메시지 불러오기
    loadPreviousMessages();

    // 구독
    const subscribeUrl = '/sub/crew-chat/' + currentRoom.id;
    console.log('⭐ 구독 시작:', subscribeUrl);

    stompClient.subscribe(subscribeUrl, function (response) {
      console.log('📩 메시지 수신:', response.body);
      const message = JSON.parse(response.body);
      console.log('📩 파싱된 메시지:', message);
      console.log('📩 메시지 타입:', message.messageType, message.type);

      displayMessage(message);

      // 시스템 메시지 수신 시 참여자 목록 자동 갱신
      if (message.messageType === 'SYSTEM') {
        setTimeout(() => {
          loadParticipants(currentRoom.id);
        }, 300);
      }
    });

    console.log('✅ 크루 채팅방 구독 완료:', subscribeUrl);
  }, function (error) {
    console.error('❌ WebSocket 연결 실패:', error);
  });
}

// ============================================
// 메시지 관련
// ============================================

function loadPreviousMessages() {
  console.log('⭐ loadPreviousMessages 시작, currentRoom.id:', currentRoom.id);  // 디버깅
  fetchWithAuth(`/api/crew-chat/${currentRoom.id}/messages`)
  .then(response => response.json())
  .then(result => {
    console.log('⭐ 과거 메시지 응답:', result);  // 디버깅
    if (result.success && result.data.length > 0) {
      result.data.forEach(message => {
        displayMessage(message, true);
      });

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

function sendMessage() {
  const input = document.getElementById('message-input');
  const content = input.value.trim();

  if (!content || !stompClient) {
    return;
  }

  stompClient.send('/pub/crew-chat/message', {}, JSON.stringify({
    roomId: currentRoom.id,
    senderId: currentUser.id,
    senderName: currentUser.name,
    content: content,
    messageType: 'TEXT'
  }));

  input.value = '';

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

function displayMessage(message, isPrevious = false) {
  console.log('⭐ displayMessage 호출:', message);  // 디버깅
  const messagesDiv = document.getElementById('chat-messages');
  console.log('⭐ messagesDiv:', messagesDiv);  // 디버깅

  // NOTICE 메시지 (공지사항 변경 알림)
  if (message.type === 'NOTICE' || message.messageType === 'NOTICE') {
    const systemDiv = document.createElement('div');
    systemDiv.className = 'system-message';
    const p = document.createElement('p');
    p.textContent = message.message || message.content;
    systemDiv.appendChild(p);
    messagesDiv.appendChild(systemDiv);

    // 공지사항 목록 다시 로드
    loadNotices();

    // 실시간 메시지는 스크롤
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
    return;
  }

  // 시스템 메시지
  if (message.messageType === 'SYSTEM') {
    const systemDiv = document.createElement('div');
    systemDiv.className = 'system-message';
    const p = document.createElement('p');
    p.textContent = message.content;
    systemDiv.appendChild(p);
    messagesDiv.appendChild(systemDiv);

    loadParticipants(currentRoom.id);
  } else {
    const isMyMessage = message.senderId == currentUser.id;

    const messageItem = document.createElement('div');
    messageItem.className = `message-item ${isMyMessage ? 'message-right'
        : 'message-left'}`;

    if (!isMyMessage) {
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
// 채팅방 퇴장
// ============================================

function leaveChatRoom() {
  // 크루 탈퇴는 크루 설정에서만 가능하므로
  // 여기서는 단순히 채팅방 목록으로 이동
  if (stompClient !== null) {
    stompClient.disconnect();
  }
  window.location.href = '/chat';
}

// ============================================
// 참여자 목록 모달
// ============================================

function openParticipantModal() {
  const modal = document.getElementById('participant-modal-overlay');
  if (modal) {
    modal.classList.add('show');
    renderParticipantList();
    loadParticipants(currentRoom.id);
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
  const totalCountEl = document.getElementById('participant-total-count');

  if (!listContainer || !participantsList.length) {
    return;
  }

  const totalCount = participantsList.length;
  if (totalCountEl) {
    totalCountEl.textContent = totalCount;
  }

  listContainer.innerHTML = '';

  // 역할 우선순위 정의
  const roleOrder = {
    'LEADER': 1,
    'SUB_LEADER': 2,
    'STAFF': 3,
    'MEMBER': 4
  };

  // 역할 순서로 정렬
  const sortedParticipants = [...participantsList].sort((a, b) => {
    const orderA = roleOrder[a.role] || 999;
    const orderB = roleOrder[b.role] || 999;
    return orderA - orderB;
  });

  sortedParticipants.forEach(participant => {
    const isCurrentUser = participant.userId == currentUser.id;

    const item = document.createElement('div');
    item.className = 'participant-item';

    const avatarWrapper = document.createElement('div');
    avatarWrapper.className = 'participant-avatar-wrapper';

    const avatar = document.createElement('div');
    avatar.className = 'participant-avatar';

    avatar.innerHTML = '<svg class="participant-avatar-icon" width="22" height="26" viewBox="0 0 22 26" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M11 0C4.925 0 0 4.925 0 11C0 17.075 4.925 22 11 22C17.075 22 22 17.075 22 11C22 4.925 17.075 0 11 0Z" fill="#E5E7EB"/></svg>';

    avatarWrapper.appendChild(avatar);

    const info = document.createElement('div');
    info.className = 'participant-info';

    const nameRow = document.createElement('div');
    nameRow.className = 'participant-name-row';

    const name = document.createElement('span');
    name.className = 'participant-name';
    name.textContent = participant.name || '이름 없음';

    nameRow.appendChild(name);

    // 크루 역할 뱃지 추가
    if (participant.role) {
      const roleBadge = document.createElement('span');
      roleBadge.className = 'participant-role-badge';

      // 역할별 스타일 분기
      if (participant.role === 'LEADER') {
        roleBadge.classList.add('leader');
        roleBadge.textContent = '크루장';
      } else if (participant.role === 'SUB_LEADER') {
        roleBadge.classList.add('sub-leader');
        roleBadge.textContent = '부크루장';
      } else if (participant.role === 'STAFF') {
        roleBadge.classList.add('staff');
        roleBadge.textContent = '운영진';
      } else if (participant.role === 'MEMBER') {
        roleBadge.classList.add('member');
        roleBadge.textContent = '멤버';
      }

      nameRow.appendChild(roleBadge);
    }

    // 현재 사용자 뱃지 (크루 역할과 별도로 표시)
    if (isCurrentUser) {
      const meBadge = document.createElement('span');
      meBadge.className = 'participant-role-badge me';
      meBadge.textContent = '나';
      nameRow.appendChild(meBadge);
    }

    info.appendChild(nameRow);

    item.appendChild(avatarWrapper);
    item.appendChild(info);

    listContainer.appendChild(item);
  });
}

// ============================================
// 공지사항 관련
// ============================================

let currentUserRole = null; // 현재 사용자의 크루 역할
let noticesList = []; // 공지사항 목록
let editingNoticeId = null; // 수정 중인 공지사항 ID

/**
 * 공지사항 목록 로드
 */
function loadNotices() {
  if (!currentRoom) {
    return;
  }

  fetchWithAuth(`/api/crew-chat/rooms/${currentRoom.id}/notices`)
  .then(response => response.json())
  .then(result => {
    if (result.success) {
      noticesList = result.data;
      renderNotices();
      updateNoticeCount();

      // 공지사항이 있거나 운영진 이상이면 표시
      const noticeSection = document.getElementById('notice-section');
      if (noticesList.length > 0 || isStaffOrAbove()) {
        noticeSection.style.display = 'block';
      }
    }
  })
  .catch(error => console.error('공지사항 로드 실패:', error));
}

/**
 * 공지사항 렌더링
 */
function renderNotices() {
  const noticeList = document.getElementById('notice-list');

  if (noticesList.length === 0) {
    noticeList.innerHTML = '<div class="notice-empty">등록된 공지사항이 없습니다.</div>';
    return;
  }

  noticeList.innerHTML = '';

  noticesList.forEach(notice => {
    const noticeItem = document.createElement('div');
    noticeItem.className = 'notice-item';

    const header = document.createElement('div');
    header.className = 'notice-item-header';

    const author = document.createElement('div');
    author.className = 'notice-author';
    author.textContent = notice.createdByName;

    header.appendChild(author);

    // 작성자 본인이거나 운영진 이상이면 수정/삭제 버튼 표시
    if (canEditNotice(notice)) {
      const actions = document.createElement('div');
      actions.className = 'notice-item-actions';

      // 수정 버튼
      const editBtn = document.createElement('button');
      editBtn.className = 'notice-edit-btn';
      editBtn.innerHTML = '<svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M10 1L13 4L5 12H2V9L10 1Z" stroke="#92400e" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>';
      editBtn.onclick = () => openEditNoticeModal(notice);

      // 삭제 버튼
      const deleteBtn = document.createElement('button');
      deleteBtn.className = 'notice-delete-btn';
      deleteBtn.innerHTML = '<svg width="14" height="14" viewBox="0 0 14 14" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M1 3.5H13M5.5 6.5V10.5M8.5 6.5V10.5M2.5 3.5L3.5 12.5H10.5L11.5 3.5M5.5 3.5V1.5H8.5V3.5" stroke="#dc2626" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>';
      deleteBtn.onclick = () => deleteNotice(notice.id);

      actions.appendChild(editBtn);
      actions.appendChild(deleteBtn);
      header.appendChild(actions);
    }

    noticeItem.appendChild(header);

    const content = document.createElement('div');
    content.className = 'notice-content';
    content.textContent = notice.content;
    noticeItem.appendChild(content);

    const date = document.createElement('div');
    date.className = 'notice-date';
    const createdDate = new Date(notice.createdAt);
    date.textContent = formatNoticeDate(createdDate);
    noticeItem.appendChild(date);

    noticeList.appendChild(noticeItem);
  });
}

/**
 * 공지사항 개수 업데이트
 */
function updateNoticeCount() {
  const countEl = document.getElementById('notice-count');
  if (countEl) {
    countEl.textContent = noticesList.length;
  }
}

/**
 * 날짜 포맷팅
 */
function formatNoticeDate(date) {
  const now = new Date();
  const diff = now - date;
  const diffMinutes = Math.floor(diff / 60000);
  const diffHours = Math.floor(diff / 3600000);
  const diffDays = Math.floor(diff / 86400000);

  if (diffMinutes < 1) {
    return '방금 전';
  }
  if (diffMinutes < 60) {
    return `${diffMinutes}분 전`;
  }
  if (diffHours < 24) {
    return `${diffHours}시간 전`;
  }
  if (diffDays < 7) {
    return `${diffDays}일 전`;
  }

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}.${month}.${day}`;
}

/**
 * 공지사항 수정/삭제 권한 체크
 */
function canEditNotice(notice) {
  if (!currentUser || !currentUserRole) {
    return false;
  }

  // 작성자 본인
  if (notice.createdBy === currentUser.id) {
    return true;
  }

  // STAFF 이상
  return isStaffOrAbove();
}

/**
 * STAFF 이상 권한 체크
 */
function isStaffOrAbove() {
  return currentUserRole === 'LEADER' ||
      currentUserRole === 'SUB_LEADER' ||
      currentUserRole === 'STAFF';
}

/**
 * 공지사항 작성 모달 열기
 */
function openCreateNoticeModal() {
  editingNoticeId = null;
  document.getElementById('notice-modal-title').textContent = '공지사항 작성';
  document.getElementById('notice-textarea').value = '';
  document.getElementById('notice-char-current').textContent = '0';
  document.getElementById('notice-submit-btn').textContent = '등록';
  document.getElementById('notice-modal-overlay').classList.add('show');
}

/**
 * 공지사항 수정 모달 열기
 */
function openEditNoticeModal(notice) {
  editingNoticeId = notice.id;
  document.getElementById('notice-modal-title').textContent = '공지사항 수정';
  document.getElementById('notice-textarea').value = notice.content;
  document.getElementById(
      'notice-char-current').textContent = notice.content.length;
  document.getElementById('notice-submit-btn').textContent = '수정';
  document.getElementById('notice-modal-overlay').classList.add('show');
}

/**
 * 공지사항 모달 닫기
 */
function closeNoticeModal() {
  document.getElementById('notice-modal-overlay').classList.remove('show');
  editingNoticeId = null;
}

/**
 * 공지사항 제출 (작성/수정)
 */
function submitNotice() {
  const content = document.getElementById('notice-textarea').value.trim();

  if (!content) {
    alert('공지사항 내용을 입력해주세요.');
    return;
  }

  if (content.length > 500) {
    alert('공지사항은 500자 이내로 작성해주세요.');
    return;
  }

  const reqDto = {content};

  if (editingNoticeId) {
    // 수정
    fetchWithAuth(`/api/crew-chat/notices/${editingNoticeId}`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify(reqDto)
    })
    .then(response => response.json())
    .then(result => {
      if (result.success) {
        closeNoticeModal();
        loadNotices();
      } else {
        alert(result.message || '공지사항 수정에 실패했습니다.');
      }
    })
    .catch(error => {
      console.error('공지사항 수정 실패:', error);
      alert('공지사항 수정에 실패했습니다.');
    });
  } else {
    // 작성
    fetchWithAuth(`/api/crew-chat/rooms/${currentRoom.id}/notices`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(reqDto)
    })
    .then(response => response.json())
    .then(result => {
      if (result.success) {
        closeNoticeModal();
        loadNotices();
      } else {
        alert(result.message || '공지사항 작성에 실패했습니다.');
      }
    })
    .catch(error => {
      console.error('공지사항 작성 실패:', error);
      alert('공지사항 작성에 실패했습니다.');
    });
  }
}

/**
 * 공지사항 삭제
 */
function deleteNotice(noticeId) {
  if (!confirm('공지사항을 삭제하시겠습니까?')) {
    return;
  }

  fetchWithAuth(`/api/crew-chat/notices/${noticeId}`, {
    method: 'DELETE'
  })
  .then(response => response.json())
  .then(result => {
    if (result.success) {
      loadNotices();
    } else {
      alert(result.message || '공지사항 삭제에 실패했습니다.');
    }
  })
  .catch(error => {
    console.error('공지사항 삭제 실패:', error);
    alert('공지사항 삭제에 실패했습니다.');
  });
}

/**
 * 공지사항 접기/펼치기
 */
function toggleNoticeList() {
  const noticeList = document.getElementById('notice-list');
  const toggleBtn = document.getElementById('notice-toggle-btn');
  const toggleText = toggleBtn.querySelector('.toggle-text');

  noticeList.classList.toggle('collapsed');
  toggleBtn.classList.toggle('collapsed');

  // 텍스트 변경
  if (noticeList.classList.contains('collapsed')) {
    toggleText.textContent = '펼치기';
  } else {
    toggleText.textContent = '접기';
  }
}

/**
 * 현재 사용자의 크루 역할 조회
 */
function loadCurrentUserRole() {
  if (!currentRoom || !currentUser) {
    return;
  }

  fetchWithAuth(`/api/crew-chat/rooms/${currentRoom.id}/users`)
  .then(response => response.json())
  .then(result => {
    if (result.success) {
      const currentUserData = result.data.find(
          u => u.userId === currentUser.id);
      if (currentUserData) {
        currentUserRole = currentUserData.role;

        // STAFF 이상이면 공지 추가 버튼 표시
        if (isStaffOrAbove()) {
          document.getElementById('notice-add-btn').style.display = 'flex';
        }
      }
    }
  })
  .catch(error => console.error('사용자 역할 조회 실패:', error));
}
