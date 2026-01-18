/**
 * TtsManager (frontend singleton)
 * - mode별 presigned URL batch 로드 + localStorage 캐시(2h TTL)
 * - 단일 Audio 채널로 큐/우선순위/중복(쿨다운) 관리
 *
 * 사용:
 *   await window.TtsManager.ensureLoaded({ mode: "OFFLINE" })
 *   window.TtsManager.speak("START_RUN")
 */
(function () {
  const STORAGE_KEY_PREFIX = "tts:batch:";
  const RUN_STATE_KEY_PREFIX = "tts:runstate:";
  const RUN_STATE_TTL_MS = 6 * 60 * 60 * 1000; // 6h

  function nowMs() {
    return Date.now();
  }

  function safeJsonParse(raw, fallback = null) {
    try {
      return JSON.parse(raw);
    } catch (e) {
      return fallback;
    }
  }

  function normalizeMode(mode) {
    if (!mode) return "OFFLINE";
    const m = String(mode).trim().toUpperCase();
    if (m === "ONLINE" || m === "OFFLINE" || m === "GHOST" || m === "SOLO")
      return m;
    return "OFFLINE";
  }

  function clamp(v, min, max) {
    return Math.max(min, Math.min(max, v));
  }

  function storageKey(sessionId, mode) {
    return `${STORAGE_KEY_PREFIX}${sessionId || "global"}:${normalizeMode(
      mode
    )}`;
  }

  function runStateKey(sessionId, mode) {
    return `${RUN_STATE_KEY_PREFIX}${sessionId || "global"}:${normalizeMode(
      mode
    )}`;
  }

  function getAccessToken() {
    const token = localStorage.getItem("accessToken");
    if (!token) return null;
    return token.startsWith("Bearer ") ? token : `Bearer ${token}`;
  }

  async function fetchBatch(mode) {
    const token = getAccessToken();
    if (!token) throw new Error("로그인이 필요합니다.");

    const res = await fetch(
      `/api/tts/presigned/batch/me?mode=${encodeURIComponent(mode)}`,
      {
        method: "POST",
        headers: {
          Authorization: token,
          "Content-Type": "application/json",
        },
      }
    );
    const body = await res.json().catch(() => null);
    if (!res.ok || !body?.success) {
      throw new Error(body?.message || "TTS URL을 불러올 수 없습니다.");
    }
    return body.data;
  }

  // 우선순위(큰 값이 더 우선)
  function cuePriority(cue) {
    const c = String(cue || "");
    if (
      c === "OFF_ROUTE" ||
      c === "SPEED_TOO_FAST" ||
      c === "GPS_WEAK" ||
      c === "WEB_SOCKET_DISCONNECTED" ||
      c === "HOST_SIGNAL_LOST"
    ) {
      return 3; // HIGH (interrupt)
    }
    if (
      c.startsWith("DIST_REMAIN_") ||
      c.startsWith("DIST_DONE_") ||
      c === "START_RUN" ||
      c === "ARRIVED_DESTINATION" ||
      c === "END_RUN" ||
      c === "WEB_SOCKET_RECONNECTED" ||
      c === "HOST_SIGNAL_RESUMED" ||
      c === "WIN" ||
      c === "RANK_2" ||
      c === "RANK_3" ||
      c === "RANK_LAST"
    ) {
      return 2; // MID (queue)
    }
    return 1; // LOW (drop if busy)
  }

  function defaultCooldownMs(cue) {
    const c = String(cue || "");
    if (c === "MOTIVATE_GOOD_JOB") return 5 * 60 * 1000;
    if (c.startsWith("PACE_")) return 3 * 60 * 1000;
    if (c === "OFF_ROUTE") return 30 * 1000;
    if (c === "BACK_ON_ROUTE") return 30 * 1000;
    if (c === "SPEED_TOO_FAST") return 15 * 1000;
    if (c === "GPS_WEAK") return 30 * 1000;
    if (c.startsWith("WEB_SOCKET_")) return 15 * 1000;
    if (c.startsWith("HOST_SIGNAL_")) return 15 * 1000;
    if (c === "GHOST_AHEAD" || c === "GHOST_BEHIND") return 15 * 1000;
    return 10 * 1000;
  }

  function paceToCue(avgPaceMinPerKm) {
    const p = Number(avgPaceMinPerKm);
    if (!Number.isFinite(p) || p <= 0) return null;
    // 10분 이상
    if (p >= 10.0) return "PACE_10M_PLUS";

    // ✅ 3분대: 3:00~3:29, 3분30초대: 3:30~3:59 등
    // minutes: 1..9, secBucket: 0 or 30
    const totalSec = Math.round(p * 60);
    const min = Math.floor(totalSec / 60);
    const sec = totalSec % 60;
    // 0~29초는 0, 30~59초는 30
    const bucketSec = sec < 30 ? 0 : 30;
    const normalizedMin = clamp(min, 1, 9);
    if (normalizedMin === 9 && bucketSec === 30) return "PACE_9M30";
    return `PACE_${normalizedMin}M${bucketSec === 0 ? "00" : "30"}`;
  }

  class TtsManagerImpl {
    constructor() {
      this._sessionId = null;
      this._mode = "OFFLINE";
      this._urls = null; // { cue: url }
      this._expiresAtMs = 0;
      this._voicePackId = null;

      this._audio = new Audio();
      this._isPlaying = false;
      this._queue = [];
      this._cooldowns = new Map(); // cue -> lastMs

      this._lastSpokenPaceCue = null;
      this._distanceMilestonesSpoken = new Set(); // "done:1" etc
      this._remainingSpoken = new Set(); // "remain:500"
      this._splitPaceSpokenKm = new Set(); // "split:1" etc

      this._audio.addEventListener("ended", () => {
        this._isPlaying = false;
        this._playNext();
      });
      this._audio.addEventListener("error", () => {
        this._isPlaying = false;
        this._playNext();
      });
    }

    setContext({ sessionId, mode }) {
      this._sessionId = sessionId || null;
      this._mode = normalizeMode(mode);
      this._loadRunState();
    }

    _loadRunState() {
      try {
        const key = runStateKey(this._sessionId, this._mode);
        const raw = localStorage.getItem(key);
        const saved = safeJsonParse(raw, null);
        if (!saved || typeof saved !== "object") return;

        const savedAtMs = Number(saved.savedAtMs);
        if (
          !Number.isFinite(savedAtMs) ||
          savedAtMs <= 0 ||
          nowMs() - savedAtMs > RUN_STATE_TTL_MS
        ) {
          localStorage.removeItem(key);
          return;
        }

        const remaining = Array.isArray(saved.remaining) ? saved.remaining : [];
        const split = Array.isArray(saved.split) ? saved.split : [];
        remaining.forEach((k) => this._remainingSpoken.add(String(k)));
        split.forEach((k) => this._splitPaceSpokenKm.add(String(k)));

        const lastCue = saved.lastSpokenPaceCue;
        this._lastSpokenPaceCue = lastCue ? String(lastCue) : null;
      } catch (e) {
        // ignore
      }
    }

    _saveRunState() {
      try {
        const key = runStateKey(this._sessionId, this._mode);
        const payload = {
          savedAtMs: nowMs(),
          remaining: Array.from(this._remainingSpoken),
          split: Array.from(this._splitPaceSpokenKm),
          lastSpokenPaceCue: this._lastSpokenPaceCue,
        };
        localStorage.setItem(key, JSON.stringify(payload));
      } catch (e) {
        // ignore
      }
    }

    _loadCache() {
      const key = storageKey(this._sessionId, this._mode);
      const raw = localStorage.getItem(key);
      const cached = safeJsonParse(raw, null);
      if (!cached) return false;
      const exp = Number(cached.expiresAtMs);
      if (!Number.isFinite(exp) || exp <= nowMs() + 5 * 60 * 1000) return false; // 5분 버퍼
      if (!cached.urls || typeof cached.urls !== "object") return false;
      this._urls = cached.urls;
      this._expiresAtMs = exp;
      this._voicePackId = cached.voicePackId ?? null;
      return true;
    }

    _saveCache(data) {
      const key = storageKey(this._sessionId, this._mode);
      // data.expiresAt는 ISO(OffsetDateTime)로 옴
      let expMs = 0;
      try {
        expMs = data?.expiresAt ? new Date(data.expiresAt).getTime() : 0;
      } catch (e) {
        expMs = 0;
      }
      if (!Number.isFinite(expMs) || expMs <= 0) {
        // expiresAt 파싱 실패 시 ttl로 추정
        const ttlSec = Number(data?.expiresInSeconds) || 0;
        expMs = nowMs() + ttlSec * 1000;
      }

      const payload = {
        voicePackId: data?.voicePackId ?? null,
        expiresAtMs: expMs,
        urls: data?.urls ?? {},
      };
      localStorage.setItem(key, JSON.stringify(payload));
      this._urls = payload.urls;
      this._expiresAtMs = payload.expiresAtMs;
      this._voicePackId = payload.voicePackId;
    }

    async ensureLoaded({ sessionId, mode }) {
      this.setContext({ sessionId, mode });
      if (this._urls && this._expiresAtMs > nowMs() + 5 * 60 * 1000)
        return true;
      if (this._loadCache()) return true;
      const data = await fetchBatch(this._mode);
      this._saveCache(data);
      return true;
    }

    _canSpeak(cue, cooldownMs) {
      const cd = cooldownMs != null ? cooldownMs : defaultCooldownMs(cue);
      const last = this._cooldowns.get(cue) || 0;
      return nowMs() - last >= cd;
    }

    _markSpoken(cue) {
      this._cooldowns.set(cue, nowMs());
    }

    speak(cue, opts = {}) {
      const c = String(cue || "");
      if (!c) return false;
      if (!this._urls || !this._urls[c]) return false;

      const prio = opts.priority != null ? opts.priority : cuePriority(c);
      const cooldownMs = opts.cooldownMs;

      if (!this._canSpeak(c, cooldownMs)) return false;

      // LOW는 재생 중이면 드랍
      if (prio <= 1 && this._isPlaying) return false;

      const item = {
        cue: c,
        url: this._urls[c],
        priority: prio,
        interrupt: prio >= 3,
      };

      // HIGH는 즉시 인터럽트
      if (item.interrupt) {
        try {
          this._audio.pause();
          this._audio.currentTime = 0;
        } catch (e) {}
        this._queue = [];
        this._isPlaying = false;
        this._enqueue(item);
        this._playNext(true);
        this._markSpoken(c);
        return true;
      }

      // MID/LOW는 큐에 넣고 순서대로
      this._enqueue(item);
      this._playNext(false);
      this._markSpoken(c);
      return true;
    }

    _enqueue(item) {
      this._queue.push(item);
      // priority 높은 게 앞
      this._queue.sort((a, b) => (b.priority || 0) - (a.priority || 0));
      // 길이 제한(안전)
      if (this._queue.length > 10) this._queue.length = 10;
    }

    _playNext(force) {
      if (this._isPlaying && !force) return;
      if (!this._queue.length) return;
      const next = this._queue.shift();
      if (!next || !next.url) return;

      this._isPlaying = true;
      try {
        this._audio.src = next.url;
        // play()는 사용자 제스처가 없으면 막힐 수 있음 (모바일 정책)
        const p = this._audio.play();
        if (p && typeof p.catch === "function") {
          p.catch(() => {
            this._isPlaying = false;
            this._playNext(false);
          });
        }
      } catch (e) {
        this._isPlaying = false;
        this._playNext(false);
      }
    }

    // ===== 러닝 이벤트 헬퍼 =====
    onDistance(totalDistanceKm, remainingDistanceKm) {
      const r = Number(remainingDistanceKm);
      if (Number.isFinite(r) && r >= 0) {
        const m = r * 1000;
        const thresholds = [
          { m: 500, cue: "DIST_REMAIN_500M" },
          { m: 300, cue: "DIST_REMAIN_300M" },
          { m: 100, cue: "DIST_REMAIN_100M" },
        ];
        // ✅ 작은 값부터 정렬하여 가장 가까운 임계값 하나만 재생
        // 예: 100m 남았으면 100m만 재생 (500m, 300m는 이미 지나감)
        thresholds.sort((a, b) => a.m - b.m);
        for (const t of thresholds) {
          const key = `remain:${t.m}`;
          // 현재 남은 거리가 임계값보다 작거나 같고, 아직 말하지 않았으면 재생
          if (m <= t.m && !this._remainingSpoken.has(key)) {
            this._remainingSpoken.add(key);
            this._saveRunState();
            this.speak(t.cue, { priority: 2, cooldownMs: 0 });
            break; // 가장 작은 임계값 하나만 재생하고 종료
          }
        }
      }
    }

    maybeSpeakPace(avgPaceMinPerKm) {
      const cue = paceToCue(avgPaceMinPerKm);
      if (!cue) return false;
      // 같은 cue 반복은 쿨다운으로 자연히 막히지만, 상태도 저장
      if (this._lastSpokenPaceCue === cue) return false;
      const ok = this.speak(cue, { priority: 1, cooldownMs: 3 * 60 * 1000 });
      if (ok) {
        this._lastSpokenPaceCue = cue;
        this._saveRunState();
      }
      return ok;
    }

    /**
     * 거리 관련 상태 초기화 (재진입 시 중복 방지)
     * @param {number} remainingDistanceKm - 남은 거리 (km), 재진입 시 이미 지나간 거리 TTS 스킵용
     */
    resetDistanceState(remainingDistanceKm) {
      // ✅ 재연결/재진입 시 과거 구간/남은거리 안내가 다시 재생되면 UX가 깨지므로
      // 이미 말한 구간/남은거리 상태는 유지하고, 페이스(LOW)만 리셋한다.
      this._lastSpokenPaceCue = null;

      // ✅ 재진입 시 현재 남은 거리를 기준으로 이미 지나간 거리 TTS는 스킵
      if (remainingDistanceKm != null) {
        const remainingDistM = remainingDistanceKm * 1000;

        // ✅ "처음 진입했을 때 이미 남은 거리가 임계값보다 작거나 같다면" 그 임계값 멘트는 재생하면 안 됨
        // 예: 목표 0.2km(200m)인 러닝에 들어오면 300m 안내가 나오면 안 되므로 300/500은 스킵 처리
        // ✅ 중요: 100m에 도달했을 때는 재생해야 하므로, 정확히 그 임계값보다 작을 때만 스킵
        // 예: 150m 남았으면 100m는 아직 재생하지 않음, 100m 남았으면 재생해야 함
        const thresholdsM = [500, 300, 100];
        for (const t of thresholdsM) {
          // ✅ 현재 남은 거리가 임계값보다 작으면 (같지 않고 작으면) 이미 지나간 것으로 간주
          if (remainingDistM < t) {
            this._remainingSpoken.add(`remain:${t}`);
          }
        }
      }

      this._saveRunState();
    }

    /**
     * 1km split 기반 안내:
     * - segmentPaces에 새로 생긴 km(1..10)에 대해
     *   DIST_DONE_{km}KM + 해당 km pace cue를 연속으로 1회만 재생
     */
    onSplitPaces(segmentPaces) {
      if (!segmentPaces || typeof segmentPaces !== "object") return;
      for (let km = 1; km <= 10; km++) {
        const key = String(km);
        const val = segmentPaces[key] ?? segmentPaces[km];
        const pace = Number(val);
        if (!Number.isFinite(pace) || pace <= 0) continue;

        const spokenKey = `split:${km}`;
        if (this._splitPaceSpokenKm.has(spokenKey)) continue;
        this._splitPaceSpokenKm.add(spokenKey);
        this._saveRunState();

        // 1) km 통과
        this.speak(`DIST_DONE_${km}KM`, { priority: 2, cooldownMs: 0 });
        // 2) 해당 km 페이스(큐에 쌓이도록 priority=2)
        const paceCue = paceToCue(pace);
        if (paceCue) {
          this.speak(paceCue, { priority: 2, cooldownMs: 0 });
        }
      }
    }
  }

  window.TtsManager = new TtsManagerImpl();
})();
