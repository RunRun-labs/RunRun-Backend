/**
 * 런닝 GPS 추적 클래스
 * - 1초마다 GPS 위치 추적
 * - 거리 계산 (Haversine 공식)
 * - WebSocket으로 서버 전송
 *
 * @author : chang
 * @since : 2024-12-23
 */
class RunningTracker {

  constructor(sessionId, userId, stompClient, testMode = false) {
    this.sessionId = sessionId;
    this.userId = userId;
    this.stompClient = stompClient;
    this.testMode = testMode;  // ⭐ 테스트 모드

    // GPS 추적 상태
    this.watchId = null;
    this.isTracking = false;

    // 전송 제어
    this.lastSentTime = 0;
    this.sendInterval = 1000;  // 1초마다 전송

    // 런닝 데이터
    this.totalDistance = 0;     // 총 거리 (km)
    this.startTime = null;       // 시작 시간
    this.lastPosition = null;    // 이전 GPS 위치

    // ⭐ 테스트 모드 전용
    this.testTimer = null;
    this.testStartLat = 37.5289;   // 한강 시작점
    this.testStartLon = 126.9344;
    this.testSpeed = 0.1;           // 100m/초 (매우 빠른 속도로 테스트)

    console.log('🎯 RunningTracker 초기화:', {
      sessionId: this.sessionId,
      userId: this.userId,
      testMode: this.testMode ? '🧪 테스트 모드' : '📍 실제 GPS'
    });
  }

  /**
   * GPS 추적 시작
   */
  startTracking() {
    if (this.isTracking) {
      console.warn('⚠️ 이미 GPS 추적 중입니다');
      return;
    }

    this.isTracking = true;
    this.startTime = Date.now();
    this.totalDistance = 0;
    this.lastPosition = null;
    this.lastSentTime = 0;

    // ⭐ 테스트 모드
    if (this.testMode) {
      console.log('🧪 테스트 모드 - 가상 GPS 시작');
      this.startTestMode();
      return;
    }

    // ⭐ 실제 GPS 모드
    if (!navigator.geolocation) {
      alert('이 브라우저는 GPS를 지원하지 않습니다.');
      return;
    }

    console.log('🚀 실제 GPS 추적 시작');

    this.watchId = navigator.geolocation.watchPosition(
        (position) => this.onGPSUpdate(position),
        (error) => this.onGPSError(error),
        {
          enableHighAccuracy: true,
          timeout: 5000,
          maximumAge: 0
        }
    );
  }

  /**
   * GPS 추적 중지
   */
  stopTracking() {
    if (!this.isTracking) {
      return;
    }

    console.log('🛑 GPS 추적 중지');

    // ⭐ 테스트 모드 타이머 중지
    if (this.testTimer) {
      clearInterval(this.testTimer);
      this.testTimer = null;
    }

    // ⭐ 실제 GPS 중지
    if (this.watchId !== null) {
      navigator.geolocation.clearWatch(this.watchId);
      this.watchId = null;
    }

    this.isTracking = false;
  }

  /**
   * GPS 위치 업데이트 콜백
   */
  onGPSUpdate(position) {
    if (!this.isTracking) {
      console.log('⚠️ GPS 추적이 중지되었습니다. 업데이트 무시.');
      return;
    }
    const now = Date.now();

    // 정확도 필터링 (20미터 이하만 사용)
    if (position.coords.accuracy > 20) {
      console.warn('⚠️ GPS 정확도 낮음:', position.coords.accuracy, 'm');
      return;
    }

    // 거리 계산 (이전 위치가 있을 때만)
    if (this.lastPosition) {
      const distance = this.calculateDistance(
          this.lastPosition.coords.latitude,
          this.lastPosition.coords.longitude,
          position.coords.latitude,
          position.coords.longitude
      );

      // 거리가 너무 크면 무시 (100m 이상 = GPS 오류)
      if (distance < 0.1) {  // 0.1km = 100m
        this.totalDistance += distance;
      } else {
        console.warn('⚠️ GPS 점프 감지:', distance, 'km - 무시');
      }
    }

    // 이전 위치 저장
    this.lastPosition = position;

    // 1초마다 서버로 전송
    if (now - this.lastSentTime >= this.sendInterval) {
      this.sendToServer(position);
      this.lastSentTime = now;
    }
  }

