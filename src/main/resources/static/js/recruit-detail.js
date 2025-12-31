document.addEventListener("DOMContentLoaded", () => {
  let map = null;
  let marker = null;
  let infowindow = null;
  let currentUserGender = null;
  let currentUserAge = null;

  const urlParams = new URLSearchParams(window.location.search);
  let recruitId = urlParams.get("recruitId");

  if (!recruitId) {
    const pathParts = window.location.pathname.split("/");
    recruitId = pathParts[pathParts.length - 1];
  }

  if (!recruitId || recruitId === "recruit" || recruitId === "detail") {
    console.error("recruitId를 찾을 수 없습니다.");
    // showToast는 아직 정의되지 않았으므로 alert 유지
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
      // 참가하기 버튼 - 항상 활성화 상태로 표시
      actionButton.textContent = "참가하기";
      actionButton.style.display = "block";
      actionButton.disabled = false;
      actionButton.style.opacity = "1";
      actionButton.style.cursor = "pointer";
      
      // data- 속성으로 조건 정보 저장
      if (recruitData.ageMin !== null && recruitData.ageMin !== undefined) {
        actionButton.setAttribute("data-age-min", recruitData.ageMin);
      }
      if (recruitData.ageMax !== null && recruitData.ageMax !== undefined) {
        actionButton.setAttribute("data-age-max", recruitData.ageMax);
      }
      if (recruitData.genderLimit) {
        actionButton.setAttribute("data-gender-limit", recruitData.genderLimit);
      }
      
      // 클릭 이벤트 - 조건 검사 후 토스트 표시
      actionButton.onclick = async () => {
        // Step 1: 프론트엔드 유효성 검사
        const ageMin = actionButton.getAttribute("data-age-min");
        const ageMax = actionButton.getAttribute("data-age-max");
        const genderLimit = actionButton.getAttribute("data-gender-limit");
        
        // 로그인 체크
        const token = localStorage.getItem("accessToken");
        if (!token) {
          showToast("로그인이 필요합니다.", "error");
          setTimeout(() => {
            window.location.href = "/login";
          }, 1500);
          return;
        }
        
        // 성별 조건 체크
        if (genderLimit && genderLimit !== "BOTH" && currentUserGender) {
          if (genderLimit === "M" && currentUserGender !== "M") {
            showToast("성별 조건이 맞지 않아 참여할 수 없습니다.", "error");
            return;
          }
          if (genderLimit === "F" && currentUserGender !== "F") {
            showToast("성별 조건이 맞지 않아 참여할 수 없습니다.", "error");
            return;
          }
        }
        
        // 나이 조건 체크
        if (ageMin && ageMax && currentUserAge !== null) {
          const ageMinNum = parseInt(ageMin, 10);
          const ageMaxNum = parseInt(ageMax, 10);
          if (currentUserAge < ageMinNum || currentUserAge > ageMaxNum) {
            showToast("참여 가능한 나이대가 아닙니다.", "error");
            return;
          }
        }
        
        // Step 2: API 호출
        try {
          const headers = {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`,
          };

          const response = await fetch(`/api/recruit/${recruitId}/join`, {
            method: "POST",
            headers: headers,
          });

          const result = await response.json();

          // Step 3: API 에러 핸들링 (토스트로 처리)
          if (response.ok && result.success) {
            showToast("참여가 완료되었습니다.", "success");
            setTimeout(() => {
              location.reload();
            }, 1500);
          } else {
            const errorMessage = result.message || "참여 처리 중 오류가 발생했습니다.";
            showToast(errorMessage, "error");
          }
        } catch (error) {
          console.error("참여 처리 중 오류:", error);
          showToast("참여 처리 중 오류가 발생했습니다.", "error");
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
        showToast("로그인이 필요합니다.", "error");
        setTimeout(() => {
          window.location.href = "/login";
        }, 1500);
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
        showToast("참여가 취소되었습니다.", "success");
        setTimeout(() => {
          location.reload();
        }, 1500);
      } else {
        showToast(result.message || "참여 취소 처리 중 오류가 발생했습니다.", "error");
      }
    } catch (error) {
      console.error("참여 취소 처리 중 오류:", error);
      showToast("참여 취소 처리 중 오류가 발생했습니다.", "error");
    }
  }

  // 모집글 삭제
  async function deleteRecruit() {
    try {
      const token = localStorage.getItem("accessToken") || getCookie("accessToken");
      if (!token) {
        showToast("로그인이 필요합니다.", "error");
        setTimeout(() => {
          window.location.href = "/login";
        }, 1500);
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
        showToast("모집글이 삭제되었습니다.", "success");
        setTimeout(() => {
          window.location.href = "/recruit";
        }, 1500);
      } else {
        showToast(result.message || "모집글 삭제 중 오류가 발생했습니다.", "error");
      }
    } catch (error) {
      console.error("모집글 삭제 중 오류:", error);
      showToast("모집글 삭제 중 오류가 발생했습니다.", "error");
    }
  }

  // 매칭 확정 (러닝 시작하기)
  async function confirmMatch() {
    try {
      const token = localStorage.getItem("accessToken") || getCookie("accessToken");
      if (!token) {
        showToast("로그인이 필요합니다.", "error");
        setTimeout(() => {
          window.location.href = "/login";
        }, 1500);
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
        showToast("매칭이 확정되었습니다", "success");
        setTimeout(() => {
          window.location.href = "/recruit";
        }, 1500);
      } else {
        showToast(result.message || "매칭 확정 처리 중 오류가 발생했습니다.", "error");
      }
    } catch (error) {
      console.error("매칭 확정 처리 중 오류:", error);
      showToast("매칭 확정 처리 중 오류가 발생했습니다.", "error");
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

  // Toast 알림 표시
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

  // 생년월일로 나이 계산
  function calculateAge(birthDate) {
    if (!birthDate) return null;
    const birth = new Date(birthDate);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return age;
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
          // 나이 계산
          if (result.data.birthDate) {
            currentUserAge = calculateAge(result.data.birthDate);
          }
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
      showToast("모집글을 불러오는 중 오류가 발생했습니다.", "error");
    }
  }

  document.getElementById("backButton").addEventListener("click", () => {
    window.location.href = "/recruit";
  });

  loadRecruitDetail();
});
