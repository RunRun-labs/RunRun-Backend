package com.multi.runrunbackend.domain.recruit.dto.req;

import lombok.Data;

/**
 * @author : KIMGWANGHO
 * @description : 모집글 조회 요청 dto
 * @filename : RecruitListReqDto
 * @since : 2025-12-18 목요일
 */
@Data
public class RecruitListReqDto {

  private Double latitude;
  private Double longitude;
  private Double radiusKm;

}