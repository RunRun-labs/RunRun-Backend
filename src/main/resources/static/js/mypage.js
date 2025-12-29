document.addEventListener("DOMContentLoaded", () => {
    console.log("mypage.js loaded");
    attachProfileEditHandler();
    attachProfileImageClickHandler();
    attachChallengeHandler();
    attachSettingsHandler();
    loadMyBodyInfo();
});

async function loadMyBodyInfo() {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) return;

        const res = await fetch("/users", {
            headers: {"Authorization": `Bearer ${token}`}
        });

        if (!res.ok) throw new Error("조회 실패");

        const payload = await res.json();
        const user = payload?.data ?? null;
        renderBodyInfo(user);
        renderProfileImage(user);
    } catch (e) {
        console.error(e);
    }
}

function renderBodyInfo(user) {
    const heightEl = document.getElementById("heightCm");
    const weightEl = document.getElementById("weightKg");
    const bmiEl = document.getElementById("bmiValue");

    const height = user?.heightCm;
    const weight = user?.weightKg;

    heightEl.textContent = height ?? "-";
    weightEl.textContent = weight ?? "-";

    if (height && weight) {
        bmiEl.textContent = calculateBMI(height, weight).toFixed(1);
    } else {
        bmiEl.textContent = "-";
    }
}

function calculateBMI(heightCm, weightKg) {
    const h = heightCm / 100;
    return weightKg / (h * h);
}


function attachProfileImageClickHandler() {
    const container =
        document.querySelector('[data-role="avatar-click"]') ||
        document.querySelector(".profile-avatar");

    if (!container) return;


    container.style.cursor = "pointer";
    container.setAttribute("role", "button");
    container.setAttribute("tabindex", "0");

    const goEdit = () => {
        window.location.href = "/myPage/edit";
    };

    container.addEventListener("click", goEdit);


}

function attachProfileEditHandler() {
    const profileSettingsBtn = document.querySelector('[data-role="profile-settings"]');
    if (!profileSettingsBtn) return;

    profileSettingsBtn.addEventListener("click", () => {
        window.location.href = "/myPage/edit";
    });
}

function attachChallengeHandler() {
    const challengeBtn = document.querySelector('.profile-actions .action-pill:first-child');
    if (!challengeBtn) return;

    challengeBtn.addEventListener("click", () => {
        window.location.href = "/challenge";
    });
}

function attachSettingsHandler() {
    const settingsBtn = document.querySelector('[data-role="settings"]');
    if (!settingsBtn) return;

    settingsBtn.addEventListener("click", () => {
        window.location.href = "/setting";
    });
}


function renderProfileImage(user) {
    const imgEl = document.querySelector('img[data-role="profile-preview"]');
    const initialEl = document.querySelector('span[data-role="profile-initial"]');

    if (!imgEl) return;

    const url = user?.profileImageUrl;

    if (!url) {

        imgEl.removeAttribute("src");
        imgEl.hidden = true;

        if (initialEl) {
            initialEl.textContent = "";
            initialEl.hidden = true;
        }
        return;
    }


    imgEl.src = url;
    imgEl.alt = "프로필 이미지";
    imgEl.decoding = "async";
    imgEl.loading = "lazy";
    imgEl.hidden = false;

    if (initialEl) {
        initialEl.textContent = "";
        initialEl.hidden = true;
    }


    imgEl.addEventListener("error", () => {
        imgEl.removeAttribute("src");
        imgEl.hidden = true;

        if (initialEl) {
            initialEl.textContent = "";
            initialEl.hidden = true;
        }
    }, {once: true});
}