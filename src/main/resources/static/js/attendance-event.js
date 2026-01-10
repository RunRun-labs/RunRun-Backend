// ========================================
// 출석 이벤트 페이지 JavaScript
// ========================================

document.addEventListener('DOMContentLoaded', async () => {
    console.log('✅ 출석 이벤트 페이지 로드');

    // JWT 토큰 가져오기
    const token = localStorage.getItem('accessToken');

    if (!token) {
        console.log('❌ JWT 토큰 없음 → 로그인 페이지로 이동');
        alert('로그인이 필요한 서비스입니다.');
        window.location.href = '/login';
        return;
    }

    console.log('✅ JWT 토큰 확인:', token.substring(0, 20) + '...');

    // DOM 요소
    const calendarMonthEl = document.querySelector('[data-role="calendar-month"]');
    const monthlyCountEl = document.querySelector('[data-role="monthly-count"]');
    const calendarDaysEl = document.querySelector('[data-role="calendar-days"]');
    const attendanceButton = document.querySelector('[data-role="attendance-button"]');

    const popupOverlay = document.querySelector('[data-role="popup-overlay"]');
    const popupTitle = document.querySelector('[data-role="popup-title"]');
    const popupMessage = document.querySelector('[data-role="popup-message"]');
    const popupClose = document.querySelector('[data-role="popup-close"]');

    // 현재 날짜 정보
    const today = new Date();
    const currentYear = today.getFullYear();
    const currentMonth = today.getMonth() + 1;
    const currentDay = today.getDate();

    // 출석 현황 로드
    await loadAttendanceStatus();

    // 출석 버튼 클릭
    attendanceButton.addEventListener('click', handleAttendanceCheck);

    // 팝업 닫기
    popupClose.addEventListener('click', closePopup);
    popupOverlay.addEventListener('click', (e) => {
        if (e.target === popupOverlay) {
            closePopup();
        }
    });

    // ========================================
    // 출석 현황 조회
    // ========================================
    async function loadAttendanceStatus() {
        try {
            console.log('📡 출석 현황 조회 시작...');

            const response = await fetch('/api/users/attendance/status', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                }
            });

            console.log('📡 응답 상태:', response.status);

            if (response.status === 401) {
                console.log('❌ 인증 만료');
                alert('로그인이 만료되었습니다. 다시 로그인해주세요.');
                localStorage.removeItem('accessToken');
                window.location.href = '/login';
                return;
            }

            if (!response.ok) {
                throw new Error(`출석 현황 조회 실패: ${response.status}`);
            }

            const data = await response.json();
            console.log('📊 출석 현황:', data);

            // 달력 렌더링 (매달 자동 변경!)
            renderCalendar(data.currentYear, data.currentMonth, data.attendedDays, data.attendedToday);

            // 출석 횟수 표시
            monthlyCountEl.textContent = data.monthlyCount;

            // 월 표시 (매달 자동 변경!)
            calendarMonthEl.textContent = `${data.currentMonth}월`;

            // 버튼 상태 업데이트
            if (data.attendedToday) {
                attendanceButton.textContent = '출석 완료';
                attendanceButton.classList.add('completed');
            }

        } catch (error) {
            console.error('❌ 출석 현황 조회 실패:', error);

            console.log('⚠️ 기본 달력 표시');
            const today = new Date();
            renderCalendar(today.getFullYear(), today.getMonth() + 1, [], false);
            monthlyCountEl.textContent = '0';
            calendarMonthEl.textContent = `${today.getMonth() + 1}월`;

            showErrorPopup('출석 현황을 불러올 수 없습니다.\n나중에 다시 시도해주세요.');
        }
    }

    // ========================================
    // 달력 렌더링 (매달 자동 변경!)
    // ========================================
    function renderCalendar(year, month, attendedDays = [], attendedToday = false) {
        calendarDaysEl.innerHTML = '';

        // 이번 달 첫날의 요일 (0=일요일, 6=토요일)
        const firstDay = new Date(year, month - 1, 1).getDay();

        // 이번 달 마지막 날
        const lastDate = new Date(year, month, 0).getDate();

        // 현재 날짜 (오늘)
        const todayDate = new Date();
        const isCurrentMonth = (todayDate.getFullYear() === year && todayDate.getMonth() + 1 === month);
        const todayDay = isCurrentMonth ? todayDate.getDate() : -1;

        // 빈 칸 채우기 (첫날 이전)
        for (let i = 0; i < firstDay; i++) {
            const emptyDiv = document.createElement('div');
            emptyDiv.className = 'calendar-day empty';
            calendarDaysEl.appendChild(emptyDiv);
        }

        // 날짜 채우기
        for (let day = 1; day <= lastDate; day++) {
            const dayDiv = document.createElement('div');
            dayDiv.className = 'calendar-day';
            dayDiv.textContent = day;

            // 오늘 날짜 표시 (현재 월일 때만)
            if (day === todayDay) {
                dayDiv.classList.add('today');
            }

            // 출석한 날짜 표시
            if (attendedDays.includes(day)) {
                dayDiv.classList.add('attended');
            }

            calendarDaysEl.appendChild(dayDiv);
        }
    }

    // ========================================
    // 출석 체크
    // ========================================
    async function handleAttendanceCheck() {

        if (attendanceButton.classList.contains('completed')) {
            showAlreadyAttendedPopup();
            return;
        }

        // 버튼 비활성화 (중복 클릭 방지)
        attendanceButton.disabled = true;

        try {
            console.log('📡 출석 체크 시작...');

            const response = await fetch('/api/users/attendance', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                }
            });

            console.log('📡 응답 상태:', response.status);

            if (response.status === 401) {
                console.log('❌ 인증 만료');
                alert('로그인이 만료되었습니다. 다시 로그인해주세요.');
                localStorage.removeItem('accessToken');
                window.location.href = '/login';
                return;
            }

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || '출석 체크 실패');
            }

            const data = await response.json();
            console.log('🎉 출석 완료:', data);

            showSuccessPopup(data.message, data.pointsEarned);

            // 달력 새로고침
            await loadAttendanceStatus();

        } catch (error) {
            console.error('❌ 출석 체크 실패:', error);

            // 에러 종류에 따라 다른 메시지
            if (error.message.includes('이미 출석')) {
                showAlreadyAttendedPopup();
                attendanceButton.textContent = '출석 완료';
                attendanceButton.classList.add('completed');
            } else {
                showErrorPopup('출석 체크에 실패했습니다.\n다시 시도해주세요.');
                attendanceButton.disabled = false;
            }
        }
    }

    // ========================================
