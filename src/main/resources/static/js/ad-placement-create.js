document.addEventListener("DOMContentLoaded", function () {
  const form = document.getElementById("adPlacementForm");
  const createBtn = document.getElementById("createBtn");
  const slotSelect = document.getElementById("slotId");
  const adSelect = document.getElementById("adId");

  // 필드 요소들
  const fields = {
    slotId: slotSelect,
    adId: adSelect,
    weight: document.getElementById("weight"),
    startAt: document.getElementById("startAt"),
    endAt: document.getElementById("endAt"),
  };

  // 슬롯 목록 로드
  async function loadSlots() {
    try {
      const response = await fetch("/api/admin/ad-slot?page=0&size=1000", {
        method: "GET",
        headers: getAuthHeaders(),
      });
      const result = await response.json();
      if (result.success && result.data && result.data.content) {
        slotSelect.innerHTML = '<option value="">선택하세요</option>';
        result.data.content.forEach((slot) => {
          const option = document.createElement("option");
          option.value = slot.slotId;
          option.textContent = `${slot.name} (${slot.slotType})`;
          slotSelect.appendChild(option);
        });
      }
    } catch (error) {
      console.error("Error loading slots:", error);
    }
  }

  // 광고 목록 로드
  async function loadAds() {
    try {
      const response = await fetch("/api/admin/ads?page=0&size=1000", {
        method: "GET",
        headers: getAuthHeaders(),
      });
      const result = await response.json();
      if (result.success && result.data && result.data.content) {
        adSelect.innerHTML = '<option value="">선택하세요</option>';
        result.data.content.forEach((ad) => {
          const option = document.createElement("option");
          option.value = ad.id;
          option.textContent = ad.name;
          adSelect.appendChild(option);
        });
      }
    } catch (error) {
      console.error("Error loading ads:", error);
    }
  }

  // 날짜 제한 설정
  const now = new Date();
  const nowStr = now.toISOString().slice(0, 16);

  if (fields.startAt) {
    fields.startAt.setAttribute("min", nowStr);
  }

  if (fields.startAt && fields.endAt) {
    fields.startAt.addEventListener("change", function () {
      const startValue = fields.startAt.value;
      if (startValue) {
        fields.endAt.setAttribute("min", startValue);
        if (fields.endAt.value && fields.endAt.value <= startValue) {
          fields.endAt.value = "";
        }
      }
    });

    fields.endAt.setAttribute("min", nowStr);
  }

  // 벨리데이션 규칙
  const validators = {
    slotId: (value) => {
      if (!value || value === "") {
        return "슬롯은 필수입니다.";
      }
      return null;
    },

    adId: (value) => {
      if (!value || value === "") {
        return "광고는 필수입니다.";
      }
      return null;
    },

    weight: (value) => {
      if (value === "" || value === null || value === undefined) {
        return "가중치는 필수입니다.";
      }
      const num = parseInt(value);
      if (isNaN(num) || num < 1) {
        return "가중치는 1 이상이어야 합니다.";
      }
      return null;
    },

    startAt: (value) => {
      if (!value || value === "") {
        return "시작일은 필수입니다.";
      }
      return null;
    },

    endAt: (value) => {
      if (!value || value === "") {
        return "종료일은 필수입니다.";
      }
      if (fields.startAt.value && value <= fields.startAt.value) {
        return "종료일은 시작일 이후여야 합니다.";
      }
      return null;
    },
  };

  // 필드 검증
  function validateField(fieldName, showError = true) {
    const field = fields[fieldName];
    if (!field) return false;

    const value = field.value;
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
      field.addEventListener("blur", () => validateField(fieldName));
      if (fieldName !== "slotId" && fieldName !== "adId") {
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

    // datetime-local을 LocalDateTime 형식으로 변환
    const startAtValue = fields.startAt.value;
    const endAtValue = fields.endAt.value;

    const startAt = startAtValue ? new Date(startAtValue).toISOString() : null;
    const endAt = endAtValue ? new Date(endAtValue).toISOString() : null;

    const data = {
      slotId: parseInt(fields.slotId.value),
      adId: parseInt(fields.adId.value),
      weight: parseInt(fields.weight.value),
      startAt: startAt,
      endAt: endAt,
    };

    try {
      createBtn.disabled = true;
      createBtn.textContent = "생성 중...";

      const response = await fetch("/api/admin/ad-placements", {
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
        alert("광고 배치가 생성되었습니다.");
        window.location.href = "/admin/ad-placement/inquiry";
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

  // 초기 로드
  loadSlots();
  loadAds();
});
