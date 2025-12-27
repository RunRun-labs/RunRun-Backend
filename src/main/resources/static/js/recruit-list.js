document.addEventListener("DOMContentLoaded", () => {
  let map = null;
  let markers = [];
  let infowindows = [];
  let userLat = null;
  let userLng = null;
  let currentRadius = 3;
  let currentSortBy = "distance";
  let currentKeyword = "";

  // 사용자 위치 가져오기
  function getUserLocation() {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
          (position) => {
            userLat = position.coords.latitude;
            userLng = position.coords.longitude;
            initMap();
            loadRecruitList();
          },
          (error) => {
            console.error("위치 정보를 가져올 수 없습니다:", error);
            userLat = 37.5665; // 기본 위치 (서울시청)
            userLng = 126.9780;
            initMap();
            loadRecruitList();
          }
      );
    } else {
      console.error("Geolocation을 지원하지 않는 브라우저입니다.");
      userLat = 37.5665;
      userLng = 126.9780;
      initMap();
      loadRecruitList();
    }
  }

  // 카카오맵 초기화
  function initMap() {
    const mapContainer = document.getElementById("map");
    const mapOption = {
      center: new kakao.maps.LatLng(userLat, userLng),
      level: 5,
    };

    map = new kakao.maps.Map(mapContainer, mapOption);

    const imageSrc = "https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/markerStar.png";
    const imageSize = new kakao.maps.Size(24, 35);
    const imageOption = {offset: new kakao.maps.Point(12, 35)};
    const markerImage = new kakao.maps.MarkerImage(imageSrc, imageSize,
        imageOption);

    new kakao.maps.Marker({
      position: new kakao.maps.LatLng(userLat, userLng),
      map: map,
      image: markerImage,
      title: "내 위치"
    });
  }

  function clearMarkers() {
    markers.forEach((marker) => marker.setMap(null));
    infowindows.forEach((infowindow) => infowindow.close());
    markers = [];
    infowindows = [];
  }

  // [수정] 모집글 목록 로드 (토큰 로직 추가)
  async function loadRecruitList() {
    const loadingSpinner = document.getElementById("loadingSpinner");
    const recruitList = document.getElementById("recruitList");

    loadingSpinner.style.display = "flex";
    recruitList.innerHTML = "";

    try {
      const params = new URLSearchParams({
        latitude: userLat.toString(),
        longitude: userLng.toString(),
        radiusKm: currentRadius.toString(),
      });

      if (currentSortBy && currentSortBy !== "latest") {
        params.append("sortBy", currentSortBy);
      }
      if (currentKeyword) {
        params.append("keyword", currentKeyword);
      }

      // 1. 토큰 가져오기 (localStorage 또는 쿠키)
      const token = localStorage.getItem("accessToken") || getCookie(
          "accessToken");

      // 2. 헤더 설정
      const headers = {
        "Content-Type": "application/json"
      };

      if (token) {
        headers["Authorization"] = `Bearer ${token}`; // JWT 인증 헤더 추가
      }

      // 3. fetch 호출 시 headers 포함
      const response = await fetch(`/api/recruit?${params.toString()}`, {
        method: "GET",
        headers: headers
      });

      const result = await response.json();

      if (result.success && result.data) {
        // Slice 구조: result.data는 Slice 객체이므로 content 배열을 가져옴
        const recruits = result.data.content || [];
        if (recruits.length > 0) {
          displayRecruits(recruits);
          displayMarkers(recruits);
        } else {
          recruitList.innerHTML = "<p>모집글이 없습니다.</p>";
        }
      } else {
        recruitList.innerHTML = "<p>모집글이 없습니다.</p>";
      }
    } catch (error) {
      console.error("모집글 목록 로드 중 오류:", error);
      recruitList.innerHTML = "<p>오류가 발생했습니다.</p>";
    } finally {
      loadingSpinner.style.display = "none";
    }
  }

  // 모집글 카드 표시
  function displayRecruits(recruits) {
    const recruitList = document.getElementById("recruitList");
    if (recruits.length === 0) {
      recruitList.innerHTML = "<p>모집글이 없습니다.</p>";
      return;
    }

    // HTML 이스케이프 함수
    function escapeHtml(text) {
      const div = document.createElement("div");
      div.textContent = text;
      return div.innerHTML;
    }

    // 페이스 포맷팅
    function formatPace(pace) {
      if (!pace) return "-";
      if (pace === "2:00") {
        return "2:00/km 이하";
      } else if (pace === "9:00") {
        return "9:00/km 이상";
      }
      return `${pace}/km`;
    }

    // 날짜/시간 포맷팅 (간단하게)
    function formatMeetingDateTime(dateTimeString) {
      if (!dateTimeString) {
        return "";
      }
      const date = new Date(dateTimeString);
      const month = date.getMonth() + 1;
      const day = date.getDate();
      const hours = String(date.getHours()).padStart(2, "0");
      const minutes = String(date.getMinutes()).padStart(2, "0");
      const today = new Date();
      const isToday = date.getFullYear() === today.getFullYear() &&
          date.getMonth() === today.getMonth() &&
          date.getDate() === today.getDate();
      
      if (isToday) {
        return `오늘 ${hours}:${minutes}`;
      }
      return `${month}/${day} ${hours}:${minutes}`;
    }

    recruitList.innerHTML = recruits.map((recruit) => {
      const distanceText = recruit.distanceKm
          ? `${recruit.distanceKm.toFixed(1)}km`
          : "";
      const paceText = formatPace(recruit.targetPace);
      const meetingDateTime = formatMeetingDateTime(recruit.meetingAt);

      return `
          <div class="recruit-card" data-recruit-id="${recruit.recruitId}">
            <!-- 1행: 뱃지 -->
            <div class="card-badge-wrapper">
              <span class="card-badge recruiting">모집중</span>
            </div>
            
            <!-- 2행: 제목 -->
            <h3 class="card-title">${escapeHtml(recruit.title)}</h3>
            
            <!-- 3행: 거리 정보 -->
            ${distanceText ? `
            <div class="card-distance">
              <svg class="card-distance-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M21 10C21 17 12 23 12 23C12 23 3 17 3 10C3 7.61305 3.94821 5.32387 5.63604 3.63604C7.32387 1.94821 9.61305 1 12 1C14.3869 1 16.6761 1.94821 18.364 3.63604C20.0518 5.32387 21 7.61305 21 10Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M12 13C13.6569 13 15 11.6569 15 10C15 8.34315 13.6569 7 12 7C10.3431 7 9 8.34315 9 10C9 11.6569 10.3431 13 12 13Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span>내 위치에서 ${distanceText}</span>
            </div>
            ` : ""}
            
            <!-- 4행: 상세 스펙 (날짜/시간 | 페이스 | 인원) -->
            <div class="card-specs">
              <div class="card-spec-item">
                <svg class="card-spec-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M8 2V6M16 2V6M3 10H21M5 4H19C20.1046 4 21 4.89543 21 6V20C21 21.1046 20.1046 22 19 22H5C3.89543 22 3 21.1046 3 20V6C3 4.89543 3.89543 4 5 4Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span>${meetingDateTime}</span>
              </div>
              <span class="card-spec-divider">·</span>
              <div class="card-spec-item">
                <svg class="card-spec-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M13 2L3 14H12L11 22L21 10H12L13 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span>${paceText}</span>
              </div>
              <span class="card-spec-divider">·</span>
              <div class="card-spec-item">
                <svg class="card-spec-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M17 21V19C17 17.9391 16.5786 16.9217 15.8284 16.1716C15.0783 15.4214 14.0609 15 13 15H5C3.93913 15 2.92172 15.4214 2.17157 16.1716C1.42143 16.9217 1 17.9391 1 19V21M23 21V19C22.9993 18.1137 22.7044 17.2528 22.1614 16.5523C21.6184 15.8519 20.8581 15.3516 20 15.13M16 3.13C16.8604 3.35031 17.623 3.85071 18.1676 4.55232C18.7122 5.25392 19.0078 6.11683 19.0078 7.005C19.0078 7.89318 18.7122 8.75608 18.1676 9.45769C17.623 10.1593 16.8604 10.6597 16 10.88M13 7C13 9.20914 11.2091 11 9 11C6.79086 11 5 9.20914 5 7C5 4.79086 6.79086 3 9 3C11.2091 3 13 4.79086 13 7Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span>${recruit.currentParticipants || 1}/${recruit.maxParticipants}명</span>
              </div>
            </div>
          </div>
        `;
    }).join("");

    document.querySelectorAll(".recruit-card").forEach((card) => {
      card.addEventListener("click", () => {
        const recruitId = card.getAttribute("data-recruit-id");
        window.location.href = `/recruit/${recruitId}`;
      });
    });
  }

  // 마커 및 지도 범위 설정
  function displayMarkers(recruits) {
    clearMarkers();
    const bounds = new kakao.maps.LatLngBounds();
    if (userLat && userLng) {
      bounds.extend(
          new kakao.maps.LatLng(userLat, userLng));
    }

    let hasValidMarker = false;
    recruits.forEach((recruit) => {
      if (recruit.latitude && recruit.longitude) {
        const markerPosition = new kakao.maps.LatLng(
            parseFloat(recruit.latitude), parseFloat(recruit.longitude));
        const marker = new kakao.maps.Marker(
            {position: markerPosition, map: map});
        const infowindow = new kakao.maps.InfoWindow({
          content: `<div style="padding: 5px; font-size: 12px; color: #000;">${recruit.title}</div>`
        });

        markers.push(marker);
        infowindows.push(infowindow);
        kakao.maps.event.addListener(marker, "click", () => {
          infowindows.forEach((iw) => iw.close());
          infowindow.open(map, marker);
        });
        bounds.extend(markerPosition);
        hasValidMarker = true;
      }
    });
    if (hasValidMarker) {
      map.setBounds(bounds);
    }
  }

  // 유틸리티 함수
  function formatDateTime(dateTimeString) {
    if (!dateTimeString) {
      return "";
    }
    const date = new Date(dateTimeString);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2,
        "0")}-${String(date.getDate()).padStart(2, "0")} ${String(
        date.getHours()).padStart(2, "0")}시 ${String(
        date.getMinutes()).padStart(2, "0")}분`;
  }

  function getGenderLimitText(genderLimit) {
    const genderMap = {M: "남성", F: "여성", BOTH: "무관"};
    return genderMap[genderLimit] || genderLimit;
  }

  // [추가] 쿠키 가져오기 헬퍼
  function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
      return parts.pop().split(";").shift();
    }
    return null;
  }

  // 이벤트 리스너
  const backBtn = document.getElementById("backBtn");
  if (backBtn) {
    backBtn.addEventListener("click", () => {
      window.history.back();
    });
  }

  const radiusSelect = document.getElementById("radiusSelect");
  if (radiusSelect) {
    radiusSelect.addEventListener("change", (e) => {
      currentRadius = parseInt(e.target.value);
      loadRecruitList();
    });
  }

  const sortSelect = document.getElementById("sortSelect");
  if (sortSelect) {
    sortSelect.addEventListener("change", (e) => {
      currentSortBy = e.target.value;
      loadRecruitList();
    });
  }

  // 검색 입력창 이벤트 (Enter 키 및 실시간 검색)
  const keywordInput = document.getElementById("keywordInput");
  if (keywordInput) {
    // Enter 키로 검색
    keywordInput.addEventListener("keypress", (e) => {
      if (e.key === "Enter") {
        currentKeyword = keywordInput.value.trim();
        loadRecruitList();
      }
    });
    // 입력 시 실시간 검색 (선택사항 - 필요시 주석 해제)
    // keywordInput.addEventListener("input", (e) => {
    //   currentKeyword = e.target.value.trim();
    //   loadRecruitList();
    // });
  }

  const createBtn = document.getElementById("createBtn");
  if (createBtn) {
    createBtn.addEventListener("click", () => {
      window.location.href = "/recruit/create";
    });
  }

  getUserLocation();
});