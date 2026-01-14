/**
 * Match Result - 배틀 결과 페이지
 */

// 전역 변수
let SESSION_ID = null;
let myUserId = null;
let resultData = null;

// localStorage에서 userId 가져오기
const storedUserId = localStorage.getItem('userId');
if (storedUserId) {
  myUserId = parseInt(storedUserId);
  console.log('👤 현재 사용자 ID:', myUserId);
}

// 페이지 로드 시 초기화
document.addEventListener("DOMContentLoaded", () => {
  console.log("🎯 결과 페이지 초기화");
  
  // URL에서 sessionId 가져오기
  const urlParams = new URLSearchParams(window.location.search);
  SESSION_ID = parseInt(urlParams.get('sessionId'));
  
  if (!SESSION_ID) {
    console.error('❌ SESSION_ID가 없습니다!');
    alert('잘못된 접근입니다.');
    window.location.href = '/match/select';
    return;
  }
  
  console.log('📍 Session ID:', SESSION_ID);
  
  // ✅ TTS 초기화 및 타임아웃된 사람을 위한 END_RUN 재생
  initTtsForTimeout();
  
  // 결과 데이터 로드
  loadResultData();
  
  // 이벤트 리스너 설정
  setupEventListeners();
});

/**
 * ✅ 타임아웃된 사람을 위한 TTS 초기화
 */
async function initTtsForTimeout() {
  if (window.TtsManager) {
    try {
      // TTS batch 로드 (mode는 ONLINE으로 설정)
      await window.TtsManager.ensureLoaded({ sessionId: SESSION_ID, mode: "ONLINE" });
      console.log('[match-result] TTS batch loaded');
      
      // ✅ 타임아웃된 사람은 결과 페이지에서 END_RUN 재생
      // (완주한 사람은 이미 handleFinish에서 재생했으므로 여기서는 타임아웃된 사람만)
      setTimeout(() => {
        if (window.TtsManager) {
          window.TtsManager.speak("END_RUN", { priority: 2, cooldownMs: 0 });
        }
      }, 500); // 페이지 로드 후 0.5초 후 재생
    } catch (e) {
      console.warn('[match-result] TTS 초기화 실패 (무시):', e?.message || e);
    }
  }
}

/**
 * 결과 데이터 로드
 */
function loadResultData() {
  const token = localStorage.getItem('accessToken');
  
  console.log('📡 API 호출 시작: /api/battle/' + SESSION_ID + '/result');
  
  fetch('/api/battle/' + SESSION_ID + '/result', {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? 'Bearer ' + token : ''
    }
  })
  .then(response => {
    console.log('📡 API 응답 상태:', response.status, response.statusText);
    if (!response.ok) {
      return response.text().then(text => {
        console.error('❌ API 오류 응답:', text);
        throw new Error('API 호출 실패: ' + response.status + ' - ' + text);
      });
    }
    return response.json();
  })
  .then(data => {
    console.log('✅ API 응답 데이터:', JSON.stringify(data, null, 2));
    
    if (!data || !data.data) {
      throw new Error('데이터 형식 오류: data.data가 없음');
    }
    
    resultData = data.data;
    console.log('📋 결과 데이터 파싱 완료:', resultData);
    
    renderResult(resultData);
    
    // ✅ 러닝 결과 로드 후 광고 팝업 표시 (큰 사이즈 함수 사용)
    setTimeout(async () => {
      try {
        if (typeof loadAd === 'function' && typeof createAdPopupForRunningResult === 'function') {
          const adData = await loadAd('RUN_END_BANNER');
          if (adData) {
            const adPopup = createAdPopupForRunningResult(adData);
            document.body.appendChild(adPopup);
          }
        }
      } catch (error) {
        console.warn('러닝 결과 광고 로드 실패:', error);
      }
    }, 1000);
  })
  .catch(error => {
    console.error('❌ 결과 데이터 로드 실패:', error);
    console.error('❌ 에러 상세:', error.message);
    console.error('❌ 에러 스택:', error.stack);
    
    // ✅ 페이지를 이동시키지 않고 에러 메시지 표시
    showErrorMessage(error.message);
  });
}

