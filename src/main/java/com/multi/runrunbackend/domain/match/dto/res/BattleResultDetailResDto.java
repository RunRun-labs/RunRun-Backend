package com.multi.runrunbackend.domain.match.dto.res;

import com.multi.runrunbackend.common.constant.DistanceType;
import com.multi.runrunbackend.domain.match.constant.SessionType;
import com.multi.runrunbackend.domain.match.entity.BattleResult;
import com.multi.runrunbackend.domain.match.entity.MatchSession;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : OnlineMatchSessionResDto
 * @since : 2026-01-02 금요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BattleResultDetailResDto {

  private Long sessionId;
  private SessionType sessionType;
  private DistanceType distanceType;
  private List<BattleParticipantResDto> results;

  public static BattleResultDetailResDto from(
      MatchSession session,
      List<BattleResult> battleResults
  ) {
    List<BattleParticipantResDto> rows = battleResults.stream()
        .map(BattleParticipantResDto::from)
        .toList();

    return new BattleResultDetailResDto(
        session.getId(),
        session.getType(),
        battleResults.get(0).getDistanceType(),
        rows
    );

  }
}
