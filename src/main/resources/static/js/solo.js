document.addEventListener("DOMContentLoaded", () => {
  const backButton = document.getElementById("backBtn");
  const distanceChips = document.getElementById("distanceChips");
  const startButton = document.getElementById("startButton");
  const manualDistanceInput = document.getElementById("manualDistanceInput");
  const infoBox = document.getElementById("infoBox");
  const infoBoxTitle = document.getElementById("infoBoxTitle");
  const infoBoxText = document.getElementById("infoBoxText");
  const manualInputLabel = document.getElementById("manualInputLabel");
  const courseModeNotice = document.getElementById("courseModeNotice");

  let selectedDistance = null;
  let manualDistanceValue = null;
  let courseId = null;
  let isCourseMode = false;

  // URL 파라미터 읽기
  const urlParams = new URLSearchParams(window.location.search);
  const urlCourseId = urlParams.get("courseId");
  const urlDistance = urlParams.get("distance");
  const urlTitle = urlParams.get("title");

  // courseId가 있으면 코스 모드 활성화
  if (urlCourseId) {
    courseId = urlCourseId;
    isCourseMode = true;
    
    let convertedDistance = null;
    
    // distance 값이 있으면 입력창에 채우기
    if (urlDistance) {
      let distanceValue = parseFloat(urlDistance);
      if (!isNaN(distanceValue) && distanceValue > 0) {
        // 1000 이상이면 미터 단위로 간주하고 km로 변환 (4000m -> 4km)
        if (distanceValue >= 1000) {
          distanceValue = distanceValue / 1000;
        }
        convertedDistance = distanceValue;
        manualDistanceValue = distanceValue;
        if (manualDistanceInput) {
          manualDistanceInput.value = distanceValue;
          manualDistanceInput.readOnly = true;
        }
      }
    }

    // 코스 모드 UI 업데이트
    if (infoBox) {
      infoBox.classList.add("course-mode");
    }
    
    if (infoBoxTitle && infoBoxText) {
      const courseTitle = urlTitle || (convertedDistance ? `${convertedDistance}km` : "선택하신");
      // title 부분을 강조하기 위해 HTML 구조 변경
      infoBoxTitle.innerHTML = `선택하신 <span class="course-title-highlight">${courseTitle}</span> 코스로 목표 거리가 설정되었습니다`;
      infoBoxText.textContent = "코스 정보에 따라 솔로런을 시작합니다";
    }

    if (manualInputLabel) {
      manualInputLabel.textContent = "코스 지정 거리";
    }

    if (courseModeNotice) {
      courseModeNotice.style.display = "block";
    }
  }

  // 거리 옵션 정의 (백엔드 DistanceType Enum과 일치)
  const distanceOptions = [
    { value: "KM_3", label: "3km", numericValue: 3 },
    { value: "KM_5", label: "5km", numericValue: 5 },
    { value: "KM_10", label: "10km", numericValue: 10 }
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
      
      // 코스 모드일 때 버튼 비활성화
      if (isCourseMode) {
        chip.disabled = true;
      } else {
        chip.addEventListener("click", () => {
          selectedDistance = option.value;
          manualDistanceValue = null;
          // 버튼 클릭 시 입력창에 해당 거리 값 채우기
          if (manualDistanceInput) {
            manualDistanceInput.value = option.numericValue;
            manualDistanceInput.readOnly = false;
          }
          renderDistanceChips();
          updateStartButton();
        });
      }
      
      distanceChips.appendChild(chip);
    });
  }

  // 시작 버튼 상태 업데이트
  function updateStartButton() {
    // 코스 모드일 때는 항상 활성화 (courseId가 있으면)
    if (isCourseMode && courseId) {
      startButton.disabled = false;
      return;
    }
    
    const hasValidDistance = selectedDistance || (manualDistanceValue && manualDistanceValue > 0);
    if (hasValidDistance) {
      startButton.disabled = false;
    } else {
      startButton.disabled = true;
    }
  }

  // 직접 입력창 이벤트 핸들러 (코스 모드가 아닐 때만)
  if (manualDistanceInput && !isCourseMode) {
    manualDistanceInput.addEventListener("input", (e) => {
      const value = parseFloat(e.target.value);
      // 직접 입력 시 버튼 선택 해제
      if (!isNaN(value) && value > 0) {
        selectedDistance = null;
        manualDistanceValue = value;
        renderDistanceChips();
      } else if (e.target.value === "" || e.target.value === null) {
        manualDistanceValue = null;
      } else {
        manualDistanceValue = null;
      }
      updateStartButton();
    });

    // 입력창 포커스 아웃 시 유효성 검사
    manualDistanceInput.addEventListener("blur", (e) => {
      const value = parseFloat(e.target.value);
      if (e.target.value !== "" && (isNaN(value) || value <= 0)) {
        e.target.value = "";
        manualDistanceValue = null;
        updateStartButton();
      }
    });
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
    // 코스 모드가 아닐 때만 유효성 검사
    if (!isCourseMode) {
      if (!selectedDistance && (!manualDistanceValue || manualDistanceValue <= 0)) {
        alert("목표 거리를 입력해주세요");
        return;
      }
    }

    const token = getToken();
    const headers = {
      "Content-Type": "application/json"
    };

    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    // 요청 데이터: courseId가 있으면 courseId 포함, 없으면 manualDistance만 전송
    const requestData = {
      distance: selectedDistance || null,
      manualDistance: isCourseMode ? (manualDistanceValue || null) : (manualDistanceValue || null),
      courseId: courseId ? parseInt(courseId) : null
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

