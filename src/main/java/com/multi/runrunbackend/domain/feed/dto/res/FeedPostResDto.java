package com.multi.runrunbackend.domain.feed.dto.res;

import com.multi.runrunbackend.domain.feed.entity.FeedPost;
import com.multi.runrunbackend.domain.match.constant.RunningType;
import com.multi.runrunbackend.domain.match.entity.RunningResult;
import com.multi.runrunbackend.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 *
 * @author : kimyongwon
 * @description : 피드 카드 응답 DTO
 * @filename : FeedPostResDto
 * @since : 26. 1. 3. 오후 9:03 토요일
 */
@Getter
@Builder
public class FeedPostResDto {

    private Long feedId;
    private String content;
    private LocalDateTime createdAt;

    // 작성자
    private Long userId;
    private String userName;
    private String profileImageUrl;

    // 러닝 정보
    private Long runningResultId;
    private BigDecimal totalDistance;
    private Integer totalTime;
    private BigDecimal avgPace;
    private RunningType runningType;
    private String runningTypeDescription;

    // 좋아요
    private long likeCount;

    //댓글
    private long commentCount;

    public static FeedPostResDto from(
            FeedPost feedPost,
            long likeCount,
            long commentCount

    ) {
        RunningResult r = feedPost.getRunningResult();
        User u = feedPost.getUser();

        return FeedPostResDto.builder()
                .feedId(feedPost.getId())
                .content(feedPost.getContent())
                .createdAt(feedPost.getCreatedAt())
                .userId(u.getId())
                .userName(u.getName())
                .profileImageUrl(u.getProfileImageUrl())
                .runningResultId(r.getId())
                .totalDistance(r.getTotalDistance())
                .totalTime(r.getTotalTime())
                .avgPace(r.getAvgPace())
                .runningType(r.getRunningType())
                .runningTypeDescription(r.getRunningType().getDescription())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .build();
    }
}