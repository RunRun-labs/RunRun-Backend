package com.multi.runrunbackend.domain.user.dto.req;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserSignUpDto 유효성 검증 테스트")
class UserSignUpDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("유효한 데이터 테스트")
    class ValidDataTests {

        @Test
        @DisplayName("모든 필드가 유효한 경우")
        void validDto_AllFieldsValid() {
            // given
            UserSignUpDto dto = createValidDto();

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("선택적 필드(키, 몸무게)가 없는 경우")
        void validDto_OptionalFieldsNull() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setHeightCm(null);
            dto.setWeightKg(null);

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("LoginId 검증 테스트")
    class LoginIdValidationTests {

        @Test
        @DisplayName("LoginId가 null인 경우")
        void loginId_Null() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setLoginId(null);

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("아이디는 필수 입력 사항입니다"));
        }

        @Test
        @DisplayName("LoginId가 빈 문자열인 경우")
        void loginId_Empty() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setLoginId("");

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("LoginId가 5자 미만인 경우")
        void loginId_TooShort() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setLoginId("test"); // 4자

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("아이디는 최대 15자여야 합니다"));
        }

        @Test
        @DisplayName("LoginId가 10자를 초과하는 경우")
        void loginId_TooLong() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setLoginId("thisislonger"); // 12자

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("LoginId가 정확히 5자인 경우 (경계값)")
        void loginId_ExactlyMinLength() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setLoginId("testU"); // 5자

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("LoginId가 정확히 10자인 경우 (경계값)")
        void loginId_ExactlyMaxLength() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setLoginId("testuser10"); // 10자

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Password 검증 테스트")
    class PasswordValidationTests {

        @Test
        @DisplayName("Password가 null인 경우")
        void password_Null() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setUserPassword(null);

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("비밀번호는 필수 입력 사항입니다"));
        }

        @Test
        @DisplayName("Password가 8자 미만인 경우")
        void password_TooShort() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setUserPassword("Pass12!"); // 7자

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("비밀번호는 최대 18자여야 합니다"));
        }

        @Test
        @DisplayName("Password가 18자를 초과하는 경우")
        void password_TooLong() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setUserPassword("ThisPasswordIsTooLong123!"); // 25자

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Password가 정확히 8자인 경우 (경계값)")
        void password_ExactlyMinLength() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setUserPassword("Pass123!"); // 8자

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Email 검증 테스트")
    class EmailValidationTests {

        @Test
        @DisplayName("Email이 null인 경우")
        void email_Null() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setUserEmail(null);

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("이메일은 필수 입력 사항입니다"));
        }

        @Test
        @DisplayName("Email 형식이 잘못된 경우 - @ 없음")
        void email_InvalidFormat_NoAtSign() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setUserEmail("invalidemail.com");

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("이메일 형식을 유지해야 합니다"));
        }

        @Test
        @DisplayName("Email 형식이 잘못된 경우 - 도메인 없음")
        void email_InvalidFormat_NoDomain() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setUserEmail("test@");

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("유효한 Email 형식들")
        void email_ValidFormats() {
            String[] validEmails = {
                "test@example.com",
                "user.name@example.com",
                "user+tag@example.co.kr",
                "test123@test-domain.com"
            };

            for (String email : validEmails) {
                UserSignUpDto dto = createValidDto();
                dto.setUserEmail(email);

                Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

                assertThat(violations).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Name 검증 테스트")
    class NameValidationTests {

        @Test
        @DisplayName("Name이 null인 경우")
        void name_Null() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setUserName(null);

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("이름은 필수 입력 사항입니다"));
        }

        @Test
        @DisplayName("Name이 4자를 초과하는 경우")
        void name_TooLong() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setUserName("가나다라마"); // 5자

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("이름은 최대 4자여야 합니다"));
        }

        @Test
        @DisplayName("Name이 정확히 4자인 경우")
        void name_ExactlyMaxLength() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setUserName("김철수입"); // 4자

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("BirthDate 검증 테스트")
    class BirthDateValidationTests {

        @Test
        @DisplayName("BirthDate가 null인 경우")
        void birthDate_Null() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setBirthDate(null);

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("생년월일은 필수 입력 사항입니다"));
        }

        @Test
        @DisplayName("BirthDate가 미래 날짜인 경우")
        void birthDate_Future() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setBirthDate(LocalDate.now().plusDays(1));

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("생년월일은 오늘 또는 과거 날짜만 입력할 수 있습니다"));
        }

        @Test
        @DisplayName("BirthDate가 오늘인 경우")
        void birthDate_Today() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setBirthDate(LocalDate.now());

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("BirthDate가 과거인 경우")
        void birthDate_Past() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setBirthDate(LocalDate.of(1990, 1, 1));

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Gender 검증 테스트")
    class GenderValidationTests {

        @Test
        @DisplayName("Gender가 null인 경우")
        void gender_Null() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setGender(null);

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("성별은 필수 입력 사항입니다"));
        }

        @Test
        @DisplayName("Gender가 M인 경우")
        void gender_Male() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setGender("M");

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Gender가 F인 경우")
        void gender_Female() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setGender("F");

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Gender가 잘못된 값인 경우")
        void gender_Invalid() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setGender("X");

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("성별은 MALE 또는 FEMALE만 입력 가능합니다"));
        }
    }

    @Nested
    @DisplayName("Height 검증 테스트")
    class HeightValidationTests {

        @Test
        @DisplayName("Height가 50cm 미만인 경우")
        void height_TooSmall() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setHeightCm(49);

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("키는 50cm 이상이어야 합니다"));
        }

        @Test
        @DisplayName("Height가 300cm를 초과하는 경우")
        void height_TooLarge() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setHeightCm(301);

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("키는 300cm 이하이어야 합니다"));
        }

        @Test
        @DisplayName("Height가 경계값인 경우")
        void height_BoundaryValues() {
            // 50cm
            UserSignUpDto dto1 = createValidDto();
            dto1.setHeightCm(50);
            assertThat(validator.validate(dto1)).isEmpty();

            // 300cm
            UserSignUpDto dto2 = createValidDto();
            dto2.setHeightCm(300);
            assertThat(validator.validate(dto2)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Weight 검증 테스트")
    class WeightValidationTests {

        @Test
        @DisplayName("Weight가 10kg 미만인 경우")
        void weight_TooSmall() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setWeightKg(9);

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("몸무게는 10kg 이상이어야 합니다"));
        }

        @Test
        @DisplayName("Weight가 500kg를 초과하는 경우")
        void weight_TooLarge() {
            // given
            UserSignUpDto dto = createValidDto();
            dto.setWeightKg(501);

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("몸무게는 500kg 이하이어야 합니다"));
        }

        @Test
        @DisplayName("Weight가 경계값인 경우")
        void weight_BoundaryValues() {
            // 10kg
            UserSignUpDto dto1 = createValidDto();
            dto1.setWeightKg(10);
            assertThat(validator.validate(dto1)).isEmpty();

            // 500kg
            UserSignUpDto dto2 = createValidDto();
            dto2.setWeightKg(500);
            assertThat(validator.validate(dto2)).isEmpty();
        }
    }

    @Nested
    @DisplayName("복합 검증 테스트")
    class CombinedValidationTests {

        @Test
        @DisplayName("여러 필드가 동시에 유효하지 않은 경우")
        void multipleFieldsInvalid() {
            // given
            UserSignUpDto dto = UserSignUpDto.builder()
                .loginId("abc") // too short
                .userPassword("pass") // too short
                .userName("가나다라마") // too long
                .userEmail("invalid-email") // invalid format
                .birthDate(LocalDate.now().plusDays(1)) // future
                .gender("X") // invalid
                .heightCm(49) // too small
                .weightKg(9) // too small
                .build();

            // when
            Set<ConstraintViolation<UserSignUpDto>> violations = validator.validate(dto);

            // then
            assertThat(violations).hasSizeGreaterThanOrEqualTo(8);
        }
    }

    // Helper method
    private UserSignUpDto createValidDto() {
        return UserSignUpDto.builder()
            .loginId("testuser")
            .userPassword("Password123!")
            .userName("테스트")
            .userEmail("test@example.com")
            .birthDate(LocalDate.of(1990, 1, 1))
            .gender("M")
            .heightCm(175)
            .weightKg(70)
            .build();
    }
}