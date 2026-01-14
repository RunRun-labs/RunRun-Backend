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

    const response = await fetch('/api/chat/all-rooms', {  // ⭐ 통합 API로 변경
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
      
      // ✅ 크루 채팅방 데이터 확인
      console.log('=== 채팅방 목록 로드 ===');
      console.log('전체 채팅방 수:', chatRooms.length);
      const crewRooms = chatRooms.filter(r => r.chatType === 'CREW');
      console.log('크루 채팅방 수:', crewRooms.length);
      crewRooms.forEach(room => {
        console.log('크루:', room.crewName);
        console.log('  - crewImageUrl:', room.crewImageUrl);
        console.log('  - chatRoomId:', room.chatRoomId);
      });
      
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
    return meetingTime > now && room.sessionStatus === 'STANDBY';
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

  // 카드 보이기
  const card = document.querySelector('.next-running-card');
  if (card) card.style.display = 'flex';

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
  const offlineCount = chatRooms.filter(r => r.chatType === 'OFFLINE').length;
  const crewCount = chatRooms.filter(r => r.chatType === 'CREW').length;

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
        case 'crew':
          countEl.textContent = crewCount;
          break;
      }
    }
  });
}

// 채팅방 리스트 렌더링 (오프라인 + 크루 통합)
function renderChatList(chatRooms) {
  const listContainer = document.getElementById('chat-list');
  if (!listContainer) return;

  listContainer.innerHTML = '';

  if (!chatRooms || chatRooms.length === 0) {
    listContainer.innerHTML = '<div style="padding: 40px; text-align: center; color: #9CA3AF;">참여 중인 채팅방이 없습니다.</div>';
    return;
  }

  chatRooms.forEach(room => {
    // ⭐ chatType에 따라 다르게 렌더링
    if (room.chatType === 'CREW') {
      renderCrewChatItem(listContainer, room);
    } else {
      renderOfflineChatItem(listContainer, room);
    }
  });
}

// ⭐ 크루 채팅방 아이템 렌더링
function renderCrewChatItem(container, room) {
  console.log('=== renderCrewChatItem 호출 ===');
  console.log('크루명:', room.crewName);
  console.log('crewImageUrl:', room.crewImageUrl);
  console.log('room 객체 전체:', room);
  
  const item = document.createElement('a');
  item.className = 'chat-item';
  item.href = `/chat/crew?roomId=${room.chatRoomId}`;  // ⭐ 경로 수정

  // ✅ 크루 이미지 아바타
  const avatar = document.createElement('div');
  avatar.className = 'chat-avatar';
  
  const avatarImg = document.createElement('img');
  // ⭐ null, undefined, 빈 문자열 모두 처리
  const imageUrl = (room.crewImageUrl && room.crewImageUrl.trim()) 
    ? room.crewImageUrl 
    : '/img/default-crew.svg';  // ✅ 크루 전용 디폴트 이미지
  console.log('✅ 크루 이미지 URL:', room.crewImageUrl, '→', imageUrl);
  
  avatarImg.src = imageUrl;
  avatarImg.alt = room.crewName || '크루';
  avatarImg.style.cssText = 'width: 100%; height: 100%; object-fit: cover; border-radius: 50%;';
  avatarImg.onerror = function() {
    console.log('❌ 이미지 로드 실패:', this.src);
    this.src = '/img/default-crew.svg';  // ✅ 크루 디폴트로 폴백
  };
  avatarImg.onload = function() {
    console.log('✅ 이미지 로드 성공:', this.src);
  };
  avatar.appendChild(avatarImg);

  // 콘텐츠
  const content = document.createElement('div');
  content.className = 'chat-content';

  // 헤더
  const headerRow = document.createElement('div');
  headerRow.className = 'chat-header-row';

  const titleRow = document.createElement('div');
  titleRow.className = 'chat-title-row';

  const title = document.createElement('span');
  title.className = 'chat-title';
  title.textContent = room.chatRoomTitle || '제목 없음';
  titleRow.appendChild(title);

  // 크루 뱃지
  const crewBadge = document.createElement('span');
  crewBadge.className = 'chat-status-badge scheduled';
  crewBadge.textContent = '크루';
  crewBadge.style.backgroundColor = '#10B981';  // 녹색
  titleRow.appendChild(crewBadge);

  headerRow.appendChild(titleRow);

  // 시간
  const time = document.createElement('span');
  time.className = 'chat-time';
  time.textContent = formatTime(room.lastMessageTime);
  headerRow.appendChild(time);

  content.appendChild(headerRow);

  // 크루 정보 태그
  const tags = document.createElement('div');
  tags.className = 'chat-tags';
  
  if (room.crewDescription) {
    const descTag = document.createElement('span');
    descTag.className = 'chat-tag';
    descTag.textContent = room.crewDescription;
    tags.appendChild(descTag);
  }

  const memberTag = document.createElement('span');
  memberTag.className = 'chat-tag';
  memberTag.textContent = `👥 ${room.currentParticipants}명`;
  tags.appendChild(memberTag);

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
  container.appendChild(item);
}

