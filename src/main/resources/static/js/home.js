// src/main/resources/static/js/home.js
document.addEventListener("DOMContentLoaded", () => {
    const bottomNavMount = document.getElementById("bottomNavMount");
    const bottomNavTemplate = document.getElementById("bottomNavTemplate");
    const notificationButton = document.querySelector('[data-role="notification-button"]');
    const runningStatsButton = document.querySelector('[data-role="running-stats-button"]');
    const courseFindButton = document.querySelector('[data-role="course-find-button"]');

    if (
        bottomNavMount &&
        bottomNavMount.childElementCount === 0 &&
        bottomNavTemplate
    ) {
        const clone = bottomNavTemplate.content.firstElementChild.cloneNode(true);
        bottomNavMount.replaceWith(clone);
    }

    if (notificationButton) {
        notificationButton.addEventListener("click", () => {
            window.location.href = "/notification";
        });
    }

    if (runningStatsButton) {
        runningStatsButton.addEventListener("click", () => {
            console.log("러닝 통계 페이지로 이동 (추후 구현)");
        });
    }

    if (courseFindButton) {
        courseFindButton.addEventListener("click", () => {
            window.location.href = "/course";
        });
    }

    loadNotificationCount();
    loadTodayStats();
    loadWeeklyStats();
    loadLatestChallengeForBanner();
});

async function loadNotificationCount() {
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken) return;
    try {
        // 추후 구현
    } catch (error) {
        console.error("알림 개수 로드 실패:", error);
    }
}

function updateNotificationBadge(count) {
    const badge = document.querySelector('[data-role="notification-badge"]');
    if (!badge) return;
    if (count > 0) {
        badge.textContent = count > 99 ? "99+" : count.toString();
        badge.hidden = false;
    } else {
        badge.hidden = true;
    }
}

async function loadTodayStats() {
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken) {
        return;
    }
    try {
        const userResponse = await fetch("/users", {
            headers: {Authorization: `Bearer ${accessToken}`},
        });

        let userWeight = 70;
        if (userResponse.ok) {
            const userData = await userResponse.json();
            userWeight = userData?.data?.weightKg || 70;
        }

        const todayStats = {
            distance: 0.0,
            duration: 0,
            calories: 0,
        };

        if (todayStats.distance > 0) {
            todayStats.calories = Math.round(todayStats.distance * userWeight * 1.036);
        }

        renderTodayStats(todayStats);
    } catch (error) {
        console.error("오늘의 통계 로드 실패:", error);
    }
}

function renderTodayStats(stats) {
    const distanceEl = document.querySelector('[data-role="today-distance"]');
    const durationEl = document.querySelector('[data-role="today-duration"]');
    const caloriesEl = document.querySelector('[data-role="today-calories"]');

    if (distanceEl) {
        distanceEl.innerHTML = `${stats.distance.toFixed(1)}<span class="stat-unit">Km</span>`;
    }

    if (durationEl) {
        durationEl.textContent = formatDuration(stats.duration);
    }

    if (caloriesEl) {
        caloriesEl.innerHTML = `${stats.calories}<span class="stat-unit">kcal</span>`;
    }
}

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

async function loadWeeklyStats() {
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken) {
        renderWeeklyChart([]);
        return;
    }

    try {
        const weeklyData = [0, 0, 0, 0, 0, 0, 0];
        renderWeeklyChart(weeklyData);
    } catch (error) {
        console.error("주간 통계 로드 실패:", error);
        renderWeeklyChart([]);
    }
}

function getStartOfWeek(date) {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    return new Date(d.setDate(diff));
}

function renderWeeklyChart(distances) {
    const chartBars = document.querySelector('[data-role="chart-bars"]');
    if (!chartBars) return;
    chartBars.innerHTML = "";
    if (!Array.isArray(distances) || distances.length === 0) {
        for (let i = 0; i < 7; i++) {
            const bar = document.createElement("div");
            bar.className = "chart-bar";
            bar.style.height = "13px";
            chartBars.appendChild(bar);
        }
        return;
    }

    const maxDistance = Math.max(...distances, 0.1);
    distances.forEach((distance) => {
        const bar = document.createElement("div");
        bar.className = "chart-bar";
        const heightRatio = distance / maxDistance;
        const height = Math.max(13, 20 + 60 * heightRatio);
        bar.style.height = `${height}px`;
        chartBars.appendChild(bar);
    });
}

