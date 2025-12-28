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
            const returnTo = getSafeReturnTo();

            // 1) 명시적 returnTo가 있으면 최우선
            if (returnTo) {
                window.location.href = returnTo;
                return;
            }

            // 2) referrer가 같은 origin이고, 종료 목록에서 왔으면 그쪽으로
            if (isSafeReferrerPath("/challenge/end")) {
                window.location.href = "/challenge/end";
                return;
            }

            // 3) 일반적인 뒤로가기가 가능한 경우 history 사용
            if (window.history.length > 1 && isSameOriginReferrer()) {
                window.history.back();
                return;
            }

            // 4) 폴백
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

    // 수정 버튼 클릭 이벤트
    if (editButton) {
        editButton.addEventListener("click", () => {
            handleEditChallenge(challengeId);
        });
    }

    // 삭제 버튼 클릭 이벤트
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
 * JWT 토큰에서 사용자 역할 추출 (수정됨: 다양한 키 지원 및 디버깅 로그)
 */
function getUserRoleFromToken() {
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken) {
        return null;
    }

    try {
        // JWT 토큰은 Base64로 인코딩된 3부분으로 구성: Header.Payload.Signature
        const parts = accessToken.split(".");
        if (parts.length !== 3) {
            return null;
        }

        // Payload 부분 디코딩 (Base64 URL Safe)
        const payload = parts[1];
        // Base64 URL Safe 디코딩을 위해 패딩 추가 및 문자 변환
        let base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
        while (base64.length % 4) {
            base64 += "=";
        }
        const decodedPayload = JSON.parse(atob(base64));

        console.log("[DEBUG] Decoded Token Payload:", decodedPayload);

        // "auth", "role", "roles", "authorities" 필드 중 하나에서 역할 정보 추출 시도
        const auth = decodedPayload.auth || decodedPayload.role || decodedPayload.roles || decodedPayload.authorities;

        if (!auth) {
            console.warn("[DEBUG] Token payload does not contain role information (keys: auth, role, roles, authorities)");
            return null;
        }

        // 문자열인 경우 콤마로 구분하여 배열로 변환, 이미 배열인 경우 그대로 반환
        let roles = [];
        if (Array.isArray(auth)) {
            roles = auth;
        } else if (typeof auth === 'string') {
            roles = auth.split(",").map(role => role.trim());
        }

        console.log("[DEBUG] Extracted Roles:", roles);
        return roles;
    } catch (error) {
        console.error("JWT 토큰 디코딩 실패:", error);
        return null;
    }
}

/**
 * 사용자가 관리자인지 확인 (수정됨: ROLE_ADMIN 및 ADMIN 체크)
 */
