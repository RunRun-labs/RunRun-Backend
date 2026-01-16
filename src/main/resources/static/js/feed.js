document.addEventListener("DOMContentLoaded", () => {
    console.log("feed.js loaded");
    initFeedPage();
});

// 전역 변수
let currentPage = 0;
let hasNext = true;
let isLoading = false;
let currentSort = "latest"; // latest, popular, my
let feedLikes = new Map(); // feedId -> isLiked 상태 추적
let openCommentSections = new Set(); // 댓글 영역이 열린 feedId들

/**
 * JWT 토큰에서 사용자 역할 추출
 */
function getUserRoleFromToken() {
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken) {
        return null;
    }

    try {
        const parts = accessToken.split(".");
        if (parts.length !== 3) {
            return null;
        }

        const payload = parts[1];
        let base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
        while (base64.length % 4) {
            base64 += "=";
        }
        const decodedPayload = JSON.parse(atob(base64));

        const auth = decodedPayload.auth || decodedPayload.role || decodedPayload.roles || decodedPayload.authorities;

        if (!auth) {
            return null;
        }

        let roles = [];
        if (Array.isArray(auth)) {
            roles = auth;
        } else if (typeof auth === 'string') {
            roles = auth.split(",").map(role => role.trim());
        }

        return roles;
    } catch (error) {
        console.error("JWT 토큰 디코딩 실패:", error);
        return null;
    }
}

/**
 * 사용자가 관리자인지 확인
 */
function isAdmin() {
    const roles = getUserRoleFromToken();
    if (!roles) {
        return false;
    }
    return roles.includes("ROLE_ADMIN") || roles.includes("ADMIN");
}

/**
 * 피드 페이지 초기화
 */
function initFeedPage() {
    // URL 파라미터에서 정렬 옵션 확인
    const urlParams = new URLSearchParams(window.location.search);
    let sortParam = urlParams.get("sort");

    // URL 파라미터가 없으면 localStorage 확인 (마이페이지에서 "내 게시물" 클릭 시)
    if (!sortParam) {
        const feedSortToMy = localStorage.getItem("feedSortToMy");
        if (feedSortToMy === "true") {
            sortParam = "my";
            // 플래그 제거 (한 번만 적용)
            localStorage.removeItem("feedSortToMy");
        }
    }

    if (sortParam && ["latest", "popular", "my"].includes(sortParam)) {
        currentSort = sortParam;
        // 해당 정렬 탭 활성화
        const sortItems = document.querySelectorAll(".sort-item");
        sortItems.forEach(item => {
            item.classList.remove("active");
            if (item.getAttribute("data-sort") === sortParam) {
                item.classList.add("active");
            }
        });
    }

    attachShareButtonHandler();
    attachSortHandlers();
    setActiveBottomNavItem();
    initInfiniteScroll();
    hideEmptyState(); // 초기 로드 시 빈 상태 숨김
    loadFeeds(0, true);
}

/**
 * 하단 네비게이션 활성 항목 설정
 */
function setActiveBottomNavItem() {
    // bottom-nav가 렌더링될 때까지 대기
    const checkBottomNav = () => {
        const navItems = document.querySelectorAll(".bottom-nav .nav-item");
        if (navItems.length === 0) {
            // 아직 렌더링되지 않았으면 잠시 후 다시 시도
            setTimeout(checkBottomNav, 100);
            return;
        }

        navItems.forEach(item => {
            const href = item.getAttribute("href");
            // 피드 페이지인 경우 feed 항목 활성화
            if (href && (href === "/feed" || href === "/feed/" || href.startsWith("/feed"))) {
                item.classList.add("active");
            } else {
                item.classList.remove("active");
            }
        });
    };

    checkBottomNav();
}

/**
 * 나의 런 공유하기 버튼 핸들러
 */
function attachShareButtonHandler() {
    const shareButton = document.getElementById("shareRunButton");
    if (shareButton) {
        shareButton.addEventListener("click", () => {
            window.location.href = "/feed/records";
        });
    }
}

/**
 * 정렬 옵션 핸들러
 */
function attachSortHandlers() {
    const sortItems = document.querySelectorAll(".sort-item");
    sortItems.forEach(item => {
        item.addEventListener("click", () => {
            const sort = item.getAttribute("data-sort");
            if (sort === currentSort) return;

            // 활성 상태 변경
            sortItems.forEach(i => i.classList.remove("active"));
            item.classList.add("active");

            // 정렬 변경 및 피드 다시 로드
            currentSort = sort;
            currentPage = 0;
            hasNext = true;
            loadFeeds(0, true);
        });
    });
}

/**
 * 무한 스크롤 초기화
 */
