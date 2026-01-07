document.addEventListener("DOMContentLoaded", () => {
    console.log("myrun-list.js loaded");
    attachBackButtonHandler();
    initInfiniteScroll();
    attachUserScrollGate();
    loadRunningRecords(0, true); // 초기 로드 (첫 페이지, 초기화)
});

// 러닝 기록 무한 스크롤 관련 전역 변수
let currentPage = 0;
let hasNext = true;
let isLoading = false;
let userHasInteracted = false;
let scrollObserver = null;

/**
 * 뒤로가기 버튼 핸들러
 */
function attachBackButtonHandler() {
    const backButton = document.querySelector('[data-role="back-button"]');
    if (backButton) {
        backButton.addEventListener("click", () => {
            window.history.back();
        });
    }
}

/**
 * 러닝 타입을 한국어로 변환
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
 * 러닝 기록 로드 (API 연동) - 공유되지 않은 기록만
 */
async function loadRunningRecords(page = 0, reset = false) {
    if (isLoading || (!hasNext && !reset)) return;

    isLoading = true;
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            isLoading = false;
            window.location.href = "/login";
            return;
        }

        const res = await fetch(`/api/records/unshared?page=${page}&size=10&sort=startedAt,desc`, {
            headers: {Authorization: `Bearer ${token}`}
        });

        if (!res.ok) {
            if (res.status === 401) {
                window.location.href = "/login";
                return;
            }
            throw new Error("러닝 기록 조회 실패");
        }

        const payload = await res.json();
        const sliceData = payload?.data;

        if (!sliceData) {
            isLoading = false;
            return;
        }

        const records = sliceData.content || [];
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
        }

        // 무한 스크롤 업데이트
        updateScrollSentinel();

    } catch (e) {
        console.error("러닝 기록 로드 실패:", e);
        if (reset && currentPage === 0) {
            showEmptyState();
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

    // 코스 이미지 URL
    const imageUrl = record.courseThumbnailUrl || null;
    const courseTitle = record.courseTitle || '러닝';

    // 이미지가 있을 때만 img 태그 추가
    const thumbContent = imageUrl
        ? `<img src="${imageUrl}" alt="${courseTitle}" style="display: block;" onerror="this.style.display='none'" />`
        : '';

    // 주소 정보 (courseTitle에서 추출하거나 별도 필드 사용)
    const address = courseTitle;

    article.innerHTML = `
        <div class="run-thumb">
            ${thumbContent}
        </div>
        <div class="run-content">
            <div class="run-header">
                <div class="run-date-location">
                    <span class="run-date">${formattedDate}</span>
                    <span class="run-location">${address}</span>
                </div>
                <span class="run-type">${getRunningTypeLabel(record.runningType)}</span>
            </div>
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
            <button class="run-select-button" type="button" data-record-id="${record.runningResultId}">선택</button>
        </div>
    `;

    // 선택 버튼 클릭 이벤트
    const selectBtn = article.querySelector('.run-select-button');
    if (selectBtn) {
        selectBtn.addEventListener('click', () => {
            const recordId = selectBtn.getAttribute('data-record-id');
            // 추후 구현: 입력 폼으로 이동
            // window.location.href = `/feed/create?runningResultId=${recordId}`;
            alert(`선택된 러닝 기록 ID: ${recordId}\n(입력 폼 페이지는 추후 구현 예정)`);
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
 * 무한 스크롤 초기화
 */
function initInfiniteScroll() {
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

    runList.appendChild(sentinel);

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

        window.removeEventListener('scroll', markInteracted);
        window.removeEventListener('wheel', markInteracted);
        window.removeEventListener('touchmove', markInteracted);

        const page = document.querySelector('.myrun-list-page');
        if (page) {
            page.removeEventListener('scroll', markInteracted);
            page.removeEventListener('wheel', markInteracted);
            page.removeEventListener('touchmove', markInteracted);
        }

        const sentinel = document.getElementById("scrollSentinel");
        if (sentinel && scrollObserver) {
            requestAnimationFrame(() => {
                const sentinelEl = document.getElementById("scrollSentinel");
                if (!sentinelEl || !scrollObserver) return;

                scrollObserver.observe(sentinelEl);
                console.log("Sentinel observed after user interaction, hasNext:", hasNext);

                if (hasNext && !isLoading) {
                    const rect = sentinelEl.getBoundingClientRect();
                    if (rect.top <= window.innerHeight + 200) {
                        console.log("Sentinel already visible upon interaction, loading next page:", currentPage + 1);
                        loadRunningRecords(currentPage + 1, false);
                    }
                }
            });
        }
    };

    window.addEventListener('scroll', markInteracted, {passive: true});
    window.addEventListener('wheel', markInteracted, {passive: true});
    window.addEventListener('touchmove', markInteracted, {passive: true});

    const page = document.querySelector('.myrun-list-page');
    if (page) {
        page.addEventListener('scroll', markInteracted, {passive: true});
        page.addEventListener('wheel', markInteracted, {passive: true});
        page.addEventListener('touchmove', markInteracted, {passive: true});
    }
}

