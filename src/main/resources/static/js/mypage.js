document.addEventListener("DOMContentLoaded", () => {
    console.log("mypage.js loaded");
    attachProfileEditHandler();
    attachProfileImageClickHandler();
    attachChallengeHandler();
    attachFriendHandler();
    attachCouponsHandler();
    attachSettingsHandler();
    attachMyCoursesHandler();
    attachMyPostsHandler();
    loadMyBodyInfo();

    // 초기 로드 시 빈 상태 숨김
    hideEmptyState();

    // 실제 스크롤이 발생하는 컨테이너를 기준으로 무한 스크롤 동작
    initInfiniteScroll();

    // '스크롤(또는 휠/터치) 입력' 이후에만 다음 페이지 로드
    attachUserScrollGate();

    loadRunningRecords(0, true); // 초기 로드 (첫 페이지, 초기화)
});

async function loadMyBodyInfo() {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) return;

        const res = await fetch("/users", {
            headers: {"Authorization": `Bearer ${token}`}
        });

        if (!res.ok) throw new Error("조회 실패");

        const payload = await res.json();
        const user = payload?.data ?? null;
        renderTierAndRating(user);
        renderProfileImage(user);
    } catch (e) {
        console.error(e);
    }
}

function renderTierAndRating(user) {
    const tierImage = document.getElementById("tierImage");
    const tierText = document.getElementById("tierText");
    const ratingValue = document.getElementById("ratingValue");

    // 티어 정보 (추후 API 연동 예정)
    const tier = user?.tier || "토끼";
    const tierImagePath = `/img/tier/${tier.toLowerCase()}.png`;
    
    // 티어 이미지 설정
    if (tierImage) {
        tierImage.src = tierImagePath;
        tierImage.alt = tier;
        // 이미지 로드 실패 시 텍스트 표시
        tierImage.onerror = function() {
            this.style.display = "none";
            if (tierText) {
                tierText.style.display = "inline";
            }
        };
        // 이미지 로드 성공 시 텍스트 숨김
        tierImage.onload = function() {
            if (tierText) {
                tierText.style.display = "none";
            }
        };
    }
    
    // 티어 텍스트 설정 (이미지가 없을 경우 대체)
    if (tierText) {
        const tierEmojiMap = {
            "토끼": "🐰",
            "rabbit": "🐰"
        };
        tierText.textContent = tierEmojiMap[tier] || "🐰";
    }

    // 레이팅 정보 (추후 API 연동 예정)
    const rating = user?.rating || null;
    if (ratingValue) {
        ratingValue.textContent = rating !== null ? rating : "-";
    }
}


function attachProfileImageClickHandler() {
    const container =
        document.querySelector('[data-role="avatar-click"]') ||
        document.querySelector(".profile-avatar");

    if (!container) return;


    container.style.cursor = "pointer";
    container.setAttribute("role", "button");
    container.setAttribute("tabindex", "0");

    const goEdit = () => {
        window.location.href = "/myPage/edit";
    };

    container.addEventListener("click", goEdit);


}

function attachProfileEditHandler() {
    const profileSettingsBtn = document.querySelector('[data-role="profile-settings"]');
    if (!profileSettingsBtn) return;

    profileSettingsBtn.addEventListener("click", () => {
        window.location.href = "/myPage/edit";
    });
}

function attachChallengeHandler() {
    const challengeBtn = document.querySelector('[data-role="challenge"]');
    const modal = document.querySelector('[data-role="challenge-modal"]');
    const modalOverlay = document.querySelector('[data-role="challenge-modal-overlay"]');
    const modalClose = document.querySelector('[data-role="challenge-modal-close"]');
    const challengeOptions = document.querySelectorAll('[data-role="challenge-option"]');

    if (!challengeBtn || !modal) return;

    // 모달 열기
    challengeBtn.addEventListener("click", () => {
        modal.classList.add("active");
        document.body.style.overflow = "hidden";
    });

    // 모달 닫기
    const closeModal = () => {
        modal.classList.remove("active");
        document.body.style.overflow = "";
    };

    if (modalOverlay) {
        modalOverlay.addEventListener("click", closeModal);
    }

    if (modalClose) {
        modalClose.addEventListener("click", closeModal);
    }

    // ESC 키로 모달 닫기
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && modal.classList.contains("active")) {
            closeModal();
        }
    });

    // 챌린지 옵션 클릭 처리
    challengeOptions.forEach((option) => {
        option.addEventListener("click", () => {
            const type = option.getAttribute("data-type");
            let url = "/challenge";

            switch (type) {
                case "active":
                    url = "/challenge";
                    break;
                case "ended":
                    url = "/challenge/end";
                    break;
            }

            window.location.href = url;
        });
    });
}

function attachFriendHandler() {
    const friendBtn = document.querySelector('[data-role="friends"]');
    if (!friendBtn) return;

    friendBtn.addEventListener("click", () => {
        window.location.href = "/friends/list";
    });
}

function attachCouponsHandler() {
    const couponsBtn = document.querySelector('[data-role="coupons"]');
    if (!couponsBtn) return;

    couponsBtn.addEventListener("click", () => {
        window.location.href = "/coupon/my";
    });
}

function attachSettingsHandler() {
    const settingsBtn = document.querySelector('[data-role="settings"]');
    if (!settingsBtn) return;

    settingsBtn.addEventListener("click", () => {
        window.location.href = "/setting";
    });
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
            initialEl.textContent = "";
            initialEl.hidden = true;
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
            initialEl.textContent = "";
            initialEl.hidden = true;
        }
    }, {once: true});
}

