package com.multi.runrunbackend.domain.point.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * @author : BoKyung
 * @description : 포인트 유효기간 관련 엔티티
 * @filename : PointExpiration
 * @since : 25. 12. 17. 수요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PointExpiration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_history_id", nullable = false)
    private PointHistory pointHistory;

    @Column(name = "earned_point", nullable = false)
    private Integer earnedPoint;

    @Column(name = "remaining_point", nullable = false)
    private Integer remainingPoint;

    @Column(name = "expiration_status", length = 20)
    private String expirationStatus; // ACTIVE, USED, EXPIRED

    @Column(name = "earned_at")
    private LocalDateTime earnedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;  // 만료 예정일

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    /**
     * @description : toEntity - 엔티티 생성 정적 팩토리 메서드
     * @filename : PointExpiration
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public static PointExpiration toEntity(User user, PointHistory pointHistory,
                                           Integer earnedPoint) {
        LocalDateTime now = LocalDateTime.now();
        return PointExpiration.builder()
                .user(user)
                .pointHistory(pointHistory)
                .earnedPoint(earnedPoint)
                .remainingPoint(earnedPoint)
                .expirationStatus("ACTIVE")
                .earnedAt(now)
                .expiresAt(now.plusYears(1)) // 1년 후 만료
                .build();
    }

    /**
     * @description : usePoint - 포인트 사용 (FIFO)
     * @filename : PointExpiration
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void usePoint(Integer amount) {
        if (this.remainingPoint < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }
        this.remainingPoint -= amount;
        if (this.remainingPoint == 0) {
            this.expirationStatus = "USED";
        }
    }

    /**
     * @description : expire - 포인트 만료 처리
     * @filename : PointExpiration
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public void expire() {
        this.expirationStatus = "EXPIRED";
        this.expiredAt = LocalDateTime.now();
    }
}
