document.addEventListener("DOMContentLoaded", function () {
  const form = document.getElementById("adPlacementForm");
  const updateBtn = document.getElementById("updateBtn");
  const slotSelect = document.getElementById("slotId");
  const adSelect = document.getElementById("adId");
  let placementId = null;
  let canChangeSlotAndAd = true;

  // URL에서 배치 ID 가져오기
  const pathParts = window.location.pathname.split("/");
  const updateIndex = pathParts.indexOf("update");
  if (updateIndex !== -1 && pathParts[updateIndex + 1]) {
    placementId = pathParts[updateIndex + 1];
  }

  if (!placementId) {
    alert("배치 ID를 찾을 수 없습니다.");
    window.location.href = "/admin/ad-placement/inquiry";
    return;
  }

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

  // 배치 데이터 로드
  async function loadPlacementData() {
    try {
      const response = await fetch(`/api/admin/ad-placements?page=0&size=1000`, {
        method: "GET",
        headers: getAuthHeaders(),
      });
      const result = await response.json();
      if (result.success && result.data && result.data.content) {
        const placement = result.data.content.find(
          (p) => p.placementId === parseInt(placementId)
        );

        if (!placement) {
          throw new Error("배치를 찾을 수 없습니다.");
        }

        // 활성화 상태인 배치는 수정 불가
        if (placement.isActive) {
          alert("활성화된 배치는 수정할 수 없습니다. 먼저 비활성화해주세요.");
          window.location.href = "/admin/ad-placement/inquiry";
          return;
        }

        // 비활성화 상태이거나 시작일이 시작되지 않았으면 슬롯/광고 변경 가능
        const now = new Date();
        const startAt = new Date(placement.startAt);
        // 비활성화 상태이거나 시작일 전이면 변경 가능
        canChangeSlotAndAd = !placement.isActive || now < startAt;

        if (!canChangeSlotAndAd) {
          slotSelect.disabled = true;
          adSelect.disabled = true;
          document.getElementById("slotId-help").style.display = "block";
          document.getElementById("adId-help").style.display = "block";
        }

        // 폼에 데이터 채우기
        fields.slotId.value = placement.slotId;
        fields.adId.value = placement.adId;
        fields.weight.value = placement.weight || 1;

        // date 형식으로 변환 (YYYY-MM-DD)
        const startAtDate = new Date(placement.startAt);
        const endAtDate = new Date(placement.endAt);
        const startAtStr = startAtDate.toISOString().split("T")[0];
        const endAtStr = endAtDate.toISOString().split("T")[0];
        fields.startAt.value = startAtStr;
        fields.endAt.value = endAtStr;
      } else {
        throw new Error(result.message || "배치 데이터를 불러올 수 없습니다.");
      }
    } catch (error) {
      console.error("Error:", error);
      alert(error.message || "배치 데이터를 불러오는 중 오류가 발생했습니다.");
      window.location.href = "/admin/ad-placement/inquiry";
    }
  }

  // 날짜 제한 설정 (type="date" 사용)
  const today = new Date();
  const todayStr = today.toISOString().split("T")[0];

  if (fields.startAt && fields.endAt) {
    fields.startAt.addEventListener("change", function () {
      const startDate = fields.startAt.value;
      if (startDate) {
        // 시작일 다음 날을 min으로 설정
        const start = new Date(startDate);
        start.setDate(start.getDate() + 1);
        const nextDayStr = start.toISOString().split("T")[0];
        fields.endAt.setAttribute("min", nextDayStr);

        // 종료일이 시작일 이전이면 초기화
        if (fields.endAt.value && fields.endAt.value <= startDate) {
          fields.endAt.value = "";
        }
      } else {
        fields.endAt.setAttribute("min", todayStr);
      }
      validateField("endAt", true);
    });
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

  // 수정 버튼 클릭
  updateBtn.addEventListener("click", async function () {
    if (!validateForm()) {
      alert("입력한 정보를 확인해주세요.");
      return;
    }

    // date를 LocalDateTime 형식으로 변환 (00:00:00으로 설정)
    const startAtValue = fields.startAt.value;
    const endAtValue = fields.endAt.value;

    const startAt = startAtValue ? `${startAtValue}T00:00:00` : null;
    const endAt = endAtValue ? `${endAtValue}T00:00:00` : null;

    const data = {
      slotId: parseInt(fields.slotId.value),
      adId: parseInt(fields.adId.value),
      weight: parseInt(fields.weight.value),
      startAt: startAt,
      endAt: endAt,
    };

    try {
      updateBtn.disabled = true;
      updateBtn.textContent = "수정 중...";

      const response = await fetch(`/api/admin/ad-placements/${placementId}`, {
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
        alert("광고 배치가 수정되었습니다.");
        window.location.href = `/admin/ad-placement/detail/${placementId}`;
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

  // 초기 로드
  loadSlots();
  loadAds();
  loadPlacementData();
});
