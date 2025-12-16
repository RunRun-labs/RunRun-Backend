package com.multi.runrunbackend.common.exception.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCode Enum 단위 테스트")
class ErrorCodeTest {

    @Nested
    @DisplayName("공통 에러 코드 테스트")
    class CommonErrorCodesTests {

        @Test
        @DisplayName("INVALID_REQUEST - 400 Bad Request")
        void invalidRequest() {
            // when
            ErrorCode errorCode = ErrorCode.INVALID_REQUEST;

            // then
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(errorCode.getCode()).isEqualTo("C001");
            assertThat(errorCode.getMessage()).isEqualTo("잘못된 요청입니다.");
        }

        @Test
        @DisplayName("INTERNAL_SERVER_ERROR - 500 Internal Server Error")
        void internalServerError() {
            // when
            ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

            // then
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(errorCode.getCode()).isEqualTo("C002");
            assertThat(errorCode.getMessage()).isEqualTo("서버 오류가 발생했습니다.");
        }
    }

    @Nested
    @DisplayName("사용자 관련 에러 코드 테스트")
    class UserErrorCodesTests {

        @Test
        @DisplayName("USER_NOT_FOUND - 404 Not Found")
        void userNotFound() {
            // when
            ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;

            // then
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(errorCode.getCode()).isEqualTo("U001");
            assertThat(errorCode.getMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("DUPLICATE_EMAIL - 409 Conflict")
        void duplicateEmail() {
            // when
            ErrorCode errorCode = ErrorCode.DUPLICATE_EMAIL;

            // then
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(errorCode.getCode()).isEqualTo("U002");
            assertThat(errorCode.getMessage()).isEqualTo("이미 사용 중인 이메일입니다.");
        }

        @Test
        @DisplayName("INVALID_PASSWORD - 400 Bad Request")
        void invalidPassword() {
            // when
            ErrorCode errorCode = ErrorCode.INVALID_PASSWORD;

            // then
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(errorCode.getCode()).isEqualTo("U003");
            assertThat(errorCode.getMessage()).isEqualTo("비밀번호가 올바르지 않습니다.");
        }

        @Test
        @DisplayName("UNAUTHORIZED - 401 Unauthorized")
        void unauthorized() {
            // when
            ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

            // then
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(errorCode.getCode()).isEqualTo("U004");
            assertThat(errorCode.getMessage()).isEqualTo("인증이 필요합니다.");
        }

        @Test
        @DisplayName("DUPLICATE_USER - 409 Conflict")
        void duplicateUser() {
            // when
            ErrorCode errorCode = ErrorCode.DUPLICATE_USER;

            // then
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(errorCode.getCode()).isEqualTo("U005");
            assertThat(errorCode.getMessage()).isEqualTo("이미 사용 중인 아이디 입니다");
        }
    }

    @Nested
    @DisplayName("토큰 관련 에러 코드 테스트")
    class TokenErrorCodesTests {

        @Test
        @DisplayName("INVALID_TOKEN - 401 Unauthorized")
        void invalidToken() {
            // when
            ErrorCode errorCode = ErrorCode.INVALID_TOKEN;

            // then
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(errorCode.getCode()).isEqualTo("A001");
            assertThat(errorCode.getMessage()).isEqualTo("유효하지 않은 토큰입니다.");
        }

        @Test
        @DisplayName("EXPIRED_TOKEN - 401 Unauthorized")
        void expiredToken() {
            // when
            ErrorCode errorCode = ErrorCode.EXPIRED_TOKEN;

            // then
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(errorCode.getCode()).isEqualTo("A002");
            assertThat(errorCode.getMessage()).isEqualTo("만료된 토큰입니다.");
        }

        @Test
        @DisplayName("TOKEN_NOT_FOUND - 401 Unauthorized")
        void tokenNotFound() {
            // when
            ErrorCode errorCode = ErrorCode.TOKEN_NOT_FOUND;

            // then
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(errorCode.getCode()).isEqualTo("A003");
            assertThat(errorCode.getMessage()).isEqualTo("토큰이 존재하지 않습니다.");
        }

        @Test
        @DisplayName("MISSING_AUTHORIZATION_HEADER - 401 Unauthorized")
        void missingAuthorizationHeader() {
            // when
            ErrorCode errorCode = ErrorCode.MISSING_AUTHORIZATION_HEADER;

            // then
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(errorCode.getCode()).isEqualTo("A004");
            assertThat(errorCode.getMessage()).isEqualTo("Authorization 헤더가 존재하지 않습니다");
        }

        @Test
        @DisplayName("TOKEN_BAD_REQUEST - 400 Bad Request")
        void tokenBadRequest() {
            // when
            ErrorCode errorCode = ErrorCode.TOKEN_BAD_REQUEST;

            // then
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(errorCode.getCode()).isEqualTo("A005");
            assertThat(errorCode.getMessage()).isEqualTo("토큰 값이 올바르지 않습니다");
        }
    }

    @Nested
    @DisplayName("리프레시 토큰 관련 에러 코드 테스트")
    class RefreshTokenErrorCodesTests {

        @Test
        @DisplayName("REFRESH_TOKEN_EXPIRED - 401 Unauthorized")
        void refreshTokenExpired() {
            // when
            ErrorCode errorCode = ErrorCode.REFRESH_TOKEN_EXPIRED;

            // then
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(errorCode.getCode()).isEqualTo("AUTH_101");
            assertThat(errorCode.getMessage()).isEqualTo("리프레시 토큰이 만료되었습니다. 다시 로그인하세요.");
        }

        @Test
        @DisplayName("INVALID_REFRESH_TOKEN - 401 Unauthorized")
        void invalidRefreshToken() {
            // when
            ErrorCode errorCode = ErrorCode.INVALID_REFRESH_TOKEN;

            // then
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(errorCode.getCode()).isEqualTo("AUTH_102");
            assertThat(errorCode.getMessage()).isEqualTo("유효하지 않은 리프레시 토큰입니다.");
        }
    }

    @Nested
    @DisplayName("파일 관련 에러 코드 테스트")
    class FileErrorCodesTests {

        @Test
        @DisplayName("FILE_UPLOAD_FAILED - 500 Internal Server Error")
        void fileUploadFailed() {
            // when
            ErrorCode errorCode = ErrorCode.FILE_UPLOAD_FAILED;

            // then
            assertThat(errorCode.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(errorCode.getCode()).isEqualTo("F001");
            assertThat(errorCode.getMessage()).isEqualTo("파일 업로드에 실패했습니다.");
        }
    }

    @Nested
    @DisplayName("Enum 특성 테스트")
    class EnumCharacteristicsTests {

        @Test
        @DisplayName("모든 ErrorCode 값 확인")
        void allErrorCodes() {
            // when
            ErrorCode[] allCodes = ErrorCode.values();

            // then
            assertThat(allCodes).isNotEmpty();
            assertThat(allCodes.length).isGreaterThanOrEqualTo(17);
        }

        @Test
        @DisplayName("ErrorCode valueOf 테스트")
        void valueOf() {
            // when
            ErrorCode errorCode = ErrorCode.valueOf("USER_NOT_FOUND");

            // then
            assertThat(errorCode).isEqualTo(ErrorCode.USER_NOT_FOUND);
            assertThat(errorCode.getCode()).isEqualTo("U001");
        }

        @Test
        @DisplayName("모든 ErrorCode는 null이 아닌 속성을 가짐")
        void allCodesHaveNonNullProperties() {
            // given
            ErrorCode[] allCodes = ErrorCode.values();

            // then
            for (ErrorCode code : allCodes) {
                assertThat(code.getHttpStatus()).isNotNull();
                assertThat(code.getCode()).isNotNull();
                assertThat(code.getMessage()).isNotNull();
            }
        }
    }
}