package com.multi.runrunbackend.domain.crew.controller;

import com.multi.runrunbackend.common.jwt.provider.TokenProvider;
import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.crew.dto.req.CrewJoinReqDto;
import com.multi.runrunbackend.domain.crew.service.CrewJoinService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author : BoKyung
 * @description : 크루 가입 관련 Controller
 * @filename : CrewJoinController
 * @since : 25. 12. 18. 목요일
 */
@RestController
@RequestMapping("/api/crews")
@RequiredArgsConstructor
@Slf4j
public class CrewJoinController {

    private final CrewJoinService crewJoinService;
    private final TokenProvider tokenProvider;

    /**
     * @param crewId        가입 신청할 크루 ID
     * @param authorization JWT 토큰
     * @param reqDto        가입 신청 정보 (자기소개, 거리, 페이스, 지역)
     * @description : 회원이 크루에 가입 신청을 한다 (100P가 차감되며, 크루장에게 알림 발송)
     */
    @PostMapping("/{crewId}/join")
    public ResponseEntity<ApiResponse<Void>> requestJoin(
            @Parameter(description = "크루 ID", required = true)
            @PathVariable Long crewId,

            @Parameter(description = "JWT 토큰", required = true)
            @RequestHeader("Authorization") String authorization,

            @Parameter(description = "가입 신청 정보", required = true)
            @Valid @RequestBody CrewJoinReqDto reqDto
    ) {
        String jwt = tokenProvider.resolveToken(authorization);
        String loginId = tokenProvider.getUserId(jwt);

        crewJoinService.requestJoin(crewId, loginId, reqDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successNoData("크루 가입 신청이 완료되었습니다."));
    }

}
