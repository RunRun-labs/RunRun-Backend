/**
 * 크루 가입 신청 목록 조회 페이지 JavaScript
 * - 크루 가입 신청 목록 로딩
 * - 승인/거절 기능
 * - 무한 스크롤 (필요시)
 */

// ========================================
// 전역 변수
// ========================================
let crewId = null;
let joinRequests = [];
let displayedRequests = [];
let currentPage = 0;
const PAGE_SIZE = 5;
let isLoading = false;
let hasMore = true;
let pollingInterval = null;

// ========================================
// DOMContentLoaded 이벤트 핸들러
// ========================================
document.addEventListener('DOMContentLoaded', async () => {
    console.log('크루 가입 신청 목록 페이지 초기화');

    // URL에서 crewId 추출
    const pathParts = window.location.pathname.split('/');
    const crewIdIndex = pathParts.indexOf('crews');
    if (crewIdIndex !== -1 && pathParts.length > crewIdIndex + 1) {
        crewId = pathParts[crewIdIndex + 1];
    }

    // hidden input에서 crewId 가져오기
    const crewIdInput = document.getElementById('crewId');
    if (crewIdInput && crewIdInput.value) {
        crewId = crewIdInput.value;
    }

    if (!crewId) {
        console.error('크루 ID를 찾을 수 없습니다.');
        alert('잘못된 접근입니다. 크루 ID가 필요합니다.');
        history.back();
        return;
    }

    const initialRequests = document.querySelectorAll('.join-request-card');
    if (initialRequests.length > 0) {

        initialRequests.forEach(card => {
            const requestId = card.dataset.requestId;
            if (requestId) {
                joinRequests.push({
                    joinRequestId: parseInt(requestId)
                });
            }
        });
        updatePendingCount();
    } else {
        await loadJoinRequests();
    }

    initEventListeners();

    initInfiniteScroll();

    startPolling();
});

// ========================================
// 이벤트 리스너 초기화
// ========================================
function initEventListeners() {
    // 승인 버튼
    document.querySelectorAll('.btn-approve').forEach(btn => {
        btn.addEventListener('click', handleApprove);
    });

    // 거절 버튼
    document.querySelectorAll('.btn-reject').forEach(btn => {
        btn.addEventListener('click', handleReject);
    });
}

// ========================================
// 무한 스크롤 초기화
// ========================================
function initInfiniteScroll() {
    let timeout;
    window.addEventListener('scroll', () => {
        clearTimeout(timeout);

        timeout = setTimeout(() => {
            if (isLoading || !hasMore) return;

            const scrollHeight = document.documentElement.scrollHeight;
            const scrollTop = document.documentElement.scrollTop;
            const clientHeight = document.documentElement.clientHeight;

            if (scrollTop + clientHeight >= scrollHeight - 300) {
                console.log('스크롤 감지 - 다음 페이지 렌더링');
                renderNextPage();
            }
        }, 200);
    });
}

// ========================================
// 폴링으로 목록 새로고침
// ========================================
async function refreshJoinRequests() {
    const token = getAccessToken();
    if (!token) return;

    try {
        const response = await fetch(`/api/crews/${crewId}/join-requests`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) return;

        const result = await response.json();
        const data = result && typeof result === 'object' ? (result.data || result) : null;

        if (!data || !Array.isArray(data)) return;

        // 새로운 신청이 있으면 알림
        if (data.length > joinRequests.length) {
            console.log('새로운 가입 신청이 있습니다!');
        }

        joinRequests = data;
        updatePendingCount();

    } catch (error) {
        console.error('폴링 실패:', error);
    }
}

function startPolling() {
    if (pollingInterval) return;
    pollingInterval = setInterval(() => {
        refreshJoinRequests();
    }, 3000);
}

function stopPolling() {
    if (!pollingInterval) return;
    clearInterval(pollingInterval);
    pollingInterval = null;
}

window.addEventListener('beforeunload', stopPolling);

// ========================================
// 가입 신청 목록 로드
// ========================================
async function loadJoinRequests() {
    if (isLoading) return;
    isLoading = true;

    try {
        const token = getAccessToken();
        if (!token) {
            alert('로그인이 필요합니다.');
            window.location.href = '/login';
            return;
        }

        const response = await fetch(`/api/crews/${crewId}/join-requests`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            if (response.status === 403) {
                const result = await response.json().catch(() => ({}));
                const errorMessage = result?.message || result?.error || '';

                if (errorMessage.includes('크루장') || errorMessage.includes('부크루장') || errorMessage.includes('권한')) {
                    alert('가입 신청 목록은 크루장과 부크루장만 확인할 수 있습니다.');
                } else {
                    alert('접근 권한이 없습니다.\n크루장 또는 부크루장만 가입 신청 목록을 확인할 수 있습니다.');
                }

                // 크루 상세 페이지로 이동
                window.location.href = `/crews/${crewId}`;
                return;
            }

            throw new Error('가입 신청 목록을 불러오지 못했습니다.');
        }

        const result = await response.json();
        console.log('가입 신청 목록 API 응답:', result);

        const data = result && typeof result === 'object' ? (result.data || result) : null;

        if (!data || !Array.isArray(data)) {
            throw new Error('가입 신청 목록 데이터가 올바르지 않습니다.');
        }

        joinRequests = data;
        renderJoinRequests(joinRequests);
        updatePendingCount();

    } catch (error) {
        console.error('가입 신청 목록 로드 실패:', error);
        alert(error.message || '가입 신청 목록을 불러오는데 실패했습니다.');
    } finally {
        isLoading = false;
    }
}

