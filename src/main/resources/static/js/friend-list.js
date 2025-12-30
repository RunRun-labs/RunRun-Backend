document.addEventListener("DOMContentLoaded", () => {
    console.log("friend-list.js loaded");
    
    attachBackButtonHandler();
    loadFriendRequests();
    loadFriends();
});

// 기본 프로필 이미지 SVG
const DEFAULT_PROFILE_SVG = `<svg xmlns="http://www.w3.org/2000/svg" width="60" height="60" viewBox="0 0 60 60" fill="none">
  <g clip-path="url(#clip0_129_2707)">
    <path d="M41.25 22.5C41.25 25.4837 40.0647 28.3452 37.955 30.455C35.8452 32.5647 32.9837 33.75 30 33.75C27.0163 33.75 24.1548 32.5647 22.045 30.455C19.9353 28.3452 18.75 25.4837 18.75 22.5C18.75 19.5163 19.9353 16.6548 22.045 14.545C24.1548 12.4353 27.0163 11.25 30 11.25C32.9837 11.25 35.8452 12.4353 37.955 14.545C40.0647 16.6548 41.25 19.5163 41.25 22.5Z" fill="black" fill-opacity="0.5"/>
    <path fill-rule="evenodd" clip-rule="evenodd" d="M0 30C0 22.0435 3.16071 14.4129 8.7868 8.7868C14.4129 3.16071 22.0435 0 30 0C37.9565 0 45.5871 3.16071 51.2132 8.7868C56.8393 14.4129 60 22.0435 60 30C60 37.9565 56.8393 45.5871 51.2132 51.2132C45.5871 56.8393 37.9565 60 30 60C22.0435 60 14.4129 56.8393 8.7868 51.2132C3.16071 45.5871 0 37.9565 0 30ZM30 3.75C25.0567 3.75026 20.2139 5.14636 16.029 7.7776C11.8441 10.4088 8.48731 14.1683 6.34487 18.6232C4.20242 23.0782 3.36145 28.0475 3.91874 32.9593C4.47604 37.8712 6.40895 42.5258 9.495 46.3875C12.1575 42.0975 18.0188 37.5 30 37.5C41.9813 37.5 47.8388 42.0938 50.505 46.3875C53.5911 42.5258 55.524 37.8712 56.0813 32.9593C56.6386 28.0475 55.7976 23.0782 53.6551 18.6232C51.5127 14.1683 48.1559 10.4088 43.971 7.7776C39.7861 5.14636 34.9433 3.75026 30 3.75Z" fill="black" fill-opacity="0.5"/>
  </g>
  <defs>
    <clipPath id="clip0_129_2707">
      <rect width="60" height="60" fill="white"/>
    </clipPath>
  </defs>
</svg>`;

function attachBackButtonHandler() {
    const backBtn = document.querySelector('[data-role="back"]');
    if (!backBtn) return;

    backBtn.addEventListener("click", () => {
        window.location.href = "/myPage";
    });
}

async function loadFriendRequests() {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            console.error("No access token found");
            return;
        }

        const res = await fetch("/friends/requests/received", {
            headers: {"Authorization": `Bearer ${token}`}
        });

        if (!res.ok) {
            throw new Error("친구 요청 목록 조회 실패");
        }

        const payload = await res.json();
        const requests = payload?.data ?? [];

        renderFriendRequestNotification(requests.length);
        renderFriendRequests(requests);
    } catch (e) {
        console.error("Error loading friend requests:", e);
    }
}

async function loadFriends() {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            console.error("No access token found");
            return;
        }

        const res = await fetch("/friends", {
            headers: {"Authorization": `Bearer ${token}`}
        });

        if (!res.ok) {
            throw new Error("친구 목록 조회 실패");
        }

        const payload = await res.json();
        const friends = payload?.data ?? [];

        renderFriends(friends);
        
        // 친구 요청 개수도 함께 확인
        const requestRes = await fetch("/friends/requests/received", {
            headers: {"Authorization": `Bearer ${token}`}
        });
        const requestPayload = requestRes.ok ? await requestRes.json() : {data: []};
        const requestCount = requestPayload?.data?.length ?? 0;
        
        updateEmptyState(requestCount, friends.length);
    } catch (e) {
        console.error("Error loading friends:", e);
    }
}

function renderFriendRequestNotification(count) {
    const notificationEl = document.getElementById("friendRequestNotification");
    const countEl = document.getElementById("requestCount");
    
    if (!notificationEl || !countEl) return;

    if (count === 0) {
        notificationEl.setAttribute("hidden", "hidden");
    } else {
        notificationEl.removeAttribute("hidden");
        countEl.textContent = count;
    }
}

function renderFriendRequests(requests) {
    const container = document.getElementById("friendRequestsList");
    if (!container) return;

    container.innerHTML = "";

    if (requests.length === 0) {
        return;
    }

    // 최근 요청이 위로 오도록 정렬 (생성일 기준 내림차순)
    const sortedRequests = [...requests].sort((a, b) => {
        // friendId로 정렬 (최근 것이 큰 ID를 가질 가능성이 높음)
        return (b.friendId || 0) - (a.friendId || 0);
    });

    sortedRequests.forEach(request => {
        const item = createFriendRequestItem(request);
        container.appendChild(item);
    });
}

function renderFriends(friends) {
    const container = document.getElementById("friendsList");
    if (!container) return;

    container.innerHTML = "";

    if (friends.length === 0) {
        return;
    }

    friends.forEach(friend => {
        const item = createFriendItem(friend);
        container.appendChild(item);
    });
}

