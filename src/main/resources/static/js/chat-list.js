// 채팅 리스트 페이지 스크립트

let stompClient = null;
let chatRooms = []; // 전역으로 채팅방 데이터 관리

document.addEventListener('DOMContentLoaded', function() {
  // 필터 버튼 이벤트
  const filterButtons = document.querySelectorAll('.filter-btn');
  filterButtons.forEach(btn => {
    btn.addEventListener('click', function() {
      filterButtons.forEach(b => b.classList.remove('active'));
      this.classList.add('active');
      
      const filter = this.dataset.filter;
      filterChatList(filter);
    });
  });

  // 검색 기능
  const searchInput = document.querySelector('.search-input');
  if (searchInput) {
    searchInput.addEventListener('input', function(e) {
      const query = e.target.value.trim();
      searchChatList(query);
    });
  }

  // 채팅방 리스트 로드
  loadChatList();

  // 페이지 나갈 때 WebSocket 연결 해제
  window.addEventListener('beforeunload', function() {
    if (stompClient && stompClient.connected) {
      stompClient.disconnect();
    }
  });
});

// 채팅방 리스트 로드 (실제 API 호출)
async function loadChatList() {
  try {
    const accessToken = localStorage.getItem('accessToken');
    
    if (!accessToken) {
      alert('로그인이 필요합니다.');
      window.location.href = '/login';
      return;
    }

    const response = await fetch('/api/chat/rooms', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
      },
      credentials: 'include'
    });

    if (!response.ok) {
      if (response.status === 401) {
        alert('로그인이 필요합니다.');
        window.location.href = '/login';
        return;
      }
      throw new Error('채팅방 목록을 불러오는데 실패했습니다.');
    }

    const result = await response.json();
    
    if (result.success) {
      chatRooms = result.data; // 전역 변수에 저장
      renderChatList(chatRooms);
      updateNextRunning(chatRooms);
      updateFilterCounts(chatRooms);
      
      // WebSocket 연결 및 모든 채팅방 구독
      connectWebSocket();
    } else {
      console.error('채팅방 목록 로드 실패:', result.message);
      alert(result.message);
    }
  } catch (error) {
    console.error('채팅방 목록 로드 에러:', error);
    alert('채팅방 목록을 불러오는데 실패했습니다.');
  }
}

// 다음 러닝 정보 업데이트
function updateNextRunning(chatRooms) {
  if (!chatRooms || chatRooms.length === 0) {
    // 채팅방이 없으면 카드 숨기기
    const card = document.querySelector('.next-running-card');
    if (card) card.style.display = 'none';
    return;
  }

  // 가장 가까운 미래의 모임 찾기
  const now = new Date();
  const upcomingRooms = chatRooms.filter(room => {
    if (!room.meetingAt) return false;
    const meetingTime = new Date(room.meetingAt);
    return meetingTime > now && room.sessionStatus === 'WAITING';
  });

  if (upcomingRooms.length === 0) {
    const card = document.querySelector('.next-running-card');
    if (card) card.style.display = 'none';
    return;
  }

  const nextRoom = upcomingRooms[0]; // 이미 meetingAt 기준 정렬되어 있음
  const meetingTime = new Date(nextRoom.meetingAt);
  const timeDiff = meetingTime - now;
  
  // 시간 계산
  const hours = Math.floor(timeDiff / (1000 * 60 * 60));
  const minutes = Math.floor((timeDiff % (1000 * 60 * 60)) / (1000 * 60));
  
  const timeText = hours > 0 
    ? `${hours}시간 ${minutes}분` 
    : `${minutes}분`;

  // 카드 업데이트
  const timeEl = document.querySelector('.next-running-time');
  const infoEl = document.querySelector('.next-running-info');
  const distanceEl = document.querySelector('.next-running-distance');

  if (timeEl) timeEl.textContent = timeText;
  if (infoEl) infoEl.textContent = nextRoom.meetingPlace || '장소 미정';
  if (distanceEl) distanceEl.textContent = nextRoom.targetDistance ? `${nextRoom.targetDistance}km` : '';
}

