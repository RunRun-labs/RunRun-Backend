package com.multi.runrunbackend.domain.match.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.match.constant.SessionStatus;
import com.multi.runrunbackend.domain.match.dto.req.OfflineMatchConfirmReqDto;
import com.multi.runrunbackend.domain.match.dto.req.OnlineMatchJoinReqDto;
import com.multi.runrunbackend.domain.match.dto.res.MatchWaitingInfoDto;
import com.multi.runrunbackend.domain.match.dto.res.OfflineMatchConfirmResDto;
import com.multi.runrunbackend.domain.match.dto.res.OnlineMatchStatusResDto;
import com.multi.runrunbackend.domain.match.service.MatchSessionService;
import com.multi.runrunbackend.domain.match.service.MatchingQueueService;
import com.multi.runrunbackend.domain.running.battle.service.BattleService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : KIMGWANGHO
 * @description : MatchSession 관련 api 처리하는 Controller class
 * @filename : MatchController
 * @since : 2025-12-21 일요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/match")
public class MatchSessionController {

  private final MatchSessionService matchSessionService;
  private final MatchingQueueService matchingQueueService;
  private final BattleService battleService;
  private final SimpMessagingTemplate messagingTemplate;

  @PostMapping("/offline/confirm")
  public ResponseEntity<ApiResponse<OfflineMatchConfirmResDto>> confirmMatch(
      @AuthenticationPrincipal CustomUser principal,
      @RequestBody @Valid OfflineMatchConfirmReqDto reqDto
  ) {
    Long sessionId = matchSessionService.createOfflineSession(
        reqDto.getRecruitId(),
        principal
    );
    return ResponseEntity.ok(ApiResponse.success(new OfflineMatchConfirmResDto(sessionId)));
  }

  @PostMapping("/online/join")
  public ResponseEntity<ApiResponse<Long>> joinOnlineMatch(
      @AuthenticationPrincipal CustomUser principal,
      @RequestBody @Valid OnlineMatchJoinReqDto reqDto
  ) {
    Long existingSessionId = matchingQueueService.addQueue(principal, reqDto.getDistance(),
        reqDto.getParticipantCount());
    if (existingSessionId != null) {
      return ResponseEntity.ok(ApiResponse.success("이미 매칭된 세션이 있습니다.", existingSessionId));
    }

    return ResponseEntity.ok(ApiResponse.success("온라인 매칭 대기열에 등록되었습니다.", null));
  }

  @GetMapping("/online/status")
  public ResponseEntity<ApiResponse<OnlineMatchStatusResDto>> checkOnlineMatchStatus(
      @AuthenticationPrincipal CustomUser principal
  ) {
    OnlineMatchStatusResDto response = matchingQueueService.checkMatchStatus(principal);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @DeleteMapping("/online/join")
  public ResponseEntity<ApiResponse<Void>> cancelOnlineMatch(
      @AuthenticationPrincipal CustomUser principal
  ) {
    matchingQueueService.removeQueue(principal);
    return ResponseEntity.ok(ApiResponse.successNoData("매칭 대기가 취소되었습니다."));
  }

  /**
   * 대기방 정보 조회
   */
  @GetMapping("/session/{sessionId}")
  public ResponseEntity<ApiResponse<MatchWaitingInfoDto>> getWaitingInfo(
      @PathVariable Long sessionId,
      @AuthenticationPrincipal CustomUser principal
  ) {
    MatchWaitingInfoDto info = matchSessionService.getWaitingInfo(sessionId, principal.getUserId());
    return ResponseEntity.ok(ApiResponse.success(info));
  }

  /**
   * 타임아웃 처리 (5분 경과)
   */
  @PostMapping("/session/{sessionId}/timeout")
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleTimeout(
      @PathVariable Long sessionId,
      @AuthenticationPrincipal CustomUser principal
  ) {
    // 타임아웃 처리 (true: 시작, false: 취소)
    boolean started = battleService.handleTimeout(sessionId);

    // 세션 상태 확인 (이미 시작된 경우 메시지 보내지 않기)
    MatchWaitingInfoDto info = matchSessionService.getWaitingInfo(sessionId, principal.getUserId());
    boolean alreadyStarted = SessionStatus.IN_PROGRESS.equals(info.getStatus());  // enum 비교 수정

    Map<String, Object> result = new HashMap<>();
    result.put("sessionId", sessionId);
    result.put("started", started);
    result.put("alreadyStarted", alreadyStarted);

    // 이미 시작된 경우 WebSocket 메시지 보내지 않음
    if (!alreadyStarted) {
      // WebSocket으로 모든 참가자에게 알림
      if (started) {
        // 시작됨 (타임아웃으로)
        Map<String, Object> message = new HashMap<>();
        message.put("type", "TIMEOUT_START");
        message.put("message", "일부 참가자가 강퇴되었습니다. 배틀을 시작합니다.");
        message.put("timestamp", LocalDateTime.now());

        messagingTemplate.convertAndSend(
            "/sub/battle/" + sessionId + "/timeout",
            (Object) message
        );

        // 배틀 시작 알림
        Map<String, Object> startMessage = new HashMap<>();
        startMessage.put("type", "BATTLE_START");
        startMessage.put("sessionId", sessionId);
        startMessage.put("timestamp", LocalDateTime.now());

        messagingTemplate.convertAndSend(
            "/sub/battle/" + sessionId + "/start",
            (Object) startMessage
        );

      } else {
        // 취소됨
        Map<String, Object> message = new HashMap<>();
        message.put("type", "TIMEOUT_CANCEL");
        message.put("message", "참가자가 부족하여 매치가 취소되었습니다.");
        message.put("timestamp", LocalDateTime.now());

        messagingTemplate.convertAndSend(
            "/sub/battle/" + sessionId + "/timeout",
            (Object) message
        );
      }
    }

    return ResponseEntity.ok(ApiResponse.success(result));
  }
}
