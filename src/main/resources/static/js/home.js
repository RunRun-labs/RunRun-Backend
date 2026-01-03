// src/main/resources/static/js/home.js
document.addEventListener("DOMContentLoaded", () => {
    const bottomNavMount = document.getElementById("bottomNavMount");
    const bottomNavTemplate = document.getElementById("bottomNavTemplate");
    const notificationButton = document.querySelector('[data-role="notification-button"]');

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

    loadNotificationCount();
    loadTodayStats();
    initWeekSelector();
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

// 주 선택 관련 전역 변수
let currentWeekOffset = 0; // 0 = 이번 주, -1 = 지난 주, 1 = 다음 주 등

/**
 * 주 선택 기능 초기화
 */
function initWeekSelector() {
    const prevBtn = document.querySelector('[data-role="week-prev"]');
    const nextBtn = document.querySelector('[data-role="week-next"]');

    if (prevBtn) {
        prevBtn.addEventListener("click", () => {
            if (currentWeekOffset > -3) { // 최대 한달 전까지 (4주)
                currentWeekOffset--;
                updateWeekLabel();
                loadWeeklyStats();
            }
        });
    }

    if (nextBtn) {
        nextBtn.addEventListener("click", () => {
            if (currentWeekOffset < 0) { // 현재 주까지만 (미래 주는 불가)
                currentWeekOffset++;
                updateWeekLabel();
                loadWeeklyStats();
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
    const weekEnd = new Date(weekStart);
    weekEnd.setDate(weekStart.getDate() + 6);

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

async function loadWeeklyStats() {
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken) {
        renderWeeklyChart([]);
        updateWeeklyTotals(0, 0);
        return;
    }

    try {
        // TODO: 실제 API 연동 시 주별 데이터 조회
        const weeklyData = [0, 0, 0, 0, 0, 0, 0];
        renderWeeklyChart(weeklyData);
        
        // 주별 총 거리와 시간 계산
        const totalDistance = weeklyData.reduce((sum, dist) => sum + dist, 0);
        const totalDuration = 0; // TODO: 실제 API에서 시간 데이터 가져오기
        
        updateWeeklyTotals(totalDistance, totalDuration);
    } catch (error) {
        console.error("주간 통계 로드 실패:", error);
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
        distanceEl.textContent = `${distance.toFixed(1)}km`;
    }

    if (durationEl) {
        // durationSeconds를 시간:분 형식으로 변환
        const hours = Math.floor(durationSeconds / 3600);
        const minutes = Math.floor((durationSeconds % 3600) / 60);
        
        if (hours > 0) {
            durationEl.textContent = `${hours}h ${minutes}m`;
        } else {
            durationEl.textContent = `${minutes}m`;
        }
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
            bar.style.height = "24.631px"; // 최소 높이
            chartBars.appendChild(bar);
        }
        return;
    }

    const maxDistance = Math.max(...distances, 0.1);
    distances.forEach((distance) => {
        const bar = document.createElement("div");
        bar.className = "chart-bar";
        const heightRatio = distance / maxDistance;
        // 최소 높이: 약 24.631px, 최대 높이: 약 98.539px (Figma 디자인 기준)
        const minHeight = 24.631;
        const maxHeight = 98.539;
        const height = Math.max(minHeight, minHeight + (maxHeight - minHeight) * heightRatio);
        bar.style.height = `${height}px`;
        chartBars.appendChild(bar);
    });
}

let challengeSlideInterval = null;
let currentSlideIndex = 0;
let allChallenges = []; // 모든 챌린지 목록 저장

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

        // 모든 챌린지 저장 (네비게이션용)
        allChallenges = sortedChallenges;
        currentSlideIndex = 0;
        
        // 모든 챌린지 렌더링
        renderChallengeBanner(sortedChallenges);
        
        // 네비게이션 버튼 이벤트 리스너 추가
        initChallengeNavigation();

    } catch (error) {
        console.error("챌린지 로드 실패:", error);
        renderChallengeBanner([]);
    }
}

function renderChallengeBanner(challenges) {
    const slider = document.querySelector('[data-role="challenge-banner-slider"]');
    const container = document.querySelector('[data-role="challenge-banner-container"]');
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
        
        // 화살표 버튼 숨기기
        const prevBtn = document.querySelector('[data-role="challenge-prev"]');
        const nextBtn = document.querySelector('[data-role="challenge-next"]');
        if (prevBtn) prevBtn.style.display = "none";
        if (nextBtn) nextBtn.style.display = "none";
        return;
    }
    
    // 화살표 버튼 표시 (챌린지가 1개 이상일 때만)
    const prevBtn = document.querySelector('[data-role="challenge-prev"]');
    const nextBtn = document.querySelector('[data-role="challenge-next"]');
    if (challenges.length > 1) {
        if (prevBtn) prevBtn.style.display = "flex";
        if (nextBtn) nextBtn.style.display = "flex";
    } else {
        if (prevBtn) prevBtn.style.display = "none";
        if (nextBtn) nextBtn.style.display = "none";
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
                <button class="challenge-banner-button" type="button">상세보기</button>
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

/**
 * 챌린지 네비게이션 초기화
 */
function initChallengeNavigation() {
    const prevBtn = document.querySelector('[data-role="challenge-prev"]');
    const nextBtn = document.querySelector('[data-role="challenge-next"]');
    
    if (prevBtn) {
        prevBtn.addEventListener("click", (e) => {
            e.stopPropagation();
            navigateChallenge(-1);
        });
    }
    
    if (nextBtn) {
        nextBtn.addEventListener("click", (e) => {
            e.stopPropagation();
            navigateChallenge(1);
        });
    }
}

/**
 * 챌린지 네비게이션 (이전/다음)
 */
function navigateChallenge(direction) {
    if (allChallenges.length === 0) return;
    
    // 자동 슬라이드 중지
    if (challengeSlideInterval) {
        clearInterval(challengeSlideInterval);
        challengeSlideInterval = null;
    }
    
    // 인덱스 업데이트
    currentSlideIndex += direction;
    
    // 순환 처리
    if (currentSlideIndex < 0) {
        currentSlideIndex = allChallenges.length - 1;
    } else if (currentSlideIndex >= allChallenges.length) {
        currentSlideIndex = 0;
    }
    
    // 슬라이더 이동
    const slider = document.querySelector('[data-role="challenge-banner-slider"]');
    if (slider) {
        slider.style.transform = `translateX(-${currentSlideIndex * 100}%)`;
    }
    
    // 5초 후 자동 슬라이드 재개
    setTimeout(() => {
        if (allChallenges.length > 1) {
            startChallengeSlide(allChallenges.length);
        }
    }, 5000);
}

function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
}