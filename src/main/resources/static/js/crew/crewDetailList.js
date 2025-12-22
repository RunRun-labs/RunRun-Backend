/**
 * 런런 프로젝트 - 크루 상세 화면
 * 신청/취소 기능 구현
 */

// ===========================
// 전역 변수
// ===========================
let crewId = null;
let isApplied = false; // 신청 여부
let isFavorite = false;

const joinBtn = document.getElementById('joinBtn');
const cancelBtn = document.getElementById('cancelBtn');
const favoriteBtn = document.getElementById('favoriteBtn');

// ===========================
// 초기화 함수
// ===========================
document.addEventListener('DOMContentLoaded', async () => {
    console.log('크루 상세 페이지 초기화');

    const pathParts = window.location.pathname.split('/');
    const crewIdIndex = pathParts.indexOf('crews');
    if (crewIdIndex !== -1 && pathParts.length > crewIdIndex + 1) {
        crewId = pathParts[crewIdIndex + 1];
        console.log('크루 ID (URL에서 추출):', crewId);

        const crewIdInput = document.getElementById('crewId');
        if (crewIdInput) {
            crewIdInput.value = crewId;
        }
    }

    if (!crewId) {
        console.error('크루 ID를 찾을 수 없습니다.');
        alert('크루 정보를 불러올 수 없습니다.');
        history.back();
        return;
    }

    await loadCrewData();

    initEventListeners();

    updateButtonUI();
});

// ===========================
// 크루 데이터 로드
// ===========================
async function loadCrewData() {
    try {
        console.log('크루 데이터 로드 시작 - crewId:', crewId);

        const token = getAccessToken();
        const headers = {};

        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const response = await fetch(`/api/crews/${crewId}`, {
            method: 'GET',
            headers: headers
        });

        if (!response.ok) {
            throw new Error('크루 정보를 불러오지 못했습니다.');
        }

        const result = await response.json();
        console.log('크루 데이터 응답:', result);

        const data = result && typeof result === 'object' ? (result.data || result) : null;

        if (!data) {
            throw new Error('크루 데이터가 없습니다.');
        }

        updateCrewUI(data);

        // 신청 여부 확인 (로그인한 경우)
        if (token) {
            try {
                const appliedRes = await fetch(`/api/crews/${crewId}/applied`, {method: 'GET', headers});
                if (appliedRes.ok) {
                    const appliedJson = await appliedRes.json();
                    const appliedData = appliedJson.data || appliedJson;
                    isApplied = appliedData?.applied || false;
                }
            } catch (e) {
                console.warn('applied 조회 실패:', e);
            }
        }

    } catch (error) {
        console.error('크루 데이터 로드 실패:', error);
        alert('크루 정보를 불러오는데 실패했습니다.\n' + error.message);
        setTimeout(() => {
            history.back();
        }, 2000);
    }
}

