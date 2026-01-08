package com.multi.runrunbackend.domain.rating.controller;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.rating.dto.res.DistanceRatingResDto;
import com.multi.runrunbackend.domain.rating.service.DistanceRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : system
 * @description : 거리별 레이팅 조회 API를 제공하는 컨트롤러
 * @filename : RatingController
 */
@RestController
@RequestMapping("/api/rating")
@RequiredArgsConstructor
public class DistanceRatingController {

  private final DistanceRatingService distanceRatingService;

  /**
   * 사용자의 거리별 레이팅과 티어 조회
   */
  @GetMapping("/distance")
  public ResponseEntity<ApiResponse<DistanceRatingResDto>> getUserDistanceRating(
      @AuthenticationPrincipal CustomUser principal,
      @RequestParam DistanceType distanceType
  ) {
    DistanceRatingResDto response = distanceRatingService.getUserDistanceRating(principal,
        distanceType);
    return ResponseEntity.ok(
        ApiResponse.success("거리별 레이팅 조회 성공", response)
    );
  }
}