function isAdmin() {
    const roles = getUserRoleFromToken();
    if (!roles) {
        return false;
    }
    // "ROLE_ADMIN" 또는 "ADMIN" 문자열이 포함되어 있는지 확인
    const isAdminUser = roles.includes("ROLE_ADMIN") || roles.includes("ADMIN");
    console.log("[DEBUG] Is Admin User?", isAdminUser);
    return isAdminUser;
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

            // [수정] 무한 루프 방지
            imageEl.onerror = function () {
                this.onerror = null;
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

    // 남은 일수 계산 및 메시지 표시 로직 개선
    const daysMessageEl = document.querySelector('[data-role="days-message-text"]');
    if (daysMessageEl) {
        // [수정] 시작 날짜까지 남은 일수 계산
        const daysToStart = calculateDaysRemaining(challenge.startDate);
        const daysRemaining = calculateDaysRemaining(challenge.endDate);

        // 아직 시작하지 않은 챌린지 (시작일이 미래)
        // 참여 상태와 무관하게 시작 전이면 시작일까지 남은 시간 안내
        if (daysToStart > 0) {
            daysMessageEl.textContent = `챌린지 시작까지 ${daysToStart}일 남았어요!`;
        } else if (daysToStart === 0) {
            // 오늘 시작하는 챌린지라면 (이미 시작된 것으로 간주할 수도 있고, 오늘부터 시작이라고 알릴 수도 있음)
            // 여기서는 오늘 시작일 때도 남은 기간을 보여주거나, 별도 문구 가능
            // "오늘 챌린지가 시작됩니다!" 혹은 바로 남은 기간 표시
            // 일반적으로 시작일 포함이므로 남은 기간 표시로 넘어감
            if (daysRemaining >= 0) {
                daysMessageEl.textContent = `목표 달성 기간이 ${daysRemaining}일 남았어요!`;
            } else {
                daysMessageEl.textContent = "종료된 챌린지입니다.";
            }
        } else {
            // 이미 시작된 챌린지
            if (daysRemaining > 0) {
                daysMessageEl.textContent = `목표 달성 기간이 ${daysRemaining}일 남았어요!`;
            } else if (daysRemaining === 0) {
                daysMessageEl.textContent = "오늘이 챌린지 마지막 날입니다!";
            } else {
                daysMessageEl.textContent = "종료된 챌린지입니다.";
            }
        }
    }

    // 사용자 참여 상태에 따른 UI 표시
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

    // 관리자인 경우: 수정하기/삭제하기 버튼 표시
    if (isAdmin()) {
        console.log("[DEBUG] Admin user detected. Showing edit/delete buttons.");
        if (editBtn) {
            editBtn.hidden = false;
            editBtn.style.display = "block";
        }
        if (deleteBtn) {
            deleteBtn.hidden = false;
            deleteBtn.style.display = "block";
        }
        // 관리자여도 챌린지 상세 내용은 봐야 하므로 progress는 숨기고, 참여 버튼도 숨김
    } else {
        // 일반 사용자인 경우: 기존 로직 유지
        console.log("[DEBUG] Normal user detected.");

        // [수정] 참여 기록이 있는 모든 상태(JOINED, IN_PROGRESS, COMPLETED, FAILED)에서 진행도 표시
        if (status === "JOINED" || status === "IN_PROGRESS" || status === "COMPLETED" || status === "FAILED") {
            if (progressArea) {
                progressArea.hidden = false;
                progressArea.style.display = "flex";
            }
            if (progressBarContainer) {
                progressBarContainer.hidden = false;
                progressBarContainer.style.display = "block";
            }
            showProgressArea(challenge);

            // 단, 포기 버튼은 '진행 중'일 때만 표시 (완료/실패 시에는 포기 불가)
            if (status === "JOINED" || status === "IN_PROGRESS") {
                if (cancelBtn) {
                    cancelBtn.hidden = false;
                    cancelBtn.style.display = "block";
                }
            }
        }

        // 미참여(null) 또는 취소(CANCELED) 상태인 경우
        else {
            // 기간이 지난(0 미만) 챌린지에는 참여 버튼을 띄우지 않음
            const daysRemaining = calculateDaysRemaining(challenge.endDate);
            if (daysRemaining >= 0 && joinBtn) {
                joinBtn.hidden = false;
                joinBtn.style.display = "block";
            }
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

    // TIME 타입인 경우 목표값을 '시간' 단위로 간주하여 '분'으로 변환
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

        // [추가] 실패 시 빨간색 등으로 표시하고 싶다면 클래스 추가 (선택사항)
        if (challenge.myStatus === "FAILED") {
            progressFillEl.style.backgroundColor = "#ff4d4d"; // 예: 빨간색
        } else if (challenge.myStatus === "COMPLETED") {
            progressFillEl.style.backgroundColor = "#4caf50"; // 예: 초록색
        } else {
            // 진행중일 때는 CSS에 정의된 기본 색상(형광 연두 등) 사용
            progressFillEl.style.backgroundColor = "";
        }
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
 * 시간 포맷팅 (분 단위를 시간으로 변환)
 * 예: 90분 -> 1.5시간, 60분 -> 1시간
 */
function formatTime(minutes) {
    if (!minutes && minutes !== 0) return "0시간";

    const hours = minutes / 60;
    const formattedHours = parseFloat(hours.toFixed(1));

    return `${formattedHours}시간`;
}

/**
 * 남은 일수 계산
 */
function calculateDaysRemaining(endDateString) {
    if (!endDateString) return -1;

    // 종료일 (시간은 00:00:00 기준)
    const endDate = new Date(endDateString);
    endDate.setHours(0, 0, 0, 0);

    // 오늘 날짜 (시간은 00:00:00 기준)
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // 차이 계산 (밀리초)
    const diffTime = endDate.getTime() - today.getTime();

    // 차이가 음수면 이미 지난 날짜 -> -1 반환
    if (diffTime < 0) return -1;

    // 차이가 0이면 오늘 -> 0 반환
    // 차이가 양수면 남은 일수 -> 올림 처리 필요 없음 (시간이 00:00이므로 정확히 나누어 떨어짐)
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

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

/**
 * 챌린지 수정하기 (관리자)
 */
function handleEditChallenge(challengeId) {
    // 챌린지 수정 페이지로 이동 (또는 모달 열기)
    window.location.href = `/challenge/${challengeId}/edit`;
}

/**
 * 챌린지 삭제하기 (관리자)
 */
async function handleDeleteChallenge(challengeId) {
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken) {
        alert("로그인이 필요합니다.");
        return;
    }

    if (!confirm("정말 챌린지를 삭제하시겠습니까?\n삭제된 챌린지는 복구할 수 없습니다.")) {
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

function getSafeReturnTo() {
    try {
        const params = new URLSearchParams(window.location.search);
        const raw = params.get("returnTo");
        if (!raw) return null;

        // open redirect 방지: 반드시 내부 상대경로만 허용
        if (!raw.startsWith("/")) return null;
        // 더 엄격히: 우리가 쓰는 경로만 허용 (필요시 목록 추가)
        const allowed = new Set(["/challenge/end", "/challenge"]);
        if (!allowed.has(raw)) return null;

        return raw;
    } catch {
        return null;
    }
}

function isSameOriginReferrer() {
    try {
        if (!document.referrer) return false;
        const ref = new URL(document.referrer);
        return ref.origin === window.location.origin;
    } catch {
        return false;
    }
}

function isSafeReferrerPath(expectedPath) {
    try {
        if (!document.referrer) return false;
        const ref = new URL(document.referrer);
        if (ref.origin !== window.location.origin) return false;
        return ref.pathname === expectedPath;
    } catch {
        return false;
    }
}
