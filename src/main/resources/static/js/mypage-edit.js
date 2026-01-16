document.addEventListener("DOMContentLoaded", () => {
    const backButton = document.querySelector(".back-button");
    const bottomNavMount = document.getElementById("bottomNavMount");
    const bottomNavTemplate = document.getElementById("bottomNavTemplate");
    const editForm = document.querySelector('form[data-form="mypage-edit"]');
    const messageBox = document.querySelector('[data-role="edit-message"]');
    const profilePreview = document.querySelector('[data-role="profile-preview"]');
    const profileInitial = document.querySelector('[data-role="profile-initial"]');
    const defaultProfile = document.querySelector('[data-role="default-profile"]');
    const profileImageInput = document.getElementById("profileImageInput");

    const fieldOrder = [
        "userEmail",
        "userName",
        "heightCm",
        "weightKg",
    ];

    const optionalFields = new Set();
    const CLIENT_MAX_IMAGE_SIZE = 1 * 1024 * 1024; // 1MB

    let currentProfileImageUrl = "";
    let selectedFile = null;

    const validators = {
        userEmail: (value) => {
            if (!value) return "이메일을 입력해 주세요.";
            if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
                return "올바른 이메일 형식을 입력해 주세요.";
            }
            return "";
        },
        userName: (value) => {
            if (!value) return "이름을 입력해 주세요.";
            if (value.length > 4) {
                return "이름은 최대 4자까지 입력할 수 있습니다.";
            }
            return "";
        },
        heightCm: (value) => {
            if (!value) return "키를 입력해 주세요.";
            const n = Number(value);
            if (!Number.isFinite(n)) return "키는 숫자만 입력할 수 있습니다.";
            if (n < 50 || n > 300) {
                return "키는 50cm 이상 300cm 이하로 입력해 주세요.";
            }
            return "";
        },
        weightKg: (value) => {
            if (!value) return "몸무게를 입력해 주세요.";
            const n = Number(value);
            if (!Number.isFinite(n)) return "몸무게는 숫자만 입력할 수 있습니다.";
            if (n < 10 || n > 500) {
                return "몸무게는 10kg 이상 500kg 이하로 입력해 주세요.";
            }
            return "";
        },
    };

    if (backButton) {
        backButton.addEventListener("click", () => {
            if (window.history.length > 1) {
                window.history.back();
            } else {
                window.location.href = "/myPage";
            }
        });
    }


    function enableProfileClickToChoose() {
        const trigger = (e) => {
            e.preventDefault();
            profileImageInput?.click();
        };
        if (profilePreview) profilePreview.addEventListener("click", trigger);
        if (profileInitial) profileInitial.addEventListener("click", trigger);
        if (defaultProfile) defaultProfile.addEventListener("click", trigger);
    }


    function handleProfileFileChange(event) {
        const files = event.target.files;
        if (!files || files.length === 0) return;
        const file = files[0];


        if (!file.type || !file.type.startsWith("image/")) {
            setMessage("이미지 파일만 업로드할 수 있습니다.");
            profileImageInput.value = "";
            return;
        }
        if (file.size > CLIENT_MAX_IMAGE_SIZE) {
            setMessage("이미지 크기는 1MB 이하만 업로드할 수 있습니다.");
            profileImageInput.value = "";
            return;
        }


        selectedFile = file;


        try {
            const objectUrl = URL.createObjectURL(file);
            if (profilePreview) {
                profilePreview.src = objectUrl;
                profilePreview.hidden = false;
            }
            if (profileInitial) profileInitial.hidden = true;
            if (defaultProfile) defaultProfile.hidden = true;
            setMessage("이미지가 선택되었습니다. 하단 저장 버튼을 눌러주세요.", "success");
        } catch (e) {

        }
    }

    if (editForm) {
        enableRealtimeValidation(editForm);
        loadCurrentUser();

        // BMI 계산 및 표시를 위한 이벤트 리스너 추가
        const heightInput = editForm.querySelector('input[name="heightCm"]');
        const weightInput = editForm.querySelector('input[name="weightKg"]');
        
        if (heightInput && weightInput) {
            const updateBMI = () => {
                const height = parseFloat(heightInput.value);
                const weight = parseFloat(weightInput.value);
                updateBMIDisplay(height, weight);
            };
            
            heightInput.addEventListener("input", updateBMI);
            weightInput.addEventListener("input", updateBMI);
        }

        enableProfileClickToChoose();
        if (profileImageInput) {
            profileImageInput.addEventListener("change", handleProfileFileChange);
        }

        editForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            
            const submitBtn = editForm.querySelector('button[type="submit"]');
            
            // 이미 처리 중이면 무시
            if (submitBtn?.hasAttribute("disabled")) {
                return;
            }
            
            const formData = new FormData(editForm);
            const values = collectFormValues(formData);
            const {valid, message} = validateAllFields(values);
            decorateAllFields(values);

            if (!valid) {
                setMessage(message);
                return;
            }


            const payloadToSend = {
                userEmail: values.userEmail,
                userName: values.userName,
                heightCm: Number(values.heightCm),
                weightKg: Number(values.weightKg),

                profileImageUrl: selectedFile ? null : currentProfileImageUrl
            };

            const accessToken = localStorage.getItem("accessToken");
            if (!accessToken) {
                setMessage("로그인이 필요합니다. 다시 로그인해 주세요.");
                return;
            }

            // 유효성 검사 통과 후 버튼 비활성화 및 로딩 표시
            submitBtn?.setAttribute("disabled", "true");
            const originalText = submitBtn?.textContent;
            if (submitBtn) {
                submitBtn.textContent = "저장 중...";
                submitBtn.style.opacity = "0.6";
                submitBtn.style.cursor = "not-allowed";
            }
            setMessage("내 정보를 저장 중입니다...");

            try {

                const submissionData = new FormData();

                const jsonBlob = new Blob([JSON.stringify(payloadToSend)], {
                    type: "application/json"
                });
                submissionData.append("request", jsonBlob);


                if (selectedFile) {
                    submissionData.append("file", selectedFile);
                }

                const response = await fetch("/users", {
                    method: "PUT",
                    headers: {

                        Authorization: `Bearer ${accessToken}`,
                    },
                    body: submissionData,
                });

                if (!response.ok) {
                    let errorMessage = "내 정보를 수정하는 중 오류가 발생했습니다.";
                    try {
                        const payload = await response.json();
                        if (payload?.message) {
                            errorMessage = payload.message;
                        }
                    } catch (e) {

                    }
                    throw new Error(errorMessage);
                }

                setMessage("변경 사항이 저장되었습니다.", "success");
                setTimeout(() => {
                    window.location.href = "/myPage";
                }, 800);
            } catch (error) {
                setMessage(error.message || "내 정보를 수정하는 중 오류가 발생했습니다.");
                // 에러 발생 시 버튼 다시 활성화
                submitBtn?.removeAttribute("disabled");
                if (submitBtn && originalText) {
                    submitBtn.textContent = originalText;
                    submitBtn.style.opacity = "1";
                    submitBtn.style.cursor = "pointer";
                }
            } finally {
                // 성공 시에는 페이지 이동하므로 finally에서 처리 불필요
                // 하지만 안전을 위해 남겨둠 (에러 발생 시에만 활성화)
            }
        });
    }


    if (
        bottomNavMount &&
        bottomNavMount.childElementCount === 0 &&
        bottomNavTemplate
    ) {
        const clone = bottomNavTemplate.content.firstElementChild.cloneNode(true);
        bottomNavMount.replaceWith(clone);
    }

    async function loadCurrentUser() {
        const accessToken = localStorage.getItem("accessToken");
        if (!accessToken) {
            setMessage("로그인이 필요합니다. 다시 로그인해 주세요.");
            return;
        }

        try {
            const response = await fetch("/users", {
                headers: {
                    Authorization: `Bearer ${accessToken}`,
                },
            });

            if (!response.ok) {
                throw new Error("내 정보를 불러오지 못했습니다.");
            }

            const payload = await response.json();
            const data = payload?.data ?? null;
            currentProfileImageUrl = data?.profileImageUrl || "";
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

        if (emailInput) emailInput.value = user?.email ?? "";
        if (nameInput) nameInput.value = user?.name ?? "";
        if (heightInput) heightInput.value = user?.heightCm ?? "";
        if (weightInput) weightInput.value = user?.weightKg ?? "";

        updateProfileVisual(user?.name, user?.profileImageUrl);
        
        // BMI 표시 업데이트
        const height = user?.heightCm ? parseFloat(user.heightCm) : null;
        const weight = user?.weightKg ? parseFloat(user.weightKg) : null;
        updateBMIDisplay(height, weight);
    }

    function updateProfileVisual(name, imageUrl) {
        if (!profilePreview || !profileInitial) return;

        if (imageUrl) {
            profilePreview.src = imageUrl;
            profilePreview.hidden = false;
            profileInitial.hidden = true;
            if (defaultProfile) defaultProfile.hidden = true;
        } else {
            profilePreview.hidden = true;
            if (defaultProfile) {
                defaultProfile.hidden = false;
                profileInitial.hidden = true;
            } else {
                profileInitial.hidden = false;
                profileInitial.textContent = makeInitial(name);
            }
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
            const eventName = "input";
            input.addEventListener(eventName, () => {
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
        if (type === "success") {
            messageBox.classList.add("success");
        }
    }

    function validateAllFields(values) {
        for (const name of fieldOrder) {
            const {valid, message} = evaluateField(name, values);
            if (!valid) {
                return {valid: false, message};
            }
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
        if (!validator) {
            return {valid: true, message: ""};
        }
        const message = validator(values[name], values);
        return {valid: !message, message};
    }

    function setInputState(input, state) {
        input.classList.remove("input-valid", "input-invalid");
        if (!state) {
            return;
        }
        input.classList.add(state === "valid" ? "input-valid" : "input-invalid");
    }

    function setFieldMessage(name, message) {
        const feedback = document.querySelector(`[data-feedback="${name}"]`);
        if (!feedback) return;
        feedback.textContent = message;
        feedback.classList.toggle("hidden", !message);
    }

    /**
     * BMI 계산
     */
    function calculateBMI(heightCm, weightKg) {
        if (!heightCm || !weightKg || heightCm <= 0 || weightKg <= 0) {
            return null;
        }
        const heightM = heightCm / 100;
        return weightKg / (heightM * heightM);
    }

    /**
     * BMI 분류 (대한민국 기준)
     */
    function getBMICategory(bmi) {
        if (bmi === null || isNaN(bmi)) {
            return null;
        }
        
        if (bmi < 18.5) {
            return {
                status: "underweight",
                text: "저체중입니다"
            };
        } else if (bmi < 23) {
            return {
                status: "normal",
                text: "정상체중입니다"
            };
        } else if (bmi < 25) {
            return {
                status: "overweight",
                text: "과체중입니다"
            };
        } else {
            return {
                status: "obese",
                text: "비만입니다"
            };
        }
    }

    /**
     * BMI 표시 업데이트
     */
    function updateBMIDisplay(heightCm, weightKg) {
        const bmiDisplay = document.getElementById("bmiDisplay");
        const bmiValue = document.getElementById("bmiValue");
        const bmiStatus = document.getElementById("bmiStatus");
        
        if (!bmiDisplay || !bmiValue || !bmiStatus) return;

        const bmi = calculateBMI(heightCm, weightKg);
        
        if (bmi === null) {
            bmiDisplay.hidden = true;
            return;
        }

        bmiDisplay.hidden = false;
        bmiValue.textContent = bmi.toFixed(1);
        
        const category = getBMICategory(bmi);
        if (category) {
            bmiStatus.textContent = category.text;
            bmiStatus.className = `bmi-status ${category.status}`;
        } else {
            bmiStatus.textContent = "";
            bmiStatus.className = "bmi-status";
        }
    }
});