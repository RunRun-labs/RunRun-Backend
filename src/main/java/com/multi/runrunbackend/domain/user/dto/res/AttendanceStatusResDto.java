package com.multi.runrunbackend.domain.user.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * @author : BoKyung
 * @description : 출석 현황 조회 응답 DTO
 * @filename : AttendanceStatusResDto
 * @since : 2026. 01. 09. 금요일
 */
@Getter
@Builder
@AllArgsConstructor
public class AttendanceStatusResDto {
    private Boolean attendedToday;
    private Integer monthlyCount;
    private List<Integer> attendedDays;
    private LocalDate todayDate;
    private Integer currentYear;
    private Integer currentMonth;
}

