package com.multi.runrunbackend.domain.challenge.dto.res;

import com.multi.runrunbackend.domain.challenge.constant.ChallengeType;
import com.multi.runrunbackend.domain.challenge.constant.UserChallengeStatus;
import com.multi.runrunbackend.domain.challenge.entity.Challenge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 *
 * @author : kimyongwon
 * @description : 챌린지 정보 응답 객체
 * @filename : ChallengeResDto
 * @since : 25. 12. 21. 오후 9:24 일요일
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class ChallengeResDto {
    private Long id;
    private String title;
    private ChallengeType challengeType;
    private Double targetValue;
    private String description;
    private String imageUrl;
    private LocalDate startDate;
    private LocalDate endDate;


    private UserChallengeStatus myStatus;
    private Double progressValue;

    private Long participantCount;


    public static ChallengeResDto from(Challenge challenge) {
        return ChallengeResDto.builder()
                .id(challenge.getId())
                .title(challenge.getTitle())
                .challengeType(challenge.getChallengeType())
                .targetValue(challenge.getTargetValue())
                .description(challenge.getDescription())
                .imageUrl(challenge.getImageUrl())
                .startDate(challenge.getStartDate())
                .endDate(challenge.getEndDate())
                .build();
    }
}