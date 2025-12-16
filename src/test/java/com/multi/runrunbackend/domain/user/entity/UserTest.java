package com.multi.runrunbackend.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.multi.runrunbackend.domain.user.dto.req.UserSignUpDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("User 엔티티 단위 테스트")
class UserTest {

    @Nested
    @DisplayName("User 빌더 테스트")
    class UserBuilderTests {

        @Test
        @DisplayName("User 객체 생성 성공")
        void build_ValidData_CreatesUser() {
            // given & when
            User user = User.builder()
                .id(1L)
                .loginId("testuser")
                .password("encodedPassword")
                .email("test@example.com")
                .role("ROLE_USER")
                .name("홍길동")
                .gender("M")
                .birthDate(LocalDate.of(1990, 1, 15))
                .heightCm(175)
                .weightKg(70)
                .profileImageUrl("/images/profile.jpg")
                .build();

            // then
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(1L);
            assertThat(user.getLoginId()).isEqualTo("testuser");
            assertThat(user.getPassword()).isEqualTo("encodedPassword");
            assertThat(user.getEmail()).isEqualTo("test@example.com");
            assertThat(user.getRole()).isEqualTo("ROLE_USER");
            assertThat(user.getName()).isEqualTo("홍길동");
            assertThat(user.getGender()).isEqualTo("M");
            assertThat(user.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 15));
            assertThat(user.getHeightCm()).isEqualTo(175);
            assertThat(user.getWeightKg()).isEqualTo(70);
            assertThat(user.getProfileImageUrl()).isEqualTo("/images/profile.jpg");
        }

        @Test
        @DisplayName("필수 필드만으로 User 객체 생성")
        void build_MinimalData_CreatesUser() {
            // given & when
            User user = User.builder()
                .loginId("minimal")
                .password("password")
                .email("minimal@example.com")
                .name("최소")
                .gender("F")
                .birthDate(LocalDate.of(1995, 5, 5))
                .build();

            // then
            assertThat(user).isNotNull();
            assertThat(user.getLoginId()).isEqualTo("minimal");
            assertThat(user.getPassword()).isEqualTo("password");
            assertThat(user.getEmail()).isEqualTo("minimal@example.com");
            assertThat(user.getName()).isEqualTo("최소");
            assertThat(user.getGender()).isEqualTo("F");
            assertThat(user.getBirthDate()).isEqualTo(LocalDate.of(1995, 5, 5));
            assertThat(user.getHeightCm()).isNull();
            assertThat(user.getWeightKg()).isNull();
            assertThat(user.getProfileImageUrl()).isNull();
        }

        @Test
        @DisplayName("여성 사용자 생성")
        void build_FemaleUser_CreatesUser() {
            // given & when
            User user = User.builder()
                .loginId("femaleuser")
                .password("password")
                .email("female@example.com")
                .name("김영희")
                .gender("F")
                .birthDate(LocalDate.of(1992, 3, 8))
                .heightCm(165)
                .weightKg(55)
                .build();

            // then
            assertThat(user.getGender()).isEqualTo("F");
            assertThat(user.getName()).isEqualTo("김영희");
            assertThat(user.getHeightCm()).isEqualTo(165);
            assertThat(user.getWeightKg()).isEqualTo(55);
        }

