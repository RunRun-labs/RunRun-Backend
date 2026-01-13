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
    attachImageModalHandlers();
    attachDeleteRecordModalHandlers();
    attachCalendarModalHandlers();
    attachTierRatingModalHandlers();
    attachPointClickHandler();
    loadMyBodyInfo();
    loadPointBalance();

    // 초기 로드 시 빈 상태 숨김
    hideEmptyState();

    // 실제 스크롤이 발생하는 컨테이너를 기준으로 무한 스크롤 동작
    initInfiniteScroll();

    // '스크롤(또는 휠/터치) 입력' 이후에만 다음 페이지 로드
    attachUserScrollGate();

    loadRunningRecords(0, true); // 초기 로드 (첫 페이지, 초기화)

    // 달력 모달 초기화
    initCalendarModal();
});

async function loadMyBodyInfo() {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) return;

        // 1. 기본 유저 정보 조회
        const res = await fetch("/users", {
            headers: {"Authorization": `Bearer ${token}`}
        });

        if (!res.ok) throw new Error("조회 실패");

        const payload = await res.json();
        const user = payload?.data ?? null;

        // 2. 레이팅 정보 조회 (추가)
        if (user) {
            try {
                // 거리별 레이팅 조회를 위해 기본값 KM_3 사용
                const targetDistanceType = "KM_3";

                const rateRes = await fetch(`/api/rating/distance?distanceType=${targetDistanceType}`, {
                    headers: {"Authorization": `Bearer ${token}`}
                });

                if (rateRes.ok) {
                    const ratePayload = await rateRes.json();
                    const rateData = ratePayload.data;

                    if (rateData) {
                        // user 객체에 레이팅 및 티어 정보 병합
                        user.rating = rateData.currentRating;
                        user.tierName = rateData.currentTier; // 예: "RABBIT", "TURTLE" 등
                    }
                }
            } catch (rateError) {
                console.warn("레이팅 정보 로드 실패 (기본값 표시):", rateError);
            }
        }

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

    // 티어 정보 (API에서 받은 tierName 사용, 없으면 기본값)
    // 백엔드에서 한글 티어 이름이 올 수 있음: 거북이, 토끼, 사슴, 표범, 호랑이, 장산범
    const tier = user?.tierName || "거북이"; // 기본값 거북이

    // 티어 이모지 매핑 (한글 이름 기준)
    const tierEmojiMap = {
        "거북이": "🐢",
        "토끼": "🐇",
        "사슴": "🦌",
        "표범": "🐆",
        "호랑이": "🐅",
        "장산범": "🫅"
    };

    // 티어 텍스트 설정
    if (tierText) {
        const emoji = tierEmojiMap[tier] || "🐢";
        tierText.textContent = emoji;
        tierText.setAttribute("title", tier);
        // 이미지를 사용하지 않으므로 텍스트(이모지)를 항상 표시하고 폰트 크기를 키움
        tierText.style.display = "inline";
        tierText.style.fontSize = "2rem";
    }

    // 티어 이미지 설정 (사용하지 않음, 숨김 처리)
    if (tierImage) {
        tierImage.style.display = "none";
        tierImage.src = ""; // 불필요한 네트워크 요청 방지
    } else {
        if (tierText) {
            tierText.style.fontSize = "2rem";
        }
    }

    // 레이팅 정보
    const rating = user?.rating;
    if (ratingValue) {
        ratingValue.textContent = rating !== undefined && rating !== null
            ? Math.floor(rating).toLocaleString()
            : "-";
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

    // 버튼 활성화 (disabled 속성 제거)
    myPostsBtn.disabled = false;
    myPostsBtn.style.cursor = "pointer";
    myPostsBtn.style.opacity = "1";

    myPostsBtn.addEventListener("click", () => {
        // localStorage에 "내 글" 탭 활성화 플래그 설정
        localStorage.setItem("feedSortToMy", "true");
        window.location.href = "/feed";
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

/**
 * 러닝 상태를 한국어로 변환
 * RunStatus enum 참고: COMPLETED("완료"), TIME_OUT("타임아웃"), GIVE_UP("포기"), IN_PROGRESS("진행중"), CANCELLED("취소")
 */
function getRunStatusLabel(runStatus) {
    const statusMap = {
        COMPLETED: "완료",
        TIME_OUT: "타임아웃",
        GIVE_UP: "포기",
        IN_PROGRESS: "진행중",
        CANCELLED: "취소"
    };
    return statusMap[runStatus] || runStatus || "-";
}

// 러닝 기록 무한 스크롤 관련 전역 변수
let currentPage = 0;
let hasNext = true;
let isLoading = false;
let userHasInteracted = false; // 사용자가 실제로 스크롤을 했는지
let scrollObserver = null; // IntersectionObserver 인스턴스
let selectedDate = null; // 선택된 날짜 (YYYY-MM-DD 형식)
let allRecordsDates = new Set(); // 로드된 모든 기록의 날짜 목록 (YYYY-MM-DD 형식)

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

        // 날짜 필터 계산: 기본적으로 최근 7일만 조회 (선택된 날짜가 없을 때)
        let url = `/api/records/me?page=${page}&size=4&sort=startedAt,desc`;

        if (selectedDate) {
            // 선택된 날짜가 있으면 해당 날짜만 조회
            url += `&startDate=${selectedDate}&endDate=${selectedDate}`;
        } else if (reset && page === 0) {
            // 초기 로드이고 날짜 선택이 없으면 최근 7일만 조회
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

        if (!res.ok) throw new Error("러닝 기록 조회 실패");

        const payload = await res.json();
        const sliceData = payload?.data;

        if (!sliceData) {
            isLoading = false;
            return;
        }

        const records = sliceData.content || [];

        // 기록의 날짜를 allRecordsDates에 추가 (달력 표시용)
        records.forEach(record => {
            if (record.startedAt) {
                const dateStr = formatDateForAPI(new Date(record.startedAt));
                allRecordsDates.add(dateStr);
            }
        });

        // Page 객체의 last 속성 사용
        hasNext = !(sliceData.last ?? true);
        currentPage = page;

        if (reset) {
            const runList = document.querySelector('[data-role="run-list"]');
            if (runList) runList.innerHTML = "";
            if (!selectedDate) {
                // 날짜 필터가 없을 때만 날짜 목록 초기화
                allRecordsDates.clear();
            }
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
 * 날짜를 API 형식으로 포맷팅 (YYYY-MM-DD)
 */
function formatDateForAPI(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
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

        // 빈 상태 메시지 설정
        const emptyTextSmall = emptyState.querySelector(".empty-text-small");
        if (emptyTextSmall) {
            if (selectedDate) {
                // 선택된 날짜가 있으면 해당 날짜 메시지
                const dateObj = new Date(selectedDate);
                const month = dateObj.getMonth() + 1;
                const day = dateObj.getDate();
                emptyTextSmall.textContent = `${month}월 ${day}일 러닝 기록이 없어요`;
            } else {
                // 선택된 날짜가 없으면 기본 메시지
                emptyTextSmall.textContent = "이번 주 러닝 기록이 없어요";
            }
        }
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

    const defaultOnlineBattleImageUrl = '/img/online-1st.png'; // fallback (이미지 자산이 1~4만 있는 상태)

    // 썸네일 URL 우선순위:
    // 1) 고스트런: 고정 이미지
    // 2) 온라인배틀: 등수별 이미지
    // 3) 일반: courseThumbnailUrl
    const imageUrl = isGhostRun
        ? defaultGhostImageUrl
        : (isOnlineBattle
            ? (onlineBattleRankImageMap[onlineBattleRanking] || defaultOnlineBattleImageUrl)
            : (record.courseThumbnailUrl || null));

    // ✅ 제목 결정 (우선순위: 고스트런 > 온라인배틀 > 일반)
    const courseTitle = isGhostRun
        ? '고스트런'
        : (isOnlineBattle ? '온라인배틀' : (record.courseTitle || '러닝'));

    const titleSuffix = (!isGhostRun && isOnlineBattle && onlineBattleRanking)
        ? ` <span class="run-title-rank">#${onlineBattleRanking}</span>`
        : '';

    // 러닝 상태 확인
    const runStatus = record.runStatus || 'COMPLETED';
    const statusLabel = getRunStatusLabel(runStatus);
    const isCompleted = runStatus === 'COMPLETED';
    const canShare = isCompleted; // COMPLETED 상태만 공유 가능

    // 이미지가 있을 때만 img 태그 추가
    const thumbContent = imageUrl
        ? `<img src="${imageUrl}" alt="${courseTitle}" style="display: block; cursor: pointer;" onerror="this.style.display='none'" data-image-url="${imageUrl}" />`
        : '';

    article.innerHTML = `
        <div class="run-thumb">
            ${thumbContent}
        </div>
        <div class="run-content">
            <div class="run-header">
                <span class="run-date">${formattedDate}</span>
                <div class="run-header-right">
                    <span class="run-type">${getRunningTypeLabel(record.runningType)}</span>
                    <span class="run-status-badge run-status-${runStatus.toLowerCase().replace('_', '-')}">${statusLabel}</span>
                </div>
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
            <div class="run-actions">
                <button class="run-share" type="button" ${!canShare ? 'disabled' : ''} ${!canShare ? 'style="opacity: 0.5; cursor: not-allowed;"' : ''}>공유</button>
                <button class="run-delete" type="button" data-record-id="${record.runningResultId}">삭제</button>
            </div>
        </div>
    `;

    // 썸네일 클릭 이벤트 추가
    if (imageUrl) {
        const thumbContainer = article.querySelector('.run-thumb');
        if (thumbContainer) {
            thumbContainer.addEventListener('click', () => {
                openImageModal(imageUrl);
            });
        }
    }

    // 삭제 버튼 클릭 이벤트 추가
    const deleteBtn = article.querySelector('.run-delete');
    if (deleteBtn) {
        deleteBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            const recordId = deleteBtn.getAttribute('data-record-id');
            if (recordId) {
                openDeleteRecordModal(Number(recordId), article);
            }
        });
    }

    // 공유 버튼 클릭 이벤트 추가 (COMPLETED 상태만 가능)
    const shareBtn = article.querySelector('.run-share');
    if (shareBtn && canShare) {
        shareBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            const recordId = record.runningResultId;
            if (recordId) {
                window.location.href = `/feed/post?runningResultId=${recordId}`;
            }
        });
    } else if (shareBtn && !canShare) {
        // 공유 불가능한 상태일 때 클릭 이벤트는 추가하지 않음 (disabled 상태)
        shareBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            e.preventDefault();
        });
    }

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

