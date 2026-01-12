/**
 * 고스트런 결과 페이지
 */

// URL에서 sessionId 가져오기
const urlParams = new URLSearchParams(window.location.search);
const SESSION_ID = urlParams.get('sessionId');

// DOM Elements
const resultStatus = document.getElementById('resultStatus');
const resultMessage = document.getElementById('resultMessage');

const myTime = document.getElementById('myTime');
const myPace = document.getElementById('myPace');
const myDistance = document.getElementById('myDistance');

const ghostTime = document.getElementById('ghostTime');
const ghostPace = document.getElementById('ghostPace');
const ghostDistance = document.getElementById('ghostDistance');

const finalDifference = document.getElementById('finalDifference');
const ghostDate = document.getElementById('ghostDate');
const compareMethod = document.getElementById('compareMethod');

const backButton = document.getElementById('backButton');
const homeButton = document.getElementById('homeButton');
const retryButton = document.getElementById('retryButton');

// 데이터
let myResult = null;
let ghostResult = null;

/**
 * 초기화
 */
async function init() {
  if (!SESSION_ID) {
    alert('잘못된 접근입니다.');
    window.location.href = '/match/select';
    return;
  }

  await loadResults();
}

/**
 * 결과 데이터 로드
 */
async function loadResults() {
  const token = getToken();

  try {
    // 세션 정보 조회 (고스트 기록 포함)
    const sessionResponse = await fetch(
        `/api/match/ghost/session/${SESSION_ID}`, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });

    if (!sessionResponse.ok) {
      throw new Error('세션 조회 실패');
    }

    const sessionData = await sessionResponse.json();
    ghostResult = sessionData.data.ghostRecord;

    // 내 러닝 결과 조회 (TODO: API 필요)
    // 임시로 localStorage에서 가져오기
    const myTotalDistance = parseFloat(
        localStorage.getItem('ghost_my_distance') || '0');
    const myTotalTime = parseInt(localStorage.getItem('ghost_my_time') || '0');
    const myTimeDiff = parseInt(localStorage.getItem('ghost_time_diff') || '0');
    const myStatus = localStorage.getItem('ghost_status') || 'EVEN';

    myResult = {
      totalDistance: myTotalDistance,
      totalTime: myTotalTime,
      avgPace: myTotalTime / myTotalDistance, // 초/km
      timeDiff: myTimeDiff,
      status: myStatus
    };

    console.log('✅ 결과 로드:', {myResult, ghostResult});

    updateUI();

  } catch (error) {
    console.error('❌ 결과 로드 실패:', error);
    alert('결과를 불러오는데 실패했습니다.');
  }
}

/**
 * UI 업데이트
 */
function updateUI() {
  // 1. 승패 판정
  updateResultBanner();

  // 2. 내 기록
  myDistance.textContent = `${myResult.totalDistance.toFixed(2)} km`;
  myTime.textContent = formatTime(myResult.totalTime);
  myPace.textContent = formatPace(myResult.avgPace);

  // 3. 고스트 기록
  ghostDistance.textContent = `${ghostResult.totalDistance} km`;
  ghostTime.textContent = formatTime(ghostResult.totalTime);
  ghostPace.textContent = formatPace(ghostResult.avgPace);

  // 4. 최종 차이
  updateFinalDifference();

  // 5. 고스트 정보
  ghostDate.textContent = formatDate(ghostResult.startedAt);
  compareMethod.textContent = ghostResult.splitPace
  && ghostResult.splitPace.length > 0
      ? 'km별 정밀 비교'
      : '평균 페이스 비교';
}

/**
 * 결과 배너 업데이트
 */
function updateResultBanner() {
  const {status, timeDiff} = myResult;

  if (status === 'AHEAD') {
    // 승리
    resultStatus.innerHTML = '<span class="win">🏆 승리!</span>';
    resultMessage.innerHTML = '<span>고스트를 이겼습니다!</span>';
  } else if (status === 'BEHIND') {
    // 패배
    resultStatus.innerHTML = '<span class="lose">💪 패배</span>';
    resultMessage.innerHTML = '<span>다음엔 더 잘할 수 있어요!</span>';
  } else {
    // 무승부
    resultStatus.innerHTML = '<span>⚡ 동률!</span>';
    resultMessage.innerHTML = '<span>고스트와 동점입니다!</span>';
  }
}

/**
 * 최종 차이 업데이트
 */
function updateFinalDifference() {
  const {status, timeDiff} = myResult;
  const diffElement = finalDifference.querySelector('.difference-value');

  if (status === 'AHEAD') {
    finalDifference.classList.add('win');
    diffElement.textContent = `${Math.abs(timeDiff)}초 빠름! 🔥`;
  } else if (status === 'BEHIND') {
    finalDifference.classList.add('lose');
    diffElement.textContent = `${Math.abs(timeDiff)}초 느림`;
  } else {
    diffElement.textContent = '동률!';
  }
}

/**
 * 시간 포맷 (초 → MM:SS)
 */
function formatTime(seconds) {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2,
      '0')}`;
}

/**
 * 페이스 포맷 (초/km → M:SS/km)
 */
function formatPace(pace) {
  const mins = Math.floor(pace / 60);
  const secs = Math.floor(pace % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}/km`;
}

/**
 * 날짜 포맷
 */
function formatDate(dateString) {
  const date = new Date(dateString);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}.${month}.${day}`;
}

/**
 * 토큰 가져오기
 */
function getToken() {
  return localStorage.getItem('accessToken') || getCookie('accessToken');
}

function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) {
    return parts.pop().split(';').shift();
  }
  return null;
}

/**
 * 버튼 이벤트
 */
backButton.addEventListener('click', () => {
  window.history.back();
});

homeButton.addEventListener('click', () => {
  // localStorage 정리
  localStorage.removeItem('ghost_my_distance');
  localStorage.removeItem('ghost_my_time');
  localStorage.removeItem('ghost_time_diff');
  localStorage.removeItem('ghost_status');

  window.location.href = '/match/select';
});

retryButton.addEventListener('click', () => {
  // localStorage 정리
  localStorage.removeItem('ghost_my_distance');
  localStorage.removeItem('ghost_my_time');
  localStorage.removeItem('ghost_time_diff');
  localStorage.removeItem('ghost_status');

  // 고스트 목록 조회 페이지로
  window.location.href = '/match/ghost?mode=select';
});

// 페이지 로드 시 초기화
window.addEventListener('DOMContentLoaded', init);
