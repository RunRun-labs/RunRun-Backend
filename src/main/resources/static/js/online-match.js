// 전역 변수 (이벤트 리스너에서 접근 가능하도록)
let selectedDistance = null;
let selectedParticipant = null;
let isMatching = false;
let userProfileUrl = null;
let matchFoundTimeout = null;
let countdownInterval = null;
let currentSessionId = null; // 현재 매칭된 세션 ID 저장
let fallbackCheckDone = false; // 폴백 체크 완료 플래그 (중복 처리 방지)
let fallbackCheckInterval = null; // 주기적 폴백 체크 interval (SSE 연결이 끊겼을 때만 실행)
let sseCheckInterval = null; // SSE 연결 상태 확인 interval
let matchStartTime = null; // 매칭 시작 시간 (오래된 알림 필터링용)

// DOM 요소 참조 (나중에 초기화)
let statusTitle = null;
let statusSubtitle = null;
let matchFoundSection = null;
let countdownText = null;
let opponentProfiles = null;
let connectionLines = null;
let cancelButton = null;
let matchingOverlay = null;
let startButton = null;

// SSE 알림 수신 리스너 등록 (DOMContentLoaded 전에 등록)
window.addEventListener('notification-received', async (event) => {
  const notification = event.detail;

  console.log('[online-match] notification-received 이벤트 수신:', notification);
  console.log('[online-match] isMatching 상태:', isMatching);
  console.log('[online-match] notificationType:', notification.notificationType);
  console.log('[online-match] relatedId:', notification.relatedId);

  // MATCH_FOUND 타입이고 relatedId(sessionId)가 있는 경우에만 처리
  // ✅ isMatching 체크 제거: 매칭 상태와 관계없이 알림 처리 (모집글 매칭과 동일하게)
  if (notification.notificationType === 'MATCH_FOUND' && notification.relatedId) {
    const sessionId = notification.relatedId;

    console.log('[online-match] 매칭 완료 알림 수신 - sessionId:', sessionId);

    // ✅ 매칭 중이 아니어도 알림은 처리 (매칭 시작 전/후 알림도 처리)
    // ✅ 폴백 체크 플래그 설정 (중복 처리 방지)
    fallbackCheckDone = true;
    isMatching = false; // 매칭 상태 종료
    matchStartTime = null; // ✅ 매칭 시작 시간 리셋
    
    try {
      // ✅ Redis 키 정리를 위해 status API 호출
      const token = localStorage.getItem("accessToken");
      const headers = {
        "Content-Type": "application/json",
      };
      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }
      
      try {
        await fetch("/api/match/online/status", {
          method: "GET",
          headers: headers,
        });
        console.log('[online-match] Redis 키 정리 완료 (status API 호출)');
      } catch (statusError) {
        console.warn('[online-match] status API 호출 실패 (무시하고 계속):', statusError);
        // 키 정리 실패해도 매칭 완료 처리는 계속 진행
      }
      
      await showMatchFound(sessionId);
    } catch (error) {
      console.error('[online-match] 매칭 완료 처리 오류:', error);
      if (matchingOverlay) {
        hideMatchingOverlay();
      }
      alert('매칭 완료 처리 중 오류가 발생했습니다.');
    }
  } else {
    console.log('[online-match] MATCH_FOUND가 아니거나 relatedId가 없음');
  }
});

// BFCache(뒤로가기 캐시)에서 복원될 때 DOMContentLoaded가 다시 실행되지 않을 수 있음.
// 따라서 pageshow에서 상태를 리셋하고 SSE 연결을 다시 보장한다.
window.addEventListener('pageshow', async (e) => {
  if (!e.persisted) {
    return;
  }

  console.log('[online-match] pageshow(bfcache restore) - 상태 리셋 + SSE 재연결 보장');

  // 매칭 상태 리셋 (이전 세션/플래그가 남아있으면 MATCH_FOUND를 못 받았을 때 계속 SEARCHING 상태가 됨)
  isMatching = false;
  currentSessionId = null;
  fallbackCheckDone = false; // ✅ 폴백 체크 플래그도 리셋
  matchStartTime = null; // ✅ 매칭 시작 시간 리셋
  
  // 폴백 체크 interval 정리
  if (fallbackCheckInterval) {
    clearInterval(fallbackCheckInterval);
    fallbackCheckInterval = null;
  }
  if (sseCheckInterval) {
    clearInterval(sseCheckInterval);
    sseCheckInterval = null;
  }

  try {
    // UI 초기화
    resetMatchUI();
    updateStartButton();
  } catch (err) {
    console.warn('[online-match] pageshow UI 리셋 중 오류:', err);
  }

  try {
    if (typeof window.ensureSseConnected === 'function') {
      await window.ensureSseConnected();
      console.log('[online-match] pageshow SSE 재연결 확인 완료');
    } else {
      console.warn('[online-match] ensureSseConnected 함수를 사용할 수 없습니다. notification-common.js 로드 여부 확인 필요');
    }
  } catch (err) {
    console.error('[online-match] pageshow SSE 재연결 실패:', err);
  }
});