function initInfiniteScroll() {
    const sentinel = document.createElement("div");
    sentinel.id = "scroll-sentinel";
    sentinel.style.height = "1px";
    document.querySelector(".feed-list").appendChild(sentinel);

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting && hasNext && !isLoading) {
                loadFeeds(currentPage + 1, false);
            }
        });
    }, {threshold: 0.1});

    observer.observe(sentinel);
}

/**
 * 피드 목록 로드
 */
async function loadFeeds(page = 0, reset = false) {
    if (isLoading || (!hasNext && !reset)) return;

    isLoading = true;
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            window.location.href = "/login";
            return;
        }

        // 정렬에 따라 API 엔드포인트 선택
        let url = "/api/feed";
        if (currentSort === "my") {
            url = "/api/feed/me";
        }

        // 인기순의 경우 전체 데이터를 가져와서 정렬 (페이지네이션 없이)
        if (currentSort === "popular") {
            url += `?page=0&size=1000&sort=createdAt,desc`; // 충분히 큰 사이즈로 전체 데이터 가져오기
        } else {
            url += `?page=${page}&size=5&sort=createdAt,desc`;
        }

        const response = await fetch(url, {
            headers: {Authorization: `Bearer ${token}`}
        });

        if (!response.ok) {
            if (response.status === 401) {
                window.location.href = "/login";
                return;
            }
            throw new Error("피드 조회 실패");
        }

        const payload = await response.json();
        const pageData = payload?.data;

        if (!pageData) {
            isLoading = false;
            return;
        }

        let feeds = pageData.content || [];

        // 인기순 정렬: 좋아요 개수 + 댓글 개수 합계로 정렬
        if (currentSort === "popular") {
            feeds = feeds.sort((a, b) => {
                const popularityA = (a.likeCount || 0) + (a.commentCount || 0);
                const popularityB = (b.likeCount || 0) + (b.commentCount || 0);
                return popularityB - popularityA; // 내림차순 정렬
            });

            // 페이지네이션 처리 (인기순은 전체 데이터를 가져온 후 프론트에서 페이지네이션)
            const pageSize = 5;
            const startIndex = page * pageSize;
            const endIndex = startIndex + pageSize;
            feeds = feeds.slice(startIndex, endIndex);
            hasNext = endIndex < pageData.content.length;
        } else {
            hasNext = !pageData.last;
        }

        currentPage = page;

        // 디버깅: 피드 데이터 확인
        if (feeds.length > 0) {
            console.log("피드 데이터 샘플:", feeds[0]);
            console.log("첫 번째 피드 isLiked:", feeds[0]?.isLiked);
        }

        if (reset) {
            const feedList = document.querySelector('[data-role="feed-list"]');
            if (feedList) feedList.innerHTML = "";
            openCommentSections.clear();
            feedLikes.clear(); // 리셋 시 좋아요 상태 초기화
            hideEmptyState(); // 리셋 시 빈 상태 숨김
        }

        if (feeds.length > 0) {
            renderFeeds(feeds);
            hideEmptyState();
        } else if (reset && currentPage === 0) {
            // 초기 로드이고 피드가 없을 때만 빈 상태 표시
            showEmptyState();
        }

    } catch (error) {
        console.error("피드 로드 실패:", error);
        if (reset && currentPage === 0) {
            showEmptyState();
        }
    } finally {
        isLoading = false;
    }
}

/**
 * 피드 카드 렌더링
 */
function renderFeeds(feeds) {
    const feedList = document.querySelector('[data-role="feed-list"]');
    if (!feedList) return;

    // 피드가 있으면 빈 상태 먼저 숨김
    hideEmptyState();

    let feedCount = 0;
    feeds.forEach(feed => {
        feedCount++;
        const feedCard = createFeedCard(feed);
        feedList.appendChild(feedCard);
        
        // ✅ 5개마다 1개 광고 삽입 (5개 미만이어도 1개는 표시)
        if (feedCount === 1 || feedCount % 5 === 0) {
          insertFeedAd(feedList);
        }
    });
}

/**
 * 피드 리스트에 광고 삽입
 */
async function insertFeedAd(feedList) {
  try {
    if (typeof loadAd === 'function' && typeof createAdBanner === 'function') {
      const adData = await loadAd('FEED_LIST_ITEM');
      if (adData) {
        const adBanner = createAdBanner(adData, 'feed-ad-banner');
        feedList.appendChild(adBanner);
      }
    }
  } catch (error) {
    console.warn('피드 광고 로드 실패:', error);
  }
}

/**
 * 피드 카드 생성
 */
