package com.multi.runrunbackend.domain.recruit.dto.res;

import com.multi.runrunbackend.domain.recruit.entity.Recruit;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : KIMGWANGHO
 * @description : 러닝 모집글 생성 성공 시, 생성된 모집글의 식별자(ID) 등 결과 정보를 반환하는 응답 DTO 클래스
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
