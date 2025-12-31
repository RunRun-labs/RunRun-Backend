package com.multi.runrunbackend.domain.match.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.match.constant.RunningResultFilterType;
import com.multi.runrunbackend.domain.match.dto.req.RunningRecordResDto;
import com.multi.runrunbackend.domain.match.dto.req.OfflineMatchConfirmReqDto;
import com.multi.runrunbackend.domain.match.dto.req.OnlineMatchJoinReqDto;
import com.multi.runrunbackend.domain.match.dto.res.OfflineMatchConfirmResDto;
import com.multi.runrunbackend.domain.match.dto.res.OnlineMatchStatusResDto;
import com.multi.runrunbackend.domain.match.service.MatchSessionService;
import com.multi.runrunbackend.domain.match.service.MatchingQueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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


  @PostMapping("/ghost/{runningResultId}/start")
  public ResponseEntity<ApiResponse<Long>> startGhostMatch(
      @AuthenticationPrincipal CustomUser principal,
      @PathVariable Long runningResultId
  ) {

    Long sessionId = matchSessionService.createGhostSession(runningResultId, principal);

    return ResponseEntity.ok(ApiResponse.success("고스트런 세션이 생성되었습니다.", sessionId));

  }

  @GetMapping("/ghost")
  public ResponseEntity<ApiResponse<Slice<RunningRecordResDto>>> getMyRunningRecords(
      @AuthenticationPrincipal CustomUser principal,
      @RequestParam(required = false, defaultValue = "ALL") RunningResultFilterType filter,
      @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable
  ) {
    Slice<RunningRecordResDto> records = matchSessionService.getMyRunningRecords(principal, filter,
        pageable);
    return ResponseEntity.ok(ApiResponse.success("내 러닝 결과 조회 성공", records));
  }

}
