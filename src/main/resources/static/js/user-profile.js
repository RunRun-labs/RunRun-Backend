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

    // 자신의 프로필 페이지에 접근한 경우 마이페이지로 리디렉션
    const currentUserId = localStorage.getItem("userId");
    if (currentUserId && Number(currentUserId) === userId) {
        window.location.href = "/myPage";
        return;
    }

    attachBackButtonHandler();
    attachFriendButtonHandler(userId);
    attachBlockButtonHandler(userId);
    attachFriendDeleteModalHandlers(userId);
    attachBlockModalHandlers(userId);
    loadUserProfile(userId);

    // 초기 로드 시 빈 상태 숨김
    hideEmptyState();

    // 주간 요약 및 러닝 기록 로드
    initWeekSelector(userId);
    loadWeeklyStats(userId);

    // 러닝 기록 무한 스크롤 초기화
    initInfiniteScroll(userId);
    attachUserScrollGate(userId);
    loadRunningRecords(userId, 0, true);
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

    // 최근활동/총 러닝 횟수 정보 (나중에 연동 예정)
    const lastActivityEl = document.getElementById("lastActivity");
    const totalRunsEl = document.getElementById("totalRuns");

    if (lastActivityEl) {
        lastActivityEl.textContent = "오늘"; // TODO: API 연동 후 실제 데이터 표시
    }
    if (totalRunsEl) {
        totalRunsEl.textContent = "42"; // TODO: API 연동 후 실제 데이터 표시
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
            // 친구인 경우 모달 표시
            openFriendDeleteModal();
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

async function deleteFriend(userId, showAlert = true) {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            if (showAlert) {
                alert("로그인이 필요합니다.");
            }
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

        if (showAlert) {
            alert("친구가 삭제되었습니다.");
        }
        // 친구 상태 업데이트 (친구 아님)
        await checkFriendStatus(targetUserId);
    } catch (e) {
        console.error("Error deleting friend:", e);
        if (showAlert) {
            alert(e.message || "친구 삭제에 실패했습니다.");
        }
        throw e; // 에러를 다시 throw하여 상위 함수에서 처리 가능하도록
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
    const blockBtn = document.getElementById("blockButton");
    if (!friendBtn) return;

    // 기존 클래스 제거
    friendBtn.classList.remove("is-friend", "is-sent", "is-received");

    switch (status) {
        case "accepted":
            friendBtn.classList.add("is-friend");
            friendBtn.textContent = "친구삭제";
            friendBtn.dataset.friendId = friendId || "";
            // 친구인 경우 차단 버튼 숨김
            if (blockBtn) {
                blockBtn.setAttribute("hidden", "hidden");
            }
            break;
        case "sent":
            friendBtn.classList.add("is-sent");
            friendBtn.textContent = "요청 보냄";
            friendBtn.dataset.friendId = friendId || "";
            // 친구 요청 보낸 경우 차단 버튼 표시
            if (blockBtn) {
                blockBtn.removeAttribute("hidden");
            }
            break;
        case "received":
            friendBtn.classList.add("is-received");
            friendBtn.textContent = "요청 수락";
            friendBtn.dataset.friendId = friendId || "";
            // 친구 요청 받은 경우 차단 버튼 표시
            if (blockBtn) {
                blockBtn.removeAttribute("hidden");
            }
            break;
        case "none":
        default:
            friendBtn.textContent = "친구신청";
            friendBtn.dataset.friendId = "";
            // 친구가 아닌 경우 차단 버튼 표시
            if (blockBtn) {
                blockBtn.removeAttribute("hidden");
            }
            break;
    }
}

function openFriendDeleteModal() {
    const modal = document.getElementById("friendDeleteModal");
    if (!modal) return;
    modal.removeAttribute("hidden");
}

function closeFriendDeleteModal() {
    const modal = document.getElementById("friendDeleteModal");
    if (!modal) return;
    modal.setAttribute("hidden", "hidden");
}

function attachFriendDeleteModalHandlers(userId) {
    const modal = document.getElementById("friendDeleteModal");
    if (!modal) return;

    // 모달 닫기 버튼
    const closeBtn = modal.querySelector('[data-role="close-friend-delete-modal"]');
    if (closeBtn) {
        closeBtn.addEventListener("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            closeFriendDeleteModal();
        });
    }

    // 모달 배경 클릭 시 닫기
    modal.addEventListener("click", (e) => {
        if (e.target === modal) {
            closeFriendDeleteModal();
        }
    });

    // 모달 내용 클릭 시 이벤트 전파 방지
    const modalContent = modal.querySelector(".modal-content");
    if (modalContent) {
        modalContent.addEventListener("click", (e) => {
            e.stopPropagation();
        });
    }

    // 삭제만 버튼
    const deleteOnlyBtn = modal.querySelector('[data-role="delete-only"]');
    if (deleteOnlyBtn) {
        deleteOnlyBtn.addEventListener("click", async () => {
            await deleteFriend(userId);
            closeFriendDeleteModal();
        });
    }

    // 삭제 및 차단 버튼
    const deleteAndBlockBtn = modal.querySelector('[data-role="delete-and-block"]');
    if (deleteAndBlockBtn) {
        deleteAndBlockBtn.addEventListener("click", async () => {
            await deleteFriendAndBlock(userId);
            closeFriendDeleteModal();
        });
    }
}

