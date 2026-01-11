document.addEventListener("DOMContentLoaded", function () {
  const form = document.getElementById("adSlotForm");
  const updateBtn = document.getElementById("updateBtn");
  let slotId = null;

  // URL에서 슬롯 ID 가져오기
  const pathParts = window.location.pathname.split("/");
  const updateIndex = pathParts.indexOf("update");
  if (updateIndex !== -1 && pathParts[updateIndex + 1]) {
    slotId = pathParts[updateIndex + 1];
  }

  if (!slotId) {
    alert("슬롯 ID를 찾을 수 없습니다.");
    window.location.href = "/admin/ad-slot/inquiry";
    return;
  }

  // 필드 요소들
  const fields = {
    name: document.getElementById("name"),
    slotType: document.getElementById("slotType"),
    dailyLimit: document.getElementById("dailyLimit"),
    allowPremium: document.querySelector('input[name="allowPremium"]'),
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
      // 체크박스는 항상 boolean 값이므로 항상 유효
      return null;
    },
  };

  // 필드 검증
  function validateField(fieldName, showError = true) {
    const field = fields[fieldName];
    if (!field) return false;

    let value;
    if (fieldName === "allowPremium") {
      value = field.checked;
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
        field.addEventListener("change", () => {
          validateField("allowPremium");
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

  // 슬롯 데이터 로드 (목록 API에서 찾기)
  function loadSlotData() {
    fetch(`/api/admin/ad-slot?page=0&size=1000`, {
      method: "GET",
      headers: getAuthHeaders(),
    })
      .then((response) => {
        return response.json().then((data) => {
          if (!response.ok) {
            return Promise.reject(data);
          }
          return data;
        });
      })
      .then((result) => {
        if (result.success && result.data && result.data.content) {
          const slot = result.data.content.find((s) => s.slotId === parseInt(slotId));
          
          if (!slot) {
            throw new Error("슬롯을 찾을 수 없습니다.");
          }

          // 활성화 상태인 슬롯은 수정 불가
          if (slot.status === "ENABLED") {
            alert("활성화된 슬롯은 수정할 수 없습니다. 먼저 비활성화해주세요.");
            window.location.href = "/admin/ad-slot/inquiry";
            return;
          }

          // 폼에 데이터 채우기
          fields.name.value = slot.name || "";
          fields.slotType.value = slot.slotType || "";
          fields.dailyLimit.value = slot.dailyLimit || 0;
          
          fields.allowPremium.checked = slot.allowPremium || false;
        } else {
          throw new Error(result.message || "슬롯 데이터를 불러올 수 없습니다.");
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        let errorMessage = "슬롯 데이터를 불러오는 중 오류가 발생했습니다.";
        if (error.message) {
          errorMessage = error.message;
        }
        alert(errorMessage);
        window.location.href = "/admin/ad-slot/inquiry";
      });
  }

  // 수정 버튼 클릭
  updateBtn.addEventListener("click", async function () {
    if (!validateForm()) {
      alert("입력한 정보를 확인해주세요.");
      return;
    }

    const allowPremium = fields.allowPremium.checked;

    const data = {
      name: fields.name.value.trim(),
      slotType: fields.slotType.value,
      dailyLimit: parseInt(fields.dailyLimit.value),
      allowPremium: allowPremium,
    };

    try {
      updateBtn.disabled = true;
      updateBtn.textContent = "수정 중...";

      const response = await fetch(`/api/admin/ad-slot/${slotId}`, {
        method: "PUT",
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
        throw new Error(result.message || "수정에 실패했습니다.");
      }

      if (result.success) {
        alert("광고 슬롯이 수정되었습니다.");
        window.location.href = "/admin/ad-slot/inquiry";
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
  loadSlotData();
});