// ========================================
// 가입 신청 목록 렌더링
// ========================================
function renderJoinRequests(requests) {
    const listContainer = document.getElementById('joinRequestList');
    const emptyState = document.getElementById('emptyState');

    if (!listContainer) return;

    joinRequests = requests;

    currentPage = 0;
    displayedRequests = [];
    listContainer.innerHTML = '';

    if (requests.length === 0) {
        listContainer.style.display = 'none';
        if (emptyState) {
            emptyState.style.display = 'flex';
        }
        hasMore = false;
        return;
    }

    listContainer.style.display = 'flex';
    if (emptyState) {
        emptyState.style.display = 'none';
    }
    renderNextPage();
}

function renderNextPage() {
    const listContainer = document.getElementById('joinRequestList');
    if (!listContainer) return;

    // 현재 페이지의 시작/끝 인덱스
    const start = currentPage * PAGE_SIZE;
    const end = start + PAGE_SIZE;
    const pageRequests = joinRequests.slice(start, end);

    if (pageRequests.length === 0) {
        hasMore = false;
        return;
    }

    // 페이지 데이터 렌더링
    pageRequests.forEach(request => {
        const card = createJoinRequestCard(request);
        listContainer.appendChild(card);
        displayedRequests.push(request);
    });

    // 다음 페이지 존재 여부
    hasMore = end < joinRequests.length;
    currentPage++;
}

// ========================================
// 가입 신청 카드 생성
// ========================================
function createJoinRequestCard(request) {
    const card = document.createElement('article');
    card.className = 'join-request-card';
    card.dataset.requestId = request.joinRequestId;
    card.dataset.dynamic = 'true';

    const profileImageUrl = request.userProfileImageUrl || 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iMzAiIGN5PSIzMCIgcj0iMzAiIGZpbGw9IiNGNUY1RjUiLz48Y2lyY2xlIGN4PSIzMCIgY3k9IjI1IiByPSI4IiBmaWxsPSIjOTk5Ii8+PHBhdGggZD0iTTEwIDQwQzEwIDM1IDE1IDMwIDIwIDMwSDQwQzQ1IDMwIDUwIDM1IDUwIDQwIiBmaWxsPSIjOTk5Ii8+PC9zdmc+';
    const userName = request.userName || '사용자';
    const createdAt = request.createdAt ? formatDate(request.createdAt) : '';
    const introduction = request.introduction || '자기소개가 없습니다.';
    const distanceText = mapDistance(request.distance); // CrewDistanceType -> "10km" 같은 표시값
    const paceText = mapPace(request.pace);             // CrewPaceType -> "5'30\"/km" 같은 표시값
    const regionText = request.region;                  // 필요하면 같이 노출


    card.innerHTML = `
          <div class="join-request-card__header">
        <img src="${profileImageUrl}" 
             alt="프로필" 
             class="join-request-card__profile-image"
             onerror="this.src='data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iMzAiIGN5PSIzMCIgcj0iMzAiIGZpbGw9IiNGNUY1RjUiLz48Y2lyY2xlIGN4PSIzMCIgY3k9IjI1IiByPSI4IiBmaWxsPSIjOTk5Ii8+PHBhdGggZD0iTTEwIDQwQzEwIDM1IDE1IDMwIDIwIDMwSDQwQzQ1IDMwIDUwIDM1IDUwIDQwIiBmaWxsPSIjOTk5Ii8+PC9zdmc+'">
        
        <div class="join-request-card__info-wrapper">
            <p class="join-request-card__name">${escapeHtml(userName)}</p>
            <span class="join-request-card__meta-item">희망거리: <b>${escapeHtml(distanceText)}</b></span>
            
            <p class="join-request-card__date">신청일: ${createdAt}</p>
            <span class="join-request-card__meta-item">희망페이스: <b>${escapeHtml(paceText)}</b></span>
            
            <span class="join-request-card__meta-item meta-item-region">선호지역: <b>${escapeHtml(regionText)}</b></span>
        </div>
    </div>
    <div class="join-request-card__message">
        <p>${escapeHtml(introduction)}</p>
    </div>
        <div class="join-request-card__actions">
            <button type="button" class="btn-reject" data-request-id="${request.joinRequestId}">
                거절
            </button>
            <button type="button" class="btn-approve" data-request-id="${request.joinRequestId}">
                승인
            </button>
        </div>
    `;

    const rejectBtn = card.querySelector('.btn-reject');
    const approveBtn = card.querySelector('.btn-approve');
    if (rejectBtn) {
        rejectBtn.addEventListener('click', handleReject);
    }
    if (approveBtn) {
        approveBtn.addEventListener('click', handleApprove);
    }

    return card;
}

