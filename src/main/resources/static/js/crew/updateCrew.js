/**
 * 크루 수정 페이지 JavaScript
 * - 기존 크루 데이터 로딩
 * - 정보 수정
 * - 모집 상태 변경
 */

// ========================================
// 전역 변수
// ========================================
let currentCrewData = null;
let selectedImageFile = null;
let uploadedImageUrl = null;

// ========================================
// URL에서 크루 ID 추출
// ========================================
function getCrewIdFromUrl() {
    const pathParts = window.location.pathname.split('/');
    const crewIdIndex = pathParts.indexOf('crews');
    if (crewIdIndex !== -1 && pathParts.length > crewIdIndex + 1) {
        return pathParts[crewIdIndex + 1];
    }
    return null;
}

// ========================================
// 크루장 권한 확인
// ========================================
async function checkLeaderPermission() {
    try {
        const crewId = getCrewIdFromUrl();

        if (!crewId) {
            alert('크루 ID를 찾을 수 없습니다.');
            window.history.back();
            return false;
        }

        const token = localStorage.getItem('accessToken');
        const userId = localStorage.getItem('userId');

        if (!token || !userId) {
            alert('로그인이 필요합니다.');
            window.location.href = '/auth/login';
            return false;
        }

        // 크루원 목록 조회
        const response = await fetch(`/api/crews/${crewId}/users`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('크루 정보를 불러올 수 없습니다.');
        }

        const result = await response.json();
        const members = result.data || result;

        // 내 정보 찾기
        const myInfo = members.find(member => member.userId === parseInt(userId));

        // 크루장인지 확인
        if (!myInfo || myInfo.role !== 'LEADER') {
            alert('크루장만 접근할 수 있습니다.');
            window.location.href = `/crews/${crewId}`;
            return false;
        }

        console.log('권한 확인 완료: 크루장');
        return true;

    } catch (error) {
        console.error('권한 확인 실패:', error);
        alert('접근 권한이 없습니다.');
        window.history.back();
        return false;
    }
}

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
    // 권한 확인(가장 먼저)
    const hasPermission = await checkLeaderPermission();
    if (!hasPermission) {
        return;
    }

    // 권한 있으면 페이지 로드
    await loadCrewData();
    initializeForm();
    initializeImageUpload();
    initializeRecruitButton();
    initLiveValidation();
});

// ========================================
// 기존 크루 데이터 로딩
// ========================================
async function loadCrewData() {
    try {
        showLoading(true);

        const crewId = getCrewIdFromUrl();

        if (!crewId) {
            throw new Error('크루 ID를 찾을 수 없습니다.');
        }

        const crewIdInput = document.getElementById('crewId');
        if (crewIdInput) {
            crewIdInput.value = crewId;
        }

        const response = await fetch(`/api/crews/${crewId}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${getAccessToken()}`
            }
        });

        if (!response.ok) {
            throw new Error('크루 정보를 불러오지 못했습니다.');
        }

        const result = await response.json();

        const data = result && typeof result === 'object' ? (result.data || result) : null;
        if (data) {
            currentCrewData = data;
            fillFormWithData(currentCrewData);
        } else {
            throw new Error('크루 데이터가 없습니다.');
        }

    } catch (error) {
        console.error('크루 데이터 로딩 에러:', error);
        showError(error.message || '크루 정보를 불러오는데 실패했습니다.');
        setTimeout(() => {
            history.back();
        }, 2000);
    } finally {
        showLoading(false);
    }
}

