package com.multi.runrunbackend.domain.payment.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.payment.dto.req.PaymentRequestReqDto;
import com.multi.runrunbackend.domain.payment.dto.res.PaymentRequestResDto;
import com.multi.runrunbackend.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