/**
 * 이미지 모달 핸들러
 */
function attachImageModalHandlers() {
    const modal = document.getElementById("imageModal");
    const closeBtn = document.querySelector('[data-role="close-image-modal"]');
    const modalOverlay = document.querySelector('.image-modal-overlay');

    if (!modal) return;

    // 닫기 버튼
    if (closeBtn) {
        closeBtn.addEventListener("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            closeImageModal();
        });
    }

    // 배경 클릭 시 닫기
    if (modalOverlay) {
        modalOverlay.addEventListener("click", (e) => {
            if (e.target === modalOverlay) {
                closeImageModal();
            }
        });

        // 모달 콘텐츠 클릭 시 닫히지 않도록
        const modalContent = modalOverlay.querySelector(".image-modal-content");
        if (modalContent) {
            modalContent.addEventListener("click", (e) => {
                e.stopPropagation();
            });
        }
    }

    // ESC 키로 닫기
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && !modal.hasAttribute("hidden")) {
            closeImageModal();
        }

        // 삭제 모달 ESC 키로 닫기
        const deleteModal = document.getElementById("deleteRecordModal");
        if (e.key === "Escape" && deleteModal && !deleteModal.hasAttribute("hidden")) {
            closeDeleteRecordModal();
        }
    });
}