function createFeedCard(feed) {
    const article = document.createElement("article");
    article.className = "feed-card";
    article.setAttribute("data-feed-id", feed.feedId);

    // 헤더 (프로필 이미지, 사용자 정보)
    const header = document.createElement("div");
    header.className = "feed-card-header";

    const profileImg = document.createElement("img");
    profileImg.className = "feed-profile-image";
    profileImg.src = feed.profileImageUrl || '/img/default-profile.svg';
    profileImg.alt = feed.userLoginId;
    profileImg.style.cursor = "pointer";
    profileImg.onerror = function () {
        // 기본 이미지로 교체
        this.src = '/img/default-profile.svg';
    };
    // 프로필 이미지 클릭 시 프로필 페이지로 이동
    profileImg.addEventListener("click", () => {
        window.location.href = `/profile/${feed.userId}`;
    });

    const userInfo = document.createElement("div");
    userInfo.className = "feed-user-info";

    const loginId = document.createElement("div");
    loginId.className = "feed-user-login-id";
    loginId.textContent = feed.userLoginId || "-";

    const dateInfo = document.createElement("div");
    dateInfo.className = "feed-date-info";
    
    // 러닝 날짜와 게시 날짜 모두 표시
    const parts = [];
    
    // 러닝 날짜 (선택적 - 없을 수도 있음)
    if (feed.startedAt) {
        try {
            const runDate = formatRunDate(feed.startedAt);
            if (runDate && runDate !== '-') {
                parts.push(`🏃 ${runDate}`);
            }
        } catch (error) {
            console.error('러닝 날짜 포맷팅 에러:', error);
        }
    }
    
    // 게시 날짜 (필수)
    if (feed.createdAt) {
        try {
            const postDate = formatRelativeTime(feed.createdAt);
            if (postDate && postDate !== '-') {
                parts.push(`${postDate} 게시`);
            }
        } catch (error) {
            console.error('게시 날짜 포맷팅 에러:', error);
            // 최종 fallback
            parts.push(`게시: ${formatDate(feed.createdAt)}`);
        }
    }
    
    dateInfo.textContent = parts.length > 0 ? parts.join(' • ') : '';

    userInfo.appendChild(loginId);
    userInfo.appendChild(dateInfo);
    header.appendChild(profileImg);
    header.appendChild(userInfo);

    // 이미지
    const imageContainer = document.createElement("div");
    imageContainer.className = "feed-image-container";

    const image = document.createElement("img");
    image.className = "feed-image";
    image.src = feed.imageUrl || '';
    image.alt = "러닝 코스 이미지";
    image.onerror = function () {
        this.style.display = 'none';
    };

    imageContainer.appendChild(image);

    // 코스 제목 (이미지 하단)
    const courseTitle = document.createElement("div");
    courseTitle.className = "feed-course-title";
    courseTitle.textContent = getFeedDisplayTitle(feed);

    // ONLINEBATTLE인데 등수가 아직 없으면 비동기 로드 후 타이틀 업데이트
    if (feed.runningType === 'ONLINEBATTLE' && !feed.courseTitle) {
        const runningResultId = feed.runningResultId;
        fetchOnlineBattleRanking(runningResultId).then((rank) => {
            if (rank && courseTitle.isConnected) {
                courseTitle.textContent = `온라인배틀 #${rank}`;
            }
        });
    }

    // 통계 (거리, 시간)
    const stats = document.createElement("div");
    stats.className = "feed-stats";

    const distanceItem = document.createElement("div");
    distanceItem.className = "feed-stat-item";
    const distanceIcon = document.createElement("span");
    distanceIcon.className = "feed-stat-icon";
    distanceIcon.textContent = "🏃‍♂️";
    const distanceValue = document.createElement("span");
    distanceValue.className = "feed-stat-value";
    distanceValue.textContent = `${feed.totalDistance?.toFixed(1) || 0}km`;
    distanceItem.appendChild(distanceIcon);
    distanceItem.appendChild(distanceValue);

    const timeItem = document.createElement("div");
    timeItem.className = "feed-stat-item";
    const timeIcon = document.createElement("span");
    timeIcon.className = "feed-stat-icon";
    timeIcon.textContent = "⏱";
    const timeValue = document.createElement("span");
    timeValue.className = "feed-stat-value";
    timeValue.textContent = formatDuration(feed.totalTime || 0);
    timeItem.appendChild(timeIcon);
    timeItem.appendChild(timeValue);

    stats.appendChild(distanceItem);
    stats.appendChild(timeItem);

    // 평균 페이스
    const paceText = document.createElement("div");
    paceText.className = "feed-pace-text";
    paceText.textContent = `평균 페이스: ${formatPace(feed.avgPace)}`;

    // 내용
    const content = document.createElement("div");
    content.className = "feed-content";
    content.textContent = feed.content || "";

    // 액션 버튼 (좋아요, 댓글)
    const actions = document.createElement("div");
    actions.className = "feed-actions";

    // 좋아요 버튼
    const likeAction = document.createElement("div");
    likeAction.className = "feed-action-item";
    likeAction.setAttribute("data-action", "like");
    likeAction.setAttribute("data-feed-id", feed.feedId);

    // 좋아요 상태 초기화 (백엔드에서 받은 isLiked 값 사용)
    // Jackson 직렬화로 인해 isLiked 또는 liked로 올 수 있음
    const isLiked = feed.isLiked === true || feed.liked === true;
    feedLikes.set(feed.feedId, isLiked);

    // 디버깅: 좋아요 상태 확인
    if (feed.feedId) {
        console.log(`피드 ${feed.feedId} - 원본 데이터:`, {
            isLiked: feed.isLiked,
            liked: feed.liked,
            최종값: isLiked
        });
    }

    const likeIcon = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    likeIcon.className = "feed-action-icon";
    likeIcon.setAttribute("width", "16");
    likeIcon.setAttribute("height", "16");
    likeIcon.setAttribute("viewBox", "0 0 16 16");

    const likePath = document.createElementNS("http://www.w3.org/2000/svg", "path");

    if (isLiked) {
        // 이미 좋아요를 눌렀으면 채워진 하트
        likeIcon.setAttribute("fill", "currentColor");
        likePath.setAttribute("fill-rule", "evenodd");
        likePath.setAttribute("d", "M8 1.314C12.438-3.248 23.534 4.735 8 15-7.534 4.736 3.562-3.248 8 1.314");
        likePath.setAttribute("fill", "currentColor");
    } else {
        // 빈 하트
        likeIcon.setAttribute("fill", "currentColor");
        likePath.setAttribute("d", "m8 2.748-.717-.737C5.6.281 2.514.878 1.4 3.053c-.523 1.023-.641 2.5.314 4.385.92 1.815 2.834 3.989 6.286 6.357 3.452-2.368 5.365-4.542 6.286-6.357.955-1.886.838-3.362.314-4.385C13.486.878 10.4.28 8.717 2.01zM8 15C-7.333 4.868 3.279-3.04 7.824 1.143q.09.083.176.171a3 3 0 0 1 .176-.17C12.72-3.042 23.333 4.867 8 15");
        likePath.setAttribute("fill", "currentColor");
    }

    likeIcon.appendChild(likePath);

    const likeCount = document.createElement("span");
    likeCount.className = "feed-action-count";
    likeCount.textContent = feed.likeCount || 0;

    likeAction.appendChild(likeIcon);
    likeAction.appendChild(likeCount);

    // 좋아요 클릭 핸들러
    likeAction.addEventListener("click", () => handleLikeClick(feed.feedId, likeAction, likeCount));

    // 댓글 버튼
    const commentAction = document.createElement("div");
    commentAction.className = "feed-action-item";
    commentAction.setAttribute("data-action", "comment");
    commentAction.setAttribute("data-feed-id", feed.feedId);

    const commentIcon = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    commentIcon.className = "feed-action-icon";
    commentIcon.setAttribute("width", "16");
    commentIcon.setAttribute("height", "16");
    commentIcon.setAttribute("fill", "currentColor");
    commentIcon.setAttribute("viewBox", "0 0 16 16");
    const commentPath1 = document.createElementNS("http://www.w3.org/2000/svg", "path");
    commentPath1.setAttribute("d", "M5 8a1 1 0 1 1-2 0 1 1 0 0 1 2 0m4 0a1 1 0 1 1-2 0 1 1 0 0 1 2 0m3 1a1 1 0 1 0 0-2 1 1 0 0 0 0 2");
    const commentPath2 = document.createElementNS("http://www.w3.org/2000/svg", "path");
    commentPath2.setAttribute("d", "m2.165 15.803.02-.004c1.83-.363 2.948-.842 3.468-1.105A9 9 0 0 0 8 15c4.418 0 8-3.134 8-7s-3.582-7-8-7-8 3.134-8 7c0 1.76.743 3.37 1.97 4.6a10.4 10.4 0 0 1-.524 2.318l-.003.011a11 11 0 0 1-.244.637c-.079.186.074.394.273.362a22 22 0 0 0 .693-.125m.8-3.108a1 1 0 0 0-.287-.801C1.618 10.83 1 9.468 1 8c0-3.192 3.004-6 7-6s7 2.808 7 6-3.004 6-7 6a8 8 0 0 1-2.088-.272 1 1 0 0 0-.711.074c-.387.196-1.24.57-2.634.893a11 11 0 0 0 .398-2");
    commentIcon.appendChild(commentPath1);
    commentIcon.appendChild(commentPath2);

    const commentCount = document.createElement("span");
    commentCount.className = "feed-action-count";
    commentCount.textContent = feed.commentCount || 0;

    commentAction.appendChild(commentIcon);
    commentAction.appendChild(commentCount);

    // 댓글 클릭 핸들러
    commentAction.addEventListener("click", () => handleCommentClick(feed.feedId, article));

    // 좋아요/댓글 아이콘을 그룹화
    const actionItemsGroup = document.createElement("div");
    actionItemsGroup.className = "feed-action-items-group";
    actionItemsGroup.appendChild(likeAction);
    actionItemsGroup.appendChild(commentAction);
    actions.appendChild(actionItemsGroup);

    // 자신의 게시물일 경우 수정/삭제 버튼 추가, 관리자는 삭제만 가능
    const currentUserId = localStorage.getItem("userId");
    const isMyPost = currentUserId && Number(currentUserId) === feed.userId;
    const isAdminUser = isAdmin();

    if (isMyPost || isAdminUser) {
        const editDeleteActions = document.createElement("div");
        editDeleteActions.className = "feed-edit-delete-actions";

        // 자신의 게시물일 경우에만 수정 버튼 표시
        if (isMyPost) {
            const editButton = document.createElement("button");
            editButton.className = "feed-edit-button";
            editButton.textContent = "수정";
            editButton.addEventListener("click", (e) => {
                e.stopPropagation();
                window.location.href = `/feed/update?feedId=${feed.feedId}`;
            });
            editDeleteActions.appendChild(editButton);
        }

        // 자신의 게시물이거나 관리자인 경우 삭제 버튼 표시
        const deleteButton = document.createElement("button");
        deleteButton.className = "feed-delete-button";
        deleteButton.textContent = "삭제";
        deleteButton.addEventListener("click", (e) => {
            e.stopPropagation();
            openDeleteModal(feed.feedId, article);
        });
        editDeleteActions.appendChild(deleteButton);
        actions.appendChild(editDeleteActions);
    }

    // 조립
    article.appendChild(header);
    article.appendChild(imageContainer);
    article.appendChild(courseTitle);
    article.appendChild(stats);
    article.appendChild(paceText);
    article.appendChild(content);
    article.appendChild(actions);

    // 댓글 영역 (초기에는 숨김) - actions 다음에 추가
    const commentsSection = createCommentsSection(feed.feedId);
    article.appendChild(commentsSection);

    return article;
}

