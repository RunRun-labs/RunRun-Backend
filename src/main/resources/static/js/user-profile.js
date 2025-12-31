document.addEventListener("DOMContentLoaded", () => {
    console.log("user-profile.js loaded");

    // Thymeleaf에서 전달된 userId 우선 사용, 없으면 URL에서 추출
    // URL 형식: /profile/{userId}
    let userId = window.userProfileUserId;

    if (!userId) {
        const urlParams = new URLSearchParams(window.location.search);
        // /profile/{userId} 경로에서 userId 추출
        const userIdFromPath = window.location.pathname.split('/').pop();
        userId = userIdFromPath || urlParams.get('userId');
    }

    if (!userId) {
        console.error("User ID not found");
        return;
    }

    // userId를 Number로 변환 (타입 불일치 해결)
    userId = Number(userId);
    if (isNaN(userId)) {
        console.error("Invalid User ID");
        return;
    }

    attachBackButtonHandler();
    attachFriendButtonHandler(userId);
    attachBlockButtonHandler(userId);
    attachBlockModalHandlers(userId);
    loadUserProfile(userId);
});

async function loadUserProfile(userId) {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            console.error("No access token found");
            return;
        }

        const res = await fetch(`/users/${userId}`, {
            headers: {"Authorization": `Bearer ${token}`}
        });

        if (!res.ok) {
            if (res.status === 403) {
                alert("차단된 사용자입니다.");
                window.history.back();
                return;
            }
            throw new Error("프로필 조회 실패");
        }

        const payload = await res.json();
        const user = payload?.data ?? null;

        if (!user) {
            console.error("User data not found");
            return;
        }

        renderProfile(user);
        // 친구 관계 확인
        await checkFriendStatus(userId);
    } catch (e) {
        console.error("Error loading user profile:", e);
        alert("프로필을 불러오는데 실패했습니다.");
    }
}

function renderProfile(user) {
    // 프로필 제목 설정
    const titleEl = document.getElementById("profileTitle");
    if (titleEl) {
        const loginId = user?.loginId; // 백엔드 응답 필드명이 다르면 여기만 맞추면 됨
        titleEl.textContent = `${loginId ? loginId : "사용자"} 님의 프로필`;
    }

    // 프로필 이미지 렌더링
    renderProfileImage(user);

    // 누적/랭킹 정보 (나중에 연동 예정)
    const totalDistanceEl = document.getElementById("totalDistance");
    const rankEl = document.getElementById("rank");

    if (totalDistanceEl) {
        totalDistanceEl.textContent = "-"; // TODO: API 연동 후 실제 데이터 표시
    }
    if (rankEl) {
        rankEl.textContent = "-"; // TODO: API 연동 후 실제 데이터 표시
    }
}

function renderProfileImage(user) {
    const imgEl = document.querySelector('img[data-role="profile-preview"]');
    const initialEl = document.querySelector('span[data-role="profile-initial"]');

    if (!imgEl) return;

    const url = user?.profileImageUrl;

    if (!url) {
        imgEl.removeAttribute("src");
        imgEl.hidden = true;

        if (initialEl) {
            const name = user?.name || "";
            initialEl.textContent = name.charAt(0).toUpperCase() || "";
            initialEl.hidden = false;
        }
        return;
    }

    imgEl.src = url;
    imgEl.alt = "프로필 이미지";
    imgEl.decoding = "async";
    imgEl.loading = "lazy";
    imgEl.hidden = false;

    if (initialEl) {
        initialEl.textContent = "";
        initialEl.hidden = true;
    }

    imgEl.addEventListener("error", () => {
        imgEl.removeAttribute("src");
        imgEl.hidden = true;

        if (initialEl) {
            const name = user?.name || "";
            initialEl.textContent = name.charAt(0).toUpperCase() || "";
            initialEl.hidden = false;
        }
    }, {once: true});
}

function attachBackButtonHandler() {
    const backBtn = document.querySelector('[data-role="back"]');
    if (!backBtn) return;

    backBtn.addEventListener("click", () => {
        window.history.back();
    });
}

