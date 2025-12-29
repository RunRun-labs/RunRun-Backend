package com.multi.runrunbackend.domain.match.dto.req;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : KIMGWANGHO
 * @description : 매칭 확정 요청 request Dto
 * @filename : MatchSessionConfrimReqDto
 * @since : 2025-12-21 일요일
 */
@Getter
@NoArgsConstructor
public class MatchConfirmReqDto {

  private Long recruitId;
}
