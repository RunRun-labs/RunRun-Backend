/**
 * 크루 생성 페이지 JavaScript
 * - 폼 유효성 검사
 * - 이미지 미리보기
 * - API 연동
 */

// ========================================
// 전역 변수
// ========================================
let uploadedImageUrl = null;
// ========================================
// 초기화
// ========================================
document.addEventListener('DOMContentLoaded', () => {
    initializeForm();
    initializeImageUpload();
    initLiveValidation();
});

// ========================================
// 실시간 Validation 초기화
// ========================================
function initLiveValidation() {

    const crewNameInput = document.getElementById('crewName');
    if (crewNameInput) {
        crewNameInput.addEventListener('input', () => validateCrewName());
        crewNameInput.addEventListener('blur', () => validateCrewName());
    }

    const descriptionInput = document.getElementById('description');
    if (descriptionInput) {
        descriptionInput.addEventListener('input', () => validateDescription());
        descriptionInput.addEventListener('blur', () => validateDescription());
        descriptionInput.addEventListener('focus', () => validatePreviousFields('description'));
    }

    const runningDistanceSelect = document.getElementById('runningDistance');
    if (runningDistanceSelect) {
        runningDistanceSelect.addEventListener('change', () => validateRunningDistance());
        runningDistanceSelect.addEventListener('focus', () => validatePreviousFields('runningDistance'));
    }

    const averagePaceSelect = document.getElementById('averagePace');
    if (averagePaceSelect) {
        averagePaceSelect.addEventListener('change', () => validateAveragePace());
        averagePaceSelect.addEventListener('focus', () => validatePreviousFields('averagePace'));
    }

    const activityRegionInput = document.getElementById('activityRegion');
    if (activityRegionInput) {
        activityRegionInput.addEventListener('input', () => validateActivityRegion());
        activityRegionInput.addEventListener('blur', () => validateActivityRegion());
        activityRegionInput.addEventListener('focus', () => validatePreviousFields('activityRegion'));
    }

    const regularMeetingTimeInput = document.getElementById('regularMeetingTime');
    if (regularMeetingTimeInput) {
        regularMeetingTimeInput.addEventListener('input', () => validateRegularMeetingTime());
        regularMeetingTimeInput.addEventListener('blur', () => validateRegularMeetingTime());
        regularMeetingTimeInput.addEventListener('focus', () => validatePreviousFields('regularMeetingTime'));
    }

    const imageInput = document.getElementById('imageInput');
    if (imageInput) {
        imageInput.addEventListener('change', () => validateImage());
    }

    const imageUploadBtn = document.querySelector('.image-upload-btn');
    if (imageUploadBtn) {
        imageUploadBtn.addEventListener('click', () => validateAllRequiredFieldsOnFocus());
    }
}

// ========================================
// 개별 필드 Validation 함수들
// ========================================
function validateCrewName() {
    const input = document.getElementById('crewName');
    const errorElement = document.getElementById('crewNameError');
    const value = input.value.trim();

    if (!value) {
        showFieldError(input, errorElement, '크루명은 필수입니다.');
        return false;
    }

    if (value.length > 100) {
        showFieldError(input, errorElement, '크루명은 100자 이내로 입력해주세요.');
        return false;
    }

    clearFieldError(input, errorElement);
    return true;
}

function validatePreviousFields(currentField) {

    const crewNameInput = document.getElementById('crewName');
    if (crewNameInput && !crewNameInput.value.trim()) {
        validateCrewName();
    }

    if (currentField === 'averagePace' || currentField === 'activityRegion' || currentField === 'regularMeetingTime' || currentField === 'image') {
        const runningDistanceSelect = document.getElementById('runningDistance');
        if (runningDistanceSelect && !runningDistanceSelect.value) {
            validateRunningDistance();
        }
    }

    if (currentField === 'activityRegion' || currentField === 'regularMeetingTime' || currentField === 'image') {
        const averagePaceSelect = document.getElementById('averagePace');
        if (averagePaceSelect && !averagePaceSelect.value) {
            validateAveragePace();
        }
    }

    if (currentField === 'regularMeetingTime' || currentField === 'image') {
        const activityRegionInput = document.getElementById('activityRegion');
        if (activityRegionInput && !activityRegionInput.value.trim()) {
            validateActivityRegion();
        }
    }

    if (currentField === 'image') {
        const regularMeetingTimeInput = document.getElementById('regularMeetingTime');
        if (regularMeetingTimeInput && !regularMeetingTimeInput.value.trim()) {
            validateRegularMeetingTime();
        }
    }
}

