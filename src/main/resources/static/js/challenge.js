document.addEventListener("DOMContentLoaded", () => {
    const backButton = document.querySelector(".back-button");
    const bottomNavMount = document.getElementById("bottomNavMount");
    const bottomNavTemplate = document.getElementById("bottomNavTemplate");

    // 뒤로가기 버튼 클릭 이벤트
    if (backButton) {
        backButton.addEventListener("click", () => {
            if (window.history.length > 1) {
                window.history.back();
            } else {
                window.location.href = "/myPage";
            }
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

    loadChallenges();
});

/**
 * 챌린지 목록 로드
 * - 사용자가 참여 중인 챌린지 (JOINED, IN_PROGRESS) → "도전중"
 * - 참여하지 않은 챌린지 중 종료일이 지나지 않은 것 → "도전 가능"
 */
async function loadChallenges() {
    const accessToken = localStorage.getItem("accessToken");
    // accessToken이 없어도 공개 챌린지 목록은 가져올 수 있게 허용

    try {
        const response = await fetch("/challenges", {
            headers: accessToken ? {Authorization: `Bearer ${accessToken}`} : {},
        });

        if (response.status === 401) {
            console.warn("챌린지 조회: 인증 필요");
            return;
        }

        if (!response.ok) {
            const text = await response.text().catch(() => "");
            throw new Error(`챌린지 목록을 불러오지 못했습니다. ${text}`);
        }

        const payload = await response.json().catch(() => null);
        const challenges = payload?.data ?? payload ?? [];

        // [수정] 별도의 /user-challenges/me 호출 로직 제거
        // 백엔드에서 이미 myStatus 필드에 상태를 담아줍니다.

        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const ongoing = [];
        const available = [];

        (Array.isArray(challenges) ? challenges : []).forEach((challenge) => {
            const endDate = new Date(challenge.endDate);
            endDate.setHours(0, 0, 0, 0);

            // 종료일이 지난 챌린지는 제외 (화면 정책에 따라 다를 수 있음)
            if (!isNaN(endDate.getTime()) && endDate < today) {
                return;
            }

            // [수정] DTO에 포함된 myStatus 사용
            const status = challenge.myStatus;

            // 도전중: JOINED 또는 IN_PROGRESS
            if (status === "JOINED" || status === "IN_PROGRESS") {
                ongoing.push(challenge);
            } else {
                // 도전 가능: 참여하지 않았거나(null) COMPLETED/FAILED/CANCELED
                // 이미 참여 완료된 것도 목록에서 보여줄지 여부는 기획에 따름 (여기선 available로 분류)
                available.push(challenge);
            }
        });

        renderChallenges("ongoing", ongoing);
        renderChallenges("available", available);
    } catch (error) {
        console.error("챌린지 로드 실패:", error);
    }
}

/**
 * 챌린지 목록 렌더링
 */
function renderChallenges(sectionType, challenges) {
    const listContainer = document.querySelector(`[data-role="${sectionType}-list"]`);
    const emptyMessage = document.querySelector(`[data-role="${sectionType}-empty"]`);

    if (!listContainer) return;

    // 기존 카드 제거 (빈 메시지 제외)
    Array.from(listContainer.children).forEach((child) => {
        if (!child.hasAttribute("data-role") || !child.hasAttribute("hidden")) {
            child.remove();
        }
    });

    if (!Array.isArray(challenges) || challenges.length === 0) {
        if (emptyMessage) {
            emptyMessage.hidden = false;
        }
        return;
    }

    if (emptyMessage) {
        emptyMessage.hidden = true;
    }

    challenges.forEach((challenge) => {
        const card = createChallengeCard(challenge, sectionType);
        listContainer.appendChild(card);
    });
}

/**
 * 챌린지 카드 생성
 */
function createChallengeCard(challenge, sectionType) {
    const card = document.createElement("div");
    card.className = "challenge-card";
    card.setAttribute("data-challenge-id", challenge.id);

    // 클릭 시 상세 페이지로 이동
    card.addEventListener("click", () => {
        // 상세 페이지 경로는 PathController 설정에 따름 (/challenge/{id} 등)
        // 현재는 목록 조회까지만 구현되었으므로 임시 경로
        window.location.href = `/challenge/${challenge.id}`;
    });

    // 썸네일
    const thumb = document.createElement("div");
    thumb.className = "challenge-thumb";
    const thumbImg = document.createElement("img");
    thumbImg.src = challenge.imageUrl || "/img/default-challenge.png";
    thumbImg.alt = challenge.title || "챌린지 이미지";

    // [수정] 무한 루프 방지 로직 추가
    thumbImg.onerror = function () {
        this.onerror = null; // 이벤트 핸들러 제거 (루프 방지)
        this.src = "/img/default-challenge.png";
    };

    thumb.appendChild(thumbImg);

    // 콘텐츠
    const content = document.createElement("div");
    content.className = "challenge-content";

    // 제목
    const title = document.createElement("h3");
    title.className = "challenge-title";
    title.textContent = challenge.title || "챌린지 제목";

    // 메타 정보
    const meta = document.createElement("div");
    meta.className = "challenge-meta";

    // 날짜
    const dateRow = document.createElement("div");
    dateRow.className = "challenge-meta-row";
    const startDate = formatDate(challenge.startDate);
    const endDate = formatDate(challenge.endDate);
    dateRow.innerHTML = `
        <span class="challenge-meta-label">📅 </span>
        <span class="challenge-meta-value">${startDate} ~ ${endDate}</span>
    `;

    // 타입
    const typeRow = document.createElement("div");
    typeRow.className = "challenge-meta-row";
    const typeBadge = document.createElement("span");
    typeBadge.className = "challenge-type-badge";
    typeBadge.textContent = getChallengeTypeLabel(challenge.challengeType);
    typeRow.appendChild(typeBadge);

    // 참여 인원 (DTO에 participantCount가 없다면 0 처리)
    const participantRow = document.createElement("div");
    participantRow.className = "challenge-meta-row";
    const participantCount = challenge.participantCount || 0;
    participantRow.innerHTML = `
        <span class="challenge-meta-label">👥 </span>
        <span class="challenge-meta-value">${participantCount}명</span>
    `;

    meta.appendChild(dateRow);
    meta.appendChild(typeRow);
    meta.appendChild(participantRow);

    content.appendChild(title);
    content.appendChild(meta);

    card.appendChild(thumb);
    card.appendChild(content);

    // (옵션) 도전 중인 카드는 스타일 강조
    if (sectionType === "ongoing") {
        card.classList.add("challenge-ongoing");
    }

    return card;
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
 * 챌린지 타입 라벨 변환
 */
function getChallengeTypeLabel(type) {
    const labels = {
        DISTANCE: "거리형",
        TIME: "시간형",
        ATTENDANCE: "출석형", // COUNT -> ATTENDANCE (Enum명 확인 필요)
    };
    return labels[type] || type;
}