// 시작 버튼 활성화 상태 업데이트
function updateStartButton() {
  if (startButton) {
    if (selectedDistance && selectedParticipant) {
      startButton.disabled = false;
    } else {
      startButton.disabled = true;
    }
  }
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

    // 성공 여부와 관계없이 오버레이 닫기
    isMatching = false;
    matchStartTime = null; // ✅ 매칭 시작 시간 리셋
    
    // 폴백 체크 중단
    if (fallbackCheckInterval) {
      clearInterval(fallbackCheckInterval);
      fallbackCheckInterval = null;
    }
    if (sseCheckInterval) {
      clearInterval(sseCheckInterval);
      sseCheckInterval = null;
    }
    
    hideMatchingOverlay();

    if (!response.ok || !result?.success) {
      console.warn("매칭 취소 응답:", result?.message);
    }

    // 온라인 매칭 페이지로 복귀
    window.location.href = "/match/online";
  } catch (error) {
    console.error("매칭 취소 오류:", error);
    // 에러가 발생해도 오버레이 닫기
    isMatching = false;
    matchStartTime = null; // ✅ 매칭 시작 시간 리셋
    
    // 폴백 체크 중단
    if (fallbackCheckInterval) {
      clearInterval(fallbackCheckInterval);
      fallbackCheckInterval = null;
    }
    if (sseCheckInterval) {
      clearInterval(sseCheckInterval);
      sseCheckInterval = null;
    }
    
    hideMatchingOverlay();
    window.location.href = "/match/online";
  }
}

document.addEventListener("DOMContentLoaded", () => {
  const backButton = document.querySelector(".back-button");
  const optionButtons = document.querySelectorAll(".option-card");
  startButton = document.getElementById("startButton");
  matchingOverlay = document.getElementById("matchingOverlay");
  cancelButton = document.getElementById("cancelButton");
  const userProfileImage = document.getElementById("userProfileImage");
  statusTitle = document.getElementById("statusTitle");
  statusSubtitle = document.getElementById("statusSubtitle");
  const matchFoundText = document.getElementById("matchFoundText");
  opponentProfiles = document.getElementById("opponentProfiles");
  connectionLines = document.getElementById("connectionLines");
  matchFoundSection = document.getElementById("matchFoundSection");
  countdownText = document.getElementById("countdownText");

  // 초기 상태에서 버튼 숨김
  if (startButton) {
    startButton.style.display = "none";
  }

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

      // 거리 선택 시: 이미 선택된 버튼을 다시 클릭하면 해제
      if (type === "distance" && selectedDistance === value && button.classList.contains("active")) {
        button.classList.remove("active");
        selectedDistance = null;
        
        // match-content에서 has-distance-selected 클래스 제거
        const matchContent = document.querySelector(".match-content");
        if (matchContent) {
          matchContent.classList.remove("has-distance-selected");
        }
        
        // 인원 선택 숨기기 및 초기화
        const participantGroup = document.querySelector('.setting-group[data-type="participant"]');
        if (participantGroup) {
          participantGroup.classList.remove("show");
        }
        
        // 인원 선택도 초기화
        selectedParticipant = null;
        document.querySelectorAll('.option-card[data-type="participant"]')
          .forEach(btn => btn.classList.remove("active"));
        
        updateStartButton();
        return;
      }

      // 같은 타입의 다른 버튼들 비활성화
      document
      .querySelectorAll(`.option-card[data-type="${type}"]`)
      .forEach((btn) => btn.classList.remove("active"));

      // 선택한 버튼 활성화
      button.classList.add("active");

      // 선택 값 저장
      if (type === "distance") {
        selectedDistance = value;
        
        // match-content에 has-distance-selected 클래스 추가
        const matchContent = document.querySelector(".match-content");
        if (matchContent) {
          matchContent.classList.add("has-distance-selected");
        }
        
        // 거리 선택 시 인원 선택 표시
        const participantGroup = document.querySelector('.setting-group[data-type="participant"]');
        if (participantGroup) {
          setTimeout(() => {
            participantGroup.classList.add("show");
            // 인원 선택 섹션으로 부드럽게 스크롤
            setTimeout(() => {
              participantGroup.scrollIntoView({ 
                behavior: 'smooth', 
                block: 'center' 
              });
            }, 400);
          }, 100);
        }
      } else if (type === "participant") {
        selectedParticipant = parseInt(value, 10);
      }

      // 시작 버튼 활성화 확인
      updateStartButton();
    });
  });

  function updateStartButton() {
    const selectionSummary = document.getElementById("selectionSummary");
    const selectedDistanceEl = document.getElementById("selectedDistance");
    const selectedParticipantEl = document.getElementById("selectedParticipant");
    const matchContent = document.querySelector(".match-content");

    if (selectedDistance && selectedParticipant) {
      startButton.disabled = false;
      
      // 버튼 표시
      startButton.style.display = "block";
      
      // 선택 완료 클래스 추가 (레이아웃 변경 최소화)
      if (matchContent) {
        matchContent.classList.add("selection-complete");
        // 스크롤 자동 이동 제거 - 자연스럽게 유지
      }
      
      // 선택 요약 카드 표시 및 업데이트
      if (selectionSummary) {
        // 거리 값 변환 (KM_3 -> 3km)
        const distanceMap = {
          "KM_3": "3km",
          "KM_5": "5km",
          "KM_10": "10km"
        };
        if (selectedDistanceEl) {
          selectedDistanceEl.textContent = distanceMap[selectedDistance] || selectedDistance;
        }
        
        // 인원 값 표시
        if (selectedParticipantEl) {
          selectedParticipantEl.textContent = `${selectedParticipant}명`;
        }
        
        // 애니메이션을 위해 약간의 지연 후 표시
        setTimeout(() => {
          selectionSummary.style.display = "flex";
          // 다음 프레임에서 애니메이션 시작
          requestAnimationFrame(() => {
            selectionSummary.classList.add("show");
          });
        }, 300);
        
        // 거리별 레이팅과 티어 조회
        fetchUserDistanceRating(selectedDistance);
      }
    } else {
      startButton.disabled = true;
      
      // 버튼 숨김
      startButton.style.display = "none";
      
      // 선택 완료 클래스 제거 (중앙으로 복귀)
      if (matchContent) {
        matchContent.classList.remove("selection-complete");
      }
      
      // 선택 요약 카드 숨기기
      if (selectionSummary) {
        selectionSummary.classList.remove("show");
        setTimeout(() => {
          selectionSummary.style.display = "none";
        }, 300);
      }
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

  // 매칭 취소
  if (cancelButton) {
    cancelButton.addEventListener("click", () => {
      handleCancel();
    });
  }

  // ✅ beforeunload에서 매칭 취소 제거
  // 다른 페이지로 이동해도 매칭은 계속 진행되어야 함
  // 사용자가 명시적으로 취소 버튼을 누를 때만 handleCancel()에서 취소 처리
  // 브라우저를 닫거나 새로고침하는 경우는 서버의 타임아웃/하트비트로 처리

  // ✅ URL에서 autoMatch 파라미터 확인 (다른 페이지에서 매칭 알림 받아서 온 경우)
  const urlParams = new URLSearchParams(window.location.search);
  const autoMatchSessionId = urlParams.get('autoMatch');
  
  if (autoMatchSessionId) {
    console.log('[online-match] autoMatch 파라미터 감지 - 자동으로 매칭 완료 처리:', autoMatchSessionId);
    
    // 매칭 중 상태로 설정 (애니메이션이 정상 작동하도록)
    isMatching = true;
    
    // 매칭 오버레이 표시 (showMatchFound가 오버레이가 이미 표시된 상태를 가정함)
    if (matchingOverlay) {
      matchingOverlay.style.display = "flex";
    }
    
    // 매칭 시작 버튼 숨기기
    if (startButton) {
      startButton.style.display = "none";
    }
    
    // 매칭 UI 초기화 (SEARCHING 상태로 시작)
    resetMatchUI();
    
    // ✅ 매칭 오버레이가 표시된 후 티어 정보 업데이트
    setTimeout(() => {
      updateUserTierForMatching();
    }, 100);
    
    // 레이더 애니메이션과 매칭 완료 연출 실행
    // 약간의 지연을 주어 DOM이 완전히 로드되고 애니메이션이 시작되도록 함
    setTimeout(async () => {
      try {
        await showMatchFound(autoMatchSessionId);
      } catch (error) {
        console.error('[online-match] autoMatch 처리 오류:', error);
        alert('매칭 완료 처리 중 오류가 발생했습니다.');
        isMatching = false;
        if (matchingOverlay) {
          hideMatchingOverlay();
        }
      }
      
      // URL에서 파라미터 제거 (새로고침 시 중복 실행 방지)
      const newUrl = window.location.pathname;
      window.history.replaceState({}, '', newUrl);
    }, 300); // 애니메이션이 보이도록 약간 더 지연
  }
});

