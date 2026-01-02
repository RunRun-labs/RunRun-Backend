package com.multi.runrunbackend.domain.match.dto.res;

import lombok.Builder;
import lombok.Getter;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : OnlineMatchStatusResDto
 * @since : 2025-12-27 토요일
 */
@Getter
@Builder
public class OnlineMatchStatusResDto {

  private String status;
  private Long sessionId;
}