/**
 * 이미지 모달 열기
 */
function openImageModal(imageUrl) {
    const modal = document.getElementById("imageModal");
    const modalImg = document.getElementById("imageModalImg");

    if (!modal || !modalImg) return;

    modalImg.src = imageUrl;
    modal.removeAttribute("hidden");
    document.body.style.overflow = "hidden";
}

/**
 * 이미지 모달 닫기
 */
function closeImageModal() {
    const modal = document.getElementById("imageModal");
    const modalImg = document.getElementById("imageModalImg");

    if (!modal) return;

    modal.setAttribute("hidden", "hidden");
    document.body.style.overflow = "";

    // 이미지 소스 제거 (메모리 절약)
    if (modalImg) {
        modalImg.src = "";
    }
}

/**
 * 삭제 확인 모달 핸들러
 */
function attachDeleteRecordModalHandlers() {
    const modal = document.getElementById("deleteRecordModal");
    const closeBtn = document.querySelector('[data-role="close-delete-modal"]');
    const cancelBtn = document.querySelector('[data-role="cancel-delete-record"]');
    const confirmBtn = document.querySelector('[data-role="confirm-delete-record"]');
    const modalOverlay = document.querySelector('#deleteRecordModal');

    if (!modal) return;

    // 닫기 버튼
    if (closeBtn) {
        closeBtn.addEventListener("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            closeDeleteRecordModal();
        });
    }

    // 취소 버튼
    if (cancelBtn) {
        cancelBtn.addEventListener("click", () => {
            closeDeleteRecordModal();
        });
    }

    // 확인 버튼
    if (confirmBtn) {
        confirmBtn.addEventListener("click", async () => {
            const recordId = confirmBtn.getAttribute('data-record-id');

            if (recordId) {
                // recordId로 카드 찾기
                const recordElement = document.querySelector(`[data-record-id="${recordId}"]`)?.closest('.run-card');
                await deleteRunningRecord(Number(recordId), recordElement);
            }
        });
    }

    // 배경 클릭 시 닫기
    if (modalOverlay) {
        modalOverlay.addEventListener("click", (e) => {
            if (e.target === modalOverlay) {
                closeDeleteRecordModal();
            }
        });

        // 모달 콘텐츠 클릭 시 닫히지 않도록
        const modalContent = modalOverlay.querySelector(".modal-content");
        if (modalContent) {
            modalContent.addEventListener("click", (e) => {
                e.stopPropagation();
            });
        }
    }
}