// 매칭 시작 처리
async function handleMatchStart() {
  // ✅ 상태 초기화
  fallbackCheckDone = false;
  isMatching = true;
  matchStartTime = Date.now(); // ✅ 매칭 시작 시간 저장 (오래된 알림 필터링용)
  if (startButton) {
    startButton.disabled = true;
  }

  try {
    // 1. SSE 연결이 열려있는지 보장
    console.log('[online-match] SSE 연결 확인 중...');
    try {
      if (typeof window.ensureSseConnected === 'function') {
        await window.ensureSseConnected();
        console.log('[online-match] SSE 연결 확인 완료');
      } else {
        console.warn('[online-match] ensureSseConnected 함수를 사용할 수 없습니다. notification-common.js가 로드되었는지 확인하세요.');
        // 연결 확인을 기다리지 않고 진행 (하위 호환성)
        await new Promise(resolve => setTimeout(resolve, 500)); // 0.5초 대기
      }
    } catch (sseError) {
      console.error('[online-match] SSE 연결 실패:', sseError);
      // SSE 연결 실패해도 매칭 시작은 진행 (폴백 체크로 보완)
    }

    const token = localStorage.getItem("accessToken");

    // ✅ 2. 티어 정보를 먼저 조회 (오버레이 표시 전)
    let userTier = "거북이"; // 기본값
    try {
      if (token && selectedDistance) {
        let distanceType = "KM_5";
        if (selectedDistance === 3 || selectedDistance === "3") {
          distanceType = "KM_3";
        } else if (selectedDistance === 5 || selectedDistance === "5") {
          distanceType = "KM_5";
        } else if (selectedDistance === 10 || selectedDistance === "10") {
          distanceType = "KM_10";
        } else if (selectedDistance && (selectedDistance.startsWith("KM_") || selectedDistance.startsWith("km_"))) {
          distanceType = selectedDistance.toUpperCase();
        } else if (selectedDistance) {
          distanceType = selectedDistance;
        }

        console.log('[DEBUG] 매칭 시작 전 티어 조회 - selectedDistance:', selectedDistance, 'distanceType:', distanceType);
        
        const tierResponse = await fetch(`/api/rating/distance?distanceType=${distanceType}`, {
          headers: { "Authorization": `Bearer ${token}` }
        });

        if (tierResponse.ok) {
          const tierResult = await tierResponse.json();
          userTier = tierResult?.data?.currentTier || "거북이";
          console.log('[DEBUG] 매칭 시작 전 티어 조회 성공:', userTier);
        }
      }
    } catch (error) {
      console.error("매칭 시작 전 티어 조회 실패:", error);
    }

    const headers = {
      "Content-Type": "application/json",
    };

    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    // 3. 매칭 신청 API 호출
    console.log('[online-match] 매칭 신청 API 호출...');
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

    // 3. 응답에 sessionId가 있으면 이미 참여 중인 방이 있음
    if (result?.data) {
      const sessionId = result.data;

      // 1. matchingOverlay를 display: flex로 보여준다
      matchingOverlay.style.display = "flex";
      // 매칭 시작 버튼 숨기기
      if (startButton) {
        startButton.style.display = "none";
      }
      // 헤더 숨기기
      const header = document.querySelector(".header");
      if (header) {
        header.style.display = "none";
      }

      // 2. resetMatchUI()를 호출해서 초기화한다
      resetMatchUI();
      
      // ✅ 조회한 티어를 즉시 설정 (setTimeout 제거)
      const userTierEl = document.getElementById("userTier");
      if (userTierEl) {
        userTierEl.textContent = userTier;
        const tierStr = String(userTier).trim();
        if (tierStr === "장산범" || tierStr === "JANGSANBEOM" || tierStr.toLowerCase() === "장산범") {
          userTierEl.classList.add("tier-jangsanbeom");
        } else {
          userTierEl.classList.remove("tier-jangsanbeom");
        }
      }

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
        window.location.href = `/match/waiting?sessionId=${sessionId}`;
      }, 2000);

      return;
    }

    // 4. sessionId가 없으면 대기 화면 표시 및 폴백 체크
    matchingOverlay.style.display = "flex";
    // 매칭 시작 버튼 숨기기
    if (startButton) {
      startButton.style.display = "none";
    }
    // 헤더 숨기기
    const header = document.querySelector(".header");
    if (header) {
      header.style.display = "none";
    }
    resetMatchUI();
    
    // ✅ 조회한 티어를 즉시 설정 (setTimeout 제거)
    const userTierEl = document.getElementById("userTier");
    if (userTierEl) {
      userTierEl.textContent = userTier;
      const tierStr = String(userTier).trim();
      if (tierStr === "장산범" || tierStr === "JANGSANBEOM" || tierStr.toLowerCase() === "장산범") {
        userTierEl.classList.add("tier-jangsanbeom");
      } else {
        userTierEl.classList.remove("tier-jangsanbeom");
      }
    }

    // 5. 폴백 체크: SSE 연결이 끊겼을 때만 주기적으로 매칭 상태 확인
    // 기존 interval이 있으면 정리
    if (fallbackCheckInterval) {
      clearInterval(fallbackCheckInterval);
      fallbackCheckInterval = null;
    }
    if (sseCheckInterval) {
      clearInterval(sseCheckInterval);
      sseCheckInterval = null;
    }

    // 폴백 체크 함수 (SSE 연결이 끊겼을 때만 실행됨)
    const performFallbackCheck = async () => {
      // 매칭 중인 상태가 아니면 폴백 체크 중단
      if (!isMatching) {
        if (fallbackCheckInterval) {
          clearInterval(fallbackCheckInterval);
          fallbackCheckInterval = null;
        }
        return;
      }
      
      // ✅ 이미 SSE 이벤트로 매칭 완료되었으면 폴백 체크 중단
      if (fallbackCheckDone) {
        console.log('[online-match] 이미 매칭 완료 처리됨, 폴백 체크 중단');
        if (fallbackCheckInterval) {
          clearInterval(fallbackCheckInterval);
          fallbackCheckInterval = null;
        }
        return;
      }

      // SSE 연결이 끊겼을 때만 폴백 체크 실행
      console.log('[online-match] 폴백 체크: SSE 연결 끊김 상태, 매칭 상태 확인 중...');
      try {
        // 방법 1: /api/match/online/status 사용 (더 정확)
        const statusResponse = await fetch("/api/match/online/status", {
          method: "GET",
          headers: headers,
        });

        if (statusResponse.ok) {
          const statusResult = await statusResponse.json();
          if (statusResult?.success && statusResult?.data) {
            const { status, sessionId } = statusResult.data;
            
            console.log('[online-match] 폴백 체크 응답:', { status, sessionId });

            if (status === "MATCHED" && sessionId) {
              console.log('[online-match] 폴백 체크: 이미 매칭 완료됨', sessionId);
              fallbackCheckDone = true; // ✅ 플래그 설정
              isMatching = false;
              matchStartTime = null; // ✅ 매칭 시작 시간 리셋
              
              // 폴백 체크 중단
              if (fallbackCheckInterval) {
                clearInterval(fallbackCheckInterval);
                fallbackCheckInterval = null;
              }
              
              await showMatchFound(sessionId);
              return;
            }
          }
        }

        // 방법 2: 알림 목록에서 MATCH_FOUND 확인 (방법 1 실패 시)
        const notificationResponse = await fetch("/api/notifications/remaining?page=0&size=10", {
          method: "GET",
          headers: headers,
        });

        if (notificationResponse.ok) {
          const notificationResult = await notificationResponse.json();
          if (notificationResult?.success && notificationResult?.data?.content) {
            const matchFound = notificationResult.data.content.find(
              n => n.notificationType === 'MATCH_FOUND' 
                && n.relatedId 
                && !n.isRead
                && matchStartTime !== null // ✅ 매칭 시작 시간이 설정되어 있어야 함
                && new Date(n.createdAt).getTime() >= matchStartTime // ✅ 현재 매칭 시작 이후의 알림만 처리
                && (Date.now() - new Date(n.createdAt).getTime()) < 30000 // 30초 이내로 확장
            );

            if (matchFound) {
              console.log('[online-match] 폴백 체크: MATCH_FOUND 알림 발견', matchFound);
              fallbackCheckDone = true; // ✅ 플래그 설정
              
              // 폴백 체크 중단
              if (fallbackCheckInterval) {
                clearInterval(fallbackCheckInterval);
                fallbackCheckInterval = null;
              }
              
              // 읽음 처리
              await fetch(`/api/notifications/${matchFound.id}/read`, {
                method: "PATCH",
                headers: headers,
              }).catch(() => {});

              // ✅ Redis 키 정리를 위해 status API 호출
              try {
                await fetch("/api/match/online/status", {
                  method: "GET",
                  headers: headers,
                });
                console.log('[online-match] Redis 키 정리 완료 (폴백 체크 - 알림 목록)');
              } catch (statusError) {
                console.warn('[online-match] status API 호출 실패 (무시하고 계속):', statusError);
                // 키 정리 실패해도 매칭 완료 처리는 계속 진행
              }

              // 매칭 완료 처리
              isMatching = false;
              matchStartTime = null; // ✅ 매칭 시작 시간 리셋
              await showMatchFound(matchFound.relatedId);
              return;
            }
          }
        }

        console.log('[online-match] 폴백 체크: 매칭 대기 중');
      } catch (fallbackError) {
        console.warn('[online-match] 폴백 체크 실패 (무시하고 계속):', fallbackError);
      }
    };

    // SSE 연결 상태 모니터링 및 폴백 체크 관리
    const checkSseAndStartFallback = () => {
      // 매칭 중이 아니면 모니터링 중단
      if (!isMatching) {
        if (sseCheckInterval) {
          clearInterval(sseCheckInterval);
          sseCheckInterval = null;
        }
        if (fallbackCheckInterval) {
          clearInterval(fallbackCheckInterval);
          fallbackCheckInterval = null;
        }
        return;
      }
      
      // 이미 매칭 완료되었으면 중단
      if (fallbackCheckDone) {
        if (sseCheckInterval) {
          clearInterval(sseCheckInterval);
          sseCheckInterval = null;
        }
        if (fallbackCheckInterval) {
          clearInterval(fallbackCheckInterval);
          fallbackCheckInterval = null;
        }
        return;
      }
      
      // SSE 연결 상태 확인
      const isConnected = typeof window.isSseConnected === 'function' && window.isSseConnected();
      
      if (!isConnected) {
        // SSE 연결이 끊김 → 폴백 체크 시작
        if (!fallbackCheckInterval) {
          console.log('[online-match] SSE 연결 끊김 감지 - 폴백 체크 시작');
          performFallbackCheck(); // 즉시 한 번 실행
          fallbackCheckInterval = setInterval(performFallbackCheck, 5000);
        }
      } else {
        // SSE 연결이 정상 → 폴백 체크 중단
        if (fallbackCheckInterval) {
          console.log('[online-match] SSE 연결 복구 감지 - 폴백 체크 중단');
          clearInterval(fallbackCheckInterval);
          fallbackCheckInterval = null;
        }
      }
    };

    // SSE 연결 상태를 주기적으로 확인 (2초마다)
    // 연결이 끊겼을 때만 폴백 체크를 시작하고, 복구되면 중단
    setTimeout(() => {
      checkSseAndStartFallback(); // 즉시 한 번 실행
      sseCheckInterval = setInterval(checkSseAndStartFallback, 2000); // 2초마다 SSE 상태 확인
    }, 2000);

    // SSE 이벤트를 기다림 (notification-received 이벤트 리스너가 처리)
    console.log('[online-match] SSE 이벤트 대기 중...');
  } catch (error) {
    console.error("매칭 시작 오류:", error);
    alert(error.message || "매칭 시작 중 오류가 발생했습니다.");
    hideMatchingOverlay();
    isMatching = false;
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
  if (fallbackCheckInterval) {
    clearInterval(fallbackCheckInterval);
    fallbackCheckInterval = null;
  }
  if (sseCheckInterval) {
    clearInterval(sseCheckInterval);
    sseCheckInterval = null;
  }
  
  // ✅ 폴백 체크 플래그 리셋
  fallbackCheckDone = false;
  matchStartTime = null; // ✅ 매칭 시작 시간 리셋
}

