package com.multi.runrunbackend.domain.crew.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.crew.dto.req.CrewCreateReqDto;
import com.multi.runrunbackend.domain.crew.service.CrewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author : BoKyung
 * @description : 크루 Controller
 * @filename : CrewController
 * @since : 25. 12. 18. 목요일
 */
@RestController
@RequestMapping("/api/crews")
@RequiredArgsConstructor
@Tag(name = "크루 API")
public class CrewController {

    private final CrewService crewService;

    /**
     * @param accessToken 액세스 토큰 (Authorization 헤더)
     * @param reqDto      크루 생성 요청 DTO
     * @description : 크루 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createCrew(
            @RequestHeader("Authorization") String accessToken,
            @Valid @RequestBody CrewCreateReqDto reqDto
    ) {
        Long crewId = crewService.createCrew(accessToken, reqDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("크루 생성 성공", crewId));
    }
}
