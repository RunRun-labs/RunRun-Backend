/**
 * RunningTracker (단일 정본)
 * - GPS watchPosition으로 위치 추적
 * - 이동 거리 누적(점프 필터 포함)
 * - 진행 방향(heading) 계산/전송
 * - WebSocket(/pub/running/gps)으로 서버 전송
 *
 * NOTE:
 * - running.js에서 onGPSUpdate를 오버라이드해서 지도/경로 트리밍에 사용함.
 */
class RunningTracker {
  constructor(sessionId, userId, stompClient, testMode = false) {
    this.sessionId = sessionId;
    this.userId = userId;
    this.stompClient = stompClient;
    this.testMode = testMode;

    // GPS 상태
    this.watchId = null;
    this.isTracking = false;

    // 전송 제어
    this.lastSentTime = 0;
    this.sendInterval = 500; // 0.5초

    // 런닝 데이터 (km, sec)
    this.totalDistance = 0;
    this.startTime = null; // epoch ms
    this.lastPosition = null;
    this._bootstrapped = false;

    // heading (deg, 0=N)
    this.heading = null;

    // 코스 위 매칭 진행도(미터) - running.js가 계산해서 주입
    this.matchedDistanceM = null;
    // 호스트만 코스 매칭 진행도를 전송해야 한다(참여자가 0을 보내서 덮어쓰는 사고 방지)
    this.includeMatchedDistanceM = false;

    // 필터
    // ✅ 모바일 실환경에서 35m는 너무 빡세서 전송이 거의 드랍될 수 있음
    // - 러닝앱 UX 기준: "선 지우기/통계"가 멈추는 것보다 약간의 오차가 낫다.
    // GPS accuracy가 너무 나쁘면(튀면) 아예 전송/누적에서 제외
    this.maxAccuracyM = 35;
    this.maxJumpKm = 0.05; // 50m 이상은 점프(무시)

    // 속도 제한(경고용)
    // - 하드: 8.5m/s(30.6km/h) 이상은 거의 GPS 점프/차량 → 즉시 경고
    // - 소프트: 6.0m/s(21.6km/h) 이상이 3회 연속이면 경고
    this.tooFastHardMps = 8.5;
    this.tooFastSoftMps = 6.0;
    this.tooFastSoftCount = 0;
    this.tooFastAlertCooldownMs = 15000;
    this.lastTooFastAlertAt = 0;

    // 테스트 모드
    this.testTimer = null;
    this.testStartLat = 37.5289;
    this.testStartLon = 126.9344;
    this.testSpeedKmPerSec = 0.1; // 100m/s
  }

  /**
   * 재진입/새로고침 시 이어달리기용 초기값 주입
   * - totalDistanceKm, runningTimeSec 기반으로 startTime을 되돌려서 이후 시간/거리 계속 증가
   */
  bootstrap(initialDistanceKm, initialRunningTimeSec) {
    const d = Number(initialDistanceKm);
    const t = Number(initialRunningTimeSec);
    const safeD = Number.isFinite(d) ? Math.max(0, d) : 0;
    const safeT = Number.isFinite(t) ? Math.max(0, t) : 0;

    this.totalDistance = safeD;
    this.startTime = Date.now() - safeT * 1000;
    this._bootstrapped = true;
  }

  startTracking() {
    if (this.isTracking) {
      console.warn("⚠️ 이미 GPS 추적 중입니다");
      return;
    }

    this.isTracking = true;

    // bootstrap()이 불렸다면 거리/시간은 유지
    if (!this._bootstrapped || this.startTime == null) {
      this.startTime = Date.now();
      this.totalDistance = 0;
    }
    this.lastPosition = null;
    this.lastSentTime = 0;
    this.matchedDistanceM = null;

    if (this.testMode) {
      this.startTestMode();
      return;
    }

    if (!navigator.geolocation) {
      alert("이 브라우저는 GPS를 지원하지 않습니다.");
      this.isTracking = false;
      return;
    }

    this.watchId = navigator.geolocation.watchPosition(
      (position) => this.onGPSUpdate(position),
      (error) => this.onGPSError(error),
      {
        enableHighAccuracy: true,
        timeout: 5000,
        maximumAge: 0,
      }
    );
  }

  stopTracking() {
    if (!this.isTracking) {
      return;
    }

    if (this.testTimer) {
      clearInterval(this.testTimer);
      this.testTimer = null;
    }

    if (this.watchId != null && navigator.geolocation) {
      navigator.geolocation.clearWatch(this.watchId);
      this.watchId = null;
    }
    this.isTracking = false;
  }

