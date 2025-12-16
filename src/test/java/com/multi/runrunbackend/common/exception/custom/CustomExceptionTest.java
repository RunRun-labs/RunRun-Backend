package com.multi.runrunbackend.common.exception.custom;

import static org.assertj.core.api.Assertions.assertThat;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CustomException 계층 구조 테스트")
class CustomExceptionTest {

    @Nested
    @DisplayName("NotFoundException 테스트")
    class NotFoundExceptionTests {

        @Test
        @DisplayName("NotFoundException 생성 및 ErrorCode 확인")
        void create_WithErrorCode_Success() {
            // given
            ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;

            // when
            NotFoundException exception = new NotFoundException(errorCode);

            // then
            assertThat(exception).isInstanceOf(CustomException.class);
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            assertThat(exception.getMessage()).isEqualTo(errorCode.getMessage());
        }

        @Test
        @DisplayName("다양한 ErrorCode로 NotFoundException 생성")
        void create_WithDifferentErrorCodes_Success() {
            // given
            ErrorCode[] errorCodes = {
                ErrorCode.USER_NOT_FOUND,
                ErrorCode.INVALID_REQUEST
            };

            for (ErrorCode errorCode : errorCodes) {
                // when
                NotFoundException exception = new NotFoundException(errorCode);

                // then
                assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            }
        }
    }

    @Nested
    @DisplayName("TokenException 테스트")
    class TokenExceptionTests {

        @Test
        @DisplayName("TokenException 생성 및 ErrorCode 확인")
        void create_WithErrorCode_Success() {
            // given
            ErrorCode errorCode = ErrorCode.INVALID_TOKEN;

            // when
            TokenException exception = new TokenException(errorCode);

            // then
            assertThat(exception).isInstanceOf(CustomException.class);
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            assertThat(exception.getMessage()).isEqualTo(errorCode.getMessage());
        }

        @Test
        @DisplayName("다양한 토큰 관련 ErrorCode로 생성")
        void create_WithTokenErrorCodes_Success() {
            // given
            ErrorCode[] errorCodes = {
                ErrorCode.INVALID_TOKEN,
                ErrorCode.EXPIRED_TOKEN,
                ErrorCode.TOKEN_NOT_FOUND
            };

            for (ErrorCode errorCode : errorCodes) {
                // when
                TokenException exception = new TokenException(errorCode);

                // then
                assertThat(exception.getErrorCode()).isEqualTo(errorCode);
                assertThat(exception.getMessage()).contains("토큰");
            }
        }
    }

    @Nested
    @DisplayName("DuplicateUsernameException 테스트")
    class DuplicateUsernameExceptionTests {

        @Test
        @DisplayName("DuplicateUsernameException 생성 및 ErrorCode 확인")
        void create_WithErrorCode_Success() {
            // given
            ErrorCode errorCode = ErrorCode.DUPLICATE_USER;

            // when
            DuplicateUsernameException exception = new DuplicateUsernameException(errorCode);

            // then
            assertThat(exception).isInstanceOf(CustomException.class);
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        }
    }

    @Nested
    @DisplayName("RefreshTokenException 테스트")
    class RefreshTokenExceptionTests {

        @Test
        @DisplayName("RefreshTokenException 생성 및 ErrorCode 확인")
        void create_WithErrorCode_Success() {
            // given
            ErrorCode errorCode = ErrorCode.REFRESH_TOKEN_EXPIRED;

            // when
            RefreshTokenException exception = new RefreshTokenException(errorCode);

            // then
            assertThat(exception).isInstanceOf(CustomException.class);
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            assertThat(exception.getMessage()).contains("리프레시 토큰");
        }

        @Test
        @DisplayName("다양한 리프레시 토큰 ErrorCode로 생성")
        void create_WithRefreshTokenErrorCodes_Success() {
            // given
            ErrorCode[] errorCodes = {
                ErrorCode.REFRESH_TOKEN_EXPIRED,
                ErrorCode.INVALID_REFRESH_TOKEN
            };

            for (ErrorCode errorCode : errorCodes) {
                // when
                RefreshTokenException exception = new RefreshTokenException(errorCode);

                // then
                assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            }
        }
    }

    @Nested
    @DisplayName("FileUploadException 테스트")
    class FileUploadExceptionTests {

        @Test
        @DisplayName("FileUploadException 생성 및 ErrorCode 확인")
        void create_WithErrorCode_Success() {
            // given
            ErrorCode errorCode = ErrorCode.FILE_UPLOAD_FAILED;

            // when
            FileUploadException exception = new FileUploadException(errorCode);

            // then
            assertThat(exception).isInstanceOf(CustomException.class);
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            assertThat(exception.getMessage()).contains("파일 업로드");
        }
    }

    @Nested
    @DisplayName("예외 계층 구조 테스트")
    class ExceptionHierarchyTests {

        @Test
        @DisplayName("모든 커스텀 예외가 CustomException을 상속")
        void allCustomExceptions_ExtendCustomException() {
            // given & when & then
            assertThat(new NotFoundException(ErrorCode.USER_NOT_FOUND))
                .isInstanceOf(CustomException.class);
            assertThat(new TokenException(ErrorCode.INVALID_TOKEN))
                .isInstanceOf(CustomException.class);
            assertThat(new DuplicateUsernameException(ErrorCode.DUPLICATE_USER))
                .isInstanceOf(CustomException.class);
            assertThat(new RefreshTokenException(ErrorCode.REFRESH_TOKEN_EXPIRED))
                .isInstanceOf(CustomException.class);
            assertThat(new FileUploadException(ErrorCode.FILE_UPLOAD_FAILED))
                .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("모든 커스텀 예외가 RuntimeException을 상속")
        void allCustomExceptions_ExtendRuntimeException() {
            // given & when & then
            assertThat(new NotFoundException(ErrorCode.USER_NOT_FOUND))
                .isInstanceOf(RuntimeException.class);
            assertThat(new TokenException(ErrorCode.INVALID_TOKEN))
                .isInstanceOf(RuntimeException.class);
        }
    }
}