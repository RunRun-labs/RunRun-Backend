// ============================
// 전역 변수
// ============================
let currentUserId = null;
let crewId = null;
let currentUserRole = null;

// ============================
// 초기화
// ============================
document.addEventListener('DOMContentLoaded', async function () {
    // URL 경로에서 crewId 추출
    const pathParts = window.location.pathname.split('/');
    const crewIdIndex = pathParts.indexOf('crews');
    if (crewIdIndex !== -1 && pathParts.length > crewIdIndex + 1) {
        crewId = pathParts[crewIdIndex + 1];
    }

    // URL 쿼리 파라미터에서도 확인
    if (!crewId) {
        const urlParams = new URLSearchParams(window.location.search);
        crewId = urlParams.get('crewId');
    }

    if (!crewId) {
        showError('크루 ID를 찾을 수 없습니다..');
        return;
    }

    // 권한 확인 및 멤버 로드 추가
    const hasPermission = await checkCrewMemberPermission();

    if (hasPermission) {
        // 크루원이면 목록 로드
        await fetchMembers();
    }

    // 로컬스토리지에서 현재 사용자 ID 가져오기
    try {
        const userId = localStorage.getItem('userId');
        if (userId) {
            currentUserId = parseInt(userId);
        }
    } catch (error) {
        console.warn('사용자 ID를 가져오는데 실패했습니다:', error);
    }

    // 드롭다운 닫기 이벤트
    document.addEventListener('click', closeAllDropdowns);
});

// ============================
// API 호출
// ============================
async function fetchMembers() {
    showLoading();

    try {
        const token = getAccessToken();

        if (!token) {
            throw new Error('로그인이 필요합니다.');
        }

        const response = await fetch(`/api/crews/${crewId}/users`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || '크루원 목록을 불러오는데 실패했습니다.');
        }

        const result = await response.json();
        const members = result.data || result;

        renderMembers(members);
        hideLoading();

    } catch (error) {
        console.error('크루원 목록 조회 실패:', error);
        showError(error.message);
    }
}

// ============================
// 렌더링
// ============================
function renderMembers(members) {
    // 총 인원 표시
    document.getElementById('total-count').textContent = members.length;

    // 현재 사용자의 역할 저장
    const currentMember = members.find(member => member.userId === currentUserId);
    if (currentMember) {
        currentUserRole = currentMember.role;
    }

    // 크루장과 일반 크루원 분리
    const leader = members.find(member => member.role === 'LEADER');
    const otherMembers = members.filter(member => member.role !== 'LEADER');

    // 크루장 렌더링
    if (leader) {
        document.getElementById('leader-section').style.display = 'block';
        document.getElementById('leader-container').innerHTML = createLeaderCard(leader);
    } else {
        document.getElementById('leader-section').style.display = 'none';
    }

    // 일반 크루원 렌더링
    const membersHTML = otherMembers.map(member => createMemberCard(member)).join('');
    document.getElementById('members-container').innerHTML = membersHTML;

    // 메인 컨테이너 표시
    document.getElementById('main-container').style.display = 'block';
}

// 크루장 카드 생성
function createLeaderCard(member) {
    const avatar = createAvatar(member, true);
    const joinDate = formatDate(member.createdAt);
    const stats = createStatsSection(member);

    // 본인 여부 확인
    const isMyself = member.userId === currentUserId;

    let actionSection = '';
    if (isMyself) {
        actionSection = `
            <div class="member-right">
                ${stats}
                <button class="leave-button" onclick="handleLeave(${member.userId})">탈퇴</button>
            </div>
        `;
    } else {
        actionSection = `
            <div class="member-right">
                ${stats}
            </div>
        `;
    }

    return `
        <div class="member-card leader-card">
            ${avatar}
            <div class="member-info">
                <div class="member-header">
                    <div class="member-left">
                        <h3 class="member-name">${escapeHtml(member.userName)} <span class="role-badge leader">👑</span></h3>
                        <p class="member-join-date">가입일: ${joinDate}</p>
                    </div>
                    ${actionSection}
                </div>
            </div>
        </div>
    `;
}