/**
 * 친구 삭제 및 차단
 */
async function deleteFriendAndBlock(userId) {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            alert("로그인이 필요합니다.");
            return;
        }

        const targetUserId = Number(userId);
        if (isNaN(targetUserId)) {
            throw new Error("잘못된 사용자 ID입니다.");
        }

        // 1. 친구 삭제 (알림 없이)
        await deleteFriend(targetUserId, false);

        // 2. 차단
        const blockRes = await fetch(`/users/blocks/${targetUserId}`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!blockRes.ok) {
            const errorData = await blockRes.json().catch(() => ({}));
            throw new Error(errorData.message || "차단 실패");
        }

        alert("친구가 삭제되고 차단되었습니다.");
        
        // 차단 후 이전 페이지로 이동
        window.history.back();
    } catch (e) {
        console.error("Error deleting friend and blocking user:", e);
        alert(e.message || "친구 삭제 및 차단에 실패했습니다.");
    }
}

/**
 * 차단 버튼 핸들러
 */
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
    const blockBtn = modal.querySelector('[data-role="block-only"]');
    if (blockBtn) {
        blockBtn.addEventListener("click", async () => {
            await blockUser(userId, false);
        });
    }
}

/**
 * 사용자 차단
 */
async function blockUser(userId, shouldReport) {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            alert("로그인이 필요합니다.");
            return;
        }

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
        alert("사용자가 차단되었습니다.");

        // 차단 후 이전 페이지로 이동
        window.history.back();
    } catch (e) {
        console.error("Error blocking user:", e);
        alert(e.message || "사용자 차단에 실패했습니다.");
    }
}

// 주 선택 관련 전역 변수
let currentWeekOffset = 0; // 0 = 이번 주, -1 = 지난 주, 1 = 다음 주 등

/**
 * 주 선택 기능 초기화
 */
function initWeekSelector(userId) {
    const prevBtn = document.querySelector('[data-role="week-prev"]');
    const nextBtn = document.querySelector('[data-role="week-next"]');

    if (prevBtn) {
        prevBtn.addEventListener("click", () => {
            if (currentWeekOffset > -3) { // 최대 한달 전까지 (4주)
                currentWeekOffset--;
                updateWeekLabel();
                loadWeeklyStats(userId);
            }
        });
    }

    if (nextBtn) {
        nextBtn.addEventListener("click", () => {
            if (currentWeekOffset < 0) { // 현재 주까지만 (미래 주는 불가)
                currentWeekOffset++;
                updateWeekLabel();
                loadWeeklyStats(userId);
            }
        });
    }

    updateWeekLabel();
}

/**
 * 주 레이블 업데이트
 */
