package com.multi.runrunbackend.domain.user.entity;

import com.multi.runrunbackend.common.entitiy.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

/**
 * @author : BoKyung
 * @description : 사용자 출석 엔티티
 * @filename : UserAttendance
 * @since : 2026. 01. 09. 금요일
 */
@Entity
@Table(
        name = "user_attendance",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "attendance_date"})
        },
        indexes = {
                @Index(name = "idx_user_attendance_date", columnList = "user_id, attendance_date")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserAttendance extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    public static UserAttendance create(User user, LocalDate attendanceDate) {
        return UserAttendance.builder()
                .user(user)
                .attendanceDate(attendanceDate)
                .build();
    }
}
