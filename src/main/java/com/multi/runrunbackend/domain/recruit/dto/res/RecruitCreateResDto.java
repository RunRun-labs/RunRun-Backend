package com.multi.runrunbackend.domain.recruit.dto.res;

import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : RecruitCreateResDto
 * @since : 2025-12-17 수요일
 */
@Getter
@Builder
public class RecruitCreateResDto {

  private Long recruitId;

  public static RecruitCreateResDto from(Recruit recruit) {
    return RecruitCreateResDto.builder()
        .recruitId(recruit.getId())
        .build();
  }
}
