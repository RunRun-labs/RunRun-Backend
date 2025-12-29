/**
 * 크루 신청 페이지 JavaScript
 * - 크루 상세 정보 로딩
 * - 폼 검증 및 실시간 미리보기
 * - 크루 가입 신청 API 연동
 */

// ========================================
// 전역 변수
// ========================================
let crewId = null;
let crewName = '';

// ========================================
// 글자 수 카운터 업데이트 함수
// ========================================
function updateCharCount(inputId, countId) {
    const input = document.getElementById(inputId);
    const counter = document.getElementById(countId);

    if (input && counter) {
        counter.textContent = input.value.length;
    }
}

// ========================================
// 초기화
// ========================================
document.addEventListener('DOMContentLoaded', async () => {
    console.log('크루 신청 페이지 초기화');

    const pathParts = window.location.pathname.split('/');
    const crewIdIndex = pathParts.indexOf('crews');
    if (crewIdIndex !== -1 && pathParts.length > crewIdIndex + 1) {
        crewId = pathParts[crewIdIndex + 1];
    }

    if (!crewId) {
        alert('크루 ID를 찾을 수 없습니다.');
        history.back();
        return;
    }

    const crewIdInput = document.getElementById('crewId');
    if (crewIdInput) {
        crewIdInput.value = crewId;
    }

    await loadCrewDetail();

    initializeEventListeners();

    initializePreview();
});

// ========================================
// 크루 상세 정보 로드
// ========================================
async function loadCrewDetail() {
    try {
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
        const data = result && typeof result === 'object' ? (result.data || result) : null;

        if (!data) {
            throw new Error('크루 데이터가 없습니다.');
        }

        // 크루명 표시
        crewName = data.crewName || '';
        const crewNameInput = document.getElementById('crewName');
        if (crewNameInput) {
            crewNameInput.value = crewName;
        }

        console.log('크루 상세 정보 로드 완료:', data);

    } catch (error) {
        console.error('크루 상세 정보 로드 실패:', error);
        alert('크루 정보를 불러오는데 실패했습니다.\n' + error.message);
        setTimeout(() => {
            history.back();
        }, 2000);
    }
}

// ========================================
// 이벤트 리스너 초기화
// ========================================
function initializeEventListeners() {
    const form = document.getElementById('crewJoinForm');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        await submitJoinRequest();
    });

    initLiveValidation();

    // 실시간 미리보기 업데이트
    const introduction = document.getElementById('introduction');
    const distance = document.getElementById('distance');
    const pace = document.getElementById('pace');
    const region = document.getElementById('region');

    introduction.addEventListener('input', updatePreview);
    distance.addEventListener('change', updatePreview);
    pace.addEventListener('change', updatePreview);
    region.addEventListener('input', updatePreview);
}

// ========================================
// 실시간 미리보기 초기화 및 업데이트
// ========================================
function initializePreview() {
    updatePreview();
}

function updatePreview() {
    const distanceSelect = document.getElementById('distance');
    const paceSelect = document.getElementById('pace');
    const regionInput = document.getElementById('region');

    const distanceValue = distanceSelect.value;
    const paceValue = paceSelect.value;
    const regionValue = regionInput.value.trim();

    const previewPlaceholder = document.querySelector('.preview-box__placeholder');
    const previewContent = document.getElementById('previewContent');
    const previewDistance = document.getElementById('previewDistance');
    const previewPace = document.getElementById('previewPace');
    const previewRegion = document.getElementById('previewRegion');

    const distanceText = getDistanceText(distanceValue);
    const paceText = getPaceText(paceValue);

    // 미리보기 표시 여부 결정
    const hasData = distanceValue || paceValue || regionValue;

    if (hasData) {
        previewPlaceholder.style.display = 'none';
        previewContent.style.display = 'flex';

        if (distanceValue) {
            previewDistance.textContent = `거리 : ${distanceText}`;
            previewDistance.style.display = 'block';
        } else {
            previewDistance.style.display = 'none';
        }

        if (paceValue) {
            previewPace.textContent = `페이스 : ${paceText}`;
            previewPace.style.display = 'block';
        } else {
            previewPace.style.display = 'none';
        }

        if (regionValue) {
            previewRegion.textContent = `위치 : ${regionValue}`;
            previewRegion.style.display = 'block';
        } else {
            previewRegion.style.display = 'none';
        }
    } else {
        previewPlaceholder.style.display = 'block';
        previewContent.style.display = 'none';
    }
}

