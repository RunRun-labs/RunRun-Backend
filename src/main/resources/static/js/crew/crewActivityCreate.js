/**
 * 크루 활동 생성 페이지
 */

let crewId = null;
let crewMembers = [];
let selectedParticipants = [];

// ===========================
// 초기화
// ===========================
document.addEventListener('DOMContentLoaded', async () => {
    console.log('크루 활동 생성 페이지 초기화');

    // crewId 가져오기
    const crewIdInput = document.getElementById('crewId');
    if (crewIdInput) {
        crewId = crewIdInput.value;
        console.log('크루 ID:', crewId);
    }

    if (!crewId) {
        alert('크루 정보를 찾을 수 없습니다.');
        history.back();
        return;
    }

    // 오늘 날짜 설정
    setTodayDate();

    // 크루원 목록 로드
    await loadCrewMembers();

    // 드롭다운 이벤트
    initDropdown();

    // Validation 초기화
    initValidation();

    // 폼 제출 이벤트
    const activityForm = document.getElementById('activityForm');
    if (activityForm) {
        activityForm.addEventListener('submit', handleSubmit);
    }
});

// ===========================
// 오늘 날짜 설정
// ===========================
function setTodayDate() {
    const activityDateInput = document.getElementById('activityDate');
    if (activityDateInput) {
        const today = new Date();
        const year = today.getFullYear();
        const month = String(today.getMonth() + 1).padStart(2, '0');
        const day = String(today.getDate()).padStart(2, '0');
        activityDateInput.value = `${year}.${month}.${day}`;
    }
}

// ===========================
// Validation 초기화
// ===========================
function initValidation() {
    const regionInput = document.getElementById('region');
    const distanceInput = document.getElementById('distance');
    const dropdownButton = document.getElementById('dropdownButton');

    // 활동 지역 validation
    if (regionInput) {
        regionInput.addEventListener('input', () => validateRegion());
        regionInput.addEventListener('blur', () => validateRegion());
    }

    // 활동 거리 validation
    if (distanceInput) {
        distanceInput.addEventListener('input', () => validateDistance());
        distanceInput.addEventListener('blur', () => validateDistance());
        distanceInput.addEventListener('focus', () => validatePreviousFields('distance'));
    }

    // 참여 크루원 선택 validation
    if (dropdownButton) {
        dropdownButton.addEventListener('click', () => validatePreviousFields('participants'));
    }
}

// ===========================
// 이전 필드 검증
// ===========================
function validatePreviousFields(currentField) {
    const regionInput = document.getElementById('region');

    // 활동 거리나 참여 크루원 선택 시도 시, 활동 지역부터 검증
    if (currentField === 'distance' || currentField === 'participants') {
        if (regionInput && !regionInput.value.trim()) {
            validateRegion();
        }
    }

    // 참여 크루원 선택 시도 시, 활동 거리도 검증
    if (currentField === 'participants') {
        const distanceInput = document.getElementById('distance');
        if (distanceInput && !distanceInput.value) {
            validateDistance();
        }
    }
}

// ===========================
// 활동 지역 검증
// ===========================
function validateRegion() {
    const input = document.getElementById('region');
    const errorElement = document.getElementById('regionError');
    const value = input.value.trim();

    if (!value) {
        showFieldError(input, errorElement, '활동 지역은 필수입니다.');
        return false;
    }

    if (value.length > 100) {
        input.value = value.slice(0, 100);
        showFieldError(input, errorElement, '활동 지역은 100자 이내로 입력해주세요.');
        return false;
    }

    clearFieldError(input, errorElement);
    return true;
}

// ===========================
// 활동 거리 검증
// ===========================
function validateDistance() {
    const input = document.getElementById('distance');
    const errorElement = document.getElementById('distanceError');
    const value = parseInt(input.value);

    if (!input.value || isNaN(value)) {
        showFieldError(input, errorElement, '활동 거리는 필수입니다.');
        return false;
    }

    if (value < 1 || value > 100) {
        showFieldError(input, errorElement, '활동 거리는 1~100km 사이로 입력해주세요.');
        return false;
    }

    clearFieldError(input, errorElement);
    return true;
}

// ===========================
// 참여 크루원 검증
// ===========================
function validateParticipants() {
    const dropdownButton = document.getElementById('dropdownButton');
    const errorElement = document.getElementById('participantsError');

    if (selectedParticipants.length === 0) {
        showFieldError(dropdownButton, errorElement, '참여 크루원은 필수입니다.');
        return false;
    }

    clearFieldError(dropdownButton, errorElement);
    return true;
}

// ===========================
// 에러 표시
// ===========================
function showFieldError(inputElement, errorElement, message) {
    if (inputElement) {
        inputElement.classList.add('error');
        inputElement.style.borderColor = 'red';
    }
    if (errorElement) {
        errorElement.textContent = message;
        errorElement.style.display = 'block';
    }
}

// ===========================
// 에러 제거
// ===========================
function clearFieldError(inputElement, errorElement) {
    if (inputElement) {
        inputElement.classList.remove('error');
        inputElement.style.borderColor = '';
    }
    if (errorElement) {
        errorElement.textContent = '';
        errorElement.style.display = 'none';
    }
}