// 일반 크루원 카드 생성
function createMemberCard(member) {
    const roleBadge = getRoleBadge(member.role);
    const avatar = createAvatar(member, false);
    const joinDate = formatDate(member.createdAt);
    const stats = createStatsSection(member);

    // 본인 여부 확인
    const isMyself = member.userId === currentUserId;

    // 크루장이면 권한 관리 메뉴 표시
    const isLeader = currentUserRole === 'LEADER';

    let actionSection = '';

    if (isMyself) {
        // 본인이면 탈퇴 버튼만
        actionSection = `
            <div class="member-right">
                <button class="leave-button" onclick="handleLeave(${member.userId})">탈퇴</button>
            </div>
        `;
    } else if (isLeader && !isMyself) {
        // 크루장이고 본인이 아니면 통계 + 드롭다운
        actionSection = `
            <div class="member-right">
                ${stats}
                ${createRoleManageButton(member)}
            </div>
        `;
    } else {
        // 일반 크루원 본인 아닌 경우 통계만
        actionSection = `
            <div class="member-right">
                ${stats}
            </div>
        `;
    }

    return `
        <div class="member-card">
            ${avatar}
            <div class="member-info">
                <div class="member-header">
                    <div class="member-left">
                        <h3 class="member-name">${escapeHtml(member.userName)}${roleBadge}</h3>
                        <p class="member-join-date">가입일: ${joinDate}</p>
                    </div>
                    ${actionSection}
                </div>
            </div>
        </div>
    `;
}

// 활동 통계 섹션 생성
function createStatsSection(member) {
    const stats = [];

    // 마지막 활동
    if (member.lastActivityDate) {
        const timeDiff = getTimeDiff(member.lastActivityDate);
        if (timeDiff) {
            stats.push(`<span class="stat-item">(${timeDiff})</span>`);
        }
    }

    // 출석 횟수
    if (member.participationCount !== undefined && member.participationCount !== null) {
        stats.push(`<span class="stat-item">출석 ${member.participationCount}회</span>`);
    }

    if (stats.length === 0) {
        return '';
    }

    return `<div class="member-stats">${stats.join('')}</div>`;
}

// 권한 관리 버튼 생성
function createRoleManageButton(member) {
    return `
        <div class="role-dropdown-container">
            <button class="role-manage-btn" onclick="toggleDropdown(event, ${member.userId})">
                <span class="dots">⋮</span>
            </button>
            <div class="role-dropdown" id="dropdown-${member.userId}">
                ${createDropdownOptions(member)}
            </div>
        </div>
    `;
}

// 드롭다운 옵션 생성
function createDropdownOptions(member) {
    const options = [];

    if (member.role === 'MEMBER') {
        // 일반 멤버 → 부크루장/운영진만 임명 가능 (크루장 위임 불가)
        options.push(`
            <div class="dropdown-item" onclick="changeRole(${member.userId}, 'SUB_LEADER', event)">
                <span class="role-icon">🛡️</span>
                <span>부크루장 임명</span>
            </div>
        `);
        options.push(`
            <div class="dropdown-item" onclick="changeRole(${member.userId}, 'STAFF', event)">
                <span class="role-icon">⭐</span>
                <span>운영진 임명</span>
            </div>
        `);
    } else if (member.role === 'SUB_LEADER' || member.role === 'STAFF') {
        // 부크루장/운영진 → 권한 해제 또는 크루장 위임 가능
        options.push(`
            <div class="dropdown-item" onclick="changeRole(${member.userId}, 'MEMBER', event)">
                <span class="role-icon">↓</span>
                <span>권한 해제</span>
            </div>
        `);
        options.push(`
            <div class="dropdown-item" onclick="changeRole(${member.userId}, 'LEADER', event)">
                <span class="role-icon">👑</span>
                <span>크루장 위임</span>
            </div>
        `);
    }

    return options.join('');
}