// 이미 출석한 경우 팝업
// ========================================
    function showAlreadyAttendedPopup() {
        popupTitle.textContent = '알림';
        popupMessage.textContent = '이미 출석 이벤트에 참가했습니다.';

        const popupIcon = document.querySelector('.popup-icon');
        popupIcon.innerHTML = `
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="80" height="80">
            <circle cx="50" cy="50" r="45" fill="#4CAF50" stroke="#2E7D32" stroke-width="3"/>
            <path d="M30 50 L42 62 L70 34" 
                  stroke="white" 
                  stroke-width="6" 
                  stroke-linecap="round" 
                  stroke-linejoin="round" 
                  fill="none"/>
        </svg>
    `;

        popupOverlay.hidden = false;
    }

    // ========================================
    // 성공 팝업
    // ========================================
    function showSuccessPopup(message, points) {
        popupTitle.textContent = '출석 완료!';
        popupMessage.textContent = `50P가 적립되었습니다.`;

        // ✨ 2번 이미지처럼 "출석체크" 각인 도장 + 쾅! 효과
        const popupIcon = document.querySelector('.popup-icon');
        popupIcon.innerHTML = `
        <div style="position: relative; display: inline-block; width: 120px; height: 120px;">
            <!-- 쾅! 효과선 -->
            <svg style="position: absolute; top: -10px; left: -10px; width: 140px; height: 140px; animation: stamp-impact 0.4s ease-out;" 
                 viewBox="0 0 140 140" xmlns="http://www.w3.org/2000/svg">
                <circle cx="70" cy="70" r="65" fill="none" stroke="#FFD700" stroke-width="3" opacity="0.6"/>
                <circle cx="70" cy="70" r="55" fill="none" stroke="#FFA500" stroke-width="2" opacity="0.8"/>
                <line x1="70" y1="5" x2="70" y2="15" stroke="#FFD700" stroke-width="2"/>
                <line x1="70" y1="125" x2="70" y2="135" stroke="#FFD700" stroke-width="2"/>
                <line x1="5" y1="70" x2="15" y2="70" stroke="#FFD700" stroke-width="2"/>
                <line x1="125" y1="70" x2="135" y2="70" stroke="#FFD700" stroke-width="2"/>
                <line x1="20" y1="20" x2="28" y2="28" stroke="#FFA500" stroke-width="2"/>
                <line x1="112" y1="112" x2="120" y2="120" stroke="#FFA500" stroke-width="2"/>
                <line x1="112" y1="20" x2="120" y2="28" stroke="#FFA500" stroke-width="2"/>
                <line x1="20" y1="112" x2="28" y2="120" stroke="#FFA500" stroke-width="2"/>
            </svg>
            
            <!-- 메인 도장 -->
            <svg style="position: absolute; top: 10px; left: 10px; width: 100px; height: 100px; animation: stamp-rotation 0.5s ease-out;" 
                 viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
                <!-- 외곽 원 (이중 테두리) -->
                <circle cx="100" cy="100" r="95" fill="#E63946" stroke="#A4161A" stroke-width="4"/>
                <circle cx="100" cy="100" r="88" fill="none" stroke="#F1FAEE" stroke-width="2"/>
                <circle cx="100" cy="100" r="80" fill="none" stroke="#F1FAEE" stroke-width="2"/>
                
                <!-- 별 장식 -->
                <path d="M100,30 L105,45 L120,45 L108,55 L113,70 L100,60 L87,70 L92,55 L80,45 L95,45 Z" 
                      fill="#F1FAEE" opacity="0.8"/>
                <circle cx="50" cy="70" r="3" fill="#F1FAEE"/>
                <circle cx="150" cy="70" r="3" fill="#F1FAEE"/>
                <circle cx="40" cy="100" r="2" fill="#F1FAEE"/>
                <circle cx="160" cy="100" r="2" fill="#F1FAEE"/>
                
                <!-- "출석체크" 텍스트 (각인 느낌) -->
                <text x="100" y="105" 
                      font-family="'Noto Sans KR', Arial, sans-serif" 
                      font-size="28" 
                      font-weight="900" 
                      fill="#F1FAEE" 
                      text-anchor="middle"
                      letter-spacing="2">출석체크</text>
                
                <!-- 텍스트 그림자 효과 -->
                <text x="101" y="106" 
                      font-family="'Noto Sans KR', Arial, sans-serif" 
                      font-size="28" 
                      font-weight="900" 
                      fill="#A4161A" 
                      text-anchor="middle"
                      letter-spacing="2"
                      opacity="0.3">출석체크</text>
            </svg>
        </div>
    `;

        popupOverlay.hidden = false;
    }

    // ========================================
    // 팝업 닫기
    // ========================================
    function closePopup() {
        popupOverlay.hidden = true;
    }
});