/**
 * 댓글 영역 생성
 */
function createCommentsSection(feedId) {
    const section = document.createElement("div");
    section.className = "feed-comments-section";
    section.setAttribute("data-feed-id", feedId);

    const title = document.createElement("h3");
    title.className = "feed-comments-title";
    title.textContent = "댓글";

    const commentsList = document.createElement("div");
    commentsList.className = "feed-comments-list";
    commentsList.setAttribute("data-feed-id", feedId);

    const form = document.createElement("form");
    form.className = "feed-comment-form";
    form.setAttribute("data-feed-id", feedId);

    const input = document.createElement("input");
    input.type = "text";
    input.className = "feed-comment-input";
    input.placeholder = "내용을 입력하세요";
    input.maxLength = 100;

    const submit = document.createElement("button");
    submit.type = "submit";
    submit.className = "feed-comment-submit";
    submit.textContent = "등록";

    form.appendChild(input);
    form.appendChild(submit);

    // 댓글 등록 핸들러
    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const content = input.value.trim();
        if (!content) return;

        await submitComment(feedId, content, commentsList);
        input.value = "";
    });

    section.appendChild(title);
    section.appendChild(commentsList);
    section.appendChild(form);

    return section;
}

/**
 * 좋아요 클릭 핸들러
 */
