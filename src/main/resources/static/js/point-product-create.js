// point-product-create.js - 포인트 상품 생성 + 이미지 미리보기(div 배경)

document.addEventListener("DOMContentLoaded", () => {
    /* =====================
     * 1. DOM 바인딩
     * ===================== */
    const createBtn = document.getElementById("createBtn");
    const imagePreview = document.getElementById("imagePreview"); // div
    const uploadPlaceholder = document.getElementById("uploadPlaceholder"); // placeholder
    const imageUploadArea = document.getElementById("imageUploadArea"); // dropzone

    const fields = {
        productName: document.getElementById("productName"),
        productDescription: document.getElementById("productDescription"),
        requiredPoint: document.getElementById("requiredPoint"),
        isAvailable: document.getElementById("isAvailable"),
        imageFile: document.getElementById("imageFile"),
    };

    // 필수 DOM 체크
    if (!createBtn || Object.values(fields).some((el) => !el)) {
        console.error("필수 DOM 요소를 찾지 못했습니다. HTML id를 확인하세요.", {
            createBtn,
            fields,
            imagePreview,
            uploadPlaceholder,
            imageUploadArea,
        });
        return;
    }

    /* =====================
     * 2. 벨리데이션 규칙
     * ===================== */
    const validators = {
        productName: (value) => {
            if (!value || value.trim() === "") return "상품명은 필수입니다.";
            if (value.length < 2 || value.length > 100) return "상품명은 2~100자여야 합니다.";
            return null;
        },

        productDescription: (value) => {
            if (!value || value.trim() === "") return "상품 설명은 필수입니다.";
            if (value.length < 10 || value.length > 1000) return "상품 설명은 10~1000자여야 합니다.";
            return null;
        },

        requiredPoint: (value) => {
            if (!value || value.trim() === "") return "필요 포인트는 필수입니다.";
            const point = parseInt(value, 10);
            if (isNaN(point) || point < 1) return "필요 포인트는 1 이상이어야 합니다.";
            return null;
        },

        imageFile: (fileInput) => {
            if (!fileInput || fileInput.files.length === 0) return "상품 이미지는 필수입니다.";
            const file = fileInput.files[0];
            if (!file.type.startsWith("image/")) return "이미지 파일만 업로드 가능합니다.";
            if (file.size > 5 * 1024 * 1024) return "파일 크기는 5MB 이하여야 합니다.";
            return null;
        },
    };

    /* =====================
     * 3. 필드 검증 함수
     * ===================== */
    function validateField(fieldName, showError = true) {
        const field = fields[fieldName];
        if (!field) return false;

        const value = fieldName === "imageFile" ? field : field.value;
        const error = validators[fieldName]?.(value);
        const errorElement = document.getElementById(`${fieldName}-error`);

        if (error) {
            if (showError && errorElement) {
                errorElement.textContent = error;
                errorElement.style.display = "block";
            }
            field.classList.add("error");
            field.classList.remove("valid");
            return false;
        }

        if (errorElement) {
            errorElement.textContent = "";
            errorElement.style.display = "none";
        }
        field.classList.remove("error");
        field.classList.add("valid");
        return true;
    }

    function validateForm() {
        return Object.keys(validators).every((name) => validateField(name, true));
    }

    /* =====================
     * 4. 실시간 검증
     * ===================== */
    fields.productName.addEventListener("blur", () => validateField("productName"));
    fields.productDescription.addEventListener("blur", () => validateField("productDescription"));
    fields.requiredPoint.addEventListener("blur", () => validateField("requiredPoint"));

    /* =====================
     * 5. 이미지 변경 → 검증 + 미리보기
     * ===================== */
    let currentPreviewUrl = null;

    function clearPreview() {
        if (!imagePreview) return;

        imagePreview.style.backgroundImage = "none";
        imagePreview.style.display = "none";

        if (uploadPlaceholder) uploadPlaceholder.style.display = "flex";
        if (imageUploadArea) imageUploadArea.classList.remove("has-preview");

        if (currentPreviewUrl) {
            URL.revokeObjectURL(currentPreviewUrl);
            currentPreviewUrl = null;
        }
    }

    function showPreview(file) {
        if (!imagePreview) return;

        // 이전 url 정리
        if (currentPreviewUrl) URL.revokeObjectURL(currentPreviewUrl);

        currentPreviewUrl = URL.createObjectURL(file);

        // div에 배경이미지로 미리보기 적용
        imagePreview.style.backgroundImage = `url("${currentPreviewUrl}")`;
        imagePreview.style.backgroundRepeat = "no-repeat";
        imagePreview.style.backgroundPosition = "center";
        imagePreview.style.backgroundSize = "contain";
        imagePreview.style.display = "block";

        // placeholder 숨김
        if (uploadPlaceholder) uploadPlaceholder.style.display = "none";
        if (imageUploadArea) imageUploadArea.classList.add("has-preview");
    }

    fields.imageFile.addEventListener("change", () => {
        const ok = validateField("imageFile");

        const file = fields.imageFile.files && fields.imageFile.files[0];
        if (!ok || !file || !file.type.startsWith("image/")) {
            clearPreview();
            return;
        }

        showPreview(file);
    });

    /* =====================
     * 6. 생성 버튼 클릭
     * ===================== */
    createBtn.addEventListener("click", async () => {
        if (!validateForm()) {
            alert("입력한 정보를 확인해주세요.");
            return;
        }

        try {
            createBtn.disabled = true;
            createBtn.textContent = "생성 중...";

            /* 1) 이미지 업로드 */
            const imageFormData = new FormData();
            imageFormData.append("file", fields.imageFile.files[0]);

            const token = getAccessToken();
            const uploadResponse = await fetch("/api/admin/points/products/upload", {
                method: "POST",
                headers: {Authorization: token},
                body: imageFormData,
            });

            if (!uploadResponse.ok) throw new Error("이미지 업로드에 실패했습니다.");

            const uploadResult = await uploadResponse.json();
            const imageKey = uploadResult?.data?.key;

            if (!imageKey) throw new Error("이미지 업로드 응답에 key가 없습니다.");

            /* 2) 상품 생성(JSON) */
            const requestData = {
                productName: fields.productName.value.trim(),
                productDescription: fields.productDescription.value.trim(),
                requiredPoint: parseInt(fields.requiredPoint.value, 10),
                productImageUrl: imageKey,
                isAvailable: fields.isAvailable.checked,
            };

            const response = await fetch("/api/admin/points/products", {
                method: "POST",
                headers: {
                    Authorization: token,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(requestData),
            });

            const result = await response.json();

            if (!response.ok) {
                if (response.status === 401) {
                    alert("로그인이 필요합니다.");
                    window.location.href = "/login";
                    return;
                }
                throw new Error(result?.message || "생성에 실패했습니다.");
            }

            if (result?.success) {
                alert("포인트 상품이 생성되었습니다.");
                window.location.href = "/admin/points/products";
            } else {
                throw new Error(result?.message || "생성에 실패했습니다.");
            }
        } catch (error) {
            console.error(error);
            alert(error.message || "생성 중 오류가 발생했습니다.");
        } finally {
            createBtn.disabled = false;
            createBtn.textContent = "생성하기";
        }
    });

    // 페이지 진입 시 초기화(혹시 이전 상태 남는 경우 방지)
    clearPreview();
});
