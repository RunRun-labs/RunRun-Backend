document.addEventListener("DOMContentLoaded", () => {
    const backButton = document.querySelector(".back-button");
    const bottomNavMount = document.getElementById("bottomNavMount");
    const bottomNavTemplate = document.getElementById("bottomNavTemplate");
    const form = document.getElementById("challengeEditForm");
    const imageFileInput = document.getElementById("imageFile");
    const imagePreview = document.getElementById("imagePreview");
    const imagePlaceholder = document.getElementById("imagePlaceholder");
    const previewImage = document.getElementById("previewImage");
    const removeImageButton = document.getElementById("removeImage");
    const messageBox = document.getElementById("messageBox");
    const submitButton = document.getElementById("submitButton");

    // URL에서 챌린지 ID 추출
    const challengeId = extractChallengeIdFromUrl();
    if (!challengeId) {
        alert("잘못된 접근입니다.");
        window.location.href = "/challenge";
        return;
    }

    // 뒤로가기 버튼 클릭 이벤트
    if (backButton) {
        backButton.addEventListener("click", () => {
            window.location.href = `/challenge/${challengeId}`;
        });
    }

    // Static fallback: use inline template when Thymeleaf include is not processed
    if (
        bottomNavMount &&
        bottomNavMount.childElementCount === 0 &&
        bottomNavTemplate
    ) {
        const clone = bottomNavTemplate.content.firstElementChild.cloneNode(true);
        bottomNavMount.replaceWith(clone);
    }

    // 이미지 파일 선택 이벤트
    if (imageFileInput) {
        imageFileInput.addEventListener("change", (e) => {
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
    }

    // 챌린지 데이터 로드
    loadChallengeData(challengeId);

    function extractChallengeIdFromUrl() {
        const path = window.location.pathname;
        const match = path.match(/\/challenge\/(\d+)\/edit/);
        return match ? match[1] : null;
    }

    async function loadChallengeData(challengeId) {
        const accessToken = localStorage.getItem("accessToken");

        try {
            const response = await fetch(`/challenges/${challengeId}`, {
                headers: accessToken
                    ? {Authorization: `Bearer ${accessToken}`}
                    : {},
            });

            if (!response.ok) {
                const text = await response.text().catch(() => "");
                throw new Error(`챌린지 정보를 불러오지 못했습니다. (Status: ${response.status}) ${text}`);
            }

            const payload = await response.json().catch(() => null);
            const challenge = payload?.data ?? payload;

            if (!challenge) {
                throw new Error("서버에서 받은 챌린지 정보가 비어있습니다.");
            }

            fillFormWithChallengeData(challenge);
        } catch (error) {
            console.error("챌린지 로드 실패:", error);
            alert(error.message || "챌린지 정보를 불러오는 중 오류가 발생했습니다.");
            window.location.href = "/challenge";
        }
    }

    function fillFormWithChallengeData(challenge) {
        // 제목
        const titleInput = document.getElementById("title");
        if (titleInput) {
            titleInput.value = challenge.title || "";
        }

        // 이미지
        if (challenge.imageUrl) {
            previewImage.src = challenge.imageUrl;
            imagePreview.hidden = false;
            if (imagePlaceholder) {
                imagePlaceholder.hidden = true;
            }
        }

        // 챌린지 타입
        const challengeType = challenge.challengeType || "DISTANCE";
        const typeRadio = form?.querySelector(`input[name="challengeType"][value="${challengeType}"]`);
        if (typeRadio) {
            typeRadio.checked = true;
            updateTargetValueField(challengeType);
        }

        // 목표값
        if (targetValueInput) {
            let targetValue = challenge.targetValue || 0;
            // TIME 타입인 경우 시간 단위로 변환 (분 -> 시간)
            if (challengeType === "TIME" && targetValue > 0) {
                targetValue = targetValue / 60;
            }
            targetValueInput.value = targetValue;
        }

        // 날짜
        const startDateInput = document.getElementById("startDate");
        const endDateInput = document.getElementById("endDate");
        if (startDateInput && challenge.startDate) {
            startDateInput.value = formatDateForInput(challenge.startDate);
            startDateInput.min = new Date().toISOString().split("T")[0];
        }
        if (endDateInput && challenge.endDate) {
            endDateInput.value = formatDateForInput(challenge.endDate);
            if (startDateInput) {
                endDateInput.min = startDateInput.value;
            }
        }

        // 시작일 변경 시 종료일 최소값 업데이트
        if (startDateInput && endDateInput) {
            startDateInput.addEventListener("change", () => {
                if (endDateInput.value && endDateInput.value < startDateInput.value) {
                    endDateInput.value = startDateInput.value;
                }
                endDateInput.min = startDateInput.value;
            });
        }

        // 설명
        const descriptionInput = document.getElementById("description");
        if (descriptionInput) {
            descriptionInput.value = challenge.description || "";
        }
    }

    function formatDateForInput(dateString) {
        if (!dateString) return "";
        const date = new Date(dateString);
        if (isNaN(date.getTime())) return "";
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        return `${year}-${month}-${day}`;
    }

    function handleImageSelect(file) {
        if (!file) {
            return;
        }

        // 이미지 파일 검증
        if (!file.type.startsWith("image/")) {
            setMessage("이미지 파일만 업로드 가능합니다.", "error");
            return;
        }

        // 파일 크기 검증 (10MB 제한)
        const maxSize = 10 * 1024 * 1024; // 10MB
        if (file.size > maxSize) {
            setMessage("이미지 파일 크기는 10MB 이하여야 합니다.", "error");
            return;
        }

        // 미리보기 표시
        const reader = new FileReader();
        reader.onload = (e) => {
            previewImage.src = e.target.result;
            imagePreview.hidden = false;
            if (imagePlaceholder) {
                imagePlaceholder.hidden = true;
            }
            setMessage("");
        };
        reader.onerror = () => {
            setMessage("이미지를 불러오는 중 오류가 발생했습니다.", "error");
        };
        reader.readAsDataURL(file);
    }

    function clearImagePreview() {
        if (imageFileInput) {
            imageFileInput.value = "";
        }
        if (previewImage) {
            previewImage.src = "";
        }
        if (imagePreview) {
            imagePreview.hidden = true;
        }
        if (imagePlaceholder) {
            imagePlaceholder.hidden = false;
        }
        setMessage("");
    }

    async function handleFormSubmit(e) {
        e.preventDefault();

        const accessToken = localStorage.getItem("accessToken");
        if (!accessToken) {
            alert("로그인이 필요합니다.");
            window.location.href = "/login";
            return;
        }

        // 폼 데이터 수집
        const formData = new FormData(form);

        // 제목 검증
        const title = formData.get("title")?.toString().trim();
        if (!title) {
            setMessage("제목을 입력해주세요.", "error");
            return;
        }

        // 날짜 검증
        const startDate = formData.get("startDate");
        const endDate = formData.get("endDate");
        if (!startDate || !endDate) {
            setMessage("시작일과 종료일을 모두 입력해주세요.", "error");
            return;
        }

        if (new Date(endDate) < new Date(startDate)) {
            setMessage("종료일은 시작일보다 나중이어야 합니다.", "error");
            return;
        }

        // 설명 검증
        const description = formData.get("description")?.toString().trim();
        if (!description) {
            setMessage("상세정보를 입력해주세요.", "error");
            return;
        }

        // 챌린지 타입 검증
        const challengeType = formData.get("challengeType");
        if (!challengeType) {
            setMessage("챌린지 타입을 선택해주세요.", "error");
            return;
        }

        // 목표값 검증
        const targetValueStr = formData.get("targetValue")?.toString().trim();
        if (!targetValueStr) {
            setMessage("목표값을 입력해주세요.", "error");
            return;
        }

        let targetValue = parseFloat(targetValueStr);
        if (isNaN(targetValue) || targetValue <= 0) {
            setMessage("목표값은 0보다 큰 숫자여야 합니다.", "error");
            return;
        }

        // TIME 타입인 경우 시간을 분으로 변환
        if (challengeType === "TIME") {
            targetValue = targetValue * 60;
        }

        // 요청 데이터 생성
        const requestData = {
            title: title,
            challengeType: challengeType,
            targetValue: targetValue,
            description: description,
            startDate: startDate,
            endDate: endDate,
        };

        // FormData에 JSON 요청 추가
        const requestBlob = new Blob([JSON.stringify(requestData)], {
            type: "application/json",
        });
        formData.set("request", requestBlob);

        // 제출 버튼 비활성화
        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = "수정 중...";
        }

        setMessage("");

        try {
            const response = await fetch(`/challenges/${challengeId}`, {
                method: "PUT",
                headers: {
                    Authorization: `Bearer ${accessToken}`,
                },
                body: formData,
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                const errorMessage =
                    errorData?.message ||
                    errorData?.data?.message ||
                    `챌린지 수정에 실패했습니다. (Status: ${response.status})`;
                throw new Error(errorMessage);
            }

            const result = await response.json();
            setMessage("챌린지가 성공적으로 수정되었습니다!", "success");

            // 성공 후 챌린지 상세보기로 이동
            setTimeout(() => {
                window.location.href = `/challenge/${challengeId}`;
            }, 1500);
        } catch (error) {
            console.error("챌린지 수정 실패:", error);
            setMessage(error.message || "챌린지 수정 중 오류가 발생했습니다.", "error");
        } finally {
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.textContent = "챌린지 수정하기";
            }
        }
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
});