// ========================================
// enum 값 → 표시용 텍스트 변환
// ========================================
function getDistanceText(enumValue) {
    const map = {
        'UNDER_3KM': '3km 미만',
        'KM_3': '3km',
        'KM_5': '5km',
        'KM_10': '10km',
        'OVER_10KM': '10km 이상'
    };
    return map[enumValue] || enumValue;
}

function getPaceText(enumValue) {
    const map = {
        'UNDER_3_MIN': '2분/km 이하',
        'MIN_3_TO_4': '3~4분/km',
        'MIN_5_TO_6': '5~6분/km',
        'MIN_7_TO_8': '7~8분/km',
        'OVER_9_MIN': '9분/km 이상'
    };
    return map[enumValue] || enumValue;
}

// ========================================
// 실시간 Validation 초기화
// ========================================
function initLiveValidation() {
    // 자기소개 (선택사항, 최대 100자)
    const introductionInput = document.getElementById('introduction');
    if (introductionInput) {
        introductionInput.addEventListener('input', () => {
            validateIntroduction();
            updateCharCount('introduction', 'introductionCount');
        });
        introductionInput.addEventListener('blur', () => {
            validateIntroduction();
        });
    }

    // 희망 러닝 거리 (필수)
    const distanceSelect = document.getElementById('distance');
    if (distanceSelect) {
        distanceSelect.addEventListener('change', () => validateDistance());
        distanceSelect.addEventListener('blur', () => validateDistance());
    }

    // 평균 페이스 (필수)
    const paceSelect = document.getElementById('pace');
    if (paceSelect) {
        paceSelect.addEventListener('change', () => validatePace());
        paceSelect.addEventListener('blur', () => validatePace());
        paceSelect.addEventListener('focus', () => validatePreviousFields('pace'));
    }

    // 위치 (필수, 최대 100자)
    const regionInput = document.getElementById('region');
    if (regionInput) {
        regionInput.addEventListener('input', () => {
            validateRegion();
            updateCharCount('region', 'regionCount');
        });
        regionInput.addEventListener('blur', () => validateRegion());
        regionInput.addEventListener('focus', () => validatePreviousFields('region'));
    }
}

// ========================================
// 순서대로 이전 필드 검증
// ========================================
function validatePreviousFields(currentField) {
    // 평균 페이스 클릭 시 → 거리 먼저 체크
    if (currentField === 'pace') {
        const distanceSelect = document.getElementById('distance');
        if (!distanceSelect.value) {
            validateDistance();
            return;
        }
    }
    // 지역 클릭 → 거리 + 페이스 둘 다 체크
    if (currentField === 'region') {
        const distanceSelect = document.getElementById('distance');
        const paceSelect = document.getElementById('pace');

        if (!distanceSelect.value) {
            validateDistance();
        }

        if (!paceSelect.value) {
            validatePace();
        }
    }
}

// ========================================
// 개별 필드 Validation 함수들
// ========================================
// 자기소개 검증 (DTO: 선택사항, 최대 100자)
function validateIntroduction() {
    const input = document.getElementById('introduction');
    const errorElement = document.getElementById('introductionError');
    const charCount = document.getElementById('introductionCount');
    if (!input || !errorElement) return true;

    const value = input.value;

    if (value.length > 100) {

        input.value = value.slice(0, 100);

        showFieldError(input, errorElement, '자기소개는 100자 이내로 입력해주세요.');
        if (charCount) {
            charCount.parentElement.classList.add('over-limit');
        }
        showToast('자기소개는 100자 이내로 입력해주세요.');
        return false;
    }

    clearFieldError(input, errorElement);
    if (charCount) {
        charCount.parentElement.classList.remove('over-limit');
    }
    return true;
}


// 희망 러닝 거리 검증 (DTO: @NotNull - 필수)
function validateDistance() {
    const select = document.getElementById('distance');
    const errorElement = document.getElementById('distanceError');
    if (!select || !errorElement) return true;

    const value = select.value;

    if (!value) {
        showFieldError(select, errorElement, '희망 러닝 거리를 선택해주세요.');
        return false;
    }

    clearFieldError(select, errorElement);
    return true;
}