function updateWeekLabel() {
    const weekLabel = document.querySelector('[data-role="week-label"]');
    if (!weekLabel) return;

    const today = new Date();
    const targetDate = new Date(today);
    targetDate.setDate(today.getDate() + (currentWeekOffset * 7));

    const weekStart = getStartOfWeek(targetDate);
    const month = weekStart.getMonth() + 1;
    const weekNumber = getWeekNumber(weekStart);

    let label = `${month}월 ${getWeekLabel(weekNumber)}째 주`;

    if (currentWeekOffset === 0) {
        label = `이번 주`;
    } else if (currentWeekOffset === -1) {
        label = `지난 주`;
    } else {
        label = `${month}월 ${getWeekLabel(weekNumber)}째 주`;
    }

    weekLabel.textContent = label;
}

/**
 * 주의 몇째 주인지 계산
 */
function getWeekNumber(date) {
    const firstDay = new Date(date.getFullYear(), date.getMonth(), 1);
    const firstDayOfWeek = firstDay.getDay() === 0 ? 7 : firstDay.getDay();
    const dayOfMonth = date.getDate();
    const weekNumber = Math.ceil((dayOfMonth + firstDayOfWeek - 1) / 7);
    return weekNumber;
}

/**
 * 주 레이블 한글 변환
 */
function getWeekLabel(weekNumber) {
    const labels = ["첫", "둘", "셋", "넷", "다섯"];
    return labels[weekNumber - 1] || weekNumber.toString();
}

function getStartOfWeek(date) {
    const d = new Date(date);
    const diff = d.getDate() - d.getDay() + (d.getDay() === 0 ? -6 : 1);
    return new Date(d.setDate(diff));
}

/**
 * 주간 러닝 요약 로드
 */
async function loadWeeklyStats(userId) {
    const token = localStorage.getItem("accessToken");
    if (!token) return;

    try {
        const res = await fetch(`/api/summary/weekly/${userId}?weekOffset=${currentWeekOffset}`, {
            headers: {Authorization: `Bearer ${token}`},
        });

        if (!res.ok) {
            // 403 에러인 경우 공개 범위 확인
            if (res.status === 403) {
                try {
                    const errorData = await res.json();
                    const errorCode = errorData?.code || errorData?.errorCode;

                    // 비공개 또는 친구만 공개인 경우 주간 통계는 0으로 표시
                    if (errorCode === "PR002" || errorCode === "PR001") {
                        renderWeeklyChart([]);
                        updateWeeklyTotals(0, 0);
                        return;
                    }
                } catch (parseError) {
                    console.error("에러 응답 파싱 실패:", parseError);
                    // 파싱 실패해도 403이면 0으로 표시
                    renderWeeklyChart([]);
                    updateWeeklyTotals(0, 0);
                    return;
                }
            }
            throw new Error();
        }

        const payload = await res.json();
        const data = payload.data;

        renderWeeklyChart(data.dailyDistances);
        updateWeeklyTotals(
            data.totalDistanceKm,
            data.totalDurationSec
        );

    } catch (e) {
        console.error("주간 러닝 통계 실패", e);
        renderWeeklyChart([]);
        updateWeeklyTotals(0, 0);
    }
}

/**
 * 주별 총 거리와 시간 업데이트
 */
function updateWeeklyTotals(distance, durationSeconds) {
    const distanceEl = document.querySelector('[data-role="weekly-total-distance"]');
    const durationEl = document.querySelector('[data-role="weekly-total-duration"]');

    if (distanceEl) {
        const distanceKm = distance ? parseFloat(distance) : 0;
        distanceEl.textContent = `${distanceKm.toFixed(1)}km`;
    }

    if (durationEl) {
        const hours = Math.floor(durationSeconds / 3600);
        const minutes = Math.floor((durationSeconds % 3600) / 60);
        durationEl.textContent = `${hours}h ${minutes}m`;
    }
}