// 필터 카운트 업데이트
function updateFilterCounts(chatRooms) {
  const allCount = chatRooms.length;
  const offlineCount = chatRooms.length; // 현재 모두 오프라인
  const onlineCount = 0; // TODO: 온라인 배틀 구분 필요
  const crewCount = 0; // TODO: 크루 구분 필요

  const filterButtons = document.querySelectorAll('.filter-btn');
  filterButtons.forEach(btn => {
    const filter = btn.dataset.filter;
    const countEl = btn.querySelector('.filter-count');
    if (countEl) {
      switch(filter) {
        case 'all':
          countEl.textContent = allCount;
          break;
        case 'offline':
          countEl.textContent = offlineCount;
          break;
        case 'online':
          countEl.textContent = onlineCount;
          break;
        case 'crew':
          countEl.textContent = crewCount;
          break;
      }
    }
  });
}

// 채팅방 리스트 렌더링 (백엔드 데이터 구조에 맞춤)
function renderChatList(chatRooms) {
  const listContainer = document.getElementById('chat-list');
  if (!listContainer) return;

  listContainer.innerHTML = '';

  if (!chatRooms || chatRooms.length === 0) {
    listContainer.innerHTML = '<div style="padding: 40px; text-align: center; color: #9CA3AF;">참여 중인 채팅방이 없습니다.</div>';
    return;
  }

  chatRooms.forEach(room => {
    const item = document.createElement('a');
    item.className = 'chat-item';
    item.href = `/chat/chat1?sessionId=${room.sessionId}`;

    // 아바타
    const avatar = document.createElement('div');
    avatar.className = 'chat-avatar avatar-gray';
    avatar.innerHTML = '<svg class="chat-avatar-icon" width="24" height="29" viewBox="0 0 24 29" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M12 0C5.373 0 0 5.373 0 12C0 18.627 5.373 24 12 24C18.627 24 24 18.627 24 12C24 5.373 18.627 0 12 0Z" fill="#E5E7EB"/></svg>';

    // 콘텐츠
    const content = document.createElement('div');
    content.className = 'chat-content';

    // 헤더 행
    const headerRow = document.createElement('div');
    headerRow.className = 'chat-header-row';

    const titleRow = document.createElement('div');
    titleRow.className = 'chat-title-row';

    const title = document.createElement('span');
    title.className = 'chat-title';
    title.textContent = room.title || '제목 없음';
    titleRow.appendChild(title);

    // 상태 뱃지
    if (room.sessionStatus === 'WAITING') {
      const statusBadge = document.createElement('span');
      statusBadge.className = 'chat-status-badge scheduled';
      statusBadge.textContent = '예정';
      titleRow.appendChild(statusBadge);
    } else if (room.sessionStatus === 'IN_PROGRESS') {
      const statusBadge = document.createElement('span');
      statusBadge.className = 'chat-status-badge scheduled';
      statusBadge.textContent = '진행중';
      titleRow.appendChild(statusBadge);
    }

    headerRow.appendChild(titleRow);

    // 시간 표시
    const time = document.createElement('span');
    time.className = 'chat-time';
    time.textContent = formatTime(room.lastMessageTime);
    headerRow.appendChild(time);

    content.appendChild(headerRow);

    // 태그 (모임 시간, 장소, 거리)
    const tags = document.createElement('div');
    tags.className = 'chat-tags';
    
    if (room.meetingAt) {
      const meetingTag = document.createElement('span');
      meetingTag.className = 'chat-tag';
      meetingTag.textContent = formatMeetingTime(room.meetingAt);
      tags.appendChild(meetingTag);
    }

    if (room.meetingPlace) {
      const placeTag = document.createElement('span');
      placeTag.className = 'chat-tag';
      placeTag.textContent = `📍 ${room.meetingPlace}`;
      tags.appendChild(placeTag);
    }

    if (room.targetDistance) {
      const distanceTag = document.createElement('span');
      distanceTag.className = 'chat-tag large';
      distanceTag.textContent = `${room.targetDistance}km`;
      tags.appendChild(distanceTag);
    }

    content.appendChild(tags);

    // 푸터
    const footer = document.createElement('div');
    footer.className = 'chat-footer';

    const message = document.createElement('div');
    message.className = 'chat-message';
    message.textContent = room.lastMessageContent 
      ? `${room.lastMessageSender}: ${room.lastMessageContent}` 
      : '메시지가 없습니다.';
    footer.appendChild(message);

    const footerRight = document.createElement('div');
    footerRight.className = 'chat-footer-right';

    // 준비 상태 뱃지
    const readyBadge = document.createElement('span');
    readyBadge.className = 'chat-ready-badge';
    readyBadge.textContent = `${room.readyCount}/${room.currentParticipants} 준비`;
    footerRight.appendChild(readyBadge);

    // 읽지 않은 메시지
    if (room.unreadCount && room.unreadCount > 0) {
      const unreadBadge = document.createElement('div');
      unreadBadge.className = 'chat-unread-badge';
      if (room.unreadCount > 9) {
        unreadBadge.classList.add('small');
      }
      unreadBadge.textContent = room.unreadCount > 99 ? '99+' : room.unreadCount;
      footerRight.appendChild(unreadBadge);
    }

    footer.appendChild(footerRight);
    content.appendChild(footer);

    item.appendChild(avatar);
    item.appendChild(content);
    listContainer.appendChild(item);
  });
}

