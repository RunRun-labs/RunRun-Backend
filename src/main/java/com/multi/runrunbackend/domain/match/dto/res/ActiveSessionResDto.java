package com.multi.runrunbackend.domain.match.dto.res;

import lombok.Builder;
import lombok.Getter;

/**
 * @author : KIMGWANGHO
 * @description : 활성 세션 정보 응답 DTO (STANDBY 또는 IN_PROGRESS 상태의 세션)
 * @filename : ActiveSessionResDto
 * @since : 2025-12-30 월요일
 */
@Getter
@Builder
public class ActiveSessionResDto {

  private Long sessionId;
  private String status; // "STANDBY" 또는 "IN_PROGRESS"
  private String redirectUrl; // 클라이언트가 이동할 URL
}

