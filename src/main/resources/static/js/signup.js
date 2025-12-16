document.addEventListener("DOMContentLoaded", () => {
  const backButton = document.querySelector(".back-button");
  const toggleButtons = Array.from(
    document.querySelectorAll(".toggle-password")
  );
  const genderButtons = Array.from(document.querySelectorAll(".gender-btn"));
  const genderHidden = document.querySelector('input[name="gender"]');
  const bottomNavMount = document.getElementById("bottomNavMount");
  const bottomNavTemplate = document.getElementById("bottomNavTemplate");
  const signupForm = document.querySelector('form[data-form="signup"]');
  const messageBox = document.querySelector('[data-role="signup-message"]');
  const optionalFields = new Set(["heightCm", "weightKg"]);
  const fieldOrder = [
    "userName",
    "loginId",
    "userPassword",
    "confirmPassword",
    "userEmail",
    "birthDate",
    "gender",
    "heightCm",
    "weightKg",
  ];

  const validators = {
    userName: (value) => {
      if (!value) return "이름을 입력해 주세요.";
      if (value.length > 4) return "이름은 최대 4자까지 입력할 수 있습니다.";
      return "";
    },
    loginId: (value) => {
      if (!value) return "아이디를 입력해 주세요.";
      if (value.length < 5 || value.length > 10) {
        return "아이디는 5자 이상 10자 이하로 입력해 주세요.";
      }
      return "";
    },
    userPassword: (value) => {
      if (!value) return "비밀번호를 입력해 주세요.";
      if (value.length < 8 || value.length > 18) {
        return "비밀번호는 8자 이상 18자 이하로 입력해 주세요.";
      }
      return "";
    },
    confirmPassword: (value, values) => {
      if (!value) return "비밀번호 확인을 입력해 주세요.";
      if (value !== values.userPassword) {
        return "비밀번호가 일치하지 않습니다.";
      }
      return "";
    },
    userEmail: (value) => {
      if (!value) return "이메일을 입력해 주세요.";
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
        return "올바른 이메일 형식을 입력해 주세요.";
      }
      return "";
    },
    birthDate: (value) => {
      if (!value) return "생년월일을 선택해 주세요.";
      const birth = new Date(value);
      const today = new Date();
      if (Number.isNaN(birth.getTime())) {
        return "생년월일 형식이 올바르지 않습니다.";
      }
      if (birth > today) {
        return "생년월일은 오늘 이후일 수 없습니다.";
      }
      return "";
    },
    heightCm: (value) => {
      if (!value) return "";
      const heightNumber = Number(value);
      if (!Number.isFinite(heightNumber)) {
        return "키는 숫자만 입력할 수 있습니다.";
      }
      if (heightNumber < 50 || heightNumber > 300) {
        return "키는 50cm 이상 300cm 이하로 입력해 주세요.";
      }
      return "";
    },
    weightKg: (value) => {
      if (!value) return "";
      const weightNumber = Number(value);
      if (!Number.isFinite(weightNumber)) {
        return "몸무게는 숫자만 입력할 수 있습니다.";
      }
      if (weightNumber < 10 || weightNumber > 500) {
        return "몸무게는 10kg 이상 500kg 이하로 입력해 주세요.";
      }
      return "";
    },
    gender: (value) => {
      if (!value) return "성별을 선택해 주세요.";
      if (!["M", "F"].includes(value)) {
        return "성별 값을 다시 선택해 주세요.";
      }
      return "";
    },
  };

  if (backButton) {
    backButton.addEventListener("click", () => {
      window.history.length > 1
        ? window.history.back()
        : (window.location.href = "/login");
    });
  }

  toggleButtons.forEach((btn) => {
    const input = btn.parentElement?.querySelector("input");
    if (!input) return;
    let visible = false;
    btn.addEventListener("click", () => {
      visible = !visible;
      input.type = visible ? "text" : "password";
      btn.setAttribute(
        "aria-label",
        visible ? "비밀번호 숨기기" : "비밀번호 표시"
      );
    });
  });

  genderButtons.forEach((btn) => {
    btn.addEventListener("click", () => {
      const genderValue = btn.dataset.gender || "M";
      genderButtons.forEach((b) => {
        const isActive = b === btn;
        b.classList.toggle("active", isActive);
        b.setAttribute("aria-pressed", String(isActive));
      });
      if (genderHidden) {
        genderHidden.value = genderValue;
        const values = collectFormValues(new FormData(signupForm));
        validateAndDecorateField("gender", values);
      }
    });
  });

  if (signupForm) {
    const submitBtn = signupForm.querySelector('button[type="submit"]');

    enableRealtimeValidation(signupForm);

    signupForm.addEventListener("submit", async (event) => {
      event.preventDefault();

      const formData = new FormData(signupForm);
      const values = collectFormValues(formData);
      const { valid, message } = validateAllFields(values);
      decorateAllFields(values);

      if (!valid) {
        setMessage(message);
        return;
      }

      const payload = {
        loginId: values.loginId,
        userPassword: values.userPassword,
        userName: values.userName,
        userEmail: values.userEmail,
        birthDate: values.birthDate,
        gender: values.gender,
        heightCm: parseOptionalNumber(values.heightCm),
        weightKg: parseOptionalNumber(values.weightKg),
      };

      submitBtn?.setAttribute("disabled", "true");
      setMessage("회원가입 중입니다...");

      try {
        const response = await fetch(signupForm.action || "/auth/signup", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(payload),
        });

        const result = await response.json().catch(() => ({}));
        if (!response.ok || !result?.success) {
          throw new Error(result?.message || "회원가입에 실패했습니다.");
        }

        setMessage("회원가입이 완료되었습니다. 로그인 페이지로 이동합니다.");
        window.location.href = "/login";
      } catch (error) {
        setMessage(error.message || "회원가입 중 오류가 발생했습니다.");
      } finally {
        submitBtn?.removeAttribute("disabled");
      }
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

  function enableRealtimeValidation(form) {
    fieldOrder.forEach((name) => {
      const input = form.querySelector(`[name="${name}"]`);
      if (!input) {
        return;
      }
      const eventName = input.type === "date" ? "change" : "input";
      input.addEventListener(eventName, () => {
        const values = collectFormValues(new FormData(form));
        validateAndDecorateField(name, values);
        if (name === "userPassword") {
          validateAndDecorateField("confirmPassword", values);
        }
        setMessage("");
      });
    });
  }

  function collectFormValues(formData) {
    return {
      loginId: getTrimmedValue(formData, "loginId"),
      userPassword: getTrimmedValue(formData, "userPassword"),
      confirmPassword: getTrimmedValue(formData, "confirmPassword"),
      userName: getTrimmedValue(formData, "userName"),
      userEmail: getTrimmedValue(formData, "userEmail"),
      birthDate: getTrimmedValue(formData, "birthDate"),
      gender: (formData.get("gender") || "M").toString(),
      heightCm: getTrimmedValue(formData, "heightCm"),
      weightKg: getTrimmedValue(formData, "weightKg"),
    };
  }

  function getTrimmedValue(formData, key) {
    const raw = formData.get(key);
    return raw === null ? "" : raw.toString().trim();
  }

  function setMessage(message) {
    if (!messageBox) return;
    messageBox.textContent = message;
    messageBox.hidden = !message;
  }

  function parseOptionalNumber(value) {
    if (!value) {
      return null;
    }
    const asNumber = Number(value);
    return Number.isFinite(asNumber) ? asNumber : null;
  }

  function validateAllFields(values) {
    for (const name of fieldOrder) {
      const { valid, message } = evaluateField(name, values);
      if (!valid) {
        return { valid: false, message };
      }
    }
    return { valid: true, message: "" };
  }

  function validateAndDecorateField(name, values) {
    const input =
      signupForm?.querySelector(`[name="${name}"]`) ??
      document.querySelector(`[name="${name}"]`);
    const { valid, message } = evaluateField(name, values);

    const value = values[name];
    const shouldDecorate =
      !optionalFields.has(name) || (value !== undefined && value !== "");

    if (input) {
      setInputState(input, shouldDecorate ? (valid ? "valid" : "invalid") : "");
    }

    setFieldMessage(name, shouldDecorate ? (valid ? "" : message) : "");

    return { valid, message };
  }

  function decorateAllFields(values) {
    fieldOrder.forEach((name) => validateAndDecorateField(name, values));
  }

  function evaluateField(name, values) {
    const validator = validators[name];
    if (!validator) {
      return { valid: true, message: "" };
    }
    const message = validator(values[name], values);
    return { valid: !message, message };
  }

  function setInputState(input, state) {
    input.classList.remove("input-valid", "input-invalid");
    if (!state) {
      return;
    }
    input.classList.add(state === "valid" ? "input-valid" : "input-invalid");
  }

  function setFieldMessage(name, message) {
    const feedback = document.querySelector(`[data-feedback="${name}"]`);
    if (!feedback) return;
    feedback.textContent = message;
    feedback.classList.toggle("hidden", !message);
  }
});
