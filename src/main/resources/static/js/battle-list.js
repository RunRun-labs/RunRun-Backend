document.addEventListener("DOMContentLoaded", () => {
  const backButton = document.getElementById("backBtn");
  const filterTabs = document.querySelectorAll(".filter-tab");
  const battleList = document.getElementById("battleList");
  const loadingSpinner = document.getElementById("loadingSpinner");
  const emptyState = document.getElementById("emptyState");
  const sentinel = document.getElementById("sentinel");
  const container = document.querySelector(".container");

  let currentDistance = "KM_3";
  let currentPage = 0;
  let hasNext = true;
  let isLoading = false;
  let observer = null;
  let lastRenderedDate = null; // 마지막으로 렌더링된 날짜 추적

  // 거리 타입 매핑
  const distanceMap = {
    "KM_3": "3km",
    "KM_5": "5km",
    "KM_10": "10km"
  };

  // 뒤로가기 버튼
  if (backButton) {
    backButton.addEventListener("click", () => {
      window.history.length > 1
          ? window.history.back()
          : (window.location.href = "/");
    });
  }

  // 필터 탭 클릭 이벤트
  filterTabs.forEach((tab) => {
    tab.addEventListener("click", () => {
      const distance = tab.dataset.distance;
      if (distance !== currentDistance) {
        currentDistance = distance;
        currentPage = 0;
        hasNext = true;
        battleList.innerHTML = "";

        // 옵저버 해제
        if (observer) {
          observer.disconnect();
          observer = null;
        }

        // 스크롤 위치 초기화 (.container 기준)
        if (container) {
          container.scrollTo({top: 0, behavior: 'smooth'});
        }

        updateFilterTabs();
        loadBattleResults(true);
      }
    });
  });

  // 필터 탭 활성화 상태 업데이트
  function updateFilterTabs() {
    filterTabs.forEach((tab) => {
      if (tab.dataset.distance === currentDistance) {
        tab.classList.add("active");
      } else {
        tab.classList.remove("active");
      }
    });
  }

  // 시간 포맷팅 (초를 MM:SS로)
  function formatTime(seconds) {
    if (!seconds) {
      return "00:00";
    }
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes.toString().padStart(2, "0")}:${secs.toString().padStart(2,
        "0")}`;
  }

  // 페이스 포맷팅 (분/km)
  function formatPace(paceMinutes) {
    if (!paceMinutes) {
      return "0:00/km";
    }
    const minutes = Math.floor(paceMinutes);
    const seconds = Math.round((paceMinutes - minutes) * 60);
    return `${minutes}:${seconds.toString().padStart(2, "0")}/km`;
  }

  // 날짜 포맷팅 (YYYY.MM.DD)
  function formatDate(dateString) {
    if (!dateString) {
      return "";
    }
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}.${month}.${day}`;
  }

  // 날짜 및 시간 포맷팅 (YYYY.MM.DD HH:mm)
  function formatDateTime(dateString) {
    if (!dateString) {
      return "";
    }
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${year}.${month}.${day} ${hours}:${minutes}`;
  }

  // 날짜 그룹 헤더 텍스트 생성
  function getDateGroupHeader(dateString) {
    if (!dateString) {
      return "";
    }
    const date = new Date(dateString);
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    // 시간을 00:00:00으로 설정하여 날짜만 비교
    const recordDate = new Date(date.getFullYear(), date.getMonth(),
        date.getDate());
    const todayDate = new Date(today.getFullYear(), today.getMonth(),
        today.getDate());
    const yesterdayDate = new Date(yesterday.getFullYear(),
        yesterday.getMonth(), yesterday.getDate());

    if (recordDate.getTime() === todayDate.getTime()) {
      return "오늘";
    } else if (recordDate.getTime() === yesterdayDate.getTime()) {
      return "어제";
    } else {
      return formatDate(dateString);
    }
  }

  // 날짜별로 그룹화
  function groupRecordsByDate(records) {
    const groups = {};
    records.forEach((record) => {
      const dateKey = formatDate(record.createdAt);
      if (!groups[dateKey]) {
        groups[dateKey] = [];
      }
      groups[dateKey].push(record);
    });
    return groups;
  }

  // 토큰 가져오기
  function getToken() {
    return localStorage.getItem("accessToken") || getCookie("accessToken");
  }

  // 쿠키에서 값 가져오기
  function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
      return parts.pop().split(";").shift();
    }
    return null;
  }

  // 배틀 기록 로드
  async function loadBattleResults(reset = false) {
    if (isLoading || (!hasNext && !reset)) {
      return;
    }

    isLoading = true;

    if (reset) {
      battleList.innerHTML = "";
      currentPage = 0;
      hasNext = true;
      lastRenderedDate = null; // 리셋 시 날짜 추적 초기화

      // 옵저버 해제
      if (observer) {
        observer.disconnect();
        observer = null;
      }
    }

    loadingSpinner.style.display = "flex";
    emptyState.style.display = "none";

    try {
      const token = getToken();
      const headers = {
        "Content-Type": "application/json"
      };

      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }

      const params = new URLSearchParams({
        distanceType: currentDistance,
        page: currentPage.toString(),
        size: "10"
      });

      const response = await fetch(`/api/battle-result/results?${params}`, {
        method: "GET",
        headers: headers
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      if (result.success && result.data) {
        const data = result.data;

        // Slice 구조: { content: [...], hasNext: boolean, last: boolean, number: number, size: number, ... }
        const records = data.content || [];

        // Slice의 hasNext 확인 (고스트런과 동일한 로직)
        // hasNext 필드가 없을 경우 !data.last를 확인
        hasNext = data.hasNext !== undefined ? data.hasNext : !data.last;

        if (records.length === 0 && reset) {
          emptyState.style.display = "flex";
          lastRenderedDate = null; // 리셋 시 날짜 추적 초기화
        } else if (records.length > 0) {
          // 기록들을 날짜순으로 정렬 (최신이 먼저)
          const sortedRecords = [...records].sort((a, b) => {
            return new Date(b.createdAt) - new Date(a.createdAt);
          });

          // reset이 아닐 때, 이미 렌더링된 마지막 날짜 확인
          if (!reset && battleList.children.length > 0) {
            // 마지막 요소부터 역순으로 날짜 헤더 찾기
            for (let i = battleList.children.length - 1; i >= 0; i--) {
              const child = battleList.children[i];
              if (child.classList.contains("date-section-header")) {
                // 이미 렌더링된 마지막 날짜 헤더의 텍스트에서 날짜 추출
                const headerText = child.textContent.trim();
                // "오늘", "어제" 또는 날짜 형식(YYYY.MM.DD) 확인
                if (headerText === "오늘") {
                  lastRenderedDate = formatDate(new Date());
                } else if (headerText === "어제") {
                  const yesterday = new Date();
                  yesterday.setDate(yesterday.getDate() - 1);
                  lastRenderedDate = formatDate(yesterday);
                } else if (headerText.match(/^\d{4}\.\d{2}\.\d{2}$/)) {
                  lastRenderedDate = headerText;
                }
                break;
              } else if (child.classList.contains("battle-card")) {
                // 카드에서 날짜 추출
                const cardDateAttr = child.querySelector(".battle-card-time");
                if (cardDateAttr) {
                  const cardDateText = cardDateAttr.textContent.trim();
                  // YYYY.MM.DD HH:mm 형식에서 날짜 부분만 추출
                  const dateMatch = cardDateText.match(
                      /^(\d{4}\.\d{2}\.\d{2})/);
                  if (dateMatch) {
                    lastRenderedDate = dateMatch[1];
                  }
                }
                break;
              }
            }
          }

          // 각 기록을 순회하며 날짜 헤더 추가
          sortedRecords.forEach((record) => {
            const recordDate = formatDate(record.createdAt);

            // 날짜가 바뀔 때만 헤더 추가 (reset이거나 이전 날짜와 다를 때)
            if (lastRenderedDate === null || lastRenderedDate !== recordDate) {
              const headerText = getDateGroupHeader(record.createdAt);
              const headerDiv = document.createElement("div");
              headerDiv.className = "date-section-header";
              headerDiv.textContent = headerText;
              battleList.appendChild(headerDiv);
              lastRenderedDate = recordDate;
            }

            // 카드 렌더링
            renderBattleCard(record);
          });

          // 렌더링 후 마지막 레코드의 날짜를 저장 (다음 페이지 로드를 위해)
          if (sortedRecords.length > 0) {
            const lastRecord = sortedRecords[sortedRecords.length - 1];
            lastRenderedDate = formatDate(lastRecord.createdAt);
          }

          // 다음 페이지를 위해 페이지 번호 증가
          currentPage++;
        }

        // 무한 스크롤 옵저버 설정 (데이터가 있을 때만)
        if (records.length > 0) {
          if (hasNext && sentinel) {
            setupInfiniteScroll();
          } else if (observer) {
            observer.disconnect();
            observer = null;
          }
        } else if (observer) {
          observer.disconnect();
          observer = null;
        }
      }
    } catch (error) {
      console.error("배틀 기록 로드 실패:", error);
      if (reset) {
        emptyState.style.display = "flex";
        emptyState.innerHTML = `<p>배틀 기록을 불러오는데 실패했습니다.<br>${error.message}</p>`;
      }
    } finally {
      loadingSpinner.style.display = "none";
      isLoading = false;
    }
  }

  // 배틀 카드 렌더링
  function renderBattleCard(record) {
    const card = document.createElement("div");
    card.dataset.sessionId = record.sessionId;

    const time = formatTime(record.totalTime);
    const pace = formatPace(record.avgPace);
    const distance = distanceMap[record.distanceType] || record.distanceType;
    const distanceValue = record.totalDistance ? record.totalDistance.toFixed(2)
        : distance.replace("km", "");
    const dateTime = formatDateTime(record.createdAt);

    // 레이팅 정보
    const previousRating = record.previousRating || 0;
    const currentRating = record.currentRating || 0;
    const delta = record.delta || 0;
    const deltaClass = delta > 0 ? "positive" : delta < 0 ? "negative" : "zero";
    const deltaText = delta > 0 ? `+${delta}` : delta.toString();

    // 세션 타입 및 상태
    const sessionType = record.sessionType || "";
    const sessionStatus = record.sessionStatus || "";
    const sessionTypeText = sessionType === "ONLINE" ? "온라인" : sessionType
    === "BATTLE" ? "배틀" : sessionType;
    const isCompleted = sessionStatus === "COMPLETED";

    // 참가자 수
    const participants = record.participants || 0;

    // 순위 클래스 및 텍스트 결정
    let rankClass = "other";
    let rankText = "";
    const ranking = record.ranking;

    // ranking이 0이면 "포기"로 표시
    if (ranking === 0 || ranking === null || ranking === undefined) {
      rankClass = "";
      rankText = "포기";
    } else if (ranking === 1) {
      rankClass = "first";
      rankText = "1st";
    } else if (ranking === 2) {
      rankClass = "second";
      rankText = "2nd";
    } else if (ranking === 3) {
      rankClass = "third";
      rankText = "3rd";
    } else {
      // 4 이상의 순위도 서수 형식으로 표시
      const lastDigit = ranking % 10;
      const lastTwoDigits = ranking % 100;

      if (lastTwoDigits >= 11 && lastTwoDigits <= 13) {
        rankText = `${ranking}th`;
      } else if (lastDigit === 1) {
        rankText = `${ranking}st`;
      } else if (lastDigit === 2) {
        rankText = `${ranking}nd`;
      } else if (lastDigit === 3) {
        rankText = `${ranking}rd`;
      } else {
        rankText = `${ranking}th`;
      }
    }

    // 1등 카드인지 확인
    const isFirstPlace = record.ranking === 1;
    let cardClass = isFirstPlace ? "battle-card first-place" : "battle-card";
    if (!isCompleted) {
      cardClass += " incomplete";
    }

    card.className = cardClass;

    // 순위/참가자 수 표시
    const rankParticipantsText = (ranking === 0 || ranking === null || ranking === undefined)
        ? rankText
        : (participants > 0 ? `${record.ranking} / ${participants}` : rankText);

    card.innerHTML = `
      <div class="battle-card-header">
        <div class="battle-card-left">
          <div class="battle-card-title-row">
            ${sessionType
        ? `<span class="session-type-badge ${sessionType.toLowerCase()}">${sessionTypeText}</span>`
        : ""}
            <h3 class="battle-card-title">${distanceValue}km 배틀 결과</h3>
          </div>
          <span class="battle-card-time">${dateTime}</span>
        </div>
        <div class="battle-card-right">
          <span class="battle-rank ${rankClass}">${rankText}</span>
          ${participants > 0
        ? `<span class="battle-participants">${rankParticipantsText}</span>`
        : ""}
        </div>
      </div>
      <div class="battle-card-stats">
        <span class="battle-stat-chip">
          <span class="chip-label">기록</span>
          <span class="chip-value">${time}</span>
        </span>
        <span class="battle-stat-chip">
          <span class="chip-label">페이스</span>
          <span class="chip-value">${pace}</span>
        </span>
        <span class="battle-stat-chip distance-chip">
          <span class="chip-label">거리</span>
          <span class="chip-value">${distanceValue}km</span>
        </span>
      </div>
      <div class="battle-card-footer">
        <div class="rating-flow ${deltaClass}">
          <span class="rating-previous">${previousRating.toLocaleString()} LP</span>
          <span class="rating-arrow">→</span>
          <span class="rating-current">${currentRating.toLocaleString()} LP</span>
          <span class="rating-delta">${delta > 0 ? `+${delta.toLocaleString()}`
        : delta.toLocaleString()}</span>
        </div>
      </div>
    `;

    // 카드 클릭 시 상세 페이지로 이동
    card.addEventListener("click", () => {
      window.location.href = `/match/battleDetail/${record.sessionId}`;
    });

    battleList.appendChild(card);
  }

  // 무한 스크롤 설정 (Intersection Observer)
  function setupInfiniteScroll() {
    if (observer) {
      observer.disconnect();
      observer = null;
    }

    if (!sentinel || !hasNext || !container) {
      return;
    }

    observer = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting && hasNext && !isLoading) {
              loadBattleResults(false);
            }
          });
        },
        {
          root: container,
          rootMargin: "200px",
          threshold: 0.1
        }
    );

    observer.observe(sentinel);
  }

  // 초기화
  loadBattleResults(true);
});

