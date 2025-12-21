document.addEventListener("DOMContentLoaded", () => {
  let map = null;
  let marker = null;
  let infowindow = null;

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
      MALE: "남성",
      FEMALE: "여성",
      ANY: "무관",
    };
    return genderMap[genderLimit] || genderLimit;
  }

  function getAgeRangeText(ageMin, ageMax) {
    if (!ageMin || !ageMax) {
      return "";
    }
    const ageMinDecade = Math.floor(ageMin / 10) * 10;
    const ageMaxDecade = Math.floor(ageMax / 10) * 10;

    if (ageMinDecade === ageMaxDecade) {
      return `${ageMinDecade}대`;
    } else {
      return `${ageMinDecade}대~${ageMaxDecade}대`;
    }
  }

  function initMap(latitude, longitude, meetingPlace) {
    const mapContainer = document.getElementById("map");
    const mapOption = {
      center: new kakao.maps.LatLng(latitude, longitude),
      level: 3,
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
      document.getElementById(
          "targetPace").textContent = `${recruitData.targetPace}/km`;
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

    document.getElementById("recruitContent").textContent = recruitData.content
        || "";
  }

  function renderActionButton(recruitData) {
    const actionButton = document.getElementById("actionButton");
    const actionDisabledText = document.getElementById("actionDisabledText");

    const isAuthor = recruitData.isAuthor || false;
    const isParticipant = recruitData.isParticipant || false;
    const currentParticipants = recruitData.currentParticipants || 1;

    actionButton.style.display = "none";
    actionDisabledText.style.display = "none";

    if (isAuthor && currentParticipants === 1) {
      actionButton.textContent = "수정하기";
      actionButton.style.display = "block";
      actionButton.onclick = () => {
        window.location.href = `/recruit/${recruitId}/update`;
      };
      return;
    }

    if (!isAuthor && isParticipant) {
      actionButton.textContent = "참가 취소";
      actionButton.style.display = "block";
      actionButton.onclick = () => {
        leaveRecruit();
      };
      return;
    }

    if (!isAuthor && !isParticipant) {
      actionButton.textContent = "참가하기";
      actionButton.style.display = "block";
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

          if (response.ok) {
            alert("참여가 완료되었습니다.");
            location.reload();
          } else {
            const result = await response.json();
            alert(result.message || "참여 처리 중 오류가 발생했습니다.");
          }
        } catch (error) {
          console.error("참여 처리 중 오류:", error);
          alert("참여 처리 중 오류가 발생했습니다.");
        }
      };
      return;
    }

    if (isAuthor && currentParticipants > 1) {
      actionDisabledText.textContent = "수정 불가";
      actionDisabledText.style.display = "block";
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

  // Cookie에서 토큰 가져오기
  function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
      return parts.pop().split(";").shift();
    }
    return null;
  }

  async function loadRecruitDetail() {
    try {
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

      const recruitData = result.data;
      console.log("API 응답 데이터:", recruitData);

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
