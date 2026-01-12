document.addEventListener("DOMContentLoaded", () => {
  let map = null;
  let marker = null;
  let geocoder = null;
  let selectedLat = null;
  let selectedLng = null;
  let selectedCourseId = null;
  let selectedAddress = null;
  let isDistanceLocked = false;
  let isLocationLockedByCourse = false;

  const DRAFT_KEY = "recruitDraft:create";

  const selectedTags = {
    distance: null,
    pace: null,
    ages: [],
    gender: null,
  };

  function setDistanceLock(distanceKm) {
    if (distanceKm === null || distanceKm === undefined || Number.isNaN(
        distanceKm)) {
      return;
    }

    isDistanceLocked = true;
    selectedTags.distance = distanceKm;

    // 거리 칩(3/5/10) 잠금 + 선택 해제
    document.querySelectorAll('.chip[data-distance]').forEach((chip) => {
      chip.classList.remove("selected");
      chip.disabled = true;
      chip.classList.add("disabled");
    });
  }

  function saveDraft(extra = {}) {
    try {
      const title = document.getElementById("title")?.value ?? "";
      const content = document.getElementById("content")?.value ?? "";
      const maxParticipants = document.getElementById("maxParticipants")?.value
          ?? "";
      const meetingDate = document.getElementById("meetingDate")?.value ?? "";
      const meetingTime = document.getElementById("meetingTime")?.value ?? "";
      const courseText = document.getElementById("course")?.value ?? "";

      const draft = {
        savedAt: Date.now(),
        title,
        content,
        maxParticipants,
        meetingDate,
        meetingTime,
        selectedLat,
        selectedLng,
        selectedAddress,
        selectedCourseId,
        courseText,
        isDistanceLocked,
        selectedTags: {
          distance: selectedTags.distance,
          pace: selectedTags.pace,
          ages: Array.isArray(selectedTags.ages) ? [...selectedTags.ages] : [],
          gender: selectedTags.gender,
        },
        ...extra,
      };

      sessionStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
    } catch (e) {
      // ignore
    }
  }

  function restoreDraft() {
    try {
      const raw = sessionStorage.getItem(DRAFT_KEY);
      if (!raw) {
        return false;
      }
      const draft = JSON.parse(raw);
      if (!draft || typeof draft !== "object") {
        return false;
      }

      // 너무 오래된 초안은 무시 (1시간)
      if (draft.savedAt && Date.now() - draft.savedAt > 60 * 60 * 1000) {
        sessionStorage.removeItem(DRAFT_KEY);
        return false;
      }

      const titleEl = document.getElementById("title");
      const contentEl = document.getElementById("content");
      const maxEl = document.getElementById("maxParticipants");
      const dateEl = document.getElementById("meetingDate");
      const timeEl = document.getElementById("meetingTime");
      const courseEl = document.getElementById("course");

      if (titleEl && typeof draft.title
          === "string") {
        titleEl.value = draft.title;
      }
      if (contentEl && typeof draft.content
          === "string") {
        contentEl.value = draft.content;
      }
      if (maxEl && draft.maxParticipants != null) {
        maxEl.value = String(
            draft.maxParticipants);
      }
      if (dateEl && typeof draft.meetingDate
          === "string") {
        dateEl.value = draft.meetingDate;
      }
      if (timeEl && typeof draft.meetingTime
          === "string") {
        timeEl.value = draft.meetingTime;
      }

      if (typeof draft.selectedLat
          === "number") {
        selectedLat = draft.selectedLat;
      }
      if (typeof draft.selectedLng
          === "number") {
        selectedLng = draft.selectedLng;
      }
      if (typeof draft.selectedAddress
          === "string") {
        selectedAddress = draft.selectedAddress;
      }
      if (draft.selectedCourseId != null) {
        selectedCourseId = Number(
            draft.selectedCourseId);
      }
      if (courseEl && typeof draft.courseText === "string"
          && draft.courseText) {
        courseEl.value = draft.courseText;
      }

      if (draft.selectedTags && typeof draft.selectedTags === "object") {
        selectedTags.distance = draft.selectedTags.distance ?? null;
        selectedTags.pace = draft.selectedTags.pace ?? null;
        selectedTags.ages = Array.isArray(draft.selectedTags.ages)
            ? [...draft.selectedTags.ages] : [];
        selectedTags.gender = draft.selectedTags.gender ?? null;
      }

      if (draft.isDistanceLocked) {
        setDistanceLock(Number(selectedTags.distance));
      }

      // UI 반영
      try {
        updateLocationSummary();
      } catch (e) {
        // ignore
      }
      try {
        updateChipSelection();
      } catch (e) {
        // ignore
      }
      try {
        updateConditionSummary();
      } catch (e) {
        // ignore
      }
      try {
        generateTimeOptions();
      } catch (e) {
        // ignore
      }

      return true;
    } catch (e) {
      return false;
    }
  }

  function generateTimeOptions() {
    const timeOptionContainer = document.getElementById("timeOptionContainer");
  }

  // 로컬 시간 기준으로 날짜 문자열 생성 (타임존 문제 방지)
  function getLocalDateString(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  // 로컬 시간 기준으로 시간 문자열 생성
  function getLocalTimeString(date) {
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${hours}:${minutes}`;
  }

  // 시간 입력 검증 함수
  function validateTimeInput() {
    const dateInput = document.getElementById("meetingDate");
    const timeInput = document.getElementById("meetingTime");

    if (!dateInput || !timeInput || !dateInput.value || !timeInput.value) {
      return true; // 날짜나 시간이 없으면 검증 통과 (나중에 제출 시 검증)
    }

    const selectedDate = dateInput.value;
    const selectedTime = timeInput.value;

    // 날짜와 시간을 결합하여 LocalDateTime 생성
    const selectedDateTime = new Date(`${selectedDate}T${selectedTime}:00`);
    const now = new Date();
    const oneHourLater = new Date(now.getTime() + 60 * 60 * 1000); // 1시간 후

    // 오늘 날짜인 경우, 현재 시간 + 1시간 이후만 허용
    const today = getLocalDateString(now);
    if (selectedDate === today) {
      if (selectedDateTime < oneHourLater) {
        timeInput.setCustomValidity("모임 시간은 최소 1시간 후여야 합니다.");
        timeInput.reportValidity();
        return false;
      }
    }

    // 최대 2주 후까지 허용
    const twoWeeksLater = new Date(now.getTime() + 14 * 24 * 60 * 60 * 1000);
    if (selectedDateTime > twoWeeksLater) {
      timeInput.setCustomValidity("모임 시간은 최대 2주 후까지 설정할 수 있습니다.");
      timeInput.reportValidity();
      return false;
    }

    timeInput.setCustomValidity("");
    return true;
  }

  function setupDateChangeListener() {
    const dateInput = document.getElementById("meetingDate");
    const timeInput = document.getElementById("meetingTime");

    if (!dateInput || !timeInput) {
      console.error("날짜/시간 입력 필드를 찾을 수 없습니다.");
      return;
    }

    // 날짜 입력의 min/max 설정
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const twoWeeksLater = new Date(today);
    twoWeeksLater.setDate(twoWeeksLater.getDate() + 14);

    dateInput.min = getLocalDateString(today);
    dateInput.max = getLocalDateString(twoWeeksLater);

    // 날짜 변경 시 시간 입력 제약 조건 업데이트
    function updateTimeInputConstraints() {
      const selectedDate = dateInput.value;
      const todayStr = getLocalDateString(today);

      if (selectedDate === todayStr) {
        // 오늘 날짜인 경우: 현재 시간 + 1시간 이후만 허용
        const oneHourLater = new Date(now.getTime() + 60 * 60 * 1000);
        timeInput.min = getLocalTimeString(oneHourLater);

        // 현재 선택된 시간이 최소 시간보다 이전이면 초기화
        if (timeInput.value && timeInput.value < timeInput.min) {
          timeInput.value = '';
        }
      } else {
        // 미래 날짜인 경우: 제한 없음
        timeInput.min = '';
      }

      // 최대 시간: 23:59
      timeInput.max = '23:59';
    }

    // 초기 설정
    updateTimeInputConstraints();

    // 날짜 입력 필드 전체 클릭 가능하게
    dateInput.addEventListener("click", (e) => {
      // 브라우저 기본 동작 유지하되, 전체 영역 클릭 가능하게
      if (e.target === dateInput) {
        // showPicker() API 사용 (최신 브라우저)
        if (dateInput.showPicker) {
          try {
            dateInput.showPicker();
          } catch (err) {
            // showPicker()가 실패하면 focus()로 폴백
            dateInput.focus();
          }
        } else {
          // 구형 브라우저 폴백
          dateInput.focus();
        }
      }
    });

    // 시간 입력 필드 전체 클릭 가능하게
    timeInput.addEventListener("click", (e) => {
      if (e.target === timeInput) {
        // showPicker() API 사용 (최신 브라우저)
        if (timeInput.showPicker) {
          try {
            timeInput.showPicker();
          } catch (err) {
            timeInput.focus();
          }
        } else {
          timeInput.focus();
        }
      }
    });

    // 날짜 변경 시 시간 제약 조건 업데이트 및 자동 시간 설정
    dateInput.addEventListener("change", () => {
      const selectedDate = dateInput.value;
      const todayStr = getLocalDateString(today);

      if (selectedDate === todayStr && !timeInput.value) {
        // 오늘 날짜 선택 시 자동으로 1시간 후 시간 설정
        const oneHourLater = new Date(now.getTime() + 60 * 60 * 1000);
        timeInput.value = getLocalTimeString(oneHourLater);
      } else {
        timeInput.value = "";
      }

      updateTimeInputConstraints();
      validateTimeInput();
    });

    // 시간 입력 시 검증
    timeInput.addEventListener("change", () => {
      validateTimeInput();
    });

    // 시간 입력 시 실시간 검증 (blur 이벤트)
    timeInput.addEventListener("blur", () => {
      validateTimeInput();
    });
  }

  function initMap() {
    const mapContainer = document.getElementById("map");
    if (!mapContainer) {
      return;
    }

    const mapOption = {
      center: new kakao.maps.LatLng(37.5665, 126.9780),
      level: 3,
    };

    map = new kakao.maps.Map(mapContainer, mapOption);
    geocoder = new kakao.maps.services.Geocoder();

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
            map.setCenter(moveLatLon);
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

  async function fetchCourseDetail(courseId) {
    if (!courseId) {
      return null;
    }
    const token = localStorage.getItem("accessToken") || getCookie(
        "accessToken");
    const headers = {"Content-Type": "application/json"};
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
    const res = await fetch(`/api/courses/${courseId}`, {
      method: "GET",
      headers,
    });
    const body = await res.json().catch(() => null);
    if (!res.ok || !body?.success) {
      throw new Error(body?.message || "코스 정보를 불러올 수 없습니다.");
    }
    return body.data;
  }

  function setLocationLockByCourse(locked) {
    isLocationLockedByCourse = locked === true;
    const locationSummaryEl = document.getElementById("locationSummary");
    if (locationSummaryEl) {
      if (isLocationLockedByCourse) {
        locationSummaryEl.classList.add("locked");
      } else {
        locationSummaryEl.classList.remove("locked");
      }
    }
  }

  async function applyCourseStartLocationIfNeeded() {
    if (!selectedCourseId) {
      setLocationLockByCourse(false);
      return;
    }
    try {
      setLocationLockByCourse(true);
      const course = await fetchCourseDetail(selectedCourseId);
      const lat = course?.startLat;
      const lng = course?.startLng;
      const addr = course?.address;
      if (lat == null || lng == null || !Number.isFinite(lat)
          || !Number.isFinite(lng)) {
        throw new Error("코스 출발지 좌표가 없습니다.");
      }
      selectedLat = Number(lat);
      selectedLng = Number(lng);
      selectedAddress = addr ? String(addr) : "코스 출발지";
      updateLocationSummary();

      // 지도(모달)가 이미 만들어져 있으면 마커도 동기화
      try {
        if (map) {
          const latlng = new kakao.maps.LatLng(selectedLat, selectedLng);
          map.setCenter(latlng);
          if (marker) {
            marker.setMap(null);
          }
          marker = new kakao.maps.Marker({position: latlng, map});
        }
      } catch (e) {
        // ignore
      }
    } catch (e) {
      console.warn("코스 출발지 자동 적용 실패:", e?.message || e);
      showToast(e?.message || "코스 출발지를 불러올 수 없습니다.", "error");
    }
  }

  if (locationModal && locationSummary && closeLocationModalBtn
      && confirmLocationModalBtn) {
    function openLocationModal() {
      if (isLocationLockedByCourse) {
        showToast("코스를 선택한 경우 출발지는 코스 출발지로 고정됩니다.", "error");
        return;
      }
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

  // (코스 선택에서 돌아온 경우/새로고침) 작성 중이던 초안 복원
  restoreDraft();

  // 러닝(코스) 선택: 코스 목록 -> 코스 상세의 '확인' 버튼으로 선택 확정
  function applyCourseFromQueryParams() {
    try {
      const u = new URL(window.location.href);
      const courseId = u.searchParams.get("courseId");
      const courseName = u.searchParams.get("courseName");
      const courseDistanceKm = u.searchParams.get("courseDistanceKm");
      if (courseId) {
        selectedCourseId = parseInt(courseId, 10);
        const courseInput = document.getElementById("course");
        if (courseInput) {
          courseInput.value = courseName || "코스 선택됨";
        }
        if (courseDistanceKm) {
          setDistanceLock(parseFloat(courseDistanceKm));
          updateConditionSummary();
        }
        // ✅ 코스 출발지 자동 세팅 + 출발지 잠금
        applyCourseStartLocationIfNeeded();
      }
    } catch (e) {
      // ignore
    }
  }

  applyCourseFromQueryParams();
  // (초안 복원 등) courseId가 이미 잡혀있으면 출발지 재동기화
  applyCourseStartLocationIfNeeded();

  const courseInputEl = document.getElementById("course");
  if (courseInputEl) {
    courseInputEl.addEventListener("click", () => {
      // 코스 선택하러 가기 전에 현재 작성 중 값 저장
      saveDraft({from: "courseSelect"});
      const returnTo = window.location.pathname + window.location.search;
      window.location.href = `/course?selectMode=recruit&returnTo=${encodeURIComponent(
          returnTo)}`;
    });
  }

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

  // 오버레이 클릭 시 모달 닫기
  conditionModal.querySelector(".bottom-sheet-overlay").addEventListener(
      "click", closeConditionModal);

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
          // 이미 선택된 나이대를 클릭한 경우 - 해제
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
          // 새로운 나이대를 선택하는 경우
          if (selectedTags.ages.length > 0) {
            // 기존 선택된 나이대들과의 범위 계산
            const sorted = [...selectedTags.ages].sort((a, b) => a - b);
            const min = sorted[0];
            const max = sorted[sorted.length - 1];

            // 나이대 순서 배열
            const allAges = [10, 20, 30, 40, 50];

            // 선택하려는 나이대가 범위 밖에 있는 경우, 사이의 모든 나이대 선택
            if (age < min) {
              // 선택하려는 나이대가 기존 최소값보다 작은 경우
              // age부터 min까지의 모든 나이대 선택
              const startIndex = allAges.indexOf(age);
              const endIndex = allAges.indexOf(min);

              for (let i = startIndex; i <= endIndex; i++) {
                const ageToAdd = allAges[i];
                if (!selectedTags.ages.includes(ageToAdd)) {
                  selectedTags.ages.push(ageToAdd);
                  const chipToSelect = document.querySelector(
                      `.chip[data-age="${ageToAdd}"]`);
                  if (chipToSelect) {
                    chipToSelect.classList.add("selected");
                  }
                }
              }
            } else if (age > max) {
              // 선택하려는 나이대가 기존 최대값보다 큰 경우
              // max부터 age까지의 모든 나이대 선택
              const startIndex = allAges.indexOf(max);
              const endIndex = allAges.indexOf(age);

              for (let i = startIndex; i <= endIndex; i++) {
                const ageToAdd = allAges[i];
                if (!selectedTags.ages.includes(ageToAdd)) {
                  selectedTags.ages.push(ageToAdd);
                  const chipToSelect = document.querySelector(
                      `.chip[data-age="${ageToAdd}"]`);
                  if (chipToSelect) {
                    chipToSelect.classList.add("selected");
                  }
                }
              }
            } else {
              // 선택하려는 나이대가 범위 안에 있는 경우 (이미 선택된 나이대 사이)
              // 이 경우는 발생하지 않아야 하지만 안전을 위해 처리
              chip.classList.add("selected");
              selectedTags.ages.push(age);
            }
          } else {
            // 첫 번째 선택인 경우
            chip.classList.add("selected");
            selectedTags.ages.push(age);
          }
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
        chip.textContent = `${selectedTags.pace}분대`;
        summaryChips.appendChild(chip);
      }

      if (selectedTags.ages.length > 0) {
        const sorted = [...selectedTags.ages].sort((a, b) => a - b);
        const ageTexts = sorted.map(age => {
          if (age === 50) {
            return "50대 이상";
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

    // 50대 이상인 경우 100세까지
    if (sorted[sorted.length - 1] === 50) {
      ageMax = 100;
    }

    return {ageMin, ageMax};
  }

  // 에러 클래스 제거 함수
  function clearErrors() {
    document.querySelectorAll(
        '.form-input.error, .form-textarea.error, .condition-summary.error, #locationSummary.error, #dateTimeSummary.error').forEach(
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

    // 페이스를 "분:초" 형식으로 변환 (예: "3" → "3:00")
    const targetPace = `${selectedTags.pace}:00`;

    const requestData = {
      title: title,
      content: content,
      meetingPlace: selectedAddress,
      latitude: selectedLat,
      longitude: selectedLng,
      targetDistance: selectedTags.distance,
      targetPace: targetPace,
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

      const response = await fetch("/api/recruit", {
        method: "POST",
        headers: headers,
        body: JSON.stringify(requestData),
      });

      const result = await response.json();

      if (response.ok && result.success) {
        try {
          sessionStorage.removeItem(DRAFT_KEY);
        } catch (e) {
          // ignore
        }
        showToast("모집글이 성공적으로 생성되었습니다.", "success");
        setTimeout(() => {
          window.location.href = "/recruit";
        }, 1500);
      } else {
        // 백엔드 에러 메시지를 사용자 친화적으로 변경
        let errorMessage = result.message || "모집글 생성에 실패했습니다.";
        // 에러 코드 또는 메시지로 확인
        if (result.code === "RO16" || errorMessage.includes("참여 가능 성별이 아닙니다")) {
          errorMessage = "본인의 성별과 다른 성별 제한은 설정할 수 없습니다.";
        } else if (result.code === "RO15" || errorMessage.includes(
            "참여 가능 나이가 아닙니다")) {
          errorMessage = "본인의 나이가 포함된 연령대만 설정할 수 있습니다.";
        }
        showToast(errorMessage, "error");
        console.error("Error:", result);
      }
    } catch (error) {
      console.error("모집글 생성 중 오류 발생:", error);
      showToast("모집글 생성 중 오류가 발생했습니다.", "error");
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

  generateTimeOptions();

  // 초기 요약 뷰 업데이트
  updateConditionSummary();
  updateLocationSummary();
  updateDateTimeSummary();
});
