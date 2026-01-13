document.addEventListener("DOMContentLoaded", () => {
  const backButton = document.getElementById("backBtn");
  const rankingList = document.getElementById("rankingList");
  const headerBadges = document.getElementById("headerBadges");

  let currentUserId = null;
  let sessionData = null;
  let selectedRowId = null;

  // 뒤로가기 버튼
  if (backButton) {
    backButton.addEventListener("click", () => {
      window.history.length > 1
        ? window.history.back()
        : (window.location.href = "/match/battleList");
    });
  }

  // 현재 로그인한 유저 ID 가져오기
  function getCurrentUserId() {
    const userId = localStorage.getItem("userId");
    if (userId) {
      return parseInt(userId, 10);
    }
    return null;
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

  // 시간 포맷팅 (초를 MM:SS로)
  function formatTime(seconds) {
    if (!seconds && seconds !== 0) return "00:00";
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  }

  // 페이스 포맷팅 (분/km)
  function formatPace(paceMinutes) {
    if (!paceMinutes && paceMinutes !== 0) return "0:00/km";
    const minutes = Math.floor(paceMinutes);
    const seconds = Math.round((paceMinutes - minutes) * 60);
    return `${minutes}:${seconds.toString().padStart(2, "0")}/km`;
  }

  // 날짜 포맷팅 (YYYY.MM.DD HH:mm)
  function formatDateTime(dateString) {
    if (!dateString) return "-";
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${year}.${month}.${day} ${hours}:${minutes}`;
  }

  // 세션 타입 한글 변환
  function getSessionTypeText(sessionType) {
    const typeMap = {
      "ONLINE": "온라인 매칭",
      "OFFLINE": "오프라인 매칭",
      "GHOST": "고스트런",
      "SOLO": "솔로런"
    };
    return typeMap[sessionType] || sessionType;
  }

  // 거리 타입 한글 변환
  function getDistanceText(distanceType) {
    const distanceMap = {
      "KM_3": "3km",
      "KM_5": "5km",
      "KM_10": "10km"
    };
    return distanceMap[distanceType] || distanceType.replace("KM_", "") + "km";
  }

  // 구간별 페이스 막대 그래프 렌더링 (ghost.js 로직 참조)
  function renderPaceChart(splitPace) {
    if (!splitPace || !Array.isArray(splitPace) || splitPace.length === 0) {
      return '<p style="color: var(--text-secondary); font-size: 12px; padding: 12px;">구간별 페이스 데이터가 없습니다.</p>';
    }

    // 새로운 형식: [{"km": 1, "pace": 0.17, "time": 10}, ...]
    const paces = splitPace.map((item, index) => {
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

    if (paces.length === 0) {
      return '<p style="color: var(--text-secondary); font-size: 12px; padding: 12px;">구간별 페이스 데이터가 없습니다.</p>';
    }

    // 최대값과 최소값 찾기 (정규화용)
    const paceValues = paces.map(p => p.pace).filter(p => p > 0);
    if (paceValues.length === 0) {
      return '<p style="color: var(--text-secondary); font-size: 12px; padding: 12px;">구간별 페이스 데이터가 없습니다.</p>';
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
        const normalized = range > 0 ? ((item.pace - minPace) / range) : 0.5;
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

  // 배틀 상세 결과 로드
  async function loadBattleDetail() {
    if (!sessionId) {
      rankingList.innerHTML = '<div style="padding: 40px; text-align: center; color: var(--text-secondary);">세션 ID가 없습니다.</div>';
      return;
    }

    try {
      const token = getToken();
      const headers = {
        "Content-Type": "application/json"
      };

      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }

      const response = await fetch(`/api/battle-result/sessions/${sessionId}/results`, {
        method: "GET",
        headers: headers
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      if (result.success && result.data) {
        sessionData = result.data;
        
        // 현재 로그인한 유저 ID 가져오기
        currentUserId = getCurrentUserId();
        
        // 헤더 배지 정보 표시
        if (headerBadges) {
          const typeText = getSessionTypeText(sessionData.sessionType);
          const distanceText = getDistanceText(sessionData.distanceType);
          headerBadges.innerHTML = `
            <span class="header-badge">${typeText}</span>
            <span class="header-badge-separator">|</span>
            <span class="header-badge">${distanceText}</span>
          `;
        }
        

        if (sessionData.results && sessionData.results.length > 0) {
          renderRankingList();
        } else {
          rankingList.innerHTML = '<div style="padding: 40px; text-align: center; color: var(--text-secondary);">랭킹 데이터가 없습니다.</div>';
        }
      }
    } catch (error) {
      console.error("배틀 상세 결과 로드 실패:", error);
      rankingList.innerHTML = `<div style="padding: 40px; text-align: center; color: var(--text-secondary);">데이터를 불러오는데 실패했습니다.<br>${error.message}</div>`;
    }
  }

  // 랭킹 행 렌더링
  function renderRankingRow(participant) {
    const row = document.createElement("div");
    const isMyRecord = currentUserId !== null && participant.userId === currentUserId;
    const rowId = `ranking-row-${participant.userId}-${participant.ranking}`;
    const isSelected = selectedRowId === rowId;
    
    row.className = `ranking-row ${isMyRecord ? 'my-record' : ''} ${isSelected ? 'selected' : ''}`;
    row.id = rowId;
    row.dataset.userId = participant.userId;

    const time = formatTime(participant.totalTime);
    const pace = formatPace(participant.avgPace);
    const totalDistance = participant.totalDistance ? participant.totalDistance.toFixed(2) : "0.00";
    
    // 레이팅 정보
    const previousRating = participant.previousRating || 0;
    const currentRating = participant.currentRating || 0;
    const delta = participant.delta || 0;
    const deltaClass = delta > 0 ? "positive" : delta < 0 ? "negative" : "zero";
    const deltaText = delta > 0 ? `+${delta}` : delta.toString();

    // splitPace 데이터 확인
    const splitPace = participant.splitPace || null;

    // ranking이 0이거나 null이면 "포기"로 표시
    const rankDisplay = (participant.ranking === 0 || participant.ranking === null || participant.ranking === undefined) ? '포기' : participant.ranking;

    row.innerHTML = `
      <div class="ranking-row-main" onclick="toggleRow('${rowId}')">
        <div class="rank-cell">
          <span class="rank-number">${rankDisplay}</span>
        </div>
        <div class="user-cell">
          <div class="user-info">
            <span class="user-name">${participant.loginId || 'Unknown'}</span>
            ${isMyRecord ? '<span class="my-record-badge">나</span>' : ''}
          </div>
        </div>
        <div class="record-cell">
          <div class="record-time">${time}</div>
        </div>
        <div class="chevron-cell">
          <button class="chevron-button" type="button" aria-label="상세 정보 열기" onclick="event.stopPropagation(); toggleRow('${rowId}');">
            <svg class="chevron-icon ${isSelected ? 'open' : ''}" width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M6 9L12 15L18 9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
      </div>
      <div class="ranking-row-expand">
        <div class="expand-content">
          <div class="expand-user-info">
            <span class="expand-user-name">${participant.loginId || 'Unknown'}</span>
            <a href="/profile/${participant.userId}" class="profile-button" onclick="event.stopPropagation();">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6M15 3h6v6M10 14L21 3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              프로필 보기
            </a>
          </div>
          <div class="expand-stats">
            <span class="stat-chip">
              <span class="stat-label">기록</span>
              <span class="stat-value">${time}</span>
            </span>
            <span class="stat-chip">
              <span class="stat-label">페이스</span>
              <span class="stat-value">${pace}</span>
            </span>
            <span class="stat-chip">
              <span class="stat-label">거리</span>
              <span class="stat-value">${totalDistance}km</span>
            </span>
          </div>
          <div class="rating-history">
            <div class="rating-history-label">레이팅 변화</div>
            <div class="rating-flow ${deltaClass}">
              <span class="rating-previous">${previousRating.toLocaleString()} LP</span>
              <span class="rating-arrow">→</span>
              <span class="rating-current-full">${currentRating.toLocaleString()} LP</span>
              <span class="rating-delta-full">${deltaText}</span>
            </div>
          </div>
          ${splitPace ? renderPaceChart(splitPace) : '<p style="color: var(--text-secondary); font-size: 12px; padding: 12px;">구간별 페이스 데이터가 없습니다.</p>'}
        </div>
      </div>
    `;

    rankingList.appendChild(row);
  }

  // 행 토글 함수 (전역으로 노출)
  window.toggleRow = function(rowId) {
    const row = document.getElementById(rowId);
    if (!row) return;

    // 다른 행 닫기
    if (selectedRowId && selectedRowId !== rowId) {
      const prevRow = document.getElementById(selectedRowId);
      if (prevRow) {
        prevRow.classList.remove("selected");
        const prevExpand = prevRow.querySelector(".ranking-row-expand");
        const prevChevron = prevRow.querySelector(".chevron-icon");
        if (prevExpand) {
          prevExpand.style.maxHeight = null;
        }
        if (prevChevron) {
          prevChevron.classList.remove("open");
        }
      }
    }

    // 현재 행 토글
    if (selectedRowId === rowId) {
      row.classList.remove("selected");
      const expand = row.querySelector(".ranking-row-expand");
      const chevron = row.querySelector(".chevron-icon");
      if (expand) {
        expand.style.maxHeight = null;
      }
      if (chevron) {
        chevron.classList.remove("open");
      }
      selectedRowId = null;
    } else {
      row.classList.add("selected");
      const expand = row.querySelector(".ranking-row-expand");
      const chevron = row.querySelector(".chevron-icon");
      if (expand) {
        expand.style.maxHeight = expand.scrollHeight + "px";
      }
      if (chevron) {
        chevron.classList.add("open");
      }
      selectedRowId = rowId;
    }
  };

  // 랭킹 리스트 렌더링
  function renderRankingList() {
    if (!sessionData || !sessionData.results) {
      rankingList.innerHTML = '<div style="padding: 40px; text-align: center; color: var(--text-secondary);">데이터를 불러올 수 없습니다.</div>';
      return;
    }

    rankingList.innerHTML = "";
    sessionData.results.forEach((participant) => {
      renderRankingRow(participant);
    });
  }

  // 초기화
  loadBattleDetail();
});

