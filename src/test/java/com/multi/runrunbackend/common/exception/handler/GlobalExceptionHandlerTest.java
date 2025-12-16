package com.multi.runrunbackend.common.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.multi.runrunbackend.common.exception.custom.CustomException;
import com.multi.runrunbackend.common.exception.custom.DuplicateUsernameException;
import com.multi.runrunbackend.common.exception.custom.FileUploadException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.custom.RefreshTokenException;
import com.multi.runrunbackend.common.exception.custom.TokenException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("GlobalExceptionHandler 단위 테스트")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("handleBusinessException 메서드 테스트")
    class HandleBusinessExceptionTests {

        @Test
        @DisplayName("NotFoundException 처리")
        void handleBusinessException_NotFoundException_ReturnsNotFound() {
            // given
            CustomException exception = new NotFoundException(ErrorCode.USER_NOT_FOUND);

            // when
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(
                exception);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getState()).isEqualTo(404);
            assertThat(response.getBody().getMessage()).isEqualTo(
                ErrorCode.USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("TokenException 처리")
        void handleBusinessException_TokenException_ReturnsUnauthorized() {
            // given
            CustomException exception = new TokenException(ErrorCode.INVALID_TOKEN);

            // when
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(
                exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getState()).isEqualTo(401);
            assertThat(response.getBody().getMessage()).contains("유효하지 않은 토큰");
        }

        @Test
        @DisplayName("DuplicateUsernameException 처리")
        void handleBusinessException_DuplicateUsernameException_ReturnsConflict() {
            // given
            CustomException exception = new DuplicateUsernameException(ErrorCode.DUPLICATE_USER);

            // when
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(
                exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getState()).isEqualTo(409);
        }

        @Test
        @DisplayName("RefreshTokenException 처리")
        void handleBusinessException_RefreshTokenException_ReturnsUnauthorized() {
            // given
            CustomException exception = new RefreshTokenException(
                ErrorCode.REFRESH_TOKEN_EXPIRED);

            // when
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(
                exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("리프레시 토큰");
        }

        @Test
        @DisplayName("FileUploadException 처리")
        void handleBusinessException_FileUploadException_ReturnsInternalServerError() {
            // given
            CustomException exception = new FileUploadException(ErrorCode.FILE_UPLOAD_FAILED);

            // when
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(
                exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("파일 업로드");
        }

        @Test
        @DisplayName("다양한 ErrorCode 처리")
        void handleBusinessException_DifferentErrorCodes_ReturnsCorrectResponses() {
            // given
            ErrorCode[] errorCodes = {
                ErrorCode.INVALID_REQUEST,
                ErrorCode.UNAUTHORIZED,
                ErrorCode.EXPIRED_TOKEN,
                ErrorCode.DUPLICATE_EMAIL
            };

            // when & then
            for (ErrorCode errorCode : errorCodes) {
                CustomException exception = new TokenException(errorCode);
                ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(
                    exception);

                assertThat(response.getStatusCode()).isEqualTo(errorCode.getHttpStatus());
                assertThat(response.getBody().getMessage()).isEqualTo(errorCode.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("handleException 메서드 테스트")
    class HandleExceptionTests {

        @Test
        @DisplayName("일반 Exception 처리")
        void handleException_GeneralException_ReturnsInternalServerError() {
            // given
            Exception exception = new Exception("Unexpected error");

            // when
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleException(
                exception);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getState()).isEqualTo(500);
            assertThat(response.getBody().getMessage()).isEqualTo(
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        }

        @Test
        @DisplayName("NullPointerException 처리")
        void handleException_NullPointerException_ReturnsInternalServerError() {
            // given
            Exception exception = new NullPointerException("Null value encountered");

            // when
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleException(
                exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("RuntimeException 처리")
        void handleException_RuntimeException_ReturnsInternalServerError() {
            // given
            Exception exception = new RuntimeException("Runtime error occurred");

            // when
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleException(
                exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().getMessage()).contains("서버 오류");
        }

        @Test
        @DisplayName("IllegalArgumentException 처리")
        void handleException_IllegalArgumentException_ReturnsInternalServerError() {
            // given
            Exception exception = new IllegalArgumentException("Invalid argument");

            // when
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleException(
                exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("예외 처리 우선순위 테스트")
    class ExceptionPriorityTests {

        @Test
        @DisplayName("CustomException이 일반 Exception보다 우선 처리됨")
        void customExceptionTakesPrecedence() {
            // given
            CustomException customException = new NotFoundException(ErrorCode.USER_NOT_FOUND);

            // when
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(
                customException);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().getMessage()).isEqualTo(
                ErrorCode.USER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("응답 형식 검증 테스트")
    class ResponseFormatTests {

        @Test
        @DisplayName("에러 응답이 ApiResponse 형식을 따름")
        void errorResponse_FollowsApiResponseFormat() {
            // given
            CustomException exception = new NotFoundException(ErrorCode.USER_NOT_FOUND);

            // when
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(
                exception);

            // then
            ApiResponse<Void> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getState()).isNotNull();
            assertThat(body.getMessage()).isNotNull();
            assertThat(body.getData()).isNull();
        }

        @Test
        @DisplayName("모든 에러 응답에 상태 코드와 메시지 포함")
        void allErrorResponses_ContainStatusAndMessage() {
            // given
            CustomException[] exceptions = {
                new NotFoundException(ErrorCode.USER_NOT_FOUND),
                new TokenException(ErrorCode.INVALID_TOKEN),
                new DuplicateUsernameException(ErrorCode.DUPLICATE_USER)
            };

            // when & then
            for (CustomException exception : exceptions) {
                ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(
                    exception);

                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().getState()).isGreaterThan(0);
                assertThat(response.getBody().getMessage()).isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("실제 시나리오 테스트")
    class RealWorldScenarioTests {

        @Test
        @DisplayName("로그인 실패 - 사용자 없음")
        void loginFailed_UserNotFound_Scenario() {
            // given
            CustomException exception = new NotFoundException(ErrorCode.USER_NOT_FOUND);

            // when
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(
                exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().getMessage()).contains("사용자");
        }

        @Test
        @DisplayName("회원가입 실패 - 중복 이메일")
        void signupFailed_DuplicateEmail_Scenario() {
            // given
            CustomException exception = new DuplicateUsernameException(ErrorCode.DUPLICATE_EMAIL);

            // when
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(
                exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().getMessage()).contains("이메일");
        }

        @Test
        @DisplayName("API 호출 실패 - 토큰 만료")
        void apiCallFailed_TokenExpired_Scenario() {
            // given
            CustomException exception = new TokenException(ErrorCode.EXPIRED_TOKEN);

            // when
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(
                exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().getMessage()).contains("만료");
        }

        @Test
        @DisplayName("파일 업로드 실패")
        void fileUploadFailed_Scenario() {
            // given
            CustomException exception = new FileUploadException(ErrorCode.FILE_UPLOAD_FAILED);

            // when
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(
                exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().getMessage()).contains("파일");
        }

        @Test
        @DisplayName("예상치 못한 서버 에러")
        void unexpectedServerError_Scenario() {
            // given
            Exception exception = new RuntimeException("Database connection failed");

            // when
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleException(
                exception);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().getMessage()).contains("서버 오류");
        }
    }
}