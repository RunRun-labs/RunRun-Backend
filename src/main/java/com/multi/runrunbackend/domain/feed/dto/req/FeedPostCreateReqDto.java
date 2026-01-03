package com.multi.runrunbackend.domain.feed.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *
 * @author : kimyongwon
 * @description : 피드 게시물 생성 요청 DTO
 * @filename : FeedPostCreateDto
 * @since : 26. 1. 3. 오후 9:02 토요일
 */
@Getter
@NoArgsConstructor
public class FeedPostCreateReqDto {

    @NotNull
    private Long runningResultId;

    @Size(max = 500)
    private String content;
}