function attachFriendButtonHandler(userId) {
    const friendBtn = document.getElementById("friendButton");
    if (!friendBtn) return;

    friendBtn.addEventListener("click", async () => {
        const isFriend = friendBtn.classList.contains("is-friend");
        const isReceived = friendBtn.classList.contains("is-received");
        const isSent = friendBtn.classList.contains("is-sent");

        if (isFriend) {
            // 친구 삭제
            if (confirm("친구를 삭제하시겠습니까?")) {
                await deleteFriend(userId);
            }
        } else if (isReceived) {
            // 받은 친구 요청 수락
            await acceptReceivedFriendRequest(userId);
        } else if (isSent) {
            // 이미 친구 요청을 보낸 상태
            alert("이미 친구 요청을 보냈습니다.");
        } else {
            // 친구 신청
            await requestFriend(userId);
        }
    });
}

/**
 * FriendResDto에서 상대방 사용자 정보를 안전하게 가져오는 헬퍼 함수
 * 필드명이 user 또는 counterpart로 변경되어도 대응 가능
 */
function getCounterpart(friendDto) {
    // user 필드 우선 확인, 없으면 counterpart 필드 확인
    return friendDto?.user || friendDto?.counterpart || null;
}

/**
 * 상대방 userId를 안전하게 가져오는 헬퍼 함수
 * 타입 변환도 함께 처리
 */
function getCounterpartUserId(friendDto) {
    const counterpart = getCounterpart(friendDto);
    if (!counterpart) return null;
    
    // userId 필드 확인 (타입 변환)
    const userId = counterpart.userId || counterpart.id || null;
    return userId != null ? Number(userId) : null;
}

async function requestFriend(userId) {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            alert("로그인이 필요합니다.");
            return;
        }

        // userId가 Number인지 확인
        const targetUserId = Number(userId);
        if (isNaN(targetUserId)) {
            throw new Error("잘못된 사용자 ID입니다.");
        }

        const res = await fetch(`/friends/${targetUserId}/requests`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!res.ok) {
            const errorData = await res.json().catch(() => ({}));
            const errorMessage = errorData.message || "친구 신청 실패";
            
            // 이미 친구 요청이 존재하는 경우
            if (res.status === 409 || errorMessage.includes("이미")) {
                alert("이미 친구 요청이 존재합니다.");
                // 상태 다시 확인
                await checkFriendStatus(targetUserId);
                return;
            }
            
            throw new Error(errorMessage);
        }

        alert("친구 신청이 완료되었습니다.");
        // 친구 상태 업데이트 (요청 보냄 상태)
        await checkFriendStatus(targetUserId);
    } catch (e) {
        console.error("Error requesting friend:", e);
        alert(e.message || "친구 신청에 실패했습니다.");
    }
}

async function deleteFriend(userId) {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            alert("로그인이 필요합니다.");
            return;
        }

        // userId가 Number인지 확인
        const targetUserId = Number(userId);
        if (isNaN(targetUserId)) {
            throw new Error("잘못된 사용자 ID입니다.");
        }

        // 친구 삭제 API 호출
        const res = await fetch(`/friends/${targetUserId}`, {
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
        // 친구 상태 업데이트 (친구 아님)
        await checkFriendStatus(targetUserId);
    } catch (e) {
        console.error("Error deleting friend:", e);
        alert(e.message || "친구 삭제에 실패했습니다.");
    }
}

/**
 * 받은 친구 요청 수락
 */
