package com.multi.runrunbackend.domain.match.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.match.dto.req.MatchConfirmReqDto;
import com.multi.runrunbackend.domain.match.dto.res.MatchConfirmResDto;
import com.multi.runrunbackend.domain.match.service.MatchSessionService;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
  private final UserRepository userRepository;

  @PostMapping("/confirm")
  public ResponseEntity<ApiResponse<MatchConfirmResDto>> confirmMatch(
      @RequestBody MatchConfirmReqDto reqDto,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    Long sessionId = matchSessionService.createOfflineSession(
        reqDto.getRecruitId(),
        userDetails
    );
    return ResponseEntity.ok(ApiResponse.success(new MatchConfirmResDto(sessionId)));
  }
}