// ===========================
// 크루원 목록 로드
// ===========================
async function loadCrewMembers() {
    try {
        const token = localStorage.getItem('accessToken');
        if (!token) {
            alert('로그인이 필요합니다.');
            window.location.href = '/login';
            return;
        }

        const response = await fetch(`/api/crews/${crewId}/users`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            throw new Error('크루원 목록을 불러올 수 없습니다.');
        }

        const result = await response.json();
        crewMembers = result.data || result;

        console.log('크루원 목록:', crewMembers);
        renderParticipantList();

    } catch (error) {
        console.error('크루원 목록 로드 실패:', error);
        alert('크루원 목록을 불러오는데 실패했습니다.');
    }
}

// ===========================
// 참여자 목록 렌더링
// ===========================
function renderParticipantList(searchTerm = '') {
    const participantList = document.getElementById('participantList');
    if (!participantList) return;

    participantList.innerHTML = '';

    const filteredMembers = crewMembers.filter(member =>
        member.userName.toLowerCase().includes(searchTerm.toLowerCase())
    );

    filteredMembers.forEach(member => {
        const item = document.createElement('div');
        item.className = 'participant-item';

        const isSelected = selectedParticipants.includes(member.userId);

        item.innerHTML = `
            <input 
                type="checkbox" 
                class="participant-checkbox" 
                id="participant-${member.userId}"
                ${isSelected ? 'checked' : ''}
            />
            <div class="participant-info">
                <img src="${member.profileImageUrl || '/img/default-avatar.png'}" 
                     alt="${member.userName}" 
                     class="participant-avatar" />
                <span class="participant-name">${member.userName}</span>
                <span class="participant-role">${getRoleText(member.role)}</span>
            </div>
        `;

        const checkbox = item.querySelector('.participant-checkbox');
        checkbox.addEventListener('change', () => {
            toggleParticipant(member.userId);
        });

        item.addEventListener('click', (e) => {
            if (e.target !== checkbox) {
                checkbox.checked = !checkbox.checked;
                toggleParticipant(member.userId);
            }
        });

        participantList.appendChild(item);
    });
}

// ===========================
// 참여자 토글
// ===========================
function toggleParticipant(userId) {
    const index = selectedParticipants.indexOf(userId);
    if (index === -1) {
        selectedParticipants.push(userId);
    } else {
        selectedParticipants.splice(index, 1);
    }
    updateSelectedCount();
    // 참여 크루원 validation
    validateParticipants();
}

// ===========================
// 선택된 참여자 수 업데이트
// ===========================
function updateSelectedCount() {
    const selectedCount = document.getElementById('selectedCount');
    if (selectedCount) {
        selectedCount.textContent = `${selectedParticipants.length}명 선택됨`;
    }
}

// ===========================
// 드롭다운 초기화
// ===========================
function initDropdown() {
    const dropdownButton = document.getElementById('dropdownButton');
    const dropdownMenu = document.getElementById('dropdownMenu');
    const searchInput = document.getElementById('searchInput');

    if (dropdownButton && dropdownMenu) {
        dropdownButton.addEventListener('click', () => {
            dropdownButton.classList.toggle('active');
            dropdownMenu.classList.toggle('active');
        });

        // 외부 클릭 시 닫기
        document.addEventListener('click', (e) => {
            if (!dropdownButton.contains(e.target) && !dropdownMenu.contains(e.target)) {
                dropdownButton.classList.remove('active');
                dropdownMenu.classList.remove('active');
            }
        });
    }

    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            renderParticipantList(e.target.value);
        });
    }
}

// ===========================
// 역할 텍스트 변환
// ===========================
function getRoleText(role) {
    const roleMap = {
        'LEADER': '크루장',
        'SUB_LEADER': '부크루장',
        'STAFF': '운영진',
        'MEMBER': '크루원'
    };
    return roleMap[role] || '크루원';
}

// ===========================
// 폼 제출
// ===========================
async function handleSubmit(e) {
    e.preventDefault();

    // 모든 필드 검증
    const isRegionValid = validateRegion();
    const isDistanceValid = validateDistance();
    const isParticipantsValid = validateParticipants();

    if (!isRegionValid || !isDistanceValid || !isParticipantsValid) {
        alert('입력 항목을 확인해주세요.');
        return;
    }

    const region = document.getElementById('region').value.trim();
    const distance = parseInt(document.getElementById('distance').value);

    const submitBtn = document.querySelector('.btn-submit');
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = '등록 중...';
    }

    try {
        const token = localStorage.getItem('accessToken');
        if (!token) {
            alert('로그인이 필요합니다.');
            window.location.href = '/login';
            return;
        }

        const response = await fetch(`/api/crews/${crewId}/activities`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                region: region,
                distance: distance,
                participantUserIds: selectedParticipants
            })
        });

        const result = await response.json();

        if (!response.ok) {
            throw new Error(result.message || '활동 등록에 실패했습니다.');
        }

        alert('크루 활동이 등록되었습니다!');
        window.location.href = `/crews/${crewId}`;

    } catch (error) {
        console.error('활동 등록 실패:', error);
        alert(error.message || '활동 등록 중 오류가 발생했습니다.');

        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = '등록하기';
        }
    }
}