/**
 * 삭제 확인 모달 열기
 */
function openDeleteRecordModal(recordId, recordElement) {
    const modal = document.getElementById("deleteRecordModal");
    const confirmBtn = document.querySelector('[data-role="confirm-delete-record"]');

    if (!modal) return;

    // 확인 버튼에 recordId와 element 정보 저장
    if (confirmBtn) {
        confirmBtn.setAttribute('data-record-id', recordId);
        // recordElement를 직접 저장할 수 없으므로, recordId로 나중에 찾을 수 있도록 함
        if (recordElement) {
            recordElement.setAttribute('data-delete-target', 'true');
        }
    }

    modal.removeAttribute("hidden");
    document.body.style.overflow = "hidden";
}

/**
 * 삭제 확인 모달 닫기
 */
function closeDeleteRecordModal() {
    const modal = document.getElementById("deleteRecordModal");
    const confirmBtn = document.querySelector('[data-role="confirm-delete-record"]');

    if (!modal) return;

    modal.setAttribute("hidden", "hidden");
    document.body.style.overflow = "";

    // 저장된 데이터 제거 및 버튼 상태 초기화
    if (confirmBtn) {
        const recordId = confirmBtn.getAttribute('data-record-id');
        if (recordId) {
            const recordElement = document.querySelector(`[data-record-id="${recordId}"]`)?.closest('.run-card');
            if (recordElement) {
                recordElement.removeAttribute('data-delete-target');
            }
        }
        confirmBtn.removeAttribute('data-record-id');
        // 버튼 상태 초기화
        confirmBtn.disabled = false;
        confirmBtn.textContent = "삭제";
    }
}

// 달력 모달 관련 전역 변수
let currentCalendarYear = new Date().getFullYear();
let currentCalendarMonth = new Date().getMonth(); // 0-11
let calendarRecordsDates = new Set(); // 달력에 표시할 기록이 있는 날짜 목록

/**
 * 달력 모달 핸들러
 */