// ========================================
// 폼에 기존 데이터 채우기
// ========================================
function fillFormWithData(data) {
    document.getElementById('crewName').value = data.crewName || '';
    document.getElementById('description').value = data.crewDescription || data.description || '';
    document.getElementById('runningDistance').value = data.distance || '';
    document.getElementById('averagePace').value = data.averagePace || '';
    document.getElementById('activityRegion').value = data.region || '';
    document.getElementById('regularMeetingTime').value = data.regularMeetingTime || data.activityTime || '';

    if (data.crewImageUrl) {
        uploadedImageUrl = data.crewImageUrl;
        showExistingImage(data.crewImageUrl);
    }

    const recruitBtn = document.getElementById('btnCloseRecruit');
    const currentStatus = data.crewRecruitStatus || data.recruitStatus;

    if (currentStatus === 'CLOSED') {

        recruitBtn.textContent = '모집 열기';
        recruitBtn.style.backgroundColor = 'var(--primary-color)';
        recruitBtn.style.color = 'var(--text-primary)';
    } else {

        recruitBtn.textContent = '모집 마감';
        recruitBtn.style.backgroundColor = 'var(--danger-color)';
        recruitBtn.style.color = 'var(--bg-white)';
    }

    console.log('폼 데이터 채우기 완료:', data);
}

// ========================================
// 기존 이미지 표시
// ========================================
function showExistingImage(imageUrl) {
    const previewImg = document.getElementById('previewImg');
    const imagePreview = document.getElementById('imagePreview');
    const uploadBtn = document.getElementById('uploadBtn');

    previewImg.src = imageUrl;
    imagePreview.style.display = 'block';
    uploadBtn.style.display = 'none';
}

// ========================================
// 폼 초기화
// ========================================
function initializeForm() {
    const form = document.getElementById('crewUpdateForm');

    if (!form) {
        console.error('폼을 찾을 수 없습니다: crewUpdateForm');
        return;
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        console.log('폼 제출 이벤트 발생');

        if (!validateForm()) {
            console.warn('폼 유효성 검사 실패');
            return;
        }

        console.log('폼 유효성 검사 통과 - updateCrew 호출');
        await updateCrew();
    });

    const submitBtn = form.querySelector('button[type="submit"]');
    if (submitBtn) {
        submitBtn.addEventListener('click', async (e) => {
            e.preventDefault();
            console.log('수정하기 버튼 클릭');

            if (!validateForm()) {
                console.warn('폼 유효성 검사 실패');
                return;
            }

            console.log('폼 유효성 검사 통과 - updateCrew 호출');
            await updateCrew();
        });
    }
}

// ========================================
// 폼 유효성 검사
// ========================================
function validateForm() {
    console.log('폼 유효성 검사 시작');

    let isValid = true;

    if (!validateCrewName()) {
        isValid = false;
    }

    if (!validateDescription()) {
        isValid = false;
    }

    if (!validateRunningDistance()) {
        isValid = false;
    }

    if (!validateAveragePace()) {
        isValid = false;
    }

    if (!validateActivityRegion()) {
        isValid = false;
    }

    if (!validateRegularMeetingTime()) {
        isValid = false;
    }

    if (isValid) {
        console.log('폼 유효성 검사 통과');
    } else {
        console.warn('폼 유효성 검사 실패');
    }

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

        if (file.size > 10 * 1024 * 1024) {
            showError('이미지 크기는 10MB 이하여야 합니다.');
            return;
        }

        selectedImageFile = file;
        showImagePreview(file);

    });
}

// ========================================
// 이미지 미리보기
// ========================================
function showImagePreview(file) {
    const reader = new FileReader();

    reader.onload = (e) => {
        const previewImg = document.getElementById('previewImg');
        previewImg.src = e.target.result;
    };

    reader.readAsDataURL(file);
}

// ========================================
// 서버에 이미지 업로드
// ========================================
async function uploadImageToServer(file) {
    try {
        showLoading(true);

        const formData = new FormData();
        formData.append('file', file);
        formData.append('domain', 'CREW_IMAGE');

        const crewId = document.getElementById('crewId')?.value;
        if (crewId) {
            formData.append('refId', crewId);
        }

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

        if (result.data) {
            uploadedImageUrl = result.data.url || result.data;
            console.log('이미지 업로드 성공:', uploadedImageUrl);

            validateImage();
        } else {
            throw new Error('이미지 URL을 받지 못했습니다.');
        }

    } catch (error) {
        console.error('이미지 업로드 에러:', error);
        showError('이미지 업로드에 실패했습니다.');
    } finally {
        showLoading(false);
    }
}

