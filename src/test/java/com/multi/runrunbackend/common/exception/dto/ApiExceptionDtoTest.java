package com.multi.runrunbackend.common.exception.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("ApiExceptionDto 단위 테스트")
class ApiExceptionDtoTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTests {

        @Test
        @DisplayName("HttpStatus와 메시지로 생성")
        void create_WithHttpStatusAndMessage_Success() {
            // given
            HttpStatus status = HttpStatus.BAD_REQUEST;
            String message = "잘못된 요청입니다";

            // when
            ApiExceptionDto dto = new ApiExceptionDto(status, message);

            // then
            assertThat(dto.getState()).isEqualTo(400);
            assertThat(dto.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("다양한 HttpStatus로 생성")
        void create_WithDifferentHttpStatuses_Success() {
            // given
            HttpStatus[] statuses = {
                HttpStatus.BAD_REQUEST,
                HttpStatus.UNAUTHORIZED,
                HttpStatus.NOT_FOUND,
                HttpStatus.INTERNAL_SERVER_ERROR
            };

            for (HttpStatus status : statuses) {
                // when
                ApiExceptionDto dto = new ApiExceptionDto(status, "Test message");

                // then
                assertThat(dto.getState()).isEqualTo(status.value());
            }
        }

        @Test
        @DisplayName("기본 생성자로 생성")
        void create_WithNoArgsConstructor_Success() {
            // when
            ApiExceptionDto dto = new ApiExceptionDto();

            // then
            assertThat(dto).isNotNull();
        }
    }

    @Nested
    @DisplayName("필드 접근 테스트")
    class FieldAccessTests {

        @Test
        @DisplayName("state 필드 접근")
        void getState_ReturnsCorrectValue() {
            // given
            ApiExceptionDto dto = new ApiExceptionDto(HttpStatus.NOT_FOUND, "Not found");

            // when
            int state = dto.getState();

            // then
            assertThat(state).isEqualTo(404);
        }

        @Test
        @DisplayName("message 필드 접근")
        void getMessage_ReturnsCorrectValue() {
            // given
            String expectedMessage = "Test error message";
            ApiExceptionDto dto = new ApiExceptionDto(HttpStatus.BAD_REQUEST, expectedMessage);

            // when
            String message = dto.getMessage();

            // then
            assertThat(message).isEqualTo(expectedMessage);
        }
    }

    @Nested
    @DisplayName("실제 사용 시나리오 테스트")
    class RealWorldScenarioTests {

        @Test
        @DisplayName("400 Bad Request 에러 DTO 생성")
        void badRequest_Scenario() {
            // when
            ApiExceptionDto dto = new ApiExceptionDto(HttpStatus.BAD_REQUEST,
                "입력값이 올바르지 않습니다");

            // then
            assertThat(dto.getState()).isEqualTo(400);
            assertThat(dto.getMessage()).contains("입력값");
        }

        @Test
        @DisplayName("401 Unauthorized 에러 DTO 생성")
        void unauthorized_Scenario() {
            // when
            ApiExceptionDto dto = new ApiExceptionDto(HttpStatus.UNAUTHORIZED,
                "인증이 필요합니다");

            // then
            assertThat(dto.getState()).isEqualTo(401);
            assertThat(dto.getMessage()).contains("인증");
        }

        @Test
        @DisplayName("404 Not Found 에러 DTO 생성")
        void notFound_Scenario() {
            // when
            ApiExceptionDto dto = new ApiExceptionDto(HttpStatus.NOT_FOUND,
                "사용자를 찾을 수 없습니다");

            // then
            assertThat(dto.getState()).isEqualTo(404);
            assertThat(dto.getMessage()).contains("찾을 수 없습니다");
        }

        @Test
        @DisplayName("500 Internal Server Error DTO 생성")
        void internalServerError_Scenario() {
            // when
            ApiExceptionDto dto = new ApiExceptionDto(HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 오류가 발생했습니다");

            // then
            assertThat(dto.getState()).isEqualTo(500);
            assertThat(dto.getMessage()).contains("서버 오류");
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTests {

        @Test
        @DisplayName("toString이 유효한 문자열 반환")
        void toString_ReturnsValidString() {
            // given
            ApiExceptionDto dto = new ApiExceptionDto(HttpStatus.BAD_REQUEST, "Error message");

            // when
            String result = dto.toString();

            // then
            assertThat(result).isNotNull();
            assertThat(result).contains("400");
            assertThat(result).contains("Error message");
        }
    }
}