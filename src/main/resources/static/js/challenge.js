document.addEventListener("DOMContentLoaded", () => {
    const backButton = document.querySelector(".back-button");
    const bottomNavMount = document.getElementById("bottomNavMount");
    const bottomNavTemplate = document.getElementById("bottomNavTemplate");
    const createButton = document.querySelector('[data-role="create-button"]');

    if (backButton) {
        backButton.addEventListener("click", () => {
            // 챌린지 목록의 상위는 마이페이지
            window.location.href = "/myPage";
        });
    }

    if (bottomNavMount && bottomNavMount.childElementCount === 0 && bottomNavTemplate) {
        const clone = bottomNavTemplate.content.firstElementChild.cloneNode(true);
        bottomNavMount.replaceWith(clone);
    }

    // 등록하기 버튼 클릭 이벤트
    if (createButton) {
        createButton.addEventListener("click", () => {
            window.location.href = "/challenge/create";
        });
    }

    // 종료된 챌린지 보기 버튼 클릭 이벤트
    const endChallengeButton = document.querySelector('[data-role="end-challenge-button"]');
    if (endChallengeButton) {
        endChallengeButton.addEventListener("click", () => {
            window.location.href = "/challenge/end";
        });
    }

    loadChallenges();
});

async function loadChallenges() {
    const accessToken = localStorage.getItem("accessToken");

    try {
        const role = getRoleFromJwt(accessToken);
        const isAdmin = role === "ROLE_ADMIN";

        // ✅ 관리자면 안내 문구 숨김 및 등록하기 버튼 표시
        if (isAdmin) {
            const intro = document.querySelector(".challenge-intro");
            if (intro) intro.hidden = true;

            const createButton = document.querySelector('[data-role="create-button"]');
            if (createButton) {
                createButton.hidden = false;
            }
        }

        const response = await fetch("/challenges", {
            headers: accessToken ? {Authorization: `Bearer ${accessToken}`} : {},
        });

        if (response.status === 401) return;

        if (!response.ok) {
            const text = await response.text().catch(() => "");
            throw new Error(`챌린지 목록을 불러오지 못했습니다. ${text}`);
        }

        const payload = await response.json().catch(() => null);
        const challenges = payload?.data ?? payload ?? [];

        const today = new Date();
        today.setHours(0, 0, 0, 0);

        if (isAdmin) {
            // 관리자는 종료일이 미래인 챌린지만 표시
            const filtered = (Array.isArray(challenges) ? challenges : []).filter((c) => {
                const endDate = new Date(c.endDate);
                endDate.setHours(0, 0, 0, 0);
                return isNaN(endDate.getTime()) ? true : endDate >= today;
            });

            // 관리자: 참여 중(ongoing) 섹션 숨김 + 도전 가능(available) 영역에만 표시
            const ongoingSection = document.querySelector('[data-section="ongoing"]');
            if (ongoingSection) ongoingSection.hidden = true;

            renderChallenges("available", filtered);
            return;
        }

        // 일반 사용자: 참여 중(ongoing) + 도전 가능(available)
        const ongoing = [];
        const available = [];

        (Array.isArray(challenges) ? challenges : []).forEach((challenge) => {
            const status = challenge.myStatus;

            const endDate = new Date(challenge.endDate);
            endDate.setHours(0, 0, 0, 0);

            const isFutureOrToday = isNaN(endDate.getTime()) ? true : endDate >= today;

            // 1) 도전종료: 무시 (탭 삭제됨)
            if (status === "COMPLETED" || status === "FAILED") {
                return;
            }

            // 2) 도전중: JOINED/IN_PROGRESS && endDate >= today
            if ((status === "JOINED" || status === "IN_PROGRESS") && isFutureOrToday) {
                ongoing.push(challenge);
                return;
            }

            // 3) 도전 가능: 위 조건에 해당하지 않는 나머지 && endDate >= today
            if (isFutureOrToday) {
                available.push(challenge);
            }
        });

        renderChallenges("ongoing", ongoing);
        renderChallenges("available", available);

    } catch (error) {
        console.error("챌린지 로드 실패:", error);
    }
}

function hideSectionTitle(sectionKey) {
    const section = document.querySelector(`[data-section="${sectionKey}"]`);
    if (!section) return;
    const title = section.querySelector(".section-title");
    if (title) title.hidden = true;
}

function setEmptyMessageHidden(sectionType, hidden) {
    const emptyMessage = document.querySelector(`[data-role="${sectionType}-empty"]`);
    if (emptyMessage) emptyMessage.hidden = hidden;
}

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

function decodeBase64Url(base64Url) {
    let base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    while (base64.length % 4) base64 += "=";

    const binary = atob(base64);
    const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0));
    return new TextDecoder("utf-8").decode(bytes);
}