// ========================================
// 크루 정보 수정 API 호출
// ========================================
async function updateCrew() {
    try {
        console.log('updateCrew 함수 시작');
        showLoading(true);

        const crewIdInput = document.getElementById('crewId');
        if (!crewIdInput || !crewIdInput.value) {
            throw new Error('크루 ID를 찾을 수 없습니다.');
        }
        const crewId = crewIdInput.value;
        console.log('크루 ID:', crewId);

        const region = document.getElementById('activityRegion').value.trim();
        const distance = document.getElementById('runningDistance').value.trim();
        const averagePace = document.getElementById('averagePace').value.trim();
        const activityTime = document.getElementById('regularMeetingTime').value.trim();

        if (!region) throw new Error('지역을 입력해주세요.');
        if (!distance) throw new Error('러닝 거리를 입력해주세요.');
        if (!averagePace) throw new Error('평균 페이스를 입력해주세요.');
        if (!activityTime) throw new Error('정기 모임 시간을 입력해주세요.');

        // FormData 생성!
        const formData = new FormData();

        // JSON 데이터
        const crewData = {
            crewName: document.getElementById('crewName').value.trim(),
            crewDescription: document.getElementById('description').value.trim(),
            region: region,
            distance: distance,
            averagePace: averagePace,
            activityTime: activityTime
        };

        formData.append('crew', new Blob([JSON.stringify(crewData)], {
            type: 'application/json'
        }));

        // 이미지 파일 추가 (선택 사항!)
        if (selectedImageFile) {
            formData.append('crewImageFile', selectedImageFile);
            console.log('새 이미지 파일:', selectedImageFile.name);
        } else {
            console.log('이미지 변경 없음 - 기존 이미지 유지');
        }

        console.log('크루 수정 요청 데이터:', crewData);

        // API 호출
        const response = await fetch(`/api/crews/${crewId}`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${getAccessToken()}`
            },
            body: formData
        });

        const result = await response.json();
        console.log('크루 수정 API 응답:', result);
        console.log('HTTP 상태 코드:', response.status);

        if (!response.ok) {
            const errorMessage = result?.message || result?.error || '크루 수정에 실패했습니다.';
            console.error('크루 수정 실패:', errorMessage);
            throw new Error(errorMessage);
        }

        if (result && result.success === false) {
            const errorMessage = result.message || '크루 수정에 실패했습니다.';
            console.error('크루 수정 실패:', errorMessage);
            throw new Error(errorMessage);
        }

        alert('크루 정보가 성공적으로 수정되었습니다!');
        console.log(`크루 수정 완료 - 상세보기로 이동: /crews/${crewId}`);
        window.location.href = `/crews/${crewId}`;

    } catch (error) {
        console.error('크루 수정 에러:', error);
        console.error('에러 상세:', {
            message: error.message,
            stack: error.stack,
            name: error.name
        });

        const errorMessage = error.message || '크루 수정에 실패했습니다.';
        alert(`크루 수정 실패:\n${errorMessage}\n\n콘솔을 확인해주세요.`);
        showError(errorMessage);
    } finally {
        showLoading(false);
        console.log('updateCrew 함수 종료');
    }
}


// ========================================
// 모집 상태 변경 버튼 초기화
// ========================================
function initializeRecruitButton() {
    const recruitBtn = document.getElementById('btnCloseRecruit');

    recruitBtn.addEventListener('click', async () => {
        const currentStatus = currentCrewData?.crewRecruitStatus || currentCrewData?.recruitStatus;
        const isClosed = currentStatus === 'CLOSED';

        let confirmMessage = '';
        if (isClosed) {
            confirmMessage = '크루 모집을 다시 열겠습니까?';
        } else {
            confirmMessage = '크루 모집을 마감하시겠습니까?';
        }

        const confirmed = confirm(confirmMessage);
        if (!confirmed) return;

        await toggleRecruitStatus();
    });

    const deleteBtn = document.getElementById('btnDeleteCrew');
    if (deleteBtn) {
        deleteBtn.addEventListener('click', async () => {
            const confirmed = confirm('정말로 크루를 해체하시겠습니까? 이 작업은 되돌릴 수 없습니다.');
            if (!confirmed) return;

            try {
                showLoading(true);
                const crewId = document.getElementById('crewId').value;
                const response = await fetch(`/api/crews/${crewId}`, {
                    method: 'DELETE',
                    headers: {
                        'Authorization': `Bearer ${getAccessToken()}`
                    }
                });

                const result = await response.json().catch(() => ({}));
                if (!response.ok || (result && result.success === false)) {
                    throw new Error((result && result.message) || '크루 해체에 실패했습니다.');
                }

                alert('크루가 해체되었습니다.');
                window.location.href = '/crews';
            } catch (error) {
                console.error('크루 해체 에러:', error);
                showError(error.message || '크루 해체에 실패했습니다.');
            } finally {
                showLoading(false);
            }
        });
    }
}

// ========================================
// 모집 상태 변경 (토글)
// ========================================
async function toggleRecruitStatus() {
    try {
        showLoading(true);

        const crewId = document.getElementById('crewId').value;
        const currentStatus = currentCrewData?.crewRecruitStatus || currentCrewData?.recruitStatus;

        const newStatus = (currentStatus === 'RECRUITING' || currentStatus === 'OPEN') ? 'CLOSED' : 'RECRUITING';

        const response = await fetch(`/api/crews/${crewId}/status`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getAccessToken()}`
            },
            body: JSON.stringify({
                recruitStatus: newStatus
            })
        });

        const result = await response.json().catch(() => ({}));

        if (!response.ok || (result && result.success === false)) {
            throw new Error((result && result.message) || '모집 상태 변경에 실패했습니다.');
        }

        const message = newStatus === 'CLOSED'
            ? '크루 모집이 마감되었습니다.'
            : '크루 모집이 재개되었습니다.';

        alert(message);

        location.reload();

    } catch (error) {
        console.error('모집 상태 변경 에러:', error);
        showError(error.message || '모집 상태 변경에 실패했습니다.');
    } finally {
        showLoading(false);
    }
}

