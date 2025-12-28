package com.multi.runrunbackend.domain.challenge.dto.req;

import com.multi.runrunbackend.domain.challenge.constant.ChallengeType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 *
 * @author : kimyongwon
 * @description : 챌린지 생성 요청 객체
 * @filename : ChallengeCreateReqDto
 * @since : 25. 12. 21. 오후 9:23 일요일
 */
@Getter
@Setter
@NoArgsConstructor
public class ChallengeReqDto {

    @NotBlank(message = "챌린지 제목은 필수입니다.")
    @Size(min = 2, max = 50, message = "제목은 2자 이상 50자 이하로 입력해주세요.")
    private String title;

    @NotNull(message = "챌린지 타입은 필수입니다.")
    private ChallengeType challengeType;

    @NotNull(message = "목표값은 필수입니다.")
    @DecimalMin(value = "0.1", message = "목표값은 0보다 커야 합니다.")
    @DecimalMax(value = "12000", message = "목표값이 너무 큽니다.")
    private Double targetValue;

    @NotBlank(message = "설명은 필수입니다.")
    @Size(min = 10, max = 500, message = "설명은 10자 이상 500자 이하로 입력해주세요.")
    private String description;
    ;

    @NotNull(message = "시작일은 필수입니다.")
    @FutureOrPresent(message = "시작일은 오늘 이후여야 합니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    @FutureOrPresent(message = "종료일은 현재보다 과거일 수 없습니다.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}