async function acceptReceivedFriendRequest(targetUserId) {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            alert("로그인이 필요합니다.");
            return;
        }

        const targetId = Number(targetUserId);
        if (isNaN(targetId)) {
            throw new Error("잘못된 사용자 ID입니다.");
        }

        // 받은 친구 요청 목록에서 friendId 찾기
        const receivedRes = await fetch("/friends/requests/received", {
            headers: {"Authorization": `Bearer ${token}`}
        });

        if (!receivedRes.ok) {
            throw new Error("친구 요청 목록 조회 실패");
        }

        const receivedData = await receivedRes.json();
        const receivedRequests = receivedData?.data ?? [];
        
        // 헬퍼 함수를 사용하여 안전하게 찾기
        const receivedRequest = receivedRequests.find(req => {
            const counterpartUserId = getCounterpartUserId(req);
            return counterpartUserId !== null && counterpartUserId === targetId;
        });

        if (!receivedRequest || !receivedRequest.friendId) {
            alert("친구 요청을 찾을 수 없습니다.");
            return;
        }

        // 친구 요청 수락
        const acceptRes = await fetch(`/friends/requests/${receivedRequest.friendId}/accept`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!acceptRes.ok) {
            const errorData = await acceptRes.json().catch(() => ({}));
            throw new Error(errorData.message || "친구 요청 수락 실패");
        }

        alert("친구 요청을 수락했습니다.");
        // 친구 상태 업데이트 (친구됨)
        await checkFriendStatus(targetId);
    } catch (e) {
        console.error("Error accepting friend request:", e);
        alert(e.message || "친구 요청 수락에 실패했습니다.");
    }
}

/**
 * 친구 상태 확인
 * 상태: "none" | "sent" | "received" | "accepted"
 */
async function checkFriendStatus(targetUserId) {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            return;
        }

        // targetUserId를 Number로 변환
        const targetId = Number(targetUserId);
        if (isNaN(targetId)) {
            console.error("Invalid target user ID:", targetUserId);
            updateFriendButtonStatus("none");
            return;
        }

        // 1. 받은 친구 요청 목록 확인
        const receivedRes = await fetch("/friends/requests/received", {
            headers: {"Authorization": `Bearer ${token}`}
        });
        
        if (receivedRes.ok) {
            const receivedData = await receivedRes.json();
            const receivedRequests = receivedData?.data ?? [];
            
            // 헬퍼 함수를 사용하여 안전하게 찾기
            const receivedRequest = receivedRequests.find(req => {
                const counterpartUserId = getCounterpartUserId(req);
                return counterpartUserId !== null && counterpartUserId === targetId;
            });
            
            if (receivedRequest) {
                updateFriendButtonStatus("received", receivedRequest.friendId);
                return;
            }
        }

        // 2. 보낸 친구 요청 목록 확인
        const sentRes = await fetch("/friends/requests/sent", {
            headers: {"Authorization": `Bearer ${token}`}
        });
        
        if (sentRes.ok) {
            const sentData = await sentRes.json();
            const sentRequests = sentData?.data ?? [];
            
            // 헬퍼 함수를 사용하여 안전하게 찾기
            const sentRequest = sentRequests.find(req => {
                const counterpartUserId = getCounterpartUserId(req);
                return counterpartUserId !== null && counterpartUserId === targetId;
            });
            
            if (sentRequest) {
                updateFriendButtonStatus("sent", sentRequest.friendId);
                return;
            }
        }

        // 3. 친구 목록 확인 (여러 페이지 확인)
        let page = 0;
        let hasNext = true;
        
        while (hasNext) {
            const friendsRes = await fetch(`/friends?page=${page}&size=20`, {
                headers: {"Authorization": `Bearer ${token}`}
            });
            
            if (!friendsRes.ok) {
                break;
            }
            
            const friendsData = await friendsRes.json();
            const sliceData = friendsData?.data ?? {};
            const friends = sliceData?.content ?? [];
            
            // 헬퍼 함수를 사용하여 안전하게 찾기
            const friend = friends.find(f => {
                const counterpartUserId = getCounterpartUserId(f);
                return counterpartUserId !== null && 
                       counterpartUserId === targetId && 
                       f.status === "ACCEPTED";
            });
            
            if (friend) {
                updateFriendButtonStatus("accepted", friend.friendId);
                return;
            }
            
            hasNext = sliceData?.hasNext ?? false;
            page++;
            
            // 최대 3페이지까지만 확인 (60명까지)
            if (page >= 3) {
                break;
            }
        }

        // 친구 관계 없음
        updateFriendButtonStatus("none");
    } catch (e) {
        console.error("Error checking friend status:", e);
        // 에러 시 기본값으로 설정
        updateFriendButtonStatus("none");
    }
}

