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

    // 러닝 결과
    private Long runningResultId;
    private BigDecimal totalDistance;
    private Integer totalTime;
    private BigDecimal avgPace;
    private LocalDateTime startedAt;

    // runningType 노출
    private RunningType runningType;
    private String runningTypeDescription;

    // 사용자
    private Long userId;
    private String userName;
    private String profileImageUrl;

    // 피드 내용
    private String content;

    private LocalDateTime createdAt;

    public static FeedPostResDto from(FeedPost feedPost) {
        RunningResult r = feedPost.getRunningResult();
        User u = feedPost.getUser();

        return FeedPostResDto.builder()
                .feedId(feedPost.getId())
                .runningResultId(r.getId())
                .totalDistance(r.getTotalDistance())
                .totalTime(r.getTotalTime())
                .avgPace(r.getAvgPace())
                .startedAt(r.getStartedAt())
                .runningType(r.getRunningType())
                .runningTypeDescription(r.getRunningType().getDescription())
                .userId(u.getId())
                .userName(u.getName())
                .profileImageUrl(u.getProfileImageUrl())
                .content(feedPost.getContent())
                .createdAt(feedPost.getCreatedAt())
                .build();
    }
}