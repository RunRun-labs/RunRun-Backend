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
        // TODO: 친구 관계 확인 API 연동
        // checkFriendStatus(userId);
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

        if (isFriend) {
            // 친구 삭제
            if (confirm("친구를 삭제하시겠습니까?")) {
                await deleteFriend(userId);
            }
        } else {
            // 친구 신청
            await requestFriend(userId);
        }
    });
}

async function requestFriend(userId) {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            alert("로그인이 필요합니다.");
            return;
        }

        // TODO: 친구 신청 API 연동
        // const res = await fetch(`/friends/request`, {
        //     method: "POST",
        //     headers: {
        //         "Authorization": `Bearer ${token}`,
        //         "Content-Type": "application/json"
        //     },
        //     body: JSON.stringify({ userId })
        // });

        // if (!res.ok) throw new Error("친구 신청 실패");

        // 임시: 성공 메시지 표시
        alert("친구 신청이 완료되었습니다.");
        // TODO: 친구 상태 업데이트
        // updateFriendButton(true);
    } catch (e) {
        console.error("Error requesting friend:", e);
        alert("친구 신청에 실패했습니다.");
    }
}

async function deleteFriend(userId) {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            alert("로그인이 필요합니다.");
            return;
        }

        // TODO: 친구 삭제 API 연동
        // const res = await fetch(`/friends/${userId}`, {
        //     method: "DELETE",
        //     headers: {
        //         "Authorization": `Bearer ${token}`
        //     }
        // });

        // if (!res.ok) throw new Error("친구 삭제 실패");

        // 임시: 성공 메시지 표시
        alert("친구가 삭제되었습니다.");
        // TODO: 친구 상태 업데이트
        // updateFriendButton(false);
    } catch (e) {
        console.error("Error deleting friend:", e);
        alert("친구 삭제에 실패했습니다.");
    }
}

function updateFriendButton(isFriend) {
    const friendBtn = document.getElementById("friendButton");
    if (!friendBtn) return;

    if (isFriend) {
        friendBtn.classList.add("is-friend");
        friendBtn.textContent = "친구삭제";
    } else {
        friendBtn.classList.remove("is-friend");
        friendBtn.textContent = "친구신청";
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

        const res = await fetch(`/users/blocks/${userId}`, {
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