/**
 * 친구 버튼 상태 업데이트
 * @param {string} status - "none" | "sent" | "received" | "accepted"
 * @param {number} friendId - 친구 관계 ID (optional)
 */
function updateFriendButtonStatus(status, friendId = null) {
    const friendBtn = document.getElementById("friendButton");
    if (!friendBtn) return;

    // 기존 클래스 제거
    friendBtn.classList.remove("is-friend", "is-sent", "is-received");

    switch (status) {
        case "accepted":
            friendBtn.classList.add("is-friend");
            friendBtn.textContent = "친구삭제";
            friendBtn.dataset.friendId = friendId || "";
            break;
        case "sent":
            friendBtn.classList.add("is-sent");
            friendBtn.textContent = "요청 보냄";
            friendBtn.dataset.friendId = friendId || "";
            break;
        case "received":
            friendBtn.classList.add("is-received");
            friendBtn.textContent = "요청 수락";
            friendBtn.dataset.friendId = friendId || "";
            break;
        case "none":
        default:
            friendBtn.textContent = "친구신청";
            friendBtn.dataset.friendId = "";
            break;
    }
}

function attachBlockButtonHandler(userId) {
    const blockBtn = document.getElementById("blockButton");
    if (!blockBtn) return;

    blockBtn.addEventListener("click", () => {
        openBlockModal();
    });
}

function openBlockModal() {
    const modal = document.getElementById("blockModal");
    if (!modal) return;
    modal.removeAttribute("hidden");
}

function closeBlockModal() {
    const modal = document.getElementById("blockModal");
    if (!modal) return;
    modal.setAttribute("hidden", "hidden");
}

function attachBlockModalHandlers(userId) {
    const modal = document.getElementById("blockModal");
    if (!modal) return;

    // 모달 닫기 버튼
    const closeBtn = modal.querySelector('[data-role="close-block-modal"]');
    if (closeBtn) {
        closeBtn.addEventListener("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            closeBlockModal();
        });
    }

    // 모달 배경 클릭 시 닫기
    modal.addEventListener("click", (e) => {
        if (e.target === modal) {
            closeBlockModal();
        }
    });

    // 모달 내용 클릭 시 이벤트 전파 방지
    const modalContent = modal.querySelector(".modal-content");
    if (modalContent) {
        modalContent.addEventListener("click", (e) => {
            e.stopPropagation();
        });
    }

    // 차단 버튼
    const blockOnlyBtn = modal.querySelector('[data-role="block-only"]');
    if (blockOnlyBtn) {
        blockOnlyBtn.addEventListener("click", async () => {
            await blockUser(userId, false);
        });
    }

    // 차단 및 신고 버튼
    const blockAndReportBtn = modal.querySelector('[data-role="block-and-report"]');
    if (blockAndReportBtn) {
        blockAndReportBtn.addEventListener("click", async () => {
            await blockUser(userId, true);
        });
    }
}

async function blockUser(userId, shouldReport) {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            alert("로그인이 필요합니다.");
            return;
        }

        // userId가 Number인지 확인
        const targetUserId = Number(userId);
        if (isNaN(targetUserId)) {
            throw new Error("잘못된 사용자 ID입니다.");
        }

        const res = await fetch(`/users/blocks/${targetUserId}`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!res.ok) {
            const errorData = await res.json().catch(() => ({}));
            throw new Error(errorData.message || "차단 실패");
        }

        closeBlockModal();

        if (shouldReport) {
            // TODO: 신고 API 연동 (나중에 구현 예정)
            alert("사용자가 차단되었습니다. 신고 기능은 곧 구현될 예정입니다.");
        } else {
            alert("사용자가 차단되었습니다.");
        }

        // 차단 후 이전 페이지로 이동
        window.history.back();
    } catch (e) {
        console.error("Error blocking user:", e);
        alert(e.message || "사용자 차단에 실패했습니다.");
    }
}

