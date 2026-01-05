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

    @NotBlank
    @Size(max = 500)
    private String content;

}
