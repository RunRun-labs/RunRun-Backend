package com.multi.runrunbackend.domain.payment.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.payment.dto.req.BillingFirstPaymentConfirmReqDto;
import com.multi.runrunbackend.domain.payment.dto.req.FreePaymentConfirmReqDto;
import com.multi.runrunbackend.domain.payment.dto.req.PaymentApproveReqDto;
import com.multi.runrunbackend.domain.payment.dto.req.PaymentRequestReqDto;
import com.multi.runrunbackend.domain.payment.dto.res.PaymentApproveResDto;
import com.multi.runrunbackend.domain.payment.dto.res.PaymentHistoryResDto;
import com.multi.runrunbackend.domain.payment.dto.res.PaymentRequestResDto;
import com.multi.runrunbackend.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * @author : BoKyung
 * @description : 결제 관리 컨트롤러
 * @filename : PaymentController
 * @since : 2026. 1. 1.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 요청 생성 (POST /api/payments/request)
     */
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<PaymentRequestResDto>> createPaymentRequest(
            @AuthenticationPrincipal CustomUser principal,
            @Valid @RequestBody PaymentRequestReqDto req
    ) {
        PaymentRequestResDto res = paymentService.createPaymentRequest(principal, req);
        return ResponseEntity.ok(
                ApiResponse.success("결제 요청 생성 완료", res)
        );
    }

    /**
     * 결제 승인 (POST /api/payments/confirm)
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentApproveResDto>> confirmPayment(
            @AuthenticationPrincipal CustomUser principal,
            @Valid @RequestBody PaymentApproveReqDto req
    ) {
        PaymentApproveResDto result = paymentService.confirmPayment(principal, req);
        return ResponseEntity.ok(ApiResponse.success("결제 승인 완료", result));
    }

    /**
     * 무료 결제 승인 (0원 결제)
     */
    @PostMapping("/confirm-free")
    public ResponseEntity<ApiResponse<PaymentApproveResDto>> confirmFreePayment(
            @AuthenticationPrincipal CustomUser principal,
            @Valid @RequestBody FreePaymentConfirmReqDto request
    ) {
        PaymentApproveResDto res = paymentService.confirmFreePayment(principal, request.getOrderId());
        return ResponseEntity.ok(
                ApiResponse.success("무료 결제 처리 완료", res)
        );
    }

    @PostMapping("/billing/confirm")
    public ResponseEntity<ApiResponse<PaymentApproveResDto>> confirmBilling(
            @AuthenticationPrincipal CustomUser principal,
            @Valid @RequestBody BillingFirstPaymentConfirmReqDto request
    ) {
        PaymentApproveResDto res = paymentService.confirmBillingFirstPayment(principal, request);
        return ResponseEntity.ok(ApiResponse.success("빌링키 발급 및 첫 결제 완료", res));
    }

    /**
     * 결제 내역 조회 (GET /api/payments/history)
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<PaymentHistoryResDto>>> getPaymentHistory(
            @AuthenticationPrincipal CustomUser principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<PaymentHistoryResDto> res = paymentService.getPaymentHistory(principal, pageable);
        return ResponseEntity.ok(
                ApiResponse.success("결제 내역 조회 성공", res)
        );
    }
}
