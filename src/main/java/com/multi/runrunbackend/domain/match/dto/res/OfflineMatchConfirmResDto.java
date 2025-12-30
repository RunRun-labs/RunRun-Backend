package com.multi.runrunbackend.domain.match.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author : KIMGWANGHO
 * @description : 매칭 확정 response Dto
 * @filename : MatchSessionConfirmResDto
 * @since : 2025-12-21 일요일
 */
@Getter
@AllArgsConstructor
public class OfflineMatchConfirmResDto {

  private Long sessionId;
}