// 드롭다운 토글
function toggleDropdown(event, userId) {
    event.stopPropagation();

    // 다른 드롭다운 닫기
    const allDropdowns = document.querySelectorAll('.role-dropdown');
    allDropdowns.forEach(dropdown => {
        if (dropdown.id !== `dropdown-${userId}`) {
            dropdown.classList.remove('show');
        }
    });

    // 현재 드롭다운 토글
    const dropdown = document.getElementById(`dropdown-${userId}`);
    dropdown.classList.toggle('show');
}

// 모든 드롭다운 닫기
function closeAllDropdowns(event) {
    if (!event.target.closest('.role-dropdown-container')) {
        const allDropdowns = document.querySelectorAll('.role-dropdown');
        allDropdowns.forEach(dropdown => {
            dropdown.classList.remove('show');
        });
    }
}

// 프로필 이미지 생성
function createAvatar(member, isLeader) {
    const avatarClass = isLeader ? 'member-avatar leader-avatar' : 'member-avatar';

    if (member.profileImageUrl) {
        return `<div class="${avatarClass}">
                    <img src="${escapeHtml(member.profileImageUrl)}" alt="${escapeHtml(member.userName)}" onerror="this.parentElement.innerHTML='<div class=\\'avatar-placeholder\\'>${escapeHtml(member.userName.charAt(0))}</div>'">
                </div>`;
    } else {
        const initial = member.userName ? member.userName.charAt(0).toUpperCase() : '?';
        return `<div class="${avatarClass}">
                    <div class="avatar-placeholder">${initial}</div>
                </div>`;
    }
}

// 역할 뱃지
function getRoleBadge(role) {
    const badges = {
        'LEADER': '<span class="role-badge leader">👑</span>',
        'SUB_LEADER': '<span class="role-badge sub-leader"><span class="role-icon">🛡️</span> 부크루장</span>',
        'STAFF': '<span class="role-badge staff"><span class="role-icon">⭐</span> 운영진</span>',
        'MEMBER': ''
    };
    return badges[role] || '';
}

// ============================
// 날짜 유틸
// ============================
function formatDate(dateString) {
    if (!dateString) return '';

    try {
        const date = new Date(dateString);
        if (isNaN(date.getTime())) {
            return '';
        }

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');

        return `${year}.${month}.${day}`;
    } catch (error) {
        console.warn('날짜 포맷팅 실패:', error);
        return '';
    }
}

function getTimeDiff(dateString) {
    if (!dateString) return '';

    try {
        const now = new Date();
        const past = new Date(dateString);
        if (isNaN(past.getTime())) {
            return '';
        }

        const diffMs = now - past;
        const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

        if (diffDays < 0) return '오늘 활동';
        if (diffDays === 0) return '오늘 활동';
        if (diffDays === 1) return '1일 전 활동';
        if (diffDays < 7) return `${diffDays}일 전 활동`;
        if (diffDays < 30) {
            const weeks = Math.floor(diffDays / 7);
            return `${weeks}주 전 활동`;
        }
        const months = Math.floor(diffDays / 30);
        return `${months}개월 전 활동`;
    } catch (error) {
        console.warn('시간 차이 계산 실패:', error);
        return '';
    }
}

// ========================================
// 크루원 여부 확인 및 권한 체크
// ========================================
async function checkCrewMemberPermission() {
    try {
        const token = localStorage.getItem('accessToken');
        const userId = parseInt(localStorage.getItem('userId'));

        if (!token || !userId) {
            showNoPermissionState();
            return false;
        }

        const response = await fetch(`/api/crews/${crewId}/users`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            showNoPermissionState();
            return false;
        }

        const result = await response.json();
        const members = result.data || result;

        // 내가 크루원인지 확인
        const myInfo = members.find(member => member.userId === userId);

        if (!myInfo) {
            showNoPermissionState();
            return false;
        }

        // 크루원이면 목록 표시
        return true;

    } catch (error) {
        console.error('권한 확인 실패:', error);
        showNoPermissionState();
        return false;
    }
}

