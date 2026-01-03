package com.multi.runrunbackend.domain.match.dto.req;

import com.multi.runrunbackend.common.constant.DistanceType;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : SoloRunStartReqDto
 * @since : 2026-01-01 목요일
 */
@Getter
@NoArgsConstructor
public class SoloRunStartReqDto {

  private DistanceType distance;

  private Double manualDistance;

  private Long courseId;


}