function renderWeeklyChart(distances) {
    const chartBars = document.querySelector('[data-role="chart-bars"]');
    if (!chartBars) return;
    chartBars.innerHTML = "";
    if (!Array.isArray(distances) || distances.length === 0) {
        for (let i = 0; i < 7; i++) {
            const circle = document.createElement("div");
            circle.className = "chart-circle";
            chartBars.appendChild(circle);
        }
        return;
    }

    const maxDistance = Math.max(...distances, 0.1);
    distances.forEach((distance, index) => {
        const distValue = distance ? parseFloat(distance) : 0;
        if (distValue === 0 || distValue < 0.01) {
            // 거리가 0일 때는 동그란 원 생성
            const circle = document.createElement("div");
            circle.className = "chart-circle";
            circle.setAttribute("data-day-index", index);
            chartBars.appendChild(circle);
        } else {
            // 거리가 있을 때는 막대 그래프 생성
            const bar = document.createElement("div");
            bar.className = "chart-bar";
            const heightRatio = distValue / maxDistance;
            // 최소 높이: 약 24.631px, 최대 높이: 약 98.539px (Figma 디자인 기준)
            const minHeight = 24.631;
            const maxHeight = 98.539;
            const height = Math.max(minHeight, minHeight + (maxHeight - minHeight) * heightRatio);
            bar.style.height = `${height}px`;
            chartBars.appendChild(bar);
        }
    });
}

// 러닝 기록 무한 스크롤 관련 전역 변수
let currentPage = 0;
let hasNext = true;
let isLoading = false;
let userHasInteracted = false;
let scrollObserver = null;

/**
 * 날짜를 API 형식으로 포맷팅 (YYYY-MM-DD)
 */
function formatDateForAPI(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

/**
 * 러닝 기록 로드 (API 연동)
 */
async function loadRunningRecords(userId, page = 0, reset = false) {
    if (isLoading || (!hasNext && !reset)) return;

    isLoading = true;
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            isLoading = false;
            return;
        }

        // 날짜 필터 계산: 기본적으로 최근 7일만 조회 (초기 로드일 때만)
        let url = `/api/records/${userId}?page=${page}&size=4&sort=startedAt,desc`;
        
        if (reset && page === 0) {
            // 초기 로드일 때만 최근 7일만 조회
            const today = new Date();
            const sevenDaysAgo = new Date(today);
            sevenDaysAgo.setDate(today.getDate() - 6); // 7일 전 (오늘 포함)
            
            const startDateStr = formatDateForAPI(sevenDaysAgo);
            const endDateStr = formatDateForAPI(today);
            url += `&startDate=${startDateStr}&endDate=${endDateStr}`;
        }

        const res = await fetch(url, {
            headers: {Authorization: `Bearer ${token}`}
        });

        if (!res.ok) {
            // 403 에러인 경우 공개 범위 확인
            if (res.status === 403) {
                try {
                    const errorData = await res.json();
                    const errorCode = errorData?.code || errorData?.errorCode;

                    if (errorCode === "PR002") {
                        // 비공개 프로필
                        if (reset && page === 0) {
                            showProfileRestricted("비공개 프로필입니다");
                        }
                        isLoading = false;
                        return;
                    } else if (errorCode === "PR001") {
                        // 친구만 공개
                        if (reset && page === 0) {
                            showProfileRestricted("친구공개 프로필입니다");
                        }
                        isLoading = false;
                        return;
                    }
                } catch (parseError) {
                    console.error("에러 응답 파싱 실패:", parseError);
                }
            }
            throw new Error("러닝 기록 조회 실패");
        }

        const payload = await res.json();
        const sliceData = payload?.data;

        if (!sliceData) {
            // 데이터가 없을 때 초기 로드면 빈 상태 표시
            if (reset && page === 0) {
                const runList = document.querySelector('[data-role="run-list"]');
                if (runList) runList.innerHTML = "";
                showEmptyState("이번 주 러닝 기록이 없어요");
            }
            isLoading = false;
            return;
        }

        const records = sliceData.content || [];
        hasNext = sliceData.hasNext ?? false;
        currentPage = page;

        if (reset) {
            const runList = document.querySelector('[data-role="run-list"]');
            if (runList) runList.innerHTML = "";
        }

        // 기록이 있으면 렌더링하고 빈 상태 숨김
        if (records.length > 0) {
            renderRunningRecords(records);
            hideEmptyState();
        } else if (reset && currentPage === 0) {
            // 초기 로드 시 기록이 없으면 빈 상태 표시
            showEmptyState("이번 주 러닝 기록이 없어요");
        } else {
            // 추가 페이지 로드 시 기록이 없으면 빈 상태는 유지 (이미 표시되어 있을 수 있음)
            // 빈 상태가 이미 표시되어 있지 않다면 숨김
            if (currentPage > 0) {
                hideEmptyState();
            }
        }

        // 무한 스크롤 업데이트
        updateScrollSentinel();

    } catch (e) {
        console.error("러닝 기록 로드 실패:", e);
        // 초기 로드 시 에러가 발생하면 빈 상태 표시
        if (reset && page === 0) {
            const runList = document.querySelector('[data-role="run-list"]');
            if (runList) runList.innerHTML = "";
            showEmptyState("이번 주 러닝 기록이 없어요");
        }
    } finally {
        isLoading = false;
    }
}

