package com.multi.runrunbackend.domain.user.repository;

import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.entity.UserAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * @author : BoKyung
 * @description : 사용자 출석 Repository
 * @filename : UserAttendanceRepository
 * @since : 2026. 01. 09. 금요일
 */
public interface UserAttendanceRepository extends JpaRepository<UserAttendance, Long> {

    /**
     * 특정 날짜에 출석했는지 확인
     */
    boolean existsByUserAndAttendanceDate(User user, LocalDate attendanceDate);

    /**
     * 이번 달 출석 일수 조회
     */
    @Query("SELECT COUNT(a) FROM UserAttendance a " +
            "WHERE a.user = :user " +
            "AND YEAR(a.attendanceDate) = :year " +
            "AND MONTH(a.attendanceDate) = :month")
    int countMonthlyAttendance(@Param("user") User user,
                               @Param("year") int year,
                               @Param("month") int month);

    /**
     * 이번 달 출석한 날짜 목록 조회
     */
    @Query("SELECT a.attendanceDate FROM UserAttendance a " +
            "WHERE a.user = :user " +
            "AND YEAR(a.attendanceDate) = :year " +
            "AND MONTH(a.attendanceDate) = :month " +
            "ORDER BY a.attendanceDate ASC")
    List<LocalDate> findMonthlyAttendanceDates(@Param("user") User user,
                                               @Param("year") int year,
                                               @Param("month") int month);
}