// 모임 시간 포맷팅
function formatMeetingTime(meetingAt) {
  const date = new Date(meetingAt);
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const tomorrow = new Date(today);
  tomorrow.setDate(tomorrow.getDate() + 1);
  const meetingDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());

  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');
  const timeStr = `${hours}:${minutes}`;

  if (meetingDate.getTime() === today.getTime()) {
    return `오늘 ${timeStr}`;
  } else if (meetingDate.getTime() === tomorrow.getTime()) {
    return `내일 ${timeStr}`;
  } else {
    const month = date.getMonth() + 1;
    const day = date.getDate();
    return `${month}/${day} ${timeStr}`;
  }
}

// 시간 포맷팅 (방금, 5분 전, 1시간 전, 어제 등)
function formatTime(lastMessageTime) {
  if (!lastMessageTime) return '';

  const now = new Date();
  const messageTime = new Date(lastMessageTime);
  const diffMs = now - messageTime;
  const diffMins = Math.floor(diffMs / (1000 * 60));
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffMins < 1) return '방금';
  if (diffMins < 60) return `${diffMins}분 전`;
  if (diffHours < 24) return `${diffHours}시간 전`;
  if (diffDays === 1) return '어제';
  if (diffDays < 7) return `${diffDays}일 전`;
  
  const month = messageTime.getMonth() + 1;
  const day = messageTime.getDate();
  return `${month}/${day}`;
}

// 필터링
function filterChatList(filter) {
  let filteredRooms = [];
  
  switch(filter) {
    case 'all':
      filteredRooms = chatRooms;
      break;
    case 'offline':
      // OFFLINE 타입만 필터링 (sessionStatus가 WAITING 또는 IN_PROGRESS)
      filteredRooms = chatRooms;
      break;
    case 'online':
      // 온라인 배틀은 현재 구현되지 않음
      filteredRooms = [];
      break;
    case 'crew':
      // 크루는 현재 구현되지 않음
      filteredRooms = [];
      break;
    default:
      filteredRooms = chatRooms;
  }
  
  renderChatList(filteredRooms);
}