// 평균 페이스 검증 (DTO: @NotNull - 필수)
function validatePace() {
    const select = document.getElementById('pace');
    const errorElement = document.getElementById('paceError');
    if (!select || !errorElement) return true;

    const value = select.value;

    if (!value) {
        showFieldError(select, errorElement, '평균 페이스를 선택해주세요.');
        return false;
    }

    clearFieldError(select, errorElement);
    return true;
}

// 위치 검증 (DTO: @NotBlank, 최대 100자 - 필수)
function validateRegion() {
    const input = document.getElementById('region');
    const errorElement = document.getElementById('regionError');
    const charCount = document.getElementById('regionCount');
    if (!input || !errorElement) return true;

    const value = input.value;

    if (value.length > 100) {

        input.value = value.slice(0, 100);
        showFieldError(input, errorElement, '러닝 희망 장소는 100자 이내로 입력해주세요.');
        if (charCount) {
            charCount.parentElement.classList.add('over-limit');
        }
        showToast('러닝 희망 장소는 100자 이내로 입력해주세요.');

        return false;
    }

    if (!value.trim()) {
        showFieldError(input, errorElement, '러닝 희망 장소를 입력해주세요.');
        return false;
    }

    clearFieldError(input, errorElement);
    if (charCount) {
        charCount.parentElement.classList.remove('over-limit');
    }
    return true;
}

// ========================================
// 폼 검증 (제출 시)
// ========================================
function validateForm() {
    console.log('전체 폼 검증 시작');

    let isValid = true;
    let firstErrorField = null;
    let errorCount = 0;

    // 모든 필드를 한번에 검증!
    // 자기소개 (선택이지만 100자 제한 체크)
    if (!validateIntroduction()) {
        isValid = false;
        errorCount++;
        if (!firstErrorField) firstErrorField = document.getElementById('introduction');
    }

    // 희망 러닝 거리 (필수)
    if (!validateDistance()) {
        isValid = false;
        errorCount++;
        if (!firstErrorField) firstErrorField = document.getElementById('distance');
    }

    // 평균 페이스 (필수)
    if (!validatePace()) {
        isValid = false;
        errorCount++;
        if (!firstErrorField) firstErrorField = document.getElementById('pace');
    }

    // 위치 (필수)
    if (!validateRegion()) {
        isValid = false;
        errorCount++;
        if (!firstErrorField) firstErrorField = document.getElementById('region');
    }

    if (!isValid) {
        console.log(`폼 검증 실패 - ${errorCount}개 항목 확인 필요`);

        if (errorCount === 1) {
            showToast('입력 항목을 확인해주세요.');
        } else {
            showToast(`${errorCount}개 항목을 확인해주세요.`);
        }

        if (firstErrorField) {
            firstErrorField.scrollIntoView({
                behavior: 'smooth',
                block: 'center'
            });

            setTimeout(() => {
                firstErrorField.focus();

                if (firstErrorField.tagName === 'SELECT') {
                    try {
                        firstErrorField.click();
                    } catch (e) {
                    }
                }
            }, 300);
        }
    } else {
        console.log('폼 검증 성공');
    }

    return isValid;
}

// ========================================
// 에러 표시/제거 헬퍼 함수
// ========================================
function showFieldError(inputElement, errorElement, message) {
    if (inputElement) {
        inputElement.classList.add('error');
    }
    if (errorElement) {
        errorElement.textContent = message;
    }
}

function clearFieldError(inputElement, errorElement) {
    if (inputElement) {
        inputElement.classList.remove('error');
        inputElement.style.borderColor = '';
    }
    if (errorElement) {
        errorElement.textContent = '';
    }
}

