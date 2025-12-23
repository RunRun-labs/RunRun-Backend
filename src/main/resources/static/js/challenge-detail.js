document.addEventListener("DOMContentLoaded", () => {
    const backButton = document.querySelector(".back-button");
    const bottomNavMount = document.getElementById("bottomNavMount");
    const bottomNavTemplate = document.getElementById("bottomNavTemplate");
    const joinButton = document.querySelector('[data-role="join-button"]');
    const cancelButton = document.querySelector('[data-role="cancel-button"]');
    const editButton = document.querySelector('[data-role="edit-button"]');
    const deleteButton = document.querySelector('[data-role="delete-button"]');
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

    // 수정하기 버튼 클릭 이벤트
    if (editButton) {
        editButton.addEventListener("click", () => {
            handleEditChallenge(challengeId);
        });
    }

    // 삭제하기 버튼 클릭 이벤트
    if (deleteButton) {
        deleteButton.addEventListener("click", () => {
            handleDeleteChallenge(challengeId);
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
        // 관리자 권한 확인
        const role = getRoleFromJwt(accessToken);
        const isAdmin = role === "ROLE_ADMIN";

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

        renderChallengeDetail(challenge, isAdmin);
    } catch (error) {
        console.error("챌린지 로드 실패:", error);
        alert(error.message || "챌린지 정보를 불러오는 중 오류가 발생했습니다.");
    }
}

/**
 * 챌린지 상세 정보 렌더링
 */
function renderChallengeDetail(challenge, isAdmin = false) {
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

    // 챌린지 기간 (시작일 ~ 종료일)
    const periodTextEl = document.querySelector('[data-role="period-text"]');
    if (periodTextEl) {
        const startDate = formatDate(challenge.startDate);
        const endDate = formatDate(challenge.endDate);
        periodTextEl.textContent = `${startDate} ~ ${endDate}`;
    }

    // 참가자 수
    const participantTextEl = document.querySelector('[data-role="participant-text"]');
    if (participantTextEl) {
        const participantCount = challenge.participantCount || 0;
        participantTextEl.textContent = `${participantCount}명 참가`;
    }

    // 남은 일수 계산
    const daysRemaining = calculateDaysRemaining(challenge.endDate);
    const daysMessageEl = document.querySelector('[data-role="days-message-text"]');
    if (daysMessageEl) {
        if (daysRemaining >= 0) {
            daysMessageEl.textContent = `목표 달성 기간이 ${daysRemaining}일 남았어요!`;
        } else {
            daysMessageEl.textContent = "챌린지가 종료되었습니다.";
        }
    }

    // 관리자일 경우 수정하기/삭제하기 버튼 표시
    if (isAdmin) {
        const joinBtn = document.querySelector('[data-role="join-button"]');
        const cancelBtn = document.querySelector('[data-role="cancel-button"]');
        const editBtn = document.querySelector('[data-role="edit-button"]');
        const deleteBtn = document.querySelector('[data-role="delete-button"]');
        const progressArea = document.querySelector('[data-role="progress-area"]');
        const progressBarContainer = document.querySelector('[data-role="progress-bar-container"]');

        // 일반 사용자용 버튼 숨김
        if (joinBtn) {
            joinBtn.hidden = true;
            joinBtn.style.display = "none";
        }
        if (cancelBtn) {
            cancelBtn.hidden = true;
            cancelBtn.style.display = "none";
        }
        if (progressArea) {
            progressArea.hidden = true;
            progressArea.style.display = "none";
        }
        if (progressBarContainer) {
            progressBarContainer.hidden = true;
            progressBarContainer.style.display = "none";
        }

        // 관리자용 버튼 표시
        if (editBtn) {
            editBtn.hidden = false;
            editBtn.style.display = "block";
        }
        if (deleteBtn) {
            deleteBtn.hidden = false;
            deleteBtn.style.display = "block";
        }
        return;
    }

    // 일반 사용자: 참여 상태에 따른 UI 표시
    const status = challenge.myStatus; // JOINED, IN_PROGRESS, COMPLETED, FAILED, CANCELED, null

    // 모든 버튼/영역 초기화 (숨김)
    const joinBtn = document.querySelector('[data-role="join-button"]');
    const cancelBtn = document.querySelector('[data-role="cancel-button"]');
    const editBtn = document.querySelector('[data-role="edit-button"]');
    const deleteBtn = document.querySelector('[data-role="delete-button"]');
    const progressArea = document.querySelector('[data-role="progress-area"]');
    const progressBarContainer = document.querySelector('[data-role="progress-bar-container"]');

    // 모든 UI 요소를 명시적으로 숨김
    if (joinBtn) {
        joinBtn.hidden = true;
        joinBtn.style.display = "none";
    }
    if (cancelBtn) {
        cancelBtn.hidden = true;
        cancelBtn.style.display = "none";
    }
    if (editBtn) {
        editBtn.hidden = true;
        editBtn.style.display = "none";
    }
    if (deleteBtn) {
        deleteBtn.hidden = true;
        deleteBtn.style.display = "none";
    }
    if (progressArea) {
        progressArea.hidden = true;
        progressArea.style.display = "none";
    }
    if (progressBarContainer) {
        progressBarContainer.hidden = true;
        progressBarContainer.style.display = "none";
    }

    if (status === "JOINED" || status === "IN_PROGRESS") {
        // 1. 참여중: 진행 상황 표시 + 포기 버튼
        if (progressArea) {
            progressArea.hidden = false;
            progressArea.style.display = "flex";
        }
        if (progressBarContainer) {
            progressBarContainer.hidden = false;
            progressBarContainer.style.display = "block";
        }
        showProgressArea(challenge);
        if (cancelBtn) {
            cancelBtn.hidden = false;
            cancelBtn.style.display = "block";
        }
    } else if (status === "COMPLETED" || status === "FAILED") {
        // 2. 완료/실패: 아무 버튼도 안 띄우거나 "완료됨" 표시
        // progress area는 숨김 상태 유지
    } else {
        // 3. 미참여 (null, CANCELED): 참여하기 버튼 표시 (단, 기간이 남았을 때만)
        // progress area는 숨김 상태 유지
        if (daysRemaining >= 0 && joinBtn) {
            joinBtn.hidden = false;
            joinBtn.style.display = "block";
        }
    }
}

/**
 * 진행 상황 영역 표시
 */
function showProgressArea(challenge) {
    const progressArea = document.querySelector('[data-role="progress-area"]');
    if (!progressArea) return;

    // 이미 renderChallengeDetail에서 표시 처리했지만, 이중으로 보장
    progressArea.hidden = false;
    progressArea.style.display = "flex";

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

    // 진행 통계 (현재 / 목표) - 카드 형식에 맞게 표시
    const progressStatsEl = document.querySelector('[data-role="progress-stats"]');
    if (progressStatsEl) {
        const currentText = formatProgressValue(progressValue, challenge.challengeType);
        const targetText = formatProgressValue(targetValue, challenge.challengeType);
        progressStatsEl.textContent = `${currentText}/${targetText}`;
    }

    // 진행 바
    const progressFillEl = document.querySelector('[data-role="progress-fill"]');
    if (progressFillEl) {
        progressFillEl.style.width = `${progressPercent}%`;
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
            return `${Math.round(value)}일`;
        default:
            return value.toString();
    }
}

/**
 * 날짜 포맷팅 (YYYY-MM-DD → YYYY.MM.DD)
 */
function formatDate(dateString) {
    if (!dateString) return "-";
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return "-";
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}.${month}.${day}`;
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

    // 재확인 팝업
    const confirmed = confirm("정말 챌린지를 포기하시겠습니까?\n\n포기한 챌린지는 다시 참여할 수 있습니다.");
    if (!confirmed) {
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

/**
 * JWT에서 역할(role) 추출
 */
function getRoleFromJwt(token) {
    if (!token || typeof token !== "string") return null;
    const parts = token.split(".");
    if (parts.length < 2) return null;

    try {
        const payloadJson = decodeBase64Url(parts[1]);
        const payload = JSON.parse(payloadJson);

        if (typeof payload.role === "string") return payload.role;
        if (typeof payload.auth === "string") return payload.auth;

        const authorities = payload.authorities || payload.roles;
        if (Array.isArray(authorities) && typeof authorities[0] === "string") return authorities[0];

        return null;
    } catch {
        return null;
    }
}

/**
 * Base64URL 디코딩
 */
function decodeBase64Url(base64Url) {
    let base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    while (base64.length % 4) base64 += "=";

    const binary = atob(base64);
    const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0));
    return new TextDecoder("utf-8").decode(bytes);
}

/**
 * 챌린지 수정하기
 */
async function handleEditChallenge(challengeId) {
    if (!challengeId) {
        console.error("챌린지 ID가 없습니다.");
        return;
    }
    window.location.href = `/challenge/${challengeId}/edit`;
}

/**
 * 챌린지 삭제하기
 */
async function handleDeleteChallenge(challengeId) {
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken) {
        alert("로그인이 필요합니다.");
        return;
    }

    if (!confirm("정말 챌린지를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")) {
        return;
    }

    const deleteButton = document.querySelector('[data-role="delete-button"]');
    if (deleteButton) {
        deleteButton.disabled = true;
        deleteButton.textContent = "삭제 중...";
    }

    try {
        const response = await fetch(`/challenges/${challengeId}`, {
            method: "DELETE",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${accessToken}`,
            },
        });

        if (!response.ok) {
            const data = await response.json().catch(() => ({}));
            throw new Error(data?.message || "챌린지 삭제에 실패했습니다.");
        }

        alert("챌린지가 삭제되었습니다.");
        window.location.href = "/challenge";
    } catch (error) {
        alert(error.message || "챌린지 삭제 중 오류가 발생했습니다.");
    } finally {
        if (deleteButton) {
            deleteButton.disabled = false;
            deleteButton.textContent = "삭제하기";
        }
    }
}
