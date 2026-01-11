document.addEventListener("DOMContentLoaded", function () {
  const form = document.getElementById("adSlotForm");
  const createBtn = document.getElementById("createBtn");

  // 필드 요소들
  const fields = {
    name: document.getElementById("name"),
    slotType: document.getElementById("slotType"),
    dailyLimit: document.getElementById("dailyLimit"),
    allowPremium: document.querySelector('input[name="allowPremium"]:checked'),
  };

  // 벨리데이션 규칙
  const validators = {
    name: (value) => {
      if (!value || value.trim() === "") {
        return "슬롯명은 필수입니다.";
      }
      if (value.length < 2 || value.length > 50) {
        return "슬롯명은 2~50자여야 합니다.";
      }
      return null;
    },

    slotType: (value) => {
      if (!value || value === "") {
        return "슬롯 타입은 필수입니다.";
      }
      return null;
    },

    dailyLimit: (value) => {
      if (value === "" || value === null || value === undefined) {
        return "일일 제한은 필수입니다. (0=무제한)";
      }
      const num = parseInt(value);
      if (isNaN(num) || num < 0) {
        return "일일 제한은 0 이상이어야 합니다.";
      }
      return null;
    },

    allowPremium: (value) => {
      if (value === null || value === undefined) {
        return "프리미엄 허용 여부는 필수입니다.";
      }
      return null;
    },
  };

  // 필드 검증
  function validateField(fieldName, showError = true) {
    const field = fields[fieldName];
    if (!field) return false;

    let value;
    if (fieldName === "allowPremium") {
      const checked = document.querySelector('input[name="allowPremium"]:checked');
      value = checked ? checked.value === "true" : null;
    } else {
      value = field.value;
    }

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
  Object.keys(fields).forEach((fieldName) => {
    const field = fields[fieldName];
    if (field) {
      if (fieldName === "allowPremium") {
        document.querySelectorAll('input[name="allowPremium"]').forEach((radio) => {
          radio.addEventListener("change", () => {
            fields.allowPremium = document.querySelector('input[name="allowPremium"]:checked');
            validateField("allowPremium");
          });
        });
      } else {
        field.addEventListener("blur", () => validateField(fieldName));
        field.addEventListener("input", () => {
          if (field.classList && field.classList.contains("error")) {
            validateField(fieldName);
          }
        });
      }
    }
  });

  // 생성 버튼 클릭
  createBtn.addEventListener("click", async function () {
    if (!validateForm()) {
      alert("입력한 정보를 확인해주세요.");
      return;
    }

    const allowPremiumChecked = document.querySelector('input[name="allowPremium"]:checked');
    const allowPremium = allowPremiumChecked ? allowPremiumChecked.value === "true" : false;

    const data = {
      name: fields.name.value.trim(),
      slotType: fields.slotType.value,
      dailyLimit: parseInt(fields.dailyLimit.value),
      allowPremium: allowPremium,
    };

    try {
      createBtn.disabled = true;
      createBtn.textContent = "생성 중...";

      const response = await fetch("/api/admin/ad-slot", {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify(data),
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
        alert("광고 슬롯이 생성되었습니다.");
        window.location.href = "/admin/ad-slot/inquiry";
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
