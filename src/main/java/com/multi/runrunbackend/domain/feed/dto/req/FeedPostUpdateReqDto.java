package com.multi.runrunbackend.domain.feed.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *
 * @author : kimyongwon
 * @description : 피드 게시물 수정 요청 DTO
 * @filename : FeedPostUpdateReqDto
 * @since : 26. 1. 5. 오전 10:33 월요일
 */
@Getter
@NoArgsConstructor
public class FeedPostUpdateReqDto {

    @NotBlank(message = "피드 내용은 비어 있을 수 없습니다.")
    @Size(max = 500, message = "피드 내용은 최대 500자까지 작성할 수 있습니다.")
    private String content;

}
