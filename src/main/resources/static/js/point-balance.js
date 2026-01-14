const API_BASE_URL = '/api';

// JWT 토큰 가져오기
function getToken() {
    return localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
}

// API 호출
async function apiCall(url, options = {}) {
    const token = getToken();

    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': token ? `Bearer ${token}` : ''
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

// 포인트 잔액 조회
async function loadPointBalance() {
    try {
        const data = await apiCall(`${API_BASE_URL}/points`);

        // 사용 가능 포인트
        document.getElementById('balanceAmount').textContent =
            data.availablePoints.toLocaleString() + ' P';

        // 이번 달 적립/사용
        document.getElementById('earnedAmount').textContent =
            '+' + data.summary.earnedPoints.toLocaleString() + ' P';
        document.getElementById('usedAmount').textContent =
            '-' + data.summary.usedPoints.toLocaleString() + ' P';

        // 소멸 예정 포인트
        if (data.upcomingExpiry && data.upcomingExpiry.expiringPoints > 0) {
            document.getElementById('expiryInfo').textContent =
                `이번 달 ${data.upcomingExpiry.expiringPoints.toLocaleString()}P 소멸 예정`;
        } else {
            document.getElementById('expiryInfo').textContent =
                '이번 달 소멸 예정 포인트 없음';
        }

        // 총 적립 포인트
        document.getElementById('totalPoints').textContent =
            data.summary.totalAccumulated.toLocaleString() + ' P';

    } catch (error) {
        console.error('Failed to load balance:', error);
    }
}

// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', () => {
    const token = getToken();

    if (!token) {
        alert('로그인이 필요합니다.');
        window.location.href = '/auth/login';
        return;
    }

    loadPointBalance();
});