/**
 * 에러 메시지 표시
 */
function showErrorMessage(errorMsg) {
  const errorDiv = document.createElement('div');
  errorDiv.style.cssText = `
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    background: rgba(255, 68, 68, 0.95);
    color: white;
    padding: 30px;
    border-radius: 20px;
    text-align: center;
    z-index: 9999;
    max-width: 80%;
  `;
  
  errorDiv.innerHTML = `
    <div style="font-size: 24px; font-weight: 700; margin-bottom: 15px;">❌ 결과 로드 실패</div>
    <div style="font-size: 14px; margin-bottom: 20px;">${errorMsg}</div>
    <button onclick="location.reload()" style="
      background: white;
      color: #ff4444;
      border: none;
      padding: 10px 30px;
      border-radius: 10px;
      font-weight: 600;
      cursor: pointer;
      margin-right: 10px;
    ">재시도</button>
    <button onclick="window.location.href='/match/select'" style="
      background: rgba(255,255,255,0.3);
      color: white;
      border: none;
      padding: 10px 30px;
      border-radius: 10px;
      font-weight: 600;
      cursor: pointer;
    ">돌아가기</button>
  `;
  
  document.body.appendChild(errorDiv);
}

/**
 * 결과 렌더링
 */
function renderResult(data) {
  // 배너 섹션
  renderBanner(data);
  
  // 나의 기록
  renderMyRecord(data);
  
  // 최종 순위
  renderRankings(data.rankings);
  
  // 1위와 비교 (내가 1위가 아니고 완주한 경우만)
  if (data.myRank > 1 && data.myRank !== 0) {
    renderComparison(data);
  } else {
    document.querySelector('.comparison-section').style.display = 'none';
  }
}

/**
 * 배너 렌더링
 */
function renderBanner(data) {
  const targetKm = (data.targetDistance).toFixed(1);  // ✅ 이미 km 단위
  
  // 완료 배지
  document.querySelector('.completion-badge span').textContent = 
    `🏁 ${targetKm}km 스피드 배틀 종료`;
  
  // ✅ 타임아웃 판단: targetDistance는 km, totalDistance는 m
  const targetMeters = data.targetDistance * 1000;  // ✅ km → m 변환
  const isTimeout = data.totalDistance < (targetMeters * 0.9);
  
  // ✅ 순위 표시
  if (data.myRank === 0 || isTimeout) {
    document.querySelector('.rank-number').textContent = '';
    document.querySelector('.rank-text').textContent = '완주 실패';
    document.querySelector('.rank-text').style.left = '50%';
    document.querySelector('.rank-text').style.fontSize = '28px';
  } else {
    document.querySelector('.rank-number').textContent = data.myRank;
    document.querySelector('.rank-text').textContent = '등';
    document.querySelector('.rank-text').style.left = 'calc(50% + 45px)';
    document.querySelector('.rank-text').style.fontSize = '18px';
  }
  
  // 결과 메시지
  const messageText = document.querySelector('.message-text');
  
  if (data.myRank === 0 || isTimeout) {
    // ✅ 미완주자 - 짧고 명확하게
    const reachedKm = (data.totalDistance / 1000).toFixed(2);
    messageText.innerHTML = `<span class="message-muted">목표 거리 미달성 (${reachedKm}km / ${targetKm}km)</span>`;
  } else if (data.myRank === 1) {
    messageText.innerHTML = '<span class="message-muted">축하합니다! </span>1등<span class="message-muted">으로 완주했어요 </span>🏆';
  } else {
    const firstPlace = data.rankings.find(r => r.rank === 1);
    const timeDiff = data.finishTime - firstPlace.finishTime;  // 밀리초
    const diffSeconds = Math.floor(timeDiff / 1000);  // 초
    
    // ✅ 음수 처리: 음수면 오히려 먼저 도착!
    if (diffSeconds < 0) {
      // 음수 = 내가 더 빠름 (순위 버그!)
      const absDiff = Math.abs(diffSeconds);
      if (absDiff >= 60) {
        const minutes = Math.floor(absDiff / 60);
        const seconds = absDiff % 60;
        messageText.innerHTML = 
          `⚠️ ${firstPlace.username}<span class="message-muted">님보다 </span>${minutes}분 ${seconds}초<span class="message-muted"> 빠르게 도착했는데 순위가 잘못되었어요</span>`;
      } else {
        messageText.innerHTML = 
          `⚠️ ${firstPlace.username}<span class="message-muted">님보다 </span>${absDiff}초<span class="message-muted"> 빠르게 도착했는데 순위가 잘못되었어요</span>`;
      }
    } else {
      // 양수 = 정상 (늘게 도착)
      if (diffSeconds >= 60) {
        const minutes = Math.floor(diffSeconds / 60);
        const seconds = diffSeconds % 60;
        messageText.innerHTML = 
          `${firstPlace.username}<span class="message-muted">님보다 </span>${minutes}분 ${seconds}초<span class="message-muted"> 늦게 도착했어요</span>`;
      } else {
        messageText.innerHTML = 
          `${firstPlace.username}<span class="message-muted">님보다 </span>${diffSeconds}초<span class="message-muted"> 늦게 도착했어요</span>`;
      }
    }
  }
}

