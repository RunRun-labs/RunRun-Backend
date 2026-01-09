document.addEventListener("DOMContentLoaded", () => {
  const backButton = document.querySelector(".back-button");
  const passwordInput = document.querySelector(".password-field input");
  const togglePasswordBtn = document.querySelector(".toggle-password");
  const bottomNavMount = document.getElementById("bottomNavMount");
  const bottomNavTemplate = document.getElementById("bottomNavTemplate");
  const loginForm = document.querySelector('form[data-form="login"]');
  const messageBox = document.querySelector('[data-role="login-message"]');

  if (backButton) {
    backButton.addEventListener("click", () => {
      window.history.length > 1
          ? window.history.back()
          : (window.location.href = "/");
    });
  }

  if (togglePasswordBtn && passwordInput) {
    let isVisible = false;
    const eyeIcon = togglePasswordBtn.querySelector('.eye-icon');
    
    togglePasswordBtn.addEventListener("click", () => {
      isVisible = !isVisible;
      passwordInput.type = isVisible ? "text" : "password";
      togglePasswordBtn.setAttribute(
          "aria-label",
          isVisible ? "비밀번호 숨기기" : "비밀번호 표시"
      );
      
      // 아이콘 변경
      if (eyeIcon) {
        eyeIcon.src = isVisible ? '/img/eye-open.svg' : '/img/eye-closed.svg';
        eyeIcon.alt = isVisible ? '비밀번호 표시 아이콘' : '비밀번호 감춤 아이콘';
      }
    });
  }

  if (loginForm) {
    const submitBtn = loginForm.querySelector('button[type="submit"]');
    loginForm.addEventListener("submit", async (event) => {
      event.preventDefault();

      const formData = new FormData(loginForm);
      const loginId = formData.get("loginId")?.trim();
      const loginPw = formData.get("loginPw")?.trim();

      const validationError = validateLoginFields(loginId, loginPw);
      if (validationError) {
        setMessage(validationError);
        return;
      }

      submitBtn?.setAttribute("disabled", "true");
      setMessage("로그인 중입니다...");

      try {
        const response = await fetch(loginForm.action || "/auth/login", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({loginId, loginPw}),
        });

        const result = await response.json().catch(() => ({}));

        if (!response.ok || !result?.success) {
          throw new Error(result?.message || "로그인에 실패했습니다.");
        }

        // 새로운 응답 구조: { token: TokenDto, userId: Long }
        const loginData = result.data;
        
        // 항상 덮어쓰기 위해 조건 없이 저장
        if (loginData?.token?.accessToken) {
          localStorage.setItem("accessToken", loginData.token.accessToken);
        } else {
          localStorage.removeItem("accessToken");
        }
        
        if (loginData?.token?.refreshToken) {
          localStorage.setItem("refreshToken", loginData.token.refreshToken);
        } else {
          localStorage.removeItem("refreshToken");
        }
        
        // userId는 항상 덮어쓰기 (값이 없으면 제거)
        if (loginData?.userId != null && loginData?.userId !== undefined) {
          localStorage.setItem("userId", String(loginData.userId));
        } else {
          localStorage.removeItem("userId");
        }

        setMessage("로그인 성공! 홈으로 이동합니다.");
        window.location.href = "/home";
      } catch (error) {
        setMessage(error.message || "로그인 중 오류가 발생했습니다.");
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

  function setMessage(message) {
    if (!messageBox) {
      return;
    }
    messageBox.textContent = message;
    messageBox.hidden = !message;
  }

  function validateLoginFields(loginId, loginPw) {
    if (!loginId) {
      return "아이디를 입력해 주세요.";
    }
    if (loginId.length < 4 || loginId.length > 10) {
      return "아이디는 5자 이상 10자 이하로 입력해 주세요.";
    }
    if (!loginPw) {
      return "비밀번호를 입력해 주세요.";
    }
    if (loginPw.length < 8 || loginPw.length > 18) {
      return "비밀번호는 8자 이상 18자 이하로 입력해 주세요.";
    }
    return "";
  }
});
