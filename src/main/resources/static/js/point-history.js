const API_BASE_URL = '/api';

// 전역 변수
let currentFilter = 'ALL';  // ALL, EARN, USE
let lastId = null;
let isLoading = false;
let hasMore = true;

// JWT 토큰 가져오기
function getAccessToken() {
    let token = localStorage.getItem("accessToken");
    if (token) {
        return token.startsWith("Bearer ") ? token : `Bearer ${token}`;
    }
    const cookies = document.cookie.split(";");
    for (let cookie of cookies) {
        const [name, value] = cookie.trim().split("=");
        if (name === "accessToken" || name === "token") {
            return value.startsWith("Bearer ") ? value : `Bearer ${value}`;
        }
    }
    return null;
}

// 인증 헤더 가져오기
function getAuthHeaders(additionalHeaders = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...additionalHeaders,
    };
    const token = getAccessToken();
    if (token) {
        headers["Authorization"] = token;
    }
    return headers;
}

// API 호출
async function apiCall(url, options = {}) {
    const token = getAccessToken();

    if (!token) {
        alert('로그인이 필요합니다.');
        window.location.href = '/login';
        return;
    }

    try {
        const response = await fetch(url, {
            ...options,
            headers: getAuthHeaders(options.headers)
        });

        if (response.status === 401) {
            alert('로그인이 만료되었습니다.');
            localStorage.removeItem('accessToken');
            window.location.href = '/login';
            return;
        }

        const result = await response.json();

        if (!result.success) {
            throw new Error(result.message || '요청에 실패했습니다.');
        }

        return result.data;
    } catch (error) {
        console.error('API Error:', error);
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
            size: 20,
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
            document.getElementById('historyList').innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">📋</div>
                    <div>포인트 내역이 없습니다</div>
                </div>
            `;
        }

    } catch (error) {
        console.error('Failed to load history:', error);
        if (reset) {
            document.getElementById('historyList').innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">⚠️</div>
                    <div>내역을 불러올 수 없습니다</div>
                </div>
            `;
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
        
        const isEarn = history.pointType === 'EARN';
        const typeClass = isEarn ? 'earn' : 'use';
        const amountText = isEarn ? `+${formatNumber(history.amount)}` : `-${formatNumber(history.amount)}`;
        
        item.className = `history-item ${typeClass}`;

        const displayText = history.productName || getReasonText(history.reason);
        const formattedDate = formatDate(history.transactionDate || history.createdAt);

        item.innerHTML = `
            <div class="history-info">
                <div class="history-reason">${displayText}</div>
                <div class="history-date">${formattedDate}</div>
            </div>
            <div class="history-amount-wrapper">
                <div class="history-amount">${amountText} P</div>
                ${history.balance ? `<div class="history-balance">잔액 ${formatNumber(history.balance)} P</div>` : ''}
            </div>
        `;

        listElement.appendChild(item);
    });
}

// 숫자 포맷 (천 단위 콤마)
function formatNumber(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

// 날짜 포맷
function formatDate(dateString) {
    if (!dateString) return "-";
    try {
        const date = new Date(dateString);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        const hour = String(date.getHours()).padStart(2, "0");
        const minute = String(date.getMinutes()).padStart(2, "0");
        return `${year}.${month}.${day} ${hour}:${minute}`;
    } catch (e) {
        return "-";
    }
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
        'CREW_JOIN': '크루 가입',
        'PRODUCT_EXCHANGE': '상품 교환',
        'MEMBERSHIP_PURCHASE': '멤버십 구매',
        'MEMBERSHIP_TRIAL': '멤버십 체험',
        'COUPON_USE': '쿠폰 사용',
        'ADMIN': '관리자 지급'
    };
    return reasonMap[reason] || reason;
}

// 필터 변경
window.changeFilter = function(filter, event) {
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
    let scrollTimeout = null;
    
    const handleScroll = () => {
        if (scrollTimeout) {
            clearTimeout(scrollTimeout);
        }
        
        scrollTimeout = setTimeout(() => {
            if (isLoading || !hasMore) return;
            
            const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
            const scrollHeight = document.documentElement.scrollHeight;
            const clientHeight = window.innerHeight;
            
            const distanceFromBottom = scrollHeight - (scrollTop + clientHeight);
            
            // 하단 200px 이내에 도달하면 로드
            if (distanceFromBottom <= 200) {
                loadPointHistory(false);
            }
        }, 150);
    };
    
    window.addEventListener('scroll', handleScroll);
}

// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', () => {
    const token = getAccessToken();

    if (!token) {
        alert('로그인이 필요합니다.');
        window.location.href = '/login';
        return;
    }

    // 첫 페이지 로드
    loadPointHistory(true);

    // 무한 스크롤 설정
    setupInfiniteScroll();
});