function attachCalendarModalHandlers() {
    const dateSearchButton = document.getElementById("dateSearchButton");
    const calendarModal = document.getElementById("calendarModal");
    const closeBtn = document.querySelector('[data-role="close-calendar-modal"]');
    const modalOverlay = document.querySelector('.calendar-modal-overlay');
    const resetButton = document.getElementById("calendarResetButton");

    if (!dateSearchButton || !calendarModal) return;

    // 날짜 검색 버튼 클릭 시 달력 모달 열기
    dateSearchButton.addEventListener("click", () => {
        openCalendarModal();
    });

    // 닫기 버튼
    if (closeBtn) {
        closeBtn.addEventListener("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            closeCalendarModal();
        });
    }

    // 초기화 버튼
    if (resetButton) {
        resetButton.addEventListener("click", () => {
            selectedDate = null;
            updateDateSearchLabel();
            closeCalendarModal();
            currentPage = 0;
            hasNext = true;
            allRecordsDates.clear();
            loadRunningRecords(0, true);
        });
    }

    // 배경 클릭 시 닫기
    if (modalOverlay) {
        modalOverlay.addEventListener("click", (e) => {
            if (e.target === modalOverlay) {
                closeCalendarModal();
            }
        });

        // 모달 콘텐츠 클릭 시 닫히지 않도록
        const modalContent = modalOverlay.querySelector(".calendar-modal-content");
        if (modalContent) {
            modalContent.addEventListener("click", (e) => {
                e.stopPropagation();
            });
        }
    }

    // ESC 키로 닫기 (이벤트 리스너는 한 번만 추가되도록)
    if (!window.calendarModalEscHandler) {
        window.calendarModalEscHandler = (e) => {
            const calendarModal = document.getElementById("calendarModal");
            if (e.key === "Escape" && calendarModal && !calendarModal.hasAttribute("hidden")) {
                closeCalendarModal();
            }
        };
        document.addEventListener("keydown", window.calendarModalEscHandler);
    }
}

/**
 * 달력 모달 열기
 */
async function openCalendarModal() {
    const modal = document.getElementById("calendarModal");
    if (!modal) return;

    // 현재 달력 년/월로 초기화
    const today = new Date();
    currentCalendarYear = today.getFullYear();
    currentCalendarMonth = today.getMonth();

    // 달력 렌더링 (내부에서 기록 날짜 로드)
    await renderCalendar();

    modal.removeAttribute("hidden");
    document.body.style.overflow = "hidden";
}

/**
 * 달력 모달 닫기
 */
function closeCalendarModal() {
    const modal = document.getElementById("calendarModal");
    if (!modal) return;

    modal.setAttribute("hidden", "hidden");
    document.body.style.overflow = "";
}

/**
 * 달력 모달 초기화 (월 선택 버튼 등)
 */
function initCalendarModal() {
    const prevButton = document.getElementById("calendarPrevMonth");
    const nextButton = document.getElementById("calendarNextMonth");
    const calendarDays = document.getElementById("calendarDays");

    if (!prevButton || !nextButton || !calendarDays) return;

    // 이전 달 버튼
    prevButton.addEventListener("click", () => {
        currentCalendarMonth--;
        if (currentCalendarMonth < 0) {
            currentCalendarMonth = 11;
            currentCalendarYear--;
        }
        renderCalendar();
    });

    // 다음 달 버튼
    nextButton.addEventListener("click", () => {
        currentCalendarMonth++;
        if (currentCalendarMonth > 11) {
            currentCalendarMonth = 0;
            currentCalendarYear++;
        }
        renderCalendar();
    });
}

/**
 * 달력에 표시할 기록이 있는 날짜 목록 로드 (현재 년/월 기준)
 */
async function loadCalendarRecordsDates() {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) return;

        // 현재 달의 첫 날과 마지막 날 계산
        const firstDay = new Date(currentCalendarYear, currentCalendarMonth, 1);
        const lastDay = new Date(currentCalendarYear, currentCalendarMonth + 1, 0);

        const startDateStr = formatDateForAPI(firstDay);
        const endDateStr = formatDateForAPI(lastDay);

        // 해당 월의 모든 기록 조회 (페이지네이션 없이 큰 사이즈로)
        const res = await fetch(`/api/records/me?page=0&size=1000&sort=startedAt,desc&startDate=${startDateStr}&endDate=${endDateStr}`, {
            headers: {Authorization: `Bearer ${token}`}
        });

        if (!res.ok) throw new Error("러닝 기록 조회 실패");

        const payload = await res.json();
        const sliceData = payload?.data;
        const records = sliceData?.content || [];

        // 기록이 있는 날짜를 Set에 추가
        calendarRecordsDates.clear();
        records.forEach(record => {
            if (record.startedAt) {
                const dateStr = formatDateForAPI(new Date(record.startedAt));
                calendarRecordsDates.add(dateStr);
                // allRecordsDates에도 추가 (다른 곳에서 사용할 수 있음)
                allRecordsDates.add(dateStr);
            }
        });

    } catch (error) {
        console.error("달력 기록 날짜 로드 실패:", error);
        calendarRecordsDates.clear();
    }
}

/**
 * 달력 렌더링
 */
