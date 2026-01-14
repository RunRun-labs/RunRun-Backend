const API_BASE_URL = 'http://localhost:8080/api';

// JWT 토큰 가져오기
function getToken() {

    return localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
}

// API 호출
async function apiCall(url, options = {}) {
    const token = getToken();
    console.log('토큰 확인:', token);

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

// 포인트 메인 조회
async function loadPointMain() {
    try {
        const data = await apiCall(`${API_BASE_URL}/points`);

        // 포인트 금액 표시
        document.getElementById('availablePoints').textContent =
            data.availablePoints.toLocaleString() + ' P';

        // 적립 방법 표시
        const earnMethodsHtml = data.earnMethods.map(method => `
            <div class="earn-method-item">
                <div class="earn-method-info">
                    <div class="earn-method-icon">${getMethodIcon(method.methodName)}</div>
                    <div class="earn-method-text">
                        <h3>${method.methodName}</h3>
                        <p>${method.description}</p>
                    </div>
                </div>
                <div class="earn-method-point">+${method.earnAmount} P</div>
            </div>
        `).join('');

        document.getElementById('earnMethods').innerHTML = earnMethodsHtml;

    } catch (error) {
        console.error('Failed to load point main:', error);
        document.getElementById('earnMethods').innerHTML = '<div class="empty-state">데이터를 불러올 수 없습니다</div>';
    }
}

// 아이콘 매핑
function getMethodIcon(methodName) {
    const icons = {
        '러닝 완료': '🏃',
        '경기 참여': '🏃',
        '출석 체크': '✅',
        '출석': '✅',
        '친구 초대': '👥',
        '친구 추천': '👥',
        '챌린지 성공': '📝'
    };
    return icons[methodName] || '⭐';
}