let challengeSlideInterval = null;
let currentSlideIndex = 0;

/**
 * 챌린지 배너 로드 (수정됨)
 * - 생성일(createdAt)이 없으면 ID를 기준으로 내림차순 정렬하여 최신 챌린지 선택
 */
async function loadLatestChallengeForBanner() {
    const accessToken = localStorage.getItem("accessToken");

    try {
        const response = await fetch("/challenges", {
            headers: accessToken ? {Authorization: `Bearer ${accessToken}`} : {},
        });

        if (response.status === 401) {
            renderChallengeBanner([]);
            return;
        }

        if (!response.ok) {
            throw new Error("챌린지 목록을 불러오지 못했습니다.");
        }

        const payload = await response.json().catch(() => null);
        const challenges = payload?.data ?? payload ?? [];

        if (!Array.isArray(challenges) || challenges.length === 0) {
            renderChallengeBanner([]);
            return;
        }

        // 최신순 정렬 (ID 내림차순을 기본으로 사용, createdAt이 있다면 그것을 우선)
        const sortedChallenges = challenges.sort((a, b) => {
            // createdAt 필드가 존재한다면 날짜 비교 (내림차순)
            const timeA = new Date(a.createdAt || a.created_at || 0).getTime();
            const timeB = new Date(b.createdAt || b.created_at || 0).getTime();
            if (timeA !== timeB && timeA > 0 && timeB > 0) {
                return timeB - timeA;
            }
            // 날짜 정보가 없으면 ID 비교 (내림차순 = 최신순)
            return (b.id || 0) - (a.id || 0);
        });

        // 가장 최신 챌린지 1개 선택
        const newest = sortedChallenges[0];
        renderChallengeBanner([newest]);

    } catch (error) {
        console.error("챌린지 로드 실패:", error);
        renderChallengeBanner([]);
    }
}

function renderChallengeBanner(challenges) {
    const slider = document.querySelector('[data-role="challenge-banner-slider"]');
    if (!slider) return;

    slider.innerHTML = "";

    if (!Array.isArray(challenges) || challenges.length === 0) {
        const emptySlide = document.createElement("div");
        emptySlide.className = "challenge-banner-slide";
        emptySlide.innerHTML = `
            <div style="width: 100%; height: 100%; background: #eef2f4; display: flex; align-items: center; justify-content: center; color: var(--text-muted);">
                진행중인 챌린지가 없습니다.
            </div>
        `;
        slider.appendChild(emptySlide);
        return;
    }

    challenges.forEach((challenge) => {
        const slide = document.createElement("div");
        slide.className = "challenge-banner-slide";

        slide.innerHTML = `
            <img
                class="challenge-banner-image"
                src="${challenge.imageUrl || "/img/default-challenge.png"}"
                alt="${challenge.title || "챌린지 이미지"}"
                onerror="this.src='/img/default-challenge.png'"
            />
            <div class="challenge-banner-overlay">
                <h3 class="challenge-banner-title">${escapeHtml(challenge.title || "챌린지 제목")}</h3>
                <p class="challenge-banner-desc">${escapeHtml(challenge.description || "")}</p>
                <button class="challenge-banner-button" type="button">참여하기</button>
            </div>
        `;

        slide.addEventListener("click", () => {
            window.location.href = `/challenge/${challenge.id}`;
        });

        slider.appendChild(slide);
    });

    if (challenges.length > 1) {
        startChallengeSlide(challenges.length);
    } else {
        // 단일 배너일 경우 슬라이드 위치 초기화 및 인터벌 제거
        const sliderEl = document.querySelector('[data-role="challenge-banner-slider"]');
        if (sliderEl) sliderEl.style.transform = "translateX(0)";
        if (challengeSlideInterval) {
            clearInterval(challengeSlideInterval);
            challengeSlideInterval = null;
        }
    }
}

function startChallengeSlide(totalSlides) {
    if (challengeSlideInterval) {
        clearInterval(challengeSlideInterval);
    }

    currentSlideIndex = 0;
    const slider = document.querySelector('[data-role="challenge-banner-slider"]');
    if (!slider) return;

    challengeSlideInterval = setInterval(() => {
        currentSlideIndex = (currentSlideIndex + 1) % totalSlides;
        slider.style.transform = `translateX(-${currentSlideIndex * 100}%)`;
    }, 3000);
}

function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
}