document.addEventListener("DOMContentLoaded", () => {
  let map = null;
  let marker = null;
  let geocoder = null;
  let selectedLat = null;
  let selectedLng = null;
  let selectedCourseId = null;
  let recruitId = null;
  let recruitData = null;

  // 선택된 태그 상태
  const selectedTags = {
    distance: null,
    pace: null,
    ages: [],
    gender: null,
  };

  // URL에서 recruitId 추출
  function extractRecruitIdFromUrl() {
    const path = window.location.pathname;
    const match = path.match(/\/recruit\/(\d+)\/edit/);
    if (match && match[1]) {
      return parseInt(match[1], 10);
    }
    return null;
  }

  // 시간 옵션 생성 (30분 단위)
  function generateTimeOptions() {
    const timeSelect = document.getElementById("meetingTime");
    
    // 기본 옵션 추가
    const defaultOption = document.createElement("option");
    defaultOption.value = "";
    defaultOption.textContent = "시간 선택";
    defaultOption.disabled = true;
    defaultOption.selected = true;
    timeSelect.appendChild(defaultOption);
    
    for (let hour = 0; hour < 24; hour++) {
      for (let minute = 0; minute < 60; minute += 30) {
        const timeString = `${String(hour).padStart(2, "0")}:${String(
            minute).padStart(2, "0")}`;
        const option = document.createElement("option");
        option.value = timeString;
        option.textContent = timeString;
        timeSelect.appendChild(option);
      }
    }
  }

  // 카카오맵 초기화
  function initMap(lat = 37.5665, lng = 126.9780) {
    const mapContainer = document.getElementById("map");
    const mapOption = {
      center: new kakao.maps.LatLng(lat, lng),
      level: 3,
    };

    map = new kakao.maps.Map(mapContainer, mapOption);
    geocoder = new kakao.maps.services.Geocoder();

    // 지도 클릭 이벤트
    kakao.maps.event.addListener(map, "click", (mouseEvent) => {
      const latlng = mouseEvent.latLng;
      selectedLat = latlng.getLat();
      selectedLng = latlng.getLng();

      // 기존 마커 제거
      if (marker) {
        marker.setMap(null);
      }

      // 새 마커 생성
      marker = new kakao.maps.Marker({
        position: latlng,
        map: map,
      });

      // 좌표를 상세 주소로 변환
      geocoder.coord2Address(selectedLng, selectedLat, (result, status) => {
        if (status === kakao.maps.services.Status.OK) {
          // 1. 도로명 주소가 있으면 최우선으로 사용
          const roadAddr = result[0].road_address;
          // 2. 없으면 지번 주소 사용
          const jibunAddr = result[0].address;

          let detailAddress = "";

          if (roadAddr) {
            detailAddress = roadAddr.address_name;
            // 건물명이 있으면 괄호로 추가
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
  }

  // 기존 데이터로 지도 설정
  function setMapWithData(lat, lng, placeName) {
    selectedLat = lat;
    selectedLng = lng;
    
    // 지도 초기화
    initMap(lat, lng);
    
    // 마커 생성
    const position = new kakao.maps.LatLng(lat, lng);
    marker = new kakao.maps.Marker({
      position: position,
      map: map,
    });
    
    // 장소명 설정
    if (placeName) {
      document.getElementById("meetingPlace").value = placeName;
    }
  }

  // 데이터 바인딩
  function bindDataToForm(data) {
    // 제목, 내용, 최대 인원
    document.getElementById("title").value = data.title || "";
    document.getElementById("content").value = data.content || "";
    document.getElementById("maxParticipants").value = data.maxParticipants || 2;

    // meetingAt을 날짜와 시간으로 분리
    if (data.meetingAt) {
      const meetingDateTime = new Date(data.meetingAt);
      const year = meetingDateTime.getFullYear();
      const month = String(meetingDateTime.getMonth() + 1).padStart(2, "0");
      const day = String(meetingDateTime.getDate()).padStart(2, "0");
      const dateString = `${year}-${month}-${day}`;
      
      const hours = String(meetingDateTime.getHours()).padStart(2, "0");
      const minutes = String(meetingDateTime.getMinutes()).padStart(2, "0");
      const timeString = `${hours}:${minutes}`;
      
      document.getElementById("meetingDate").value = dateString;
      
      // 시간 선택 옵션 설정
      const timeSelect = document.getElementById("meetingTime");
      const timeOptions = timeSelect.querySelectorAll("option");
      timeOptions.forEach((option) => {
        if (option.value === timeString) {
          option.selected = true;
        }
      });
    }

    // 지도 및 마커 설정
    if (data.latitude && data.longitude) {
      setMapWithData(data.latitude, data.longitude, data.meetingPlace);
    }

    // 코스 정보 설정
    if (data.courseId) {
      selectedCourseId = data.courseId;
      // 코스 이름은 API 응답에 없을 수 있으므로 필요시 추가 조회
      if (data.courseImageUrl) {
        // 코스 정보가 있으면 표시 (필요시 구현)
      }
    }

    // 칩 상태 복원
    restoreChipStates(data);
  }

  // 칩 상태 복원
  function restoreChipStates(data) {
    // 단일 선택: 거리
    if (data.targetDistance) {
      const distanceChip = document.querySelector(
          `.chip[data-distance="${data.targetDistance}"]`
      );
      if (distanceChip) {
        distanceChip.classList.add("selected");
        selectedTags.distance = data.targetDistance;
      }
    }

    // 단일 선택: 페이스
    if (data.targetPace) {
      const paceChip = document.querySelector(
          `.chip[data-pace="${data.targetPace}"]`
      );
      if (paceChip) {
        paceChip.classList.add("selected");
        selectedTags.pace = data.targetPace;
      }
    }

    // 단일 선택: 성별
    if (data.genderLimit) {
      const genderChip = document.querySelector(
          `.chip[data-gender="${data.genderLimit}"]`
      );
      if (genderChip) {
        genderChip.classList.add("selected");
        selectedTags.gender = data.genderLimit;
      }
    }

    // 다중 선택: 나이대
    if (data.ageMin !== null && data.ageMin !== undefined && 
        data.ageMax !== null && data.ageMax !== undefined) {
      // ageMin과 ageMax 범위를 계산해서 해당하는 나이대 칩들을 선택
      const ageRanges = [];
      
      // 10대 이하 (0-19): ageMin <= 19 && ageMax >= 0
      if (data.ageMin <= 19 && data.ageMax >= 0) {
        ageRanges.push(0);
      }
      
      // 20대부터 60대까지 (10단위)
      // 20대: 20-29, 30대: 30-39, 40대: 40-49, 50대: 50-59, 60대: 60-69
      for (let age = 20; age <= 60; age += 10) {
        const ageEnd = age + 9; // 20대는 20-29
        if (data.ageMin <= ageEnd && data.ageMax >= age) {
          ageRanges.push(age);
        }
      }
      
      // 70대 이상 (70-100): ageMin <= 100 && ageMax >= 70
      if (data.ageMin <= 100 && data.ageMax >= 70) {
        ageRanges.push(70);
      }

      // 선택된 나이대 칩에 selected 클래스 추가
      ageRanges.forEach((age) => {
        const ageChip = document.querySelector(`.chip[data-age="${age}"]`);
        if (ageChip) {
          ageChip.classList.add("selected");
          if (!selectedTags.ages.includes(age)) {
            selectedTags.ages.push(age);
          }
        }
      });
      
      // 정렬 유지
      selectedTags.ages.sort((a, b) => a - b);
    }
  }

  // 기존 데이터 불러오기
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
        recruitData = result.data;
        
        // 수정 권한 체크
        if (!recruitData.isAuthor) {
          alert("수정할 수 없습니다.");
          window.location.href = "/recruit";
          return;
        }

        if (recruitData.currentParticipants > 1) {
          alert("수정할 수 없습니다.");
          window.location.href = "/recruit";
          return;
        }

        // 데이터 바인딩
        bindDataToForm(recruitData);
      } else {
        alert(result.message || "모집글을 불러올 수 없습니다.");
        window.location.href = "/recruit";
      }
    } catch (error) {
      console.error("모집글 불러오기 중 오류 발생:", error);
      alert("모집글을 불러오는 중 오류가 발생했습니다.");
      window.location.href = "/recruit";
    }
  }

  // 뒤로가기 버튼
  document.getElementById("backBtn").addEventListener("click", () => {
    window.location.href = "/recruit";
  });

  // 코스 조회 버튼 (임시 구현)
  document.getElementById("courseSearchBtn").addEventListener("click", () => {
    // TODO: 코스 조회 모달 또는 페이지 구현
    alert("코스 조회 기능은 추후 구현 예정입니다.");
  });

  // 뛸 거리 칩 선택
  document.querySelectorAll('.chip[data-distance]').forEach((chip) => {
    chip.addEventListener("click", () => {
      // 기존 선택 제거
      document.querySelectorAll('.chip[data-distance]').forEach((c) => {
        c.classList.remove("selected");
      });
      // 새 선택
      chip.classList.add("selected");
      selectedTags.distance = parseFloat(chip.getAttribute("data-distance"));
    });
  });

  // 페이스 칩 선택 (단일 선택)
  document.querySelectorAll('.chip[data-pace]').forEach((chip) => {
    chip.addEventListener("click", () => {
      // 기존 선택 제거
      document.querySelectorAll('.chip[data-pace]').forEach((c) => {
        c.classList.remove("selected");
      });
      // 새 선택
      chip.classList.add("selected");
      selectedTags.pace = chip.getAttribute("data-pace");
    });
  });

  // 나이대 칩 선택 (연속 선택 및 중간 해제 방지 로직)
  document.querySelectorAll('.chip[data-age]').forEach((chip) => {
    chip.addEventListener("click", () => {
      const age = parseInt(chip.getAttribute("data-age"));
      const index = selectedTags.ages.indexOf(age);

      if (index > -1) {
        // [1. 해제 시도]
        if (selectedTags.ages.length > 1) {
          const sorted = [...selectedTags.ages].sort((a, b) => a - b);
          // 선택된 것들 중 최소값도 아니고 최대값도 아니면 '중간값'임
          if (age !== sorted[0] && age !== sorted[sorted.length - 1]) {
            alert("연속된 나이대 중간은 해제할 수 없습니다. 양 끝부터 해제해주세요.");
            return; // 함수 종료 (해제 안 됨)
          }
        }

        // 끝부분일 경우에만 해제 진행
        chip.classList.remove("selected");
        selectedTags.ages.splice(index, 1);
      } else {
        // [2. 선택 시도]
        if (selectedTags.ages.length > 0) {
          const sorted = [...selectedTags.ages].sort((a, b) => a - b);
          const min = sorted[0];
          const max = sorted[sorted.length - 1];

          // 특수 케이스: 10대 이하(0)는 20대(20)와 연속으로 간주
          // 특수 케이스: 70대 이상(70)은 60대(60)와 연속으로 간주
          const isConsecutive = 
            age === min - 10 || 
            age === max + 10 ||
            (age === 0 && min === 20) ||  // 10대 이하를 20대와 연속으로 선택
            (age === 20 && max === 0) ||  // 20대를 10대 이하와 연속으로 선택
            (age === 70 && max === 60) || // 70대 이상을 60대와 연속으로 선택
            (age === 60 && min === 70);   // 60대를 70대 이상과 연속으로 선택

          if (!isConsecutive) {
            alert("연속된 나이대만 선택할 수 있습니다.");
            return;
          }
        }

        // 연속된 경우에만 선택 진행
        chip.classList.add("selected");
        selectedTags.ages.push(age);
      }

      // 최종 배열 정렬 유지
      selectedTags.ages.sort((a, b) => a - b);
    });
  });

  // 나이대 연속 선택 검증
  function validateAndCleanAges() {
    if (selectedTags.ages.length === 0) {
      // 모든 선택 제거
      document.querySelectorAll('.chip[data-age]').forEach((chip) => {
        chip.classList.remove("selected");
      });
      return;
    }

    // 정렬된 나이대 배열
    const sorted = [...selectedTags.ages].sort((a, b) => a - b);

    // 연속된 그룹 찾기 (처음부터 연속된 것만 유지)
    const validAges = [sorted[0]];

    for (let i = 1; i < sorted.length; i++) {
      if (sorted[i] === sorted[i - 1] + 10) {
        // 연속된 경우
        validAges.push(sorted[i]);
      } else {
        // 연속되지 않은 경우 중단
        break;
      }
    }

    // 유효하지 않은 선택 제거
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

  // 성별 칩 선택 (단일 선택)
  document.querySelectorAll('.chip[data-gender]').forEach((chip) => {
    chip.addEventListener("click", () => {
      // 기존 선택 제거
      document.querySelectorAll('.chip[data-gender]').forEach((c) => {
        c.classList.remove("selected");
      });
      // 새 선택
      chip.classList.add("selected");
      selectedTags.gender = chip.getAttribute("data-gender");
    });
  });

  // 나이대 범위 계산
  function calculateAgeRange() {
    if (selectedTags.ages.length === 0) {
      return {ageMin: null, ageMax: null};
    }

    const sorted = [...selectedTags.ages].sort((a, b) => a - b);
    let ageMin = sorted[0];
    let ageMax = sorted[sorted.length - 1] + 9; // 예: 20대 -> 20-29

    // 10대 이하 (data-age="0") 처리: 0-19
    if (sorted[0] === 0) {
      ageMin = 0;
    }

    // 70대 이상 (data-age="70") 처리: 70-100
    if (sorted[sorted.length - 1] === 70) {
      ageMax = 100;
    }

    return {ageMin, ageMax};
  }

  // 폼 제출 (수정)
  document.getElementById("submitBtn").addEventListener("click", async () => {
    // 유효성 검사
    const title = document.getElementById("title").value.trim();
    const content = document.getElementById("content").value.trim();
    const maxParticipants = parseInt(
        document.getElementById("maxParticipants").value);
    const meetingDate = document.getElementById("meetingDate").value;
    const meetingTime = document.getElementById("meetingTime").value;
    const meetingPlace = document.getElementById("meetingPlace").value.trim();

    if (!title) {
      alert("제목을 입력해주세요.");
      return;
    }

    if (!content) {
      alert("설명을 입력해주세요.");
      return;
    }

    if (!meetingPlace || !selectedLat || !selectedLng) {
      alert("지도에서 출발지를 선택해주세요.");
      return;
    }

    if (!maxParticipants || maxParticipants < 2) {
      alert("최대 인원은 2명 이상이어야 합니다.");
      return;
    }

    if (!meetingDate || !meetingTime) {
      alert("출발시간을 선택해주세요.");
      return;
    }

    if (!selectedTags.distance) {
      alert("뛸 거리를 선택해주세요.");
      return;
    }

    if (!selectedTags.pace) {
      alert("페이스를 선택해주세요.");
      return;
    }

    const ageRange = calculateAgeRange();
    if (!ageRange.ageMin || !ageRange.ageMax) {
      alert("나이대를 선택해주세요.");
      return;
    }

    if (!selectedTags.gender) {
      alert("성별을 선택해주세요.");
      return;
    }

    // 날짜와 시간 결합하여 ISO 형식으로 변환
    const meetingDateTime = new Date(`${meetingDate}T${meetingTime}:00`);
    const meetingAt = meetingDateTime.toISOString();

    // 요청 데이터 구성 (RecruitUpdateReqDto 구조에 맞게)
    const requestData = {
      title: title,
      content: content,
      meetingPlace: meetingPlace,
      latitude: selectedLat,
      longitude: selectedLng,
      maxParticipants: maxParticipants,
      meetingAt: meetingAt,
      targetDistance: selectedTags.distance,
      targetPace: selectedTags.pace,
      genderLimit: selectedTags.gender,
    };

    // courseId가 있으면 추가
    if (selectedCourseId) {
      requestData.courseId = selectedCourseId;
    }

    try {
      // JWT 토큰 가져오기
      const token = localStorage.getItem("accessToken") || getCookie("accessToken");
      const headers = {
        "Content-Type": "application/json",
      };

      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }

      const response = await fetch(`/api/recruit/${recruitId}`, {
        method: "PUT",
        headers: headers,
        body: JSON.stringify(requestData),
      });

      const result = await response.json();

      if (response.ok && result.success) {
        alert("모집글이 성공적으로 수정되었습니다.");
        window.location.href = "/recruit";
      } else {
        alert(result.message || "모집글 수정에 실패했습니다.");
        console.error("Error:", result);
      }
    } catch (error) {
      console.error("모집글 수정 중 오류 발생:", error);
      alert("모집글 수정 중 오류가 발생했습니다.");
    }
  });

  // Cookie에서 토큰 가져오기
  function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
      return parts.pop().split(";").shift();
    }
    return null;
  }

  // 오늘 날짜를 최소 날짜로 설정
  const today = new Date().toISOString().split("T")[0];
  document.getElementById("meetingDate").setAttribute("min", today);

  // 초기화
  recruitId = extractRecruitIdFromUrl();
  if (!recruitId) {
    alert("잘못된 URL입니다.");
    window.location.href = "/recruit";
    return;
  }

  generateTimeOptions();
  // 지도는 데이터 로드 후 설정되므로 여기서는 기본 초기화만
  initMap();
  
  // 데이터 불러오기
  loadRecruitData();
});

