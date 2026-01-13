document.addEventListener("DOMContentLoaded", function () {
  const form = document.getElementById("adForm");
  const createBtn = document.getElementById("createBtn");
  const imageFileInput = document.getElementById("imageFile");

  // 필드 요소들
  const fields = {
    name: document.getElementById("name"),
    imageFile: imageFileInput,
    redirectUrl: document.getElementById("redirectUrl"),
  };

  // 이미지 미리보기
  const imageUploadArea = document.getElementById("imageUploadArea");
  const uploadPlaceholder = document.getElementById("uploadPlaceholder");
  const imagePreview = document.getElementById("imagePreview");
  
  imageFileInput.addEventListener("change", function (e) {
    const file = e.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = function (e) {
        imagePreview.innerHTML = `<img src="${e.target.result}" alt="미리보기" />`;
        imageUploadArea.classList.add("has-preview");
      };
      reader.readAsDataURL(file);
    } else {
      imagePreview.innerHTML = "";
      imageUploadArea.classList.remove("has-preview");
    }
  });

  // 벨리데이션 규칙
  const validators = {
    name: (value) => {
      if (!value || value.trim() === "") {
        return "광고명은 필수입니다.";
      }
      if (value.length < 3 || value.length > 50) {
        return "광고명은 3~50자여야 합니다.";
      }
      return null;
    },

    imageFile: (value) => {
      if (!value || value.files.length === 0) {
        return "이미지 파일은 필수입니다.";
      }
      const file = value.files[0];
      if (!file.type.startsWith("image/")) {
        return "이미지 파일만 업로드 가능합니다.";
      }
      return null;
    },

    redirectUrl: (value) => {
      if (!value || value.trim() === "") {
        return "리다이렉트 URL은 필수입니다.";
      }
      try {
        new URL(value);
      } catch (e) {
        return "올바른 URL 형식이 아닙니다.";
      }
      return null;
    },
  };

  // 필드 검증
  function validateField(fieldName, showError = true) {
    const field = fields[fieldName];
    if (!field) return false;

    const value = fieldName === "imageFile" ? field : field.value;
    const error = validators[fieldName](value);
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
    Object.keys(validators).forEach((fieldName) => {
      if (!validateField(fieldName, true)) {
        isValid = false;
      }
    });
    return isValid;
  }

  // 필드 변경 시 실시간 검증
  fields.name.addEventListener("blur", () => validateField("name"));
  fields.name.addEventListener("input", () => {
    if (fields.name.classList.contains("error")) {
      validateField("name");
    }
  });

  fields.imageFile.addEventListener("change", () => validateField("imageFile"));
  fields.redirectUrl.addEventListener("blur", () => validateField("redirectUrl"));
  fields.redirectUrl.addEventListener("input", () => {
    if (fields.redirectUrl.classList.contains("error")) {
      validateField("redirectUrl");
    }
  });

  // 생성 버튼 클릭
  createBtn.addEventListener("click", async function () {
    if (!validateForm()) {
      alert("입력한 정보를 확인해주세요.");
      return;
    }

    const formData = new FormData();
    
    // DTO를 JSON으로 추가
    const dto = {
      name: fields.name.value.trim(),
      redirectUrl: fields.redirectUrl.value.trim(),
    };
    formData.append("dto", new Blob([JSON.stringify(dto)], { type: "application/json" }));
    
    // 이미지 파일 추가
    formData.append("imageFile", fields.imageFile.files[0]);

    try {
      createBtn.disabled = true;
      createBtn.textContent = "생성 중...";

      const token = getAccessToken();
      const response = await fetch("/api/admin/ads", {
        method: "POST",
        headers: {
          Authorization: token,
        },
        body: formData,
      });

      const result = await response.json();

      if (!response.ok) {
        if (response.status === 401) {
          alert("로그인이 필요합니다.");
          window.location.href = "/login";
          return;
        }
        throw new Error(result.message || "생성에 실패했습니다.");
      }

      if (result.success) {
        alert("광고가 생성되었습니다.");
        const adId = result.data?.adId || result.data?.id;
        if (adId) {
          window.location.href = `/admin/ad/detail/${adId}`;
        } else {
          window.location.href = "/admin/ad/inquiry";
        }
      } else {
        throw new Error(result.message || "생성에 실패했습니다.");
      }
    } catch (error) {
      console.error("Error:", error);
      alert(error.message || "생성 중 오류가 발생했습니다.");
    } finally {
      createBtn.disabled = false;
      createBtn.textContent = "생성하기";
    }
  });
});
