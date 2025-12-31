// 전역 변수: 무한 스크롤 관리
let currentPage = 0;
let hasNext = true;
let isLoading = false;
let userHasInteracted = false; // 사용자가 실제로 스크롤을 했는지

document.addEventListener("DOMContentLoaded", () => {
    console.log("friend-list.js loaded");

    attachBackButtonHandler();
    loadFriendRequests();

    // 실제 스크롤이 발생하는 컨테이너를 기준으로 무한 스크롤 동작
    initInfiniteScroll();

    // '스크롤(또는 휠/터치) 입력' 이후에만 다음 페이지 로드
    attachUserScrollGate();

    loadFriends(0, true); // 초기 로드 (첫 페이지, 초기화)
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

/**
 * 친구 목록 로드 (Slice 기반 페이지네이션)
 * @param {number} page - 페이지 번호 (0부터 시작)
 * @param {boolean} reset - true면 목록 초기화, false면 기존 목록에 추가
 */
async function loadFriends(page = 0, reset = false) {
    if (isLoading || (!hasNext && !reset)) {
        console.log("Skipping load:", {isLoading, hasNext, reset, page});
        return;
    }

    try {
        isLoading = true;
        const token = localStorage.getItem("accessToken");
        if (!token) {
            console.error("No access token found");
            return;
        }

        const url = `/friends?page=${page}&size=2`;
        console.log("Loading friends:", url);
        const res = await fetch(url, {
            headers: {"Authorization": `Bearer ${token}`}
        });

        if (!res.ok) {
            throw new Error("친구 목록 조회 실패");
        }

        const payload = await res.json();
        const pageData = payload?.data ?? {};
        const friends = pageData?.content ?? [];

        hasNext = !(pageData?.last ?? true);
        currentPage = page;

        console.log("Loaded friends:", {
            page,
            friendsCount: friends.length,
            hasNext,
            last: pageData?.last,
            totalElements: pageData?.totalElements,
            totalPages: pageData?.totalPages,
            currentPage
        });

        renderFriends(friends, reset);
    } catch (e) {
        console.error("Error loading friends:", e);
    } finally {
        isLoading = false;
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

/**
 * 친구 목록 렌더링
 * @param {Array} friends - 친구 목록 배열
 * @param {boolean} reset - true면 기존 목록 초기화, false면 기존 목록에 추가
 */
function renderFriends(friends, reset = true) {
    const container = document.getElementById("friendsList");
    if (!container) return;

    if (reset) {
        container.innerHTML = "";
    }

    if (friends.length === 0) {
        // 초기 로드이고 친구가 없을 때만 empty state 업데이트
        if (reset) {
            const requestNotification = document.getElementById("friendRequestNotification");
            const hasRequests = requestNotification && !requestNotification.hasAttribute("hidden");
            const requestCount = hasRequests
                ? parseInt(document.getElementById("requestCount")?.textContent || "0", 10)
                : 0;
            updateEmptyState(requestCount, 0);
        }
        // 센티넬 업데이트 (더 이상 불러올 데이터 없음)
        updateScrollSentinel();
        return;
    }

    friends.forEach(friend => {
        const item = createFriendItem(friend);
        container.appendChild(item);
    });

    // 무한 스크롤 센티넬 업데이트
    updateScrollSentinel();
}

function createFriendRequestItem(request) {
    const user = request.user;
    const item = document.createElement("div");
    item.className = "friend-item";
    item.dataset.friendId = request.friendId;
    item.dataset.userId = user.userId;

    item.innerHTML = `
        <div class="friend-profile-link" data-role="profile-link" data-user-id="${user.userId}">
            ${renderProfileImage(user.profileImageUrl)}
        </div>
        <div class="friend-info friend-info-link" data-role="profile-link" data-user-id="${user.userId}">
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

    // 프로필 링크 클릭 이벤트 (프로필 이미지와 정보 영역)
    const profileLinks = item.querySelectorAll('[data-role="profile-link"]');
    profileLinks.forEach(link => {
        link.addEventListener("click", (e) => {
            e.stopPropagation();
            const userId = link.dataset.userId;
            if (userId) {
                window.location.href = `/profile/${userId}`;
            }
        });
    });

    // 수락 버튼
    const acceptBtn = item.querySelector('[data-role="accept-request"]');
    if (acceptBtn) {
        acceptBtn.addEventListener("click", (e) => {
            e.stopPropagation();
            acceptFriendRequest(request.friendId);
        });
    }

    // 거절 버튼
    const rejectBtn = item.querySelector('[data-role="reject-request"]');
    if (rejectBtn) {
        rejectBtn.addEventListener("click", (e) => {
            e.stopPropagation();
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
        <div class="friend-profile-link" data-role="profile-link" data-user-id="${user.userId}">
            ${renderProfileImage(user.profileImageUrl)}
        </div>
        <div class="friend-info friend-info-link" data-role="profile-link" data-user-id="${user.userId}">
            <div class="friend-id">${escapeHtml(user.loginId)}</div>
            <div class="friend-stats">
                <div class="friend-stat">누적: xx km</div>
                <div class="friend-stat">랭킹: xx위</div>
            </div>
        </div>
        <button class="friend-delete-icon-btn" data-role="delete-friend" data-friend-id="${user.userId}" aria-label="친구 삭제">
            <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 31 31" fill="none">
                <path d="M24.2188 31C26.0173 31 27.7421 30.2855 29.0138 29.0138C30.2855 27.7421 31 26.0173 31 24.2188C31 22.4202 30.2855 20.6954 29.0138 19.4237C27.7421 18.152 26.0173 17.4375 24.2188 17.4375C22.4203 17.4375 20.6954 18.152 19.4237 19.4237C18.152 20.6954 17.4375 22.4202 17.4375 24.2188C17.4375 26.0173 18.152 27.7421 19.4237 29.0138C20.6954 30.2855 22.4203 31 24.2188 31ZM21.3125 23.25H27.125C27.3819 23.25 27.6283 23.3521 27.81 23.5337C27.9917 23.7154 28.0938 23.9618 28.0938 24.2188C28.0938 24.4757 27.9917 24.7221 27.81 24.9038C27.6283 25.0854 27.3819 25.1875 27.125 25.1875H21.3125C21.0556 25.1875 20.8092 25.0854 20.6275 24.9038C20.4458 24.7221 20.3438 24.4757 20.3438 24.2188C20.3438 23.9618 20.4458 23.7154 20.6275 23.5337C20.8092 23.3521 21.0556 23.25 21.3125 23.25ZM21.3125 9.6875C21.3125 11.2291 20.7001 12.7075 19.6101 13.7976C18.52 14.8876 17.0416 15.5 15.5 15.5C13.9584 15.5 12.48 14.8876 11.3899 13.7976C10.2999 12.7075 9.6875 11.2291 9.6875 9.6875C9.6875 8.14593 10.2999 6.6675 11.3899 5.57744C12.48 4.48739 13.9584 3.875 15.5 3.875C17.0416 3.875 18.52 4.48739 19.6101 5.57744C20.7001 6.6675 21.3125 8.14593 21.3125 9.6875ZM15.5 13.5625C16.5277 13.5625 17.5133 13.1542 18.24 12.4275C18.9667 11.7008 19.375 10.7152 19.375 9.6875C19.375 8.65979 18.9667 7.67416 18.24 6.94746C17.5133 6.22076 16.5277 5.8125 15.5 5.8125C14.4723 5.8125 13.4867 6.22076 12.76 6.94746C12.0333 7.67416 11.625 8.65979 11.625 9.6875C11.625 10.7152 12.0333 11.7008 12.76 12.4275C13.4867 13.1542 14.4723 13.5625 15.5 13.5625Z" fill="black"/>
                <path d="M15.996 27.125C15.7742 26.4957 15.6253 25.843 15.5523 25.1798H5.8125C5.81444 24.7031 6.11088 23.2694 7.4245 21.9557C8.68775 20.6925 11.0651 19.375 15.5 19.375C16.0037 19.375 16.4817 19.3911 16.9337 19.4234C17.3716 18.7627 17.8947 18.1641 18.4915 17.6448C17.5873 17.5092 16.5902 17.4401 15.5 17.4375C5.8125 17.4375 3.875 23.25 3.875 25.1875C3.875 27.125 5.8125 27.125 5.8125 27.125H15.996Z" fill="black"/>
            </svg>
        </button>
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

    // 프로필 링크 클릭 이벤트 (프로필 이미지와 정보 영역)
    const profileLinks = item.querySelectorAll('[data-role="profile-link"]');
    profileLinks.forEach(link => {
        link.addEventListener("click", (e) => {
            e.stopPropagation();
            const userId = link.dataset.userId;
            if (userId) {
                window.location.href = `/profile/${userId}`;
            }
        });
    });

    // 친구 삭제 버튼
    const deleteBtn = item.querySelector('[data-role="delete-friend"]');
    if (deleteBtn) {
        deleteBtn.addEventListener("click", (e) => {
            e.stopPropagation();
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
        // 친구 목록 초기화 후 첫 페이지 로드
        currentPage = 0;
        hasNext = true;
        loadFriends(0, true);
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
        // 목록 새로고침: 초기화 후 첫 페이지 로드
        currentPage = 0;
        hasNext = true;
        loadFriends(0, true);
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

// 무한 스크롤 Observer
let scrollObserver = null;

/**
 * 무한 스크롤 초기화
 */
function initInfiniteScroll() {
    // CSS 구조상 .friend-list-page에 height 제한이 없어 body 스크롤이 발생할 가능성이 높음
    // 따라서 root를 null(viewport)로 설정하여 어디서 스크롤하든 감지되도록 함
    const observerOptions = {
        root: null,
        rootMargin: "200px", // 하단 여유
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
            loadFriends(currentPage + 1, false);
        }
    }, observerOptions);
}

/**
 * 무한 스크롤 센티넬 요소 관리
 */
function updateScrollSentinel() {
    const listContainer = document.getElementById("friendsList");
    if (!listContainer) return;

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
    listContainer.appendChild(sentinel);

    requestAnimationFrame(() => {
        const sentinelEl = document.getElementById("scrollSentinel");
        if (!sentinelEl || !scrollObserver) return;
        scrollObserver.observe(sentinelEl);
        console.log("Sentinel observed (root: viewport) hasNext:", hasNext);
    });
}

function attachUserScrollGate() {
    const markInteracted = () => {
        if (userHasInteracted) return;
        userHasInteracted = true;
        console.log('User interaction detected: infinite scroll enabled');

        // 이벤트 리스너 정리
        window.removeEventListener('scroll', markInteracted);
        window.removeEventListener('wheel', markInteracted);
        window.removeEventListener('touchmove', markInteracted);

        const page = document.querySelector('.friend-list-page');
        if (page) {
            page.removeEventListener('scroll', markInteracted);
            page.removeEventListener('wheel', markInteracted);
            page.removeEventListener('touchmove', markInteracted);
        }

        // [추가] 인터랙션 감지 시점에 센티넬이 이미 화면 안에 있다면 즉시 로드
        // (Observer는 이미 교차 중인 상태에서는 콜백을 다시 호출하지 않기 때문)
        const sentinel = document.getElementById("scrollSentinel");
        if (sentinel && hasNext && !isLoading) {
            const rect = sentinel.getBoundingClientRect();
            // rootMargin(200px)과 동일하게 여유를 둠
            if (rect.top <= window.innerHeight + 200) {
                console.log("Sentinel already visible upon interaction, loading next page:", currentPage + 1);
                loadFriends(currentPage + 1, false);
            }
        }
    };

    // window와 .friend-list-page 모두에 이벤트 등록 (어디서 스크롤이 발생하든 감지)
    // scroll 뿐만 아니라 wheel, touchmove도 감지하여 사용자 의도를 파악
    window.addEventListener('scroll', markInteracted, {passive: true});
    window.addEventListener('wheel', markInteracted, {passive: true});
    window.addEventListener('touchmove', markInteracted, {passive: true});

    const page = document.querySelector('.friend-list-page');
    if (page) {
        page.addEventListener('scroll', markInteracted, {passive: true});
        page.addEventListener('wheel', markInteracted, {passive: true});
        page.addEventListener('touchmove', markInteracted, {passive: true});
    }
}

function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
}
