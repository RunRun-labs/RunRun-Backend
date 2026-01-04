document.addEventListener("DOMContentLoaded", () => {
    console.log("mypage.js loaded");
    attachProfileEditHandler();
    attachProfileImageClickHandler();
    attachChallengeHandler();
    attachFriendHandler();
    attachSettingsHandler();
    attachMyCoursesHandler();
    attachMyPostsHandler();
    loadMyBodyInfo();
    loadRunningRecords(); // 추후 API 연동 시 사용
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

function attachFriendHandler() {
    const friendBtn = document.querySelector('.profile-actions .action-pill:nth-child(2)');
    if (!friendBtn) return;

    friendBtn.addEventListener("click", () => {
        window.location.href = "/friends/list";
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

function attachMyCoursesHandler() {
    const myCoursesBtn = document.querySelector('[data-role="my-courses"]');
    const modal = document.querySelector('[data-role="course-modal"]');
    const modalOverlay = document.querySelector('[data-role="course-modal-overlay"]');
    const modalClose = document.querySelector('[data-role="course-modal-close"]');
    const courseOptions = document.querySelectorAll('[data-role="course-option"]');

    if (!myCoursesBtn || !modal) return;

    // 모달 열기
    myCoursesBtn.addEventListener("click", () => {
        modal.classList.add("active");
        document.body.style.overflow = "hidden";
    });

    // 모달 닫기
    const closeModal = () => {
        modal.classList.remove("active");
        document.body.style.overflow = "";
    };

    if (modalOverlay) {
        modalOverlay.addEventListener("click", closeModal);
    }

    if (modalClose) {
        modalClose.addEventListener("click", closeModal);
    }

    // ESC 키로 모달 닫기
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && modal.classList.contains("active")) {
            closeModal();
        }
    });

    // 코스 옵션 클릭 처리
    courseOptions.forEach((option) => {
        option.addEventListener("click", () => {
            const type = option.getAttribute("data-type");
            let url = "/course";

            // 타입에 따라 쿼리 파라미터 추가 (나중에 필터 기능 구현 시 사용)
            switch (type) {
                case "liked":
                    url = "/course?filter=liked";
                    break;
                case "favorited":
                    url = "/course?filter=favorited";
                    break;
                case "my":
                    url = "/course?filter=my";
                    break;
            }

            window.location.href = url;
        });
    });
}

function attachMyPostsHandler() {
    const myPostsBtn = document.querySelector('[data-role="my-posts"]');
    if (!myPostsBtn) return;

    myPostsBtn.addEventListener("click", () => {
        // 피드 기능 구현 후 연동 예정
        alert("피드 기능 구현 후 연동 예정입니다.");
    });
}

/**
 * 러닝 타입을 한국어로 변환
 * RunningType enum 참고: SOLO("솔로"), OFFLINE("오프라인"), ONLINEBATTLE("온라인배틀"), GHOST("고스트")
 */
function getRunningTypeLabel(runningType) {
    const typeMap = {
        SOLO: "솔로",
        OFFLINE: "오프라인",
        ONLINEBATTLE: "온라인배틀",
        GHOST: "고스트"
    };
    return typeMap[runningType] || runningType || "-";
}

/**
 * 러닝 기록 로드 (추후 API 연동 예정)
 */
async function loadRunningRecords() {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            // 로그인하지 않은 경우 기본값 유지
            return;
        }

        // TODO: 실제 API 엔드포인트로 교체 예정
        // const res = await fetch("/api/running-results", {
        //     headers: { Authorization: `Bearer ${token}` }
        // });
        // if (!res.ok) throw new Error("러닝 기록 조회 실패");
        // const payload = await res.json();
        // const records = payload?.data ?? [];
        // renderRunningRecords(records);

    } catch (e) {
        console.error("러닝 기록 로드 실패:", e);
    }
}

/**
 * 러닝 기록 렌더링
 */
function renderRunningRecords(records) {
    const runCards = document.querySelectorAll('.run-card');
    
    runCards.forEach((card, index) => {
        const record = records[index];
        if (!record) return;

        const runTypeEl = card.querySelector('[data-role="run-type"]');
        if (runTypeEl && record.runningType) {
            runTypeEl.textContent = getRunningTypeLabel(record.runningType);
        }
    });
}