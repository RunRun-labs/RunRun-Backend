package com.multi.runrunbackend.domain.chat.dto.req;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방에서 런닝 시작 요청 시, 방장의 현재 위치(출발점 근처 여부)를 서버가 검증하기 위해 사용한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StartRunningReqDto {
    private Double latitude;
    private Double longitude;
    private Double accuracyM;
}