function validateAllRequiredFieldsOnFocus() {
    validatePreviousFields('image');
}

function validateDescription() {
    const input = document.getElementById('description');
    const errorElement = document.getElementById('descriptionError');
    if (!input || !errorElement) return true;

    const value = input.value.trim();

    if (value.length > 500) {
        showFieldError(input, errorElement, '크루 소개글은 500자 이내로 입력해주세요.');
        return false;
    }

    clearFieldError(input, errorElement);
    return true;
}

function validateActivityRegion() {
    const input = document.getElementById('activityRegion');
    const errorElement = document.getElementById('activityRegionError');
    if (!input || !errorElement) return true;

    const value = input.value.trim();

    if (!value) {
        showFieldError(input, errorElement, '활동 지역은 필수입니다.');
        return false;
    }

    if (value.length > 100) {
        showFieldError(input, errorElement, '활동 지역은 100자 이내로 입력해주세요.');
        return false;
    }

    clearFieldError(input, errorElement);
    return true;
}

function validateRegularMeetingTime() {
    const input = document.getElementById('regularMeetingTime');
    const errorElement = document.getElementById('regularMeetingTimeError');
    if (!input || !errorElement) return true;

    const value = input.value.trim();

    if (!value) {
        showFieldError(input, errorElement, '정기 모임 일시는 필수입니다.');
        return false;
    }

    if (value.length > 100) {
        showFieldError(input, errorElement, '정기 모임 일시는 100자 이내로 입력해주세요.');
        return false;
    }

    clearFieldError(input, errorElement);
    return true;
}

function validateRunningDistance() {
    const select = document.getElementById('runningDistance');
    const errorElement = document.getElementById('runningDistanceError');
    if (!select || !errorElement) return true;

    const value = select.value;

    if (!value) {
        showFieldError(select, errorElement, '러닝 거리는 필수입니다.');
        return false;
    }

    clearFieldError(select, errorElement);
    return true;
}

function validateAveragePace() {
    const select = document.getElementById('averagePace');
    const errorElement = document.getElementById('averagePaceError');
    if (!select || !errorElement) return true;

    const value = select.value;

    if (!value) {
        showFieldError(select, errorElement, '평균 페이스는 필수입니다.');
        return false;
    }

    clearFieldError(select, errorElement);
    return true;
}

function validateImage() {
    const imageInput = document.getElementById('imageInput');
    const errorElement = document.getElementById('imageError');

    clearFieldError(imageInput, errorElement);
    return true;
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
    }
    if (errorElement) {
        errorElement.textContent = '';
    }
}

// ========================================
// 폼 초기화
// ========================================
function initializeForm() {
    const form = document.getElementById('crewForm');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        if (!validateForm()) {
            return;
        }

        await createCrew();
    });
}

// ========================================
// 폼 유효성 검사
// ========================================
function validateForm() {
    let isValid = true;

    if (!validateCrewName()) isValid = false;

    if (!validateDescription()) isValid = false;

    if (!validateRunningDistance()) isValid = false;

    if (!validateAveragePace()) isValid = false;

    if (!validateActivityRegion()) isValid = false;

    if (!validateRegularMeetingTime()) isValid = false;

    return isValid;
}

// ========================================
// 이미지 업로드 초기화
// ========================================
function initializeImageUpload() {
    const imageInput = document.getElementById('imageInput');

    imageInput.addEventListener('change', async (e) => {
        const file = e.target.files[0];

        if (!file) return;

        if (!file.type.startsWith('image/')) {
            showError('이미지 파일만 업로드할 수 있습니다.');
            return;
        }

        if (file.size > 5 * 1024 * 1024) {
            showError('이미지 크기는 5MB 이하여야 합니다.');
            return;
        }

        showImagePreview(file);

        await uploadImageToServer(file);
    });
}