  onGPSError(error) {
    console.error("❌ GPS 에러:", error?.message || error);
    // 권한 거부면 추적 중지
    if (error && error.code === error.PERMISSION_DENIED) {
      alert("GPS 권한이 거부되었습니다. 설정에서 위치 권한을 허용해주세요.");
      this.stopTracking();
    }
  }

  onGPSUpdate(position) {
    if (!this.isTracking) {
      return;
    }

    const now = Date.now();
    const coords = position?.coords;
    if (!coords) {
      return;
    }

    // ✅ 시작 직후 GPS 튀는 값 필터링 강화 (완화: 10m → 25m, 20m → 30m)
    const timeSinceStart = this.startTime ? (now - this.startTime) / 1000 : 0;
    const isJustStarted = timeSinceStart < 10; // 시작 후 10초 이내

    if (isJustStarted) {
      // 시작 직후에는 정확도가 좋은 GPS만 허용 (완화: 10m → 25m)
      if (coords.accuracy == null || coords.accuracy > 35) {
        return;
      }
      // ✅ 첫 GPS도 거리 체크: lastPosition이 없어도 정확도 체크로 필터링
      // 첫 GPS는 기준점으로만 사용하고, 거리 누적은 두 번째 GPS부터 시작
      if (this.lastPosition) {
        const prev = this.lastPosition.coords;
        const distKm = this.calculateDistance(
          prev.latitude,
          prev.longitude,
          coords.latitude,
          coords.longitude
        );
        if (Number.isFinite(distKm) && distKm > 0.03) {
          // 30m 이상은 점프로 간주
          return;
        }
      } else {
        // ✅ 첫 GPS: 정확도가 나쁘거나 이상한 값이면 무시
        // 첫 GPS는 기준점으로만 사용하고, 거리 누적은 두 번째 GPS부터
        if (coords.accuracy == null || coords.accuracy > 20) {
          return;
        }
      }
    }

    // 정확도 필터
    if (coords.accuracy != null && coords.accuracy > this.maxAccuracyM) {
      return;
    }

    // heading: 브라우저가 제공하면 우선 사용, 없으면 이동 벡터로 계산
    const nativeHeading =
      coords.heading != null && Number.isFinite(coords.heading)
        ? coords.heading
        : null;

    let distKm = null;
    let dtSec = null;
    if (this.lastPosition) {
      const prev = this.lastPosition.coords;
      distKm = this.calculateDistance(
        prev.latitude,
        prev.longitude,
        coords.latitude,
        coords.longitude
      );

      // 점프 필터: "튀는 좌표"는 전송/누적/기준좌표 업데이트를 모두 하지 않는다.
      if (Number.isFinite(distKm) && distKm > this.maxJumpKm) {
        return;
      }

      // timestamp 기반으로 dt 계산 (가능하면 더 정확)
      const prevTs = this.lastPosition.timestamp ?? null;
      const curTs = position.timestamp ?? null;
      if (prevTs != null && curTs != null) {
        const dMs = curTs - prevTs;
        if (Number.isFinite(dMs) && dMs > 0) {
          dtSec = dMs / 1000;
        }
      }

      if (nativeHeading != null) {
        this.heading = nativeHeading;
      } else if (distKm > 0.001) {
        this.heading = this.calculateBearing(
          prev.latitude,
          prev.longitude,
          coords.latitude,
          coords.longitude
        );
      }
    } else {
      if (nativeHeading != null) {
        this.heading = nativeHeading;
      }
    }

    // ✅ 정지 필터 제거: 항상 최신 GPS 좌표를 업데이트하고 전송
    if (Number.isFinite(distKm) && distKm >= 0) {
      // ✅ 첫 GPS는 거리 누적하지 않음 (기준점으로만 사용)
      if (this.lastPosition) {
        this.totalDistance += distKm;
      }
    }
    this.lastPosition = position;

    // 전송 간격
    if (now - this.lastSentTime < this.sendInterval) {
      return;
    }
    this.lastSentTime = now;

    // ✅ 속도 제한(경고/로그) - 전송 직전 판단
    try {
      let speedMps = null;
      if (
        coords.speed != null &&
        Number.isFinite(coords.speed) &&
        coords.speed > 0
      ) {
        speedMps = coords.speed; // m/s
      } else if (
        distKm != null &&
        Number.isFinite(distKm) &&
        distKm >= 0 &&
        dtSec != null &&
        Number.isFinite(dtSec) &&
        dtSec > 0
      ) {
        speedMps = (distKm * 1000) / dtSec;
      }

      if (speedMps != null && Number.isFinite(speedMps)) {
        const now2 = Date.now();
        const canAlert =
          now2 - this.lastTooFastAlertAt > this.tooFastAlertCooldownMs;

        if (speedMps >= this.tooFastHardMps) {
          if (canAlert) {
            console.warn("속도가 너무 빠릅니다(hard):", speedMps, "m/s");
            window.dispatchEvent(
              new CustomEvent("running:tooFast", {
                detail: { speedMps, hard: true },
              })
            );
            this.lastTooFastAlertAt = now2;
          }
        } else if (speedMps >= this.tooFastSoftMps) {
          this.tooFastSoftCount += 1;
          if (this.tooFastSoftCount >= 3 && canAlert) {
            console.warn("속도가 너무 빠릅니다(soft):", speedMps, "m/s");
            window.dispatchEvent(
              new CustomEvent("running:tooFast", {
                detail: { speedMps, hard: false },
              })
            );
            this.lastTooFastAlertAt = now2;
            this.tooFastSoftCount = 0;
          }
        } else {
          this.tooFastSoftCount = 0;
        }
      }
    } catch (e) {
      // ignore
    }

    // ✅ 정지 필터 제거: 항상 최신 GPS 좌표 전송
    this.sendGPSData(position);
  }