// ===========================
// 크루 UI 업데이트
// ===========================
function updateCrewUI(crew) {
    const crewImage = document.getElementById('crewImage');
    if (crewImage) {
        const defaultImageUrl = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI0Y1RjVGNSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LXNpemU9IjE4IiBmaWxsPSIjOTk5IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+6rCA7J2AIOyVlOydgCDsiqTsmYg8L3RleHQ+PC9zdmc+';
        crewImage.src = crew.crewImageUrl && crew.crewImageUrl.trim() ? crew.crewImageUrl : defaultImageUrl;
        crewImage.alt = crew.crewName || '크루 이미지';
    }

    const recruitingBadge = document.getElementById('recruitmentBadge');
    const closedBadge = document.getElementById('recruitmentBadgeClosed');
    if (recruitingBadge && closedBadge) {
        if (crew.crewRecruitStatus === 'RECRUITING' || crew.crewRecruitStatus === 'OPEN') {
            recruitingBadge.style.display = 'inline-block';
            closedBadge.style.display = 'none';
        } else {
            recruitingBadge.style.display = 'none';
            closedBadge.style.display = 'inline-block';
        }
    }

    const crewName = document.getElementById('crewName');
    if (crewName) {
        crewName.textContent = crew.crewName || '크루 이름';
    }

    const crewLeader = document.getElementById('crewLeader');
    if (crewLeader) {
        if (crew.leaderNm) {
            crewLeader.textContent = crew.leaderNm;
            crewLeader.style.display = 'inline-block';
        } else {
            crewLeader.style.display = 'none';
        }
    }

    const crewLocation = document.getElementById('crewLocation');
    if (crewLocation) {
        crewLocation.textContent = crew.region || '위치 미정';
    }

    const memberCount = document.getElementById('memberCount');
    if (memberCount) {
        memberCount.textContent = crew.memberCount || 0;
    }

    const crewDistance = document.getElementById('crewDistance');
    if (crewDistance) {
        crewDistance.textContent = crew.distance || '-';
    }

    const crewPace = document.getElementById('crewPace');
    if (crewPace) {
        crewPace.textContent = crew.averagePace || '-';
    }

    const crewDescription = document.getElementById('crewDescription');
    if (crewDescription) {
        crewDescription.textContent = crew.crewDescription || '크루 소개가 없습니다.';
    }

    const regularSchedule = document.getElementById('regularSchedule');
    if (regularSchedule) {
        regularSchedule.textContent = crew.regularMeetingTime || crew.activityTime || '정기 일정 없음';
    }

    const activityList = document.getElementById('activityList');
    const emptyActivityState = document.getElementById('emptyActivityState');
    if (activityList) {
        const existingCards = activityList.querySelectorAll('.activity-card');
        existingCards.forEach(card => card.remove());

        if (crew.recentActivities && crew.recentActivities.length > 0) {
            crew.recentActivities.forEach(activity => {
                const activityCard = document.createElement('article');
                activityCard.className = 'activity-card';

                const activityDate = activity.activityDate
                    ? new Date(activity.activityDate).toLocaleDateString('ko-KR', {
                        year: 'numeric',
                        month: '2-digit',
                        day: '2-digit'
                    }).replace(/\./g, '.').replace(/\s/g, '')
                    : '';

                activityCard.innerHTML = `
                    <div class="activity-card__badge">러닝</div>
                    <h4 class="activity-card__title">${escapeHtml(activity.activityName || '활동')}</h4>
                    <p class="activity-card__date">${activityDate} ${activity.participantCount || 0}명 참여</p>
                `;
                activityList.appendChild(activityCard);
            });

            if (emptyActivityState) {
                emptyActivityState.style.display = 'none';
            }
        } else {
            if (emptyActivityState) {
                emptyActivityState.style.display = 'block';
            }
        }
    }
}

/**
 * HTML 이스케이프 함수
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ===========================
// 이벤트 리스너 초기화
// ===========================
function initEventListeners() {
    if (joinBtn) {
        joinBtn.addEventListener('click', handleJoinCrew);
    }

    if (cancelBtn) {
        cancelBtn.addEventListener('click', handleCancelCrew);
    }

    if (favoriteBtn) {
        favoriteBtn.addEventListener('click', handleToggleFavorite);
    }
}

// ===========================
// 크루 가입 신청
// ===========================
async function handleJoinCrew() {
    console.log('크루 가입 신청 시작');

    const confirmMessage = '이 크루에 가입 신청하시겠습니까?\n크루장의 승인 후 정식 멤버가 됩니다.';
    if (!confirm(confirmMessage)) {
        return;
    }

    setButtonLoading(joinBtn, true);

    try {
        const token = getAccessToken();
        const headers = {
            'Content-Type': 'application/json'
        };

        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const response = await fetch(`/api/crews/${crewId}/join`, {
            method: 'POST',
            headers: headers
        });

        const result = await response.json();
        console.log('API 응답:', result);

        if (!response.ok) {
            throw new Error(result.message || '가입 신청에 실패했습니다.');
        }

        const data = result.data || result;

        alert('크루 가입 신청이 완료되었습니다!\n크루장의 승인을 기다려주세요.');
        isApplied = true;
        updateButtonUI();

    } catch (error) {
        console.error('크루 신청 실패:', error);
        alert(error.message || '가입 신청 중 오류가 발생했습니다.');
    } finally {
        setButtonLoading(joinBtn, false);
    }
}

// ===========================
// 크루 신청 취소
// ===========================
async function handleCancelCrew() {
    console.log('크루 신청 취소 시작');

    const confirmMessage = '정말 크루 가입 신청을 취소하시겠습니까?';
    if (!confirm(confirmMessage)) {
        return;
    }

    setButtonLoading(cancelBtn, true);

    try {
        const token = getAccessToken();
        const headers = {
            'Content-Type': 'application/json'
        };

        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const response = await fetch(`/api/crews/${crewId}/cancel`, {
            method: 'DELETE',
            headers: headers
        });

        const result = await response.json();
        console.log('API 응답:', result);

        if (!response.ok) {
            throw new Error(result.message || '신청 취소에 실패했습니다.');
        }

        alert('크루 가입 신청이 취소되었습니다.');
        isApplied = false;
        updateButtonUI();

    } catch (error) {
        console.error('크루 취소 실패:', error);
        alert(error.message || '신청 취소 중 오류가 발생했습니다.');
    } finally {
        setButtonLoading(cancelBtn, false);
    }
}

// ===========================
// 즐겨찾기 토글
// ===========================
async function handleToggleFavorite() {
    console.log('즐겨찾기 토글');

    isFavorite = !isFavorite;
    updateFavoriteUI();

    try {
        const token = getAccessToken();
        const headers = {
            'Content-Type': 'application/json'
        };

        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const endpoint = isFavorite
            ? `/api/crews/${crewId}/favorite`
            : `/api/crews/${crewId}/unfavorite`;

        const response = await fetch(endpoint, {
            method: 'POST',
            headers: headers
        });

        if (!response.ok) {
            isFavorite = !isFavorite;
            updateFavoriteUI();
            throw new Error('즐겨찾기 설정에 실패했습니다.');
        }

        const result = await response.json();
        console.log('즐겨찾기 결과:', result);

    } catch (error) {
        console.error('즐겨찾기 실패:', error);
    }
}

// ===========================
// UI 업데이트 함수들
// ===========================

/**
 * 신청/취소 버튼 상태 업데이트
 */
