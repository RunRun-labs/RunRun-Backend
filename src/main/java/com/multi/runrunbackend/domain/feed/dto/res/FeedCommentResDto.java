package com.multi.runrunbackend.domain.feed.dto.res;

import com.multi.runrunbackend.domain.feed.entity.FeedComment;
import com.multi.runrunbackend.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 *
 * @author : kimyongwon
 * @description : 댓글 조회 응답 DTO
 * @filename : FeedCommentResDto
 * @since : 26. 1. 5. 오후 1:44 월요일
 */
@Getter
@Builder
public class FeedCommentResDto {

    private Long commentId;
    private String content;

    private Long userId;
    //    private String userName;
    private String userLoginId;
    private String profileImageUrl;

    private LocalDateTime createdAt;

    public static FeedCommentResDto from(FeedComment comment) {
        User user = comment.getUser();

        return FeedCommentResDto.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .userId(user.getId())
//                .userName(user.getName())
                .userLoginId(user.getLoginId())
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