// ⭐ 오프라인 채팅방 아이템 렌더링 (기존 로직)
function renderOfflineChatItem(container, room) {
    const item = document.createElement('a');
    item.className = 'chat-item';
    item.href = `/chat/chat1?sessionId=${room.chatRoomId}`;  // ⭐ sessionId → chatRoomId

    // 아바타 (오프라인 러닝 디폴트 이미지)
    const avatar = document.createElement('div');
    avatar.className = 'chat-avatar';
    
    const avatarImg = document.createElement('img');
    avatarImg.src = '/img/default-offline.svg';  // ✅ 오프라인 전용 디폴트 이미지
    avatarImg.alt = '오프라인 러닝';
    avatarImg.style.cssText = 'width: 100%; height: 100%; object-fit: cover; border-radius: 50%;';
    avatar.appendChild(avatarImg);

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
    title.textContent = room.chatRoomTitle || '제목 없음';  // ⭐ title → chatRoomTitle
    titleRow.appendChild(title);

    // 상태 뱃지
    if (room.sessionStatus === 'STANDBY') {
      const statusBadge = document.createElement('span');
      statusBadge.className = 'chat-status-badge scheduled';
      statusBadge.textContent = '대기중';
      titleRow.appendChild(statusBadge);
    } else if (room.sessionStatus === 'IN_PROGRESS') {
      const statusBadge = document.createElement('span');
      statusBadge.className = 'chat-status-badge scheduled';
      statusBadge.textContent = '러닝 중';
      titleRow.appendChild(statusBadge);
    } else if (room.sessionStatus === 'COMPLETED') {
      const statusBadge = document.createElement('span');
      statusBadge.className = 'chat-status-badge scheduled';
      statusBadge.textContent = '러닝 종료';
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
    container.appendChild(item);
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
      filteredRooms = chatRooms.filter(room => room.chatType === 'OFFLINE');
      break;
    case 'crew':
      filteredRooms = chatRooms.filter(room => room.chatType === 'CREW');
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
    const title = room.chatRoomTitle || '';  // ⭐ title → chatRoomTitle
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
      // ⭐ chatType에 따라 다르게 구독
      if (room.chatType === 'CREW') {
        subscribeToCrewChat(room.chatRoomId);  // 크루 채팅
      } else {
        subscribeToOfflineChat(room.chatRoomId);  // ⭐ sessionId → chatRoomId
      }
    });
  }, function(error) {
    console.error('WebSocket 연결 실패:', error);
  });
}

// 오프라인 채팅방 구독
function subscribeToOfflineChat(sessionId) {
  if (!stompClient || !stompClient.connected) return;

  stompClient.subscribe('/sub/chat/' + sessionId, function(response) {
    const message = JSON.parse(response.body);
    
    // 새 메시지 수신 시 해당 채팅방의 unreadCount 증가
    handleNewOfflineMessage(sessionId, message);
  });
}

// ⭐ 크루 채팅방 구독
function subscribeToCrewChat(roomId) {
  if (!stompClient || !stompClient.connected) return;

  stompClient.subscribe('/sub/crew-chat/' + roomId, function(response) {
    const message = JSON.parse(response.body);
    console.log('⭐ 채팅방 목록: 크루 메시지 수신 roomId=' + roomId, message);
    
    // 새 메시지 수신 시 해당 채팅방의 unreadCount 증가
    handleNewCrewMessage(roomId, message);
  });
}

