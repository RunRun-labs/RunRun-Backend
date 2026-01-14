document.addEventListener("DOMContentLoaded", function () {
  const form = document.getElementById("couponForm");
  const updateBtn = document.getElementById("updateBtn");
  let couponId = null;
  let couponStatus = null;

  // URL에서 쿠폰 ID 가져오기
  const pathParts = window.location.pathname.split("/");
  const updateIndex = pathParts.indexOf("update");
  if (updateIndex !== -1 && pathParts[updateIndex + 1]) {
    couponId = pathParts[updateIndex + 1];
  }

  if (!couponId) {
    alert("쿠폰 ID를 찾을 수 없습니다.");
    window.location.href = "/admin/coupon/inquiry";
    return;
  }

  // 필드 요소들
  const fields = {
    name: document.getElementById("name"),
    description: document.getElementById("description"),
    quantity: document.getElementById("quantity"),
    codeType: document.getElementById("codeType"),
    channel: document.getElementById("channel"),
    benefitType: document.getElementById("benefitType"),
    benefitValue: document.getElementById("benefitValue"),
    code: document.getElementById("code"),
    startAt: document.getElementById("startAt"),
    endAt: document.getElementById("endAt"),
    status: document.getElementById("status"),
  };

  // 혜택 값 레이블 업데이트 함수
  function updateBenefitValueLabel() {
    const benefitValueLabel = document.querySelector('label[for="benefitValue"]');
    if (!benefitValueLabel) return;

    const benefitType = fields.benefitType.value;
    let unit = "";
    
    if (benefitType === "RATE_DISCOUNT") {
      unit = "%";
    } else if (benefitType === "FIXED_DISCOUNT") {
      unit = "원";
    } else if (benefitType === "EXPERIENCE") {
      unit = "일";
    }

    // 레이블 텍스트에서 기존 단위 제거 후 새 단위 추가
    const baseText = benefitValueLabel.textContent.replace(/ \([%원일]\)/, "");
    if (unit) {
      benefitValueLabel.textContent = baseText + ` (${unit})`;
    } else {
      benefitValueLabel.textContent = baseText;
    }
  }

  // 혜택 타입 변경 시 혜택 값 레이블에 단위 표시
  if (fields.benefitType) {
    fields.benefitType.addEventListener("change", function () {
      updateBenefitValueLabel();
    });
  }

  // 쿠폰 데이터 로드
  function loadCouponData() {
    const token = getAccessToken();
    if (!token) {
      alert("로그인이 필요합니다.");
      window.location.href = "/login";
      return;
    }

    fetch(`/api/admin/coupons/${couponId}`, {
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
        if (result.success && result.data) {
          const coupon = result.data;
          couponStatus = coupon.status;

          // DRAFT 상태가 아니면 수정 불가
          if (couponStatus !== "DRAFT") {
            alert(
              `이 쿠폰은 ${couponStatus} 상태입니다. DRAFT 상태인 쿠폰만 수정할 수 있습니다.`
            );
            window.location.href = "/admin/coupon/inquiry";
            return;
          }

          // 폼에 데이터 채우기
          fields.name.value = coupon.name || "";
          fields.description.value = coupon.description || "";
          fields.quantity.value = coupon.quantity || "";
          fields.codeType.value = coupon.codeType || "";
          fields.channel.value = coupon.channel || "";
          fields.benefitType.value = coupon.benefitType || "";
          fields.benefitValue.value = coupon.benefitValue || "";
          
          // 혜택 타입에 맞게 레이블 업데이트
          updateBenefitValueLabel();
          fields.code.value = coupon.code || "";

          // 날짜 형식 변환 (LocalDateTime -> YYYY-MM-DD)
          if (coupon.startAt) {
            const startDate = new Date(coupon.startAt);
            fields.startAt.value = startDate.toISOString().split("T")[0];
          }
          if (coupon.endAt) {
            const endDate = new Date(coupon.endAt);
            fields.endAt.value = endDate.toISOString().split("T")[0];
          }

          fields.status.value = couponStatus;

          // 날짜 제한 설정
          setupDateConstraints();
        } else {
          throw new Error(result.message || "쿠폰 데이터를 불러올 수 없습니다.");
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        let errorMessage = "쿠폰 데이터를 불러오는 중 오류가 발생했습니다.";

        if (error.message) {
          errorMessage = error.message;
        } else if (error.data && error.data.message) {
          errorMessage = error.data.message;
        }

        alert(errorMessage);
        window.location.href = "/admin/coupon/inquiry";
      });
  }

  // 날짜 제한 설정
  function setupDateConstraints() {
    // 오늘 날짜를 YYYY-MM-DD 형식으로 가져오기
    const today = new Date();
    const todayStr = today.toISOString().split("T")[0];

    // 시작일: 오늘 이후만 선택 가능
    if (fields.startAt) {
      fields.startAt.setAttribute("min", todayStr);
    }

    // 종료일: 시작일 변경 시 동적으로 min 설정
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
            fields.endAt.classList.remove("valid", "error");
            const errorElement = document.getElementById("endAt-error");
            if (errorElement) {
              errorElement.textContent = "";
            }
          }
        } else {
          // 시작일이 없으면 오늘 이후만 선택 가능
          fields.endAt.setAttribute("min", todayStr);
        }
        // 종료일 재검증
        validateField("endAt", true);
      });

      // 초기값: 오늘 이후만 선택 가능
      fields.endAt.setAttribute("min", todayStr);
    }
  }

  // 벨리데이션 규칙 (CouponUpdateReqDto 기준)
  const validators = {
    name: (value) => {
      if (!value || value.trim() === "") {
        return "쿠폰명은 필수입니다.";
      }
      if (value.length < 5 || value.length > 50) {
        return "쿠폰명은 5~50자여야 합니다.";
      }
      return null;
    },

    description: (value) => {
      if (!value || value.trim() === "") {
        return "쿠폰 설명은 필수입니다.";
      }
      if (value.length < 10 || value.length > 500) {
        return "쿠폰 설명은 10~500자여야 합니다.";
      }
      return null;
    },

    quantity: (value) => {
      if (value === "" || value === null || value === undefined) {
        return "발급 수량(quantity)은 필수입니다.";
      }
      const num = parseInt(value);
      if (isNaN(num) || num < 0) {
        return "발급 수량(quantity)은 0 이상이어야 합니다.";
      }
      return null;
    },

    codeType: (value) => {
      if (!value || value === "") {
        return "쿠폰 코드 타입(codeType)은 필수입니다. (SINGLE/MULTI)";
      }
      return null;
    },

    channel: (value) => {
      if (!value || value === "") {
        return "발급 채널(channel)은 필수입니다.";
      }
      return null;
    },

    benefitType: (value) => {
      if (!value || value === "") {
        return "혜택 타입(benefitType)은 필수입니다.";
      }
      return null;
    },

    benefitValue: (value) => {
      if (value === "" || value === null || value === undefined) {
        return "혜택 값(benefitValue)은 필수입니다.";
      }
      const num = parseInt(value);
      if (isNaN(num) || num <= 0) {
        return "혜택 값(benefitValue)은 양수여야 합니다.";
      }
      return null;
    },

    startAt: (value) => {
      if (!value || value === "") {
        return "쿠폰 시작일(startAt)은 필수입니다.";
      }
      const date = new Date(value + "T00:00:00");
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (date < today) {
        return "쿠폰 시작일(startAt)은 오늘 이후여야 합니다.";
      }
      return null;
    },

    endAt: (value, formData) => {
      if (!value || value === "") {
        return "쿠폰 종료일(endAt)은 필수입니다.";
      }
      const endDate = new Date(value + "T00:00:00");
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (endDate < today) {
        return "쿠폰 종료일(endAt)은 오늘 이후여야 합니다.";
      }

      // startAt과 비교
      if (formData.startAt) {
        const startDate = new Date(formData.startAt + "T00:00:00");
        if (endDate <= startDate) {
          return "쿠폰 종료일(endAt)은 쿠폰 시작일(startAt) 이후여야 합니다.";
        }
      }

      return null;
    },
  };

  // 필드 검증 및 스타일 적용
  function validateField(fieldName, showError = true) {
    const field = fields[fieldName];
    if (!field) return true;

    const value = field.value;
    const formData = getAllFormData();
    const error = validators[fieldName](value, formData);
    const errorElement = document.getElementById(fieldName + "-error");

    // 스타일 제거
    field.classList.remove("valid", "error");

    if (error && showError) {
      field.classList.add("error");
      if (errorElement) {
        errorElement.textContent = error;
      }
      return false;
    } else if (!error && value && value.trim() !== "") {
      field.classList.add("valid");
      if (errorElement) {
        errorElement.textContent = "";
      }
      return true;
    } else {
      if (errorElement) {
        errorElement.textContent = "";
      }
      return !error;
    }
  }

  // 모든 폼 데이터 가져오기
  function getAllFormData() {
    return {
      name: fields.name.value,
      description: fields.description.value,
      quantity: fields.quantity.value,
      codeType: fields.codeType.value,
      channel: fields.channel.value,
      benefitType: fields.benefitType.value,
      benefitValue: fields.benefitValue.value,
      code: fields.code.value,
      startAt: fields.startAt.value,
      endAt: fields.endAt.value,
    };
  }

  // 전체 폼 검증
  function validateForm() {
    let isValid = true;
    const fieldNames = ["name", "description", "quantity", "codeType", "channel", "benefitType", "benefitValue", "startAt", "endAt"];

    fieldNames.forEach((fieldName) => {
      if (!validateField(fieldName, true)) {
        isValid = false;
      }
    });

    return isValid;
  }

  // 실시간 검증 이벤트 리스너
  Object.keys(fields).forEach((fieldName) => {
    const field = fields[fieldName];
    if (!field || fieldName === "code" || fieldName === "status") return; // code와 status는 읽기 전용

    // blur 이벤트: 포커스가 벗어날 때 검증
    field.addEventListener("blur", function () {
      validateField(fieldName, true);
    });

    // input 이벤트: 입력 중일 때도 검증 (에러가 있을 때만)
    field.addEventListener("input", function () {
      if (field.classList.contains("error")) {
        validateField(fieldName, true);
      }
    });

    // select 변경 시
    if (field.tagName === "SELECT") {
      field.addEventListener("change", function () {
        validateField(fieldName, true);

        // startAt이 변경되면 endAt도 재검증
        if (fieldName === "startAt") {
          validateField("endAt", true);
        }
      });
    }
  });

  // 폼 제출 처리
  if (updateBtn && form) {
    updateBtn.addEventListener("click", function (e) {
      e.preventDefault();

      // DRAFT 상태 체크
      if (couponStatus !== "DRAFT") {
        alert("DRAFT 상태인 쿠폰만 수정할 수 있습니다.");
        return;
      }

      // 전체 검증
      if (!validateForm()) {
        // 첫 번째 에러 필드로 스크롤
        const firstError = form.querySelector(".error");
        if (firstError) {
          firstError.scrollIntoView({ behavior: "smooth", block: "center" });
          firstError.focus();
        }
        return;
      }

      // 폼 데이터 수집
      const formData = getAllFormData();

      // 날짜를 LocalDateTime 형식으로 변환 (00:00:00으로 설정)
      const startDate = new Date(formData.startAt + "T00:00:00");
      const endDate = new Date(formData.endAt + "T00:00:00");

      // ISO 형식으로 변환 (로컬 시간대 기준)
      const formatDateTime = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        return `${year}-${month}-${day}T00:00:00`;
      };

      const requestData = {
        name: formData.name.trim(),
        description: formData.description.trim(),
        quantity: parseInt(formData.quantity),
        codeType: formData.codeType,
        channel: formData.channel,
        benefitType: formData.benefitType,
        benefitValue: parseInt(formData.benefitValue),
        startAt: formatDateTime(startDate),
        endAt: formatDateTime(endDate),
      };

      // 버튼 비활성화
      updateBtn.disabled = true;
      updateBtn.textContent = "수정 중...";

      // 엑세스 토큰 가져오기
      const token = getAccessToken();
      if (!token) {
        alert("로그인이 필요합니다.");
        window.location.href = "/login";
        updateBtn.disabled = false;
        updateBtn.textContent = "수정하기";
        return;
      }

      // API 호출
      fetch(`/api/admin/coupons/${couponId}`, {
        method: "PUT",
        headers: getAuthHeaders(),
        body: JSON.stringify(requestData),
      })
        .then((response) => {
          return response.json().then((data) => {
            if (!response.ok) {
              return Promise.reject(data);
            }
            return data;
          });
        })
        .then((data) => {
          if (data.success) {
            alert("쿠폰이 성공적으로 수정되었습니다.");
            window.location.href = "/admin/coupon/inquiry";
          } else {
            alert(
              "쿠폰 수정에 실패했습니다: " + (data.message || "알 수 없는 오류")
            );
          }
        })
        .catch((error) => {
          console.error("Error:", error);
          let errorMessage = "쿠폰 수정 중 오류가 발생했습니다.";

          if (error.message) {
            errorMessage = error.message;
          } else if (error.data && error.data.message) {
            errorMessage = error.data.message;
          }

          // 벨리데이션 에러 처리
          if (error.data && error.data.fieldErrors) {
            const fieldErrors = error.data.fieldErrors;
            Object.keys(fieldErrors).forEach((fieldName) => {
              const field = fields[fieldName];
              const errorElement = document.getElementById(
                fieldName + "-error"
              );
              if (field && errorElement) {
                field.classList.add("error");
                field.classList.remove("valid");
                const errorMsg = Array.isArray(fieldErrors[fieldName])
                  ? fieldErrors[fieldName][0]
                  : fieldErrors[fieldName];
                errorElement.textContent = errorMsg;
              }
            });

            // 첫 번째 에러 필드로 스크롤
            const firstError = form.querySelector(".error");
            if (firstError) {
              firstError.scrollIntoView({
                behavior: "smooth",
                block: "center",
              });
              firstError.focus();
            }
          } else {
            alert(errorMessage);
          }
        })
        .finally(() => {
          updateBtn.disabled = false;
          updateBtn.textContent = "수정하기";
        });
    });
  }

  // 페이지 로드 시 쿠폰 데이터 로드
  loadCouponData();
});