  getRunningTime() {
    if (!this.startTime) {
      return 0;
    }
    return Math.max(0, Math.floor((Date.now() - this.startTime) / 1000));
  }

  sendGPSData(position) {
    if (!this.stompClient || !this.stompClient.connected) {
      return;
    }

    const coords = position.coords;
    const gpsData = {
      sessionId: this.sessionId,
      userId: this.userId,
      latitude: coords.latitude,
      longitude: coords.longitude,
      accuracy: coords.accuracy,
      timestamp: position.timestamp ?? Date.now(),
      totalDistance: Math.round(this.totalDistance * 1000) / 1000, // 소수점 3자리
      runningTime: this.getRunningTime(),
      speed: coords.speed || 0,
      heading: this.heading,
    };
    if (this.includeMatchedDistanceM) {
      gpsData.matchedDistanceM =
        this.matchedDistanceM != null && Number.isFinite(this.matchedDistanceM)
          ? Math.max(0, Math.round(this.matchedDistanceM))
          : 0;
    }

    try {
      this.stompClient.send("/pub/running/gps", {}, JSON.stringify(gpsData));
    } catch (e) {
      console.warn("GPS 전송 실패(무시):", e?.message || e);
    }
  }

  // km
  calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const dLat = this.toRad(lat2 - lat1);
    const dLon = this.toRad(lon2 - lon1);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(this.toRad(lat1)) *
        Math.cos(this.toRad(lat2)) *
        Math.sin(dLon / 2) *
        Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  // degrees 0..360
  calculateBearing(lat1, lon1, lat2, lon2) {
    const toRad = (v) => (v * Math.PI) / 180;
    const toDeg = (v) => (v * 180) / Math.PI;
    const φ1 = toRad(lat1);
    const φ2 = toRad(lat2);
    const Δλ = toRad(lon2 - lon1);
    const y = Math.sin(Δλ) * Math.cos(φ2);
    const x =
      Math.cos(φ1) * Math.sin(φ2) - Math.sin(φ1) * Math.cos(φ2) * Math.cos(Δλ);
    const θ = Math.atan2(y, x);
    return (toDeg(θ) + 360) % 360;
  }

  toRad(value) {
    return (value * Math.PI) / 180;
  }

  // ---- 테스트 모드 ----
  startTestMode() {
    if (this.testTimer) {
      clearInterval(this.testTimer);
    }
    this.testTimer = setInterval(() => {
      const now = Date.now();
      // 북쪽으로만 이동
      this.totalDistance += this.testSpeedKmPerSec;
      this.heading = 0;

      const kmToLatitude = 1 / 110.574;
      const latChange = this.totalDistance * kmToLatitude;
      const currentLat = this.testStartLat + latChange;
      const currentLon = this.testStartLon;

      const position = {
        coords: {
          latitude: currentLat,
          longitude: currentLon,
          accuracy: 10,
          speed: (this.testSpeedKmPerSec * 1000) / 1,
          heading: this.heading,
        },
        timestamp: now,
      };

      this.lastPosition = position;
      if (now - this.lastSentTime >= this.sendInterval) {
        this.lastSentTime = now;
        this.sendGPSData(position);
      }
    }, 1000);
  }
}