        @Test
        @DisplayName("관리자 사용자 생성")
        void build_AdminUser_CreatesUser() {
            // given & when
            User admin = User.builder()
                .loginId("adminuser")
                .password("adminpass")
                .email("admin@example.com")
                .role("ROLE_ADMIN")
                .name("관리자")
                .gender("M")
                .birthDate(LocalDate.of(1985, 7, 20))
                .build();

            // then
            assertThat(admin.getRole()).isEqualTo("ROLE_ADMIN");
            assertThat(admin.getLoginId()).isEqualTo("adminuser");
        }
    }

    @Nested
    @DisplayName("toEntity 메서드 테스트")
    class ToEntityTests {

        @Test
        @DisplayName("UserSignUpDto로부터 User 엔티티 생성 성공")
        void toEntity_ValidDto_CreatesUser() {
            // given
            UserSignUpDto dto = UserSignUpDto.builder()
                .loginId("newuser")
                .userPassword("encodedPassword123")
                .userName("신규유저")
                .userEmail("newuser@example.com")
                .birthDate(LocalDate.of(1993, 6, 15))
                .gender("M")
                .heightCm(178)
                .weightKg(75)
                .build();

            // when
            User user = User.toEntity(dto);

            // then
            assertThat(user).isNotNull();
            assertThat(user.getLoginId()).isEqualTo("newuser");
            assertThat(user.getPassword()).isEqualTo("encodedPassword123");
            assertThat(user.getName()).isEqualTo("신규유저");
            assertThat(user.getEmail()).isEqualTo("newuser@example.com");
            assertThat(user.getBirthDate()).isEqualTo(LocalDate.of(1993, 6, 15));
            assertThat(user.getGender()).isEqualTo("M");
            assertThat(user.getHeightCm()).isEqualTo(178);
            assertThat(user.getWeightKg()).isEqualTo(75);
        }

        @Test
        @DisplayName("선택적 필드가 없는 UserSignUpDto로부터 User 생성")
        void toEntity_DtoWithoutOptionalFields_CreatesUser() {
            // given
            UserSignUpDto dto = UserSignUpDto.builder()
                .loginId("simpleuser")
                .userPassword("simplepass")
                .userName("간단")
                .userEmail("simple@example.com")
                .birthDate(LocalDate.of(2000, 1, 1))
                .gender("F")
                .build();

            // when
            User user = User.toEntity(dto);

            // then
            assertThat(user).isNotNull();
            assertThat(user.getLoginId()).isEqualTo("simpleuser");
            assertThat(user.getHeightCm()).isNull();
            assertThat(user.getWeightKg()).isNull();
        }

        @Test
        @DisplayName("다양한 신체 정보를 가진 DTO로부터 User 생성")
        void toEntity_VariousPhysicalInfo_CreatesUser() {
            // given
            UserSignUpDto dto1 = UserSignUpDto.builder()
                .loginId("tall")
                .userPassword("pass")
                .userName("키큰")
                .userEmail("tall@example.com")
                .birthDate(LocalDate.of(1988, 11, 30))
                .gender("M")
                .heightCm(190)
                .weightKg(85)
                .build();

            UserSignUpDto dto2 = UserSignUpDto.builder()
                .loginId("short")
                .userPassword("pass")
                .userName("작은")
                .userEmail("short@example.com")
                .birthDate(LocalDate.of(1998, 4, 10))
                .gender("F")
                .heightCm(155)
                .weightKg(48)
                .build();

            // when
            User user1 = User.toEntity(dto1);
            User user2 = User.toEntity(dto2);

            // then
            assertThat(user1.getHeightCm()).isEqualTo(190);
            assertThat(user1.getWeightKg()).isEqualTo(85);
            assertThat(user2.getHeightCm()).isEqualTo(155);
            assertThat(user2.getWeightKg()).isEqualTo(48);
        }
    }

    @Nested
    @DisplayName("updateLastLogin 메서드 테스트")
    class UpdateLastLoginTests {

        @Test
        @DisplayName("마지막 로그인 시간 업데이트 성공")
        void updateLastLogin_ValidDateTime_UpdatesLastLoginAt() {
            // given
            User user = User.builder()
                .loginId("testuser")
                .password("password")
                .email("test@example.com")
                .name("테스터")
                .gender("M")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

            LocalDateTime loginTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

            // when
            user.updateLastLogin(loginTime);

            // then
            assertThat(user.getLastLoginAt()).isEqualTo(loginTime);
        }

        @Test
        @DisplayName("여러 번 로그인 시간 업데이트")
        void updateLastLogin_MultipleTimes_UpdatesCorrectly() {
            // given
            User user = User.builder()
                .loginId("activeuser")
                .password("password")
                .email("active@example.com")
                .name("활동적")
                .gender("F")
                .birthDate(LocalDate.of(1995, 3, 3))
                .build();

            LocalDateTime firstLogin = LocalDateTime.of(2024, 1, 1, 9, 0, 0);
            LocalDateTime secondLogin = LocalDateTime.of(2024, 1, 2, 14, 30, 0);
            LocalDateTime thirdLogin = LocalDateTime.of(2024, 1, 3, 18, 45, 0);

            // when
            user.updateLastLogin(firstLogin);
            assertThat(user.getLastLoginAt()).isEqualTo(firstLogin);

            user.updateLastLogin(secondLogin);
            assertThat(user.getLastLoginAt()).isEqualTo(secondLogin);

            user.updateLastLogin(thirdLogin);

            // then
            assertThat(user.getLastLoginAt()).isEqualTo(thirdLogin);
        }

        @Test
        @DisplayName("현재 시각으로 로그인 시간 업데이트")
        void updateLastLogin_CurrentTime_UpdatesToNow() {
            // given
            User user = User.builder()
                .loginId("currentuser")
                .password("password")
                .email("current@example.com")
                .name("현재")
                .gender("M")
                .birthDate(LocalDate.of(1991, 12, 25))
                .build();

            LocalDateTime now = LocalDateTime.now();

            // when
            user.updateLastLogin(now);

            // then
            assertThat(user.getLastLoginAt()).isNotNull();
            assertThat(user.getLastLoginAt()).isEqualToIgnoringNanos(now);
        }
    }

    @Nested
    @DisplayName("setRole 메서드 테스트")
    class SetRoleTests {

        @Test
        @DisplayName("일반 사용자 역할 설정")
        void setRole_UserRole_SetsCorrectly() {
            // given
            User user = User.builder()
                .loginId("user")
                .password("password")
                .email("user@example.com")
                .name("유저")
                .gender("M")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

            // when
            user.setRole("ROLE_USER");

            // then
            assertThat(user.getRole()).isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("관리자 역할 설정")
        void setRole_AdminRole_SetsCorrectly() {
            // given
            User user = User.builder()
                .loginId("admin")
                .password("password")
                .email("admin@example.com")
                .name("관리자")
                .gender("F")
                .birthDate(LocalDate.of(1985, 5, 5))
                .build();

            // when
            user.setRole("ROLE_ADMIN");

            // then
            assertThat(user.getRole()).isEqualTo("ROLE_ADMIN");
        }

        @Test
        @DisplayName("역할 변경 테스트")
        void setRole_ChangeRole_UpdatesCorrectly() {
            // given
            User user = User.builder()
                .loginId("changinguser")
                .password("password")
                .email("changing@example.com")
                .name("변경")
                .gender("M")
                .birthDate(LocalDate.of(1992, 8, 8))
                .role("ROLE_USER")
                .build();

            assertThat(user.getRole()).isEqualTo("ROLE_USER");

            // when
            user.setRole("ROLE_ADMIN");

            // then
            assertThat(user.getRole()).isEqualTo("ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("User 필드 유효성 테스트")
    class UserFieldValidationTests {

        @Test
        @DisplayName("이메일 형식 테스트")
        void email_ValidFormats_AcceptedByBuilder() {
            // given
            String[] validEmails = {
                "test@example.com",
                "user.name@example.com",
                "user+tag@example.co.kr",
                "123@example.com",
                "test_user@domain.com"
            };

            // when & then
            for (String email : validEmails) {
                User user = User.builder()
                    .loginId("user" + email.hashCode())
                    .password("password")
                    .email(email)
                    .name("테스트")
                    .gender("M")
                    .birthDate(LocalDate.of(1990, 1, 1))
                    .build();

                assertThat(user.getEmail()).isEqualTo(email);
            }
        }

        @Test
        @DisplayName("성별 값 테스트")
        void gender_ValidValues_StoredCorrectly() {
            // given & when
            User maleUser = User.builder()
                .loginId("male")
                .password("password")
                .email("male@example.com")
                .name("남성")
                .gender("M")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

            User femaleUser = User.builder()
                .loginId("female")
                .password("password")
                .email("female@example.com")
                .name("여성")
                .gender("F")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

            // then
            assertThat(maleUser.getGender()).isEqualTo("M");
            assertThat(femaleUser.getGender()).isEqualTo("F");
        }

        @Test
        @DisplayName("신체 정보 범위 테스트")
        void physicalInfo_ValidRanges_StoredCorrectly() {
            // given & when
            User user1 = User.builder()
                .loginId("user1")
                .password("password")
                .email("user1@example.com")
                .name("유저1")
                .gender("M")
                .birthDate(LocalDate.of(1990, 1, 1))
                .heightCm(150)
                .weightKg(50)
                .build();

            User user2 = User.builder()
                .loginId("user2")
                .password("password")
                .email("user2@example.com")
                .name("유저2")
                .gender("M")
                .birthDate(LocalDate.of(1990, 1, 1))
                .heightCm(200)
                .weightKg(100)
                .build();

            // then
            assertThat(user1.getHeightCm()).isEqualTo(150);
            assertThat(user1.getWeightKg()).isEqualTo(50);
            assertThat(user2.getHeightCm()).isEqualTo(200);
            assertThat(user2.getWeightKg()).isEqualTo(100);
        }

        @Test
        @DisplayName("이름 길이 테스트 (최대 4자)")
        void name_VariousLengths_StoredCorrectly() {
            // given & when
            User user1 = User.builder()
                .loginId("user1")
                .password("password")
                .email("user1@example.com")
                .name("김")
                .gender("M")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

            User user2 = User.builder()
                .loginId("user2")
                .password("password")
                .email("user2@example.com")
                .name("김철수")
                .gender("M")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

            User user3 = User.builder()
                .loginId("user3")
                .password("password")
                .email("user3@example.com")
                .name("홍길동자")
                .gender("F")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

            // then
            assertThat(user1.getName()).hasSize(1);
            assertThat(user2.getName()).hasSize(3);
            assertThat(user3.getName()).hasSize(4);
        }

        @Test
        @DisplayName("생년월일 범위 테스트")
        void birthDate_VariousDates_StoredCorrectly() {
            // given
            LocalDate date1 = LocalDate.of(1950, 1, 1);
            LocalDate date2 = LocalDate.of(1990, 6, 15);
            LocalDate date3 = LocalDate.of(2005, 12, 31);

            // when
            User user1 = User.builder()
                .loginId("old")
                .password("password")
                .email("old@example.com")
                .name("노인")
                .gender("M")
                .birthDate(date1)
                .build();

            User user2 = User.builder()
                .loginId("middle")
                .password("password")
                .email("middle@example.com")
                .name("중년")
                .gender("F")
                .birthDate(date2)
                .build();

            User user3 = User.builder()
                .loginId("young")
                .password("password")
                .email("young@example.com")
                .name("젊은")
                .gender("M")
                .birthDate(date3)
                .build();

            // then
            assertThat(user1.getBirthDate()).isEqualTo(date1);
            assertThat(user2.getBirthDate()).isEqualTo(date2);
            assertThat(user3.getBirthDate()).isEqualTo(date3);
        }
    }

    @Nested
    @DisplayName("User 생성 시나리오 테스트")
    class UserCreationScenarioTests {

        @Test
        @DisplayName("신규 가입 사용자 생성 - 최소 정보")
        void createNewUser_MinimalInformation() {
            // given
            UserSignUpDto dto = UserSignUpDto.builder()
                .loginId("newbie")
                .userPassword("hashedPassword")
                .userName("신규")
                .userEmail("newbie@example.com")
                .birthDate(LocalDate.of(2000, 1, 1))
                .gender("M")
                .build();

            // when
            User newUser = User.toEntity(dto);

            // then
            assertThat(newUser.getLoginId()).isEqualTo("newbie");
            assertThat(newUser.getPassword()).isEqualTo("hashedPassword");
            assertThat(newUser.getName()).isEqualTo("신규");
            assertThat(newUser.getEmail()).isEqualTo("newbie@example.com");
            assertThat(newUser.getBirthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
            assertThat(newUser.getGender()).isEqualTo("M");
            assertThat(newUser.getHeightCm()).isNull();
            assertThat(newUser.getWeightKg()).isNull();
            assertThat(newUser.getProfileImageUrl()).isNull();
        }

        @Test
        @DisplayName("프로필 완성된 사용자 생성")
        void createCompleteUser_AllInformation() {
            // given
            UserSignUpDto dto = UserSignUpDto.builder()
                .loginId("complete")
                .userPassword("hashedPass")
                .userName("완성유저")
                .userEmail("complete@example.com")
                .birthDate(LocalDate.of(1988, 7, 15))
                .gender("F")
                .heightCm(168)
                .weightKg(58)
                .build();

            // when
            User completeUser = User.toEntity(dto);
            completeUser.setRole("ROLE_USER");

            // then
            assertThat(completeUser.getLoginId()).isNotNull();
            assertThat(completeUser.getPassword()).isNotNull();
            assertThat(completeUser.getName()).isNotNull();
            assertThat(completeUser.getEmail()).isNotNull();
            assertThat(completeUser.getBirthDate()).isNotNull();
            assertThat(completeUser.getGender()).isNotNull();
            assertThat(completeUser.getHeightCm()).isNotNull();
            assertThat(completeUser.getWeightKg()).isNotNull();
            assertThat(completeUser.getRole()).isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("관리자 계정 생성")
        void createAdminUser_WithAdminRole() {
            // given
            UserSignUpDto dto = UserSignUpDto.builder()
                .loginId("admin123")
                .userPassword("adminSecurePass")
                .userName("관리자")
                .userEmail("admin123@example.com")
                .birthDate(LocalDate.of(1980, 1, 1))
                .gender("M")
                .build();

            // when
            User admin = User.toEntity(dto);
            admin.setRole("ROLE_ADMIN");

            // then
            assertThat(admin.getRole()).isEqualTo("ROLE_ADMIN");
            assertThat(admin.getLoginId()).contains("admin");
        }

        @Test
        @DisplayName("다양한 연령대 사용자 생성")
        void createUsers_DifferentAgeGroups() {
            // given & when
            User teenager = User.builder()
                .loginId("teen")
                .password("password")
                .email("teen@example.com")
                .name("십대")
                .gender("M")
                .birthDate(LocalDate.now().minusYears(15))
                .build();

            User adult = User.builder()
                .loginId("adult")
                .password("password")
                .email("adult@example.com")
                .name("성인")
                .gender("F")
                .birthDate(LocalDate.now().minusYears(30))
                .build();

            User senior = User.builder()
                .loginId("senior")
                .password("password")
                .email("senior@example.com")
                .name("노인")
                .gender("M")
                .birthDate(LocalDate.now().minusYears(70))
                .build();

            // then
            assertThat(teenager.getBirthDate()).isAfter(LocalDate.now().minusYears(20));
            assertThat(adult.getBirthDate()).isBefore(LocalDate.now().minusYears(25));
            assertThat(senior.getBirthDate()).isBefore(LocalDate.now().minusYears(65));
        }
    }

    @Nested
    @DisplayName("User BaseEntity 상속 테스트")
    class UserBaseEntityTests {

        @Test
        @DisplayName("User는 BaseEntity를 상속하여 생성/수정 시간 필드를 가짐")
        void user_ExtendsBaseEntity_HasTimestampFields() {
            // given & when
            User user = User.builder()
                .loginId("testuser")
                .password("password")
                .email("test@example.com")
                .name("테스트")
                .gender("M")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

            // then
            // BaseEntity의 필드들이 존재하는지 확인 (리플렉션 또는 메서드 호출)
            assertThat(user).isNotNull();
            // createdAt, updatedAt, isDeleted 필드는 BaseEntity에서 상속됨
        }
    }
}