// ========================================
// 유틸리티 함수
// ========================================

function showLoading(show) {
    const spinner = document.getElementById('loadingSpinner');
    const submitBtn = document.querySelector('.btn-submit');

    if (show) {
        spinner.style.display = 'flex';
        if (submitBtn) submitBtn.disabled = true;
    } else {
        spinner.style.display = 'none';
        if (submitBtn) submitBtn.disabled = false;
    }
}

function showError(message) {
    alert(message);
}

function getAccessToken() {
    const token = localStorage.getItem('accessToken');

    if (!token) {
        showError('로그인이 필요합니다.');
        window.location.href = '/auth/login';
        return null;
    }

    return token;
}

// ========================================
// 실시간 Validation 초기화
// ========================================
function initLiveValidation() {

    const crewNameInput = document.getElementById('crewName');
    if (crewNameInput) {
        crewNameInput.addEventListener('input', () => {
            validateCrewName();
            updateCharCount('crewName', 'crewNameCount');
        });
        crewNameInput.addEventListener('blur', () => validateCrewName());
    }

    const descriptionInput = document.getElementById('description');
    if (descriptionInput) {
        descriptionInput.addEventListener('input', () => {
            validateDescription();
            updateCharCount('description', 'crewDescriptionCount');
        });
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

    const imageEditBtn = document.querySelector('.btn-edit-image');
    if (imageEditBtn) {
        imageEditBtn.addEventListener('click', () => validateAllRequiredFieldsOnFocus());
    }
}

// ========================================
// 개별 필드 Validation 함수들 (DTO 규칙에 맞춤)
// ========================================

function validateCrewName() {
    const input = document.getElementById('crewName');
    const errorElement = document.getElementById('crewNameError');
    const charCount = document.getElementById('crewNameCount');
    if (!input || !errorElement) return true; // 요소가 없으면 통과

    const value = input.value;

    if (!value.trim()) {
        showFieldError(input, errorElement, '크루명은 필수입니다.');
        return false;
    }

    if (value.length > 100) {
        input.value = value.slice(0, 100);
        showFieldError(input, errorElement, '크루명은 100자 이내로 입력해주세요.');
        if (charCount) {
            charCount.parentElement.classList.add('over-limit');
        }
        showToast('크루명은 100자 이내로 입력해주세요.');
        return false;
    }

    clearFieldError(input, errorElement);
    if (charCount) {
        charCount.parentElement.classList.remove('over-limit');
    }
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
    const charCount = document.getElementById('crewDescriptionCount');
    if (!input || !errorElement) return true;

    const value = input.value;

    if (value.length > 500) {
        input.value = value.slice(0, 500);
        showFieldError(input, errorElement, '크루 소개글은 500자 이내로 입력해주세요.');
        if (charCount) {
            charCount.parentElement.classList.add('over-limit');
        }
        showToast('크루 소개글은 500자 이내로 입력해주세요.');
        return false;
    }

    clearFieldError(input, errorElement);
    if (charCount) {
        charCount.parentElement.classList.remove('over-limit');
    }
    return true;
}

function validateActivityRegion() {
    const input = document.getElementById('activityRegion');
    const errorElement = document.getElementById('activityRegionError');
    if (!input || !errorElement) return true;

    const value = input.value;

    if (!value.trim()) {
        showFieldError(input, errorElement, '활동 지역은 필수입니다.');
        return false;
    }

    if (value.length > 100) {
        input.value = value.slice(0, 100);
        showFieldError(input, errorElement, '활동 지역은 100자 이내로 입력해주세요.');
        showToast('활동 지역은 100자 이내로 입력해주세요.');
        return false;
    }

    clearFieldError(input, errorElement);
    return true;
}

function validateRegularMeetingTime() {
    const input = document.getElementById('regularMeetingTime');
    const errorElement = document.getElementById('regularMeetingTimeError');
    if (!input || !errorElement) return true;

    const value = input.value;

    if (!value.trim()) {
        showFieldError(input, errorElement, '정기모임일시는 필수입니다.');
        return false;
    }

    if (value.length > 100) {
        input.value = value.slice(0, 100);
        showFieldError(input, errorElement, '정기모임일시는 100자 이내로 입력해주세요.');
        showToast('정기모임일시은 100자 이내로 입력해주세요.');
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
        inputElement.style.borderColor = 'red';
    }
    if (errorElement) {
        errorElement.textContent = message;
        errorElement.classList.add('visible');
    }
}

function clearFieldError(inputElement, errorElement) {
    if (inputElement) {
        inputElement.classList.remove('error');
        inputElement.style.borderColor = '';
    }
    if (errorElement) {
        errorElement.textContent = '';
        errorElement.classList.remove('visible');
    }
}

// ========================================
// Toast 함수
// ========================================
function showToast(message, duration = 2000) {
    let toast = document.getElementById('toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'toast';
        toast.style.cssText = `
            position: fixed;
            bottom: 100px;
            left: 50%;
            transform: translateX(-50%);
            background-color: #333;
            color: white;
            padding: 12px 24px;
            border-radius: 8px;
            z-index: 10000;
            opacity: 0;
            transition: opacity 0.3s;
        `;
        document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.style.opacity = '1';
    setTimeout(() => {
        toast.style.opacity = '0';
    }, duration);
}
