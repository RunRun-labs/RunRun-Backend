document.addEventListener("DOMContentLoaded", () => {
    console.log("setting.js loaded");
    attachBackButtonHandler();
    attachMenuHandlers();
    loadUserSettings();
});

// 뒤로가기 버튼
function attachBackButtonHandler() {
    const backButton = document.querySelector(".back-button");
    if (backButton) {
        backButton.addEventListener("click", () => {
            window.history.back();
        });
    }
}

// 메뉴 항목 핸들러
function attachMenuHandlers() {
    const notificationSettingsBtn = document.querySelector('[data-role="notification-settings"]');
    const blockedUsersBtn = document.querySelector('[data-role="blocked-users"]');
    const logoutBtn = document.querySelector('[data-role="logout"]');
    const accountDeleteBtn = document.querySelector('[data-role="account-delete"]');
    const termsBtn = document.querySelector('[data-role="terms"]');
    const ttsSettingsBtn = document.querySelector('[data-role="tts-settings"]');
    const modalCloseBtn = document.querySelector(".modal-close");
    const saveSettingsBtn = document.querySelector('[data-role="save-settings"]');

    // 알림 설정 모달 열기
    if (notificationSettingsBtn) {
        notificationSettingsBtn.addEventListener("click", () => {
            openNotificationModal();
        });
    }

    // 차단한 사용자 관리
    if (blockedUsersBtn) {
        blockedUsersBtn.addEventListener("click", () => {
            // TODO: 차단한 사용자 관리 페이지로 이동
            alert("차단한 사용자 관리 기능은 준비 중입니다.");
        });
    }

    // 로그아웃
    if (logoutBtn) {
        logoutBtn.addEventListener("click", async () => {
            if (confirm("로그아웃 하시겠습니까?")) {
                await handleLogout();
            }
        });
    }

    // 계정 탈퇴
    if (accountDeleteBtn) {
        accountDeleteBtn.addEventListener("click", () => {
            // TODO: 계정 탈퇴 기능 구현
            alert("계정 탈퇴 기능은 준비 중입니다.");
        });
    }

    // 약관/정책 확인
    if (termsBtn) {
        termsBtn.addEventListener("click", () => {
            // TODO: 약관/정책 페이지로 이동
            alert("약관/정책 확인 기능은 준비 중입니다.");
        });
    }

    // TTS 설정
    if (ttsSettingsBtn) {
        ttsSettingsBtn.addEventListener("click", () => {
            // TODO: TTS 설정 페이지로 이동
            alert("TTS 설정 기능은 준비 중입니다.");
        });
    }

    // 모달 닫기
    if (modalCloseBtn) {
        modalCloseBtn.addEventListener("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            closeNotificationModal();
        });
    }

    // 모달 배경 클릭 시 닫기
    const modalOverlay = document.querySelector('[data-role="notification-modal"]');
    if (modalOverlay) {
        modalOverlay.addEventListener("click", (e) => {
            if (e.target === modalOverlay) {
                closeNotificationModal();
            }
        });

        // 모달 콘텐츠 클릭 시 닫히지 않도록
        const modalContent = modalOverlay.querySelector(".modal-content");
        if (modalContent) {
            modalContent.addEventListener("click", (e) => {
                e.stopPropagation();
            });
        }
    }

    // 설정 저장
    if (saveSettingsBtn) {
        saveSettingsBtn.addEventListener("click", async () => {
            await saveUserSettings();
        });
    }
}

// 사용자 설정 로드
async function loadUserSettings() {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            console.warn("No access token found");
            return;
        }

        const response = await fetch("/users/settings", {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!response.ok) {
            throw new Error("설정 조회 실패");
        }

        const result = await response.json();
        const settings = result?.data;

        if (settings) {
            updateNotificationToggles(settings);
        }
    } catch (error) {
        console.error("Failed to load user settings:", error);
    }
}

// 알림 설정 모달 열기
function openNotificationModal() {
    const modal = document.querySelector('[data-role="notification-modal"]');
    if (modal) {
        modal.removeAttribute("hidden");
        document.body.style.overflow = "hidden";
    }
}

// 알림 설정 모달 닫기
function closeNotificationModal() {
    const modal = document.querySelector('[data-role="notification-modal"]');
    if (modal) {
        modal.setAttribute("hidden", "hidden");
        document.body.style.overflow = "";
    }
}

// 토글 상태 업데이트
function updateNotificationToggles(settings) {
    const notificationEnabledToggle = document.querySelector('[data-role="notification-enabled"]');
    const nightNotificationEnabledToggle = document.querySelector('[data-role="night-notification-enabled"]');

    if (notificationEnabledToggle) {
        notificationEnabledToggle.checked = settings.notificationEnabled || false;
    }

    if (nightNotificationEnabledToggle) {
        nightNotificationEnabledToggle.checked = settings.nightNotificationEnabled || false;
    }
}

// 사용자 설정 저장
async function saveUserSettings() {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            alert("로그인이 필요합니다.");
            window.location.href = "/login";
            return;
        }

        const notificationEnabledToggle = document.querySelector('[data-role="notification-enabled"]');
        const nightNotificationEnabledToggle = document.querySelector('[data-role="night-notification-enabled"]');

        const notificationEnabled = notificationEnabledToggle?.checked || false;
        const nightNotificationEnabled = nightNotificationEnabledToggle?.checked || false;

        const response = await fetch("/users/settings", {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify({
                notificationEnabled: notificationEnabled,
                nightNotificationEnabled: nightNotificationEnabled
            })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error?.message || "설정 저장 실패");
        }

        alert("설정이 저장되었습니다.");
        closeNotificationModal();
    } catch (error) {
        console.error("Failed to save user settings:", error);
        alert(error.message || "설정 저장 중 오류가 발생했습니다.");
    }
}

// 로그아웃 처리
async function handleLogout() {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            // 토큰이 없으면 바로 로그인 페이지로 이동
            window.location.href = "/login";
            return;
        }

        const response = await fetch("/auth/logout", {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        // 성공 여부와 관계없이 로컬 스토리지 정리
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("userId");

        if (response.ok) {
            alert("로그아웃 되었습니다.");
        }

        window.location.href = "/login";
    } catch (error) {
        console.error("Failed to logout:", error);
        // 오류가 발생해도 로컬 스토리지는 정리
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("userId");
        window.location.href = "/login";
    }
}

