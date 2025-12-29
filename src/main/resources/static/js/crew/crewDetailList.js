/**
 * 런런 프로젝트 - 크루 상세 화면
 * 신청/취소 기능 구현
 */

// ===========================
// 전역 변수
// ===========================
let crewId = null;
let isApplied = false; // 신청 여부
let joinStatus = null; // 가입 신청 상태 (PENDING, APPROVED, REJECTED, CANCELED)
let isFavorite = false;
let pollingInterval = null; // 폴링 인터벌 ID
let hasJoinedCrew = false;

const joinBtn = document.getElementById('joinBtn');
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

    // 실시간 상태 갱신
    const token = getAccessToken();
    if (token) {
        startPolling();
    }
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

        // 신청 여부 및 상태 확인 (로그인한 경우)
        if (token) {
            try {
                const appliedRes = await fetch(`/api/crews/${crewId}/applied`, {method: 'GET', headers});
                if (appliedRes.ok) {
                    const appliedJson = await appliedRes.json();
                    const appliedData = appliedJson.data || appliedJson;
                    const crewJoinState = appliedData?.crewJoinState;

                    if (crewJoinState === 'PENDING') {
                        joinStatus = 'PENDING';
                        isApplied = true;
                    } else if (crewJoinState === 'APPROVED') {
                        joinStatus = 'APPROVED';
                        isApplied = true;
                    } else if (crewJoinState === 'NOT_APPLIED' || crewJoinState === 'CAN_REAPPLY') {
                        // NOT_APPLIED, CAN_REAPPLY (REJECTED, CANCELED 포함)는 모두 재신청 가능하도록 null로 처리
                        joinStatus = null;
                        isApplied = false;
                    } else {
                        // 기본값: 재신청 가능
                        joinStatus = null;
                        isApplied = false;
                    }
                    console.log('가입 신청 상태 (applied API):', {crewJoinState, joinStatus, isApplied});
                } else if (appliedRes.status === 404) {
                    // API가 없는 경우, 가입 신청 목록에서 현재 사용자의 신청 상태 확인 시도 (크루장만 접근 가능)
                    console.warn('/api/crews/{crewId}/applied API가 없습니다. 백엔드에 이 API를 추가해야 합니다.');
                    joinStatus = null;
                    isApplied = false;
                } else {
                    joinStatus = null;
                    isApplied = false;
                }
            } catch (e) {
                console.warn('applied 조회 실패:', e);
                // API가 없는 경우를 대비해 기본값 설정
                joinStatus = null;
                isApplied = false;
            }
            try {
                console.log('다른 크루 가입 여부 확인 시작');

                const crewsRes = await fetch('/api/crews', {method: 'GET', headers});
                if (crewsRes.ok) {
                    const crewsJson = await crewsRes.json();
                    console.log('크루 목록 API 응답:', crewsJson);

                    const crewsData = crewsJson.data || crewsJson;

                    // 크루 목록 추출
                    let crewsList = [];
                    if (Array.isArray(crewsData)) {
                        crewsList = crewsData;
                    } else if (crewsData.crews && Array.isArray(crewsData.crews)) {
                        crewsList = crewsData.crews;
                    }

                    console.log('크루 목록:', crewsList.length, '개');

                    // APPROVED 상태인 다른 크루 찾기
                    let foundApprovedCrew = false;

                    for (const crew of crewsList) {
                        // 현재 보고 있는 크루는 제외
                        if (crew.crewId == crewId) {
                            console.log('  현재 크루 제외:', crew.crewId, crew.crewName);
                            continue;
                        }

                        try {
                            const appliedRes = await fetch(`/api/crews/${crew.crewId}/applied`, {
                                method: 'GET',
                                headers
                            });

                            if (appliedRes.ok) {
                                const appliedJson = await appliedRes.json();
                                const appliedData = appliedJson.data || appliedJson;
                                const state = appliedData?.crewJoinState;

                                console.log(`  크루 ${crew.crewId} (${crew.crewName}) 상태:`, state);

                                if (state === 'APPROVED') {
                                    foundApprovedCrew = true;
                                    console.log('APPROVED 크루 발견!', crew.crewId, crew.crewName);
                                    break; // 하나만 찾으면 됨
                                }
                            }
                        } catch (e) {
                            console.warn(`  크루 ${crew.crewId} applied 조회 실패:`, e);
                        }
                    }

                    hasJoinedCrew = foundApprovedCrew;
                    console.log('최종 hasJoinedCrew:', hasJoinedCrew);

                } else {
                    console.warn('크루 목록 API 실패:', crewsRes.status);
                    hasJoinedCrew = false;
                }
            } catch (e) {
                console.error('크루 목록 조회 실패:', e);
                hasJoinedCrew = false;
            }
        } else {
            // 로그인하지 않은 경우
            joinStatus = null;
            isApplied = false;
            hasJoinedCrew = false;
        }

        updateButtonUI();

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
        joinBtn.addEventListener('click', handleJoinOrCancel);
    }

    if (favoriteBtn) {
        favoriteBtn.addEventListener('click', handleToggleFavorite);
    }
}

