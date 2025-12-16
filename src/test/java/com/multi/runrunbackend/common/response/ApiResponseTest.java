package com.multi.runrunbackend.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ApiResponse 단위 테스트")
class ApiResponseTest {

    @Nested
    @DisplayName("success 메서드 테스트")
    class SuccessTests {

        @Test
        @DisplayName("데이터와 함께 성공 응답 생성")
        void success_WithData_ReturnsSuccessResponse() {
            // given
            String testData = "Test Data";

            // when
            ApiResponse<String> response = ApiResponse.success(testData);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getCode()).isEqualTo("SUCCESS");
            assertThat(response.getMessage()).isEqualTo("요청이 성공적으로 처리되었습니다.");
            assertThat(response.getData()).isEqualTo(testData);
        }

        @Test
        @DisplayName("커스텀 메시지와 데이터를 포함한 성공 응답 생성")
        void success_WithMessageAndData_ReturnsCustomResponse() {
            // given
            String customMessage = "사용자 정보 조회 성공";
            String testData = "User Data";

            // when
            ApiResponse<String> response = ApiResponse.success(customMessage, testData);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getCode()).isEqualTo("SUCCESS");
            assertThat(response.getMessage()).isEqualTo(customMessage);
            assertThat(response.getData()).isEqualTo(testData);
        }

        @Test
        @DisplayName("null 데이터로 성공 응답 생성")
        void success_WithNullData_WorksCorrectly() {
            // when
            ApiResponse<String> response = ApiResponse.success(null);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isNull();
        }

        @Test
        @DisplayName("복잡한 객체를 데이터로 포함한 성공 응답")
        void success_WithComplexObject_ReturnsResponseWithObject() {
            // given
            TestDto testDto = new TestDto("test", 123);

            // when
            ApiResponse<TestDto> response = ApiResponse.success(testDto);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getData()).isEqualTo(testDto);
            assertThat(response.getData().getName()).isEqualTo("test");
            assertThat(response.getData().getValue()).isEqualTo(123);
        }
    }

    @Nested
    @DisplayName("error 메서드 테스트")
    class ErrorTests {

        @Test
        @DisplayName("ErrorCode로 에러 응답 생성")
        void error_WithErrorCode_ReturnsErrorResponse() {
            // given
            ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;

            // when
            ApiResponse<Void> response = ApiResponse.error(errorCode);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(errorCode.getCode());
            assertThat(response.getMessage()).isEqualTo(errorCode.getMessage());
            assertThat(response.getData()).isNull();
        }

        @Test
        @DisplayName("ErrorCode와 커스텀 메시지로 에러 응답 생성")
        void error_WithErrorCodeAndCustomMessage_ReturnsCustomErrorResponse() {
            // given
            ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;
            String customMessage = "해당 ID의 사용자를 찾을 수 없습니다";

            // when
            ApiResponse<Void> response = ApiResponse.error(errorCode, customMessage);

            // then
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(errorCode.getCode());
            assertThat(response.getMessage()).isEqualTo(customMessage);
            assertThat(response.getData()).isNull();
        }

        @Test
        @DisplayName("다양한 ErrorCode로 에러 응답 생성")
        void error_DifferentErrorCodes_ReturnsCorrectResponses() {
            // given
            ErrorCode[] errorCodes = {
                ErrorCode.USER_NOT_FOUND,
                ErrorCode.INVALID_TOKEN,
                ErrorCode.EXPIRED_TOKEN,
                ErrorCode.DUPLICATE_EMAIL,
                ErrorCode.UNAUTHORIZED
            };

            // when & then
            for (ErrorCode errorCode : errorCodes) {
                ApiResponse<Void> response = ApiResponse.error(errorCode);

                assertThat(response.isSuccess()).isFalse();
                assertThat(response.getCode()).isEqualTo(errorCode.getCode());
                assertThat(response.getMessage()).isEqualTo(errorCode.getMessage());
            }
        }

        @Test
        @DisplayName("400 Bad Request 에러 응답")
        void error_BadRequest_ReturnsErrorWithCorrectCode() {
            // given
            ErrorCode errorCode = ErrorCode.INVALID_REQUEST;

            // when
            ApiResponse<Void> response = ApiResponse.error(errorCode);

            // then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(errorCode.getCode());
            assertThat(response.getMessage()).contains("잘못된 요청");
        }

        @Test
        @DisplayName("401 Unauthorized 에러 응답")
        void error_Unauthorized_ReturnsCorrectError() {
            // given
            ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

            // when
            ApiResponse<Void> response = ApiResponse.error(errorCode);

            // then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(errorCode.getCode());
        }

        @Test
        @DisplayName("404 Not Found 에러 응답")
        void error_NotFound_ReturnsCorrectError() {
            // given
            ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;

            // when
            ApiResponse<Void> response = ApiResponse.error(errorCode);

            // then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(errorCode.getCode());
        }

        @Test
        @DisplayName("409 Conflict 에러 응답")
        void error_Conflict_ReturnsCorrectError() {
            // given
            ErrorCode errorCode = ErrorCode.DUPLICATE_EMAIL;

            // when
            ApiResponse<Void> response = ApiResponse.error(errorCode);

            // then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(errorCode.getCode());
        }

        @Test
        @DisplayName("500 Internal Server Error 응답")
        void error_InternalServerError_ReturnsCorrectError() {
            // given
            ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

            // when
            ApiResponse<Void> response = ApiResponse.error(errorCode);

            // then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(errorCode.getCode());
        }
    }

    @Nested
    @DisplayName("응답 구조 테스트")
    class ResponseStructureTests {

        @Test
        @DisplayName("성공 응답이 올바른 플래그를 포함")
        void successResponse_HasCorrectSuccessFlag() {
            // when
            ApiResponse<String> response = ApiResponse.success("test");

            // then
            assertThat(response.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("에러 응답이 올바른 플래그를 포함")
        void errorResponse_HasCorrectSuccessFlag() {
            // when
            ApiResponse<Void> response = ApiResponse.error(ErrorCode.USER_NOT_FOUND);

            // then
            assertThat(response.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("응답 객체의 모든 필드 접근 가능")
        void response_AllFieldsAccessible() {
            // given
            String testData = "data";
            String testMessage = "message";

            // when
            ApiResponse<String> response = ApiResponse.success(testMessage, testData);

            // then
            assertThat(response.isSuccess()).isNotNull();
            assertThat(response.getCode()).isNotNull();
            assertThat(response.getMessage()).isNotNull();
            assertThat(response.getData()).isNotNull();
        }
    }

    @Nested
    @DisplayName("실제 사용 시나리오 테스트")
    class RealWorldScenarioTests {

        @Test
        @DisplayName("사용자 조회 성공 시나리오")
        void userRetrievalSuccess_Scenario() {
            // given
            TestDto userData = new TestDto("John Doe", 12345);

            // when
            ApiResponse<TestDto> response = ApiResponse.success("사용자 조회 성공", userData);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getCode()).isEqualTo("SUCCESS");
            assertThat(response.getMessage()).isEqualTo("사용자 조회 성공");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().getName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("로그인 성공 시나리오")
        void loginSuccess_Scenario() {
            // given
            TestDto tokenData = new TestDto("Bearer", 1234567890);

            // when
            ApiResponse<TestDto> response = ApiResponse.success("로그인 성공", tokenData);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("로그인 성공");
            assertThat(response.getData()).isNotNull();
        }

        @Test
        @DisplayName("데이터 삭제 성공 시나리오")
        void deleteSuccess_Scenario() {
            // when
            ApiResponse<Void> response = ApiResponse.success("삭제가 완료되었습니다.", null);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("삭제가 완료되었습니다.");
            assertThat(response.getData()).isNull();
        }

        @Test
        @DisplayName("사용자 없음 에러 시나리오")
        void userNotFound_Scenario() {
            // when
            ApiResponse<Void> response = ApiResponse.error(ErrorCode.USER_NOT_FOUND);

            // then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).contains("사용자");
            assertThat(response.getData()).isNull();
        }

        @Test
        @DisplayName("토큰 만료 에러 시나리오")
        void tokenExpired_Scenario() {
            // when
            ApiResponse<Void> response = ApiResponse.error(ErrorCode.EXPIRED_TOKEN);

            // then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).contains("만료");
        }

        @Test
        @DisplayName("커스텀 에러 메시지 시나리오")
        void customErrorMessage_Scenario() {
            // when
            ApiResponse<Void> response = ApiResponse.error(ErrorCode.USER_NOT_FOUND,
                "ID가 123인 사용자를 찾을 수 없습니다");

            // then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND.getCode());
            assertThat(response.getMessage()).isEqualTo("ID가 123인 사용자를 찾을 수 없습니다");
        }
    }

    // Helper class for testing
    private static class TestDto {

        private String name;
        private int value;

        public TestDto(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}