async function handleLikeClick(feedId, likeAction, likeCountElement) {
    const token = localStorage.getItem("accessToken");
    if (!token) {
        window.location.href = "/login";
        return;
    }

    const isLiked = feedLikes.get(feedId) || false;
    const url = `/api/feed/${feedId}/like`;

    try {
        let response;
        if (isLiked) {
            // 좋아요 취소
            response = await fetch(url, {
                method: "DELETE",
                headers: {Authorization: `Bearer ${token}`}
            });
        } else {
            // 좋아요 추가
            response = await fetch(url, {
                method: "POST",
                headers: {Authorization: `Bearer ${token}`}
            });
        }

        if (!response.ok) {
            if (response.status === 401) {
                window.location.href = "/login";
                return;
            }
            throw new Error("좋아요 처리 실패");
        }

        // 상태 업데이트
        feedLikes.set(feedId, !isLiked);
        const newCount = parseInt(likeCountElement.textContent) + (isLiked ? -1 : 1);
        likeCountElement.textContent = Math.max(0, newCount);

        // 아이콘 스타일 업데이트
        const icon = likeAction.querySelector("svg");
        const path = likeAction.querySelector("path");
        if (icon && path) {
            if (!isLiked) {
                // 좋아요 활성화: 채워진 하트
                icon.setAttribute("fill", "currentColor");
                icon.removeAttribute("stroke");
                path.setAttribute("fill", "currentColor");
                path.removeAttribute("stroke");
                path.setAttribute("fill-rule", "evenodd");
                path.setAttribute("d", "M8 1.314C12.438-3.248 23.534 4.735 8 15-7.534 4.736 3.562-3.248 8 1.314");
            } else {
                // 좋아요 비활성화: 빈 하트
                icon.setAttribute("fill", "none");
                icon.setAttribute("stroke", "currentColor");
                path.setAttribute("fill", "none");
                path.setAttribute("stroke", "currentColor");
                path.removeAttribute("fill-rule");
                path.setAttribute("d", "m8 2.748-.717-.737C5.6.281 2.514.878 1.4 3.053c-.523 1.023-.641 2.5.314 4.385.92 1.815 2.834 3.989 6.286 6.357 3.452-2.368 5.365-4.542 6.286-6.357.955-1.886.838-3.362.314-4.385C13.486.878 10.4.28 8.717 2.01zM8 15C-7.333 4.868 3.279-3.04 7.824 1.143q.09.083.176.171a3 3 0 0 1 .176-.17C12.72-3.042 23.333 4.867 8 15");
            }
        }

    } catch (error) {
        console.error("좋아요 처리 실패:", error);
        alert("좋아요 처리 중 오류가 발생했습니다.");
    }
}

