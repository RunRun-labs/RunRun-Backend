package com.multi.runrunbackend.domain.feed.dto.req;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "피드 내용은 비어 있을 수 없습니다.")
    @Size(max = 500, message = "피드 내용은 최대 500자까지 작성할 수 있습니다.")
    private String content;
}