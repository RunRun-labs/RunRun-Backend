package com.multi.runrunbackend.domain.challenge.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.challenge.dto.req.ChallengeReqDto;
import com.multi.runrunbackend.domain.challenge.dto.res.ChallengeResDto;
import com.multi.runrunbackend.domain.challenge.service.ChallengeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 *
 * @author : kimyongwon
 * @description : 챌린지 생성, 조회를 담당하는 컨트롤러
 * @filename : ChallengeController
 * @since : 25. 12. 21. 오후 8:56 일요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/challenges")
public class ChallengeController {

    private final ChallengeService challengeService;

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<ChallengeResDto>> createChallenge(
            @RequestPart("request") @Valid ChallengeReqDto req,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal CustomUser principal
    ) {
        ChallengeResDto res = challengeService.createChallenge(req, file, principal);
        return ResponseEntity.ok(ApiResponse.success("챌린지 생성 성공", res));
    }


    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<Void>> updateChallenge(
            @PathVariable Long id,
            @RequestPart("request") @Valid ChallengeReqDto req,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal CustomUser principal
    ) {
        challengeService.updateChallenge(id, req, file, principal);
        return ResponseEntity.ok(ApiResponse.success("챌린지 수정 성공", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteChallenge(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUser principal
    ) {
        challengeService.deleteChallenge(id, principal);
        return ResponseEntity.ok(ApiResponse.success("챌린지 삭제 성공", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChallengeResDto>>> getChallengeList(
            @AuthenticationPrincipal CustomUser principal
    ) {
        List<ChallengeResDto> res = challengeService.getChallengeList(principal);
        return ResponseEntity.ok(ApiResponse.success("챌린지 목록 조회 성공", res));
    }

    // 챌린지 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChallengeResDto>> getChallengeDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUser principal
    ) {
        ChallengeResDto res = challengeService.getChallengeDetail(id, principal);
        return ResponseEntity.ok(ApiResponse.success("챌린지 상세 조회 성공", res));
    }

    // 챌린지 참여하기
    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<Void>> joinChallenge(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUser principal
    ) {
        challengeService.joinChallenge(id, principal);
        return ResponseEntity.ok(ApiResponse.success("챌린지 참여 성공", null));
    }


    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelChallenge(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUser principal
    ) {
        challengeService.cancelChallenge(id, principal);
        return ResponseEntity.ok(ApiResponse.success("챌린지 포기 성공", null));
    }

}