/**
 * 나의 기록 렌더링
 */
function renderMyRecord(data) {
  // ✅ rankings에서 나의 데이터 찾기 (일관성 유지)
  const myData = data.rankings.find(r => r.userId === myUserId);
  
  // ✅ 타임아웃 판단: targetDistance는 km, totalDistance는 m
  const targetMeters = data.targetDistance * 1000;
  const isTimeout = data.totalDistance < (targetMeters * 0.9);
  
  // ✅ 완주 실패 vs 완주 성공
  if (data.myRank === 0 || isTimeout) {
    // 완주 시간 (실패)
    document.querySelector('.stat-box:nth-child(1) .stat-value').textContent = '-';
    document.querySelector('.stat-box:nth-child(1) .stat-label').textContent = '미완주';
    
    // 평균 페이스 (실패)
    document.querySelector('.stat-box:nth-child(2) .stat-value').textContent = data.avgPace || '-';
    
    // 최대 도달 거리
    const totalKm = (data.totalDistance / 1000).toFixed(2);
    document.querySelector('.stat-box:nth-child(3) .stat-value').textContent = totalKm;
    document.querySelector('.stat-box:nth-child(3) .stat-label').textContent = '최대 도달 거리';
  } else {
    // ✅ rankings에서 가져온 데이터 사용 (일관성 유지)
    const finishTimeStr = myData ? formatTime(myData.finishTime) : formatTime(data.finishTime);
    document.querySelector('.stat-box:nth-child(1) .stat-value').textContent = finishTimeStr;
    document.querySelector('.stat-box:nth-child(1) .stat-label').textContent = '완주 시간';
    
    // 평균 페이스 - rankings에서 가져온 데이터 사용
    const avgPace = myData ? myData.currentPace : data.avgPace;
    document.querySelector('.stat-box:nth-child(2) .stat-value').textContent = avgPace;
    
    // 총 거리 - rankings에서 가져온 데이터 사용
    const totalKm = myData ? (myData.totalDistance / 1000).toFixed(2) : (data.totalDistance / 1000).toFixed(2);
    document.querySelector('.stat-box:nth-child(3) .stat-value').textContent = totalKm;
    document.querySelector('.stat-box:nth-child(3) .stat-label').textContent = '총 거리';
  }
  
  // 종료 날짜
  const now = new Date();
  const dateStr = `${now.getFullYear()}.${String(now.getMonth() + 1).padStart(2, '0')}.${String(now.getDate()).padStart(2, '0')} (${getDayOfWeek(now)}) ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')} 종료`;
  document.querySelector('.record-date').textContent = dateStr;
}