// ========================================
// 이미지 미리보기 표시
// ========================================
function showImagePreview(file) {
    const reader = new FileReader();

    reader.onload = (e) => {
        const previewImg = document.getElementById('previewImg');
        const imagePreview = document.getElementById('imagePreview');
        const uploadBtn = document.querySelector('.image-upload-btn');

        previewImg.src = e.target.result;
        imagePreview.style.display = 'block';
        uploadBtn.style.display = 'none';
    };

    reader.readAsDataURL(file);
}

// ========================================
// 이미지 제거
// ========================================
function removeImage() {
    const imageInput = document.getElementById('imageInput');
    const imagePreview = document.getElementById('imagePreview');
    const uploadBtn = document.querySelector('.image-upload-btn');

    imageInput.value = '';
    imagePreview.style.display = 'none';
    uploadBtn.style.display = 'flex';
    uploadedImageUrl = null;

    validateImage();
}

// ========================================
// 서버에 이미지 업로드
// ========================================
async function uploadImageToServer(file) {
    try {
        showLoading(true);

        const formData = new FormData();
        formData.append('file', file);
        formData.append('domain', 'CREW_IMAGE'); // FileDomainType

        const response = await fetch('/api/files/upload', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${getAccessToken()}`
            },
            body: formData
        });

        if (!response.ok) {
            throw new Error('이미지 업로드 실패');
        }

        const result = await response.json();

        if (result.data && result.data.url) {
            uploadedImageUrl = result.data.url;
            console.log('이미지 업로드 성공:', uploadedImageUrl);
        } else {
            throw new Error('이미지 URL을 받지 못했습니다.');
        }

    } catch (error) {
        console.error('이미지 업로드 에러:', error);
        showError('이미지 업로드에 실패했습니다. 다시 시도해주세요.');
        removeImage();
    } finally {
        showLoading(false);
    }
}

// ========================================
// 크루 생성 API 호출
// ========================================
async function createCrew() {
    try {
        showLoading(true);

        const defaultImageUrl = '';
        const imageUrl = uploadedImageUrl || defaultImageUrl;

        const requestData = {
            crewName: document.getElementById('crewName').value.trim(),
            crewDescription: document.getElementById('description').value.trim(),
            crewImageUrl: imageUrl,
            region: document.getElementById('activityRegion').value.trim(),
            distance: document.getElementById('runningDistance').value.trim(),
            averagePace: document.getElementById('averagePace').value,
            activityTime: document.getElementById('regularMeetingTime').value.trim()
        };

        console.log('크루 생성 요청 데이터:', requestData);

        const response = await fetch('/api/crews', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getAccessToken()}`
            },
            body: JSON.stringify(requestData)
        });

        const result = await response.json();

        if (!response.ok) {
            throw new Error(result.message || '크루 생성에 실패했습니다.');
        }

        alert('크루가 성공적으로 생성되었습니다!');
        window.location.href = '/crews';

    } catch (error) {
        console.error('크루 생성 에러:', error);
        showError(error.message || '크루 생성에 실패했습니다.');
    } finally {
        showLoading(false);
    }
}

// ========================================
// 유틸리티 함수
// ========================================
/**
 * 로딩 스피너 표시/숨김
 */
function showLoading(show) {
    const spinner = document.getElementById('loadingSpinner');
    const submitBtn = document.querySelector('.btn-submit');

    if (show) {
        spinner.style.display = 'flex';
        submitBtn.disabled = true;
    } else {
        spinner.style.display = 'none';
        submitBtn.disabled = false;
    }
}

/**
 * 에러 메시지 표시
 */
function showError(message) {
    alert(message);
}

/**
 * 액세스 토큰 가져오기
 */
function getAccessToken() {
    const token = localStorage.getItem('accessToken');

    if (!token) {
        showError('로그인이 필요합니다.');
        window.location.href = '/login';
        return null;
    }

    return token;
}