function renderChallenges(sectionType, challenges) {
    const listContainer = document.querySelector(`[data-role="${sectionType}-list"]`);
    const emptyMessage = document.querySelector(`[data-role="${sectionType}-empty"]`);
    if (!listContainer) return;

    // 기존 카드 제거 (empty 블록은 유지)
    Array.from(listContainer.children).forEach((child) => {
        const role = child.getAttribute("data-role");
        if (role === `${sectionType}-empty`) return;
        child.remove();
    });

    if (!Array.isArray(challenges) || challenges.length === 0) {
        if (emptyMessage) emptyMessage.hidden = false;
        return;
    }

    if (emptyMessage) emptyMessage.hidden = true;

    challenges.forEach((challenge) => {
        const card = createChallengeCard(challenge, sectionType);
        listContainer.appendChild(card);
    });
}

// SVG 아이콘 정의
const CHALLENGE_ICONS = {
    COUNT: `<svg class="type-icon-svg" width="24px" height="24px" stroke-width="1.5" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" color="#000000"><path d="M15 4V2M15 4V6M15 4H10.5M3 10V19C3 20.1046 3.89543 21 5 21H19C20.1046 21 21 20.1046 21 19V10H3Z" stroke="#000000" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"></path><path d="M3 10V6C3 4.89543 3.89543 4 5 4H7" stroke="#000000" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"></path><path d="M7 2V6" stroke="#000000" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"></path><path d="M21 10V6C21 4.89543 20.1046 4 19 4H18.5" stroke="#000000" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"></path></svg>`,
    DISTANCE: `<svg class="type-icon-svg" width="24px" height="24px" stroke-width="1.5" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" color="#000000"><path d="M15 7C16.1046 7 17 6.10457 17 5C17 3.89543 16.1046 3 15 3C13.8954 3 13 3.89543 13 5C13 6.10457 13.8954 7 15 7Z" stroke="#000000" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"></path><path d="M12.6133 8.26691L9.30505 12.4021L13.4403 16.5374L11.3727 21.0861" stroke="#000000" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"></path><path d="M6.4104 9.5075L9.79728 6.19931L12.6132 8.26692L15.508 11.5752H19.2297" stroke="#000000" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"></path><path d="M8.89152 15.7103L7.65095 16.5374H4.34277" stroke="#000000" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"></path></svg>`,
    TIME: `<svg class="type-icon-svg" width="24px" height="24px" stroke-width="1.5" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" color="#000000"><path d="M9 2L15 2" stroke="#000000" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"></path><path d="M12 10L12 14" stroke="#000000" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"></path><path d="M12 22C16.4183 22 20 18.4183 20 14C20 9.58172 16.4183 6 12 6C7.58172 6 4 9.58172 4 14C4 18.4183 7.58172 22 12 22Z" stroke="#000000" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"></path></svg>`
};

function createChallengeCard(challenge, sectionType) {
    const card = document.createElement("div");
    card.className = "challenge-card";
    card.setAttribute("data-challenge-id", challenge.id);

    card.addEventListener("click", () => {
        window.location.href = `/challenge/${challenge.id}`;
    });

    const thumb = document.createElement("div");
    thumb.className = "challenge-thumb";
    const thumbImg = document.createElement("img");
    thumbImg.src = challenge.imageUrl || "/img/default-challenge.png";
    thumbImg.alt = challenge.title || "챌린지 이미지";
    thumbImg.onerror = function () {
        this.onerror = null;
        this.src = "/img/default-challenge.png";
    };
    thumb.appendChild(thumbImg);

    const content = document.createElement("div");
    content.className = "challenge-content";

    const title = document.createElement("h3");
    title.className = "challenge-title";
    title.textContent = challenge.title || "챌린지 제목";

    const meta = document.createElement("div");
    meta.className = "challenge-meta";

    const dateRow = document.createElement("div");
    dateRow.className = "challenge-meta-row";
    const startDate = formatDate(challenge.startDate);
    const endDate = formatDate(challenge.endDate);
    dateRow.innerHTML = `
        <span class="challenge-meta-label">📅 </span>
        <span class="challenge-meta-value">${startDate} ~ ${endDate}</span>
    `;

    const typeRow = document.createElement("div");
    typeRow.className = "challenge-meta-row";

    // 타입 뱃지 생성 (아이콘 + 텍스트)
    const typeBadge = document.createElement("span");
    typeBadge.className = "challenge-type-badge";
    typeBadge.dataset.type = challenge.challengeType; // CSS 스타일링용 데이터 속성

    // 아이콘 wrapper
    const iconSpan = document.createElement("span");
    iconSpan.className = "challenge-type-icon";
    iconSpan.innerHTML = CHALLENGE_ICONS[challenge.challengeType] || '';

    // 텍스트 wrapper
    const textSpan = document.createElement("span");
    textSpan.textContent = getChallengeTypeLabel(challenge.challengeType);

    typeBadge.appendChild(iconSpan);
    typeBadge.appendChild(textSpan);

    typeRow.appendChild(typeBadge);

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

    if (sectionType === "ongoing") card.classList.add("challenge-ongoing");

    return card;
}

function formatDate(dateString) {
    if (!dateString) return "-";
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return "-";
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}.${month}.${day}`;
}

function getChallengeTypeLabel(type) {
    const labels = {DISTANCE: "거리형", TIME: "시간형", COUNT: "출석형"};
    return labels[type] || type;
}
