document.addEventListener("DOMContentLoaded", () => {
  const backButton = document.getElementById("backBtn");
  const distanceChips = document.getElementById("distanceChips");
  let startButton = document.getElementById("startButton");
  const manualDistanceInput = document.getElementById("manualDistanceInput");
  const infoBox = document.getElementById("infoBox");
  const infoBoxTitle = document.getElementById("infoBoxTitle");
  const infoBoxText = document.getElementById("infoBoxText");
  const manualInputLabel = document.getElementById("manualInputLabel");
  const courseInput = document.getElementById("course");
  const courseSection = document.querySelector(".course-section");
  const selectedCourseInfo = document.getElementById("selectedCourseInfo");
  const courseName = document.getElementById("courseName");
  const courseAddress = document.getElementById("courseAddress");
  const mapSection = document.getElementById("mapSection");
  const mapContainer = document.getElementById("map");
  const initialSelectionSection = document.getElementById(
      "initialSelectionSection");
  const courseSelectionButton = document.getElementById(
      "courseSelectionButton");
  const manualInputButton = document.getElementById("manualInputButton");
  const distanceSection = document.getElementById("distanceSection");

  let selectedDistance = null;
  let manualDistanceValue = null;
  let courseId = null;
  let isCourseMode = false;
  let isManualInputMode = false;
  let map = null;
  let coursePolyline = null;
  let selectedCourseData = null;
  let startMarker = null;
  let endMarker = null;

  // 코스 입력 필드 클릭 이벤트 (모집글 생성 페이지와 동일)
  const courseInputWrapper = document.querySelector('.course-input-wrapper');

  const handleCourseClick = () => {
    console.log('코스 선택 클릭됨');
    const returnTo = window.location.pathname + window.location.search;
    const targetUrl = `/course?selectMode=solo&returnTo=${encodeURIComponent(
        returnTo)}`;
    console.log('이동할 URL:', targetUrl);
    window.location.href = targetUrl;
  };

  if (courseInput) {
    console.log('솔로런 코스 입력 필드 이벤트 리스너 등록됨');
    courseInput.addEventListener("click", handleCourseClick);
    courseInput.style.cursor = 'pointer';
  } else {
    console.error('코스 입력 필드를 찾을 수 없습니다!');
  }

  // wrapper에도 클릭 이벤트 추가 (확실하게 하기 위해)
  if (courseInputWrapper) {
    console.log('코스 입력 wrapper 이벤트 리스너 등록됨');
    courseInputWrapper.addEventListener("click", handleCourseClick);
    courseInputWrapper.style.cursor = 'pointer';
  }

  // Toast 메시지 표시 함수
  function showToast(message, type = 'error') {
    // 기존 toast 제거
    const existingToast = document.querySelector('.toast');
    if (existingToast) {
      existingToast.remove();
    }

    // Toast 컨테이너 생성 (없으면)
    let container = document.querySelector('.toast-container');
    if (!container) {
      container = document.createElement('div');
      container.className = 'toast-container';
      document.body.appendChild(container);
    }

    // Toast 메시지 생성
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);

    // 애니메이션을 위해 약간의 지연 후 show 클래스 추가
    setTimeout(() => {
      toast.classList.add('show');
    }, 10);

    // 3초 후 제거
    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => {
        toast.remove();
      }, 300);
    }, 3000);
  }

  // 초기 선택 버튼 이벤트
  if (courseSelectionButton) {
    courseSelectionButton.addEventListener("click", () => {
      // 코스 선택 페이지로 이동
      const returnTo = window.location.pathname + window.location.search;
      window.location.href = `/course?selectMode=solo&returnTo=${encodeURIComponent(
          returnTo)}`;
    });
  }

  if (manualInputButton) {
    manualInputButton.addEventListener("click", () => {
      // 직접 입력 모드로 전환
      isManualInputMode = true;
      initialSelectionSection.style.display = "none";
      distanceSection.style.display = "flex";
      infoBox.style.display = "flex";
      if (infoBoxTitle) {
        infoBoxTitle.textContent = "거리를 입력해주세요";
      }
      if (infoBoxText) {
        infoBoxText.textContent = "원하는 거리를 직접 입력하고 솔로런을 시작하세요";
      }
      if (manualDistanceInput) {
        manualDistanceInput.focus();
      }
      updateStartButton();
    });
  }

  // 코스 입력 필드 클릭 이벤트
  if (courseInput) {
    courseInput.addEventListener("click", () => {
      const returnTo = window.location.pathname + window.location.search;
      window.location.href = `/course?selectMode=solo&returnTo=${encodeURIComponent(
          returnTo)}`;
    });
  }

  // URL 파라미터 읽기 (코스 상세보기에서 확인 버튼 클릭 시 돌아온 경우)
  const urlParams = new URLSearchParams(window.location.search);
  const urlCourseId = urlParams.get("courseId");
  const urlCourseName = urlParams.get("courseName");
  const urlCourseDistanceKm = urlParams.get("courseDistanceKm");

  // courseId가 있으면 코스 모드 활성화
  if (urlCourseId) {
    courseId = urlCourseId;
    isCourseMode = true;

    // 초기 선택 화면 숨기기
    if (initialSelectionSection) {
      initialSelectionSection.style.display = "none";
    }

    // 코스 섹션 표시
    if (courseSection) {
      courseSection.style.display = "block";
    }

    if (infoBox) {
      infoBox.style.display = "flex";
    }

    let convertedDistance = null;

    // distance 값이 있으면 입력창에 채우기
    if (urlCourseDistanceKm) {
      let distanceValue = parseFloat(urlCourseDistanceKm);
      if (!isNaN(distanceValue) && distanceValue > 0) {
        convertedDistance = distanceValue;
        manualDistanceValue = distanceValue;
        if (manualDistanceInput) {
          manualDistanceInput.value = distanceValue;
          manualDistanceInput.readOnly = true;
        }
      }
    }

    // 코스 입력 필드에 코스 이름 표시
    if (courseInput && urlCourseName) {
      courseInput.value = urlCourseName;
    }

    // 코스 모드 UI 업데이트
    if (infoBox) {
      infoBox.classList.add("course-mode");
    }

    if (infoBoxTitle && infoBoxText) {
      const courseTitle =
          urlCourseName ||
          (convertedDistance ? `${convertedDistance}km` : "선택하신");
      infoBoxTitle.innerHTML = `선택하신 <span class="course-title-highlight">${courseTitle}</span> 코스로 목표 거리가 설정되었습니다`;
      infoBoxText.textContent = "코스 정보에 따라 솔로런을 시작합니다";
    }

    if (manualInputLabel) {
      manualInputLabel.textContent = "코스 지정 거리";
    }

    // 코스 정보 로드 및 표시
    loadAndDisplayCourse(parseInt(urlCourseId));
    
    // 시작 버튼 활성화
    updateStartButton();
  } else {
    // 코스 모드가 아닐 때는 초기 선택 화면 표시
    if (initialSelectionSection) {
      initialSelectionSection.style.display = "flex";
    }
    if (courseSection) {
      courseSection.style.display = "none";
    }
    if (distanceSection) {
      distanceSection.style.display = "none";
    }
    // infoBox는 항상 표시
    if (infoBox) {
      infoBox.style.display = "flex";
    }
  }

  // 거리 옵션 정의 (백엔드 DistanceType Enum과 일치)
  const distanceOptions = [
    {value: "KM_3", label: "3km", numericValue: 3},
    {value: "KM_5", label: "5km", numericValue: 5},
    {value: "KM_10", label: "10km", numericValue: 10},
  ];

  // 뒤로가기 버튼
  if (backButton) {
    backButton.addEventListener("click", () => {
      window.history.length > 1
          ? window.history.back()
          : (window.location.href = "/match/select");
    });
  }

  // 시작 버튼 상태 업데이트
  function updateStartButton() {
    if (!startButton) {
      return;
    }

    // 코스 모드일 때는 항상 활성화 (courseId가 있으면)
    if (isCourseMode && courseId) {
      startButton.disabled = false;
      return;
    }

    // 직접 입력값만 확인
    const hasValidDistance = manualDistanceValue && manualDistanceValue > 0;
    if (hasValidDistance) {
      startButton.disabled = false;
    } else {
      startButton.disabled = true;
    }
  }

  // 직접 입력창 validation 함수
  const manualDistanceError = document.getElementById("manualDistanceError");
  const manualInputSection = document.querySelector(".manual-input-section");

  function validateManualDistance(value) {
    const numValue = parseFloat(value);
    const isEmpty = value === "" || value === null || value === undefined;

    if (isEmpty) {
      // 빈 값일 때는 에러 제거
      if (manualInputSection) {
        manualInputSection.classList.remove("has-error");
      }
      if (manualDistanceError) {
        manualDistanceError.textContent = "";
      }
      return true; // 빈 값은 유효 (선택사항이므로)
    }

    if (isNaN(numValue) || numValue <= 0) {
      // 0 이하 또는 숫자가 아님
      if (manualInputSection) {
        manualInputSection.classList.add("has-error");
      }
      if (manualDistanceError) {
        manualDistanceError.textContent =
            "목표 거리는 0보다 큰 값을 입력해주세요.";
      }
      return false;
    }

    if (numValue < 0.1) {
      // 0.1km(100m) 미만
      if (manualInputSection) {
        manualInputSection.classList.add("has-error");
      }
      if (manualDistanceError) {
        manualDistanceError.textContent =
            "목표 거리는 최소 0.1km(100m) 이상이어야 합니다.";
      }
      return false;
    }

    // 유효한 값
    if (manualInputSection) {
      manualInputSection.classList.remove("has-error");
    }
    if (manualDistanceError) {
      manualDistanceError.textContent = "";
    }
    return true;
  }

  // 직접 입력창 이벤트 핸들러 (코스 모드가 아닐 때만)
  if (manualDistanceInput && !isCourseMode) {
    manualDistanceInput.addEventListener("input", (e) => {
      const value = e.target.value;
      const numValue = parseFloat(value);

      // 실시간 validation
      const isValid = validateManualDistance(value);

      // 직접 입력 시 버튼 선택 해제
      if (!isNaN(numValue) && numValue > 0) {
        manualDistanceValue = isValid ? numValue : null;
      } else if (value === "" || value === null) {
        manualDistanceValue = null;
      } else {
        manualDistanceValue = null;
      }
      updateStartButton();
    });

    // 입력창 포커스 아웃 시 유효성 검사
    manualDistanceInput.addEventListener("blur", (e) => {
      const value = e.target.value;
      const numValue = parseFloat(value);

      if (value !== "" && (isNaN(numValue) || numValue <= 0)) {
        e.target.value = "";
        manualDistanceValue = null;
        validateManualDistance("");
        updateStartButton();
      } else {
        validateManualDistance(value);
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
    if (parts.length === 2) {
      return parts.pop().split(";").shift();
    }
    return null;
  }

  // 코스 정보 로드 및 표시
  async function loadAndDisplayCourse(courseId) {
    if (!courseId) {
      return;
    }

    try {
      const token = getToken();
      const headers = {
        "Content-Type": "application/json",
      };
      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }

      // 코스 상세 정보 조회
      const courseResponse = await fetch(`/api/courses/${courseId}`, {
        method: "GET",
        headers: headers,
      });

      if (!courseResponse.ok) {
        console.warn("코스 정보를 불러올 수 없습니다.");
        return;
      }

      const courseResult = await courseResponse.json();
      if (!courseResult.success || !courseResult.data) {
        console.warn("코스 데이터가 없습니다.");
        return;
      }

      selectedCourseData = courseResult.data;

      // 코스 이름 및 주소 표시
      if (courseName && selectedCourseData.title) {
        courseName.textContent = selectedCourseData.title;
      }
      if (courseAddress && selectedCourseData.address) {
        courseAddress.textContent = selectedCourseData.address;
      }
      if (selectedCourseInfo) {
        selectedCourseInfo.style.display = "block";
      }

      // 코스 상세 정보 카드 표시
      const courseDetailSection = document.getElementById("courseDetailSection");
      const courseDetailName = document.getElementById("courseDetailName");
      const courseDetailBadge = document.getElementById("courseDetailBadge");
      const courseDetailDistance = document.getElementById("courseDetailDistance");
      const courseDetailAddress = document.getElementById("courseDetailAddress");

      if (courseDetailSection) {
        courseDetailSection.style.display = "block";
      }

      if (courseDetailName && selectedCourseData.title) {
        courseDetailName.textContent = selectedCourseData.title;
      }

      if (courseDetailBadge && selectedCourseData.registerType) {
        const registerTypeText = {
          'MANUAL': '수동 등록',
          'AUTO': '자동 등록',
          'AI': 'AI 등록'
        }[selectedCourseData.registerType] || selectedCourseData.registerType;
        courseDetailBadge.textContent = registerTypeText;
      }

      if (courseDetailDistance && selectedCourseData.distanceM) {
        const distanceKm = (selectedCourseData.distanceM / 1000).toFixed(2);
        courseDetailDistance.textContent = `${distanceKm} km`;
      }

      if (courseDetailAddress && selectedCourseData.address) {
        courseDetailAddress.textContent = selectedCourseData.address;
      }

      // 카카오맵 초기화 및 경로 표시
      if (mapContainer && window.kakao && window.kakao.maps) {
        initMapAndDisplayPath(courseId);
      } else {
        // 카카오맵 SDK 로드 대기
        if (window.kakao && window.kakao.maps) {
          initMapAndDisplayPath(courseId);
        } else {
          console.warn("카카오맵 SDK가 로드되지 않았습니다.");
        }
      }
    } catch (error) {
      console.error("코스 정보 로드 중 오류:", error);
    }
  }

  // 카카오맵 초기화 및 경로 표시
  async function initMapAndDisplayPath(courseId) {
    if (!mapContainer) {
      return;
    }

    try {
      // 카카오맵 초기화
      if (!map) {
        const defaultPosition = new kakao.maps.LatLng(37.5665, 126.978);
        const mapOption = {
          center: defaultPosition,
          level: 4, // ✅ 모집글과 동일하게 4로 변경
        };
        map = new kakao.maps.Map(mapContainer, mapOption);
      }

      // 맵 섹션 표시
      if (mapSection) {
        mapSection.style.display = "block";
      }

      // 코스 경로 조회
      const token = getToken();
      const headers = {
        "Content-Type": "application/json",
      };
      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }

      const response = await fetch(`/api/courses/${courseId}/path`, {
        method: "GET",
        headers: headers,
      });

      if (!response.ok) {
        console.warn("코스 경로를 불러올 수 없습니다.");
        return;
      }

      const result = await response.json();
      if (!result.success || !result.data) {
        console.warn("코스 경로 데이터가 없습니다.");
        return;
      }

      const pathData = result.data;
      let pathCoords = [];

      // GeoJSON 형식 파싱
      if (pathData.path) {
        if (pathData.path.coordinates) {
          pathCoords = pathData.path.coordinates;
        } else if (typeof pathData.path === "string") {
          try {
            const parsed = JSON.parse(pathData.path);
            if (parsed.coordinates) {
              pathCoords = parsed.coordinates;
            } else if (Array.isArray(parsed)) {
              pathCoords = parsed;
            }
          } catch (e) {
            console.warn("경로 파싱 실패:", e);
            return;
          }
        } else if (Array.isArray(pathData.path)) {
          pathCoords = pathData.path;
        }
      }

      if (pathCoords.length < 2) {
        console.warn("경로 좌표가 충분하지 않습니다.");
        return;
      }

      // 기존 폴리라인 및 마커 제거
      if (coursePolyline) {
        coursePolyline.setMap(null);
        coursePolyline = null;
      }
      if (startMarker) {
        startMarker.setMap(null);
        startMarker = null;
      }
      if (endMarker) {
        endMarker.setMap(null);
        endMarker = null;
      }

      // 카카오맵 좌표로 변환 (GeoJSON은 [lng, lat] 순서)
      const latLngs = pathCoords.map(
          ([lng, lat]) => new kakao.maps.LatLng(lat, lng)
      );

      // 폴리라인 생성
      coursePolyline = new kakao.maps.Polyline({
        path: latLngs,
        strokeWeight: 5,
        strokeColor: "#ff3d00",
        strokeOpacity: 0.8,
        strokeStyle: "solid",
      });
      coursePolyline.setMap(map);

      // 출발지 마커 표시 (항상 첫 번째 좌표)
      if (latLngs.length > 0) {
        const startPosition = latLngs[0];

        // 출발지 인포윈도우
        const startInfoWindow = new kakao.maps.InfoWindow({
          content:
              '<div style="padding:8px 12px;font-size:13px;font-weight:600;color:#1a1c1e;white-space:nowrap;">📍 출발점</div>',
          removable: false,
        });

        startMarker = new kakao.maps.Marker({
          position: startPosition,
          map: map,
        });

        // 출발지 마커 클릭 시 인포윈도우 표시
        kakao.maps.event.addListener(startMarker, "click", function () {
          startInfoWindow.open(map, startMarker);
        });
      }

      // 도착지 마커 표시 (출발지와 다른 경우만)
      if (latLngs.length > 1) {
        const lastPosition = latLngs[latLngs.length - 1];
        const firstPosition = latLngs[0];

        // 출발지와 도착지가 다른 경우에만 도착지 마커 표시
        const latDiff = Math.abs(
            lastPosition.getLat() - firstPosition.getLat()
        );
        const lngDiff = Math.abs(
            lastPosition.getLng() - firstPosition.getLng()
        );
        if (latDiff > 0.0001 || lngDiff > 0.0001) {
          // 도착지 인포윈도우
          const endInfoWindow = new kakao.maps.InfoWindow({
            content:
                '<div style="padding:8px 12px;font-size:13px;font-weight:600;color:#1a1c1e;white-space:nowrap;">🏁 도착점</div>',
            removable: false,
          });

          endMarker = new kakao.maps.Marker({
            position: lastPosition,
            map: map,
          });

          // 도착지 마커 클릭 시 인포윈도우 표시
          kakao.maps.event.addListener(endMarker, "click", function () {
            endInfoWindow.open(map, endMarker);
          });
        }
      }

      // ✅ 출발지로 줌인 (전체 경로가 아닌 출발지 중심으로)
      if (latLngs.length > 0) {
        const startPosition = latLngs[0];
        
        // 출발지 중심으로 지도 설정
        map.setCenter(startPosition);
        map.setLevel(3); // 줌인 레벨 (1-14, 숫자가 작을수록 더 줌인)

        // ✅ 맵이 display:none에서 보이게 된 직후에는 relayout 필요
        map.relayout();

        // ✅ 추가 relayout (DOM 렌더링 완료 후)
        setTimeout(() => {
          if (map) {
            map.relayout();
            map.setCenter(startPosition);
            map.setLevel(3);
          }
        }, 100);

        setTimeout(() => {
          if (map) {
            map.relayout();
            map.setCenter(startPosition);
            map.setLevel(3);
          }
        }, 300);
      }
    } catch (error) {
      console.error("코스 경로 로드 중 오류:", error);
    }
  }

  // 솔로런 시작 API 호출
  async function startSoloRun() {
    // ✅ 코스 또는 직접 입력 중 하나는 필수 검증
    const hasCourseId = courseId != null && courseId !== "";
    const hasManualDistance =
        manualDistanceValue != null &&
        manualDistanceValue > 0 &&
        manualDistanceValue >= 0.1;

    if (!hasCourseId && !hasManualDistance) {
      showToast("코스 선택 또는 직접 입력 중 하나는 필수입니다.", "error");
      return;
    }

    // 코스 모드가 아닐 때 거리 검증
    if (!isCourseMode) {
      if (!hasManualDistance) {
        showToast("목표 거리를 입력해주세요", "error");
        return;
      }

      // ✅ 100m 이하 validation
      if (manualDistanceValue != null && manualDistanceValue > 0) {
        const targetDistanceM = manualDistanceValue * 1000;
        if (targetDistanceM < 100) {
          showToast("목표 거리는 최소 100m 이상이어야 합니다.", "error");
          return;
        }
      }
    }

    // 코스 모드일 때 courseId 검증
    if (isCourseMode && !hasCourseId) {
      showToast("코스를 선택해주세요.", "error");
      return;
    }

    const token = getToken();
    const headers = {
      "Content-Type": "application/json",
    };

    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    // 요청 데이터: null이 아닌 값만 전송
    const requestData = {};
    if (hasManualDistance) {
      requestData.manualDistance = manualDistanceValue;
    }
    if (hasCourseId) {
      requestData.courseId = parseInt(courseId);
    }

    try {
      startButton.disabled = true;
      startButton.textContent = "시작 중...";

      const response = await fetch("/api/match/solorun/start", {
        method: "POST",
        headers: headers,
        body: JSON.stringify(requestData),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        // ✅ 벨리데이션 에러 메시지 처리
        let errorMessage =
            errorData.message || `HTTP error! status: ${response.status}`;

        // 벨리데이션 에러인 경우 상세 메시지 추출
        if (errorData.errors && Array.isArray(errorData.errors)) {
          const validationMessages = errorData.errors
          .map((err) => err.defaultMessage || err.message)
          .filter((msg) => msg)
          .join("\n");
          if (validationMessages) {
            errorMessage = validationMessages;
          }
        }

        showToast(errorMessage, "error");
        startButton.disabled = false;
        startButton.textContent = "솔로런 시작하기";
        return;
      }

      const result = await response.json();
      if (result.success && result.data) {
        const sessionId = result.data;
        // 세션 ID를 사용하여 러닝 페이지로 이동
        window.location.href = `/running/${sessionId}`;
      } else {
        showToast(result.message || "솔로런 시작에 실패했습니다.", "error");
        startButton.disabled = false;
        startButton.textContent = "솔로런 시작하기";
      }
    } catch (error) {
      console.error("솔로런 시작 실패:", error);
      showToast(error.message || "솔로런 시작에 실패했습니다.", "error");
      startButton.disabled = false;
      startButton.textContent = "솔로런 시작하기";
    }
  }

  // 시작 버튼 클릭 이벤트
  if (startButton) {
    startButton.addEventListener("click", startSoloRun);
  }

  // 초기화
  updateStartButton();
});
