document.addEventListener("DOMContentLoaded", () => {
  const backButton = document.querySelector(".back-button");
  const bottomNavMount = document.getElementById("bottomNavMount");
  const bottomNavTemplate = document.getElementById("bottomNavTemplate");
  const editForm = document.querySelector('form[data-form="mypage-edit"]');
  const messageBox = document.querySelector('[data-role="edit-message"]');
  const profilePreview = document.querySelector('[data-role="profile-preview"]');
  const profileInitial = document.querySelector('[data-role="profile-initial"]');

  const fieldOrder = [
    "userEmail",
    "userName",
    "heightCm",
    "weightKg",
  ];

  const optionalFields = new Set();

  const validators = {
    userEmail: (value) => {
      if (!value) return "이메일을 입력해 주세요.";
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
        return "올바른 이메일 형식을 입력해 주세요.";
      }
      return "";
    },
    userName: (value) => {
      if (!value) return "이름을 입력해 주세요.";
      if (value.length > 4) {
        return "이름은 최대 4자까지 입력할 수 있습니다.";
      }
      return "";
    },
    heightCm: (value) => {
      if (!value) return "키를 입력해 주세요.";
      const n = Number(value);
      if (!Number.isFinite(n)) return "키는 숫자만 입력할 수 있습니다.";
      if (n < 50 || n > 300) {
        return "키는 50cm 이상 300cm 이하로 입력해 주세요.";
      }
      return "";
    },
    weightKg: (value) => {
      if (!value) return "몸무게를 입력해 주세요.";
      const n = Number(value);
      if (!Number.isFinite(n)) return "몸무게는 숫자만 입력할 수 있습니다.";
      if (n < 10 || n > 500) {
        return "몸무게는 10kg 이상 500kg 이하로 입력해 주세요.";
      }
      return "";
    },
  };

  if (backButton) {
    backButton.addEventListener("click", () => {
      if (window.history.length > 1) {
        window.history.back();
      } else {
        window.location.href = "http://localhost:8080/myPage";
      }
    });
  }

  if (editForm) {
    enableRealtimeValidation(editForm);
    loadCurrentUser();

    editForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      const formData = new FormData(editForm);
      const values = collectFormValues(formData);
      const { valid, message } = validateAllFields(values);
      decorateAllFields(values);

      if (!valid) {
        setMessage(message);
        return;
      }

      const payload = {
        userEmail: values.userEmail,
        userName: values.userName,
        heightCm: Number(values.heightCm),
        weightKg: Number(values.weightKg),
      };

      const accessToken = localStorage.getItem("accessToken");
      if (!accessToken) {
        setMessage("로그인이 필요합니다. 다시 로그인해 주세요.");
        return;
      }

      const submitBtn = editForm.querySelector('button[type="submit"]');
      submitBtn?.setAttribute("disabled", "true");
      setMessage("내 정보를 저장 중입니다...");

      try {
        const response = await fetch("/users/me", {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${accessToken}`,
          },
          body: JSON.stringify(payload),
        });

        if (!response.ok) {
          let errorMessage = "내 정보를 수정하는 중 오류가 발생했습니다.";
          try {
            const data = await response.json();
            if (data?.message) {
              errorMessage = data.message;
            }
          } catch (e) {
            // ignore parse error
          }
          throw new Error(errorMessage);
        }

        setMessage("변경 사항이 저장되었습니다.", "success");
        setTimeout(() => {
          window.location.href = "http://localhost:8080/myPage";
        }, 800);
      } catch (error) {
        setMessage(error.message || "내 정보를 수정하는 중 오류가 발생했습니다.");
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

  async function loadCurrentUser() {
    const accessToken = localStorage.getItem("accessToken");
    if (!accessToken) {
      setMessage("로그인이 필요합니다. 다시 로그인해 주세요.");
      return;
    }

    try {
      const response = await fetch("/users/me", {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      });

      if (!response.ok) {
        throw new Error("내 정보를 불러오지 못했습니다.");
      }

      const data = await response.json();
      fillFormWithUserData(data);
    } catch (error) {
      setMessage(error.message || "내 정보를 불러오는 중 오류가 발생했습니다.");
    }
  }

  function fillFormWithUserData(user) {
    if (!editForm) return;

    const emailInput = editForm.querySelector('input[name="userEmail"]');
    const nameInput = editForm.querySelector('input[name="userName"]');
    const heightInput = editForm.querySelector('input[name="heightCm"]');
    const weightInput = editForm.querySelector('input[name="weightKg"]');

    if (emailInput) emailInput.value = user.email ?? "";
    if (nameInput) nameInput.value = user.name ?? "";
    if (heightInput) heightInput.value = user.heightCm ?? "";
    if (weightInput) weightInput.value = user.weightKg ?? "";

    updateProfileVisual(user.name, user.profileImageUrl);
  }

  function updateProfileVisual(name, imageUrl) {
    if (!profilePreview || !profileInitial) return;

    if (imageUrl) {
      profilePreview.src = imageUrl;
      profilePreview.hidden = false;
      profileInitial.hidden = true;
    } else {
      profilePreview.hidden = true;
      profileInitial.hidden = false;
      profileInitial.textContent = makeInitial(name);
    }
  }

  function makeInitial(name) {
    if (!name) return "RR";
    const trimmed = name.toString().trim();
    return trimmed ? trimmed.charAt(0) : "RR";
  }

  function enableRealtimeValidation(form) {
    fieldOrder.forEach((name) => {
      const input = form.querySelector(`[name="${name}"]`);
      if (!input) return;
      const eventName = "input";
      input.addEventListener(eventName, () => {
        const values = collectFormValues(new FormData(form));
        validateAndDecorateField(name, values);
        setMessage("");
      });
    });
  }

  function collectFormValues(formData) {
    return {
      userEmail: getTrimmedValue(formData, "userEmail"),
      userName: getTrimmedValue(formData, "userName"),
      heightCm: getTrimmedValue(formData, "heightCm"),
      weightKg: getTrimmedValue(formData, "weightKg"),
    };
  }

  function getTrimmedValue(formData, key) {
    const raw = formData.get(key);
    return raw === null ? "" : raw.toString().trim();
  }

  function setMessage(message, type = "error") {
    if (!messageBox) return;
    messageBox.textContent = message;
    messageBox.hidden = !message;
    messageBox.classList.remove("success");
    if (type === "success") {
      messageBox.classList.add("success");
    }
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
      editForm?.querySelector(`[name="${name}"]`) ??
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