// ========================================
// 승인 대기 수 업데이트
// ========================================
function updatePendingCount() {
    const countElement = document.getElementById('pendingCount');
    if (countElement) {
        const count = joinRequests.length;
        countElement.textContent = `${count}명`;
    }
}

// ========================================
// 가입 신청 승인
// ========================================
async function handleApprove(event) {
    const requestId = event.target.dataset.requestId;
    if (!requestId) {
        console.error('가입 신청 ID를 찾을 수 없습니다.');
        return;
    }

    const confirmMessage = '이 가입 신청을 승인하시겠습니까?';
    if (!confirm(confirmMessage)) {
        return;
    }

    const button = event.target;
    const originalText = button.textContent;
    button.disabled = true;
    button.textContent = '처리 중...';

    try {
        const token = getAccessToken();
        if (!token) {
            alert('로그인이 필요합니다.');
            window.location.href = '/login';
            return;
        }

        const response = await fetch(`/api/crews/${crewId}/join-requests/${requestId}/approve`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        console.log('승인 API 응답:', result);

        if (!response.ok) {
            let errorMessage = result?.message || result?.error || '가입 신청 승인에 실패했습니다.';

            // 백엔드 에러 코드에 따른 메시지 처리
            if (errorMessage.includes('CREW_MEMBER_NOT_FOUND') || errorMessage.includes('크루원')) {
                errorMessage = '크루원 정보를 찾을 수 없습니다.\n크루를 생성한 크루장이 크루원으로 등록되어 있지 않습니다.\n백엔드에서 크루 생성 시 크루장을 자동으로 크루원으로 추가하도록 수정이 필요합니다.';
            } else if (errorMessage.includes('NOT_CREW_LEADER_OR_SUB_LEADER')) {
                errorMessage = '크루장 또는 부크루장만 승인할 수 있습니다.';
            } else if (errorMessage.includes('JOIN_REQUEST_NOT_FOUND')) {
                errorMessage = '가입 신청을 찾을 수 없습니다.';
            }

            throw new Error(errorMessage);
        }

        if (result && result.success === false) {
            let errorMessage = result.message || '가입 신청 승인에 실패했습니다.';

            // 백엔드 에러 코드에 따른 메시지 처리
            if (errorMessage.includes('CREW_MEMBER_NOT_FOUND') || errorMessage.includes('크루원')) {
                errorMessage = '크루원 정보를 찾을 수 없습니다.\n크루를 생성한 크루장이 크루원으로 등록되어 있지 않습니다.\n백엔드에서 크루 생성 시 크루장을 자동으로 크루원으로 추가하도록 수정이 필요합니다.';
            } else if (errorMessage.includes('NOT_CREW_LEADER_OR_SUB_LEADER')) {
                errorMessage = '크루장 또는 부크루장만 승인할 수 있습니다.';
            } else if (errorMessage.includes('JOIN_REQUEST_NOT_FOUND')) {
                errorMessage = '가입 신청을 찾을 수 없습니다.';
            }

            throw new Error(errorMessage);
        }

        alert('가입 신청이 승인되었습니다.');

        const card = button.closest('.join-request-card');
        if (card) {
            card.remove();
        }

        joinRequests = joinRequests.filter(req => req.joinRequestId !== parseInt(requestId));
        updatePendingCount();

        if (joinRequests.length === 0) {
            const listContainer = document.getElementById('joinRequestList');
            const emptyState = document.getElementById('emptyState');
            if (listContainer) listContainer.style.display = 'none';
            if (emptyState) emptyState.style.display = 'flex';
        }

    } catch (error) {
        console.error('가입 신청 승인 실패:', error);
        alert(error.message || '가입 신청 승인 중 오류가 발생했습니다.');
        button.disabled = false;
        button.textContent = originalText;
    }
}

// ========================================
// 가입 신청 거절
// ========================================
async function handleReject(event) {
    const requestId = event.target.dataset.requestId;
    if (!requestId) {
        console.error('가입 신청 ID를 찾을 수 없습니다.');
        return;
    }

    const confirmMessage = '이 가입 신청을 거절하시겠습니까?';
    if (!confirm(confirmMessage)) {
        return;
    }

    const button = event.target;
    const originalText = button.textContent;
    button.disabled = true;
    button.textContent = '처리 중...';

    try {
        const token = getAccessToken();
        if (!token) {
            alert('로그인이 필요합니다.');
            window.location.href = '/login';
            return;
        }

        const response = await fetch(`/api/crews/${crewId}/join-requests/${requestId}/reject`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        console.log('거절 API 응답:', result);

        if (!response.ok) {
            let errorMessage = result?.message || result?.error || '가입 신청 거절에 실패했습니다.';

            // 백엔드 에러 코드에 따른 메시지 처리
            if (errorMessage.includes('CREW_MEMBER_NOT_FOUND') || errorMessage.includes('크루원')) {
                errorMessage = '크루원 정보를 찾을 수 없습니다.\n크루를 생성한 크루장이 크루원으로 등록되어 있지 않습니다.\n백엔드에서 크루 생성 시 크루장을 자동으로 크루원으로 추가하도록 수정이 필요합니다.';
            } else if (errorMessage.includes('NOT_CREW_LEADER_OR_SUB_LEADER')) {
                errorMessage = '크루장 또는 부크루장만 거절할 수 있습니다.';
            } else if (errorMessage.includes('JOIN_REQUEST_NOT_FOUND')) {
                errorMessage = '가입 신청을 찾을 수 없습니다.';
            }

            throw new Error(errorMessage);
        }

        if (result && result.success === false) {
            let errorMessage = result.message || '가입 신청 거절에 실패했습니다.';

            // 백엔드 에러 코드에 따른 메시지 처리
            if (errorMessage.includes('CREW_MEMBER_NOT_FOUND') || errorMessage.includes('크루원')) {
                errorMessage = '크루원 정보를 찾을 수 없습니다.\n크루를 생성한 크루장이 크루원으로 등록되어 있지 않습니다.\n백엔드에서 크루 생성 시 크루장을 자동으로 크루원으로 추가하도록 수정이 필요합니다.';
            } else if (errorMessage.includes('NOT_CREW_LEADER_OR_SUB_LEADER')) {
                errorMessage = '크루장 또는 부크루장만 거절할 수 있습니다.';
            } else if (errorMessage.includes('JOIN_REQUEST_NOT_FOUND')) {
                errorMessage = '가입 신청을 찾을 수 없습니다.';
            }

            throw new Error(errorMessage);
        }

        alert('가입 신청이 거절되었습니다.');

        const card = button.closest('.join-request-card');
        if (card) {
            card.remove();
        }

        joinRequests = joinRequests.filter(req => req.joinRequestId !== parseInt(requestId));
        updatePendingCount();

        if (joinRequests.length === 0) {
            const listContainer = document.getElementById('joinRequestList');
            const emptyState = document.getElementById('emptyState');
            if (listContainer) listContainer.style.display = 'none';
            if (emptyState) emptyState.style.display = 'flex';
        }

    } catch (error) {
        console.error('가입 신청 거절 실패:', error);
        alert(error.message || '가입 신청 거절 중 오류가 발생했습니다.');
        button.disabled = false;
        button.textContent = originalText;
    }
}

// ========================================
// 유틸리티 함수
// ========================================

const DISTANCE_LABEL = {
    UNDER_3KM: '3km 미만',
    KM_3: '3km',
    KM_5: '5km',
    KM_10: '10km',
    OVER_10KM: '10km 이상',
};

function mapDistance(distanceEnum) {
    const key = String(distanceEnum ?? '').trim();
    return DISTANCE_LABEL[key] ?? key;
}

const PACE_LABEL = {
    UNDER_3_MIN: '2분/km 이하',
    MIN_3_TO_4: '3~4분/km',
    MIN_5_TO_6: '5~6분/km',
    MIN_7_TO_8: '7~8분/km',
    OVER_9_MIN: '9분/km 이상',
};

function mapPace(paceEnum) {
    const key = String(paceEnum ?? '').trim();
    return PACE_LABEL[key] ?? key;
}

/**
 * 날짜 포맷팅 (yyyy.MM.dd)
 */
function formatDate(dateString) {
    if (!dateString) return '';

    try {
        const date = new Date(dateString);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}.${month}.${day}`;
    } catch (e) {
        console.warn('날짜 포맷팅 실패:', e);
        return '';
    }
}

/**
 * HTML 이스케이프
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * 액세스 토큰 가져오기
 */
function getAccessToken() {
    try {
        const token = localStorage.getItem('accessToken');
        return token;
    } catch (error) {
        console.warn('토큰 가져오기 실패:', error);
        return null;
    }
}

