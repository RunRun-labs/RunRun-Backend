package com.multi.runrunbackend.domain.payment.repository;

import com.multi.runrunbackend.domain.payment.entity.Payment;
import com.multi.runrunbackend.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
    Optional<Payment> findByOrderId(String orderId);


    /**
     * @description : 최근 빌링키 조회
     */
    Optional<Payment> findFirstByUserAndBillingKeyIsNotNullOrderByCreatedAtDesc(User user);

}
