package com.multi.runrunbackend.domain.feed.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.feed.dto.req.FeedPostCreateReqDto;
import com.multi.runrunbackend.domain.feed.dto.res.FeedPostResDto;
import com.multi.runrunbackend.domain.feed.service.FeedPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author : kimyongwon
 * @description : 러닝 결과를 피드에 공유/조회 담당 컨트롤러
 * @filename : FeedPostController
 * @since : 26. 1. 4. 오후 9:18 일요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feed")
public class FeedPostController {

    private final FeedPostService feedPostService;

    /**
     * 러닝 결과를 피드에 공유
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FeedPostResDto>> createFeedPost(
            @AuthenticationPrincipal CustomUser principal,
            @RequestBody @Valid FeedPostCreateReqDto reqDto
    ) {
        FeedPostResDto resDto =
                feedPostService.createFeedPost(principal, reqDto);

        return ResponseEntity.ok(
                ApiResponse.success("러닝 결과가 피드에 공유되었습니다.", resDto)
        );
    }
}