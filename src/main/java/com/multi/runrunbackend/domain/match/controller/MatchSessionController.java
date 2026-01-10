package com.multi.runrunbackend.domain.match.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.match.dto.req.OfflineMatchConfirmReqDto;
import com.multi.runrunbackend.domain.match.dto.req.OnlineMatchJoinReqDto;
import com.multi.runrunbackend.domain.match.dto.req.SoloRunStartReqDto;
import com.multi.runrunbackend.domain.match.dto.res.MatchSessionDetailResDto;
import com.multi.runrunbackend.domain.match.dto.res.MatchWaitingInfoDto;
import com.multi.runrunbackend.domain.match.dto.res.OfflineMatchConfirmResDto;
import com.multi.runrunbackend.domain.match.dto.res.OnlineMatchStatusResDto;
import com.multi.runrunbackend.domain.match.service.MatchSessionService;
import com.multi.runrunbackend.domain.match.service.MatchingQueueService;
import com.multi.runrunbackend.domain.running.battle.service.BattleService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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


  @GetMapping("/sessions/{sessionId}")
  public ResponseEntity<ApiResponse<MatchSessionDetailResDto>> getSessionDetail(
      @PathVariable Long sessionId,
      @AuthenticationPrincipal CustomUser principal
  ) {

    MatchSessionDetailResDto res = matchSessionService.getSessionDetail(sessionId,
        principal.getUserId());
    return ResponseEntity.ok(ApiResponse.success("세션 상세 조회 성공", res));
  }

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
   * 대기방에서 나가기 / 세션 취소
   */
  @DeleteMapping("/session/{sessionId}/leave")
  public ResponseEntity<ApiResponse<Void>> leaveWaitingRoom(
      @PathVariable Long sessionId,
      @AuthenticationPrincipal CustomUser principal
  ) {
    // ✅ 1. 매칭 큐에서 제거
    matchingQueueService.removeQueue(principal);

    // ✅ 2. 세션에서 나가기
    matchSessionService.leaveSession(sessionId, principal.getUserId());

    return ResponseEntity.ok(ApiResponse.successNoData("대기방에서 나갔습니다."));
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
    // Service에서 모든 비즈니스 로직 및 WebSocket 메시지 전송 처리
    Map<String, Object> result = battleService.handleTimeout(sessionId);

    return ResponseEntity.ok(ApiResponse.success(result));
  }


  @PostMapping("/ghost/{runningResultId}/start")
  public ResponseEntity<ApiResponse<Long>> startGhostMatch(
      @AuthenticationPrincipal CustomUser principal,
      @PathVariable Long runningResultId
  ) {

    Long sessionId = matchSessionService.createGhostSession(runningResultId, principal);

    return ResponseEntity.ok(ApiResponse.success("고스트런 세션이 생성되었습니다.", sessionId));

  }

//  @GetMapping("/ghost")
//  public ResponseEntity<ApiResponse<Slice<RunningRecordResDto>>> getMyRunningRecords(
//      @AuthenticationPrincipal CustomUser principal,
//      @RequestParam(required = false, defaultValue = "ALL") RunningResultFilterType filter,
//      @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable
//  ) {
//    Slice<RunningRecordResDto> records = matchSessionService.getMyRunningRecords(principal, filter,
//        pageable);
//    return ResponseEntity.ok(ApiResponse.success("내 러닝 결과 조회 성공", records));
//  }

  /**
   * 고스트런 세션 정보 조회
   */
  @GetMapping("/ghost/session/{sessionId}")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getGhostSessionInfo(
      @PathVariable Long sessionId,
      @AuthenticationPrincipal CustomUser principal
  ) {
    Map<String, Object> info = matchSessionService.getGhostSessionInfo(sessionId);
    return ResponseEntity.ok(ApiResponse.success("고스트런 세션 정보 조회 성공", info));
  }

  @PostMapping("/solorun/start")
  public ResponseEntity<ApiResponse<Long>> startSoloRun(
      @AuthenticationPrincipal CustomUser principal,
      @RequestBody @Valid SoloRunStartReqDto reqDto
  ) {

    Long sessionId = matchSessionService.createSoloSession(principal, reqDto);
    return ResponseEntity.ok(ApiResponse.success("솔로런 세션 생성 성공 ", sessionId));

  }


}
