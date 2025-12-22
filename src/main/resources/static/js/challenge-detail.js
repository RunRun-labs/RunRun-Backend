document.addEventListener("DOMContentLoaded", () => {
    const backButton = document.querySelector(".back-button");
    const bottomNavMount = document.getElementById("bottomNavMount");
    const bottomNavTemplate = document.getElementById("bottomNavTemplate");
    const joinButton = document.querySelector('[data-role="join-button"]');
    const cancelButton = document.querySelector('[data-role="cancel-button"]');
    const progressArea = document.querySelector('[data-role="progress-area"]');

    // 뒤로가기 버튼 클릭 이벤트
    if (backButton) {
        backButton.addEventListener("click", () => {
            window.location.href = "/challenge";
        });
    }

    // Static fallback: use inline template when Thymeleaf include is not processed
    if (
        bottomNavMount &&
        bottomNavMount.childElementCount === 0 &&
        bottomNavTemplate
    ) {
        const clone = bottomNavTemplate.content.firstElementChild.cloneNode(true);
        bottomNavMount.replaceWith(clone);
    }

    // URL에서 챌린지 ID 추출
    const challengeId = extractChallengeIdFromUrl();
    console.log("Extracted Challenge ID:", challengeId); // 디버깅용 로그

    if (challengeId) {
        loadChallengeDetail(challengeId);
    } else {
        console.error("챌린지 ID를 찾을 수 없습니다. URL을 확인해주세요.");
        // alert("잘못된 접근입니다."); // 필요 시 주석 해제
        // window.location.href = "/challenge";
    }

    // 참여하기 버튼 클릭 이벤트
    if (joinButton) {
        joinButton.addEventListener("click", () => {
            handleJoinChallenge(challengeId);
        });
    }

    // 포기 버튼 클릭 이벤트
    if (cancelButton) {
        cancelButton.addEventListener("click", () => {
            handleCancelChallenge(challengeId);
        });
    }
});

/**
 * URL에서 챌린지 ID 추출
 * 수정됨: /challenge/{id}, /challenges/{id}, /challenge-detail/{id} 모두 지원
 */
function extractChallengeIdFromUrl() {
    const path = window.location.pathname;
    // (?:...): 비캡처 그룹, |: OR 연산
    // /challenge/, /challenges/, /challenge-detail/ 뒤에 오는 숫자(\d+)를 캡처
    const match = path.match(/\/(?:challenge|challenges|challenge-detail)\/(\d+)/);
    return match ? match[1] : null;
}

/**
 * 챌린지 상세 정보 로드
 */
async function loadChallengeDetail(challengeId) {
    const accessToken = localStorage.getItem("accessToken");

    try {
        // API 경로는 ChallengeController의 @RequestMapping("/challenges")를 따름
        const response = await fetch(`/challenges/${challengeId}`, {
            headers: accessToken
                ? {Authorization: `Bearer ${accessToken}`}
                : {},
        });

        if (!response.ok) {
            const text = await response.text().catch(() => "");
            throw new Error(`챌린지 정보를 불러오지 못했습니다. (Status: ${response.status}) ${text}`);
        }

        const payload = await response.json().catch(() => null);
        const challenge = payload?.data ?? payload;

        if (!challenge) {
            throw new Error("서버에서 받은 챌린지 정보가 비어있습니다.");
        }

        renderChallengeDetail(challenge);
    } catch (error) {
        console.error("챌린지 로드 실패:", error);
        alert(error.message || "챌린지 정보를 불러오는 중 오류가 발생했습니다.");
    }
}

/**
 * 챌린지 상세 정보 렌더링
 */
function renderChallengeDetail(challenge) {
    // 제목
    const titleEl = document.querySelector('[data-role="challenge-title"]');
    if (titleEl) {
        titleEl.textContent = challenge.title || "챌린지 제목";
    }

    // 이미지
    const imageEl = document.querySelector('[data-role="challenge-image"]');
    if (imageEl) {
        if (challenge.imageUrl) {
            imageEl.src = challenge.imageUrl;
            imageEl.alt = challenge.title || "챌린지 이미지";
            imageEl.onerror = function () {
                this.src = "/img/default-challenge.png";
            };
        } else {
            imageEl.src = "/img/default-challenge.png";
        }
    }

    // 설명
    const descriptionEl = document.querySelector('[data-role="challenge-description"]');
    if (descriptionEl) {
        descriptionEl.textContent = challenge.description || "상세 정보가 없습니다.";
    }

    // 남은 일수 계산
    const daysRemaining = calculateDaysRemaining(challenge.endDate);
    const daysTextEl = document.querySelector('[data-role="days-text"]');
    if (daysTextEl) {
        daysTextEl.textContent = daysRemaining >= 0 ? `${daysRemaining}일` : "종료됨";
    }

    // 사용자 참여 상태에 따른 UI 표시
    const status = challenge.myStatus; // JOINED, IN_PROGRESS, COMPLETED, FAILED, CANCELED, null

    // 모든 버튼/영역 초기화 (숨김)
    const joinBtn = document.querySelector('[data-role="join-button"]');
    const cancelBtn = document.querySelector('[data-role="cancel-button"]');
    const progressArea = document.querySelector('[data-role="progress-area"]');

    if (joinBtn) joinBtn.hidden = true;
    if (cancelBtn) cancelBtn.hidden = true;
    if (progressArea) progressArea.hidden = true;

    if (status === "JOINED" || status === "IN_PROGRESS") {
        // 1. 참여중: 진행 상황 표시 + 포기 버튼
        showProgressArea(challenge);
        if (cancelBtn) cancelBtn.hidden = false;
    } else if (status === "COMPLETED" || status === "FAILED") {
        // 2. 완료/실패: 아무 버튼도 안 띄우거나 "완료됨" 표시
    } else {
        // 3. 미참여 (null, CANCELED): 참여하기 버튼 표시 (단, 기간이 남았을 때만)
        if (daysRemaining >= 0 && joinBtn) {
            joinBtn.hidden = false;
        }
    }
}