/**
 * 러닝 기록 렌더링
 */
function renderRunningRecords(records) {
    const runList = document.querySelector('[data-role="run-list"]');
    if (!runList) return;

    // 기록이 있으면 빈 상태 먼저 숨김
    hideEmptyState();

    records.forEach(record => {
        const card = createRunCard(record);
        runList.appendChild(card);
    });
}

/**
 * 빈 상태 표시
 */
function showEmptyState(message = "이번 주 러닝 기록이 없어요") {
    const emptyState = document.getElementById("runListEmpty");
    const runList = document.querySelector('[data-role="run-list"]');
    const weeklyStatsSection = document.querySelector('.weekly-stats-section');

    if (emptyState) {
        const messageEl = document.getElementById("emptyStateMessage");
        if (messageEl) {
            messageEl.textContent = message;
        }
        emptyState.removeAttribute("hidden");
        emptyState.style.display = "flex";
    }
    if (runList) {
        runList.style.display = "none";
    }
}

/**
 * 빈 상태 숨김
 */
function hideEmptyState() {
    const emptyState = document.getElementById("runListEmpty");
    const runList = document.querySelector('[data-role="run-list"]');
    if (emptyState) {
        emptyState.setAttribute("hidden", "hidden");
        emptyState.style.display = "none";
    }
    if (runList) {
        runList.style.display = "flex";
    }
}

/**
 * 프로필 제한 상태 표시 (비공개 또는 친구만 공개)
 */
function showProfileRestricted(message) {
    // 러닝 기록 리스트 숨기기
    const runList = document.querySelector('[data-role="run-list"]');
    if (runList) {
        runList.style.display = "none";
    }

    // 빈 상태 표시
    showEmptyState(message);
}

/**
 * 러닝 기록 카드 생성 (피드에 공유 버튼 제외)
 */
