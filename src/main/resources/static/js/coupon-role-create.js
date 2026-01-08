document.addEventListener("DOMContentLoaded", function () {
  const form = document.getElementById("couponRoleForm");
  const createBtn = document.getElementById("createBtn");
  const selectCouponBtn = document.getElementById("selectCouponBtn");

  // 필드 요소들
  const fields = {
    name: document.getElementById("name"),
    couponId: document.getElementById("couponId"),
    couponName: document.getElementById("couponName"),
    triggerEvent: document.getElementById("triggerEvent"),
    conditionValue: document.getElementById("conditionValue"),
  };

  // 쿠폰 선택 버튼 클릭
  if (selectCouponBtn) {
    selectCouponBtn.addEventListener("click", function () {
      // 현재 폼 데이터를 sessionStorage에 저장
      const formData = {
        name: fields.name.value,
        triggerEvent: fields.triggerEvent.value,
        conditionValue: fields.conditionValue.value,
      };
      sessionStorage.setItem("couponRoleFormData", JSON.stringify(formData));
      sessionStorage.setItem("returnPage", "/admin/coupon-role/create");
      window.location.href = "/admin/coupon/select";
    });
  }

  // 페이지 로드 시 저장된 폼 데이터 복원
  const savedFormData = sessionStorage.getItem("couponRoleFormData");
  if (savedFormData) {
    try {
      const data = JSON.parse(savedFormData);
      fields.name.value = data.name || "";
      fields.triggerEvent.value = data.triggerEvent || "";
      fields.conditionValue.value = data.conditionValue || "";
      sessionStorage.removeItem("couponRoleFormData");
    } catch (e) {
      console.error("Failed to restore form data:", e);
    }
  }

  // 선택된 쿠폰 정보 복원
  const selectedCoupon = sessionStorage.getItem("selectedCoupon");
  if (selectedCoupon) {
    try {
      const coupon = JSON.parse(selectedCoupon);
      fields.couponId.value = coupon.id;
      fields.couponName.value = coupon.name;
      sessionStorage.removeItem("selectedCoupon");
    } catch (e) {
      console.error("Failed to restore selected coupon:", e);
    }
  }

  // 벨리데이션 규칙
  const validators = {
    name: (value) => {
      if (!value || value.trim() === "") {
        return "정책명은 필수입니다.";
      }
      if (value.length > 50) {
        return "정책명은 최대 50자까지 가능합니다.";
      }
      return null;
    },

    couponId: (value) => {
      if (!value || value === "") {
        return "쿠폰은 필수입니다.";
      }
      return null;
    },

    triggerEvent: (value) => {
      if (!value || value === "") {
        return "트리거 이벤트는 필수입니다.";
      }
      return null;
    },

    conditionValue: (value, formData) => {
      if (formData.triggerEvent === "RUN_COUNT_REACHED") {
        if (!value || value === "" || value === null || value === undefined) {
          return "RUN_COUNT_REACHED는 조건값(1 이상)이 필요합니다.";
        }
        const num = parseInt(value);
        if (isNaN(num) || num <= 0) {
          return "조건값은 1 이상이어야 합니다.";
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
      couponId: fields.couponId.value,
      triggerEvent: fields.triggerEvent.value,
      conditionValue: fields.conditionValue.value,
    };
  }

  // 전체 폼 검증
  function validateForm() {
    let isValid = true;
    const fieldNames = ["name", "couponId", "triggerEvent", "conditionValue"];

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
    if (!field || fieldName === "couponName") return;

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

        // triggerEvent가 변경되면 conditionValue도 재검증
        if (fieldName === "triggerEvent") {
          validateField("conditionValue", true);
        }
      });
    }
  });

  // 폼 제출 처리
  if (createBtn && form) {
    createBtn.addEventListener("click", function (e) {
      e.preventDefault();

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

      const requestData = {
        name: formData.name.trim(),
        couponId: parseInt(formData.couponId),
        triggerEvent: formData.triggerEvent,
        conditionValue: formData.conditionValue
          ? parseInt(formData.conditionValue)
          : null,
      };

      // 버튼 비활성화
      createBtn.disabled = true;
      createBtn.textContent = "생성 중...";

      // 엑세스 토큰 가져오기
      const token = getAccessToken();
      if (!token) {
        alert("로그인이 필요합니다.");
        window.location.href = "/login";
        createBtn.disabled = false;
        createBtn.textContent = "생성하기";
        return;
      }

      // API 호출
      fetch("/api/admin/coupon-roles", {
        method: "POST",
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
            alert("쿠폰 정책이 성공적으로 생성되었습니다.");
            window.location.href = "/admin/coupon-role/inquiry";
          } else {
            alert(
              "쿠폰 정책 생성에 실패했습니다: " +
                (data.message || "알 수 없는 오류")
            );
          }
        })
        .catch((error) => {
          console.error("Error:", error);
          let errorMessage = "쿠폰 정책 생성 중 오류가 발생했습니다.";

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
          createBtn.disabled = false;
          createBtn.textContent = "생성하기";
        });
    });
  }
});
