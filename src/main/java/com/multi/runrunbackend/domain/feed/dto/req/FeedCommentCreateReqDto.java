package com.multi.runrunbackend.domain.feed.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *
 * @author : kimyongwon
 * @description : 피드 댓글 등록 요청 DTO
 * @filename : FeedCommentCreateReqDto
 * @since : 26. 1. 5. 오후 1:07 월요일
 */
@Getter
@NoArgsConstructor
public class FeedCommentCreateReqDto {

    @NotBlank(message = "댓글 내용은 비어 있을 수 없습니다.")
    @Size(max = 100, message = "댓글은 최대 100자까지 입력할 수 있습니다.")
    private String content;
}