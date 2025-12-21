package com.multi.runrunbackend.domain.challenge.dto.req;

import com.multi.runrunbackend.domain.challenge.constant.ChallengeType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 *
 * @author : kimyongwon
 * @description : 챌린지 수정 요청 데이터를 담을 DTO
 * @filename : ChallengeUpdateReqDto
 * @since : 25. 12. 21. 오후 10:09 일요일
 */
@Getter
@Setter
@NoArgsConstructor
public class ChallengeUpdateReqDto {

    @NotBlank(message = "챌린지 제목은 필수입니다.")
    private String title;

    @NotNull(message = "챌린지 타입은 필수입니다.")
    private ChallengeType challengeType;

    @NotNull(message = "목표값은 필수입니다.")
    private Double targetValue;

    @NotBlank(message = "설명은 필수입니다.")
    private String description;

    @NotNull(message = "시작일은 필수입니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    @FutureOrPresent(message = "종료일은 현재보다 과거일 수 없습니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}