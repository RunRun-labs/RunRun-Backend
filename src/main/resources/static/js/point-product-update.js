// ad-update.js 기반 - 포인트 상품 수정

document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("productForm");
    const updateBtn = document.getElementById("updateBtn");
    const imageFileInput = document.getElementById("imageFile");
    const imageUploadArea = document.getElementById("imageUploadArea");
    const uploadPlaceholder = document.getElementById("uploadPlaceholder");
    const currentImageDiv = document.getElementById("currentImage");
    const currentImg = document.getElementById("currentImg");
    const newImagePreview = document.getElementById("newImagePreview");
    let productId = null;
    let currentImageUrl = null;

    // URL에서 상품 ID 가져오기
    const pathParts = window.location.pathname.split("/");
    const updateIndex = pathParts.indexOf("update");
    if (updateIndex !== -1 && pathParts[updateIndex + 1]) {
        productId = pathParts[updateIndex + 1];
    }

    if (!productId) {
        alert("상품 ID를 찾을 수 없습니다.");
        window.location.href = "/admin/points/products";
        return;
    }

    // 필드 요소들
    const fields = {
        productName: document.getElementById("productName"),
        productDescription: document.getElementById("productDescription"),
        requiredPoint: document.getElementById("requiredPoint"),
        imageFile: imageFileInput,
        isAvailable: document.getElementById("isAvailable"),
    };

    // 이미지 미리보기
    imageFileInput.addEventListener("change", function (e) {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = function (e) {
                newImagePreview.innerHTML = `<img src="${e.target.result}" alt="새 이미지 미리보기" />`;
                imageUploadArea.classList.add("has-preview");
            };
            reader.readAsDataURL(file);
        } else {
            newImagePreview.innerHTML = "";
            imageUploadArea.classList.remove("has-preview");
        }
    });

    // 벨리데이션 규칙
    const validators = {
        productName: (value) => {
            if (!value || value.trim() === "") {
                return "상품명은 필수입니다.";
            }
            if (value.length < 2 || value.length > 100) {
                return "상품명은 2~100자여야 합니다.";
            }
            return null;
        },

        productDescription: (value) => {
            if (!value || value.trim() === "") {
                return "상품 설명은 필수입니다.";
            }
            if (value.length < 10 || value.length > 1000) {
                return "상품 설명은 10~1000자여야 합니다.";
            }
            return null;
        },

        requiredPoint: (value) => {
            if (!value || value.trim() === "") {
                return "필요 포인트는 필수입니다.";
            }
            const point = parseInt(value, 10);
            if (isNaN(point) || point < 1) {
                return "필요 포인트는 1 이상이어야 합니다.";
            }
            return null;
        },

        imageFile: (value) => {
            if (value && value.files.length > 0) {
                const file = value.files[0];
                if (!file.type.startsWith("image/")) {
                    return "이미지 파일만 업로드 가능합니다.";
                }
                if (file.size > 5 * 1024 * 1024) {
                    return "파일 크기는 5MB 이하여야 합니다.";
                }
            }
            return null;
        },
    };

    // 필드 검증
    function validateField(fieldName, showError = true) {
        const field = fields[fieldName];
        if (!field) return false;

        const value = fieldName === "imageFile" ? field : field.value;
        const error = validators[fieldName] ? validators[fieldName](value) : null;
        const errorElement = document.getElementById(`${fieldName}-error`);

        if (error) {
            if (showError && errorElement) {
                errorElement.textContent = error;
                errorElement.style.display = "block";
            }
            if (field && field.classList) {
                field.classList.add("error");
                field.classList.remove("valid");
            }
            return false;
        } else {
            if (errorElement) {
                errorElement.textContent = "";
                errorElement.style.display = "none";
            }
            if (field && field.classList) {
                field.classList.remove("error");
                field.classList.add("valid");
            }
            return true;
        }
    }

    // 전체 폼 검증
    function validateForm() {
        let isValid = true;
        // imageFile은 선택사항이므로 제외
        ["productName", "productDescription", "requiredPoint"].forEach((fieldName) => {
            if (!validateField(fieldName, true)) {
                isValid = false;
            }
        });
        // imageFile은 있으면 검증
        if (fields.imageFile.files.length > 0) {
            if (!validateField("imageFile", true)) {
                isValid = false;
            }
        }
        return isValid;
    }

    // 필드 변경 시 실시간 검증
    fields.productName.addEventListener("blur", () => validateField("productName"));
    fields.productName.addEventListener("input", () => {
        if (fields.productName.classList.contains("error")) {
            validateField("productName");
        }
    });

    fields.productDescription.addEventListener("blur", () => validateField("productDescription"));
    fields.productDescription.addEventListener("input", () => {
        if (fields.productDescription.classList.contains("error")) {
            validateField("productDescription");
        }
    });

    fields.requiredPoint.addEventListener("blur", () => validateField("requiredPoint"));
    fields.requiredPoint.addEventListener("input", () => {
        if (fields.requiredPoint.classList.contains("error")) {
            validateField("requiredPoint");
        }
    });

    fields.imageFile.addEventListener("change", () => validateField("imageFile"));

    // 상품 데이터 로드
    async function loadProductData() {
        try {
            // 목록 API에서 단일 상품 찾기
            const response = await fetch(`/api/admin/points/products?page=0&size=1000`, {
                method: "GET",
                headers: getAuthHeaders(),
            });

            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.message || "상품 데이터를 불러올 수 없습니다.");
            }

            if (result.success && result.data && result.data.items) {
                const product = result.data.items.find((p) => p.productId === parseInt(productId));

                if (!product) {
                    throw new Error("상품을 찾을 수 없습니다.");
                }

                currentImageUrl = product.productImageUrl;

                // 폼에 데이터 채우기
                fields.productName.value = product.productName || "";
                fields.productDescription.value = product.productDescription || "";
                fields.requiredPoint.value = product.requiredPoint || "";
                fields.isAvailable.checked = product.isAvailable || false;

                // 현재 이미지 표시
                if (product.productImageUrl && currentImg && currentImageDiv) {
                    const imageUrl = product.productImageUrl.startsWith("http")
                        ? product.productImageUrl
                        : `https://runrun-uploads-bucket.s3.mx-central-1.amazonaws.com/${product.productImageUrl}`;
                    currentImg.src = imageUrl;
                    currentImageDiv.style.display = "block";
                } else if (currentImageDiv) {
                    currentImageDiv.style.display = "none";
                }
            } else {
                throw new Error(result.message || "상품 데이터를 불러올 수 없습니다.");
            }
        } catch (error) {
            console.error("Error:", error);
            alert(error.message || "상품 데이터를 불러오는 중 오류가 발생했습니다.");
            window.location.href = "/admin/points/products";
        }
    }

    // 수정 버튼 클릭
    updateBtn.addEventListener("click", async function () {
        if (!validateForm()) {
            alert("입력한 정보를 확인해주세요.");
            return;
        }

        try {
            updateBtn.disabled = true;
            updateBtn.textContent = "수정 중...";

            const token = getAccessToken();
            let imageKey = currentImageUrl; // 기존 이미지 URL 사용

            // 새 이미지가 있으면 업로드
            if (fields.imageFile.files.length > 0) {
                const imageFormData = new FormData();
                imageFormData.append("file", fields.imageFile.files[0]);

                const uploadResponse = await fetch("/api/admin/points/products/upload", {
                    method: "POST",
                    headers: {
                        Authorization: token,
                    },
                    body: imageFormData,
                });

                if (!uploadResponse.ok) {
                    throw new Error("이미지 업로드에 실패했습니다.");
                }

                const uploadResult = await uploadResponse.json();
                imageKey = uploadResult.data.key;
            }

            // 상품 정보를 JSON으로 전송
            const requestData = {
                productName: fields.productName.value.trim(),
                productDescription: fields.productDescription.value.trim(),
                requiredPoint: parseInt(fields.requiredPoint.value, 10),
                productImageUrl: imageKey,
                isAvailable: fields.isAvailable.checked,
            };

            const response = await fetch(`/api/admin/points/products/${productId}`, {
                method: "PUT",
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
                throw new Error(result.message || "수정에 실패했습니다.");
            }

            if (result.success) {
                alert("포인트 상품이 수정되었습니다.");
                window.location.href = "/admin/points/products";
            } else {
                throw new Error(result.message || "수정에 실패했습니다.");
            }
        } catch (error) {
            console.error("Error:", error);
            alert(error.message || "수정 중 오류가 발생했습니다.");
        } finally {
            updateBtn.disabled = false;
            updateBtn.textContent = "수정하기";
        }
    });

    // 초기 데이터 로드
    loadProductData();
});