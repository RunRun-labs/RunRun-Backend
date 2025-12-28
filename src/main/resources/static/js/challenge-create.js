document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("challengeCreateForm");
    const imageInput = document.getElementById("imageFile");
    const imagePreview = document.getElementById("imagePreview");
    const previewImage = document.getElementById("previewImage");
    const removeImageButton = document.getElementById("removeImage");
    const messageBox = document.getElementById("messageBox");
    const submitButton = document.getElementById("submitButton");
    const backBtn = document.querySelector(".back-button");
    const imageUploadLabel = document.querySelector(".image-upload-label");

    const fieldOrder = [
        "title",
        "challengeType",
        "targetValue",
        "startDate",
        "endDate",
        "description"
    ];


    const errorElementMap = {
        title: "titleError",
        targetValue: "targetValueError",
        startDate: "dateError",
        endDate: "dateError",
        description: "descError"
    };

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
            if (allValues.challengeType === "TIME") {
                maxLimit = 12000;
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


    const challengeTypeRadios = form?.querySelectorAll('input[name="challengeType"]');
    if (challengeTypeRadios) {
        challengeTypeRadios.forEach((radio) => {
            radio.addEventListener("change", () => {
                updateTargetValueField(radio.value);

                const values = collectFormValues(new FormData(form));
                validateAndDecorateField("targetValue", values);
            });
        });
        const selectedType = form.querySelector('input[name="challengeType"]:checked')?.value || "DISTANCE";
        updateTargetValueField(selectedType);
    }


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

            input.addEventListener("input", () => {
                const values = collectFormValues(new FormData(form));
                validateAndDecorateField(name, values);
            });
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
        input.style.borderColor = "";

        if (state === "invalid") {
            input.classList.add("input-invalid");
            input.style.borderColor = "var(--error)";
        } else if (state === "valid" && input.value) {
            input.classList.add("input-valid");
            input.style.borderColor = "var(--primary)";
        }
    }

    function setFieldMessage(name, message) {
        const input = form.querySelector(`[name="${name}"]`);
        if (!input) return;

        let errorEl = null;
        let next = input.nextElementSibling;
        while (next) {
            if (next.classList.contains('error-msg')) {
                errorEl = next;
                break;
            }
            next = next.nextElementSibling;
        }

        if (!errorEl) {
            errorEl = document.createElement("div");
            errorEl.className = "error-msg";
            errorEl.style.fontSize = "12px";
            errorEl.style.marginTop = "4px";

            input.parentNode.insertBefore(errorEl, input.nextSibling);
        }

        if (errorEl) {
            errorEl.textContent = message;
            errorEl.style.display = message ? "block" : "none";
            errorEl.style.color = "var(--error)";
        }
    }


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


            if (imageUploadLabel) {
                imageUploadLabel.style.display = "none";
            }

            setMessage("");
        };
        reader.readAsDataURL(file);
    }

    function clearImagePreview() {
        if (imageInput) imageInput.value = "";
        if (previewImage) previewImage.src = "";
        if (imagePreview) imagePreview.hidden = true;


        if (imageUploadLabel) {
            imageUploadLabel.style.display = "block";
        }

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