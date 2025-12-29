document.addEventListener("DOMContentLoaded", () => {
  let map = null;
  let marker = null;
  let geocoder = null;
  let selectedLat = null;
  let selectedLng = null;
  let selectedCourseId = null;
  let selectedAddress = null;
  let recruitId = null;

  const selectedTags = {
    distance: null,
    pace: null,
    ages: [],
    gender: null,
  };

  // URL에서 recruitId 추출
  function extractRecruitIdFromUrl() {
    const path = window.location.pathname;
    const match = path.match(/\/recruit\/(\d+)\/update/);
    if (match && match[1]) {
      return parseInt(match[1], 10);
    }
    return null;
  }

  // 나이 범위를 ages 배열로 변환
  function convertAgeRangeToAges(ageMin, ageMax) {
    const ages = [];
    if (ageMin === null || ageMax === null) {
      return ages;
    }

    // 0세~19세 -> 0
    if (ageMin === 0 && ageMax >= 19) {
      ages.push(0);
    }

    // 20대부터 60대까지
    for (let age = 20; age <= 60; age += 10) {
      if (ageMin <= age && ageMax >= age + 9) {
        ages.push(age);
      }
    }

    // 70세 이상 -> 70
    if (ageMax >= 70) {
      ages.push(70);
    }

    return ages;
  }

  // 데이터 초기화 함수
  async function initEditForm(data) {
    // 텍스트 필드
    document.getElementById("title").value = data.title || "";
    document.getElementById("content").value = data.content || "";
    document.getElementById("maxParticipants").value = data.maxParticipants || 2;

    // 글자수 카운터 업데이트
    const titleInput = document.getElementById("title");
    const titleCounter = document.getElementById("titleCounter");
    const contentInput = document.getElementById("content");
    const contentCounter = document.getElementById("contentCounter");
    if (titleInput && titleCounter) {
      updateCharCounter(titleInput, titleCounter, 100);
    }
    if (contentInput && contentCounter) {
      updateCharCounter(contentInput, contentCounter, 500);
    }

    // 출발지
    if (data.latitude && data.longitude && data.meetingPlace) {
      selectedLat = data.latitude;
      selectedLng = data.longitude;
      selectedAddress = data.meetingPlace;
      updateLocationSummary();
    }

    // 날짜/시간
    if (data.meetingAt) {
      const meetingDate = new Date(data.meetingAt);
      const year = meetingDate.getFullYear();
      const month = String(meetingDate.getMonth() + 1).padStart(2, "0");
      const day = String(meetingDate.getDate()).padStart(2, "0");
      const hours = String(meetingDate.getHours()).padStart(2, "0");
      const minutes = String(meetingDate.getMinutes()).padStart(2, "0");

      document.getElementById("meetingDate").value = `${year}-${month}-${day}`;
      document.getElementById("meetingTime").value = `${hours}:${minutes}`;
    }

    // 코스
    if (data.courseId) {
      selectedCourseId = data.courseId;
      // 코스 이름은 API에서 가져와야 할 수도 있음
      document.getElementById("course").value = data.courseImageUrl ? "코스 선택됨" : "";
    }

    // 러닝 조건
    if (data.targetDistance) {
      selectedTags.distance = data.targetDistance;
    }
    if (data.targetPace) {
      selectedTags.pace = data.targetPace;
    }
    if (data.ageMin !== null && data.ageMax !== null) {
      selectedTags.ages = convertAgeRangeToAges(data.ageMin, data.ageMax);
    }
    if (data.genderLimit) {
      selectedTags.gender = data.genderLimit;
    }

    // 칩 선택 상태 업데이트
    updateChipSelection();
    updateConditionSummary();

    // 시간 옵션 생성 (날짜가 설정된 후)
    generateTimeOptions();
  }

  // 페이지 로드 시 데이터 가져오기
  recruitId = extractRecruitIdFromUrl();
  if (!recruitId) {
    showToast("모집글 ID를 찾을 수 없습니다.", "error");
    setTimeout(() => {
      window.location.href = "/recruit";
    }, 2000);
    return;
  }

  // 모집글 데이터 로드
  async function loadRecruitData() {
    try {
      const token = localStorage.getItem("accessToken") || getCookie("accessToken");
      const headers = {
        "Content-Type": "application/json",
      };

      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }

      const response = await fetch(`/api/recruit/${recruitId}`, {
        method: "GET",
        headers: headers,
      });

      const result = await response.json();

      if (response.ok && result.success) {
        await initEditForm(result.data);
      } else {
        showToast(result.message || "모집글을 불러올 수 없습니다.", "error");
        setTimeout(() => {
          window.location.href = "/recruit";
        }, 2000);
      }
    } catch (error) {
      console.error("모집글 로드 중 오류 발생:", error);
      showToast("모집글을 불러오는 중 오류가 발생했습니다.", "error");
      setTimeout(() => {
        window.location.href = "/recruit";
      }, 2000);
    }
  }

  // 나머지 코드는 recruit-create.js와 동일 (generateTimeOptions, setupDateChangeListener 등)
  function generateTimeOptions() {
    const timeOptionContainer = document.getElementById("timeOptionContainer");
    const timeInput = document.getElementById("meetingTime");

    if (!timeOptionContainer || !timeInput) {
      return;
    }

    // 기존 칩 모두 제거
    timeOptionContainer.innerHTML = "";

    // 현재 선택된 시간 값 가져오기
    const currentSelectedTime = timeInput.value;

    // 선택된 날짜 확인
    const selectedDate = document.getElementById("meetingDate").value;

    // 로컬 시간으로 오늘 날짜 문자열 생성 (toISOString 버그 방지)
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    const today = `${year}-${month}-${day}`;

    // 오늘 날짜인 경우, 현재 시간 + 1시간 이후만 표시
    let minHour = 0;
    let minMinute = 0;

    if (selectedDate === today) {
      // 현재 시간 + 1시간 계산
      const currentHour = now.getHours();
      const currentMinute = now.getMinutes();

      // 1시간 후 계산
      let oneHourLaterHour = currentHour;
      let oneHourLaterMinute = currentMinute;

      // 1시간 추가
      oneHourLaterMinute += 60;
      if (oneHourLaterMinute >= 60) {
        oneHourLaterHour += Math.floor(oneHourLaterMinute / 60);
        oneHourLaterMinute = oneHourLaterMinute % 60;
      }
      if (oneHourLaterHour >= 24) {
        oneHourLaterHour = 23; // 최대 23시까지만
        oneHourLaterMinute = 59;
      }

      minHour = oneHourLaterHour;
      minMinute = oneHourLaterMinute;

      // 30분 단위로 올림 처리 (예: 14:15 -> 14:30, 14:45 -> 15:00)
      if (minMinute > 0 && minMinute <= 30) {
        minMinute = 30;
      } else if (minMinute > 30) {
        minHour += 1;
        minMinute = 0;
      }

      // 24시를 넘으면 다음 날로 넘어가므로 시간 옵션 생성 중단
      if (minHour >= 24) {
        return;
      }
    }

    for (let hour = 0; hour < 24; hour++) {
      for (let minute = 0; minute < 60; minute += 30) {
        // 오늘 날짜인 경우, 현재 시간 + 1시간 이후만 활성화
        if (selectedDate === today) {
          if (hour < minHour || (hour === minHour && minute < minMinute)) {
            continue; // 이 시간은 건너뛰기
          }
        }

        const timeString = `${String(hour).padStart(2, "0")}:${String(
            minute).padStart(2, "0")}`;
        const timeChip = document.createElement("button");
        timeChip.type = "button";
        timeChip.className = "time-chip";
        timeChip.textContent = timeString;
        timeChip.dataset.time = timeString;

        // 이미 선택된 시간이면 selected 클래스 추가
        if (currentSelectedTime === timeString) {
          timeChip.classList.add("selected");
        }

        // 클릭 이벤트: 시간 선택
        timeChip.addEventListener("click", () => {
          // 모든 칩에서 selected 클래스 제거
          document.querySelectorAll(".time-chip").forEach(chip => {
            chip.classList.remove("selected");
          });

          // 선택된 칩에 selected 클래스 추가
          timeChip.classList.add("selected");

          // 메인 화면의 시간 입력창에 값 표시
          timeInput.value = timeString;

          // 0.2초 후 모달 닫기
          setTimeout(() => {
            closeTimeModal();
          }, 200);
        });

        timeOptionContainer.appendChild(timeChip);
      }
    }
  }

  function setupDateChangeListener() {
    const dateInput = document.getElementById("meetingDate");
    const timeInput = document.getElementById("meetingTime");

    if (!dateInput || !timeInput) {
      console.error("날짜/시간 입력 필드를 찾을 수 없습니다.");
      return;
    }

    dateInput.addEventListener("change", () => {
      // 시간 선택 초기화
      timeInput.value = "";
      generateTimeOptions();
    });
  }

  // [추가] 시간 모달 열기/닫기 로직
  const timeModal = document.getElementById("timeBottomSheet");
  const timeInput = document.getElementById("meetingTime");
  const closeTimeModalBtn = document.getElementById("closeTimeModal");

  // 모달 닫기 함수 (generateTimeOptions에서 호출됨)
  function closeTimeModal() {
    if (timeModal) {
      timeModal.classList.remove("show");
      document.body.style.overflow = "";
    }
  }

  // 모달 요소가 존재할 때만 이벤트 연결
  if (timeModal && timeInput) {
    // 1. 입력창 클릭 시 모달 열기
    timeInput.addEventListener("click", () => {
      timeModal.classList.add("show");
      document.body.style.overflow = "hidden";
      generateTimeOptions();
    });

    // 2. 닫기 버튼 클릭 시 닫기
    if (closeTimeModalBtn) {
      closeTimeModalBtn.addEventListener("click", closeTimeModal);
    }

    // 3. 오버레이 클릭 시 닫기
    const timeOverlay = timeModal.querySelector(".bottom-sheet-overlay");
    if (timeOverlay) {
      timeOverlay.addEventListener("click", closeTimeModal);
    }
  }

  function initMap() {
    const mapContainer = document.getElementById("map");
    if (!mapContainer) {
      return;
    }

    // 이미 선택된 위치가 있으면 그 위치로, 없으면 기본 위치
    const centerLat = selectedLat || 37.5665;
    const centerLng = selectedLng || 126.9780;

    const mapOption = {
      center: new kakao.maps.LatLng(centerLat, centerLng),
      level: 3,
    };

    map = new kakao.maps.Map(mapContainer, mapOption);
    geocoder = new kakao.maps.services.Geocoder();

    // 이미 선택된 위치가 있으면 마커 표시
    if (selectedLat && selectedLng) {
      const latlng = new kakao.maps.LatLng(selectedLat, selectedLng);
      marker = new kakao.maps.Marker({
        position: latlng,
        map: map,
      });
      map.setCenter(latlng);
    }

    kakao.maps.event.addListener(map, "click", (mouseEvent) => {
      const latlng = mouseEvent.latLng;
      selectedLat = latlng.getLat();
      selectedLng = latlng.getLng();

      if (marker) {
        marker.setMap(null);
      }

      marker = new kakao.maps.Marker({
        position: latlng,
        map: map,
      });

      geocoder.coord2Address(selectedLng, selectedLat, (result, status) => {
        if (status === kakao.maps.services.Status.OK) {
          const roadAddr = result[0].road_address;
          const jibunAddr = result[0].address;

          let detailAddress = "";

          if (roadAddr) {
            detailAddress = roadAddr.address_name;
            if (roadAddr.building_name) {
              detailAddress += ` (${roadAddr.building_name})`;
            }
          } else if (jibunAddr) {
            detailAddress = jibunAddr.address_name;
          }

          selectedAddress = detailAddress;
        } else {
          console.error("주소 변환 실패:", status);
          selectedAddress = "주소를 찾을 수 없습니다";
        }
      });
    });

    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
          (position) => {
            const userLat = position.coords.latitude;
            const userLng = position.coords.longitude;
            const moveLatLon = new kakao.maps.LatLng(userLat, userLng);
            // 선택된 위치가 없을 때만 현재 위치로 이동
            if (!selectedLat || !selectedLng) {
              map.setCenter(moveLatLon);
            }
          },
          (error) => {
            console.error("위치 정보를 가져올 수 없습니다:", error);
          }
      );
    }
  }

  // 지도 모달 열기/닫기
  const locationModal = document.getElementById("locationBottomSheet");
  const locationSummary = document.getElementById("locationSummary");
  const closeLocationModalBtn = document.getElementById("closeLocationModal");
  const confirmLocationModalBtn = document.getElementById(
      "confirmLocationModal");

  // 출발지 요약 뷰 업데이트 함수 (외부에서도 호출 가능하도록)
  function updateLocationSummary() {
    const placeholder = document.getElementById("locationPlaceholder");
    const locationText = document.getElementById("locationText");

    if (!placeholder || !locationText) {
      return;
    }

    if (selectedAddress) {
      placeholder.style.display = "none";
      locationText.style.display = "block";
      locationText.textContent = selectedAddress;
    } else {
      placeholder.style.display = "block";
      locationText.style.display = "none";
    }
  }

  if (locationModal && locationSummary && closeLocationModalBtn
      && confirmLocationModalBtn) {
    function openLocationModal() {
      locationModal.classList.add("show");
      document.body.style.overflow = "hidden";

      // 지도가 이미 초기화되어 있으면 relayout
      if (map) {
        setTimeout(() => {
          map.relayout();
          if (selectedLat && selectedLng) {
            const latlng = new kakao.maps.LatLng(selectedLat, selectedLng);
            map.setCenter(latlng);
          }
        }, 100);
      } else {
        // 지도가 없으면 초기화
        initMap();
      }
    }

    function closeLocationModal() {
      locationModal.classList.remove("show");
      document.body.style.overflow = "";
      updateLocationSummary();
    }

    locationSummary.addEventListener("click", openLocationModal);
    closeLocationModalBtn.addEventListener("click", closeLocationModal);
    confirmLocationModalBtn.addEventListener("click", () => {
      if (!selectedLat || !selectedLng) {
        showToast("지도에서 위치를 선택해주세요.", "error");
        return;
      }
      closeLocationModal();
    });

    // 오버레이 클릭 시 모달 닫기
    const locationOverlay = locationModal.querySelector(
        ".bottom-sheet-overlay");
    if (locationOverlay) {
      locationOverlay.addEventListener("click", closeLocationModal);
    }
  } else {
    console.error("지도 모달 요소를 찾을 수 없습니다.");
  }

  document.getElementById("backBtn").addEventListener("click", () => {
    window.location.href = "/recruit";
  });

  document.getElementById("course").addEventListener("click", () => {
    showToast("코스 조회 기능은 추후 구현 예정입니다.", "error");
  });

  // 모달 열기/닫기
  const conditionModal = document.getElementById("conditionBottomSheet");
  const conditionSummary = document.getElementById("conditionSummary");
  const closeConditionModalBtn = document.getElementById("closeConditionModal");
  const confirmConditionModalBtn = document.getElementById(
      "confirmConditionModal");

  if (conditionModal && conditionSummary && closeConditionModalBtn
      && confirmConditionModalBtn) {
    function openConditionModal() {
      conditionModal.classList.add("show");
      document.body.style.overflow = "hidden";
      // 모달 열 때 현재 선택 상태 반영
      updateChipSelection();
    }

    function closeConditionModal() {
      conditionModal.classList.remove("show");
      document.body.style.overflow = "";
      // 모달 닫을 때 요약 뷰 업데이트
      updateConditionSummary();
    }

    // 요약 박스 클릭 시 모달 열기
    conditionSummary.addEventListener("click", openConditionModal);
    closeConditionModalBtn.addEventListener("click", closeConditionModal);
    confirmConditionModalBtn.addEventListener("click", closeConditionModal);

    // 오버레이 클릭 시 모달 닫기
    const conditionOverlay = conditionModal.querySelector(
        ".bottom-sheet-overlay");
    if (conditionOverlay) {
      conditionOverlay.addEventListener("click", closeConditionModal);
    }
  } else {
    console.error("러닝조건 모달 요소를 찾을 수 없습니다.");
  }

  // 탭 전환
  const tabButtons = document.querySelectorAll(".tab-button");
  const tabPanels = document.querySelectorAll(".tab-panel");

  tabButtons.forEach((button) => {
    button.addEventListener("click", () => {
      const targetTab = button.getAttribute("data-tab");

      // 모든 탭 버튼과 패널 비활성화
      tabButtons.forEach((btn) => btn.classList.remove("active"));
      tabPanels.forEach((panel) => panel.classList.remove("active"));

      // 선택된 탭 활성화
      button.classList.add("active");
      document.getElementById(`tab-${targetTab}`).classList.add("active");
    });
  });

  // 칩 선택 이벤트 리스너 (모달 내부)
  function setupChipListeners() {
    document.querySelectorAll('.chip[data-distance]').forEach((chip) => {
      chip.addEventListener("click", () => {
        document.querySelectorAll('.chip[data-distance]').forEach((c) => {
          c.classList.remove("selected");
        });
        chip.classList.add("selected");
        selectedTags.distance = parseFloat(chip.getAttribute("data-distance"));
        // 실시간 요약 뷰 업데이트
        updateConditionSummary();
      });
    });

    document.querySelectorAll('.chip[data-pace]').forEach((chip) => {
      chip.addEventListener("click", () => {
        document.querySelectorAll('.chip[data-pace]').forEach((c) => {
          c.classList.remove("selected");
        });
        chip.classList.add("selected");
        selectedTags.pace = chip.getAttribute("data-pace");
        updateConditionSummary();
      });
    });

    document.querySelectorAll('.chip[data-age]').forEach((chip) => {
      chip.addEventListener("click", () => {
        const age = parseInt(chip.getAttribute("data-age"));
        const index = selectedTags.ages.indexOf(age);

        if (index > -1) {
          if (selectedTags.ages.length > 1) {
            const sorted = [...selectedTags.ages].sort((a, b) => a - b);
            if (age !== sorted[0] && age !== sorted[sorted.length - 1]) {
              showToast("연속된 나이대 중간은 해제할 수 없습니다. 양 끝부터 해제해주세요.", "error");
              return;
            }
          }

          chip.classList.remove("selected");
          selectedTags.ages.splice(index, 1);
        } else {
          if (selectedTags.ages.length > 0) {
            const sorted = [...selectedTags.ages].sort((a, b) => a - b);
            const min = sorted[0];
            const max = sorted[sorted.length - 1];

            const isConsecutive =
                age === min - 10 ||
                age === max + 10 ||
                (age === 0 && min === 20) ||
                (age === 20 && max === 0) ||
                (age === 70 && max === 60) ||
                (age === 60 && min === 70);

            if (!isConsecutive) {
              showToast("연속된 나이대만 선택할 수 있습니다.", "error");
              return;
            }
          }

          chip.classList.add("selected");
          selectedTags.ages.push(age);
        }

        selectedTags.ages.sort((a, b) => a - b);
        updateConditionSummary();
      });
    });

    document.querySelectorAll('.chip[data-gender]').forEach((chip) => {
      chip.addEventListener("click", () => {
        document.querySelectorAll('.chip[data-gender]').forEach((c) => {
          c.classList.remove("selected");
        });
        chip.classList.add("selected");
        selectedTags.gender = chip.getAttribute("data-gender");
        updateConditionSummary();
      });
    });
  }

  // 칩 선택 상태를 모달에 반영
  function updateChipSelection() {
    // 거리
    document.querySelectorAll('.chip[data-distance]').forEach((chip) => {
      const distance = parseFloat(chip.getAttribute("data-distance"));
      if (selectedTags.distance === distance) {
        chip.classList.add("selected");
      } else {
        chip.classList.remove("selected");
      }
    });

    // 페이스
    document.querySelectorAll('.chip[data-pace]').forEach((chip) => {
      const pace = chip.getAttribute("data-pace");
      if (selectedTags.pace === pace) {
        chip.classList.add("selected");
      } else {
        chip.classList.remove("selected");
      }
    });

    // 나이
    document.querySelectorAll('.chip[data-age]').forEach((chip) => {
      const age = parseInt(chip.getAttribute("data-age"));
      if (selectedTags.ages.includes(age)) {
        chip.classList.add("selected");
      } else {
        chip.classList.remove("selected");
      }
    });

    // 성별
    document.querySelectorAll('.chip[data-gender]').forEach((chip) => {
      const gender = chip.getAttribute("data-gender");
      if (selectedTags.gender === gender) {
        chip.classList.add("selected");
      } else {
        chip.classList.remove("selected");
      }
    });
  }

  // 요약 뷰 업데이트
  function updateConditionSummary() {
    const summaryChips = document.getElementById("conditionSummaryChips");
    const placeholder = document.querySelector(
        "#conditionSummary .condition-summary-placeholder");
    if (!summaryChips || !placeholder) {
      return;
    }
    summaryChips.innerHTML = "";

    const hasSelection = selectedTags.distance || selectedTags.pace ||
        selectedTags.ages.length > 0 || selectedTags.gender;

    if (hasSelection) {
      placeholder.style.display = "none";
      summaryChips.style.display = "flex";

      if (selectedTags.distance) {
        const chip = document.createElement("div");
        chip.className = "condition-summary-chip";
        chip.textContent = `${selectedTags.distance}km`;
        summaryChips.appendChild(chip);
      }

      if (selectedTags.pace) {
        const chip = document.createElement("div");
        chip.className = "condition-summary-chip";
        const paceText = selectedTags.pace === "2:00" ? "2:00/km 이하" :
            selectedTags.pace === "9:00" ? "9:00/km 이상" :
                `${selectedTags.pace}/km`;
        chip.textContent = paceText;
        summaryChips.appendChild(chip);
      }

      if (selectedTags.ages.length > 0) {
        const sorted = [...selectedTags.ages].sort((a, b) => a - b);
        const ageTexts = sorted.map(age => {
          if (age === 0) {
            return "10대 이하";
          }
          if (age === 70) {
            return "70대 이상";
          }
          return `${age}대`;
        });
        const chip = document.createElement("div");
        chip.className = "condition-summary-chip";
        chip.textContent = ageTexts.join(", ");
        summaryChips.appendChild(chip);
      }

      if (selectedTags.gender) {
        const chip = document.createElement("div");
        chip.className = "condition-summary-chip";
        const genderText = selectedTags.gender === "M" ? "남성" :
            selectedTags.gender === "F" ? "여성" : "무관";
        chip.textContent = genderText;
        summaryChips.appendChild(chip);
      }
    } else {
      placeholder.style.display = "block";
      summaryChips.style.display = "none";
    }
  }

  // 초기화
  setupChipListeners();

  function calculateAgeRange() {
    if (selectedTags.ages.length === 0) {
      return {ageMin: null, ageMax: null};
    }

    const sorted = [...selectedTags.ages].sort((a, b) => a - b);
    let ageMin = sorted[0];
    let ageMax = sorted[sorted.length - 1] + 9;

    if (sorted[0] === 0) {
      ageMin = 0;
      if (sorted.length === 1) {
        ageMax = 19;
      } else {
        ageMax = sorted[sorted.length - 1] + 9;
      }
    }

    if (sorted[sorted.length - 1] === 70) {
      ageMax = 100;
    }

    return {ageMin, ageMax};
  }

  // 에러 클래스 제거 함수
  function clearErrors() {
    document.querySelectorAll(
        '.form-input.error, .form-textarea.error, .condition-summary.error, #locationSummary.error').forEach(
        el => {
          el.classList.remove('error');
        });
  }

  // 에러 표시 함수
  function showError(element, message) {
    clearErrors();

    if (element) {
      element.classList.add('error');

      // 입력 필드인 경우
      if (element.classList.contains('form-input')
          || element.classList.contains('form-textarea')) {
        element.focus();
        element.scrollIntoView({behavior: 'smooth', block: 'center'});
      }
      // 요약 박스인 경우 (출발지, 러닝조건)
      else if (element.id === 'locationSummary' ||
          element.id === 'conditionSummary') {
        element.scrollIntoView({behavior: 'smooth', block: 'center'});
        // 러닝조건 모달이 닫혀있으면 열기
        if (element.id === 'conditionSummary') {
          const conditionModal = document.getElementById(
              "conditionBottomSheet");
          if (conditionModal && !conditionModal.classList.contains('show')) {
            const conditionSummary = document.getElementById(
                "conditionSummary");
            if (conditionSummary) {
              conditionSummary.click();
            }
          }
        }
        // 출발지 모달이 닫혀있으면 열기
        else if (element.id === 'locationSummary') {
          const locationModal = document.getElementById("locationBottomSheet");
          if (locationModal && !locationModal.classList.contains('show')) {
            const locationSummary = document.getElementById("locationSummary");
            if (locationSummary) {
              locationSummary.click();
            }
          }
        }
      } else {
        // 일반 요소인 경우
        element.scrollIntoView({behavior: 'smooth', block: 'center'});
      }
    }

    showToast(message, 'error');
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

  document.getElementById("submitBtn").addEventListener("click", async () => {
    clearErrors();

    // 데이터 수집 (전역 변수 및 DOM 요소에서)
    const title = document.getElementById("title").value.trim();
    const content = document.getElementById("content").value.trim();
    const maxParticipantsInput = document.getElementById("maxParticipants");
    const maxParticipants = maxParticipantsInput ? parseInt(
        maxParticipantsInput.value) : null;
    const meetingDateInput = document.getElementById("meetingDate");
    const meetingTimeInput = document.getElementById("meetingTime");
    const meetingDate = meetingDateInput ? meetingDateInput.value : null;
    const meetingTime = meetingTimeInput ? meetingTimeInput.value : null;

    // 유효성 검사
    if (!title) {
      const titleInput = document.getElementById("title");
      showError(titleInput, "제목을 입력해주세요.");
      return;
    }

    if (!content) {
      const contentTextarea = document.getElementById("content");
      showError(contentTextarea, "설명을 입력해주세요.");
      return;
    }

    if (!selectedAddress || !selectedLat || !selectedLng) {
      const locationSummary = document.getElementById("locationSummary");
      showError(locationSummary, "지도에서 출발지를 선택해주세요.");
      return;
    }

    if (!maxParticipants || maxParticipants < 2 || maxParticipants > 20) {
      showError(maxParticipantsInput, "최대 인원은 2명 이상 20명 이하이어야 합니다.");
      return;
    }

    if (!meetingDate || !meetingTime) {
      const dateInput = document.getElementById("meetingDate");
      const timeInput = document.getElementById("meetingTime");
      if (!meetingDate && dateInput) {
        showError(dateInput, "모임 날짜를 선택해주세요.");
      } else if (!meetingTime && timeInput) {
        showError(timeInput, "모임 시간을 선택해주세요.");
      } else {
        showToast("모임 일시를 선택해주세요.", "error");
      }
      return;
    }

    if (!selectedTags.distance) {
      const conditionSummary = document.getElementById("conditionSummary");
      showError(conditionSummary, "뛸 거리를 선택해주세요.");
      return;
    }

    if (!selectedTags.pace) {
      const conditionSummary = document.getElementById("conditionSummary");
      showError(conditionSummary, "페이스를 선택해주세요.");
      return;
    }

    const ageRange = calculateAgeRange();
    if (ageRange.ageMin === null || ageRange.ageMin === undefined ||
        ageRange.ageMax === null || ageRange.ageMax === undefined) {
      const conditionSummary = document.getElementById("conditionSummary");
      showError(conditionSummary, "나이대를 선택해주세요.");
      return;
    }

    if (!selectedTags.gender) {
      const conditionSummary = document.getElementById("conditionSummary");
      showError(conditionSummary, "성별을 선택해주세요.");
      return;
    }

    // 날짜와 시간을 LocalDateTime 형식으로 변환
    // 형식: "YYYY-MM-DDTHH:mm:ss"
    const meetingAt = `${meetingDate}T${meetingTime}:00`;

    const requestData = {
      title: title,
      content: content,
      meetingPlace: selectedAddress,
      latitude: selectedLat,
      longitude: selectedLng,
      targetDistance: selectedTags.distance,
      targetPace: selectedTags.pace,
      maxParticipants: maxParticipants,
      ageMin: ageRange.ageMin,
      ageMax: ageRange.ageMax,
      genderLimit: selectedTags.gender, // M, F, BOTH
      meetingAt: meetingAt,
    };

    // courseId가 있으면 추가 (optional)
    if (selectedCourseId) {
      requestData.courseId = selectedCourseId;
    }

    try {
      const token = localStorage.getItem("accessToken") || getCookie(
          "accessToken");
      const headers = {
        "Content-Type": "application/json",
      };

      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }

      // PUT 요청으로 변경
      const response = await fetch(`/api/recruit/${recruitId}`, {
        method: "PUT",
        headers: headers,
        body: JSON.stringify(requestData),
      });

      const result = await response.json();

      if (response.ok && result.success) {
        showToast("모집글이 성공적으로 수정되었습니다.", "success");
        setTimeout(() => {
          window.location.href = `/recruit/${recruitId}`;
        }, 1500);
      } else {
        // 백엔드 에러 메시지를 사용자 친화적으로 변경
        let errorMessage = result.message || "모집글 수정에 실패했습니다.";
        // 에러 코드 또는 메시지로 확인
        if (result.code === "RO16" || errorMessage.includes("참여 가능 성별이 아닙니다")) {
          errorMessage = "본인의 성별과 다른 성별 제한은 설정할 수 없습니다.";
        } else if (result.code === "RO15" || errorMessage.includes("참여 가능 나이가 아닙니다")) {
          errorMessage = "본인의 나이가 포함된 연령대만 설정할 수 있습니다.";
        }
        showToast(errorMessage, "error");
        console.error("Error:", result);
      }
    } catch (error) {
      console.error("모집글 수정 중 오류 발생:", error);
      showToast("모집글 수정 중 오류가 발생했습니다.", "error");
    }
  });

  function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
      return parts.pop().split(";").shift();
    }
    return null;
  }

  // 글자수 카운터 업데이트 함수
  function updateCharCounter(inputElement, counterElement, maxLength) {
    const currentLength = inputElement.value.length;
    counterElement.textContent = `${currentLength}/${maxLength}`;

    // 글자수에 따라 스타일 변경
    counterElement.classList.remove('warning', 'danger');
    if (currentLength === maxLength) {
      counterElement.classList.add('danger');
    } else if (currentLength >= maxLength * 0.9) {
      counterElement.classList.add('warning');
    }
  }

  // 제목 글자수 카운터
  const titleInput = document.getElementById("title");
  const titleCounter = document.getElementById("titleCounter");
  if (titleInput && titleCounter) {
    titleInput.addEventListener('input', function () {
      updateCharCounter(this, titleCounter, 100);
      this.classList.remove('error');
    });
    titleInput.addEventListener('change', function () {
      this.classList.remove('error');
    });
  }

  // 내용 글자수 카운터
  const contentInput = document.getElementById("content");
  const contentCounter = document.getElementById("contentCounter");
  if (contentInput && contentCounter) {
    contentInput.addEventListener('input', function () {
      updateCharCounter(this, contentCounter, 500);
      this.classList.remove('error');
    });
    contentInput.addEventListener('change', function () {
      this.classList.remove('error');
    });
  }

  // 입력 필드에서 에러 클래스 자동 제거
  document.querySelectorAll('.form-input, .form-textarea').forEach(input => {
    if (input.id !== 'title' && input.id !== 'content') {
      input.addEventListener('input', function () {
        this.classList.remove('error');
      });
      input.addEventListener('change', function () {
        this.classList.remove('error');
      });
    }
  });

  // 칩 선택 시 에러 클래스 제거
  document.querySelectorAll('.chip').forEach(chip => {
    chip.addEventListener('click', function () {
      const conditionSummary = document.getElementById("conditionSummary");
      if (conditionSummary) {
        conditionSummary.classList.remove('error');
      }
    });
  });

  // 로컬 시간으로 오늘 날짜 문자열 생성 (toISOString 버그 방지)
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  const today = `${year}-${month}-${day}`;
  document.getElementById("meetingDate").setAttribute("min", today);

  // 날짜 변경 리스너 설정
  setupDateChangeListener();

  // 초기 요약 뷰 업데이트
  updateConditionSummary();
  updateLocationSummary();

  // 모집글 데이터 로드 (페이지 로드 시)
  loadRecruitData();
});