/**
 * 댓글 클릭 핸들러
 */
async function handleCommentClick(feedId, feedCard) {
    const commentsSection = feedCard.querySelector(`.feed-comments-section[data-feed-id="${feedId}"]`);
    if (!commentsSection) return;

    const isOpen = openCommentSections.has(feedId);

    if (isOpen) {
        // 댓글 영역 닫기
        commentsSection.classList.remove("active");
        openCommentSections.delete(feedId);
    } else {
        // 댓글 영역 열기
        commentsSection.classList.add("active");
        openCommentSections.add(feedId);

        // 댓글 목록 로드
        await loadComments(feedId, commentsSection.querySelector(".feed-comments-list"));
    }
}

/**
 * 댓글 목록 로드
 */
async function loadComments(feedId, commentsList) {
    const token = localStorage.getItem("accessToken");
    if (!token) return;

    try {
        const response = await fetch(`/api/feed/${feedId}/comments?page=0&size=100&sort=createdAt,asc`, {
            headers: {Authorization: `Bearer ${token}`}
        });

        if (!response.ok) {
            if (response.status === 401) {
                window.location.href = "/login";
                return;
            }
            throw new Error("댓글 조회 실패");
        }

        const payload = await response.json();
        const pageData = payload?.data;
        const comments = pageData?.content || [];

        // 댓글 렌더링
        commentsList.innerHTML = "";
        comments.forEach(comment => {
            const commentItem = createCommentItem(comment, feedId);
            commentsList.appendChild(commentItem);
        });

    } catch (error) {
        console.error("댓글 로드 실패:", error);
    }
}

/**
 * 댓글 아이템 생성
 */