/**
 * 순위 리스트 렌더링
 */
function renderRankings(rankings) {
  const rankingList = document.querySelector('.ranking-list');
  rankingList.innerHTML = '';
  
  rankings.forEach(participant => {
    const isMe = participant.userId === myUserId;
    const rankingItem = createRankingItem(participant, isMe);
    rankingList.appendChild(rankingItem);
  });
}

/**
 * 순위 아이템 생성
 */
function createRankingItem(participant, isMe) {
  const item = document.createElement('div');
  item.className = `ranking-item rank-${participant.rank}`;
  
  // ✅ 타임아웃 판단: targetDistance는 km, totalDistance는 m
  const targetMeters = resultData.targetDistance * 1000;  // ✅ km → m 변환
  const isTimeout = participant.totalDistance < (targetMeters * 0.9);
  
  // ✅ 순위 배지
  const badge = document.createElement('div');
  if (participant.rank === 0 || isTimeout) {
    badge.className = 'rank-badge rank-failed-badge';
    badge.textContent = '❌';
    badge.style.cssText = 'background: rgba(255, 68, 68, 0.2); color: #ff4444;';
  } else {
    badge.className = `rank-badge rank-${participant.rank}-badge`;
    badge.textContent = participant.rank;
  }
  
  // 아바타
  const avatar = document.createElement('div');
  avatar.className = 'participant-avatar';
  
  // ✅ 프로필 이미지 표시 (기본 이미지 포함)
  const avatarImg = document.createElement('img');
  avatarImg.src = participant.profileImage || '/img/default-profile.svg';
  avatarImg.alt = participant.username;
  avatarImg.style.cssText = 'width: 100%; height: 100%; object-fit: cover; border-radius: 50%;';
  avatarImg.onerror = function() {
    this.src = '/img/default-profile.svg';
  };
  avatar.appendChild(avatarImg);
  
  // 참가자 정보
  const info = document.createElement('div');
  info.className = 'participant-info';
  
  const name = document.createElement('div');
  name.className = 'participant-name';
  name.textContent = isMe ? '나' : participant.username;
  
  const status = document.createElement('div');
  status.className = 'participant-status';
  
  // ✅ 타임아웃 vs 완주 성공
  if (participant.rank === 0 || isTimeout) {
    const reachedKm = (participant.totalDistance / 1000).toFixed(2);
    const targetKm = (targetMeters / 1000).toFixed(1);
    status.textContent = `완주 실패 (${reachedKm}km / ${targetKm}km)`;
  } else {
    const finishTimeStr = formatTime(participant.finishTime);
    status.textContent = `${finishTimeStr} 완주${participant.rank === 1 ? ' 🏆' : ''}`;
  }
  
  info.appendChild(name);
  info.appendChild(status);
  
  // 통계
  const stats = document.createElement('div');
  stats.className = 'participant-stats';
  
  const pace = document.createElement('div');
  pace.className = 'participant-pace';
  pace.textContent = `${participant.currentPace} /km`;
  
  const distance = document.createElement('div');
  distance.className = 'participant-distance';
  distance.textContent = `${(participant.totalDistance / 1000).toFixed(2)}km`;
  
  stats.appendChild(pace);
  stats.appendChild(distance);
  
  // 조립
  item.appendChild(badge);
  item.appendChild(avatar);
  item.appendChild(info);
  item.appendChild(stats);
  
  return item;
}

/**
 * 1위와 비교 렌더링
 */