// ========================================
// 크루 가입 신청 제출
// ========================================
async function submitJoinRequest() {
    if (!validateForm()) {
        return;
    }

    const submitBtn = document.getElementById('submitBtn');
    submitBtn.disabled = true;
    submitBtn.textContent = '신청 중...';

    try {
        const introduction = document.getElementById('introduction').value.trim();
        const distance = document.getElementById('distance').value;
        const pace = document.getElementById('pace').value;
        const region = document.getElementById('region').value.trim();

        const requestData = {
            introduction: introduction || null,
            distance: distance,
            pace: pace,
            region: region
        };

        console.log('크루 가입 신청 요청 데이터:', requestData);

        const token = getAccessToken();
        if (!token) {
            throw new Error('로그인이 필요합니다.');
        }

        const response = await fetch(`/api/crews/${crewId}/join`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(requestData)
        });

        const result = await response.json();
        console.log('크루 가입 신청 API 응답:', result);

        if (!response.ok) {
            const errorMessage = result?.message || result?.error || '크루 가입 신청에 실패했습니다.';

            // 1인 1크루 검증 에러 처리
            if (errorMessage.includes('이미 가입한 크루') || errorMessage.includes('ALREADY_JOINED_CREW')) {
                alert('이미 가입한 크루가 있습니다.\n1인 1크루만 가입 가능합니다.');
                window.location.href = `/crews/${crewId}`;
                return;
            }

            // 이미 신청한 크루 검증 에러 처리
            if (errorMessage.includes('이미 가입 신청한 크루') || errorMessage.includes('ALREADY_REQUESTED_JOIN')) {
                alert('이미 가입 신청한 크루입니다.\n크루장의 승인을 기다려주세요.');
                window.location.href = `/crews/${crewId}`;
                return;
            }

            throw new Error(errorMessage);
        }

        if (result && result.success === false) {
            const errorMessage = result.message || '크루 가입 신청에 실패했습니다.';

            // 1인 1크루 검증 에러 처리
            if (errorMessage.includes('이미 가입한 크루') || errorMessage.includes('ALREADY_JOINED_CREW')) {
                alert('이미 가입한 크루가 있습니다.\n1인 1크루만 가입 가능합니다.');
                window.location.href = `/crews/${crewId}`;
                return;
            }

            // 이미 신청한 크루 검증 에러 처리
            if (errorMessage.includes('이미 가입 신청한 크루') || errorMessage.includes('ALREADY_REQUESTED_JOIN')) {
                alert('이미 가입 신청한 크루입니다.\n크루장의 승인을 기다려주세요.');
                window.location.href = `/crews/${crewId}`;
                return;
            }

            throw new Error(errorMessage);
        }

        // 성공 메시지 표시 후 크루 상세보기로 이동
        alert('크루 가입 신청이 완료되었습니다!\n크루장의 승인을 기다려주세요.');
        // 페이지를 새로고침하여 상태 업데이트 (캐시 방지를 위해 timestamp 추가)
        window.location.href = `/crews/${crewId}?t=${Date.now()}`;

    } catch (error) {
        console.error('크루 가입 신청 실패:', error);
        alert(error.message || '크루 가입 신청 중 오류가 발생했습니다.');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = '크루 신청하기';
    }
}

// ========================================
// 유틸리티 함수
// ========================================
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

// ========================================
// 토스트 메시지 표시
// ========================================
/**
 * 토스트 메시지를 화면에 표시합니다.
 * @param {string} message - 표시할 메시지
 * @param {number} duration - 표시 시간(ms), 기본값 2000ms
 */
let toastTimer = null;

function showToast(message, duration = 2000) {
    const toast = document.getElementById('toast');
    if (!toast) {
        console.warn('토스트 요소를 찾을 수 없습니다.');
        return;
    }

    toast.textContent = message;
    toast.classList.add('show');

    if (toastTimer) {
        clearTimeout(toastTimer);
    }

    toastTimer = setTimeout(() => {
        toast.classList.remove('show');
    }, duration);
}

// 토스트 애니메이션 스타일 추가
if (!document.getElementById('toast-styles')) {
    const style = document.createElement('style');
    style.id = 'toast-styles';
    style.textContent = `
        @keyframes fadeInOut {
            0% { 
                opacity: 0; 
                transform: translateX(-50%) translateY(20px); 
            }
            15% { 
                opacity: 1; 
                transform: translateX(-50%) translateY(0); 
            }
            85% { 
                opacity: 1; 
                transform: translateX(-50%) translateY(0); 
            }
            100% { 
                opacity: 0; 
                transform: translateX(-50%) translateY(-20px); 
            }
        }
    `;
    document.head.appendChild(style);
}