function createCommentItem(comment, feedId) {
    const item = document.createElement("div");
    item.className = "feed-comment-item";
    item.setAttribute("data-comment-id", comment.commentId);

    const profileImg = document.createElement("img");
    profileImg.className = "feed-comment-profile";
    profileImg.src = comment.profileImageUrl || '/img/default-profile.svg';
    profileImg.alt = comment.userLoginId;
    profileImg.style.cursor = "pointer";
    profileImg.onerror = function () {
        // 기본 이미지로 교체
        this.src = '/img/default-profile.svg';
    };
    // 프로필 이미지 클릭 시 프로필 페이지로 이동
    profileImg.addEventListener("click", () => {
        window.location.href = `/profile/${comment.userId}`;
    });

    const contentWrapper = document.createElement("div");
    contentWrapper.className = "feed-comment-content-wrapper";

    const userId = document.createElement("div");
    userId.className = "feed-comment-user-id";
    userId.textContent = comment.userLoginId || "-";

    const date = document.createElement("div");
    date.className = "feed-comment-date";
    date.textContent = formatDate(comment.createdAt);

    const text = document.createElement("div");
    text.className = "feed-comment-text";
    text.textContent = comment.content || "";

    contentWrapper.appendChild(userId);
    contentWrapper.appendChild(date);
    contentWrapper.appendChild(text);

    item.appendChild(profileImg);
    item.appendChild(contentWrapper);

    // 자신의 댓글이거나 관리자인 경우 삭제 버튼 추가
    const currentUserId = localStorage.getItem("userId");
    const isMyComment = currentUserId && Number(currentUserId) === comment.userId;
    const isAdminUser = isAdmin();

    if (isMyComment || isAdminUser) {
        const deleteButton = document.createElement("button");
        deleteButton.className = "feed-comment-delete-button";
        deleteButton.textContent = "삭제";
        deleteButton.addEventListener("click", async (e) => {
            e.stopPropagation();
            await deleteComment(feedId, comment.commentId, item);
        });
        item.appendChild(deleteButton);
    }

    return item;
}

/**
 * 댓글 등록
 */
async function submitComment(feedId, content, commentsList) {
    const token = localStorage.getItem("accessToken");
    if (!token) {
        window.location.href = "/login";
        return;
    }

    try {
        const response = await fetch(`/api/feed/${feedId}/comments`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({content})
        });

        if (!response.ok) {
            if (response.status === 401) {
                window.location.href = "/login";
                return;
            }
            throw new Error("댓글 등록 실패");
        }

        // 댓글 목록 다시 로드
        await loadComments(feedId, commentsList);

        // 댓글 개수 업데이트
        const feedCard = document.querySelector(`[data-feed-id="${feedId}"]`);
        if (feedCard) {
            const commentCountElement = feedCard.querySelector('[data-action="comment"] .feed-action-count');
            if (commentCountElement) {
                const currentCount = parseInt(commentCountElement.textContent) || 0;
                commentCountElement.textContent = currentCount + 1;
            }
        }
    } catch (error) {
        console.error("댓글 등록 실패:", error);
        alert("댓글 등록 중 오류가 발생했습니다.");
    }
}

/**
 * 댓글 삭제
 */
async function deleteComment(feedId, commentId, commentItem) {
    const token = localStorage.getItem("accessToken");
    if (!token) {
        window.location.href = "/login";
        return;
    }

    try {
        const response = await fetch(`/api/feed/${feedId}/comments/${commentId}`, {
            method: "DELETE",
            headers: {Authorization: `Bearer ${token}`}
        });

        if (!response.ok) {
            if (response.status === 401) {
                window.location.href = "/login";
                return;
            }
            throw new Error("댓글 삭제 실패");
        }

        // 댓글 아이템 제거
        if (commentItem) {
            commentItem.remove();
        }

        // 댓글 개수 업데이트
        const feedCard = document.querySelector(`[data-feed-id="${feedId}"]`);
        if (feedCard) {
            const commentCountElement = feedCard.querySelector('[data-action="comment"] .feed-action-count');
            if (commentCountElement) {
                const currentCount = parseInt(commentCountElement.textContent) || 0;
                commentCountElement.textContent = Math.max(0, currentCount - 1);
            }
        }

    } catch (error) {
        console.error("댓글 삭제 실패:", error);
        alert("댓글 삭제 중 오류가 발생했습니다.");
    }
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
    const paceValue = typeof pace === 'number' ? pace : parseFloat(pace);
    if (isNaN(paceValue)) return "-";
    const minutes = Math.floor(paceValue);
    const seconds = Math.floor((paceValue - minutes) * 60);
    return `${minutes}'${String(seconds).padStart(2, "0")}"`;
}

/**
 * 날짜 포맷팅
 */