function updateButtonUI() {
    if (isApplied) {
        if (joinBtn) joinBtn.style.display = 'none';
        if (cancelBtn) cancelBtn.style.display = 'flex';
    } else {
        if (joinBtn) joinBtn.style.display = 'flex';
        if (cancelBtn) cancelBtn.style.display = 'none';
    }
}

/**
 * 즐겨찾기 버튼 UI 업데이트
 */
function updateFavoriteUI() {
    if (!favoriteBtn) return;

    if (isFavorite) {
        favoriteBtn.classList.add('active');
    } else {
        favoriteBtn.classList.remove('active');
    }
}

/**
 * 버튼 로딩 상태 설정
 * @param {HTMLButtonElement} button - 대상 버튼
 * @param {boolean} loading - 로딩 상태 여부
 */
function setButtonLoading(button, loading) {
    if (!button) return;

    if (loading) {
        button.disabled = true;
        button.dataset.originalText = button.textContent;
        button.textContent = '처리 중...';
    } else {
        button.disabled = false;
        if (button.dataset.originalText) {
            button.textContent = button.dataset.originalText;
        }
    }
}

/**
 * 멤버 수 업데이트
 * @param {number} change - 변경할 수 (+1 또는 -1)
 */
function updateMemberCount(change) {
    const memberCountElement = document.getElementById('memberCount');
    if (!memberCountElement) return;

    const currentCount = parseInt(memberCountElement.textContent) || 0;
    const newCount = Math.max(0, currentCount + change);

    memberCountElement.textContent = newCount;

    memberCountElement.style.transition = 'transform 0.3s';
    memberCountElement.style.transform = 'scale(1.2)';

    setTimeout(() => {
        memberCountElement.style.transform = 'scale(1)';
    }, 300);
}

// ===========================
// 유틸리티 함수들
// ===========================

/**
 * 에러 메시지 표시 (토스트 형식, 선택사항)
 * @param {string} message - 표시할 메시지
 */
function showToast(message) {

    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed;
        bottom: 100px;
        left: 50%;
        transform: translateX(-50%);
        background-color: rgba(0, 0, 0, 0.8);
        color: white;
        padding: 12px 24px;
        border-radius: 24px;
        font-size: 14px;
        z-index: 1000;
        animation: fadeInOut 2s ease-in-out;
    `;

    document.body.appendChild(toast);

    setTimeout(() => {
        toast.remove();
    }, 2000);
}

const style = document.createElement('style');
style.textContent = `
    @keyframes fadeInOut {
        0% { opacity: 0; transform: translateX(-50%) translateY(20px); }
        15% { opacity: 1; transform: translateX(-50%) translateY(0); }
        85% { opacity: 1; transform: translateX(-50%) translateY(0); }
        100% { opacity: 0; transform: translateX(-50%) translateY(-20px); }
    }
`;
document.head.appendChild(style);

/**
 * API 에러 핸들링
 * @param {Error} error - 발생한 에러
 */
function handleApiError(error) {
    console.error('API 에러:', error);

    let errorMessage = '요청 처리 중 오류가 발생했습니다.';

    if (error.message) {
        errorMessage = error.message;
    }

    // 네트워크 에러
    if (!navigator.onLine) {
        errorMessage = '인터넷 연결을 확인해주세요.';
    }

    return errorMessage;
}

/**
 * 페이지 새로고침 (필요시)
 */
function refreshPage() {
    window.location.reload();
}

/**
 * 뒤로가기
 */
function goBack() {
    if (window.history.length > 1) {
        window.history.back();
    } else {
        window.location.href = '/crews';
    }
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

console.log('크루 상세 스크립트 로드 완료');