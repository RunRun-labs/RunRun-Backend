package com.multi.runrunbackend.domain.payment.client;

import com.multi.runrunbackend.common.exception.custom.ExternalApiException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.payment.dto.req.PaymentApproveReqDto;
import com.multi.runrunbackend.domain.payment.dto.req.TossBillingKeyIssueReqDto;
import com.multi.runrunbackend.domain.payment.dto.req.TossBillingPaymentReqDto;
import com.multi.runrunbackend.domain.payment.dto.res.TossBillingKeyResDto;
import com.multi.runrunbackend.domain.payment.dto.res.TossPaymentResDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * @author : BoKyung
 * @description : 토스페이먼츠 API 클라이언트
 * @filename : TossPaymentClient
 * @since : 2026. 1. 1.
 */
@Component
@Slf4j
public class TossPaymentClient {

    private final WebClient webClient;

    @Value("${toss.payments.secret-key}")
    private String secretKey;

    public TossPaymentClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.tosspayments.com")
                .build();
    }

    /**
     * @description : 일반 결제 승인
     */
    public TossPaymentResDto approvePayment(PaymentApproveReqDto req) {
        log.info("토스 결제 승인 요청 - orderId: {}", req.getOrderId());

        try {
            TossPaymentResDto response = webClient
                    .post()
                    .uri("/v1/payments/confirm")
                    .header(HttpHeaders.AUTHORIZATION, createAuthHeader())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("토스 결제 승인 실패 - 응답: {}", errorBody);
                                        return Mono.error(new ExternalApiException(ErrorCode.TOSS_API_FAILED));
                                    })
                    )
                    .bodyToMono(TossPaymentResDto.class)
                    .block();

            if (response == null) {
                throw new ExternalApiException(ErrorCode.TOSS_API_FAILED);
            }

            log.info("토스 결제 승인 성공");
            return response;

        } catch (Exception e) {
            log.error("토스 결제 승인 실패", e);
            throw new ExternalApiException(ErrorCode.PAYMENT_APPROVAL_FAILED);
        }
    }

    // 빌링키 발급
    public TossBillingKeyResDto issueBillingKey(TossBillingKeyIssueReqDto req) {
        log.info("빌링키 발급 요청 - customerKey: {}", req.getCustomerKey());

        try {
            TossBillingKeyResDto response = webClient
                    .post()
                    .uri("/v1/billing/authorizations/issue")
                    .header(HttpHeaders.AUTHORIZATION, createAuthHeader())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("빌링키 발급 실패 - 응답: {}", errorBody);
                                        return Mono.error(new ExternalApiException(ErrorCode.BILLING_KEY_ISSUE_FAILED));
                                    })
                    )
                    .bodyToMono(TossBillingKeyResDto.class)
                    .block();

            if (response == null) {
                throw new ExternalApiException(ErrorCode.BILLING_KEY_ISSUE_FAILED);
            }

            log.info("빌링키 발급 성공");
            return response;

        } catch (Exception e) {
            log.error("빌링키 발급 실패", e);
            throw new ExternalApiException(ErrorCode.BILLING_KEY_ISSUE_FAILED);
        }
    }

    // 빌링키로 자동결제
    public TossPaymentResDto payWithBillingKey(String billingKey, TossBillingPaymentReqDto req) {
        log.info("빌링키 자동결제 요청 - customerKey: {}", req.getCustomerKey());

        try {
            TossPaymentResDto response = webClient
                    .post()
                    .uri("/v1/billing/" + billingKey)
                    .header(HttpHeaders.AUTHORIZATION, createAuthHeader())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(req)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("빌링키 결제 실패 - 응답: {}", errorBody);
                                        return Mono.error(new ExternalApiException(ErrorCode.BILLING_PAYMENT_FAILED));
                                    })
                    )
                    .bodyToMono(TossPaymentResDto.class)
                    .block(Duration.ofSeconds(12));

            if (response == null) {
                throw new ExternalApiException(ErrorCode.BILLING_PAYMENT_FAILED);
            }

            log.info("빌링키 결제 성공");
            return response;

        } catch (Exception e) {
            log.error("빌링키 결제 실패", e);
            throw new ExternalApiException(ErrorCode.BILLING_PAYMENT_FAILED);
        }
    }

    private String createAuthHeader() {
        String credentials = secretKey + ":";
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedCredentials;
    }
}
