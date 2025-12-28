document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("challengeCreateForm");
    const imageInput = document.getElementById("imageFile");
    const imagePreview = document.getElementById("imagePreview");
    const previewImage = document.getElementById("previewImage");
    const removeImageButton = document.getElementById("removeImage");
    const messageBox = document.getElementById("messageBox");
    const submitButton = document.getElementById("submitButton");
    const backBtn = document.querySelector(".back-button");

    // 뒤로가기
    if (backBtn) {
        backBtn.addEventListener("click", () => {
            window.location.href = "/challenge";
        });
    }

    // 이미지 파일 선택 이벤트
    if (imageInput) {
        imageInput.addEventListener("change", (e) => {
            handleImageSelect(e.target.files[0]);
        });
    }

    // 이미지 제거 버튼 클릭 이벤트
    if (removeImageButton) {
        removeImageButton.addEventListener("click", () => {
            clearImagePreview();
        });
    }

    // 폼 제출 이벤트
    if (form) {
        form.addEventListener("submit", handleFormSubmit);
    }

    // 챌린지 타입 변경 이벤트
    const challengeTypeRadios = form?.querySelectorAll('input[name="challengeType"]');
    const targetValueLabel = document.getElementById("targetValueLabel");
    const targetValueUnit = document.getElementById("targetValueUnit");
    const targetValueInput = document.getElementById("targetValue");

    if (challengeTypeRadios) {
        challengeTypeRadios.forEach((radio) => {
            radio.addEventListener("change", () => {
                updateTargetValueField(radio.value);
            });
        });
        // 초기값 설정
        const selectedType = form.querySelector('input[name="challengeType"]:checked')?.value || "DISTANCE";
        updateTargetValueField(selectedType);
    }

    function updateTargetValueField(challengeType) {
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
        // 값 초기화 (선택 사항)
        // targetValueInput.value = "";
    }

    // 날짜 입력 기본값 설정 (오늘 날짜)
    const today = new Date().toISOString().split("T")[0];
    const startDateInput = document.getElementById("startDate");
    const endDateInput = document.getElementById("endDate");
    if (startDateInput) {
        startDateInput.min = today;
        startDateInput.value = today;
    }
    if (endDateInput) {
        endDateInput.min = today;
        if (startDateInput) {
            startDateInput.addEventListener("change", () => {
                if (endDateInput.value && endDateInput.value < startDateInput.value) {
                    endDateInput.value = startDateInput.value;
                }
                endDateInput.min = startDateInput.value;
            });
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

        const maxSize = 5 * 1024 * 1024; // 5MB (백엔드 설정에 맞춤)
        if (file.size > maxSize) {
            setMessage("이미지 파일 크기는 5MB 이하여야 합니다.", "error");
            return;
        }

        const reader = new FileReader();
        reader.onload = (e) => {
            previewImage.src = e.target.result;
            imagePreview.hidden = false;
            setMessage("");
        };
        reader.onerror = () => {
            setMessage("이미지를 불러오는 중 오류가 발생했습니다.", "error");
        };
        reader.readAsDataURL(file);
    }

    function clearImagePreview() {
        if (imageInput) {
            imageInput.value = "";
        }
        if (previewImage) {
            previewImage.src = "";
        }
        if (imagePreview) {
            imagePreview.hidden = true;
        }
        setMessage("");
    }

    // [추가] 유효성 검사 함수
    function validateForm(formData) {
        // 1. 제목 검사
        const title = formData.get("title")?.toString().trim();
        if (!title || title.length < 2 || title.length > 50) {
            setMessage("제목은 2자 이상 50자 이하여야 합니다.", "error");
            return false;
        }

        // 2. 날짜 검사
        const startDate = formData.get("startDate");
        const endDate = formData.get("endDate");
        if (!startDate || !endDate) {
            setMessage("시작일과 종료일을 모두 입력해주세요.", "error");
            return false;
        }

        // 날짜 비교 (시간 정보 제거하고 날짜만 비교)
        const start = new Date(startDate);
        const end = new Date(endDate);
        const todayDate = new Date();
        todayDate.setHours(0, 0, 0, 0);
        start.setHours(0, 0, 0, 0);

        if (start < todayDate) {
            setMessage("시작일은 오늘 이후여야 합니다.", "error");
            return false;
        }
        if (end < start) {
            setMessage("종료일은 시작일보다 나중이어야 합니다.", "error");
            return false;
        }

        // 3. 설명 검사
        const description = formData.get("description")?.toString().trim();
        if (!description || description.length < 10 || description.length > 500) {
            setMessage("상세정보는 10자 이상 500자 이하여야 합니다.", "error");
            return false;
        }

        // 4. 챌린지 타입 검증
        const challengeType = formData.get("challengeType");
        if (!challengeType) {
            setMessage("챌린지 타입을 선택해주세요.", "error");
            return false;
        }

        // 5. 목표값 검증
        const targetValueStr = formData.get("targetValue")?.toString().trim();
        if (!targetValueStr) {
            setMessage("목표값을 입력해주세요.", "error");
            return false;
        }

        let targetValue = parseFloat(targetValueStr);
        if (isNaN(targetValue) || targetValue <= 0) {
            setMessage("목표값은 0보다 큰 숫자여야 합니다.", "error");
            return false;
        }

        // [수정] TIME 타입인 경우 60을 곱해서(분 단위로 변환 후) 최대값 검사
        let checkValue = targetValue;
        if (challengeType === "TIME") {
            checkValue = targetValue * 60;
        }

        if (checkValue > 12000) {
            setMessage(`목표값이 너무 큽니다. (최대 ${challengeType === "TIME" ? (12000 / 60).toFixed(1) + "시간" : "12,000"})`, "error");
            return false;
        }

        return true;
    }

    async function handleFormSubmit(e) {
        e.preventDefault();

        const accessToken = localStorage.getItem("accessToken");
        if (!accessToken) {
            alert("로그인이 필요합니다.");
            window.location.href = "/login";
            return;
        }

        const formData = new FormData(form);

        // [수정] 유효성 검사 실행
        if (!validateForm(formData)) {
            return;
        }

        // 데이터 추출
        const title = formData.get("title")?.toString().trim();
        const challengeType = formData.get("challengeType");
        let targetValue = parseFloat(formData.get("targetValue")?.toString().trim());
        const description = formData.get("description")?.toString().trim();
        const startDate = formData.get("startDate");
        const endDate = formData.get("endDate");

        // TIME 타입인 경우 시간을 분으로 변환 (서버 전송용)
        if (challengeType === "TIME") {
            targetValue = targetValue * 60;
        }

        const requestData = {
            title: title,
            challengeType: challengeType,
            targetValue: targetValue,
            description: description,
            startDate: startDate,
            endDate: endDate,
        };

        const requestBlob = new Blob([JSON.stringify(requestData)], {
            type: "application/json",
        });

        // 기존 formData를 초기화하고 새로 구성 (이미지 포함)
        const finalFormData = new FormData();
        finalFormData.append("request", requestBlob);

        const file = imageInput.files[0];
        if (file) {
            finalFormData.append("file", file);
        }

        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = "등록 중...";
        }

        setMessage("");

        try {
            const response = await fetch("/challenges", {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${accessToken}`,
                },
                body: finalFormData,
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                const errorMessage =
                    errorData?.message ||
                    errorData?.data?.message ||
                    `챌린지 등록에 실패했습니다. (Status: ${response.status})`;
                throw new Error(errorMessage);
            }

            setMessage("챌린지가 성공적으로 등록되었습니다!", "success");

            setTimeout(() => {
                window.location.href = "/challenge";
            }, 1500);
        } catch (error) {
            console.error("챌린지 등록 실패:", error);
            setMessage(error.message || "챌린지 등록 중 오류가 발생했습니다.", "error");
        } finally {
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.textContent = "등록하기";
            }
        }
    }

    function setMessage(message, type = "error") {
        if (!messageBox) return;
        messageBox.textContent = message;
        messageBox.hidden = !message;
        messageBox.className = "message-box"; // reset classes
        messageBox.classList.add(type);

        // 스타일 적용 (edit와 동일하게)
        if (type === "success") {
            messageBox.style.color = "#ccff00"; // 형광 그린
            messageBox.style.backgroundColor = "rgba(186, 255, 41, 0.2)";
        } else {
            messageBox.style.color = "#ff4d4d"; // 빨강
            messageBox.style.backgroundColor = "rgba(255, 59, 48, 0.1)";
        }
    }
});