async function renderCalendar() {
    const calendarDays = document.getElementById("calendarDays");
    const calendarMonthYear = document.getElementById("calendarMonthYear");

    if (!calendarDays || !calendarMonthYear) return;

    // 년/월 표시 업데이트
    const monthNames = ["1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월"];
    calendarMonthYear.textContent = `${currentCalendarYear}년 ${monthNames[currentCalendarMonth]}`;

    // 해당 월의 기록이 있는 날짜 목록 로드
    await loadCalendarRecordsDates();

    // 달력 그리기
    const firstDay = new Date(currentCalendarYear, currentCalendarMonth, 1);
    const lastDay = new Date(currentCalendarYear, currentCalendarMonth + 1, 0);
    const daysInMonth = lastDay.getDate();
    const startingDayOfWeek = firstDay.getDay(); // 0 = 일요일

    calendarDays.innerHTML = "";

    // 이전 달의 마지막 날들 표시 (첫 주를 채우기 위해)
    if (startingDayOfWeek > 0) {
        const prevMonthLastDay = new Date(currentCalendarYear, currentCalendarMonth, 0).getDate();
        for (let i = startingDayOfWeek - 1; i >= 0; i--) {
            const day = prevMonthLastDay - i;
            const dayElement = createCalendarDay(day, true, false);
            calendarDays.appendChild(dayElement);
        }
    }

    // 현재 달의 날짜들 표시
    const today = new Date();
    const todayStr = formatDateForAPI(today);

    for (let day = 1; day <= daysInMonth; day++) {
        const dateStr = formatDateForAPI(new Date(currentCalendarYear, currentCalendarMonth, day));
        const isToday = dateStr === todayStr;
        const isSelected = selectedDate === dateStr;
        const hasRecord = calendarRecordsDates.has(dateStr);
        const isDisabled = dateStr > todayStr; // 미래 날짜는 비활성화

        const dayElement = createCalendarDay(day, false, isDisabled, isToday, isSelected, hasRecord, dateStr);
        calendarDays.appendChild(dayElement);
    }

    // 다음 달의 첫 날들 표시 (달력을 꽉 채우기 위해, 6주로 고정)
    const totalCells = startingDayOfWeek + daysInMonth;
    const remainingCells = 42 - totalCells; // 6주 * 7일 = 42
    if (remainingCells > 0) {
        for (let day = 1; day <= remainingCells; day++) {
            const dayElement = createCalendarDay(day, true, false);
            calendarDays.appendChild(dayElement);
        }
    }
}

/**
 * 달력 날짜 요소 생성
 */
function createCalendarDay(day, isOtherMonth, isDisabled, isToday = false, isSelected = false, hasRecord = false, dateStr = null) {
    const dayElement = document.createElement("div");
    dayElement.className = "calendar-day";
    dayElement.textContent = day;

    if (isOtherMonth) {
        dayElement.classList.add("calendar-day-other-month");
    }

    if (isDisabled) {
        dayElement.classList.add("calendar-day-disabled");
    } else if (!isOtherMonth && dateStr) {
        if (isToday) {
            dayElement.classList.add("calendar-day-today");
        }
        if (isSelected) {
            dayElement.classList.add("calendar-day-selected");
        }
        if (hasRecord) {
            dayElement.classList.add("calendar-day-has-record");
        }

        // 날짜 클릭 이벤트
        dayElement.addEventListener("click", () => {
            if (!isDisabled && !isOtherMonth) {
                selectDate(dateStr);
            }
        });
    }

    return dayElement;
}

/**
 * 날짜 선택
 */
function selectDate(dateStr) {
    if (!dateStr) return;

    selectedDate = dateStr;
    updateDateSearchLabel();
    closeCalendarModal();

    // 선택된 날짜의 기록 로드
    currentPage = 0;
    hasNext = true;
    allRecordsDates.clear();
    userHasInteracted = false; // 날짜 선택 시 스크롤 인터랙션 리셋
    loadRunningRecords(0, true);
}

/**
 * 날짜 검색 버튼 라벨 업데이트
 */
function updateDateSearchLabel() {
    const label = document.getElementById("dateSearchLabel");
    if (!label) return;

    if (selectedDate) {
        const dateObj = new Date(selectedDate);
        const month = dateObj.getMonth() + 1;
        const day = dateObj.getDate();
        label.textContent = `${month}/${day}`;
    } else {
        label.textContent = "날짜 검색";
    }
}

/**
 * 러닝 기록 삭제
 */
