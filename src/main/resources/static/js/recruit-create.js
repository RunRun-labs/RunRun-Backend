document.addEventListener("DOMContentLoaded", () => {
  let map = null;
  let marker = null;
  let geocoder = null;
  let selectedLat = null;
  let selectedLng = null;
  let selectedCourseId = null;

  const selectedTags = {
    distance: null,
    pace: null,
    ages: [],
    gender: null,
  };

  function generateTimeOptions() {
    const timeSelect = document.getElementById("meetingTime");
    
    // 기존 옵션 모두 제거
    timeSelect.innerHTML = "";

    const defaultOption = document.createElement("option");
    defaultOption.value = "";
    defaultOption.textContent = "시간 선택";
    defaultOption.disabled = true;
    defaultOption.selected = true;
    timeSelect.appendChild(defaultOption);

    // 선택된 날짜 확인
    const selectedDate = document.getElementById("meetingDate").value;
    const today = new Date().toISOString().split("T")[0];
    
    // 오늘 날짜인 경우, 현재 시간 + 1시간 이후만 표시
    let minHour = 0;
    let minMinute = 0;
    
    if (selectedDate === today) {
      // 현재 시간 + 1시간 계산
      const now = new Date();
      const oneHourLater = new Date(now.getTime() + 60 * 60 * 1000); // 1시간 후
      minHour = oneHourLater.getHours();
      minMinute = oneHourLater.getMinutes();
      
      // 30분 단위로 올림 처리 (예: 14:15 -> 14:30, 14:45 -> 15:00)
      if (minMinute > 0 && minMinute <= 30) {
        minMinute = 30;
      } else if (minMinute > 30) {
        minHour += 1;
        minMinute = 0;
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
        const option = document.createElement("option");
        option.value = timeString;
        option.textContent = timeString;
        timeSelect.appendChild(option);
      }
    }
  }
  
  // 날짜 변경 시 시간 옵션 업데이트
  function setupDateChangeListener() {
    const dateInput = document.getElementById("meetingDate");
    dateInput.addEventListener("change", () => {
      // 시간 선택 초기화
      document.getElementById("meetingTime").value = "";
      generateTimeOptions();
    });
  }

  function initMap() {
    const mapContainer = document.getElementById("map");
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

          document.getElementById("meetingPlace").value = detailAddress;

        } else {
          console.error("주소 변환 실패:", status);
          document.getElementById("meetingPlace").value = "주소를 찾을 수 없습니다";
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

  document.getElementById("backBtn").addEventListener("click", () => {
    window.location.href = "/recruit";
  });

  document.getElementById("courseSearchBtn").addEventListener("click", () => {
    showToast("코스 조회 기능은 추후 구현 예정입니다.", "error");
  });

  document.querySelectorAll('.chip[data-distance]').forEach((chip) => {
    chip.addEventListener("click", () => {

      document.querySelectorAll('.chip[data-distance]').forEach((c) => {
        c.classList.remove("selected");
      });
      chip.classList.add("selected");
      selectedTags.distance = parseFloat(chip.getAttribute("data-distance"));
    });
  });

  document.querySelectorAll('.chip[data-pace]').forEach((chip) => {
    chip.addEventListener("click", () => {
      document.querySelectorAll('.chip[data-pace]').forEach((c) => {
        c.classList.remove("selected");
      });
      chip.classList.add("selected");
      // data-pace 속성에서 직접 가져오기 (이미 "분:초" 형식)
      selectedTags.pace = chip.getAttribute("data-pace");
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
              (age === 0 && min === 20) ||  // 10대 이하를 20대와 연속으로 선택
              (age === 20 && max === 0) ||  // 20대를 10대 이하와 연속으로 선택
              (age === 70 && max === 60) || // 70대 이상을 60대와 연속으로 선택
              (age === 60 && min === 70);   // 60대를 70대 이상과 연속으로 선택

          if (!isConsecutive) {
            showToast("연속된 나이대만 선택할 수 있습니다.", "error");
            return;
          }
        }

        chip.classList.add("selected");
        selectedTags.ages.push(age);
      }

      selectedTags.ages.sort((a, b) => a - b);
    });
  });

  function validateAndCleanAges() {
    if (selectedTags.ages.length === 0) {
      document.querySelectorAll('.chip[data-age]').forEach((chip) => {
        chip.classList.remove("selected");
      });
      return;
    }

    const sorted = [...selectedTags.ages].sort((a, b) => a - b);

    const validAges = [sorted[0]];

    for (let i = 1; i < sorted.length; i++) {
      if (sorted[i] === sorted[i - 1] + 10) {
        validAges.push(sorted[i]);
      } else {
        break;
      }
    }

    selectedTags.ages = validAges;
    document.querySelectorAll('.chip[data-age]').forEach((chip) => {
      const age = parseInt(chip.getAttribute("data-age"));
      if (validAges.includes(age)) {
        chip.classList.add("selected");
      } else {
        chip.classList.remove("selected");
      }
    });
  }

  document.querySelectorAll('.chip[data-gender]').forEach((chip) => {
    chip.addEventListener("click", () => {
      document.querySelectorAll('.chip[data-gender]').forEach((c) => {
        c.classList.remove("selected");
      });
      chip.classList.add("selected");
      selectedTags.gender = chip.getAttribute("data-gender");
    });
  });

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
    document.querySelectorAll('.form-input.error, .form-textarea.error, .tag-group.error').forEach(el => {
      el.classList.remove('error');
    });
  }

  // 에러 표시 함수
  function showError(element, message) {
    clearErrors();
    
    if (element) {
      element.classList.add('error');
      
      // 입력 필드인 경우
      if (element.classList.contains('form-input') || element.classList.contains('form-textarea')) {
        element.focus();
        element.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
      // 태그 그룹인 경우
      else if (element.classList.contains('tag-group')) {
        // 태그 그룹의 첫 번째 칩으로 스크롤
        const firstChip = element.querySelector('.chip');
        if (firstChip) {
          firstChip.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
      } else {
        // 일반 요소인 경우
        element.scrollIntoView({ behavior: 'smooth', block: 'center' });
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
    
    const title = document.getElementById("title").value.trim();
    const content = document.getElementById("content").value.trim();
    const maxParticipants = parseInt(
        document.getElementById("maxParticipants").value);
    const meetingDate = document.getElementById("meetingDate").value;
    const meetingTime = document.getElementById("meetingTime").value;
    const meetingPlace = document.getElementById("meetingPlace").value.trim();

    // 유효성 검사
    if (!title) {
      showError(document.getElementById("title"), "제목을 입력해주세요.");
      return;
    }

    if (!content) {
      showError(document.getElementById("content"), "설명을 입력해주세요.");
      return;
    }

    if (!meetingPlace || !selectedLat || !selectedLng) {
      showError(document.getElementById("meetingPlace"), "지도에서 출발지를 선택해주세요.");
      return;
    }

    if (!maxParticipants || maxParticipants < 2) {
      showError(document.getElementById("maxParticipants"), "최대 인원은 2명 이상이어야 합니다.");
      return;
    }

    if (!meetingDate || !meetingTime) {
      if (!meetingDate) {
        showError(document.getElementById("meetingDate"), "출발 날짜를 선택해주세요.");
      } else {
        showError(document.getElementById("meetingTime"), "출발 시간을 선택해주세요.");
      }
      return;
    }

    if (!selectedTags.distance) {
      const distanceGroup = document.querySelector('.chip[data-distance]').closest('.tag-group');
      showError(distanceGroup, "뛸 거리를 선택해주세요.");
      return;
    }

    if (!selectedTags.pace) {
      const paceGroup = document.querySelector('.chip[data-pace]').closest('.tag-group');
      showError(paceGroup, "페이스를 선택해주세요.");
      return;
    }

    const ageRange = calculateAgeRange();
    if (ageRange.ageMin === null || ageRange.ageMin === undefined ||
        ageRange.ageMax === null || ageRange.ageMax === undefined) {
      const ageGroup = document.querySelector('.chip[data-age]').closest('.tag-group');
      showError(ageGroup, "나이대를 선택해주세요.");
      return;
    }

    if (!selectedTags.gender) {
      const genderGroup = document.querySelector('.chip[data-gender]').closest('.tag-group');
      showError(genderGroup, "성별을 선택해주세요.");
      return;
    }

    // 날짜와 시간을 LocalDateTime 형식으로 변환
    // 형식: "YYYY-MM-DDTHH:mm:ss"
    const meetingAt = `${meetingDate}T${meetingTime}:00`;

    const requestData = {
      title: title,
      content: content,
      meetingPlace: meetingPlace,
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

      const response = await fetch("/api/recruit", {
        method: "POST",
        headers: headers,
        body: JSON.stringify(requestData),
      });

      const result = await response.json();

      if (response.ok && result.success) {
        showToast("모집글이 성공적으로 생성되었습니다.", "success");
        setTimeout(() => {
          window.location.href = "/recruit";
        }, 1500);
      } else {
        showToast(result.message || "모집글 생성에 실패했습니다.", "error");
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
    titleInput.addEventListener('input', function() {
      updateCharCounter(this, titleCounter, 100);
      this.classList.remove('error');
    });
    titleInput.addEventListener('change', function() {
      this.classList.remove('error');
    });
  }

  // 내용 글자수 카운터
  const contentInput = document.getElementById("content");
  const contentCounter = document.getElementById("contentCounter");
  if (contentInput && contentCounter) {
    contentInput.addEventListener('input', function() {
      updateCharCounter(this, contentCounter, 500);
      this.classList.remove('error');
    });
    contentInput.addEventListener('change', function() {
      this.classList.remove('error');
    });
  }

  // 입력 필드에서 에러 클래스 자동 제거
  document.querySelectorAll('.form-input, .form-textarea').forEach(input => {
    if (input.id !== 'title' && input.id !== 'content') {
      input.addEventListener('input', function() {
        this.classList.remove('error');
      });
      input.addEventListener('change', function() {
        this.classList.remove('error');
      });
    }
  });

  // 칩 선택 시 에러 클래스 제거
  document.querySelectorAll('.chip').forEach(chip => {
    chip.addEventListener('click', function() {
      const tagGroup = this.closest('.tag-group');
      if (tagGroup) {
        tagGroup.classList.remove('error');
      }
    });
  });

  const today = new Date().toISOString().split("T")[0];
  document.getElementById("meetingDate").setAttribute("min", today);

  // 날짜 변경 리스너 설정
  setupDateChangeListener();
  
  generateTimeOptions();
  initMap();
});

