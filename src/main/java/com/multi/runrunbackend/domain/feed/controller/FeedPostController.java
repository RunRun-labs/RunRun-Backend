package com.multi.runrunbackend.domain.feed.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.feed.dto.req.FeedPostCreateReqDto;
import com.multi.runrunbackend.domain.feed.dto.req.FeedPostUpdateReqDto;
import com.multi.runrunbackend.domain.feed.dto.res.FeedPostResDto;
import com.multi.runrunbackend.domain.feed.service.FeedPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FeedPostResDto>> createFeedPost(
            @AuthenticationPrincipal CustomUser principal,
            @RequestPart("feedPost") @Valid FeedPostCreateReqDto reqDto,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        FeedPostResDto resDto =
                feedPostService.createFeedPost(principal, reqDto, imageFile);

        return ResponseEntity.ok(
                ApiResponse.success("러닝 결과가 피드에 공유되었습니다.", resDto)
        );
    }

    /**
     * 피드 수정
     */
    @PutMapping("/{feedId}")
    public ResponseEntity<ApiResponse<FeedPostResDto>> updateFeedPost(
            @PathVariable Long feedId,
            @AuthenticationPrincipal CustomUser principal,
            @RequestBody @Valid FeedPostUpdateReqDto req
    ) {
        FeedPostResDto res =
                feedPostService.updateFeedPost(feedId, req, principal);

        return ResponseEntity.ok(
                ApiResponse.success("피드 수정 성공", res)
        );
    }

    /**
     * 피드 삭제
     */

    @DeleteMapping("/{feedId}")
    public ResponseEntity<ApiResponse<Void>> deleteFeed(
            @PathVariable Long feedId,
            @AuthenticationPrincipal CustomUser principal
    ) {
        feedPostService.deleteFeed(feedId, principal);

        return ResponseEntity.ok(
                ApiResponse.successNoData("피드 삭제 성공")
        );
    }

    /**
     * 피드 상세 조회
     */
    @GetMapping("/{feedId}")
    public ResponseEntity<ApiResponse<FeedPostResDto>> getFeedPost(
            @PathVariable Long feedId,
            @AuthenticationPrincipal CustomUser principal
    ) {
        FeedPostResDto feed = feedPostService.getFeedPost(feedId, principal);

        return ResponseEntity.ok(
                ApiResponse.success("피드 상세 조회 성공", feed)
        );
    }

    /**
     * 피드 전체 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<FeedPostResDto>>> getFeedList(
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal CustomUser principal
    ) {
        Page<FeedPostResDto> feeds =
                feedPostService.getFeedList(pageable, principal);

        return ResponseEntity.ok(
                ApiResponse.success("피드 목록 조회 성공", feeds)
        );
    }

    /**
     * 내가 작성한 피드 조회 (마이페이지)
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<FeedPostResDto>>> getMyFeedList(
            @AuthenticationPrincipal CustomUser principal,
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<FeedPostResDto> feeds =
                feedPostService.getMyFeedList(principal, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("내 피드 목록 조회 성공", feeds)
        );
    }

}