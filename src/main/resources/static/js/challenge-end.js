document.addEventListener("DOMContentLoaded", () => {
    const backButton = document.querySelector(".back-button");
    const bottomNavMount = document.getElementById("bottomNavMount");
    const bottomNavTemplate = document.getElementById("bottomNavTemplate");

    if (backButton) {
        backButton.addEventListener("click", () => {
            window.location.href = "/challenge";
        });
    }

    if (bottomNavMount && bottomNavMount.childElementCount === 0 && bottomNavTemplate) {
        const clone = bottomNavTemplate.content.firstElementChild.cloneNode(true);
        bottomNavMount.replaceWith(clone);
    }

    loadEndedChallenges();
});

async function loadEndedChallenges() {
    const accessToken = localStorage.getItem("accessToken");

    try {
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

        // COMPLETED와 FAILED 상태만 필터링
        const completed = [];
        const failed = [];

        (Array.isArray(challenges) ? challenges : []).forEach((challenge) => {
            const status = challenge.myStatus;

            if (status === "COMPLETED") {
                completed.push(challenge);
            } else if (status === "FAILED") {
                failed.push(challenge);
            }
        });

        renderChallenges("completed", completed);
        renderChallenges("failed", failed);
    } catch (error) {
        console.error("챌린지 로드 실패:", error);
    }
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

function createChallengeCard(challenge, sectionType) {
    const card = document.createElement("div");
    card.className = "challenge-card";
    card.setAttribute("data-challenge-id", challenge.id);

    card.addEventListener("click", () => {
        // 종료 목록에서 상세로 이동한 경우, 복귀 경로를 함께 전달
        const returnTo = encodeURIComponent("/challenge/end");
        window.location.href = `/challenge/${challenge.id}?returnTo=${returnTo}`;
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
    const typeBadge = document.createElement("span");
    typeBadge.className = "challenge-type-badge";
    typeBadge.textContent = getChallengeTypeLabel(challenge.challengeType);
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