async function deleteRunningRecord(recordId, recordElement) {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            alert("로그인이 필요합니다.");
            window.location.href = "/login";
            return;
        }

        const confirmBtn = document.querySelector('[data-role="confirm-delete-record"]');
        if (confirmBtn) {
            confirmBtn.disabled = true;
            confirmBtn.textContent = "삭제 중...";
        }

        const response = await fetch(`/api/records/${recordId}`, {
            method: "DELETE",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!response.ok) {
            if (response.status === 401) {
                alert("로그인이 필요합니다.");
                window.location.href = "/login";
                return;
            }
            const error = await response.json();
            throw new Error(error?.message || "러닝 기록 삭제 실패");
        }

        // 성공 시 카드 제거
        if (recordElement) {
            recordElement.remove();

            // 기록이 없으면 빈 상태 표시
            const runList = document.querySelector('[data-role="run-list"]');
            if (runList && runList.children.length === 0) {
                showEmptyState();
            }
        }

        // 버튼 상태 초기화 후 모달 닫기
        if (confirmBtn) {
            confirmBtn.disabled = false;
            confirmBtn.textContent = "삭제";
        }

        closeDeleteRecordModal();
        alert("러닝 기록이 삭제되었습니다.");
    } catch (error) {
        console.error("Failed to delete running record:", error);
        alert(error.message || "러닝 기록 삭제 중 오류가 발생했습니다.");

        const confirmBtn = document.querySelector('[data-role="confirm-delete-record"]');
        if (confirmBtn) {
            confirmBtn.disabled = false;
            confirmBtn.textContent = "삭제";
        }
    }
}

/**
 * 티어/레이팅 모달 핸들러
 */
