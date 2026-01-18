document.addEventListener("DOMContentLoaded", () => {
    console.log("feed-update.js loaded");
    attachBackButtonHandler();
    attachFormSubmitHandler();
    loadFeedData();
});

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
 * URL에서 feedId 가져오기
 */
function getFeedId() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('feedId');
}

/**
 * 피드 데이터 로드
 */
async function loadFeedData() {
    const feedId = getFeedId();
    if (!feedId) {
        alert("피드 ID가 없습니다.");
        window.location.href = "/feed";
        return;
    }

    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            window.location.href = "/login";
            return;
        }

        const response = await fetch(`/api/feed/${feedId}`, {
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
        const feed = payload?.data;

        if (!feed) {
            throw new Error("피드 데이터가 없습니다.");
        }

        // 데이터 표시
        renderFeedData(feed);

    } catch (error) {
        console.error("피드 로드 실패:", error);
        alert("피드를 불러오는데 실패했습니다.");
        window.location.href = "/feed";
    }
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
        const parsed = (typeof ranking === 'number') ? ranking : (ranking != null ? Number(ranking) : null);
        if (parsed != null && !Number.isNaN(parsed)) {
            onlineBattleRankingCache.set(runningResultId, parsed);
            return parsed;
        }
        return null;
    } catch (e) {
        console.warn('온라인배틀 랭킹 조회 실패:', e);
        return null;
    }
}

/**
 * 피드 데이터 렌더링
 */
function renderFeedData(feed) {
    // 코스 제목
    const courseTitleLabel = document.getElementById("courseTitleLabel");
    if (courseTitleLabel) {
        courseTitleLabel.textContent = getFeedDisplayTitle(feed);

        // ONLINEBATTLE이고 코스 제목이 비어있으면 비동기 조회 후 제목 갱신
        if (feed.runningType === 'ONLINEBATTLE' && !(feed.courseTitle && String(feed.courseTitle).trim())) {
            fetchOnlineBattleRanking(feed.runningResultId).then((rank) => {
                if (rank && courseTitleLabel.isConnected) {
                    courseTitleLabel.textContent = `온라인배틀 #${rank}`;
                }
            });
        }
    }

    // 거리
    const distanceValue = document.getElementById("distanceValue");
    if (distanceValue && feed.totalDistance) {
        distanceValue.textContent = `${feed.totalDistance.toFixed(1)}km`;
    }

    // 시간
    const timeValue = document.getElementById("timeValue");
    if (timeValue && feed.totalTime) {
        timeValue.textContent = formatDuration(feed.totalTime);
    }

    // 평균 페이스
    const paceText = document.getElementById("paceText");
    if (paceText && feed.avgPace) {
        const paceStr = formatPace(feed.avgPace);
        paceText.textContent = `평균 페이스: ${paceStr}`;
    }

    // ✅ 이미지 최적화
    const previewImage = document.getElementById("previewImage");
    const imagePlaceholder = document.getElementById("imagePlaceholder");
    if (feed.imageUrl) {
        previewImage.decoding = "async";
        previewImage.loading = "lazy";
        previewImage.src = feed.imageUrl;
        previewImage.removeAttribute("hidden");
        if (imagePlaceholder) {
            imagePlaceholder.style.display = "none";
        }
    } else {
        if (imagePlaceholder) {
            imagePlaceholder.textContent = "이미지 없음";
        }
    }

    // 내용
    const contentInput = document.getElementById("contentInput");
    if (contentInput && feed.content) {
        contentInput.value = feed.content;
    }

    // feedId 저장 (폼 제출 시 사용)
    document.getElementById("feedPostForm").setAttribute("data-feed-id", feed.feedId);
}

/**
 * 피드 제목 가져오기
 */
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

    return '러닝';
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
 * 폼 제출 핸들러
 */
function attachFormSubmitHandler() {
    const form = document.getElementById("feedPostForm");
    const submitButton = document.getElementById("submitButton");

    if (!form || !submitButton) return;

    form.addEventListener("submit", async (e) => {
        e.preventDefault();

        const feedId = form.getAttribute("data-feed-id");
        if (!feedId) {
            alert("피드 ID가 없습니다.");
            return;
        }

        const contentInput = document.getElementById("contentInput");
        const content = contentInput.value.trim();

        if (!content) {
            alert("내용을 입력해주세요.");
            contentInput.focus();
            return;
        }

        if (content.length > 500) {
            alert(`피드 내용은 최대 500자까지 작성할 수 있습니다. (현재 ${content.length}자)`);
            contentInput.focus();
            return;
        }

        // 버튼 비활성화
        submitButton.disabled = true;
        submitButton.textContent = "수정 중...";

        try {
            const token = localStorage.getItem("accessToken");
            if (!token) {
                window.location.href = "/login";
                return;
            }

            const response = await fetch(`/api/feed/${feedId}`, {
                method: "PUT",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    content: content
                })
            });

            if (!response.ok) {
                if (response.status === 401) {
                    window.location.href = "/login";
                    return;
                }
                const error = await response.json();
                throw new Error(error?.message || "피드 수정 실패");
            }

            const result = await response.json();
            if (result.success) {
                alert("피드가 수정되었습니다!");
                window.location.href = "/feed";
            } else {
                throw new Error(result.message || "피드 수정 실패");
            }

        } catch (error) {
            console.error("피드 수정 실패:", error);
            alert(error.message || "피드 수정 중 오류가 발생했습니다.");
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = "수정하기";
        }
    });
}
