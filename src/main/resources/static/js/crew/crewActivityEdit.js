/**
 * 크루 활동 수정 페이지
 */

let crewId = null;
let activityId = null;
let crewMembers = [];
let selectedParticipants = [];

// ===========================
// 초기화
// ===========================
document.addEventListener('DOMContentLoaded', async () => {
    console.log('크루 활동 수정 페이지 초기화');

    // ID 가져오기
    const crewIdInput = document.getElementById('crewId');
    const activityIdInput = document.getElementById('activityId');

    if (crewIdInput) crewId = crewIdInput.value;
    if (activityIdInput) activityId = activityIdInput.value;

    console.log('크루 ID:', crewId);
    console.log('활동 ID:', activityId);

    if (!crewId || !activityId) {
        alert('잘못된 접근입니다.');
        history.back();
        return;
    }

    // 크루원 목록 로드
    await loadCrewMembers();

    // 기존 데이터 불러오기
    await loadActivityData();

    // 드롭다운 이벤트
    initDropdown();

    // Validation 초기화
    initValidation();

    // 폼 제출 이벤트
    const activityForm = document.getElementById('activityForm');
    if (activityForm) {
        activityForm.addEventListener('submit', handleSubmit);
    }

    // 삭제 버튼 이벤트
    const btnDelete = document.getElementById('btnDelete');
    if (btnDelete) {
        btnDelete.addEventListener('click', handleDelete);
    }
});

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

    } catch (error) {
        console.error('크루원 목록 로드 실패:', error);
        alert('크루원 목록을 불러오는데 실패했습니다.');
    }
}

// ===========================
// 데이터 로드
// ===========================
async function loadActivityData() {
    try {
        const token = localStorage.getItem('accessToken');
        if (!token) {
            alert('로그인이 필요합니다.');
            window.location.href = '/login';
            return;
        }

        // 크루 상세 조회로 활동 정보 가져오기
        const crewResponse = await fetch(`/api/crews/${crewId}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!crewResponse.ok) {
            throw new Error('크루 정보를 불러올 수 없습니다.');
        }

        const crewResult = await crewResponse.json();
        const crew = crewResult.data || crewResult;

        // 해당 활동 찾기
        const activity = crew.recentActivities?.find(a => a.activityId == activityId);

        if (!activity) {
            throw new Error('활동 정보를 찾을 수 없습니다.');
        }

        // 활동 날짜 표시
        const activityDateInput = document.getElementById('activityDate');
        if (activityDateInput && activity.activityDate) {
            const date = new Date(activity.activityDate);
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            activityDateInput.value = `${year}.${month}.${day}`;
        }

        // 폼에 데이터 채우기
        document.getElementById('region').value = activity.location || '';
        document.getElementById('distance').value = activity.distance || '';

        // 활동 참여자 목록 가져오기
        const participantsResponse = await fetch(`/api/crews/activities/${activityId}/participants`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (participantsResponse.ok) {
            const participantsResult = await participantsResponse.json();
            const participants = participantsResult.data || participantsResult;

            // 참여자 ID 목록 설정
            selectedParticipants = participants.map(p => p.userId);
            console.log('기존 참여자:', selectedParticipants);
        }

        // 참여자 목록 렌더링
        renderParticipantList();
        updateSelectedCount();

    } catch (error) {
        console.error('데이터 로드 실패:', error);
        alert(error.message || '데이터를 불러오는데 실패했습니다.');
        history.back();
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
// 폼 제출 (수정)
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
        submitBtn.textContent = '수정 중...';
    }

    try {
        const token = localStorage.getItem('accessToken');
        if (!token) {
            alert('로그인이 필요합니다.');
            window.location.href = '/login';
            return;
        }

        const response = await fetch(`/api/crews/${crewId}/activities/${activityId}`, {
            method: 'PUT',
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
            throw new Error(result.message || '활동 수정에 실패했습니다.');
        }

        alert('크루 활동이 수정되었습니다!');
        window.location.href = `/crews/${crewId}`;

    } catch (error) {
        console.error('활동 수정 실패:', error);
        alert(error.message || '활동 수정 중 오류가 발생했습니다.');

        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = '수정하기';
        }
    }
}

// ===========================
// 삭제
// ===========================
async function handleDelete() {
    if (!confirm('정말 이 활동을 삭제하시겠습니까?')) {
        return;
    }

    try {
        const token = localStorage.getItem('accessToken');
        if (!token) {
            alert('로그인이 필요합니다.');
            window.location.href = '/login';
            return;
        }

        const response = await fetch(`/api/crews/${crewId}/activities/${activityId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        const result = await response.json();

        if (!response.ok) {
            throw new Error(result.message || '활동 삭제에 실패했습니다.');
        }

        alert('크루 활동이 삭제되었습니다.');
        window.location.href = `/crews/${crewId}`;

    } catch (error) {
        console.error('활동 삭제 실패:', error);
        alert(error.message || '활동 삭제 중 오류가 발생했습니다.');
    }
}