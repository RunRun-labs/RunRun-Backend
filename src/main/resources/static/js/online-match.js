document.addEventListener("DOMContentLoaded", () => {
  const backButton = document.querySelector(".back-button");
  const optionButtons = document.querySelectorAll(".option-card");
  const startButton = document.getElementById("startButton");
  const matchingOverlay = document.getElementById("matchingOverlay");
  const cancelButton = document.getElementById("cancelButton");
  const userProfileImage = document.getElementById("userProfileImage");
  const statusTitle = document.getElementById("statusTitle");
  const statusSubtitle = document.getElementById("statusSubtitle");
  const matchFoundText = document.getElementById("matchFoundText");
  const opponentProfiles = document.getElementById("opponentProfiles");
  const connectionLines = document.getElementById("connectionLines");

  let selectedDistance = null;
  let selectedParticipant = null;
  let pollingInterval = null;
  let isMatching = false;
  let userProfileUrl = null;
  let matchFoundTimeout = null;
  let countdownInterval = null;
  const matchFoundSection = document.getElementById("matchFoundSection");
  const countdownText = document.getElementById("countdownText");

  // 뒤로가기 버튼
  if (backButton) {
    backButton.addEventListener("click", () => {
      if (isMatching) {
        handleCancel();
      } else {
        window.history.length > 1
            ? window.history.back()
            : (window.location.href = "/match");
      }
    });
  }

  // 옵션 버튼 선택 처리
  optionButtons.forEach((button) => {
    button.addEventListener("click", () => {
      const type = button.getAttribute("data-type");
      const value = button.getAttribute("data-value");

      // 같은 타입의 다른 버튼들 비활성화
      document
      .querySelectorAll(`.option-card[data-type="${type}"]`)
      .forEach((btn) => btn.classList.remove("active"));

      // 선택한 버튼 활성화
      button.classList.add("active");

      // 선택 값 저장
      if (type === "distance") {
        selectedDistance = value;
      } else if (type === "participant") {
        selectedParticipant = parseInt(value, 10);
      }

      // 시작 버튼 활성화 확인
      updateStartButton();
    });
  });

  // 시작 버튼 활성화 상태 업데이트
  function updateStartButton() {
    if (selectedDistance && selectedParticipant) {
      startButton.disabled = false;
    } else {
      startButton.disabled = true;
    }
  }

  // 매칭 시작
  if (startButton) {
    startButton.addEventListener("click", async () => {
      if (startButton.disabled || isMatching) {
        return;
      }

      await handleMatchStart();
    });
  }

  // 유저 프로필 이미지 로드
  async function loadUserProfile() {
    try {
      const token = localStorage.getItem("accessToken");

      const headers = {
        "Content-Type": "application/json",
      };

      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }

      const response = await fetch("/users", {
        method: "GET",
        headers: headers,
      });

      const result = await response.json();

      if (response.ok && result?.success && result?.data) {
        userProfileUrl = result.data.profileImageUrl;
        if (userProfileImage) {
          if (userProfileUrl) {
            userProfileImage.src = userProfileUrl;
          } else {
            // 기본 프로필 이미지 (SVG 아이콘)
            userProfileImage.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100' height='100' viewBox='0 0 100 100'%3E%3Ccircle cx='50' cy='50' r='50' fill='%231a1f2e'/%3E%3Ccircle cx='50' cy='35' r='15' fill='%2300ffc8'/%3E%3Cpath d='M20 80 Q20 60 50 60 Q80 60 80 80' fill='%2300ffc8'/%3E%3C/svg%3E";
          }
          userProfileImage.onerror = () => {
            // 이미지 로드 실패 시 기본 이미지
            userProfileImage.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100' height='100' viewBox='0 0 100 100'%3E%3Ccircle cx='50' cy='50' r='50' fill='%231a1f2e'/%3E%3Ccircle cx='50' cy='35' r='15' fill='%2300ffc8'/%3E%3Cpath d='M20 80 Q20 60 50 60 Q80 60 80 80' fill='%2300ffc8'/%3E%3C/svg%3E";
          };
        }
      }
    } catch (error) {
      console.error("프로필 로드 오류:", error);
      // 기본 프로필 이미지 설정
      if (userProfileImage) {
        userProfileImage.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100' height='100' viewBox='0 0 100 100'%3E%3Ccircle cx='50' cy='50' r='50' fill='%231a1f2e'/%3E%3Ccircle cx='50' cy='35' r='15' fill='%2300ffc8'/%3E%3Cpath d='M20 80 Q20 60 50 60 Q80 60 80 80' fill='%2300ffc8'/%3E%3C/svg%3E";
      }
    }
  }

  // 페이지 로드 시 프로필 이미지 로드
  loadUserProfile();

  // 매칭 시작 처리
  async function handleMatchStart() {
    isMatching = true;
    startButton.disabled = true;

    try {
      const token = localStorage.getItem("accessToken");

      const headers = {
        "Content-Type": "application/json",
      };

      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }

      // 매칭 신청 API 호출
      const response = await fetch("/api/match/online/join", {
        method: "POST",
        headers: headers,
        body: JSON.stringify({
          distance: selectedDistance,
          participantCount: selectedParticipant,
        }),
      });

      const result = await response.json();

      if (!response.ok || !result?.success) {
        throw new Error(result?.message || "매칭 신청에 실패했습니다.");
      }

      // 응답에 sessionId가 있으면 이미 참여 중인 방이 있음 - 안내 화면 표시 후 리다이렉트
      // 백엔드가 이미 매칭된 경우 result.data에 Long 타입의 SessionId를 바로 담아서 보냄 (객체가 아님)
      if (result?.data) {
        const sessionId = result.data;

        // 1. matchingOverlay를 display: flex로 보여준다
        matchingOverlay.style.display = "flex";

        // 2. resetMatchUI()를 호출해서 초기화한다
        resetMatchUI();

        // 3. statusTitle의 텍스트를 "참여 중인 매칭이 존재합니다"로 변경한다
        if (statusTitle) {
          statusTitle.textContent = "참여 중인 매칭이 존재합니다";
        }

        // 4. statusSubtitle의 텍스트를 "기존 배틀방으로 이동합니다..."로 변경한다
        if (statusSubtitle) {
          statusSubtitle.textContent = "기존 배틀방으로 이동합니다...";
          statusSubtitle.style.display = "block";
        }

        // 5. cancelButton은 숨긴다 (display: none)
        if (cancelButton) {
          cancelButton.style.display = "none";
        }

        // 6. 로딩 애니메이션이나 프로필 등 불필요한 요소는 가려준다
        const radarContainer = document.querySelector(".radar-container");
        if (radarContainer) {
          radarContainer.style.display = "none";
        }
        if (opponentProfiles) {
          opponentProfiles.style.display = "none";
        }
        if (connectionLines) {
          connectionLines.style.display = "none";
        }

        // 7. 약 2초(setTimeout) 뒤에 리다이렉트한다
        setTimeout(() => {
          window.location.href = `/match/online/confirmed?sessionId=${sessionId}`;
        }, 2000);

        return;
      }

      // sessionId가 없으면 대기 화면 표시 및 폴링 시작
      matchingOverlay.style.display = "flex";
      resetMatchUI();
      startPolling();
    } catch (error) {
      console.error("매칭 시작 오류:", error);
      alert(error.message || "매칭 시작 중 오류가 발생했습니다.");
      hideMatchingOverlay();
    }
  }

  // 매칭 UI 초기화
  function resetMatchUI() {
    if (statusTitle) {
      statusTitle.textContent = "SEARCHING FOR PLAYERS...";
    }
    if (statusSubtitle) {
      statusSubtitle.textContent = "Finding users with similar skill and latency.";
    }
    if (matchFoundSection) {
      matchFoundSection.style.display = "none";
    }
    if (countdownText) {
      countdownText.textContent = "";
    }
    if (opponentProfiles) {
      opponentProfiles.innerHTML = "";
    }
    if (connectionLines) {
      connectionLines.style.display = "none";
    }
    if (cancelButton) {
      cancelButton.style.display = "block";
    }

    // 기존 타임아웃 정리
    if (matchFoundTimeout) {
      clearTimeout(matchFoundTimeout);
      matchFoundTimeout = null;
    }
    if (countdownInterval) {
      clearInterval(countdownInterval);
      countdownInterval = null;
    }
  }

  // 상태 폴링
  function startPolling() {
    // 즉시 한 번 호출
    checkMatchStatus();

  }

  // 매칭 상태 확인
  async function checkMatchStatus() {
    try {
      const token = localStorage.getItem("accessToken");

      const headers = {
        "Content-Type": "application/json",
      };

      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }

      const response = await fetch("/api/match/online/status", {
        method: "GET",
        headers: headers,
      });

      const result = await response.json();

      if (!response.ok || !result?.success) {
        throw new Error(result?.message || "상태 확인에 실패했습니다.");
      }

      const status = result?.data?.status;

      if (status === "MATCHED") {
        // 매칭 성공 - MATCH FOUND 연출
        // 중요: MATCHED 응답을 받으면 즉시 폴링 중단 (다시 호출하면 NONE이 올 수 있음)
        stopPolling();
        showMatchFound(result?.data);
        return; // 함수 종료하여 추가 폴링 방지
      } else if (status === "WAITING") {
        // 대기 중 - 다음 폴링 예약
        pollingInterval = setTimeout(() => checkMatchStatus(), 1000);
        return;
      } else if (status === "NONE" || !status) {
        // 매칭 없음 또는 에러
        stopPolling();
        hideMatchingOverlay();
        alert("매칭이 취소되었거나 찾을 수 없습니다.");
      }
    } catch (error) {
      console.error("상태 확인 오류:", error);
      stopPolling();
      hideMatchingOverlay();
      alert(error.message || "상태 확인 중 오류가 발생했습니다.");
    }
  }

  // 폴링 중지
  function stopPolling() {
    if (pollingInterval) {
      clearTimeout(pollingInterval);
      pollingInterval = null;
    }
    isMatching = false;
  }

  // MATCH FOUND 연출
  function showMatchFound(matchData) {
    const sessionId = matchData?.sessionId;

    // 상태 텍스트 변경
    if (statusTitle) {
      statusTitle.textContent = "";
    }
    if (statusSubtitle) {
      statusSubtitle.style.display = "none";
    }
    if (matchFoundSection) {
      matchFoundSection.style.display = "block";
    }
    if (cancelButton) {
      cancelButton.style.display = "none";
    }

    // API 응답에서 상대방 정보 가져오기 (participants 배열이 있다고 가정)
    const participants = matchData?.participants || [];
    const opponentCount = participants.length > 0 ? participants.length - 1
        : (selectedParticipant || 2) - 1;

    // 상대방 프로필 생성
    if (opponentProfiles) {
      opponentProfiles.innerHTML = "";

      // 각도 계산 (원형 배치) - 시작 각도를 -90도로 설정하여 상단부터 시작
      const angleStep = (360 / opponentCount) * (Math.PI / 180);
      const startAngle = -90 * (Math.PI / 180); // 상단부터 시작
      // 반경을 최대한 크게 설정 (radar-container 크기의 약 85% 정도로 최외곽에 배치)
      const radius = 255; // 중심으로부터의 거리 (픽셀 단위) - 최외곽에 배치
      const containerSize = 300; // radar-container의 대략적인 크기 (픽셀)

      let opponentIndex = 0;
      for (let i = 0; i < participants.length; i++) {
        const participant = participants[i];
        // 본인은 제외 (userId로 비교하거나, 첫 번째가 본인이라고 가정)
        if (i === 0) {
          continue;
        } // 첫 번째가 본인이라고 가정

        const angle = startAngle + angleStep * opponentIndex;
        // 퍼센트로 변환 (radar-container 기준)
        const x = 50 + (radius / containerSize) * 50 * Math.cos(angle);
        const y = 50 + (radius / containerSize) * 50 * Math.sin(angle);

        const opponentDiv = document.createElement("div");
        opponentDiv.className = "opponent-profile";
        opponentDiv.style.left = `${x}%`;
        opponentDiv.style.top = `${y}%`;
        opponentDiv.style.animationDelay = `${opponentIndex * 0.15}s`;
        opponentDiv.dataset.initialX = x;
        opponentDiv.dataset.initialY = y;

        const img = document.createElement("img");
        if (participant?.profileImageUrl) {
          img.src = participant.profileImageUrl;
        } else {
          img.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='60' height='60' viewBox='0 0 60 60'%3E%3Ccircle cx='30' cy='30' r='30' fill='%231a1f2e'/%3E%3Ccircle cx='30' cy='20' r='10' fill='%23baff29'/%3E%3Cpath d='M10 50 Q10 40 30 40 Q50 40 50 50' fill='%23baff29'/%3E%3C/svg%3E";
        }
        img.alt = participant?.name || "상대방";
        img.onerror = () => {
          img.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='60' height='60' viewBox='0 0 60 60'%3E%3Ccircle cx='30' cy='30' r='30' fill='%231a1f2e'/%3E%3Ccircle cx='30' cy='20' r='10' fill='%23baff29'/%3E%3Cpath d='M10 50 Q10 40 30 40 Q50 40 50 50' fill='%23baff29'/%3E%3C/svg%3E";
        };

        const label = document.createElement("div");
        label.className = "opponent-label";
        const tier = participant?.tier || "토끼"; // 기본 티어
        label.innerHTML = `<span class="opponent-name">${participant?.name
        || `Player ${opponentIndex
        + 1}`}</span><span class="opponent-tier">${tier}</span>`;

        opponentDiv.appendChild(img);
        opponentDiv.appendChild(label);
        opponentProfiles.appendChild(opponentDiv);

        opponentIndex++;
      }

      // 상대방이 없으면 기본 프로필 생성
      if (opponentIndex === 0) {
        for (let i = 0; i < opponentCount; i++) {
          const angle = startAngle + angleStep * i;
          const x = 50 + (radius / containerSize) * 50 * Math.cos(angle);
          const y = 50 + (radius / containerSize) * 50 * Math.sin(angle);

          const opponentDiv = document.createElement("div");
          opponentDiv.className = "opponent-profile";
          opponentDiv.style.left = `${x}%`;
          opponentDiv.style.top = `${y}%`;
          opponentDiv.style.animationDelay = `${i * 0.15}s`;
          opponentDiv.dataset.initialX = x;
          opponentDiv.dataset.initialY = y;

          const img = document.createElement("img");
          img.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='60' height='60' viewBox='0 0 60 60'%3E%3Ccircle cx='30' cy='30' r='30' fill='%231a1f2e'/%3E%3Ccircle cx='30' cy='20' r='10' fill='%23baff29'/%3E%3Cpath d='M10 50 Q10 40 30 40 Q50 40 50 50' fill='%23baff29'/%3E%3C/svg%3E";
          img.alt = "상대방";

          const label = document.createElement("div");
          label.className = "opponent-label";
          label.innerHTML = `<span class="opponent-name">Player ${i
          + 1}</span><span class="opponent-tier">토끼</span>`;

          opponentDiv.appendChild(img);
          opponentDiv.appendChild(label);
          opponentProfiles.appendChild(opponentDiv);
        }
      }
    }

    // 연결선 표시
    if (connectionLines) {
      connectionLines.style.display = "block";
      connectionLines.innerHTML = "";

      // 연결선용 각도 계산 (상대방 프로필과 동일한 계산)
      const angleStep = (360 / opponentCount) * (Math.PI / 180);
      const startAngle = -90 * (Math.PI / 180);
      const radius = 255; // 프로필과 동일한 반경
      const containerSize = 300;

      for (let i = 0; i < opponentCount; i++) {
        const angle = startAngle + angleStep * i;
        const x2 = 50 + (radius / containerSize) * 50 * Math.cos(angle);
        const y2 = 50 + (radius / containerSize) * 50 * Math.sin(angle);

        const line = document.createElementNS("http://www.w3.org/2000/svg",
            "line");
        line.setAttribute("class", "connection-line");
        line.setAttribute("x1", "50%");
        line.setAttribute("y1", "50%");
        line.setAttribute("x2", `${x2}%`);
        line.setAttribute("y2", `${y2}%`);
        connectionLines.appendChild(line);
      }
    }

    // 1초 후 프로필이 중앙으로 빨려 들어가는 애니메이션 시작
    setTimeout(() => {
      startProfilePullAnimation();
    }, 1000);

    // 카운트다운 시작 (10초)
    let countdown = 10;
    if (countdownText) {
      countdownText.textContent = countdown;
    }

    countdownInterval = setInterval(() => {
      countdown--;
      if (countdownText) {
        if (countdown > 0) {
          countdownText.textContent = countdown;
        } else {
          countdownText.textContent = "";
        }
      }

      if (countdown <= 0) {
        clearInterval(countdownInterval);
        countdownInterval = null;

        // 페이지 이동
        if (sessionId) {
          window.location.href = `/match/online/confirmed?sessionId=${sessionId}`;
        } else {
          console.error("세션 ID를 받지 못했습니다.");
          hideMatchingOverlay();
          alert("세션 정보를 받지 못했습니다.");
        }
      }
    }, 1000);
  }

  // 프로필이 중앙으로 빨려 들어가는 애니메이션
  function startProfilePullAnimation() {
    const profiles = document.querySelectorAll(".opponent-profile");
    const centerX = 50; // 중심 X 위치 (%)
    const centerY = 50; // 중심 Y 위치 (%)

    profiles.forEach((profile, index) => {
      setTimeout(() => {
        const initialX = parseFloat(profile.style.left);
        const initialY = parseFloat(profile.style.top);

        // 애니메이션 시작
        profile.style.transition = "transform 1.5s ease-in, opacity 1.5s ease-in";
        profile.style.transform = `translate(calc(${centerX}% - ${initialX}%), calc(${centerY}% - ${initialY}%)) scale(0.2)`;
        profile.style.opacity = "0";
      }, index * 100);
    });
  }

  // 오버레이 숨기기
  function hideMatchingOverlay() {
    matchingOverlay.style.display = "none";
    isMatching = false;
    updateStartButton();
    resetMatchUI();

    // 타임아웃 정리
    if (matchFoundTimeout) {
      clearTimeout(matchFoundTimeout);
      matchFoundTimeout = null;
    }
    if (countdownInterval) {
      clearInterval(countdownInterval);
      countdownInterval = null;
    }
  }

  // 매칭 취소
  if (cancelButton) {
    cancelButton.addEventListener("click", () => {
      handleCancel();
    });
  }

  // 매칭 취소 처리
  async function handleCancel() {
    if (!isMatching) {
      return;
    }

    try {
      const token = localStorage.getItem("accessToken");

      const headers = {
        "Content-Type": "application/json",
      };

      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }

      // 매칭 취소 API 호출
      const response = await fetch("/api/match/online/join", {
        method: "DELETE",
        headers: headers,
      });

      const result = await response.json();

      // 성공 여부와 관계없이 폴링 중지 및 오버레이 닫기
      stopPolling();
      hideMatchingOverlay();

      if (!response.ok || !result?.success) {
        console.warn("매칭 취소 응답:", result?.message);
      }

      // 온라인 매칭 페이지로 복귀
      window.location.href = "/match/online";
    } catch (error) {
      console.error("매칭 취소 오류:", error);
      // 에러가 발생해도 폴링 중지 및 오버레이 닫기
      stopPolling();
      hideMatchingOverlay();
      window.location.href = "/match/online";
    }
  }

  // 페이지 이탈 시 폴링 정리
  window.addEventListener("beforeunload", () => {
    if (pollingInterval) {
      clearInterval(pollingInterval);
    }
    if (isMatching) {
      const token = localStorage.getItem("accessToken");
      navigator.sendBeacon("/api/match/online/cancel", JSON.stringify({
        headers: {"Authorization": `Bearer ${token}`}
      }));
    }
  });
});