function attachMyCoursesHandler() {
    const myCoursesBtn = document.querySelector('[data-role="my-courses"]');
    const modal = document.querySelector('[data-role="course-modal"]');
    const modalOverlay = document.querySelector('[data-role="course-modal-overlay"]');
    const modalClose = document.querySelector('[data-role="course-modal-close"]');
    const courseOptions = document.querySelectorAll('[data-role="course-option"]');

    if (!myCoursesBtn || !modal) return;

    // 모달 열기
    myCoursesBtn.addEventListener("click", () => {
        modal.classList.add("active");
        document.body.style.overflow = "hidden";
    });

    // 모달 닫기
    const closeModal = () => {
        modal.classList.remove("active");
        document.body.style.overflow = "";
    };

    if (modalOverlay) {
        modalOverlay.addEventListener("click", closeModal);
    }

    if (modalClose) {
        modalClose.addEventListener("click", closeModal);
    }

    // ESC 키로 모달 닫기
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && modal.classList.contains("active")) {
            closeModal();
        }
    });

    // 코스 옵션 클릭 처리
    courseOptions.forEach((option) => {
        option.addEventListener("click", () => {
            const type = option.getAttribute("data-type");
            let url = "/course";

            // 타입에 따라 쿼리 파라미터 추가 (나중에 필터 기능 구현 시 사용)
            switch (type) {
                case "liked":
                    url = "/course?filter=liked";
                    break;
                case "favorited":
                    url = "/course?filter=favorited";
                    break;
                case "my":
                    url = "/course?filter=my";
                    break;
            }

            window.location.href = url;
        });
    });
}

function attachMyPostsHandler() {
    const myPostsBtn = document.querySelector('[data-role="my-posts"]');
    if (!myPostsBtn) return;

    myPostsBtn.addEventListener("click", () => {
        // 피드 기능 구현 후 연동 예정
        alert("피드 기능 구현 후 연동 예정입니다.");
    });
}

/**
 * 러닝 타입을 한국어로 변환
 * RunningType enum 참고: SOLO("솔로"), OFFLINE("오프라인"), ONLINEBATTLE("온라인배틀"), GHOST("고스트")
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

// 러닝 기록 무한 스크롤 관련 전역 변수
let currentPage = 0;
let hasNext = true;
let isLoading = false;
let userHasInteracted = false; // 사용자가 실제로 스크롤을 했는지
let scrollObserver = null; // IntersectionObserver 인스턴스

/**
 * 러닝 기록 로드 (API 연동)
 */
async function loadRunningRecords(page = 0, reset = false) {
    if (isLoading || (!hasNext && !reset)) return;

    isLoading = true;
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            isLoading = false;
            return;
        }

        const res = await fetch(`/api/records/me?page=${page}&size=4&sort=startedAt,desc`, {
            headers: {Authorization: `Bearer ${token}`}
        });

        if (!res.ok) throw new Error("러닝 기록 조회 실패");

        const payload = await res.json();
        const sliceData = payload?.data;

        if (!sliceData) {
            isLoading = false;
            return;
        }

        const records = sliceData.content || [];
        // Page 객체의 last 속성 사용
        hasNext = !(sliceData.last ?? true);
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
            showEmptyState();
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
function showEmptyState() {
    const emptyState = document.getElementById("runListEmpty");
    const runList = document.querySelector('[data-role="run-list"]');
    if (emptyState) {
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
 * 러닝 기록 카드 생성
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

    // 코스 이미지 URL
    const imageUrl = record.courseThumbnailUrl || null;
    const courseTitle = record.courseTitle || '러닝';

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
                <span class="run-type">${getRunningTypeLabel(record.runningType)}</span>
            </div>
            <p class="run-title">${courseTitle}</p>
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
            <button class="run-share" type="button">피드에 공유</button>
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
    // pace는 BigDecimal로 분/km 단위
    const minutes = Math.floor(pace);
    const seconds = Math.floor((pace - minutes) * 60);
    return `${minutes}'${String(seconds).padStart(2, "0")}"`;
}

/**
 * 무한 스크롤 초기화
 */
function initInfiniteScroll() {
    // CSS 구조상 .mypage-page에 height 제한이 없어 body 스크롤이 발생할 가능성이 높음
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
            loadRunningRecords(currentPage + 1, false);
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
function attachUserScrollGate() {
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
        // 초기 로드 시 센티널이 이미 화면 안에 있을 수 있으므로, 인터랙션 후에 관찰 시작
        const sentinel = document.getElementById("scrollSentinel");
        if (sentinel && scrollObserver) {
            requestAnimationFrame(() => {
                const sentinelEl = document.getElementById("scrollSentinel");
                if (!sentinelEl || !scrollObserver) return;

                // 센티널 관찰 시작
                scrollObserver.observe(sentinelEl);
                console.log("Sentinel observed after user interaction, hasNext:", hasNext);

                // 인터랙션 감지 시점에 센티넬이 이미 화면 안에 있다면 즉시 로드
                // (Observer는 이미 교차 중인 상태에서는 콜백을 다시 호출하지 않기 때문)
                if (hasNext && !isLoading) {
                    const rect = sentinelEl.getBoundingClientRect();
                    // rootMargin(200px)과 동일하게 여유를 둠
                    if (rect.top <= window.innerHeight + 200) {
                        console.log("Sentinel already visible upon interaction, loading next page:", currentPage + 1);
                        loadRunningRecords(currentPage + 1, false);
                    }
                }
            });
        }
    };

    // window와 .mypage-page 모두에 이벤트 등록 (어디서 스크롤이 발생하든 감지)
    // scroll 뿐만 아니라 wheel, touchmove도 감지하여 사용자 의도를 파악
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