// ===========================
// 크루 가입 신청 / 취소 / 탈퇴
// ===========================
async function handleJoinOrCancel() {
    if (joinStatus === 'PENDING') {
        // 신청 대기: 가입 취소
        await handleCancelCrew();
    } else if (joinStatus === 'APPROVED') {
        // 승인됨: 가입 탈퇴
        await handleLeaveCrew();
    } else {
        // 신청 전, 거절됨, 취소됨: 가입 신청 화면으로 이동
        handleJoinCrew();
    }
}

// ===========================
// 크루 가입 신청 화면으로 이동
// ===========================
function handleJoinCrew() {
    console.log('크루 가입 신청 화면으로 이동');

    if (!crewId) {
        alert('크루 ID를 찾을 수 없습니다.');
        return;
    }

    // 모집 마감 여부 확인
    const closedBadge = document.getElementById('recruitmentBadgeClosed');
    const isRecruitingClosed = closedBadge && closedBadge.style.display !== 'none';

    if (isRecruitingClosed) {
        alert('크루 모집이 마감되었습니다.');
        return;
    }

    if (hasJoinedCrew) {
        alert('이미 가입한 크루가 있습니다.\n1인 1크루만 가입 가능합니다.');
        return;
    }

    window.location.href = `/crews/${crewId}/join`;
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

    setButtonLoading(joinBtn, true);

    try {
        const token = getAccessToken();
        if (!token) {
            alert('로그인이 필요합니다.');
            window.location.href = '/login';
            return;
        }

        const response = await fetch(`/api/crews/${crewId}/join-cancel`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        console.log('API 응답:', result);

        if (!response.ok) {
            const errorMessage = result?.message || result?.error || '신청 취소에 실패했습니다.';
            throw new Error(errorMessage);
        }

        if (result && result.success === false) {
            const errorMessage = result.message || '신청 취소에 실패했습니다.';
            throw new Error(errorMessage);
        }

        // 취소 성공: 즉시 상태 업데이트
        isApplied = false;
        joinStatus = null;
        updateButtonUI();

        showToast('크루 가입 신청이 취소되었습니다.');

    } catch (error) {
        console.error('크루 취소 실패:', error);
        alert(error.message || '신청 취소 중 오류가 발생했습니다.');
    } finally {
        setButtonLoading(joinBtn, false);
    }
}

// ===========================
// 크루 가입 탈퇴
// ===========================
async function handleLeaveCrew() {
    console.log('크루 가입 탈퇴 시작');

    const confirmMessage = '정말 크루를 탈퇴하시겠습니까?';
    if (!confirm(confirmMessage)) {
        return;
    }

    setButtonLoading(joinBtn, true);

    try {
        const token = getAccessToken();
        if (!token) {
            alert('로그인이 필요합니다.');
            window.location.href = '/login';
            return;
        }

        const response = await fetch(`/api/crews/${crewId}/leave`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();
        console.log('API 응답:', result);

        if (!response.ok) {
            const errorMessage = result?.message || result?.error || '크루 탈퇴에 실패했습니다.';
            throw new Error(errorMessage);
        }

        if (result && result.success === false) {
            const errorMessage = result.message || '크루 탈퇴에 실패했습니다.';
            throw new Error(errorMessage);
        }

        isApplied = false;
        joinStatus = null;  // 탈퇴 후 다시 신청 가능하도록 null로 설정
        updateButtonUI();

        showToast('크루에서 탈퇴되었습니다.');

    } catch (error) {
        console.error('크루 탈퇴 실패:', error);
        alert(error.message || '크루 탈퇴 중 오류가 발생했습니다.');
    } finally {
        setButtonLoading(joinBtn, false);
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
 * 신청/취소/탈퇴 버튼 상태 업데이트
 * 가입 상태별 버튼 동작:null (신청 전), PENDING (신청 대기), APPROVED (승인됨), REJECTED (거절됨), CANCELED (취소됨)
 */
function updateButtonUI() {
    if (!joinBtn) {
        console.warn('joinBtn이 없습니다.');
        return;
    }

    console.log('updateButtonUI 호출 - joinStatus:', joinStatus, 'isApplied:', isApplied);

    const closedBadge = document.getElementById('recruitmentBadgeClosed');
    const isRecruitingClosed = closedBadge && closedBadge.style.display !== 'none';

    joinBtn.classList.remove('btn-action--join', 'btn-action--cancel', 'btn-action--leave');

    // 가입 상태에 따라 버튼 텍스트, 색상, 동작 설정
    switch (joinStatus) {
        case 'PENDING':
            joinBtn.textContent = '가입 취소';
            joinBtn.classList.add('btn-action--cancel');
            joinBtn.style.display = 'flex';
            console.log('버튼 상태: 가입 취소 (빨간색) - DELETE /api/crews/' + crewId + '/join-cancel');
            break;

        case 'APPROVED':
            joinBtn.textContent = '가입 탈퇴';
            joinBtn.classList.add('btn-action--leave');
            joinBtn.style.display = 'flex';
            console.log('버튼 상태: 가입 탈퇴 (회색) - DELETE /api/crews/' + crewId + '/leave');
            break;

        case 'REJECTED':
        case 'CANCELED':
        case null:
        default:
            if (isRecruitingClosed) {
                joinBtn.textContent = '모집 마감';
                joinBtn.classList.add('btn-action--leave'); // 회색
                joinBtn.style.display = 'flex';
                joinBtn.disabled = true;
                console.log('버튼 상태: 모집 마감 (비활성화)');
            } else {
                joinBtn.textContent = '가입 신청';
                joinBtn.classList.add('btn-action--join');
                joinBtn.style.display = 'flex';
                joinBtn.disabled = false;
                console.log('버튼 상태: 가입 신청');
            }
            break;
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

        if (button.dataset.originalText && button.textContent === '처리 중...') {
            button.textContent = button.dataset.originalText;
        }
        delete button.dataset.originalText;
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

// ===========================
// 실시간 상태 갱신 (폴링)
// ===========================

/**
 * 가입 상태만 갱신하는 경량 함수
 */
async function refreshJoinStatus() {
    try {
        const token = getAccessToken();
        if (!token) {
            // 로그인하지 않은 경우 폴링 중단
            stopPolling();
            return;
        }

        const response = await fetch(`/api/crews/${crewId}/applied`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            console.warn('상태 갱신 실패:', response.status);
            return;
        }

        const result = await response.json();
        const data = result.data || result;

        const crewJoinState = data?.crewJoinState || null;

        let newJoinStatus = null;
        let newIsApplied = false;

        if (crewJoinState === 'PENDING') {
            newJoinStatus = 'PENDING';
            newIsApplied = true;
        } else if (crewJoinState === 'APPROVED') {
            newJoinStatus = 'APPROVED';
            newIsApplied = true;
        } else if (crewJoinState === 'NOT_APPLIED' || crewJoinState === 'CAN_REAPPLY') {
            // NOT_APPLIED, CAN_REAPPLY (REJECTED, CANCELED 포함)는 모두 재신청 가능하도록 null로 처리
            newJoinStatus = null;
            newIsApplied = false;
        } else {
            // 기본값
            newJoinStatus = null;
            newIsApplied = false;
        }

        // 상태가 변경된 경우에만 로그 출력 및 UI 업데이트
        const previousJoinStatus = joinStatus; // 이전 상태 저장
        if (isApplied !== newIsApplied || joinStatus !== newJoinStatus) {
            console.log('상태 변경 감지!');
            console.log('  이전:', {isApplied, joinStatus});
            console.log('  현재:', {applied: newIsApplied, joinStatus: newJoinStatus});

            // 전역 변수 업데이트
            isApplied = newIsApplied;
            joinStatus = newJoinStatus;

            // 버튼 UI만 업데이트
            updateButtonUI();

            // 사용자에게 알림
            if (newJoinStatus === 'APPROVED' && previousJoinStatus === 'PENDING') {
                showToast('크루 가입이 승인되었습니다!');
            } else if (newJoinStatus === null && previousJoinStatus === 'PENDING') {
                showToast('크루 가입 신청이 취소되었습니다.');
            } else if (newJoinStatus === null && previousJoinStatus === 'APPROVED') {
                showToast('크루에서 탈퇴되었습니다.');
            } else if (newJoinStatus === null && (previousJoinStatus === 'REJECTED' || previousJoinStatus === 'CANCELED')) {
                // 거절/취소 후 재신청 가능 상태로 변경됨
                if (previousJoinStatus === 'REJECTED') {
                    showToast('크루 가입이 거절되었습니다. 다시 신청하실 수 있습니다.');
                } else {
                    showToast('크루 가입 신청이 취소되었습니다. 다시 신청하실 수 있습니다.');
                }
            }
        }

    } catch (error) {
        console.error('상태 갱신 중 오류:', error);
    }
}

/**
 * 폴링 - 3초마다 상태 확인
 */
function startPolling() {
    if (pollingInterval) {
        console.warn('폴링이 이미 실행 중입니다.');
        return;
    }

    console.log('실시간 상태 갱신 시작 (3초 간격)');
    pollingInterval = setInterval(() => {
        refreshJoinStatus();
    }, 3000);
}

/**
 * 폴링 중지
 */
function stopPolling() {
    if (pollingInterval) {
        console.log('⏸실시간 상태 갱신 중지');
        clearInterval(pollingInterval);
        pollingInterval = null;
    }
}

/**
 * 페이지 나갈 때 폴링 중지
 */
window.addEventListener('beforeunload', () => {
    stopPolling();
});

console.log('크루 상세 스크립트 로드 완료');