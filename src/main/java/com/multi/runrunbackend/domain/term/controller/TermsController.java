package com.multi.runrunbackend.domain.term.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.term.constant.TermsType;
import com.multi.runrunbackend.domain.term.dto.req.TermsReqDto;
import com.multi.runrunbackend.domain.term.dto.res.TermsResDto;
import com.multi.runrunbackend.domain.term.service.TermsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author : kimyongwon
 * @description : 이용약관 조회 및 관리 컨트롤러
 * @filename : TermsController
 * @since : 25. 12. 29. 오전 10:26 월요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/terms")
public class TermsController {

    private final TermsService termsService;

    @GetMapping("/{type}")
    public ResponseEntity<ApiResponse<TermsResDto>> getLatestTerms(@PathVariable TermsType type) {
        TermsResDto res = termsService.getLatestTerms(type);
        return ResponseEntity.ok(ApiResponse.success("약관 조회 성공", res));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createTerms(
            @RequestBody @Valid TermsReqDto req,
            @AuthenticationPrincipal CustomUser principal
    ) {
        termsService.createTerms(req, principal);
        return ResponseEntity.ok(ApiResponse.success("약관 등록 성공", null));
    }

}