function createRunCard(record) {
    const article = document.createElement('article');
    article.className = 'run-card';

    // 날짜 포맷팅
    const date = new Date(record.startedAt);
    const month = date.getMonth() + 1;
    const day = date.getDate();
    const dayOfWeek = ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'][date.getDay()];
    const formattedDate = `${month}/${day} ${dayOfWeek}`;

    // 시간 포맷팅 (초 -> MM:SS 또는 HH:MM:SS)
    const timeStr = formatDuration(record.totalTimeSec);

    // 거리 포맷팅
    const distanceStr = record.totalDistanceKm ? record.totalDistanceKm.toFixed(1) : '0.0';

    // 페이스 포맷팅 (분/km)
    const paceStr = formatPace(record.avgPace);

    // ✅ 실행 타입/썸네일 결정
    const isGhostRun = record.runningType === 'GHOST';
    const isOnlineBattle = record.runningType === 'ONLINEBATTLE';

    const defaultGhostImageUrl = '/img/ghost-run.png';

    // 온라인배틀 등수별 이미지 (1~4등 제공)
    let onlineBattleRanking = (typeof record.onlineBattleRanking === 'number')
        ? record.onlineBattleRanking
        : (record.onlineBattleRanking ? Number(record.onlineBattleRanking) : null);

    // 디버깅: 온라인배틀일 때 등수 확인
    if (isOnlineBattle) {
        console.log('온라인배틀 기록:', {
            recordId: record.runningResultId,
            onlineBattleRanking: record.onlineBattleRanking,
            onlineBattleRankingType: typeof record.onlineBattleRanking,
            converted: onlineBattleRanking
        });
        
        // 등수 정보가 없으면 finalRank 필드 확인 (백엔드에서 다른 필드명 사용 가능성)
        if (onlineBattleRanking === null || onlineBattleRanking === undefined) {
            onlineBattleRanking = record.finalRank || record.rank || record.ranking || null;
            console.log('대체 필드에서 등수 확인:', onlineBattleRanking);
        }
    }

    const onlineBattleRankImageMap = {
        1: '/img/online-1st.png',
        2: '/img/online-2nd.png',
        3: '/img/online-3rd.png',
        4: '/img/online-4th.png'
    };

    const defaultOnlineBattleImageUrl = '/img/online-1st.png'; // fallback

    // 썸네일 URL 우선순위:
    // 1) 고스트런: 고정 이미지
    // 2) 온라인배틀: 등수별 이미지
    // 3) 일반: recordImageUrl
    const imageUrl = isGhostRun
        ? defaultGhostImageUrl
        : (isOnlineBattle
            ? (onlineBattleRankImageMap[onlineBattleRanking] || defaultOnlineBattleImageUrl)
            : (record.recordImageUrl || null));

    // ✅ 제목 결정 (우선순위: 고스트런 > 온라인배틀 > 일반)
    const courseTitle = isGhostRun
        ? '고스트런'
        : (isOnlineBattle ? '온라인배틀' : (record.courseTitle || '러닝'));

    const titleSuffix = (!isGhostRun && isOnlineBattle && onlineBattleRanking)
        ? ` <span class="run-title-rank">#${onlineBattleRanking}</span>`
        : '';

    // 러닝 타입 레이블
    const runningTypeLabel = getRunningTypeLabel(record.runningType);

    // 이미지가 있을 때만 img 태그 추가
    const thumbContent = imageUrl
        ? `<img src="${imageUrl}" alt="${courseTitle}" onerror="this.style.display='none'" />`
        : '';

    article.innerHTML = `
        <div class="run-thumb">
            ${thumbContent}
        </div>
        <div class="run-content">
            <div class="run-header">
                <span class="run-date">${formattedDate}</span>
                <span class="run-type">${runningTypeLabel}</span>
            </div>
            <p class="run-title">${courseTitle}${titleSuffix}</p>
            <div class="run-stats">
                <span class="run-stat">
                    <span class="run-icon">🏃‍♂️</span>
                    <span>${distanceStr}km</span>
                </span>
                <span class="run-stat">
                    <span class="run-icon">⏱</span>
                    <span>${timeStr}</span>
                </span>
            </div>
            <div class="run-pace">
                <span class="run-pace-label">평균 페이스</span>
                <span class="run-pace-value">${paceStr}</span>
            </div>
        </div>
    `;

    return article;
}

/**
 * 시간 포맷팅 (초 -> MM:SS 또는 HH:MM:SS)
 */
function formatDuration(seconds) {
    if (!seconds || seconds === 0) return "00:00";
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    if (hours > 0) {
        return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
    }
    return `${String(minutes).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
}

/**
 * 페이스 포맷팅 (분/km)
 */
function formatPace(pace) {
    if (!pace || pace === 0) return "-";
    const minutes = Math.floor(pace);
    const seconds = Math.floor((pace - minutes) * 60);
    return `${minutes}'${String(seconds).padStart(2, "0")}"`;
}

/**
 * 러닝 타입 레이블
 */
function getRunningTypeLabel(runningType) {
    const typeMap = {
        SOLO: "솔로",
        OFFLINE: "오프라인",
        ONLINEBATTLE: "온라인배틀",
        GHOST: "고스트"
    };
    return typeMap[runningType] || runningType || "-";
}

/**
 * 무한 스크롤 초기화
 */
