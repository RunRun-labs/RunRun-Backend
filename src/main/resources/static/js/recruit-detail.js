document.addEventListener("DOMContentLoaded", () => {
  let map = null;
  let marker = null;
  let infowindow = null;
  let currentUserGender = null;

  const urlParams = new URLSearchParams(window.location.search);
  let recruitId = urlParams.get("recruitId");

  if (!recruitId) {
    const pathParts = window.location.pathname.split("/");
    recruitId = pathParts[pathParts.length - 1];
  }

  if (!recruitId || recruitId === "recruit" || recruitId === "detail") {
    console.error("recruitId를 찾을 수 없습니다.");
    alert("모집글 ID를 찾을 수 없습니다.");
    window.location.href = "/recruit";
    return;
  }

  function formatDateTime(dateTimeString) {
    if (!dateTimeString) {
      return "";
    }
    const date = new Date(dateTimeString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${year}년 ${month}월 ${day}일 ${hours}시 ${minutes}분`;
  }

  function getGenderLimitText(genderLimit) {
    const genderMap = {
      M: "남성",
      F: "여성",
      BOTH: "무관",
    };
    return genderMap[genderLimit] || genderLimit;
  }

  function getAgeRangeText(ageMin, ageMax) {
    if (ageMin === null || ageMin === undefined || ageMax === null || ageMax === undefined) {
      return "";
    }
    // ageMin과 ageMax를 직접 사용하여 "0세~100세" 형식으로 표시
    return `${ageMin}세~${ageMax}세`;
  }

  function formatPace(pace) {
    if (!pace) return "-";
    // 2:00/km 이하 또는 9:00/km 이상일 때 "이하", "이상" 붙이기
    const paceMatch = pace.match(/^(\d{1,2}):(\d{2})$/);
    if (paceMatch) {
      const minutes = parseInt(paceMatch[1], 10);
      const seconds = parseInt(paceMatch[2], 10);
      
      if (minutes === 2 && seconds === 0) {
        return `${pace}/km 이하`;
      } else if (minutes === 9 && seconds === 0) {
        return `${pace}/km 이상`;
      }
    }
    return `${pace}/km`;
  }

  function formatDate(dateTimeString) {
    if (!dateTimeString) {
      return "";
    }
    const date = new Date(dateTimeString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }

  function initMap(latitude, longitude, meetingPlace) {
    const mapContainer = document.getElementById("map");
    const mapOption = {
      center: new kakao.maps.LatLng(latitude, longitude),
      level: 4, // level 4는 약 500m
    };

    map = new kakao.maps.Map(mapContainer, mapOption);

    const markerPosition = new kakao.maps.LatLng(latitude, longitude);
    marker = new kakao.maps.Marker({
      position: markerPosition,
    });
    marker.setMap(map);

    const iwContent = `<div style="padding:8px 12px;font-size:14px;font-weight:600;color:#1a1c1e;white-space:nowrap;">${meetingPlace}</div>`;
    infowindow = new kakao.maps.InfoWindow({
      content: iwContent,
    });
    infowindow.open(map, marker);
  }

  function displayRecruitDetail(recruitData) {
    // 제목
    document.getElementById("recruitTitle").textContent = recruitData.title
        || "";

    if (recruitData.courseId && recruitData.courseImageUrl) {
      const courseImageSection = document.getElementById("courseImageSection");
      const courseImage = document.getElementById("courseImage");

      let imageUrl = recruitData.courseImageUrl;
      if (!imageUrl.startsWith("http")) {
        imageUrl = imageUrl.startsWith("/") ? imageUrl : `/files/${imageUrl}`;
      }

      courseImage.src = imageUrl;
      courseImageSection.style.display = "block";
    } else {
      document.getElementById("courseImageSection").style.display = "none";
    }

    if (recruitData.latitude && recruitData.longitude) {
      initMap(
          recruitData.latitude,
          recruitData.longitude,
          recruitData.meetingPlace || "출발지"
      );
    }

    document.getElementById("meetingAt").textContent = formatDateTime(
        recruitData.meetingAt);

    const targetDistance = recruitData.targetDistance || 0;
    document.getElementById(
        "targetDistance").textContent = `${targetDistance}km`;

    if (recruitData.targetPace) {
      document.getElementById("targetPace").textContent = formatPace(
          recruitData.targetPace);
    } else {
      document.getElementById("targetPace").textContent = "-";
    }

    if (recruitData.currentParticipants !== undefined
        && recruitData.maxParticipants) {
      document.getElementById(
          "participants").textContent = `${recruitData.currentParticipants}명 / ${recruitData.maxParticipants}명`;
    } else {
      document.getElementById("participants").textContent = "-";
    }

    if (recruitData.genderLimit) {
      document.getElementById("genderLimit").textContent = getGenderLimitText(
          recruitData.genderLimit);
    } else {
      document.getElementById("genderLimit").textContent = "-";
    }

    if (recruitData.ageMin !== null && recruitData.ageMax !== null) {
      document.getElementById("ageRange").textContent = getAgeRangeText(
          recruitData.ageMin, recruitData.ageMax);
    } else {
      document.getElementById("ageRange").textContent = "-";
    }

    // 작성자
    if (recruitData.authorLoginId) {
      document.getElementById("authorLoginId").textContent = recruitData.authorLoginId;
    } else {
      document.getElementById("authorLoginId").textContent = "-";
    }

    // 작성일자
    if (recruitData.createdAt) {
      document.getElementById("createdAt").textContent = formatDate(recruitData.createdAt);
    } else {
      document.getElementById("createdAt").textContent = "-";
    }

    document.getElementById("recruitContent").textContent = recruitData.content
        || "";
  }

  function renderActionButton(recruitData) {
    const actionButton = document.getElementById("actionButton");
    const actionDisabledText = document.getElementById("actionDisabledText");
    const startRunningButton = document.getElementById("startRunningButton");

    const isAuthor = recruitData.isAuthor || false;
    const isParticipant = recruitData.isParticipant || false;
    const currentParticipants = recruitData.currentParticipants || 1;

    actionButton.style.display = "none";
    actionDisabledText.style.display = "none";
    startRunningButton.style.display = "none";

    // 방장인 경우 러닝 시작하기 버튼 표시
    if (isAuthor && currentParticipants > 1) {
      startRunningButton.style.display = "block";
      startRunningButton.onclick = () => {
        confirmMatch();
      };
      return;
    }

    if (isAuthor && currentParticipants === 1) {
      // 수정하기와 삭제하기 버튼 표시
      const editDeleteButtons = document.getElementById("editDeleteButtons");
      editDeleteButtons.style.display = "flex";
      
      const editButton = document.getElementById("editButton");
      const deleteButton = document.getElementById("deleteButton");
      
      editButton.onclick = () => {
        window.location.href = `/recruit/${recruitId}/update`;
      };
      
      deleteButton.onclick = () => {
        if (confirm("정말로 이 모집글을 삭제하시겠습니까?")) {
          deleteRecruit();
        }
      };
      return;
    }

    if (!isAuthor && isParticipant) {
      // 매칭 확정(MATCHED)된 모집글에서는 참가 취소 버튼 표시하지 않음
      const status = recruitData.status;
      if (status && (status === "MATCHED" || status === "COMPLETED")) {
        // 매칭 확정 또는 완료된 경우 버튼 표시하지 않음
        return;
      }
      
      actionButton.textContent = "참가 취소";
      actionButton.style.display = "block";
      actionButton.onclick = () => {
        leaveRecruit();
      };
      return;
    }

    if (!isAuthor && !isParticipant) {
      // 성별 제한 체크
      const genderLimit = recruitData.genderLimit;
      if (currentUserGender && genderLimit) {
        // genderLimit이 "M"이면 여성(F) 사용자는 참가 불가
        // genderLimit이 "F"이면 남성(M) 사용자는 참가 불가
        // genderLimit이 "BOTH"이면 모든 사용자 참가 가능
        if (genderLimit === "M" && currentUserGender !== "M") {
          actionButton.textContent = "참가 불가";
          actionButton.style.display = "block";
          actionButton.disabled = true;
          actionButton.style.opacity = "0.5";
          actionButton.style.cursor = "not-allowed";
          actionButton.onclick = () => {
            alert("남성만 참가할 수 있는 모집글입니다.");
          };
          return;
        }
        if (genderLimit === "F" && currentUserGender !== "F") {
          actionButton.textContent = "참가 불가";
          actionButton.style.display = "block";
          actionButton.disabled = true;
          actionButton.style.opacity = "0.5";
          actionButton.style.cursor = "not-allowed";
          actionButton.onclick = () => {
            alert("여성만 참가할 수 있는 모집글입니다.");
          };
          return;
        }
      }
      
      actionButton.textContent = "참가하기";
      actionButton.style.display = "block";
      actionButton.disabled = false;
      actionButton.style.opacity = "1";
      actionButton.style.cursor = "pointer";
      actionButton.onclick = async () => {
        try {
          const token = localStorage.getItem("accessToken");
          if (!token) {
            alert("로그인이 필요합니다.");
            window.location.href = "/login";
            return;
          }

          const headers = {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`,
          };

          const response = await fetch(`/api/recruit/${recruitId}/join`, {
            method: "POST",
            headers: headers,
          });

          const result = await response.json();

          if (response.ok && result.success) {
            alert("참여가 완료되었습니다.");
            location.reload();
          } else {
            alert(result.message || "참여 처리 중 오류가 발생했습니다.");
          }
        } catch (error) {
          console.error("참여 처리 중 오류:", error);
          alert("참여 처리 중 오류가 발생했습니다.");
        }
      };
      return;
    }
  }

  // 참여 취소 함수
  async function leaveRecruit() {
    try {
      const token = localStorage.getItem("accessToken") || getCookie(
          "accessToken");
      if (!token) {
        alert("로그인이 필요합니다.");
        window.location.href = "/login";
        return;
      }

      const headers = {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`,
      };

      const response = await fetch(`/api/recruit/${recruitId}/join`, {
        method: "DELETE",
        headers: headers,
      });

      const result = await response.json();

      if (response.ok && result.success) {
        alert("참여가 취소되었습니다.");
        location.reload();
      } else {
        alert(result.message || "참여 취소 처리 중 오류가 발생했습니다.");
      }
    } catch (error) {
      console.error("참여 취소 처리 중 오류:", error);
      alert("참여 취소 처리 중 오류가 발생했습니다.");
    }
  }

  // 모집글 삭제
  async function deleteRecruit() {
    try {
      const token = localStorage.getItem("accessToken") || getCookie("accessToken");
      if (!token) {
        alert("로그인이 필요합니다.");
        window.location.href = "/login";
        return;
      }

      const headers = {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`,
      };

      const response = await fetch(`/api/recruit/${recruitId}`, {
        method: "DELETE",
        headers: headers,
      });

      const result = await response.json();

      if (response.ok && result.success) {
        alert("모집글이 삭제되었습니다.");
        window.location.href = "/recruit";
      } else {
        alert(result.message || "모집글 삭제 중 오류가 발생했습니다.");
      }
    } catch (error) {
      console.error("모집글 삭제 중 오류:", error);
      alert("모집글 삭제 중 오류가 발생했습니다.");
    }
  }

  // 매칭 확정 (러닝 시작하기)
  async function confirmMatch() {
    try {
      const token = localStorage.getItem("accessToken") || getCookie("accessToken");
      if (!token) {
        alert("로그인이 필요합니다.");
        window.location.href = "/login";
        return;
      }

      const headers = {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`,
      };

      const requestBody = {
        recruitId: parseInt(recruitId, 10),
      };

      const response = await fetch("/api/match/confirm", {
        method: "POST",
        headers: headers,
        body: JSON.stringify(requestBody),
      });

      const result = await response.json();

      if (response.ok && result.success) {
        alert("매칭이 확정되었습니다");
        window.location.href = "/recruit";
      } else {
        alert(result.message || "매칭 확정 처리 중 오류가 발생했습니다.");
      }
    } catch (error) {
      console.error("매칭 확정 처리 중 오류:", error);
      alert("매칭 확정 처리 중 오류가 발생했습니다.");
    }
  }

  // Cookie에서 토큰 가져오기
  function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
      return parts.pop().split(";").shift();
    }
    return null;
  }

  // 현재 사용자 정보 가져오기
  async function loadCurrentUser() {
    try {
      const token = localStorage.getItem("accessToken");
      if (!token) {
        return; // 로그인하지 않은 경우 null 유지
      }

      const headers = {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`,
      };

      const response = await fetch("/users", {
        method: "GET",
        headers: headers,
      });

      if (response.ok) {
        const result = await response.json();
        if (result.success && result.data) {
          currentUserGender = result.data.gender;
        }
      }
    } catch (error) {
      console.error("사용자 정보 로드 중 오류:", error);
    }
  }

  async function loadRecruitDetail() {
    try {
      // 먼저 사용자 정보 로드
      await loadCurrentUser();
      
      const token = localStorage.getItem("accessToken");
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

      if (!response.ok || !result.success) {
        throw new Error(result.message || "모집글을 불러올 수 없습니다.");
      }

      // ApiResponse 구조: { success: true, data: RecruitDetailResDto }
      const recruitData = result.data;
      console.log("API 응답 데이터:", recruitData);

      if (!recruitData) {
        throw new Error("모집글 데이터가 없습니다.");
      }

      displayRecruitDetail(recruitData);
      renderActionButton(recruitData);
    } catch (error) {
      console.error("모집글 상세 정보 로드 중 오류:", error);
      alert("모집글을 불러오는 중 오류가 발생했습니다.");
    }
  }

  document.getElementById("backButton").addEventListener("click", () => {
    window.location.href = "/recruit";
  });

  loadRecruitDetail();
});