// ========================================
// 권한 없음 화면 표시
// ========================================
function showNoPermissionState() {
    // 크루원 목록 숨김
    const memberList = document.getElementById('memberList');
    if (memberList) {
        memberList.style.display = 'none';
    }

    // 권한 없음 화면 표시
    const noPermissionState = document.getElementById('noPermissionState');
    if (noPermissionState) {
        noPermissionState.style.display = 'flex';
    }
}

// ========================================
// 크루 상세로 돌아가기
// ========================================
function goToCrewDetail() {
    window.location.href = `/crews/${crewId}`;
}

// ============================
// 권한 변경 처리
// ============================
async function changeRole(userId, newRole, event) {
    event.stopPropagation();

    const roleNames = {
        'SUB_LEADER': '부크루장',
        'STAFF': '운영진',
        'MEMBER': '일반 멤버',
        'LEADER': '크루장'
    };

    let message = '';
    if (newRole === 'MEMBER') {
        message = '정말 권한을 해제하시겠습니까?';
    } else if (newRole === 'LEADER') {
        message = `정말 크루장을 위임하시겠습니까?\n크루장 권한을 넘기면 본인은 일반 멤버가 됩니다.`;
    } else {
        message = `정말 ${roleNames[newRole]}으로 임명하시겠습니까?`;
    }

    if (!confirm(message)) {
        return;
    }

    try {
        const token = getAccessToken();

        if (!token) {
            throw new Error('로그인이 필요합니다.');
        }

        // 권한 변경 API 호출
        const response = await fetch(`/api/crews/${crewId}/users/${userId}/role?role=${newRole}`, {
            method: 'PATCH',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || '권한 변경에 실패했습니다.');
        }

        alert(`권한이 ${roleNames[newRole]}으로 변경되었습니다.`);

        // 목록 새로고침
        fetchMembers();

    } catch (error) {
        console.error('권한 변경 실패:', error);
        alert(error.message || '권한 변경 중 오류가 발생했습니다.');
    }
}

// ============================
// 탈퇴 처리
// ============================
async function handleLeave(userId) {
    if (!confirm('정말 크루에서 탈퇴하시겠습니까?')) {
        return;
    }

    try {
        const token = getAccessToken();

        if (!token) {
            throw new Error('로그인이 필요합니다.');
        }

        const response = await fetch(`/api/crews/${crewId}/leave`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || '탈퇴에 실패했습니다.');
        }

        alert('크루에서 탈퇴되었습니다.');

        // 크루 목록 페이지로 이동
        window.location.href = '/feed';

    } catch (error) {
        console.error('탈퇴 실패:', error);
        alert(error.message || '탈퇴 중 오류가 발생했습니다.');
    }
}

// ============================
// UI 상태 관리
// ============================
function showLoading() {
    document.getElementById('loading').style.display = 'flex';
    document.getElementById('error').style.display = 'none';
    document.getElementById('main-container').style.display = 'none';
}

function hideLoading() {
    document.getElementById('loading').style.display = 'none';
}

function showError(message) {
    document.getElementById('error-message').textContent = message;
    document.getElementById('error').style.display = 'flex';
    document.getElementById('loading').style.display = 'none';
    document.getElementById('main-container').style.display = 'none';
}

function goBack() {
    window.history.back();
}

// ============================
// 유틸리티 함수
// ============================
function getAccessToken() {
    try {
        return localStorage.getItem('accessToken');
    } catch (error) {
        console.warn('토큰 가져오기 실패:', error);
        return null;
    }
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}