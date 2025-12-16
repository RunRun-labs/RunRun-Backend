package com.multi.runrunbackend.common.file;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FileDomainType 단위 테스트")
class FileDomainTypeTest {

    @Nested
    @DisplayName("Enum 값 테스트")
    class EnumValueTests {

        @Test
        @DisplayName("모든 FileDomainType이 정의되어 있음")
        void allFileDomainTypes_AreDefined() {
            // when
            FileDomainType[] types = FileDomainType.values();

            // then
            assertThat(types).hasSize(6);
            assertThat(types).contains(
                FileDomainType.PROFILE,
                FileDomainType.COURSE_THUMBNAIL,
                FileDomainType.COURSE_IMAGE,
                FileDomainType.AD_IMAGE,
                FileDomainType.CREW_IMAGE,
                FileDomainType.PRODUCT_IMAGE
            );
        }

        @Test
        @DisplayName("PROFILE 타입의 디렉토리 경로")
        void profile_HasCorrectDir() {
            // when
            String dir = FileDomainType.PROFILE.getDir();

            // then
            assertThat(dir).isEqualTo("profile");
        }

        @Test
        @DisplayName("COURSE_THUMBNAIL 타입의 디렉토리 경로")
        void courseThumbnail_HasCorrectDir() {
            // when
            String dir = FileDomainType.COURSE_THUMBNAIL.getDir();

            // then
            assertThat(dir).isEqualTo("course/thumbnail");
        }

        @Test
        @DisplayName("COURSE_IMAGE 타입의 디렉토리 경로")
        void courseImage_HasCorrectDir() {
            // when
            String dir = FileDomainType.COURSE_IMAGE.getDir();

            // then
            assertThat(dir).isEqualTo("course/image");
        }

        @Test
        @DisplayName("AD_IMAGE 타입의 디렉토리 경로")
        void adImage_HasCorrectDir() {
            // when
            String dir = FileDomainType.AD_IMAGE.getDir();

            // then
            assertThat(dir).isEqualTo("ad");
        }

        @Test
        @DisplayName("CREW_IMAGE 타입의 디렉토리 경로")
        void crewImage_HasCorrectDir() {
            // when
            String dir = FileDomainType.CREW_IMAGE.getDir();

            // then
            assertThat(dir).isEqualTo("crew");
        }

        @Test
        @DisplayName("PRODUCT_IMAGE 타입의 디렉토리 경로")
        void productImage_HasCorrectDir() {
            // when
            String dir = FileDomainType.PRODUCT_IMAGE.getDir();

            // then
            assertThat(dir).isEqualTo("product");
        }
    }

    @Nested
    @DisplayName("Enum 동작 테스트")
    class EnumBehaviorTests {

        @Test
        @DisplayName("valueOf로 enum 조회")
        void valueOf_ReturnsCorrectEnum() {
            // when
            FileDomainType type = FileDomainType.valueOf("PROFILE");

            // then
            assertThat(type).isEqualTo(FileDomainType.PROFILE);
            assertThat(type.getDir()).isEqualTo("profile");
        }

        @Test
        @DisplayName("각 도메인 타입이 고유한 디렉토리를 가짐")
        void eachType_HasUniqueDirectory() {
            // given
            FileDomainType[] types = FileDomainType.values();

            // when & then
            for (int i = 0; i < types.length; i++) {
                for (int j = i + 1; j < types.length; j++) {
                    assertThat(types[i].getDir()).isNotEqualTo(types[j].getDir());
                }
            }
        }

        @Test
        @DisplayName("모든 디렉토리 경로가 null이 아님")
        void allDirectories_AreNotNull() {
            // given
            FileDomainType[] types = FileDomainType.values();

            // when & then
            for (FileDomainType type : types) {
                assertThat(type.getDir()).isNotNull();
                assertThat(type.getDir()).isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("실제 사용 시나리오 테스트")
    class RealWorldScenarioTests {

        @Test
        @DisplayName("프로필 이미지 업로드 경로 생성")
        void profileImageUpload_Scenario() {
            // given
            FileDomainType type = FileDomainType.PROFILE;
            Long userId = 123L;

            // when
            String path = String.format("/files/%s/%d/image.jpg", type.getDir(), userId);

            // then
            assertThat(path).isEqualTo("/files/profile/123/image.jpg");
        }

        @Test
        @DisplayName("코스 썸네일 업로드 경로 생성")
        void courseThumbnailUpload_Scenario() {
            // given
            FileDomainType type = FileDomainType.COURSE_THUMBNAIL;
            Long courseId = 456L;

            // when
            String path = String.format("/files/%s/%d/thumb.jpg", type.getDir(), courseId);

            // then
            assertThat(path).isEqualTo("/files/course/thumbnail/456/thumb.jpg");
        }
    }
}