// 검색
function searchChatList(query) {
  if (!query || query.trim() === '') {
    // 검색어가 비어있으면 전체 목록 표시
    const activeFilter = document.querySelector('.filter-btn.active');
    const filter = activeFilter ? activeFilter.dataset.filter : 'all';
    filterChatList(filter);
    return;
  }

  // 제목, 장소, 최근 메시지에서 검색
  const searchResults = chatRooms.filter(room => {
    const title = room.title || '';
    const place = room.meetingPlace || '';
    const lastMessage = room.lastMessageContent || '';
    
    const searchText = query.toLowerCase();
    return title.toLowerCase().includes(searchText) ||
           place.toLowerCase().includes(searchText) ||
           lastMessage.toLowerCase().includes(searchText);
  });

  renderChatList(searchResults);
}

// ============================================
// WebSocket 실시간 업데이트
// ============================================

// WebSocket 연결
function connectWebSocket() {
  if (stompClient && stompClient.connected) {
    console.log('이미 WebSocket이 연결되어 있습니다.');
    return;
  }

  const socket = new SockJS('/ws');
  stompClient = Stomp.over(socket);
  stompClient.debug = null; // 디버그 로그 비활성화

  // JWT 토큰을 WebSocket 헤더에 포함
  const token = localStorage.getItem('accessToken');
  const headers = token ? { 'Authorization': 'Bearer ' + token } : {};

  stompClient.connect(headers, function(frame) {
    console.log('WebSocket 연결 성공 (채팅방 목록)');

    // 모든 참여 중인 채팅방 구독
    chatRooms.forEach(room => {
      subscribeToChat(room.sessionId);
    });
  }, function(error) {
    console.error('WebSocket 연결 실패:', error);
  });
}

// 특정 채팅방 구독
function subscribeToChat(sessionId) {
  if (!stompClient || !stompClient.connected) return;

  stompClient.subscribe('/sub/chat/' + sessionId, function(response) {
    const message = JSON.parse(response.body);
    
    // 새 메시지 수신 시 해당 채팅방의 unreadCount 증가
    handleNewMessage(sessionId, message);
  });
}

// 새 메시지 처리
function handleNewMessage(sessionId, message) {
  // chatRooms 배열에서 해당 채팅방 찾기
  const roomIndex = chatRooms.findIndex(room => room.sessionId === sessionId);
  if (roomIndex === -1) return;

  const room = chatRooms[roomIndex];

  // unreadCount 증가
  room.unreadCount = (room.unreadCount || 0) + 1;

  // 최근 메시지 업데이트
  room.lastMessageContent = message.content;
  room.lastMessageSender = message.senderName;
  room.lastMessageTime = message.createdAt || new Date().toISOString();

  // UI 업데이트 (해당 채팅방만)
  updateChatRoomUI(room);
}

// 특정 채팅방 UI 업데이트
function updateChatRoomUI(room) {
  const chatItem = document.querySelector(`a.chat-item[href="/chat/chat1?sessionId=${room.sessionId}"]`);
  if (!chatItem) return;

  // 최근 메시지 업데이트
  const messageEl = chatItem.querySelector('.chat-message');
  if (messageEl) {
    messageEl.textContent = room.lastMessageContent 
      ? `${room.lastMessageSender}: ${room.lastMessageContent}` 
      : '메시지가 없습니다.';
  }

  // 시간 업데이트
  const timeEl = chatItem.querySelector('.chat-time');
  if (timeEl) {
    timeEl.textContent = formatTime(room.lastMessageTime);
  }

  // unreadCount 뱃지 업데이트
  const footerRight = chatItem.querySelector('.chat-footer-right');
  if (!footerRight) return;

  // 기존 뱃지 제거
  const existingBadge = footerRight.querySelector('.chat-unread-badge');
  if (existingBadge) {
    existingBadge.remove();
  }

  // 새 뱃지 추가
  if (room.unreadCount && room.unreadCount > 0) {
    const unreadBadge = document.createElement('div');
    unreadBadge.className = 'chat-unread-badge';
    if (room.unreadCount > 9) {
      unreadBadge.classList.add('small');
    }
    unreadBadge.textContent = room.unreadCount > 99 ? '99+' : room.unreadCount;
    footerRight.appendChild(unreadBadge);
  }
}