// 오프라인 채팅 새 메시지 처리
function handleNewOfflineMessage(sessionId, message) {
  // chatRooms 배열에서 해당 채팅방 찾기 (⭐ chatRoomId로 찾기)
  const roomIndex = chatRooms.findIndex(room => room.chatRoomId === sessionId);
  if (roomIndex === -1) return;

  const room = chatRooms[roomIndex];

  // unreadCount 증가
  room.unreadCount = (room.unreadCount || 0) + 1;

  // 최근 메시지 업데이트
  room.lastMessageContent = message.content;
  room.lastMessageSender = message.senderName;
  room.lastMessageTime = message.createdAt || new Date().toISOString();

  // ⭐ 준비 상태 변경 메시지 감지
  if (message.messageType === 'SYSTEM' && 
      (message.content.includes('준비완료') || message.content.includes('준비를 취소'))) {
    console.log('⭐ 채팅방 목록: 준비 상태 변경 감지 -', message.content);
    updateRoomReadyCount(sessionId);
  }

  // ⭐ 입장/퇴장 메시지 감지 (참여자 수 변경)
  if (message.messageType === 'SYSTEM' && 
      (message.content.includes('입장했습니다') || 
       message.content.includes('퇴장했습니다') ||
       message.content.includes('강퇴되었습니다'))) {
    console.log('⭐ 채팅방 목록: 참여자 변경 감지 -', message.content);
    updateRoomReadyCount(sessionId);
  }

  // 채팅방 목록 재정렬 (최신 메시지가 맨 위로)
  chatRooms.sort((a, b) => {
    const timeA = a.lastMessageTime ? new Date(a.lastMessageTime) : null;
    const timeB = b.lastMessageTime ? new Date(b.lastMessageTime) : null;
    
    if (!timeA && !timeB) return 0;
    if (!timeA) return 1;  // null은 뒤로
    if (!timeB) return -1;
    
    return timeB - timeA;  // 내림차순 (최신이 먼저)
  });

  // 전체 목록 다시 렌더링 (정렬된 순서로)
  const activeFilter = document.querySelector('.filter-btn.active');
  const filter = activeFilter ? activeFilter.dataset.filter : 'all';
  filterChatList(filter);
}

// ⭐ 크루 채팅 새 메시지 처리
function handleNewCrewMessage(roomId, message) {
  // chatRooms 배열에서 해당 채팅방 찾기
  const roomIndex = chatRooms.findIndex(room => room.chatRoomId === roomId);
  if (roomIndex === -1) return;

  const room = chatRooms[roomIndex];

  // unreadCount 증가
  room.unreadCount = (room.unreadCount || 0) + 1;

  // 최근 메시지 업데이트
  room.lastMessageContent = message.content;
  room.lastMessageSender = message.senderName;
  room.lastMessageTime = message.createdAt || new Date().toISOString();

  console.log('⭐ 채팅방 목록: 크루 채팅 업데이트 roomId=' + roomId + ', unreadCount=' + room.unreadCount);

  // 채팅방 목록 재정렬 (최신 메시지가 맨 위로)
  chatRooms.sort((a, b) => {
    const timeA = a.lastMessageTime ? new Date(a.lastMessageTime) : null;
    const timeB = b.lastMessageTime ? new Date(b.lastMessageTime) : null;
    
    if (!timeA && !timeB) return 0;
    if (!timeA) return 1;
    if (!timeB) return -1;
    
    return timeB - timeA;
  });

  // 전체 목록 다시 렌더링 (정렬된 순서로)
  const activeFilter = document.querySelector('.filter-btn.active');
  const filter = activeFilter ? activeFilter.dataset.filter : 'all';
  filterChatList(filter);
}

// 특정 채팅방 UI 업데이트
function updateChatRoomUI(room) {
  const chatItem = document.querySelector(`a.chat-item[href="/chat/chat1?sessionId=${room.chatRoomId}"]`);  // ⭐ chatRoomId
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

  // ⭐ 준비 상태 배지 업데이트
  const readyBadge = chatItem.querySelector('.chat-ready-badge');
  if (readyBadge) {
    readyBadge.textContent = `${room.readyCount}/${room.currentParticipants} 준비`;
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

// ⭐ 특정 채팅방의 준비 상태 API 재호출
async function updateRoomReadyCount(sessionId) {
  try {
    const accessToken = localStorage.getItem('accessToken');
    if (!accessToken) return;

    // 참여자 목록 API 호출
    const response = await fetch(`/api/chat/sessions/${sessionId}/users`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
      }
    });

    if (!response.ok) return;

    const result = await response.json();
    if (!result.success) return;

    const participants = result.data;
    const readyCount = participants.filter(p => p.isReady).length;
    const currentParticipants = participants.length;

    // chatRooms 배열에서 해당 방 찾아서 업데이트 (⭐ chatRoomId로 찾기)
    const roomIndex = chatRooms.findIndex(room => room.chatRoomId === sessionId);
    if (roomIndex !== -1) {
      chatRooms[roomIndex].readyCount = readyCount;
      chatRooms[roomIndex].currentParticipants = currentParticipants;
      
      console.log(`⭐ 채팅방 목록: 준비 상태 업데이트 sessionId=${sessionId}, ${readyCount}/${currentParticipants}`);
      
      // UI 업데이트
      updateChatRoomUI(chatRooms[roomIndex]);
    }
  } catch (error) {
    console.error('채팅방 목록: 준비 상태 업데이트 실패:', error);
  }
}
