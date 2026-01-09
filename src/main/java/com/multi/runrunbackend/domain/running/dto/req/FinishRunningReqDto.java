package com.multi.runrunbackend.domain.running.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오프라인 러닝 종료 요청
 * - 코스가 없는 자유러닝인 경우, 먼저 코스를 저장한 뒤 courseId를 함께 넘겨 결과에 연결한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinishRunningReqDto {
    private Long courseId; // optional
}


