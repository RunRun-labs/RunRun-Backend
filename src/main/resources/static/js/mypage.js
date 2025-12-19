// `src/main/resources/static/js/mypage.js`

document.addEventListener("DOMContentLoaded", () => {
    console.log("mypage.js loaded");
    attachProfileEditHandler();
    attachProfileImageClickHandler();
    loadMyBodyInfo();
});

async function loadMyBodyInfo() {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) return;

        const res = await fetch("/users/me", {
            headers: {"Authorization": `Bearer ${token}`}
        });

        if (!res.ok) throw new Error("조회 실패");

        const user = await res.json();
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

    // 접근성/커서 UX (선택)
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

/**
 * 마크업 매칭:
 * \- 이미지: `img[data-role="profile-preview"]`
 * \- 이니셜: `span[data-role="profile-initial"]`
 * 요구사항:
 * \- profileImageUrl 있으면: 이미지 표시
 * \- 없으면: 아무것도 표시 안 함(빈 원/빈 영역)
 */
function renderProfileImage(user) {
    const imgEl = document.querySelector('img[data-role="profile-preview"]');
    const initialEl = document.querySelector('span[data-role="profile-initial"]');

    if (!imgEl) return;

    const url = user?.profileImageUrl;

    if (!url) {
        // 아무것도 표시하지 않음: 이미지 숨기고, 이니셜도 숨김
        imgEl.removeAttribute("src");
        imgEl.hidden = true;

        if (initialEl) {
            initialEl.textContent = "";
            initialEl.hidden = true;
        }
        return;
    }

    // url 있으면 이미지 표시, 이니셜은 숨김
    imgEl.src = url;
    imgEl.alt = "프로필 이미지";
    imgEl.decoding = "async";
    imgEl.loading = "lazy";
    imgEl.hidden = false;

    if (initialEl) {
        initialEl.textContent = "";
        initialEl.hidden = true;
    }

    // 로드 실패 시에도 "아무것도 표시하지 않음"
    imgEl.addEventListener("error", () => {
        imgEl.removeAttribute("src");
        imgEl.hidden = true;

        if (initialEl) {
            initialEl.textContent = "";
            initialEl.hidden = true;
        }
    }, {once: true});
}