function formatDate(dateString) {
    if (!dateString) return "";
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}.${month}.${day}`;
}

/**
 * 빈 상태 표시
 */
function showEmptyState() {
    const emptyState = document.getElementById("feedEmpty");
    const feedList = document.querySelector('[data-role="feed-list"]');

    if (emptyState) {
        emptyState.removeAttribute("hidden");
        emptyState.style.display = "flex";
    }
    if (feedList) {
        feedList.style.display = "none";
    }
}

/**
 * 빈 상태 숨김
 */
function hideEmptyState() {
    const emptyState = document.getElementById("feedEmpty");
    const feedList = document.querySelector('[data-role="feed-list"]');

    if (emptyState) {
        emptyState.setAttribute("hidden", "hidden");
        emptyState.style.display = "none";
    }
    if (feedList) {
        feedList.style.display = "flex";
    }
}

/**
 * 삭제 모달 열기
 */
function openDeleteModal(feedId, feedCard) {
    const modal = document.getElementById("deleteFeedModal");
    const cancelButton = document.getElementById("deleteCancelButton");
    const confirmButton = document.getElementById("deleteConfirmButton");

    if (!modal) return;

    modal.removeAttribute("hidden");
    modal.style.display = "flex";

    // 기존 이벤트 리스너 제거
    const newCancelButton = cancelButton.cloneNode(true);
    const newConfirmButton = confirmButton.cloneNode(true);
    cancelButton.parentNode.replaceChild(newCancelButton, cancelButton);
    confirmButton.parentNode.replaceChild(newConfirmButton, confirmButton);

    // 취소 버튼
    newCancelButton.addEventListener("click", () => {
        closeDeleteModal();
    });

    // 확인 버튼
    newConfirmButton.addEventListener("click", async () => {
        await deleteFeed(feedId, feedCard);
    });

    // 모달 배경 클릭 시 닫기
    modal.addEventListener("click", (e) => {
        if (e.target === modal) {
            closeDeleteModal();
        }
    });
}

/**
 * 삭제 모달 닫기
 */
function closeDeleteModal() {
    const modal = document.getElementById("deleteFeedModal");
    if (modal) {
        modal.setAttribute("hidden", "hidden");
        modal.style.display = "none";
    }
}

/**
 * 피드 삭제
 */
async function deleteFeed(feedId, feedCard) {
    const token = localStorage.getItem("accessToken");
    if (!token) {
        window.location.href = "/login";
        return;
    }

    const confirmButton = document.getElementById("deleteConfirmButton");
    if (confirmButton) {
        confirmButton.disabled = true;
        confirmButton.textContent = "삭제 중...";
    }

    try {
        const response = await fetch(`/api/feed/${feedId}`, {
            method: "DELETE",
            headers: {Authorization: `Bearer ${token}`}
        });

        if (!response.ok) {
            if (response.status === 401) {
                window.location.href = "/login";
                return;
            }
            throw new Error("피드 삭제 실패");
        }

        // 피드 카드 제거
        if (feedCard) {
            feedCard.remove();
        }

        // 빈 상태 확인
        const feedList = document.querySelector('[data-role="feed-list"]');
        if (feedList && feedList.children.length === 0) {
            showEmptyState();
        }

        closeDeleteModal();
        alert("피드가 삭제되었습니다.");

    } catch (error) {
        console.error("피드 삭제 실패:", error);
        alert("피드 삭제 중 오류가 발생했습니다.");
    } finally {
        if (confirmButton) {
            confirmButton.disabled = false;
            confirmButton.textContent = "삭제";
        }
    }
}

// feed 객체 기반으로 피드 카드에 표시할 타이틀(코스 주소/대체 타이틀)을 생성
function getFeedDisplayTitle(feed) {
    const baseTitle = (feed.courseTitle || '').trim();
    if (baseTitle) return baseTitle;

    const runningType = feed.runningType;

    if (runningType === 'GHOST') {
        return '고스트런';
    }

    if (runningType === 'ONLINEBATTLE') {
        const rank = (typeof feed.onlineBattleRanking === 'number')
            ? feed.onlineBattleRanking
            : (feed.onlineBattleRanking ? Number(feed.onlineBattleRanking) : null);

        return rank ? `온라인배틀 #${rank}` : '온라인배틀';
    }

    // 기타(코스 없는 경우 대비)
    return '러닝';
}

// ONLINEBATTLE 랭킹 캐시 (runningResultId -> ranking)
const onlineBattleRankingCache = new Map();

async function fetchOnlineBattleRanking(runningResultId) {
    if (!runningResultId) return null;
    if (onlineBattleRankingCache.has(runningResultId)) {
        return onlineBattleRankingCache.get(runningResultId);
    }

    const token = localStorage.getItem("accessToken");
    if (!token) return null;

    try {
        const res = await fetch(`/api/battle-result/running-results/${runningResultId}/ranking`, {
            headers: {Authorization: `Bearer ${token}`}
        });
        if (!res.ok) return null;

        const payload = await res.json();
        const ranking = payload?.data?.ranking;
        if (typeof ranking === 'number') {
            onlineBattleRankingCache.set(runningResultId, ranking);
            return ranking;
        }
        if (ranking !== undefined && ranking !== null) {
            const parsed = Number(ranking);
            if (!Number.isNaN(parsed)) {
                onlineBattleRankingCache.set(runningResultId, parsed);
                return parsed;
            }
        }
        return null;
    } catch (e) {
        console.warn('온라인배틀 랭킹 조회 실패:', e);
        return null;
    }
}
