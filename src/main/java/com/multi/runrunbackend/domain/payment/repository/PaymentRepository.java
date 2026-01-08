package com.multi.runrunbackend.domain.payment.repository;

import com.multi.runrunbackend.domain.payment.constant.PaymentStatus;
import com.multi.runrunbackend.domain.payment.entity.Payment;
import com.multi.runrunbackend.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @author : BoKyung
 * @description : 결제 레포지토리
 * @filename : PaymentRepository
 * @since : 2026. 1. 1.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * @description : 특정 사용자의 결제 내역 조회
     */
    Page<Payment> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * @description : orderId로 결제 내역 찾기
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId")
    Optional<Payment> findByOrderIdWithLock(@Param("orderId") String orderId);

    /**
     * @description : 최근 빌링키 조회
     */
    Optional<Payment> findFirstByUserAndBillingKeyIsNotNullOrderByCreatedAtDesc(User user);

    /**
     * @description : 사용자의 특정 상태 결제 목록 조회
     */
    List<Payment> findByUserAndPaymentStatus(User user, PaymentStatus paymentStatus);

    /**
     * @description : 특정 시간 이전에 생성된 READY 상태 결제 조회
     */
    @Query("SELECT p FROM Payment p WHERE p.paymentStatus = :status " +
            "AND p.createdAt < :cutoffTime")
    List<Payment> findOldPaymentsByStatus(
            @Param("status") PaymentStatus status,
            @Param("cutoffTime") LocalDateTime cutoffTime
    );
}
