package com.multi.runrunbackend.domain.match.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.match.constant.RunningResultFilterType;
import com.multi.runrunbackend.domain.match.dto.res.RunningRecordResDto;
import com.multi.runrunbackend.domain.match.service.RunningResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : RunningResultController
 * @since : 2026-01-04 일요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/running-results")
public class RunningResultController {

  private final RunningResultService runningResultService;

  @GetMapping
  public ResponseEntity<ApiResponse<Slice<RunningRecordResDto>>> getMyResults(
      @AuthenticationPrincipal CustomUser principal,
      @RequestParam(required = false, defaultValue = "ALL") RunningResultFilterType filter,
      @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable
  ) {
    Slice<RunningRecordResDto> results = runningResultService.getMyRunningResults(principal, filter,
        pageable);
    return ResponseEntity.ok(ApiResponse.success("기록 조회 성공", results));
  }

}
