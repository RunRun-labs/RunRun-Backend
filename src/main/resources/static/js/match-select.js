document.addEventListener("DOMContentLoaded", () => {
  const backButton = document.querySelector(".back-button");
  const matchButtons = document.querySelectorAll(".match-button");
  const bottomNavMount = document.getElementById("bottomNavMount");

  if (backButton) {
    backButton.addEventListener("click", () => {
      window.history.length > 1
          ? window.history.back()
          : (window.location.href = "/");
    });
  }

  matchButtons.forEach((button) => {
    button.addEventListener("click", () => {
      const matchType = button.getAttribute("data-match-type");
      handleMatchSelection(matchType);
    });
  });

  function handleMatchSelection(matchType) {
    // 매칭 타입에 따른 라우팅
    const routes = {
      online: "/match/online",
      offline: "/recruit",
      ghost: "/match/ghost?mode=select",
      solo: "/match/solo"
    };

    const route = routes[matchType];
    if (route) {
      window.location.href = route;
    }
  }

  if (bottomNavMount && bottomNavMount.childElementCount === 0) {

  }
});