function renderComparison(data) {
  const firstPlace = data.rankings.find(r => r.rank === 1);
  const myData = data.rankings.find(r => r.userId === myUserId);
  
  if (!firstPlace || !myData) {
    document.querySelector('.comparison-section').style.display = 'none';
    return;
  }
  
  // ✅ 내 아바타 업데이트
  const myAvatar = document.querySelector('.user-me');
  const myImg = document.createElement('img');
  myImg.src = myData.profileImage || '/img/default-profile.svg';
  myImg.alt = '나';
  myImg.style.cssText = 'width: 100%; height: 100%; object-fit: cover; border-radius: 50%;';
  myImg.onerror = function() {
    this.src = '/img/default-profile.svg';
  };
  myAvatar.innerHTML = '';
  myAvatar.appendChild(myImg);
  
  // ✅ 1위 아바타 업데이트
  const winnerAvatar = document.querySelector('.user-winner');
  const winnerImg = document.createElement('img');
  winnerImg.src = firstPlace.profileImage || '/img/default-profile.svg';
  winnerImg.alt = firstPlace.username;
  winnerImg.style.cssText = 'width: 100%; height: 100%; object-fit: cover; border-radius: 50%;';
  winnerImg.onerror = function() {
    this.src = '/img/default-profile.svg';
  };
  winnerAvatar.innerHTML = '';
  winnerAvatar.appendChild(winnerImg);
  
  // 헤더 - 이름
  document.querySelector('.user-me + .user-name').textContent = '나';
  document.querySelector('.user-winner + .user-name').textContent = firstPlace.username;
  
  // 완주 시간
  const finishRows = document.querySelectorAll('.comparison-row');
  const myFinishTime = formatTime(myData.finishTime);
  const firstFinishTime = formatTime(firstPlace.finishTime);
  
  finishRows[0].querySelectorAll('.comparison-value')[0].textContent = myFinishTime;
  finishRows[0].querySelectorAll('.comparison-value')[1].textContent = firstFinishTime + ' ✓';
  
  // 평균 페이스
  finishRows[1].querySelectorAll('.comparison-value')[0].textContent = myData.currentPace + ' /km';
  finishRows[1].querySelectorAll('.comparison-value')[1].textContent = firstPlace.currentPace + ' /km ✓';
  
  // 구간 페이스 (서버에서 데이터 없으면 숨기기)
  if (!data.segmentPaces) {
    for (let i = 2; i < finishRows.length; i++) {
      finishRows[i].style.display = 'none';
    }
  }
}

/**
 * 시간 포맷 (밀리초 → MM:SS)
 */
function formatTime(milliseconds) {
  const totalSeconds = Math.floor(milliseconds / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${String(seconds).padStart(2, '0')}`;
}

/**
 * 요일 반환
 */
function getDayOfWeek(date) {
  const days = ['일', '월', '화', '수', '목', '금', '토'];
  return days[date.getDay()];
}

/**
 * 이벤트 리스너 설정
 */
function setupEventListeners() {
  // 뒤로가기
  const backButton = document.getElementById('back-button');
  if (backButton) {
    backButton.addEventListener('click', () => {
      window.location.href = '/match/select';
    });
  }
  
  // 공유하기
  const shareButton = document.getElementById('share-button');
  if (shareButton) {
    shareButton.addEventListener('click', () => {
      if (navigator.share) {
        navigator.share({
          title: 'RUNRUN 대결 결과',
          text: `${resultData.myRank}등으로 완주했어요!`,
          url: window.location.href
        }).catch(err => console.log('공유 실패', err));
      } else {
        navigator.clipboard.writeText(window.location.href).then(() => {
          alert('링크가 클립보드에 복사되었습니다.');
        });
      }
    });
  }
  
  // 홈으로
  const homeButton = document.getElementById('home-button');
  if (homeButton) {
    homeButton.addEventListener('click', () => {
      window.location.href = '/home';
    });
  }
  
  // 재대결
  const rematchButton = document.getElementById('rematch-button');
  if (rematchButton) {
    rematchButton.addEventListener('click', () => {
      window.location.href = `/match/battleDetail/${SESSION_ID}`;
    });
  }
}
