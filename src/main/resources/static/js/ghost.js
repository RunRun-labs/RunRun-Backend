document.addEventListener("DOMContentLoaded", () => {
  const backButton = document.getElementById("backBtn");
  const filterChips = document.getElementById("filterChips");
  const recordsList = document.getElementById("recordsList");
  const loadingSpinner = document.getElementById("loadingSpinner");
  const emptyState = document.getElementById("emptyState");
  const startButton = document.getElementById("startButton");
  const sentinel = document.getElementById("sentinel");
  const container = document.querySelector(".container");

  let currentFilter = "ALL";
  let currentPage = 0;
  let hasNext = true;
  let isLoading = false;
  let selectedRecordId = null;
  let allRecords = []; // 모든 기록을 저장하여 섹션 그룹핑에 사용

  // 필터 타입 정의 (백엔드 RunningResultFilterType Enum과 일치)
  const filterTypes = [
    { value: "ALL", label: "전체" },
    { value: "UNDER_3", label: "3km 이하" },
    { value: "BETWEEN_3_5", label: "3 ~ 5km" },
    { value: "BETWEEN_5_10", label: "5 ~ 10km" },
    { value: "OVER_10", label: "10km 초과" }
  ];

  // 뒤로가기 버튼
  if (backButton) {
    backButton.addEventListener("click", () => {
      window.history.length > 1
        ? window.history.back()
        : (window.location.href = "/match/select");
    });
  }

  // 필터 칩 생성
  function renderFilterChips() {
    filterChips.innerHTML = "";
    filterTypes.forEach((filter) => {
      const chip = document.createElement("button");
      chip.className = `filter-chip ${currentFilter === filter.value ? "active" : ""}`;
      chip.textContent = filter.label;
      chip.dataset.filter = filter.value;
      chip.addEventListener("click", () => {
        currentFilter = filter.value;
        currentPage = 0;
        hasNext = true;
        selectedRecordId = null;
        allRecords = [];
        renderFilterChips();
        updateStartButton();
        // 필터 변경 시 스크롤 위치를 최상단으로 초기화
        if (container) {
          container.scrollTop = 0;
        }
        loadRecords(true);
      });
      filterChips.appendChild(chip);
    });
  }

  // 시간 포맷팅 (초를 mm:ss로)
  function formatTime(seconds) {
    if (!seconds) return "00:00";
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  }

  // 페이스 포맷팅 (분/km)
  function formatPace(paceMinutes) {
    if (!paceMinutes) return "0:00/km";
    const minutes = Math.floor(paceMinutes);
    const seconds = Math.round((paceMinutes - minutes) * 60);
    return `${minutes}:${seconds.toString().padStart(2, "0")}/km`;
  }

  // 거리 포맷팅
  function formatDistance(km) {
    if (!km) return "0.00 km";
    if (km < 1) {
      return `${Math.round(km * 1000)}m`;
    }
    return `${km.toFixed(2)} km`;
  }

  // 날짜 포맷팅 (YYYY.MM.DD (요일) HH:mm)
  function formatDate(dateString) {
    if (!dateString) return "";
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    
    const weekdays = ["일", "월", "화", "수", "목", "금", "토"];
    const weekday = weekdays[date.getDay()];
    
    return `${year}.${month}.${day} (${weekday}) ${hours}:${minutes}`;
  }

  // 섹션 헤더 생성 (이번 주, 지난 주 등)
  function getSectionHeader(dateString) {
    if (!dateString) return "기타";
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = now - date;
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays < 0) return "미래";
    if (diffDays === 0) return "오늘";
    if (diffDays === 1) return "어제";
    if (diffDays < 7) return "이번 주";
    if (diffDays < 14) return "지난 주";
    if (diffDays < 30) return "이번 달";
    if (diffDays < 60) return "지난 달";
    return "더 이전";
  }

  // 기록을 섹션별로 그룹핑
  function groupRecordsBySection(records) {
    const groups = {};
    records.forEach((record) => {
      const section = getSectionHeader(record.startedAt);
      if (!groups[section]) {
        groups[section] = [];
      }
      groups[section].push(record);
    });
    return groups;
  }

  // 최고 기록 찾기 (같은 거리 기준 - 소수점 둘째 자리까지 정확히)
  function findBestRecord(records) {
    if (records.length === 0) return null;
    
    // 거리별로 그룹핑 (소수점 둘째 자리까지 정확히)
    const byDistance = {};
    records.forEach((record) => {
      const distance = record.totalDistance ? parseFloat(record.totalDistance).toFixed(2) : "0.00";
      if (!byDistance[distance]) {
        byDistance[distance] = [];
      }
      byDistance[distance].push(record);
    });

    // 각 거리별로 가장 빠른 시간 찾기
    const bestRecords = {};
    Object.keys(byDistance).forEach((distance) => {
      const recordsForDistance = byDistance[distance];
      const best = recordsForDistance.reduce((prev, curr) => {
        return curr.totalTime < prev.totalTime ? curr : prev;
      });
      bestRecords[best.runningResultId] = true;
    });

    return bestRecords;
  }

  // 토큰 가져오기
  function getToken() {
    return localStorage.getItem("accessToken") || getCookie("accessToken");
  }

  // 쿠키에서 값 가져오기
  function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(";").shift();
    return null;
  }

  // API 호출
  async function fetchRecords(page = 0, filter = "ALL") {
    const token = getToken();
    const headers = {
      "Content-Type": "application/json"
    };

    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    const params = new URLSearchParams({
      filter: filter,
      page: page.toString(),
      size: "10",
      sort: "startedAt,DESC"
    });

    try {
      const response = await fetch(`/api/match/ghost?${params}`, {
        method: "GET",
        headers: headers
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      if (result.success && result.data) {
        return result.data;
      }
      throw new Error(result.message || "데이터를 불러오는데 실패했습니다.");
    } catch (error) {
      console.error("기록 조회 실패:", error);
      throw error;
    }
  }

  // 고스트런 시작
  async function startGhostRun() {
    if (!selectedRecordId) return;

    const token = getToken();
    if (!token) {
      alert("로그인이 필요합니다.");
      window.location.href = "/login";
      return;
    }

    const headers = {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${token}`
    };

    try {
      startButton.disabled = true;
      startButton.textContent = "시작 중...";

      const response = await fetch(`/api/match/ghost/${selectedRecordId}/start`, {
        method: "POST",
        headers: headers
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || "고스트런 시작에 실패했습니다.");
      }

      const result = await response.json();
      if (result.success && result.data) {
        const sessionId = result.data;
        // 실시간 고스트런 페이지로 이동
        window.location.href = `/match/ghost-run?sessionId=${sessionId}`;
      }
    } catch (error) {
      console.error("고스트런 시작 실패:", error);
      alert(error.message || "고스트런 시작에 실패했습니다.");
      startButton.disabled = false;
      startButton.textContent = "고스트런 시작하기";
    }
  }

  // 구간별 페이스 막대 그래프 렌더링 (모든 구간 표시, 5개씩 한 줄)
  function renderPaceChart(splitPace) {
    if (!splitPace || !Array.isArray(splitPace) || splitPace.length === 0) {
      return '<p style="color: var(--text-secondary); font-size: 12px;">구간별 페이스 데이터가 없습니다.</p>';
    }

    // 새로운 형식: [{"km": 1, "pace": 0.17, "time": 10}, ...]
    // 페이스 값 추출 (km, pace 속성 사용)
    const paces = splitPace.map((item, index) => {
      // km 값이 있으면 사용하고, 없으면 인덱스 + 1 사용
      let km = item.km;
      if (km === undefined || km === null) {
        km = index + 1;
      } else {
        km = typeof km === 'number' ? km : parseInt(km, 10) || (index + 1);
      }
      const pace = item.pace || 0;
      return {
        km: km,
        pace: typeof pace === 'number' ? pace : parseFloat(pace) || 0
      };
    });

    // 모든 구간 표시 (5km 초과 시에도 모두 표시)
    if (paces.length === 0) {
      return '<p style="color: var(--text-secondary); font-size: 12px;">구간별 페이스 데이터가 없습니다.</p>';
    }


    // 최대값과 최소값 찾기 (정규화용)
    const paceValues = paces.map(p => p.pace).filter(p => p > 0);
    if (paceValues.length === 0) {
      return '<p style="color: var(--text-secondary); font-size: 12px;">구간별 페이스 데이터가 없습니다.</p>';
    }
    const maxPace = Math.max(...paceValues);
    const minPace = Math.min(...paceValues);
    const range = maxPace - minPace || 1;

    // 5개씩 한 줄로 나누기
    const rows = [];
    for (let i = 0; i < paces.length; i += 5) {
      rows.push(paces.slice(i, i + 5));
    }

    // 각 줄별로 막대 그래프 생성
    const barsRows = rows.map((row) => {
      const bars = row.map((item) => {
        // 페이스가 높을수록(느릴수록) 높은 막대 (최소 페이스 = 최소 높이, 최대 페이스 = 최대 높이)
        // 범위가 작을 때는 높이 차이를 줄이기 위해 최소/최대 높이 범위를 좁힘
        const normalized = range > 0 ? ((item.pace - minPace) / range) : 0.5;
        // 최소 45%, 최대 60% 높이로 조정하여 비슷한 값일 때 차이를 줄임
        // 페이스가 높을수록(느릴수록) normalized가 커지므로 높이도 높아짐
        const height = 45 + (normalized * 15);
        const paceFormatted = formatPace(item.pace);
        return `
          <div class="pace-bar-item">
            <div class="pace-bar-container">
              <div class="pace-bar-fill" style="height: ${height}%">
                <span class="pace-bar-value">${paceFormatted}</span>
              </div>
            </div>
            <span class="pace-bar-label">${item.km}km</span>
          </div>
        `;
      }).join("");
      return `<div class="pace-bars-row">${bars}</div>`;
    }).join("");

    return `
      <div class="pace-chart">
        <div class="pace-chart-title">구간별 페이스</div>
        <div class="pace-bars-container">
          ${barsRows}
        </div>
      </div>
    `;
  }

  // 기록 카드 렌더링
  function renderRecordCard(record, isBest = false) {
    const card = document.createElement("div");
    card.className = `record-card ${selectedRecordId === record.runningResultId ? "selected" : ""}`;
    card.dataset.recordId = record.runningResultId;

    const totalTimeFormatted = formatTime(record.totalTime);
    const avgPaceFormatted = formatPace(record.avgPace);
    const distanceFormatted = formatDistance(record.totalDistance);
    const dateFormatted = formatDate(record.startedAt);

    // 거리 기반 타이틀 생성 (실제 거리 사용, 소수점 둘째 자리까지)
    const distance = record.totalDistance ? parseFloat(record.totalDistance).toFixed(2) : "0.00";
    const title = `${distance}km 최고 기록`;

    card.innerHTML = `
      <div class="record-card-header">
        <div class="record-card-left">
          <svg class="record-icon" width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M10 2L3 6V16L10 12L17 16V6L10 2Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" fill="none"/>
          </svg>
          <div class="record-title-row">
            <h3 class="record-title">${title}</h3>
            ${isBest ? '<span class="best-badge">BEST</span>' : ''}
          </div>
          <div class="record-date">${dateFormatted}</div>
          <div class="record-stats">
            <div class="stat-item">
              <svg class="stat-icon" width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="8" cy="8" r="7" stroke="currentColor" stroke-width="1.5"/>
                <path d="M8 4V8L11 11" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
              <span class="stat-value">${totalTimeFormatted}</span>
            </div>
            <div class="stat-item">
              <svg class="stat-icon" width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M8 2L10 6L14 7L11 10L11.5 14L8 12L4.5 14L5 10L2 7L6 6L8 2Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" fill="none"/>
              </svg>
              <span class="stat-value">${avgPaceFormatted}</span>
            </div>
            <div class="stat-item">
              <svg class="stat-icon" width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M2 8L6 4L10 8L14 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M2 12L6 8L10 12L14 8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span class="stat-value">${distanceFormatted}</span>
            </div>
          </div>
        </div>
        <div class="record-radio">
          <div class="record-radio-inner"></div>
        </div>
      </div>
      <div class="record-expand">
        ${renderPaceChart(record.splitPace)}
      </div>
    `;

    // 카드 클릭 이벤트
    card.addEventListener("click", () => {
      // 기존 선택 해제
      const prevSelected = document.querySelector(".record-card.selected");
      if (prevSelected) {
        prevSelected.classList.remove("selected");
      }

      // 새 선택
      if (selectedRecordId === record.runningResultId) {
        selectedRecordId = null;
        card.classList.remove("selected");
      } else {
        selectedRecordId = record.runningResultId;
        card.classList.add("selected");
      }

      updateStartButton();
    });

    return card;
  }

  // 시작 버튼 업데이트
  function updateStartButton() {
    if (selectedRecordId) {
      startButton.disabled = false;
      startButton.classList.add("active");
    } else {
      startButton.disabled = true;
      startButton.classList.remove("active");
    }
  }

  // 시작 버튼 이벤트
  if (startButton) {
    startButton.addEventListener("click", startGhostRun);
  }

  // 기록 목록 렌더링 (섹션별 그룹핑)
  function renderRecordsList() {
    if (allRecords.length === 0) {
      emptyState.style.display = "flex";
      recordsList.innerHTML = "";
      return;
    }

    emptyState.style.display = "none";
    recordsList.innerHTML = "";

    // BEST 기록 찾기
    const bestRecords = findBestRecord(allRecords);

    // 섹션별로 그룹핑
    const grouped = groupRecordsBySection(allRecords);
    const sections = Object.keys(grouped).sort((a, b) => {
      // 섹션 순서 정렬
      const order = ["오늘", "어제", "이번 주", "지난 주", "이번 달", "지난 달", "더 이전", "기타"];
      const aIndex = order.indexOf(a);
      const bIndex = order.indexOf(b);
      if (aIndex === -1 && bIndex === -1) return 0;
      if (aIndex === -1) return 1;
      if (bIndex === -1) return -1;
      return aIndex - bIndex;
    });

    sections.forEach((section) => {
      // 섹션 헤더
      const sectionHeader = document.createElement("div");
      sectionHeader.className = "section-header";
      sectionHeader.textContent = section;
      recordsList.appendChild(sectionHeader);

      // 섹션 내 기록들
      grouped[section].forEach((record) => {
        const isBest = bestRecords[record.runningResultId] || false;
        const card = renderRecordCard(record, isBest);
        recordsList.appendChild(card);
      });
    });
  }

  // 기록 목록 로드
  async function loadRecords(reset = false) {
    // 중복 호출 방지 및 데이터 체크
    if (isLoading) {
      return;
    }
    if (!hasNext && !reset) {
      return;
    }

    isLoading = true;

    if (reset) {
      allRecords = [];
      currentPage = 0;
      hasNext = true;
      selectedRecordId = null;
      updateStartButton();
    }

    loadingSpinner.style.display = "flex";
    emptyState.style.display = "none";

    try {
      const data = await fetchRecords(currentPage, currentFilter);
      // Slice 응답 구조: content 배열과 hasNext boolean
      const records = Array.isArray(data) ? data : (data.content || []);
      hasNext = data.hasNext !== undefined ? data.hasNext : (records.length >= 10);

      if (records.length === 0 && reset) {
        emptyState.style.display = "flex";
        allRecords = [];
      } else {
        allRecords = [...allRecords, ...records];
        currentPage++;
      }

      renderRecordsList();
    } catch (error) {
      console.error("기록 로드 실패:", error);
      if (reset) {
        emptyState.style.display = "flex";
        emptyState.innerHTML = `<p>기록을 불러오는데 실패했습니다.<br>${error.message}</p>`;
        allRecords = [];
      }
    } finally {
      loadingSpinner.style.display = "none";
      isLoading = false;
      // 데이터 로드 후 Intersection Observer 재설정
      setupInfiniteScroll();
    }
  }

  // Intersection Observer를 사용한 무한 스크롤
  let observer = null;
  
  function setupInfiniteScroll() {
    // 기존 observer가 있으면 해제
    if (observer) {
      observer.disconnect();
    }

    // sentinel이 없거나 hasNext가 false면 observer 생성하지 않음
    if (!sentinel || !hasNext) {
      return;
    }

    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting && !isLoading && hasNext) {
            loadRecords(false);
          }
        });
      },
      {
        root: container, // 스크롤 컨테이너
        rootMargin: "100px", // 100px 전에 미리 로드
        threshold: 0.1
      }
    );

    observer.observe(sentinel);
  }

  // 초기화
  renderFilterChips();
  loadRecords(true);
  
  // 초기 Intersection Observer 설정
  setupInfiniteScroll();
});

