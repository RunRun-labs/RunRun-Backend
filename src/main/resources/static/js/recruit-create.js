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
    alert("코스 조회 기능은 추후 구현 예정입니다.");
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

      const rawPace = chip.textContent.trim();
      const paceMatch = rawPace.match(/\d{1,2}:\d{2}/); // "분:초" 부분만 찾아냄

      if (paceMatch) {
        selectedTags.pace = paceMatch[0];
      } else {
        selectedTags.pace = rawPace;
      }
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
            alert("연속된 나이대 중간은 해제할 수 없습니다. 양 끝부터 해제해주세요.");
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
            alert("연속된 나이대만 선택할 수 있습니다.");
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

  document.getElementById("submitBtn").addEventListener("click", async () => {
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
    if (ageRange.ageMin === null || ageRange.ageMin === undefined ||
        ageRange.ageMax === null || ageRange.ageMax === undefined) {
      alert("나이대를 선택해주세요.");
      return;
    }

    if (!selectedTags.gender) {
      alert("성별을 선택해주세요.");
      return;
    }

    const meetingDateTime = new Date(`${meetingDate}T${meetingTime}:00`);
    const meetingAt = meetingDateTime.toISOString();

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
      ageMin: ageRange.ageMin,
      ageMax: ageRange.ageMax,
      genderLimit: selectedTags.gender,
    };

    // courseId가 있으면 추가
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
        alert("모집글이 성공적으로 생성되었습니다.");
        window.location.href = "/recruit";
      } else {
        alert(result.message || "모집글 생성에 실패했습니다.");
        console.error("Error:", result);
      }
    } catch (error) {
      console.error("모집글 생성 중 오류 발생:", error);
      alert("모집글 생성 중 오류가 발생했습니다.");
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

  const today = new Date().toISOString().split("T")[0];
  document.getElementById("meetingDate").setAttribute("min", today);

  generateTimeOptions();
  initMap();
});

