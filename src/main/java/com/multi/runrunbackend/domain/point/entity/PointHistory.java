package com.multi.runrunbackend.domain.point.entity;

import com.multi.runrunbackend.common.entitiy.BaseCreatedEntity;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : BoKyung
 * @description : 포인트 변동 내역 엔티티
 * @filename : PointHistory
 * @since : 25. 12. 17. 수요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PointHistory extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "point_product_id", nullable = false)
    private PointProduct pointProduct;

    @Column(name = "point_type", nullable = false, length = 20)
    private String pointType; // EARN, USE

    @Column(name = "change_amount", nullable = false)
    private Integer changeAmount;

    @Column(name = "reason", nullable = false, length = 50)
    private String reason; // RUNNING_COMPLETE, ATTENDANCE, INVITE, CREW_JOIN, PRODUCT_EXCHANGE, REFUND


    /**
     * @description : toEntity - 엔티티 생성 정적 팩토리 메서드
     * @filename : PointHistory
     * @author : BoKyung
     * @since : 25. 12. 17. 수요일
     */
    public static PointHistory toEntity(User user, PointProduct pointProduct, String pointType,
        Integer changeAmount, String reason) {
        return PointHistory.builder()
            .user(user)
            .pointProduct(pointProduct)
            .pointType(pointType)
            .changeAmount(changeAmount)
            .reason(reason)
            .build();
    }
}
