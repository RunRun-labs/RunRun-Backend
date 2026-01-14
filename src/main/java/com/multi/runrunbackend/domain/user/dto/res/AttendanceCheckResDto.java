package com.multi.runrunbackend.domain.user.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * @author : BoKyung
 * @description : 출석 체크 응답 DTO
 * @filename : AttendanceCheckResDto
 * @since : 2026. 01. 09. 금요일
 */
@Getter
@Builder
@AllArgsConstructor
public class AttendanceCheckResDto {

    private LocalDate attendanceDate;
    private Integer pointsEarned;
    private Integer monthlyCount;
    private String message;
}
