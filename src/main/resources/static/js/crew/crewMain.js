// ==========================
// DOM Elements
// ==========================
let myCrewSection;
let crewNotificationBtn;
let crewEditBtn;

function initDOMElements() {
    myCrewSection = document.querySelector('.my-crew-section');
    crewNotificationBtn = document.querySelector('[data-action="notifications"]');
    crewEditBtn = document.querySelector('[data-action="edit"]');
}

// ==========================
// State
// ==========================
let crewData = null;

// ==========================
// Get JWT Token
// ==========================
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

// ==========================
// Load My Crew Info
// ==========================
async function loadMyCrewInfo() {
    const token = getAccessToken();

    if (!token) {
        console.log("로그인 필요");
        return;
    }

    try {
        const response = await fetch('/api/crews/main', {
            method: 'GET',
            headers: {
                'Authorization': token,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('크루 정보 조회 실패');
        }

        const result = await response.json();
        crewData = result.data;

        setupEventListeners();

    } catch (error) {
        console.error("크루 메인 정보 조회 실패:", error);
    }
}

// ==========================
// Setup Event Listeners
// ==========================
function setupEventListeners() {
    // 내 크루 카드
    const myCrewCard = document.querySelector('[data-action="myCrewDetail"]');
    if (myCrewCard) {
        myCrewCard.onclick = () => {
            if (!validateCrewMember()) return;
            location.href = `/crews/${crewData.crewId}`;
        };
    }

    // 크루원 카드
    const membersCard = document.querySelector('[data-action="members"]');
    if (membersCard) {
        membersCard.onclick = () => {
            if (!validateCrewMember()) return;
            location.href = `/crews/${crewData.crewId}/users`;
        };
    }

    // 크루 알림 카드
    const notificationCard = document.querySelector('[data-action="notifications"]');
    if (notificationCard) {
        notificationCard.onclick = () => {
            if (!validateCrewManager()) return;
            location.href = `/crews/${crewData.crewId}/join-requests`;
        };
    }

    // 전체 크루 목록
    const listBtn = document.querySelector('[data-action="list"]');
    if (listBtn) {
        listBtn.onclick = () => {
            location.href = '/crews';
        };
    }

    // 크루 생성하기
    const createBtn = document.querySelector('[data-action="create"]');
    if (createBtn) {
        createBtn.onclick = () => {
            if (!validateLogin()) return;
            location.href = '/crews/new';
        };
    }

    // 크루 정보 수정하기
    const editBtn = document.querySelector('[data-action="edit"]');
    if (editBtn) {
        editBtn.onclick = () => {
            if (!validateCrewLeader()) return;
            location.href = `/crews/${crewData.crewId}/edit`;
        };
    }
}

// ==========================
// Validation Functions
// ==========================

/**
 * 로그인 검증
 */
function validateLogin() {
    const token = getAccessToken();

    if (!token) {
        alert('로그인이 필요한 서비스입니다.\n로그인 페이지로 이동합니다.');
        location.href = '/login';
        return false;
    }

    return true;
}

/**
 * 크루원 여부 검증
 */
function validateCrewMember() {
    if (!validateLogin()) return false;

    if (!crewData || !crewData.hasJoinedCrew) {
        alert('크루원만 접근 가능합니다.\n크루에 먼저 가입해주세요.');
        return false;
    }

    return true;
}

/**
 * 크루 관리자(크루장/부크루장) 검증
 */
function validateCrewManager() {
    if (!validateCrewMember()) return false;

    if (!crewData.canManageJoinRequests) {
        alert('크루장 또는 부크루장만 접근 가능합니다.');
        return false;
    }

    return true;
}

/**
 * 크루장 검증
 */
function validateCrewLeader() {
    if (!validateCrewMember()) return false;

    if (!crewData.isLeader) {
        alert('크루장만 접근 가능합니다.');
        return false;
    }

    return true;
}

// ==========================
// Initialize
// ==========================
function init() {
    initDOMElements();
    loadMyCrewInfo();
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
} else {
    init();
}