function attachTierRatingModalHandlers() {
    // tier-display div를 클릭 대상으로 설정
    const tierClickAreas = document.querySelectorAll('[data-role="tier-click"]');
    const modal = document.getElementById("tierRatingModal");
    const modalOverlay = document.querySelector('[data-role="tier-rating-modal-overlay"]');
    const modalClose = document.querySelector('[data-role="tier-rating-modal-close"]');

    console.log("티어 모달 핸들러 초기화:", {
        tierClickAreas: tierClickAreas.length,
        modal: !!modal,
        modalOverlay: !!modalOverlay,
        modalClose: !!modalClose
    });

    if (!modal) {
        console.warn("티어 상세 모달 요소를 찾을 수 없습니다.");
        return;
    }

    // 티어 클릭 시 모달 열기 (모든 클릭 영역에 이벤트 리스너 추가)
    if (tierClickAreas.length > 0) {
        tierClickAreas.forEach(area => {
            area.addEventListener("click", (e) => {
                console.log("티어 클릭 이벤트 발생");
                // 이벤트 버블링 방지 (필요한 경우)
                e.stopPropagation();
                e.preventDefault();
                openTierRatingModal();
            });
            // 커서 스타일 명시적 지정
            area.style.cursor = "pointer";
            console.log("티어 클릭 이벤트 리스너 등록 완료");
        });
    } else {
        console.warn("티어 클릭 영역(data-role='tier-click')을 찾을 수 없습니다.");
    }

    // 모달 닫기
    const closeModal = () => {
        modal.classList.remove("active");
        modal.setAttribute("hidden", "hidden");
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
}

/**
 * 티어/레이팅 모달 열기
 */
async function openTierRatingModal() {
    const modal = document.getElementById("tierRatingModal");
    const ratingList = document.getElementById("tierRatingList");

    if (!modal || !ratingList) {
        console.warn("티어 모달 또는 리스트 요소를 찾을 수 없습니다.");
        return;
    }

    console.log("티어 모달 열기 시도");

    // hidden 속성 제거 및 active 클래스 추가
    modal.removeAttribute("hidden");
    modal.classList.add("active");
    document.body.style.overflow = "hidden";

    // 로딩 표시
    ratingList.innerHTML = '<div style="padding: 20px; text-align: center; color: #999;"><p>로딩 중...</p></div>';

    // 모든 거리별 레이팅 조회
    await loadAllDistanceRatings();
}

/**
 * 모든 거리별 레이팅 조회 및 렌더링
 */
async function loadAllDistanceRatings() {
    const token = localStorage.getItem("accessToken");
    if (!token) return;

    const distanceTypes = ["KM_3", "KM_5", "KM_10"];
    const distanceLabels = {
        "KM_3": "3km",
        "KM_5": "5km",
        "KM_10": "10km"
    };

    const ratingList = document.getElementById("tierRatingList");
    if (!ratingList) return;

    try {
        // 모든 거리별 레이팅을 병렬로 조회
        const ratingPromises = distanceTypes.map(async (distanceType) => {
            try {
                const res = await fetch(`/api/rating/distance?distanceType=${distanceType}`, {
                    headers: {"Authorization": `Bearer ${token}`}
                });

                if (res.ok) {
                    const payload = await res.json();
                    return {
                        distanceType,
                        distanceLabel: distanceLabels[distanceType],
                        rating: payload.data
                    };
                }
                return {
                    distanceType,
                    distanceLabel: distanceLabels[distanceType],
                    rating: null
                };
            } catch (error) {
                console.error(`레이팅 조회 실패 (${distanceType}):`, error);
                return {
                    distanceType,
                    distanceLabel: distanceLabels[distanceType],
                    rating: null
                };
            }
        });

        const results = await Promise.all(ratingPromises);

        // 모달 렌더링
        renderTierRatingModal(results);
    } catch (error) {
        console.error("거리별 레이팅 조회 실패:", error);
        ratingList.innerHTML = '<div style="padding: 20px; text-align: center; color: #ff3b30;"><p>레이팅 조회에 실패했습니다.</p></div>';
    }
}

/**
 * 티어/레이팅 모달 렌더링
 */
function renderTierRatingModal(results) {
    const ratingList = document.getElementById("tierRatingList");
    if (!ratingList) return;

    // 티어 이모지 매핑 (한글 이름 기준)
    const tierEmojiMap = {
        "거북이": "🐢",
        "토끼": "🐇",
        "사슴": "🦌",
        "표범": "🐆",
        "호랑이": "🐅",
        "장산범": "🫅"
    };

    ratingList.innerHTML = "";

    results.forEach(result => {
        const {distanceLabel, rating} = result;

        const ratingCard = document.createElement("div");
        ratingCard.className = "course-modal-option";
        ratingCard.style.cursor = "default";

        const tierName = rating?.currentTier || "거북이";
        const tierRating = rating?.currentRating || 1000;
        const emoji = tierEmojiMap[tierName] || "🐢";

        ratingCard.innerHTML = `
            <div style="flex: 1; display: flex; flex-direction: column; gap: 8px;">
                <div style="display: flex; align-items: center; gap: 8px;">
                    <span class="option-icon" style="font-size: 1.5rem;">${emoji}</span>
                    <span class="option-text" style="font-weight: 600;">${distanceLabel}</span>
                </div>
                <div style="display: flex; flex-direction: column; gap: 4px; margin-left: 2rem;">
                    <div style="font-size: 0.9rem; color: #666;">
                        <span style="font-weight: 500;">티어:</span> ${tierName}
                    </div>
                    <div style="font-size: 0.9rem; color: #666;">
                        <span style="font-weight: 500;">레이팅:</span> ${Math.floor(tierRating).toLocaleString()}
                    </div>
                </div>
            </div>
        `;

        ratingList.appendChild(ratingCard);
    });
}

/**
 * 포인트 클릭 핸들러
 */
function attachPointClickHandler() {
    const pointSection = document.querySelector('[data-role="point-click"]');
    if (!pointSection) return;

    pointSection.addEventListener("click", () => {
        window.location.href = "/point";
    });
}

/**
 * 포인트 잔액 조회
 */
async function loadPointBalance() {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) return;

        const response = await fetch("/api/points", {
            headers: {"Authorization": `Bearer ${token}`}
        });

        if (!response.ok) {
            console.warn("포인트 조회 실패:", response.status);
            return;
        }

        const payload = await response.json();
        const pointData = payload?.data;

        if (pointData) {
            const availablePoints = pointData.availablePoints || 0;
            renderPointBalance(availablePoints);
        }
    } catch (error) {
        console.error("포인트 조회 중 오류:", error);
    }
}

/**
 * 포인트 잔액 렌더링
 */
function renderPointBalance(availablePoints) {
    const pointValueEl = document.getElementById("pointValue");
    if (pointValueEl) {
        pointValueEl.textContent = availablePoints !== undefined && availablePoints !== null
            ? availablePoints.toLocaleString()
            : "-";
    }
}