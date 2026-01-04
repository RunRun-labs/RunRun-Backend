package com.multi.runrunbackend.domain.match.dto.res;

import com.multi.runrunbackend.domain.match.entity.BattleResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : OnlineSessionParticipantRow
 * @since : 2026-01-02 금요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BattleParticipantResDto {

  private Long userId;
  private String loginId;

  private Integer ranking;

  private Integer totalTime;
  private BigDecimal avgPace;
  private BigDecimal totalDistance;

  private Integer previousRating;
  private Integer currentRating;
  private Integer delta;

  private List<Map<String, Object>> splitPace;

  public static BattleParticipantResDto from(BattleResult br) {
    return new BattleParticipantResDto(
        br.getUser().getId(),
        br.getUser().getLoginId(),
        br.getRanking(),
        br.getRunningResult().getTotalTime(),
        br.getRunningResult().getAvgPace(),
        br.getRunningResult().getTotalDistance(),
        br.getPreviousRating(),
        br.getCurrentRating(),
        br.getCurrentRating() - br.getPreviousRating(),
        br.getRunningResult().getSplitPace()
    );
  }
}
