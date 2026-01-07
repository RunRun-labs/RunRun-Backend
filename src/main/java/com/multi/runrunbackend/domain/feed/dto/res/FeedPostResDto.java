package com.multi.runrunbackend.domain.feed.dto.res;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    //    private String userName;
    private String userLoginId;
    private String profileImageUrl;

    // 러닝 정보
    private Long runningResultId;
    private BigDecimal totalDistance;
    private Integer totalTime;
    private BigDecimal avgPace;
    private RunningType runningType;
    private String runningTypeDescription;

    // 코스 정보
    private String courseTitle;

    // 좋아요
    private long likeCount;
    @JsonProperty("isLiked")
    private boolean isLiked; // 현재 사용자가 좋아요를 눌렀는지 여부

    //댓글
    private long commentCount;

    // 피드 이미지
    private String imageUrl;

    public static FeedPostResDto from(
            FeedPost feedPost,
            long likeCount,
            long commentCount,
            boolean isLiked

    ) {
        RunningResult r = feedPost.getRunningResult();
        User u = feedPost.getUser();

        return FeedPostResDto.builder()
                .feedId(feedPost.getId())
                .content(feedPost.getContent())
                .createdAt(feedPost.getCreatedAt())
                .userId(u.getId())
//                .userName(u.getName())
                .userLoginId(u.getLoginId())
                .profileImageUrl(u.getProfileImageUrl())
                .runningResultId(r.getId())
                .totalDistance(r.getTotalDistance())
                .totalTime(r.getTotalTime())
                .avgPace(r.getAvgPace())
                .runningType(r.getRunningType())
                .runningTypeDescription(r.getRunningType().getDescription())
                .courseTitle(r.getCourse() != null ? r.getCourse().getAddress() : null)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .commentCount(commentCount)
                .imageUrl(feedPost.getImageUrl())
                .build();
    }
}