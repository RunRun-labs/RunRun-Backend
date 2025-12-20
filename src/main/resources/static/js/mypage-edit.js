document.addEventListener("DOMContentLoaded", () => {
    const backButton = document.querySelector(".back-button");
    const bottomNavMount = document.getElementById("bottomNavMount");
    const bottomNavTemplate = document.getElementById("bottomNavTemplate");
    const editForm = document.querySelector('form[data-form="mypage-edit"]');
    const messageBox = document.querySelector('[data-role="edit-message"]');
    const profilePreview = document.querySelector('[data-role="profile-preview"]');
    const profileInitial = document.querySelector('[data-role="profile-initial"]');

    const avatarClickArea = document.querySelector(".profile-avatar");
    const profileImageInput = document.getElementById("profileImageInput");

    // 프론트 파일 사이즈 제한 (1MB)
    const MAX_FILE_SIZE = 1 * 1024 * 1024; // 1MB

    // STEP 1) 선택된 파일을 "기억"할 전역 변수(이 스코프에서 공유)
    let selectedProfileImageFile = null;

    // 이미지 변경 X 시 기존 URL을 유지하기 위해, 현재 유저의 profileImageUrl을 기억
    let currentProfileImageUrl = null;

    const fieldOrder = ["userEmail", "userName", "heightCm", "weightKg"];
    const optionalFields = new Set();

    // heightCm와 weightKg는 DTO에 @Min/@Max만 있고 @NotBlank/@NotNull이 없으므로
    // 프론트에서는 선택적 필드로 처리합니다 (빈값 허용). 이 셋업은 DTO 규칙과 일치합니다.
    optionalFields.add("heightCm");
    optionalFields.add("weightKg");

    const validators = {
        userEmail: (value) => {
            // DTO: @NotBlank(message = "이메일은 필수 입력 사항입니다")
            // DTO: @Email(message = "이메일 형식을 유지해야 합니다")
            if (!value) return "이메일은 필수 입력 사항입니다";
            if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
                return "이메일 형식을 유지해야 합니다";
            }
            return "";
        },
        userName: (value) => {
            // DTO: @NotBlank(message = "이름은 필수 입력 사항입니다")
            // DTO: @Size(max = 4, message = "이름은 최대 4자여야 합니다.")
            if (!value) return "이름은 필수 입력 사항입니다";
            if (value.length > 4) return "이름은 최대 4자여야 합니다.";
            return "";
        },
        heightCm: (value) => {
            // DTO에는 NotBlank가 없으므로 빈값 허용
            if (!value) return "";
            const n = Number(value);
            if (!Number.isFinite(n)) return "키는 숫자만 입력할 수 있습니다.";
            if (n < 50) return "키는 50cm 이상이어야 합니다.";
            if (n > 300) return "키는 300cm 이하이어야 합니다.";
            return "";
        },
        weightKg: (value) => {
            // DTO에는 NotBlank가 없으므로 빈값 허용
            if (!value) return "";
            const n = Number(value);
            if (!Number.isFinite(n)) return "몸무게는 숫자만 입력할 수 있습니다.";
            if (n < 10) return "몸무게는 10kg 이상이어야 합니다.";
            if (n > 500) return "몸무게는 500kg 이하이어야 합니다.";
            return "";
        },
    };

    if (backButton) {
        backButton.addEventListener("click", () => {
            if (window.history.length > 1) window.history.back();
            else window.location.href = "/myPage";
        });
    }

    attachProfileImageSelectHandler();
    attachProfileImagePreviewHandler();

    if (editForm) {
        enableRealtimeValidation(editForm);
        loadCurrentUser();

        editForm.addEventListener("submit", async (event) => {
            event.preventDefault();

            const formData = new FormData(editForm);
            const values = collectFormValues(formData);
            const {valid, message} = validateAllFields(values);
            decorateAllFields(values);

            if (!valid) {
                setMessage(message);
                return;
            }

            const accessToken = localStorage.getItem("accessToken");
            if (!accessToken) {
                setMessage("로그인이 필요합니다. 다시 로그인해 주세요.");
                return;
            }

            const submitBtn = editForm.querySelector('button[type="submit"]');
            submitBtn?.setAttribute("disabled", "true");

            try {
                // STEP 3) submit 로직에 업로드 끼워넣기
                // 이미지 변경 O: 업로드 API 1번 호출해서 URL 받기
                // 이미지 변경 X: 업로드 API 호출 안 함, 기존 URL 유지
                let profileImageUrlToSave = currentProfileImageUrl;

                if (selectedProfileImageFile) {
                    setMessage("프로필 이미지를 업로드 중입니다...");
                    profileImageUrlToSave = await uploadProfileImage(selectedProfileImageFile, accessToken);
                }

                setMessage("내 정보를 저장 중입니다...");

                const payload = {
                    userEmail: values.userEmail,
                    userName: values.userName,
                    heightCm: values.heightCm === "" ? null : Number(values.heightCm),
                    weightKg: values.weightKg === "" ? null : Number(values.weightKg),
                    profileImageUrl: profileImageUrlToSave ?? null,
                };

                const response = await fetch("/users", {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${accessToken}`,
                    },
                    body: JSON.stringify(payload),
                });

                if (!response.ok) {
                    let errorMessage = "내 정보를 수정하는 중 오류가 발생했습니다.";
                    try {
                        const data = await response.json();
                        if (data?.message) errorMessage = data.message;
                    } catch (e) {
                        // ignore
                    }
                    throw new Error(errorMessage);
                }

                // 저장 성공 후: 마이페이지 이동
                setMessage("변경 사항이 저장되었습니다.", "success");
                setTimeout(() => (window.location.href = "/myPage"), 800);
            } catch (error) {
                setMessage(error.message || "내 정보를 수정하는 중 오류가 발생했습니다.");
            } finally {
                submitBtn?.removeAttribute("disabled");
            }
        });
    }

    if (bottomNavMount && bottomNavMount.childElementCount === 0 && bottomNavTemplate) {
        const clone = bottomNavTemplate.content.firstElementChild.cloneNode(true);
        bottomNavMount.replaceWith(clone);
    }

    function attachProfileImageSelectHandler() {
        if (!avatarClickArea || !profileImageInput) return;

        avatarClickArea.style.cursor = "pointer";
        avatarClickArea.setAttribute("role", "button");
        avatarClickArea.setAttribute("tabindex", "0");

        const openPicker = () => profileImageInput.click();

        avatarClickArea.addEventListener("click", openPicker);
        avatarClickArea.addEventListener("keydown", (e) => {
            if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                openPicker();
            }
        });
    }

    function attachProfileImagePreviewHandler() {
        if (!profileImageInput || !profilePreview || !profileInitial) return;

        profileImageInput.addEventListener("change", () => {
            const file = profileImageInput.files?.[0] ?? null;

            // 선택 취소 시: 변경 없음으로 처리
            if (!file) {
                selectedProfileImageFile = null;
                return;
            }

            // 타입 검사
            if (!file.type?.startsWith("image/")) {
                setMessage("이미지 파일만 선택할 수 있습니다.");
                profileImageInput.value = "";
                selectedProfileImageFile = null;
                return;
            }

            // 사이즈 검사 (1MB)
            if (file.size > MAX_FILE_SIZE) {
                setMessage("이미지는 최대 1MB까지 업로드할 수 있습니다.");
                profileImageInput.value = "";
                selectedProfileImageFile = null;
                return;
            }

            // STEP 1) 선택된 파일을 기억
            selectedProfileImageFile = file;

            const reader = new FileReader();
            reader.onload = () => {
                const dataUrl = reader.result?.toString() ?? "";
                if (!dataUrl) return;

                profilePreview.src = dataUrl;
                profilePreview.hidden = false;

                profileInitial.textContent = "";
                profileInitial.hidden = true;

                setMessage("");
            };

            reader.onerror = () => {
                setMessage("이미지 미리보기를 불러오지 못했습니다.");
            };

            reader.readAsDataURL(file);
        });
    }

    // STEP 2) 파일 업로드 API 호출 함수
    // \- 팀 FileStorage(LocalFileStorage)가 반환하는 URL 형식: `/files/profile/{refId}/{fileName}`
    // \- 실제 업로드 컨트롤러 엔드포인트에 맞게 URL만 조정 필요
    async function uploadProfileImage(file, accessToken) {
        const fd = new FormData();
        fd.append("file", file);

        // 서버 설계에 따라 필요할 수 있는 값들 (없으면 지워도 됨)
        fd.append("domainType", "PROFILE");
        // refId를 서버가 토큰에서 추출하면 불필요. 필요 시 userId를 넣도록 수정.
        // fd.append("refId", String(userId));

        const res = await fetch("/files/upload", {
            method: "POST",
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
            body: fd,
        });

        if (!res.ok) {
            let msg = "이미지 업로드에 실패했습니다.";
            try {
                const data = await res.json();
                if (data?.message) msg = data.message;
            } catch (e) {
                // ignore
            }
            throw new Error(msg);
        }

        // 응답이 `{ url: "..." }` 또는 문자열일 수 있어 둘 다 처리
        const contentType = res.headers.get("content-type") ?? "";
        if (contentType.includes("application/json")) {
            const data = await res.json();
            const url = data?.url ?? data?.fileUrl ?? data?.profileImageUrl;
            if (!url) throw new Error("업로드 응답에 imageUrl이 없습니다.");
            return url;
        }

        const text = (await res.text()).trim();
        if (!text) throw new Error("업로드 응답이 비어 있습니다.");
        return text;
    }

    async function loadCurrentUser() {
        const accessToken = localStorage.getItem("accessToken");
        if (!accessToken) {
            setMessage("로그인이 필요합니다. 다시 로그인해 주세요.");
            return;
        }

        try {
            const response = await fetch("/users", {
                headers: {Authorization: `Bearer ${accessToken}`},
            });

            if (!response.ok) throw new Error("내 정보를 불러오지 못했습니다.");

            const data = await response.json();
            fillFormWithUserData(data);
        } catch (error) {
            setMessage(error.message || "내 정보를 불러오는 중 오류가 발생했습니다.");
        }
    }

    function fillFormWithUserData(user) {
        if (!editForm) return;

        const emailInput = editForm.querySelector('input[name="userEmail"]');
        const nameInput = editForm.querySelector('input[name="userName"]');
        const heightInput = editForm.querySelector('input[name="heightCm"]');
        const weightInput = editForm.querySelector('input[name="weightKg"]');

        if (emailInput) emailInput.value = user.email ?? "";
        if (nameInput) nameInput.value = user.name ?? "";
        if (heightInput) heightInput.value = user.heightCm ?? "";
        if (weightInput) weightInput.value = user.weightKg ?? "";

        // 기존 URL 기억 (시나리오 B에서 유지)
        currentProfileImageUrl = user.profileImageUrl ?? null;

        updateProfileVisual(user.name, user.profileImageUrl);
    }

    function updateProfileVisual(name, imageUrl) {
        if (!profilePreview || !profileInitial) return;

        if (imageUrl) {
            profilePreview.src = imageUrl;
            profilePreview.hidden = false;
            profileInitial.hidden = true;
        } else {
            profilePreview.hidden = true;
            profileInitial.hidden = false;
            profileInitial.textContent = makeInitial(name);
        }
    }

    function makeInitial(name) {
        if (!name) return "RR";
        const trimmed = name.toString().trim();
        return trimmed ? trimmed.charAt(0) : "RR";
    }

    function enableRealtimeValidation(form) {
        fieldOrder.forEach((name) => {
            const input = form.querySelector(`[name="${name}"]`);
            if (!input) return;
            input.addEventListener("input", () => {
                const values = collectFormValues(new FormData(form));
                validateAndDecorateField(name, values);
                setMessage("");
            });
        });
    }

    function collectFormValues(formData) {
        return {
            userEmail: getTrimmedValue(formData, "userEmail"),
            userName: getTrimmedValue(formData, "userName"),
            heightCm: getTrimmedValue(formData, "heightCm"),
            weightKg: getTrimmedValue(formData, "weightKg"),
        };
    }

    function getTrimmedValue(formData, key) {
        const raw = formData.get(key);
        return raw === null ? "" : raw.toString().trim();
    }

    function setMessage(message, type = "error") {
        if (!messageBox) return;
        messageBox.textContent = message;
        messageBox.hidden = !message;
        messageBox.classList.remove("success");
        if (type === "success") messageBox.classList.add("success");
    }

    function validateAllFields(values) {
        for (const name of fieldOrder) {
            const {valid, message} = evaluateField(name, values);
            if (!valid) return {valid: false, message};
        }
        return {valid: true, message: ""};
    }

    function validateAndDecorateField(name, values) {
        const input =
            editForm?.querySelector(`[name="${name}"]`) ??
            document.querySelector(`[name="${name}"]`);
        const {valid, message} = evaluateField(name, values);

        const value = values[name];
        const shouldDecorate =
            !optionalFields.has(name) || (value !== undefined && value !== "");

        if (input) {
            setInputState(input, shouldDecorate ? (valid ? "valid" : "invalid") : "");
        }

        setFieldMessage(name, shouldDecorate ? (valid ? "" : message) : "");
        return {valid, message};
    }

    function decorateAllFields(values) {
        fieldOrder.forEach((name) => validateAndDecorateField(name, values));
    }

    function evaluateField(name, values) {
        const validator = validators[name];
        if (!validator) return {valid: true, message: ""};
        const message = validator(values[name], values);
        return {valid: !message, message};
    }

    function setInputState(input, state) {
        input.classList.remove("input-valid", "input-invalid");
        if (!state) return;
        input.classList.add(state === "valid" ? "input-valid" : "input-invalid");
    }

    function setFieldMessage(name, message) {
        const feedback = document.querySelector(`[data-feedback="${name}"]`);
        if (!feedback) return;
        feedback.textContent = message;
        feedback.classList.toggle("hidden", !message);
    }
});