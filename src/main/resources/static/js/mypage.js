document.addEventListener("DOMContentLoaded", () => {
    console.log("mypage.js loaded");
    attachProfileEditHandler();
    loadMyBodyInfo();
});

async function loadMyBodyInfo() {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) return;

        const res = await fetch("/users/me", {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!res.ok) throw new Error("조회 실패");

        const user = await res.json();
        renderBodyInfo(user);
    } catch (e) {
        console.error(e);
    }
}

function renderBodyInfo(user) {
    const heightEl = document.getElementById("heightCm");
    const weightEl = document.getElementById("weightKg");
    const bmiEl = document.getElementById("bmiValue");

    const height = user.heightCm;
    const weight = user.weightKg;

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

function attachProfileEditHandler() {
    const profileSettingsBtn = document.querySelector('[data-role="profile-settings"]');
    if (!profileSettingsBtn) return;

    profileSettingsBtn.addEventListener("click", () => {
        window.location.href = "http://localhost:8080/myPage/edit";
    });
}
