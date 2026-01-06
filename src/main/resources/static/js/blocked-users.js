document.addEventListener("DOMContentLoaded", () => {
    console.log("blocked-users.js loaded");
    attachBackButtonHandler();
    loadBlockedUsers();
});

// 뒤로가기 버튼
function attachBackButtonHandler() {
    const backButton = document.querySelector(".back-button");
    if (backButton) {
        backButton.addEventListener("click", () => {
            window.history.back();
        });
    }
}

// 차단한 사용자 목록 로드
async function loadBlockedUsers() {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            console.warn("No access token found");
            window.location.href = "/login";
            return;
        }

        const response = await fetch("/users/blocks", {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!response.ok) {
            if (response.status === 401) {
                window.location.href = "/login";
                return;
            }
            throw new Error("차단 목록 조회 실패");
        }

        const result = await response.json();
        const blockedUsers = result?.data || [];

        renderBlockedUsers(blockedUsers);
    } catch (error) {
        console.error("Failed to load blocked users:", error);
        alert(error.message || "차단 목록을 불러오는 중 오류가 발생했습니다.");
    }
}

// 차단한 사용자 목록 렌더링
function renderBlockedUsers(blockedUsers) {
    const listContainer = document.querySelector('[data-role="blocked-users-list"]');
    const emptyState = document.querySelector('[data-role="empty-state"]');

    if (!listContainer) return;

    // 기존 목록 제거 (로딩 상태 등)
    const existingItems = listContainer.querySelectorAll('.blocked-user-item');
    existingItems.forEach(item => item.remove());

    if (blockedUsers.length === 0) {
        if (emptyState) {
            emptyState.removeAttribute("hidden");
        }
        return;
    }

    if (emptyState) {
        emptyState.setAttribute("hidden", "hidden");
    }

    blockedUsers.forEach(user => {
        const userItem = createBlockedUserItem(user);
        listContainer.appendChild(userItem);
    });
}

// 차단한 사용자 항목 생성
function createBlockedUserItem(user) {
    const item = document.createElement("div");
    item.className = "blocked-user-item";

    // 프로필 이미지
    const profileImage = document.createElement("div");
    profileImage.className = "user-profile-image";
    
    if (user.profileImageUrl) {
        const img = document.createElement("img");
        img.src = user.profileImageUrl;
        img.alt = `${user.loginId} 프로필`;
        img.onerror = function() {
            // 이미지 로드 실패 시 기본 SVG 표시
            this.remove();
            profileImage.innerHTML = getDefaultProfileSVG();
        };
        profileImage.appendChild(img);
    } else {
        profileImage.innerHTML = getDefaultProfileSVG();
    }

    // 사용자 정보
    const userInfo = document.createElement("div");
    userInfo.className = "user-info";
    
    const loginId = document.createElement("span");
    loginId.className = "user-login-id";
    loginId.textContent = user.loginId || user.name || "알 수 없음";
    
    userInfo.appendChild(loginId);

    // 차단 해제 버튼
    const unblockButton = document.createElement("button");
    unblockButton.className = "unblock-button";
    unblockButton.textContent = "차단 해제";
    unblockButton.type = "button";
    unblockButton.addEventListener("click", () => handleUnblock(user.blockedUserId, item));

    item.appendChild(profileImage);
    item.appendChild(userInfo);
    item.appendChild(unblockButton);

    return item;
}

// 기본 프로필 SVG
function getDefaultProfileSVG() {
    return `
        <svg width="40" height="40" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="20" cy="20" r="20" fill="#e1e4e8"/>
            <circle cx="20" cy="15" r="6" fill="#6c7278"/>
            <path d="M8 32C8 26 13 22 20 22C27 22 32 26 32 32" stroke="#6c7278" stroke-width="2" stroke-linecap="round"/>
        </svg>
    `;
}

// 차단 해제 처리
async function handleUnblock(blockedUserId, itemElement) {
    if (!confirm("차단을 해제하시겠습니까?")) {
        return;
    }

    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            alert("로그인이 필요합니다.");
            window.location.href = "/login";
            return;
        }

        const unblockButton = itemElement.querySelector(".unblock-button");
        if (unblockButton) {
            unblockButton.disabled = true;
            unblockButton.textContent = "처리 중...";
        }

        const response = await fetch(`/users/blocks/${blockedUserId}`, {
            method: "DELETE",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!response.ok) {
            if (response.status === 401) {
                window.location.href = "/login";
                return;
            }
            const error = await response.json();
            throw new Error(error?.message || "차단 해제 실패");
        }

        // 목록에서 제거
        itemElement.remove();

        // 목록이 비어있으면 empty state 표시
        const listContainer = document.querySelector('[data-role="blocked-users-list"]');
        const remainingItems = listContainer?.querySelectorAll('.blocked-user-item');
        const emptyState = document.querySelector('[data-role="empty-state"]');
        
        if (remainingItems && remainingItems.length === 0 && emptyState) {
            emptyState.removeAttribute("hidden");
        }

        alert("차단이 해제되었습니다.");
    } catch (error) {
        console.error("Failed to unblock user:", error);
        alert(error.message || "차단 해제 중 오류가 발생했습니다.");
        
        const unblockButton = itemElement.querySelector(".unblock-button");
        if (unblockButton) {
            unblockButton.disabled = false;
            unblockButton.textContent = "차단 해제";
        }
    }
}