// 매칭 정보 조회 (SSE 알림 수신 후 세션 정보를 가져오기 위해 사용)
async function fetchMatchInfo(sessionId) {
  try {
    const token = localStorage.getItem("accessToken");

    const headers = {
      "Content-Type": "application/json",
    };

    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    const response = await fetch(`/api/match/session/${sessionId}`, {
      method: "GET",
      headers: headers,
    });

    const result = await response.json();

    // 디버깅: API 응답 확인
    console.log('[DEBUG] API 응답:', result);
    console.log('[DEBUG] participants:', result?.data?.participants);
    if (result?.data?.participants) {
      result.data.participants.forEach((p, i) => {
        console.log(`[DEBUG] participant[${i}]:`, p, 'tier:', p.tier, 'tier type:', typeof p.tier);
      });
    }

    if (!response.ok || !result?.success) {
      throw new Error(result?.message || "매칭 정보 조회에 실패했습니다.");
    }

    return result?.data;
  } catch (error) {
    console.error("매칭 정보 조회 오류:", error);
    throw error;
  }
}

// MATCH FOUND 연출
async function showMatchFound(sessionId) {
  // 기존 카운트다운 정리 (중복 방지)
  if (countdownInterval) {
    clearInterval(countdownInterval);
    countdownInterval = null;
  }

  // sessionId로 매칭 정보 조회
  let matchData;
  try {
    matchData = await fetchMatchInfo(sessionId);
    // matchData에 sessionId가 없으면 추가
    if (!matchData.sessionId) {
      matchData.sessionId = sessionId;
    }
  } catch (error) {
    console.error("매칭 정보 조회 실패:", error);
    // 매칭 정보 조회 실패 시에도 기본 정보로 진행
    matchData = {sessionId: sessionId, participants: []};
  }

  const finalSessionId = matchData.sessionId || sessionId;

  // finalSessionId 검증
  if (!finalSessionId) {
    console.error("세션 ID를 받지 못했습니다. sessionId:", sessionId, "matchData:", matchData);
    hideMatchingOverlay();
    alert("세션 정보를 받지 못했습니다.");
    return;
  }

  // 전역 변수에 저장 (카운트다운 종료 시 사용)
  currentSessionId = finalSessionId;

  console.log('[online-match] showMatchFound - finalSessionId:', finalSessionId);

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

    // 현재 사용자 ID 가져오기
    const currentUserId = localStorage.getItem("userId") ? parseInt(localStorage.getItem("userId")) : null;
    
    let opponentIndex = 0;
    for (let i = 0; i < participants.length; i++) {
      const participant = participants[i];
      // 본인은 제외 (userId로 비교)
      if (currentUserId && participant?.userId === currentUserId) {
        // 내 프로필 이미지 업데이트
        if (userProfileImage && participant?.profileImage) {
          userProfileImage.src = participant.profileImage;
        }
        // 내 티어 업데이트
        const userTierEl = document.getElementById("userTier");
        if (userTierEl) {
          // API에서 티어 정보를 받아오거나, 백엔드 기본값과 일치하는 "거북이" 사용
          const tier = participant?.tier || "거북이";
          console.log('[DEBUG] 내 티어 값:', tier, '타입:', typeof tier, '원본 participant:', participant);
          userTierEl.textContent = tier;
          // 장산범일 경우 특별한 클래스 추가 (다양한 형식 대응)
          const tierStr = String(tier).trim();
          if (tierStr === "장산범" || tierStr === "JANGSANBEOM" || tierStr.toLowerCase() === "장산범") {
            userTierEl.classList.add("tier-jangsanbeom");
            console.log('[DEBUG] 장산범 티어 감지 - 빨간색 스타일 적용');
          } else {
            userTierEl.classList.remove("tier-jangsanbeom");
          }
        }
        continue;
      }
      // 첫 번째가 본인이라고 가정하는 경우도 처리
      if (i === 0 && !currentUserId) {
        // 내 프로필 이미지 업데이트
        if (userProfileImage && participant?.profileImage) {
          userProfileImage.src = participant.profileImage;
        }
        // 내 티어 업데이트
        const userTierEl = document.getElementById("userTier");
        if (userTierEl) {
          // API에서 티어 정보를 받아오거나, 백엔드 기본값과 일치하는 "거북이" 사용
          const tier = participant?.tier || "거북이";
          console.log('[DEBUG] 내 티어 값 (fallback):', tier, '타입:', typeof tier, '원본 participant:', participant);
          userTierEl.textContent = tier;
          // 장산범일 경우 특별한 클래스 추가 (다양한 형식 대응)
          const tierStr = String(tier).trim();
          if (tierStr === "장산범" || tierStr === "JANGSANBEOM" || tierStr.toLowerCase() === "장산범") {
            userTierEl.classList.add("tier-jangsanbeom");
            console.log('[DEBUG] 장산범 티어 감지 (fallback) - 빨간색 스타일 적용');
          } else {
            userTierEl.classList.remove("tier-jangsanbeom");
          }
        }
        continue;
      }

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
      if (participant?.profileImage) {
        img.src = participant.profileImage;
      } else {
        img.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='60' height='60' viewBox='0 0 60 60'%3E%3Ccircle cx='30' cy='30' r='30' fill='%231a1f2e'/%3E%3Ccircle cx='30' cy='20' r='10' fill='%23baff29'/%3E%3Cpath d='M10 50 Q10 40 30 40 Q50 40 50 50' fill='%23baff29'/%3E%3C/svg%3E";
      }
      img.alt = participant?.name || "상대방";
      img.onerror = () => {
        img.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='60' height='60' viewBox='0 0 60 60'%3E%3Ccircle cx='30' cy='30' r='30' fill='%231a1f2e'/%3E%3Ccircle cx='30' cy='20' r='10' fill='%23baff29'/%3E%3Cpath d='M10 50 Q10 40 30 40 Q50 40 50 50' fill='%23baff29'/%3E%3C/svg%3E";
      };

      const label = document.createElement("div");
      label.className = "opponent-label";
      // API에서 티어 정보를 받아오거나, 백엔드 기본값과 일치하는 "거북이" 사용
      const tier = participant?.tier || "거북이";
      console.log('[DEBUG] 상대방 티어 값:', tier, '타입:', typeof tier, 'participant:', participant);
      // 장산범일 경우 특별한 클래스 추가 (다양한 형식 대응)
      const tierStr = String(tier).trim();
      const tierClass = (tierStr === "장산범" || tierStr === "JANGSANBEOM" || tierStr.toLowerCase() === "장산범") 
        ? "opponent-tier tier-jangsanbeom" 
        : "opponent-tier";
      if (tierClass.includes("tier-jangsanbeom")) {
        console.log('[DEBUG] 상대방 장산범 티어 감지 - 빨간색 스타일 적용');
      }
      label.innerHTML = `<span class="opponent-name">${participant?.name
      || `Player ${opponentIndex
      + 1}`}</span><span class="${tierClass}">${tier}</span>`;

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
        // 기본 티어는 백엔드 기본값과 일치하는 "거북이" 사용
        label.innerHTML = `<span class="opponent-name">Player ${i
        + 1}</span><span class="opponent-tier">거북이</span>`;

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

  console.log('[online-match] 카운트다운 시작 - finalSessionId:', finalSessionId);

  // 기존 interval이 있으면 다시 정리 (이중 안전장치)
  if (countdownInterval) {
    clearInterval(countdownInterval);
    countdownInterval = null;
  }

  // finalSessionId를 로컬 변수로도 저장 (클로저 보장)
  const sessionIdForRedirect = finalSessionId;

  countdownInterval = setInterval(() => {
    countdown--;
    console.log('[online-match] 카운트다운:', countdown, 'finalSessionId:', sessionIdForRedirect, 'currentSessionId:', currentSessionId);
    
    if (countdownText) {
      if (countdown > 0) {
        countdownText.textContent = countdown;
      } else {
        countdownText.textContent = "";
      }
    }

    if (countdown <= 0) {
      console.log('[online-match] 카운트다운 종료, 페이지 이동 시도');
      console.log('[online-match] sessionIdForRedirect:', sessionIdForRedirect);
      console.log('[online-match] currentSessionId:', currentSessionId);
      
      // interval 정리
      if (countdownInterval) {
        clearInterval(countdownInterval);
        countdownInterval = null;
      }

      // 세션 ID 확인 (우선순위: 전역 변수 > 로컬 변수 > 매개변수)
      const targetSessionId = currentSessionId || sessionIdForRedirect || finalSessionId || sessionId;
      
      console.log('[online-match] 최종 targetSessionId:', targetSessionId);

      // 페이지 이동
      if (targetSessionId) {
        const redirectUrl = `/match/waiting?sessionId=${targetSessionId}`;
        console.log('[online-match] 페이지 이동 실행:', redirectUrl);
        
        // 즉시 이동 시도
        try {
          window.location.href = redirectUrl;
        } catch (e) {
          console.error('[online-match] location.href 실패, replace 시도:', e);
          try {
            window.location.replace(redirectUrl);
          } catch (e2) {
            console.error('[online-match] location.replace도 실패:', e2);
            // 최후의 수단
            setTimeout(() => {
              window.location.href = redirectUrl;
            }, 100);
          }
        }
      } else {
        console.error("[online-match] 세션 ID를 받지 못했습니다. 모든 값:", {
          currentSessionId,
          sessionIdForRedirect,
          finalSessionId,
          sessionId
        });
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
  if (matchingOverlay) {
    matchingOverlay.style.display = "none";
  }
  // 매칭 시작 버튼 다시 표시
  if (startButton) {
    startButton.style.display = "block";
  }
  // 헤더 다시 표시
  const header = document.querySelector(".header");
  if (header) {
    header.style.display = "flex";
  }
  isMatching = false;
  matchStartTime = null; // ✅ 매칭 시작 시간 리셋
  currentSessionId = null; // 세션 ID 초기화
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
  if (fallbackCheckInterval) {
    clearInterval(fallbackCheckInterval);
    fallbackCheckInterval = null;
  }
  if (sseCheckInterval) {
    clearInterval(sseCheckInterval);
    sseCheckInterval = null;
  }
}

// 시작 버튼 활성화 상태 업데이트
function updateStartButton() {
  if (startButton) {
    if (selectedDistance && selectedParticipant) {
      startButton.disabled = false;
    } else {
      startButton.disabled = true;
    }
  }
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

    // 성공 여부와 관계없이 오버레이 닫기
    isMatching = false;
    matchStartTime = null; // ✅ 매칭 시작 시간 리셋
    
    // 폴백 체크 중단
    if (fallbackCheckInterval) {
      clearInterval(fallbackCheckInterval);
      fallbackCheckInterval = null;
    }
    if (sseCheckInterval) {
      clearInterval(sseCheckInterval);
      sseCheckInterval = null;
    }
    
    hideMatchingOverlay();

    if (!response.ok || !result?.success) {
      console.warn("매칭 취소 응답:", result?.message);
    }

    // 온라인 매칭 페이지로 복귀
    window.location.href = "/match/online";
  } catch (error) {
    console.error("매칭 취소 오류:", error);
    // 에러가 발생해도 오버레이 닫기
    isMatching = false;
    matchStartTime = null; // ✅ 매칭 시작 시간 리셋
    
    // 폴백 체크 중단
    if (fallbackCheckInterval) {
      clearInterval(fallbackCheckInterval);
      fallbackCheckInterval = null;
    }
    if (sseCheckInterval) {
      clearInterval(sseCheckInterval);
      sseCheckInterval = null;
    }
    
    hideMatchingOverlay();
    window.location.href = "/match/online";
  }
}

// 거리별 레이팅과 티어 조회 함수
async function fetchUserDistanceRating(distanceType) {
  try {
    const token = localStorage.getItem("accessToken");
    if (!token) {
      console.warn("토큰이 없어 레이팅을 조회할 수 없습니다.");
      // 기본값 설정
      setDefaultRatingValues();
      return;
    }

    // API 엔드포인트 호출
    const response = await fetch(`/api/rating/distance?distanceType=${distanceType}`, {
      headers: {
        "Authorization": `Bearer ${token}`
      }
    });

    if (response.ok) {
      const result = await response.json();
      const rating = result?.data?.currentRating || 1000;
      const tier = result?.data?.currentTier || "거북이";

      // 레이팅과 티어 표시
      const ratingEl = document.getElementById("selectedRating");
      const tierEl = document.getElementById("selectedTier");

      if (ratingEl) {
        ratingEl.textContent = `${rating} LP`;
      }
      if (tierEl) {
        tierEl.textContent = tier;
      }
    } else {
      console.warn("레이팅 조회 실패:", response.status);
      setDefaultRatingValues();
    }
  } catch (error) {
    console.error("레이팅 조회 실패:", error);
    // 기본값 설정
    setDefaultRatingValues();
  }
}

// 기본 레이팅과 티어 값 설정
function setDefaultRatingValues() {
  const ratingEl = document.getElementById("selectedRating");
  const tierEl = document.getElementById("selectedTier");
  if (ratingEl) {
    ratingEl.textContent = "1000 LP";
  }
  if (tierEl) {
    tierEl.textContent = "거북이";
  }
}

// 매칭 대기 중 사용자 티어 업데이트
async function updateUserTierForMatching() {
  try {
    const token = localStorage.getItem("accessToken");
    if (!token) {
      console.warn("토큰이 없어 티어를 조회할 수 없습니다.");
      return;
    }

    // userTierEl이 DOM에 있는지 확인
    const userTierEl = document.getElementById("userTier");
    if (!userTierEl) {
      console.warn("userTierEl이 DOM에 없습니다. matchingOverlay가 표시되었는지 확인하세요.");
      return;
    }

    // 선택한 거리에 따라 DistanceType 결정
    // selectedDistance는 이미 "KM_3", "KM_5", "KM_10" 형식이므로 그대로 사용
    let distanceType = "KM_5"; // 기본값
    
    // 숫자 형식으로 저장된 경우를 대비한 변환 (하위 호환성)
    if (selectedDistance === 3 || selectedDistance === "3") {
      distanceType = "KM_3";
    } else if (selectedDistance === 5 || selectedDistance === "5") {
      distanceType = "KM_5";
    } else if (selectedDistance === 10 || selectedDistance === "10") {
      distanceType = "KM_10";
    } else if (selectedDistance && (selectedDistance.startsWith("KM_") || selectedDistance.startsWith("km_"))) {
      // 이미 "KM_3" 형식이면 그대로 사용
      distanceType = selectedDistance.toUpperCase();
    } else if (selectedDistance) {
      // 다른 형식이면 그대로 사용 (이미 올바른 형식일 수 있음)
      distanceType = selectedDistance;
    }
    
    console.log('[DEBUG] 매칭 대기 중 티어 조회 시작 - selectedDistance:', selectedDistance, 'distanceType:', distanceType);

    // 티어 정보 조회
    const response = await fetch(`/api/rating/distance?distanceType=${distanceType}`, {
      headers: {
        "Authorization": `Bearer ${token}`
      }
    });

    if (response.ok) {
      const result = await response.json();
      const tier = result?.data?.currentTier || "거북이";
      
      console.log('[DEBUG] 매칭 대기 중 티어 조회 성공:', tier, 'distanceType:', distanceType);
      
      // 매칭 화면의 티어 표시 업데이트 (다시 확인)
      const userTierEl = document.getElementById("userTier");
      if (userTierEl) {
        userTierEl.textContent = tier;
        
        // 장산범일 경우 특별한 클래스 추가
        const tierStr = String(tier).trim();
        if (tierStr === "장산범" || tierStr === "JANGSANBEOM" || tierStr.toLowerCase() === "장산범") {
          userTierEl.classList.add("tier-jangsanbeom");
          console.log('[DEBUG] 매칭 대기 중 장산범 티어 감지 - 빨간색 스타일 적용');
        } else {
          userTierEl.classList.remove("tier-jangsanbeom");
        }
      } else {
        console.error('[DEBUG] userTierEl을 찾을 수 없습니다!');
      }
    } else {
      console.warn("매칭 대기 중 티어 조회 실패:", response.status);
    }
  } catch (error) {
    console.error("매칭 대기 중 티어 조회 실패:", error);
  }
}

