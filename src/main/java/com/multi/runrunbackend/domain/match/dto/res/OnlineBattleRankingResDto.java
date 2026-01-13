package com.multi.runrunbackend.domain.match.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description ONLINEBATTLE 러닝 결과의 등수(ranking) 단건 조회 응답 DTO
 */
@Getter
@AllArgsConstructor
public class OnlineBattleRankingResDto {

    private Long runningResultId;
    private Integer ranking;
}

