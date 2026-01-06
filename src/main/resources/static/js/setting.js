document.addEventListener("DOMContentLoaded", () => {
    console.log("setting.js loaded");
    attachBackButtonHandler();
    attachMenuHandlers();
    loadUserSettings();
    
    // URL 파라미터 확인하여 약관 모달 자동 열기
    checkAndOpenTermsModal();
});

// URL 파라미터 확인하여 약관 모달 자동 열기
function checkAndOpenTermsModal() {
    const urlParams = new URLSearchParams(window.location.search);
    const openTermsModal = urlParams.get('openTermsModal');
    
    if (openTermsModal === 'true') {
        // URL에서 파라미터 제거 (히스토리 정리)
        const newUrl = window.location.pathname;
        window.history.replaceState({}, '', newUrl);
        
        // 약관 모달 열기
        setTimeout(() => {
            openTermsModal();
        }, 100);
    }
}

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
    const profileVisibilityBtn = document.querySelector('[data-role="profile-visibility"]');
    const blockedUsersBtn = document.querySelector('[data-role="blocked-users"]');
    const logoutBtn = document.querySelector('[data-role="logout"]');
    const accountDeleteBtn = document.querySelector('[data-role="account-delete"]');
    const termsBtn = document.querySelector('[data-role="terms"]');
    const ttsSettingsBtn = document.querySelector('[data-role="tts-settings"]');
    const modalCloseBtn = document.querySelector(".modal-close");
    const saveSettingsBtn = document.querySelector('[data-role="save-settings"]');
    const saveVisibilitySettingsBtn = document.querySelector('[data-role="save-visibility-settings"]');

    // 알림 설정 모달 열기
    if (notificationSettingsBtn) {
        notificationSettingsBtn.addEventListener("click", () => {
            openNotificationModal();
        });
    }

    // 공개 범위 설정 모달 열기
    if (profileVisibilityBtn) {
        profileVisibilityBtn.addEventListener("click", () => {
            openProfileVisibilityModal();
        });
    }

    // 차단한 사용자 관리
    if (blockedUsersBtn) {
        blockedUsersBtn.addEventListener("click", () => {
            window.location.href = "/setting/blocked-users";
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
            openAccountDeleteModal();
        });
    }

    // 약관/정책 확인
    if (termsBtn) {
        termsBtn.addEventListener("click", () => {
            openTermsModal();
        });
    }

    // TTS 설정
    if (ttsSettingsBtn) {
        ttsSettingsBtn.addEventListener("click", () => {
            // TODO: TTS 설정 페이지로 이동
            alert("TTS 설정 기능은 준비 중입니다.");
        });
    }

    // 알림 설정 모달 닫기
    const notificationModalCloseBtn = document.querySelector('[data-role="close-notification-modal"]');
    if (notificationModalCloseBtn) {
        notificationModalCloseBtn.addEventListener("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            closeNotificationModal();
        });
    }

    // 알림 설정 모달 배경 클릭 시 닫기
    const notificationModalOverlay = document.querySelector('[data-role="notification-modal"]');
    if (notificationModalOverlay) {
        notificationModalOverlay.addEventListener("click", (e) => {
            if (e.target === notificationModalOverlay) {
                closeNotificationModal();
            }
        });

        // 모달 콘텐츠 클릭 시 닫히지 않도록
        const modalContent = notificationModalOverlay.querySelector(".modal-content");
        if (modalContent) {
            modalContent.addEventListener("click", (e) => {
                e.stopPropagation();
            });
        }
    }

    // 약관/정책 모달 닫기
    const termsModalCloseBtn = document.querySelector('[data-role="close-terms-modal"]');
    if (termsModalCloseBtn) {
        termsModalCloseBtn.addEventListener("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            closeTermsModal();
        });
    }

    // 약관/정책 모달 배경 클릭 시 닫기
    const termsModalOverlay = document.querySelector('[data-role="terms-modal"]');
    if (termsModalOverlay) {
        termsModalOverlay.addEventListener("click", (e) => {
            if (e.target === termsModalOverlay) {
                closeTermsModal();
            }
        });

        // 모달 콘텐츠 클릭 시 닫히지 않도록
        const termsModalContent = termsModalOverlay.querySelector(".modal-content");
        if (termsModalContent) {
            termsModalContent.addEventListener("click", (e) => {
                e.stopPropagation();
            });
        }
    }

    // 약관 옵션 클릭 핸들러
    const termsServiceBtn = document.querySelector('[data-role="terms-service"]');
    if (termsServiceBtn) {
        termsServiceBtn.addEventListener("click", () => {
            window.location.href = "/terms/view?type=SERVICE";
        });
    }

    const termsPrivacyBtn = document.querySelector('[data-role="terms-privacy"]');
    if (termsPrivacyBtn) {
        termsPrivacyBtn.addEventListener("click", () => {
            window.location.href = "/terms/view?type=PRIVACY";
        });
    }

    const termsMarketingBtn = document.querySelector('[data-role="terms-marketing"]');
    if (termsMarketingBtn) {
        termsMarketingBtn.addEventListener("click", () => {
            window.location.href = "/terms/view?type=MARKETING";
        });
    }

    // 계정 탈퇴 모달 닫기
    const accountDeleteModalCloseBtn = document.querySelector('[data-role="close-account-delete-modal"]');
    if (accountDeleteModalCloseBtn) {
        accountDeleteModalCloseBtn.addEventListener("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            closeAccountDeleteModal();
        });
    }

    // 계정 탈퇴 모달 배경 클릭 시 닫기
    const accountDeleteModalOverlay = document.querySelector('[data-role="account-delete-modal"]');
    if (accountDeleteModalOverlay) {
        accountDeleteModalOverlay.addEventListener("click", (e) => {
            if (e.target === accountDeleteModalOverlay) {
                closeAccountDeleteModal();
            }
        });

        // 모달 콘텐츠 클릭 시 닫히지 않도록
        const accountDeleteModalContent = accountDeleteModalOverlay.querySelector(".modal-content");
        if (accountDeleteModalContent) {
            accountDeleteModalContent.addEventListener("click", (e) => {
                e.stopPropagation();
            });
        }
    }

    // 계정 탈퇴 취소 버튼
    const cancelDeleteBtn = document.querySelector('[data-role="cancel-delete"]');
    if (cancelDeleteBtn) {
        cancelDeleteBtn.addEventListener("click", () => {
            closeAccountDeleteModal();
        });
    }

    // 계정 탈퇴 확인 버튼
    const confirmDeleteBtn = document.querySelector('[data-role="confirm-delete"]');
    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener("click", async () => {
            await handleAccountDelete();
        });
    }

    // 설정 저장
    if (saveSettingsBtn) {
        saveSettingsBtn.addEventListener("click", async () => {
            await saveUserSettings();
        });
    }

    // 공개 범위 설정 저장
    if (saveVisibilitySettingsBtn) {
        saveVisibilitySettingsBtn.addEventListener("click", async () => {
            await saveProfileVisibilitySettings();
        });
    }

    // 공개 범위 설정 모달 닫기
    const profileVisibilityModalCloseBtn = document.querySelector('[data-role="close-profile-visibility-modal"]');
    if (profileVisibilityModalCloseBtn) {
        profileVisibilityModalCloseBtn.addEventListener("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            closeProfileVisibilityModal();
        });
    }

    // 공개 범위 설정 모달 배경 클릭 시 닫기
    const profileVisibilityModalOverlay = document.querySelector('[data-role="profile-visibility-modal"]');
    if (profileVisibilityModalOverlay) {
        profileVisibilityModalOverlay.addEventListener("click", (e) => {
            if (e.target === profileVisibilityModalOverlay) {
                closeProfileVisibilityModal();
            }
        });

        // 모달 콘텐츠 클릭 시 닫히지 않도록
        const profileVisibilityModalContent = profileVisibilityModalOverlay.querySelector(".modal-content");
        if (profileVisibilityModalContent) {
            profileVisibilityModalContent.addEventListener("click", (e) => {
                e.stopPropagation();
            });
        }
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
            updateProfileVisibilityRadio(settings);
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

// 약관/정책 모달 열기
function openTermsModal() {
    const modal = document.querySelector('[data-role="terms-modal"]');
    if (modal) {
        modal.removeAttribute("hidden");
        document.body.style.overflow = "hidden";
    }
}

// 약관/정책 모달 닫기
function closeTermsModal() {
    const modal = document.querySelector('[data-role="terms-modal"]');
    if (modal) {
        modal.setAttribute("hidden", "hidden");
        document.body.style.overflow = "";
    }
}

// 공개 범위 설정 모달 열기
function openProfileVisibilityModal() {
    const modal = document.querySelector('[data-role="profile-visibility-modal"]');
    if (modal) {
        modal.removeAttribute("hidden");
        document.body.style.overflow = "hidden";
    }
}

// 공개 범위 설정 모달 닫기
function closeProfileVisibilityModal() {
    const modal = document.querySelector('[data-role="profile-visibility-modal"]');
    if (modal) {
        modal.setAttribute("hidden", "hidden");
        document.body.style.overflow = "";
    }
}

// 계정 탈퇴 모달 열기
function openAccountDeleteModal() {
    const modal = document.querySelector('[data-role="account-delete-modal"]');
    if (modal) {
        modal.removeAttribute("hidden");
        document.body.style.overflow = "hidden";
    }
}

// 계정 탈퇴 모달 닫기
function closeAccountDeleteModal() {
    const modal = document.querySelector('[data-role="account-delete-modal"]');
    if (modal) {
        modal.setAttribute("hidden", "hidden");
        document.body.style.overflow = "";
    }
}

// 계정 탈퇴 처리
async function handleAccountDelete() {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            alert("로그인이 필요합니다.");
            window.location.href = "/login";
            return;
        }

        const confirmBtn = document.querySelector('[data-role="confirm-delete"]');
        if (confirmBtn) {
            confirmBtn.disabled = true;
            confirmBtn.textContent = "처리 중...";
        }

        const response = await fetch("/users", {
            method: "DELETE",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!response.ok) {
            if (response.status === 401) {
                alert("로그인이 필요합니다.");
                window.location.href = "/login";
                return;
            }
            const error = await response.json();
            throw new Error(error?.message || "계정 탈퇴 실패");
        }

        // 성공 시 로컬 스토리지 정리
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("userId");

        alert("회원 탈퇴가 완료되었습니다.");
        window.location.href = "/login";
    } catch (error) {
        console.error("Failed to delete account:", error);
        alert(error.message || "계정 탈퇴 중 오류가 발생했습니다.");
        
        const confirmBtn = document.querySelector('[data-role="confirm-delete"]');
        if (confirmBtn) {
            confirmBtn.disabled = false;
            confirmBtn.textContent = "예";
        }
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

// 공개 범위 라디오 버튼 상태 업데이트
function updateProfileVisibilityRadio(settings) {
    const visibility = settings.profileVisibility || "PUBLIC";
    const publicRadio = document.querySelector('[data-role="visibility-public"]');
    const friendsRadio = document.querySelector('[data-role="visibility-friends"]');
    const privateRadio = document.querySelector('[data-role="visibility-private"]');

    if (publicRadio) {
        publicRadio.checked = visibility === "PUBLIC";
    }
    if (friendsRadio) {
        friendsRadio.checked = visibility === "FRIENDS_ONLY";
    }
    if (privateRadio) {
        privateRadio.checked = visibility === "PRIVATE";
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

// 공개 범위 설정 저장
async function saveProfileVisibilitySettings() {
    try {
        const token = localStorage.getItem("accessToken");
        if (!token) {
            alert("로그인이 필요합니다.");
            window.location.href = "/login";
            return;
        }

        const selectedRadio = document.querySelector('input[name="profileVisibility"]:checked');
        if (!selectedRadio) {
            alert("공개 범위를 선택해주세요.");
            return;
        }

        const profileVisibility = selectedRadio.value;

        const response = await fetch("/users/settings", {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify({
                profileVisibility: profileVisibility
            })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error?.message || "설정 저장 실패");
        }

        alert("설정이 저장되었습니다.");
        closeProfileVisibilityModal();
    } catch (error) {
        console.error("Failed to save profile visibility settings:", error);
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