function initInfiniteScroll(userId) {
    const observerOptions = {
        root: null,
        rootMargin: "200px",
        threshold: 0
    };

    scrollObserver = new IntersectionObserver((entries) => {
        for (const entry of entries) {
            if (!entry.isIntersecting) continue;

            if (!userHasInteracted) {
                console.log('Sentinel intersecting but waiting for user scroll interaction');
                continue;
            }

            if (!hasNext || isLoading) continue;

            console.log("Sentinel intersecting, loading next page:", currentPage + 1);
            loadRunningRecords(userId, currentPage + 1, false);
        }
    }, observerOptions);
}

/**
 * 무한 스크롤 센티넬 요소 관리
 */
function updateScrollSentinel() {
    const runList = document.querySelector('[data-role="run-list"]');
    if (!runList) return;

    // 기존 센티넬 제거
    const oldSentinel = document.getElementById("scrollSentinel");
    if (oldSentinel) {
        if (scrollObserver) scrollObserver.unobserve(oldSentinel);
        oldSentinel.remove();
    }

    if (!hasNext || !scrollObserver) {
        console.log("No more data to load, hasNext:", hasNext, "observer:", !!scrollObserver);
        return;
    }

    const sentinel = document.createElement("div");
    sentinel.id = "scrollSentinel";
    sentinel.style.height = "1px";
    sentinel.style.width = "100%";
    sentinel.style.visibility = "hidden";

    // 리스트 끝에 센티넬 추가
    runList.appendChild(sentinel);

    // 사용자가 스크롤한 후에만 센티널 관찰 시작 (초기 로드 시 자동 로드 방지)
    if (userHasInteracted) {
        requestAnimationFrame(() => {
            const sentinelEl = document.getElementById("scrollSentinel");
            if (!sentinelEl || !scrollObserver) return;
            scrollObserver.observe(sentinelEl);
            console.log("Sentinel observed (root: viewport) hasNext:", hasNext);
        });
    } else {
        console.log("Sentinel created but not observed yet (waiting for user interaction)");
    }
}

/**
 * 사용자 스크롤 상호작용 감지 (무한 스크롤 활성화)
 */
function attachUserScrollGate(userId) {
    const markInteracted = () => {
        if (userHasInteracted) return;
        userHasInteracted = true;
        console.log('User interaction detected: infinite scroll enabled');

        // 이벤트 리스너 정리
        window.removeEventListener('scroll', markInteracted);
        window.removeEventListener('wheel', markInteracted);
        window.removeEventListener('touchmove', markInteracted);

        const page = document.querySelector('.mypage-page');
        if (page) {
            page.removeEventListener('scroll', markInteracted);
            page.removeEventListener('wheel', markInteracted);
            page.removeEventListener('touchmove', markInteracted);
        }

        // [추가] 인터랙션 감지 시점에 센티넬을 관찰 시작
        const sentinel = document.getElementById("scrollSentinel");
        if (sentinel && scrollObserver) {
            requestAnimationFrame(() => {
                const sentinelEl = document.getElementById("scrollSentinel");
                if (!sentinelEl || !scrollObserver) return;

                // 센티널 관찰 시작
                scrollObserver.observe(sentinelEl);
                console.log("Sentinel observed after user interaction, hasNext:", hasNext);

                // 인터랙션 감지 시점에 센티넬이 이미 화면 안에 있다면 즉시 로드
                if (hasNext && !isLoading) {
                    const rect = sentinelEl.getBoundingClientRect();
                    if (rect.top <= window.innerHeight + 200) {
                        console.log("Sentinel already visible upon interaction, loading next page:", currentPage + 1);
                        loadRunningRecords(userId, currentPage + 1, false);
                    }
                }
            });
        }
    };

    // window와 .mypage-page 모두에 이벤트 등록
    window.addEventListener('scroll', markInteracted, {passive: true});
    window.addEventListener('wheel', markInteracted, {passive: true});
    window.addEventListener('touchmove', markInteracted, {passive: true});

    const page = document.querySelector('.mypage-page');
    if (page) {
        page.addEventListener('scroll', markInteracted, {passive: true});
        page.addEventListener('wheel', markInteracted, {passive: true});
        page.addEventListener('touchmove', markInteracted, {passive: true});
    }
}

