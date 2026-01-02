document.addEventListener("DOMContentLoaded", () => {
  const backButton = document.getElementById("backBtn");
  const distanceChips = document.getElementById("distanceChips");
  const startButton = document.getElementById("startButton");

  let selectedDistance = null;

  // 거리 옵션 정의 (백엔드 DistanceType Enum과 일치)
  const distanceOptions = [
    { value: "KM_3", label: "3km" },
    { value: "KM_5", label: "5km" },
    { value: "KM_10", label: "10km" }
  ];

  // 뒤로가기 버튼
  if (backButton) {
    backButton.addEventListener("click", () => {
      window.history.length > 1
        ? window.history.back()
        : (window.location.href = "/match/select");
    });
  }

  // 거리 칩 생성
  function renderDistanceChips() {
    distanceChips.innerHTML = "";
    distanceOptions.forEach((option) => {
      const chip = document.createElement("button");
      chip.className = `distance-chip ${selectedDistance === option.value ? "active" : ""}`;
      chip.textContent = option.label;
      chip.dataset.distance = option.value;
      chip.addEventListener("click", () => {
        selectedDistance = option.value;
        renderDistanceChips();
        updateStartButton();
      });
      distanceChips.appendChild(chip);
    });
  }

  // 시작 버튼 상태 업데이트
  function updateStartButton() {
    if (selectedDistance) {
      startButton.disabled = false;
    } else {
      startButton.disabled = true;
    }
  }

  // 토큰 가져오기
  function getToken() {
    return localStorage.getItem("accessToken") || getCookie("accessToken");
  }

  // 쿠키에서 값 가져오기
  function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(";").shift();
    return null;
  }

  // 솔로런 시작 API 호출
  async function startSoloRun() {
    if (!selectedDistance) {
      alert("거리를 선택해주세요.");
      return;
    }

    const token = getToken();
    const headers = {
      "Content-Type": "application/json"
    };

    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    // 요청 데이터: courseId는 null, distance만 전송
    const requestData = {
      distance: selectedDistance,
      courseId: null
    };

    try {
      startButton.disabled = true;
      startButton.textContent = "시작 중...";

      const response = await fetch("/api/match/solorun/start", {
        method: "POST",
        headers: headers,
        body: JSON.stringify(requestData)
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      if (result.success && result.data) {
        const sessionId = result.data;
        // 세션 ID를 사용하여 러닝 페이지로 이동
        window.location.href = `/match/session/${sessionId}`;
      } else {
        throw new Error(result.message || "솔로런 시작에 실패했습니다.");
      }
    } catch (error) {
      console.error("솔로런 시작 실패:", error);
      alert(error.message || "솔로런 시작에 실패했습니다.");
      startButton.disabled = false;
      startButton.textContent = "솔로런 시작하기";
    }
  }

  // 시작 버튼 클릭 이벤트
  if (startButton) {
    startButton.addEventListener("click", startSoloRun);
  }

  // 초기화
  renderDistanceChips();
  updateStartButton();
});

