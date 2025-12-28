document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("challengeCreateForm");
    const imageInput = document.getElementById("imageFile");
    const imagePreview = document.getElementById("imagePreview");
    const previewImage = document.getElementById("previewImage");
    const removeImageButton = document.getElementById("removeImage");
    const messageBox = document.getElementById("messageBox");
    const submitButton = document.getElementById("submitButton");
    const backBtn = document.querySelector(".back-button");

    // 필드 순서 정의 (검증 순서 및 전체 검사 시 사용)
    const fieldOrder = [
        "title",
        "challengeType",
        "targetValue",
        "startDate",
        "endDate",
        "description"
    ];

    // 에러 메시지를 표시할 DOM 요소 ID 매핑 (HTML에 해당 ID를 가진 요소가 없으면 JS에서 동적으로 처리하거나 추가 필요)
    // 현재 HTML에는 해당 ID들이 없으므로, setMessage를 활용하거나 동적으로 생성하는 방식을 고려해야 합니다.
    // 하지만 mypage-edit.js처럼 data-feedback 속성을 사용하는 것이 깔끔하므로,
    // 여기서는 setFieldMessage 함수 내부에서 적절한 위치에 메시지를 띄우도록 처리하겠습니다.
    // (기존 HTML 구조를 건드리지 않기 위해 input 바로 다음이나 적절한 위치를 찾습니다.)

    // 검증 로직 정의
    const validators = {
        title: (value) => {
            if (!value) return "제목을 입력해주세요.";
            if (value.length < 2 || value.length > 50) return "제목은 2자 이상 50자 이하여야 합니다.";
            return "";
        },
        challengeType: (value) => {
            if (!value) return "챌린지 타입을 선택해주세요.";
            return "";
        },
        targetValue: (value, allValues) => {
            if (!value) return "목표값을 입력해주세요.";
            const num = parseFloat(value);
            if (isNaN(num) || num <= 0) return "목표값은 0보다 큰 숫자여야 합니다.";

            let maxLimit = 10000;
            // TIME 타입이면 200시간(12000분) 제한
            if (allValues.challengeType === "TIME") {
                maxLimit = 12000; // 200시간 * 60분
                // 입력값(시간)을 분으로 환산하여 체크
                if (num * 60 > maxLimit) return "목표값이 너무 큽니다. (최대 200시간)";
            } else {
                if (num > maxLimit) return "목표값이 너무 큽니다. (최대 10,000)";
            }
            return "";
        },
        startDate: (value) => {
            if (!value) return "시작일을 선택해주세요.";
            const start = new Date(value);
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            start.setHours(0, 0, 0, 0);
            if (start < today) return "시작일은 오늘 이후여야 합니다.";
            return "";
        },
        endDate: (value, allValues) => {
            if (!value) return "종료일을 선택해주세요.";
            const start = new Date(allValues.startDate);
            const end = new Date(value);
            start.setHours(0, 0, 0, 0);
            end.setHours(0, 0, 0, 0);

            if (allValues.startDate && end < start) return "종료일은 시작일보다 나중이어야 합니다.";
            return "";
        },
        description: (value) => {
            if (!value) return "상세정보를 입력해주세요.";
            if (value.length < 10 || value.length > 500) return "상세정보는 10자 이상 500자 이하여야 합니다.";
            return "";
        }
    };

    // 초기화 및 이벤트 바인딩
    if (backBtn) {
        backBtn.addEventListener("click", () => {
            window.location.href = "/challenge";
        });
    }

    if (imageInput) {
        imageInput.addEventListener("change", (e) => handleImageSelect(e.target.files[0]));
    }

    if (removeImageButton) {
        removeImageButton.addEventListener("click", clearImagePreview);
    }

    // 챌린지 타입 변경 이벤트
    const challengeTypeRadios = form?.querySelectorAll('input[name="challengeType"]');
    if (challengeTypeRadios) {
        challengeTypeRadios.forEach((radio) => {
            radio.addEventListener("change", () => {
                updateTargetValueField(radio.value);
                // 타입 변경 시 목표값 재검증
                const values = collectFormValues(new FormData(form));
                validateAndDecorateField("targetValue", values);
            });
        });
        const selectedType = form.querySelector('input[name="challengeType"]:checked')?.value || "DISTANCE";
        updateTargetValueField(selectedType);
    }

    // 실시간 유효성 검사 활성화
    if (form) {
        enableRealtimeValidation(form);

        form.addEventListener("submit", async (e) => {
            e.preventDefault();
            const formData = new FormData(form);
            const values = collectFormValues(formData);

            const {valid, firstError} = validateAllFields(values);
            decorateAllFields(values);

            if (!valid) {
                if (firstError) setMessage(firstError, "error");
                return;
            }

            await submitForm(values);
        });
    }

    // --- Helper Functions ---

    function collectFormValues(formData) {
        return {
            title: formData.get("title")?.toString().trim(),
            challengeType: formData.get("challengeType"),
            targetValue: formData.get("targetValue")?.toString().trim(),
            startDate: formData.get("startDate"),
            endDate: formData.get("endDate"),
            description: formData.get("description")?.toString().trim()
        };
    }

    function enableRealtimeValidation(form) {
        fieldOrder.forEach((name) => {
            const input = form.querySelector(`[name="${name}"]`);
            if (!input) return;

            // 입력 시마다 검증
            input.addEventListener("input", () => {
                const values = collectFormValues(new FormData(form));
                validateAndDecorateField(name, values);
            });
            // 날짜 등 change 이벤트가 필요한 경우
            input.addEventListener("change", () => {
                const values = collectFormValues(new FormData(form));
                validateAndDecorateField(name, values);
                if (name === "startDate") validateAndDecorateField("endDate", values);
            });
        });
    }

    function validateAndDecorateField(name, values) {
        const {valid, message} = evaluateField(name, values);
        const input = form.querySelector(`[name="${name}"]`);

        if (input) {
            setInputState(input, valid ? "valid" : "invalid");
        }
        setFieldMessage(name, valid ? "" : message);

        return {valid, message};
    }

    function evaluateField(name, values) {
        const validator = validators[name];
        if (!validator) return {valid: true, message: ""};
        const message = validator(values[name], values);
        return {valid: !message, message};
    }

    function validateAllFields(values) {
        let allValid = true;
        let firstError = "";

        fieldOrder.forEach((name) => {
            const {valid, message} = evaluateField(name, values);
            if (!valid) {
                allValid = false;
                if (!firstError) firstError = message;
            }
        });

        return {valid: allValid, firstError};
    }

    function decorateAllFields(values) {
        fieldOrder.forEach((name) => validateAndDecorateField(name, values));
    }

    function setInputState(input, state) {
        input.classList.remove("input-valid", "input-invalid");
        input.style.borderColor = ""; // 인라인 스타일 초기화

        if (state === "invalid") {
            input.classList.add("input-invalid");
            // CSS 파일에 .input-invalid가 없다면 여기서 직접 스타일 적용
            input.style.borderColor = "var(--error)";
        } else if (state === "valid" && input.value) {
            input.classList.add("input-valid");
            input.style.borderColor = "var(--primary)";
        }
    }

    function setFieldMessage(name, message) {
        // 기존 HTML 구조상 별도의 에러 메시지 컨테이너가 없을 수 있으므로
        // 동적으로 생성하거나 기존 구조 활용.
        // 여기서는 간단하게 input 요소의 부모(form-group) 안에 .error-msg를 찾거나 생성합니다.

        const input = form.querySelector(`[name="${name}"]`);
        if (!input) return;

        let errorEl = null;
        // 1. 이미 존재하는지 확인 (HTML상 형제 요소 등)
        // input 바로 다음에 있는 .error-msg 찾기 시도
        let next = input.nextElementSibling;
        while (next) {
            if (next.classList.contains('error-msg')) {
                errorEl = next;
                break;
            }
            next = next.nextElementSibling;
        }

        // 2. 없으면 생성 (input 부모 요소에 추가)
        if (!errorEl) {
            errorEl = document.createElement("div");
            errorEl.className = "error-msg";
            errorEl.style.fontSize = "12px";
            errorEl.style.marginTop = "4px";
            // input 바로 뒤에 삽입
            input.parentNode.insertBefore(errorEl, input.nextSibling);
        }

        if (errorEl) {
            errorEl.textContent = message;
            errorEl.style.display = message ? "block" : "none";
            errorEl.style.color = "var(--error)";
        }
    }

    // --- Business Logic ---

    async function submitForm(values) {
        const accessToken = localStorage.getItem("accessToken");
        if (!accessToken) {
            alert("로그인이 필요합니다.");
            window.location.href = "/login";
            return;
        }

        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = "등록 중...";
        }
        setMessage("");

        try {
            // TIME 타입 변환 (시간 -> 분)
            let targetVal = parseFloat(values.targetValue);
            if (values.challengeType === "TIME") {
                targetVal = targetVal * 60;
            }

            const requestData = {
                title: values.title,
                challengeType: values.challengeType,
                targetValue: targetVal,
                description: values.description,
                startDate: values.startDate,
                endDate: values.endDate,
            };

            const formData = new FormData();
            const requestBlob = new Blob([JSON.stringify(requestData)], {type: "application/json"});
            formData.append("request", requestBlob);

            if (imageInput.files[0]) {
                formData.append("file", imageInput.files[0]);
            }

            const response = await fetch("/challenges", {
                method: "POST",
                headers: {"Authorization": `Bearer ${accessToken}`},
                body: formData,
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `챌린지 등록 실패 (${response.status})`);
            }

            setMessage("챌린지가 성공적으로 등록되었습니다!", "success");
            setTimeout(() => {
                window.location.href = "/challenge";
            }, 1500);

        } catch (error) {
            console.error(error);
            setMessage(error.message, "error");
        } finally {
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.textContent = "등록하기";
            }
        }
    }

    // --- UI Helpers ---

    function updateTargetValueField(challengeType) {
        const targetValueLabel = document.getElementById("targetValueLabel");
        const targetValueUnit = document.getElementById("targetValueUnit");
        const targetValueInput = document.getElementById("targetValue");

        if (!targetValueLabel || !targetValueUnit || !targetValueInput) return;

        switch (challengeType) {
            case "DISTANCE":
                targetValueLabel.textContent = "목표 거리";
                targetValueUnit.textContent = "km";
                targetValueInput.placeholder = "0";
                targetValueInput.step = "0.1";
                break;
            case "TIME":
                targetValueLabel.textContent = "목표 시간";
                targetValueUnit.textContent = "시간";
                targetValueInput.placeholder = "0";
                targetValueInput.step = "0.5";
                break;
            case "COUNT":
                targetValueLabel.textContent = "목표 일수";
                targetValueUnit.textContent = "일";
                targetValueInput.placeholder = "0";
                targetValueInput.step = "1";
                break;
        }
    }

    function handleImageSelect(file) {
        if (!file) {
            clearImagePreview();
            return;
        }
        if (!file.type.startsWith("image/")) {
            setMessage("이미지 파일만 업로드 가능합니다.", "error");
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            setMessage("이미지 파일 크기는 5MB 이하여야 합니다.", "error");
            return;
        }

        const reader = new FileReader();
        reader.onload = (e) => {
            previewImage.src = e.target.result;
            imagePreview.hidden = false;
            setMessage("");
        };
        reader.readAsDataURL(file);
    }

    function clearImagePreview() {
        if (imageInput) imageInput.value = "";
        if (previewImage) previewImage.src = "";
        if (imagePreview) imagePreview.hidden = true;
        setMessage("");
    }

    function setMessage(message, type = "error") {
        if (!messageBox) return;
        messageBox.textContent = message;
        messageBox.hidden = !message;
        messageBox.className = "message-box";
        if (type === "success") {
            messageBox.style.color = "#ccff00";
            messageBox.style.backgroundColor = "rgba(186, 255, 41, 0.2)";
        } else {
            messageBox.style.color = "#ff4d4d";
            messageBox.style.backgroundColor = "rgba(255, 59, 48, 0.1)";
        }
    }
});