/**
 * 진행 상황 영역 표시
 */
function showProgressArea(challenge) {
    const progressArea = document.querySelector('[data-role="progress-area"]');
    if (!progressArea) return;

    progressArea.hidden = false;

    let targetValue = challenge.targetValue || 0;
    const progressValue = challenge.progressValue || 0;

    // [수정] TIME 타입인 경우 목표값을 '시간' 단위로 간주하여 '분'으로 변환
    if (challenge.challengeType === 'TIME') {
        targetValue = targetValue * 60;
    }

    // 0으로 나누기 방지
    let progressPercent = 0;
    if (targetValue > 0) {
        progressPercent = Math.min((progressValue / targetValue) * 100, 100);
    }

    // 진행률 텍스트
    const progressTextEl = document.querySelector('[data-role="progress-text"]');
    if (progressTextEl) {
        progressTextEl.textContent = `${Math.round(progressPercent)}%`;
    }

    // 진행 바
    const progressFillEl = document.querySelector('[data-role="progress-fill"]');
    if (progressFillEl) {
        progressFillEl.style.width = `${progressPercent}%`;
    }

    // 진행 통계 (현재 / 목표)
    const progressCurrentEl = document.querySelector('[data-role="progress-current"]');
    const progressTargetEl = document.querySelector('[data-role="progress-target"]');
    if (progressCurrentEl) {
        progressCurrentEl.textContent = formatProgressValue(progressValue, challenge.challengeType);
    }
    if (progressTargetEl) {
        // targetValue는 위에서 분 단위로 변환되었으므로 formatProgressValue(->formatTime)에서 올바르게 처리됨
        progressTargetEl.textContent = formatProgressValue(targetValue, challenge.challengeType);
    }
}

/**
 * 진행 값 포맷팅 (타입에 따라 단위 추가)
 */
function formatProgressValue(value, challengeType) {
    if (value === undefined || value === null) return "0";

    switch (challengeType) {
        case "DISTANCE":
            return `${value}km`;
        case "TIME":
            return formatTime(value);
        case "COUNT":
        case "ATTENDANCE":
            return `${Math.round(value)}회`;
        default:
            return value.toString();
    }
}

/**
 * 시간 포맷팅 (분 단위를 시간:분으로)
 */
function formatTime(minutes) {
    if (!minutes && minutes !== 0) return "0시간 0분";
    const hours = Math.floor(minutes / 60);
    const mins = Math.round(minutes % 60);

    // 1시간 이상일 때: "20시간" 또는 "1시간 30분" 형식
    if (hours > 0) {
        return mins > 0 ? `${hours}시간 ${mins}분` : `${hours}시간`;
    }
    // 1시간 미만일 때: "0시간 30분" 형식으로 수정
    return `0시간 ${mins}분`;
}

/**
 * 남은 일수 계산
 */
function calculateDaysRemaining(endDateString) {
    if (!endDateString) return -1;

    const endDate = new Date(endDateString);
    endDate.setHours(23, 59, 59, 999);

    const today = new Date();
    // today.setHours(0, 0, 0, 0);

    const diffTime = endDate - today;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    return diffDays;
}

/**
 * 챌린지 참여하기
 */
async function handleJoinChallenge(challengeId) {
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken) {
        alert("로그인이 필요합니다.");
        window.location.href = "/login";
        return;
    }

    const joinButton = document.querySelector('[data-role="join-button"]');
    if (joinButton) {
        joinButton.disabled = true;
        joinButton.textContent = "참여 중...";
    }

    try {
        const response = await fetch(`/challenges/${challengeId}/join`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${accessToken}`,
            },
        });

        if (!response.ok) {
            const data = await response.json().catch(() => ({}));
            throw new Error(data?.message || "챌린지 참여에 실패했습니다.");
        }

        alert("챌린지에 참여했습니다!");
        window.location.reload();
    } catch (error) {
        alert(error.message || "챌린지 참여 중 오류가 발생했습니다.");
    } finally {
        if (joinButton) {
            joinButton.disabled = false;
            joinButton.textContent = "참여하기";
        }
    }
}

/**
 * 챌린지 포기하기
 */
async function handleCancelChallenge(challengeId) {
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken) {
        alert("로그인이 필요합니다.");
        return;
    }

    if (!confirm("정말 챌린지를 포기하시겠습니까?")) {
        return;
    }

    const cancelButton = document.querySelector('[data-role="cancel-button"]');
    if (cancelButton) {
        cancelButton.disabled = true;
        cancelButton.textContent = "포기 중...";
    }

    try {
        const response = await fetch(`/challenges/${challengeId}/cancel`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${accessToken}`,
            },
        });

        if (!response.ok) {
            const data = await response.json().catch(() => ({}));
            throw new Error(data?.message || "챌린지 포기에 실패했습니다.");
        }

        alert("챌린지를 포기했습니다.");
        window.location.reload();
    } catch (error) {
        alert(error.message || "챌린지 포기 중 오류가 발생했습니다.");
    } finally {
        if (cancelButton) {
            cancelButton.disabled = false;
            cancelButton.textContent = "챌린지 포기";
        }
    }
}