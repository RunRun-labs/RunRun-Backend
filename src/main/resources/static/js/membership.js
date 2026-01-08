// ========================================
// 전역 변수
// ========================================
const API_BASE_URL = 'http://localhost:8080/api';
let currentMembership = null;

// ========================================
// 페이지 로드 시 실행
// ========================================
document.addEventListener('DOMContentLoaded', function () {
    // 로컬 스토리지에서 토큰 가져오기
    const token = localStorage.getItem('accessToken');

    if (!token) {
        alert('로그인이 필요합니다.');
        // 로그인 페이지로 이동
        // window.location.href = '/login.html';
        return;
    }

    // 멤버십 정보 조회
    fetchMembershipInfo(token);
});

// ========================================
// 멤버십 정보 조회 API 호출
// ========================================
async function fetchMembershipInfo(token) {
    try {
        const response = await fetch(`${API_BASE_URL}/memberships`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();

        if (result.success && result.data) {
            // 멤버십 정보가 있는 경우
            currentMembership = result.data;
            renderMembershipCard(result.data);
        } else {
            // 멤버십 정보가 없는 경우 (일반 회원)
            renderFreeMembershipCard();
        }
    } catch (error) {
        console.error('멤버십 정보 조회 실패:', error);
        // 에러 발생 시에도 무료 회원으로 표시
        renderFreeMembershipCard();
    }
}

// ========================================
// 무료 회원 카드 렌더링
// ========================================
function renderFreeMembershipCard() {
    const cardElement = document.getElementById('membershipCard');
    const benefitsTitle = document.getElementById('benefitsTitle');

    // 혜택 타이틀 변경
    benefitsTitle.textContent = '프리미엄 혜택';

    cardElement.className = 'membership-card free';
    cardElement.innerHTML = `
        <div class="status-badge">현재 이용중</div>
        <div class="membership-title-wrapper">
            <span class="membership-icon">🏃‍♂️</span>
            <h1 class="membership-title">일반 회원</h1>
        </div>
        <p class="membership-description">멤버십을 구독하세요</p>
        <div class="button-group">
            <button class="btn btn-cancel" onclick="subscribeMembership()">멤버십 결제</button>
            <button class="btn btn-detail" onclick="showMembershipDetail()">멤버십 내역</button>
        </div>
    `;
}

// ========================================
// 프리미엄 회원 카드 렌더링
// ========================================
function renderMembershipCard(membership) {
    const cardElement = document.getElementById('membershipCard');
    const benefitsTitle = document.getElementById('benefitsTitle');

    // 혜택 타이틀 변경
    benefitsTitle.textContent = '프리미엄 혜택';

    // 상태에 따라 다른 UI 표시
    if (membership.membershipStatus === 'ACTIVE') {
        // 활성 상태
        renderActiveMembershipCard(cardElement, membership);
    } else if (membership.membershipStatus === 'CANCELED') {
        // 해지 신청 상태
        renderCanceledMembershipCard(cardElement, membership);
    } else if (membership.membershipStatus === 'EXPIRED') {
        // 만료 상태
        renderFreeMembershipCard();
    }
}

// ========================================
// 활성 멤버십 카드 렌더링
// ========================================
function renderActiveMembershipCard(cardElement, membership) {
    const nextBillingDate = formatDate(membership.nextBillingDate);

    cardElement.className = 'membership-card premium';
    cardElement.innerHTML = `
        <span class="new-badge">사용중</span>
        <div class="status-badge">현재 이용중</div>
        <div class="membership-title-wrapper">
            <span class="membership-icon">⭐</span>
            <h1 class="membership-title">프리미엄</h1>
        </div>
        <p class="membership-period">${nextBillingDate}까지</p>
        <div class="button-group">
            <button class="btn btn-cancel" onclick="cancelMembership()">멤버십 해지</button>
            <button class="btn btn-detail" onclick="showMembershipDetail()">멤버십 내역</button>
        </div>
    `;
}

// ========================================
// 해지 신청된 멤버십 카드 렌더링
// ========================================
function renderCanceledMembershipCard(cardElement, membership) {
    const endDate = formatDate(membership.endDate);

    cardElement.className = 'membership-card premium';
    cardElement.innerHTML = `
        <span class="new-badge" style="background-color: #ff9800;">해지예정</span>
        <div class="status-badge">해지 신청됨</div>
        <div class="membership-title-wrapper">
            <span class="membership-icon">⭐</span>
            <h1 class="membership-title">프리미엄</h1>
        </div>
        <p class="membership-period">${endDate}까지 사용 가능</p>
        <div class="button-group">
        <button class="btn btn-resubscribe" onclick="reactivateMembership()">해지 취소</button>
            <button class="btn btn-detail" onclick="showMembershipDetail()">멤버십 내역</button>
        </div>
    `;
}

// ========================================
// 멤버십 해지 취소 (재구독)
// ========================================
async function reactivateMembership() {
    const token = localStorage.getItem('accessToken');

    if (!confirm('멤버십 해지를 취소하시겠습니까?\n다시 정상적으로 자동 결제됩니다.')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/memberships`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();

        if (result.success) {
            alert(result.message || '멤버십 해지가 취소되었습니다!');
            // 페이지 새로고침
            location.reload();
        } else {
            alert(result.message || '해지 취소에 실패했습니다.');
        }
    } catch (error) {
        console.error('멤버십 해지 취소 실패:', error);
        alert('해지 취소 중 오류가 발생했습니다.');
    }
}

// ========================================
// 날짜 포맷 변환 함수
// ========================================
function formatDate(dateString) {
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}.${month}.${day}`;
}

// ========================================
// 멤버십 구독 (결제 페이지로 이동!)
// ========================================
function subscribeMembership() {
    const token = localStorage.getItem('accessToken');

    // 로그인 체크
    if (!token) {
        alert('로그인이 필요합니다.');
        window.location.href = '/login';
        return;
    }

    // 결제 페이지로 바로 이동!
    window.location.href = '/payment/pay';
}

// ========================================
// 멤버십 해지
// ========================================
async function cancelMembership() {
    const token = localStorage.getItem('accessToken');

    if (!confirm('정말로 멤버십을 해지하시겠습니까?\n남은 기간까지는 계속 사용 가능합니다.')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/memberships`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();

        if (result.success) {
            alert(result.message || '멤버십 해지 신청이 완료되었습니다.');
            // 페이지 새로고침
            location.reload();
        } else {
            alert(result.message || '멤버십 해지에 실패했습니다.');
        }
    } catch (error) {
        console.error('멤버십 해지 실패:', error);
        alert('멤버십 해지 중 오류가 발생했습니다.');
    }
}

// ========================================
// 멤버십 내역 보기 (결제 내역 페이지로 이동)
// ========================================
function showMembershipDetail() {
    window.location.href = '/payment/history';
}