const API_BASE_URL = '/api';

// 전역 변수
let currentFilter = 'ALL';  // ALL, EARN, USE
let lastId = null;
let isLoading = false;
let hasMore = true;

// JWT 토큰 가져오기
function getToken() {
    return localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
}

// API 호출
async function apiCall(url, options = {}) {
    const token = getToken();

    if (!token) {
        alert('로그인이 필요합니다.');
        window.location.href = '/auth/login';
        return;
    }

    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        }
    };

    const mergedOptions = {
        ...defaultOptions,
        ...options,
        headers: {
            ...defaultOptions.headers,
            ...options.headers
        }
    };

    try {
        const response = await fetch(url, mergedOptions);

        if (response.status === 401) {
            alert('로그인이 만료되었습니다.');
            localStorage.removeItem('accessToken');
            sessionStorage.removeItem('accessToken');
            window.location.href = '/auth/login';
            return;
        }

        const result = await response.json();

        if (!result.success) {
            throw new Error(result.message);
        }

        return result.data;
    } catch (error) {
        console.error('API Error:', error);
        alert(error.message || '서버 연결에 실패했습니다.');
        throw error;
    }
}

// 포인트 내역 조회 (커서 기반 페이징)
async function loadPointHistory(reset = false) {
    if (isLoading || (!hasMore && !reset)) return;

    if (reset) {
        lastId = null;
        hasMore = true;
        document.getElementById('historyList').innerHTML = '';
    }

    isLoading = true;
    showLoading(true);

    try {
        // 커서 기반 페이징
        const params = new URLSearchParams({
            size: 10,
            filter: currentFilter
        });

        if (lastId) {
            params.append('cursor', lastId);
        }

        const data = await apiCall(`${API_BASE_URL}/points/history?${params}`);

        // items, hasNext, nextCursor
        const histories = data.items || [];

        if (histories.length > 0) {
            displayHistory(histories);
        }

        // 페이징 정보 업데이트
        lastId = data.nextCursor;
        hasMore = data.hasNext;

        if (reset && histories.length === 0) {
            document.getElementById('historyList').innerHTML =
                '<div class="empty-state">내역이 없습니다</div>';
        }

    } catch (error) {
        console.error('Failed to load history:', error);
        if (reset) {
            document.getElementById('historyList').innerHTML =
                '<div class="empty-state">내역을 불러올 수 없습니다</div>';
        }
    } finally {
        isLoading = false;
        showLoading(false);
    }
}

// 내역 표시
function displayHistory(histories) {
    const listElement = document.getElementById('historyList');

    histories.forEach(history => {
        const item = document.createElement('div');
        item.className = 'history-item';

        const isEarn = history.pointType === 'EARN';
        const amountClass = isEarn ? 'earn' : 'use';
        const amountText = isEarn ? `+${history.amount.toLocaleString()}` : `-${history.amount.toLocaleString()}`;

        const displayText = history.productName || getReasonText(history.reason);

        item.innerHTML = `
            <div class="history-info">
                <div class="history-reason">${displayText}</div>
                <div class="history-date">${history.formattedDate || history.transactionDate}</div>
            </div>
            <div class="history-amount ${amountClass}">${amountText} P</div>
        `;

        listElement.appendChild(item);
    });
}

// reason 텍스트 변환
function getReasonText(reason) {
    const reasonMap = {
        'ATTENDANCE': '출석 체크',
        'RUNNING_COMPLETE': '러닝 완료',
        'INVITE': '친구 초대',
        'WEEKLY_MISSION': '주간 미션',
        'MONTHLY_MISSION': '월간 미션',
        'LUCKY_BOX': '럭키박스',
        'STREET_POINT': '스트릿 포인트',
        'EVENT': '이벤트',
        'CREW_JOIN': '크루 가입 신청',
        'PRODUCT_EXCHANGE': '상품 교환',
        'MEMBERSHIP_TRIAL': '멤버십 체험'
    };
    return reasonMap[reason] || reason;
}

// 필터 변경
function changeFilter(filter) {
    currentFilter = filter;

    // 버튼 활성화 상태 변경
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    event.currentTarget.classList.add('active');

    // 내역 새로고침
    loadPointHistory(true);
}

// 로딩 표시
function showLoading(show) {
    const loader = document.getElementById('loadingSpinner');
    if (loader) {
        loader.style.display = show ? 'block' : 'none';
    }
}

// 무한 스크롤
function setupInfiniteScroll() {
    const historyList = document.getElementById('historyList');
    if (!historyList) return;

    // 기존 sentinel 제거(중복 방지)
    const old = document.getElementById('scroll-sentinel');
    if (old) old.remove();

    const sentinel = document.createElement('div');
    sentinel.id = 'scroll-sentinel';
    historyList.after(sentinel);

    const observer = new IntersectionObserver(async ([entry]) => {
        if (!entry.isIntersecting) return;
        if (isLoading || !hasMore) return;
        await loadPointHistory(false);
    }, {root: null, rootMargin: '200px'});

    observer.observe(sentinel);
}

// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', () => {
    const token = getToken();

    if (!token) {
        alert('로그인이 필요합니다.');
        window.location.href = '/auth/login';
        return;
    }

    // 첫 페이지 로드 (10개)
    loadPointHistory(true);

    // 무한 스크롤
    setupInfiniteScroll();
});