  /**
   * GPS 에러 콜백
   */
  onGPSError(error) {
    console.error('❌ GPS 에러:', error.message);

    switch (error.code) {
      case error.PERMISSION_DENIED:
        alert('GPS 권한이 거부되었습니다. 설정에서 위치 권한을 허용해주세요.');
        this.stopTracking();
        break;
      case error.POSITION_UNAVAILABLE:
        console.warn('⚠️ GPS 위치를 사용할 수 없습니다.');
        break;
      case error.TIMEOUT:
        console.warn('⚠️ GPS 타임아웃');
        break;
    }
  }

  /**
   * 서버로 GPS 데이터 전송
   */
  sendToServer(position) {
    const gpsData = {
      sessionId: this.sessionId,
      userId: this.userId,
      latitude: position.coords.latitude,
      longitude: position.coords.longitude,
      accuracy: position.coords.accuracy,
      timestamp: position.timestamp,
      totalDistance: Math.round(this.totalDistance * 1000) / 1000,  // 소수점 3자리
      runningTime: this.getRunningTime(),
      speed: position.coords.speed || 0
    };

    console.log('📡 GPS 전송:', gpsData.totalDistance, 'km,', gpsData.runningTime,
        '초');

    // WebSocket 전송
    this.stompClient.send('/pub/running/gps', {}, JSON.stringify(gpsData));
  }

  /**
   * 런닝 시간 계산 (초)
   */
  getRunningTime() {
    if (!this.startTime) {
      return 0;
    }
    return Math.floor((Date.now() - this.startTime) / 1000);
  }

  /**
   * 두 GPS 좌표 간 거리 계산 (Haversine 공식)
   * @returns 거리 (km)
   */
  calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371; // 지구 반지름 (km)

    const dLat = this.toRad(lat2 - lat1);
    const dLon = this.toRad(lon2 - lon1);

    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(this.toRad(lat1)) * Math.cos(this.toRad(lat2)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return R * c; // km
  }

  /**
   * 도(degree)를 라디안(radian)으로 변환
   */
  toRad(degrees) {
    return degrees * (Math.PI / 180);
  }

  /**
   * 현재 상태 조회
   */
  getStatus() {
    return {
      isTracking: this.isTracking,
      totalDistance: this.totalDistance,
      runningTime: this.getRunningTime()
    };
  }

  // ============================================
  // ⭐ 테스트 모드 전용 함수
  // ============================================

  /**
   * 테스트 모드 시작
   */
  startTestMode() {
    console.log('🧪 가상 GPS 시묬레이션 시작');
    console.log('📍 시작점:', this.testStartLat, this.testStartLon);
    console.log('🏎️ 속도: 100m/초 (6분/km 페이스)');

    // 1초마다 가상 GPS 데이터 생성
    this.testTimer = setInterval(() => {
      this.generateTestGPSData();
    }, 1000);
  }

  /**
   * 가상 GPS 데이터 생성 (1초마다 100m 이동)
   */
  generateTestGPSData() {
    const now = Date.now();

    // 거리 계산: 1초 = 100m = 0.1km
    this.totalDistance += this.testSpeed;

    // 위도/경도 계산 (북쪽으로 직선 이동)
    // 위도 1도 ≈ 111km
    const kmToLatitude = 1.0 / 111.0;
    const latChange = this.testSpeed * kmToLatitude;

    const currentLat = this.testStartLat + (latChange * (this.totalDistance
        / this.testSpeed));
    const currentLon = this.testStartLon;

    // 가상 GPS 객체 생성
    const testPosition = {
      coords: {
        latitude: currentLat,
        longitude: currentLon,
        accuracy: 10,  // 좋은 정확도
        speed: this.testSpeed * 1000 / 3600  // m/s
      },
      timestamp: now
    };

    // 실제 GPS 콜백과 동일하게 처리
    this.onGPSUpdate(testPosition);
  }
}

// 전역으로 내보내기
window.RunningTracker = RunningTracker;