function createFriendRequestItem(request) {
    const user = request.user;
    const item = document.createElement("div");
    item.className = "friend-item";
    item.dataset.friendId = request.friendId;
    item.dataset.userId = user.userId;

    item.innerHTML = `
        ${renderProfileImage(user.profileImageUrl)}
        <div class="friend-info">
            <div class="friend-id">${escapeHtml(user.loginId)}</div>
            <div class="friend-stats">
                <div class="friend-stat">누적: xx km</div>
                <div class="friend-stat">랭킹: xx위</div>
            </div>
        </div>
        <div class="friend-actions">
            <button class="friend-action-btn accept-btn" data-role="accept-request" data-request-id="${request.friendId}">
                수락
            </button>
            <button class="friend-action-btn reject-btn" data-role="reject-request" data-request-id="${request.friendId}">
                거절
            </button>
        </div>
    `;

    // 프로필 이미지 에러 처리
    const img = item.querySelector('.friend-profile-image');
    const defaultImg = item.querySelector('.friend-profile-default');
    if (img && defaultImg) {
        img.addEventListener("error", () => {
            img.hidden = true;
            defaultImg.hidden = false;
        }, {once: true});
    }

    // 수락 버튼
    const acceptBtn = item.querySelector('[data-role="accept-request"]');
    if (acceptBtn) {
        acceptBtn.addEventListener("click", () => {
            acceptFriendRequest(request.friendId);
        });
    }

    // 거절 버튼
    const rejectBtn = item.querySelector('[data-role="reject-request"]');
    if (rejectBtn) {
        rejectBtn.addEventListener("click", () => {
            rejectFriendRequest(request.friendId);
        });
    }

    return item;
}

function createFriendItem(friend) {
    const user = friend.user;
    const item = document.createElement("div");
    item.className = "friend-item";
    item.dataset.friendId = friend.friendId;
    item.dataset.userId = user.userId;

    item.innerHTML = `
        ${renderProfileImage(user.profileImageUrl)}
        <div class="friend-info">
            <div class="friend-id">${escapeHtml(user.loginId)}</div>
            <div class="friend-stats">
                <div class="friend-stat">누적: xx km</div>
                <div class="friend-stat">랭킹: xx위</div>
            </div>
        </div>
        <div class="friend-actions">
            <button class="friend-action-btn delete-btn" data-role="delete-friend" data-friend-id="${user.userId}">
                친구삭제
            </button>
        </div>
    `;

    // 프로필 이미지 에러 처리
    const img = item.querySelector('.friend-profile-image');
    const defaultImg = item.querySelector('.friend-profile-default');
    if (img && defaultImg) {
        img.addEventListener("error", () => {
            img.hidden = true;
            defaultImg.hidden = false;
        }, {once: true});
    }

    // 친구 삭제 버튼
    const deleteBtn = item.querySelector('[data-role="delete-friend"]');
    if (deleteBtn) {
        deleteBtn.addEventListener("click", () => {
            if (confirm("친구를 삭제하시겠습니까?")) {
                deleteFriend(user.userId);
            }
        });
    }

    return item;
}

function renderProfileImage(profileImageUrl) {
    if (profileImageUrl) {
        return `
            <div class="friend-profile-wrapper">
                <img 
                    class="friend-profile-image" 
                    src="${escapeHtml(profileImageUrl)}" 
                    alt="프로필 이미지"
                />
                <div class="friend-profile-default" style="display: none;">
                    ${DEFAULT_PROFILE_SVG}
                </div>
            </div>
        `;
    } else {
        return `
            <div class="friend-profile-default">
                ${DEFAULT_PROFILE_SVG}
            </div>
        `;
    }
}

async function acceptFriendRequest(requestId) {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            alert("로그인이 필요합니다.");
            return;
        }

        const res = await fetch(`/friends/requests/${requestId}/accept`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!res.ok) {
            const errorData = await res.json().catch(() => ({}));
            throw new Error(errorData.message || "친구 요청 수락 실패");
        }

        alert("친구 요청을 수락했습니다.");
        // 목록 새로고침
        loadFriendRequests();
        loadFriends();
    } catch (e) {
        console.error("Error accepting friend request:", e);
        alert(e.message || "친구 요청 수락에 실패했습니다.");
    }
}

async function rejectFriendRequest(requestId) {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            alert("로그인이 필요합니다.");
            return;
        }

        if (!confirm("친구 요청을 거절하시겠습니까?")) {
            return;
        }

        const res = await fetch(`/friends/requests/${requestId}/reject`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!res.ok) {
            const errorData = await res.json().catch(() => ({}));
            throw new Error(errorData.message || "친구 요청 거절 실패");
        }

        alert("친구 요청을 거절했습니다.");
        // 목록 새로고침
        loadFriendRequests();
    } catch (e) {
        console.error("Error rejecting friend request:", e);
        alert(e.message || "친구 요청 거절에 실패했습니다.");
    }
}

async function deleteFriend(friendUserId) {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            alert("로그인이 필요합니다.");
            return;
        }

        const res = await fetch(`/friends/${friendUserId}`, {
            method: "DELETE",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!res.ok) {
            const errorData = await res.json().catch(() => ({}));
            throw new Error(errorData.message || "친구 삭제 실패");
        }

        alert("친구가 삭제되었습니다.");
        // 목록 새로고침
        loadFriends();
    } catch (e) {
        console.error("Error deleting friend:", e);
        alert(e.message || "친구 삭제에 실패했습니다.");
    }
}

function updateEmptyState(requestCount, friendCount) {
    const emptyState = document.getElementById("emptyState");
    if (!emptyState) return;

    if (requestCount === 0 && friendCount === 0) {
        emptyState.removeAttribute("hidden");
    } else {
        emptyState.setAttribute("hidden", "hidden");
    }
}

function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
}

