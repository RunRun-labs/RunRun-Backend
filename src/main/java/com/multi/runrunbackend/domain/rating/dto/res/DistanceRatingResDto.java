package com.multi.runrunbackend.domain.rating.dto.res;

import com.multi.runrunbackend.domain.match.constant.Tier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : KIMGWANGHO
 * @description : 거리별 레이팅 조회 응답 DTO
 * @filename : DistanceRatingResDto
 */
@Getter
@Builder
@AllArgsConstructor
public class DistanceRatingResDto {

  private Integer currentRating;
  private Tier currentTier;
}

