package com.multi.runrunbackend.domain.match.dto.res;

import com.multi.runrunbackend.domain.match.entity.RunningResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : KIMGWANGHO
 * @description : Please explain the class!!!
 * @filename : RunningRecordResDto
 * @since : 2025-12-31 수요일
 */

@Getter
@Builder
public class RunningRecordResDto {

  private Long runningResultId;
  private Double totalDistance;
  private Integer totalTime;
  private Double avgPace;
  private LocalDateTime startedAt;
  private List<Map<String, Object>> splitPace;

  private String runningType;
  private String runStatus;

  public static RunningRecordResDto from(RunningResult result) {
    return RunningRecordResDto.builder()
        .runningResultId(result.getId())
        .totalDistance(result.getTotalDistance().doubleValue())
        .totalTime(result.getTotalTime())
        .avgPace(result.getAvgPace().doubleValue())
        .startedAt(result.getStartedAt())
        .splitPace(result.getSplitPace())
        .runningType(result.getRunningType().name())
        .runStatus(result.getRunStatus().name())
        .build();
  }

}
