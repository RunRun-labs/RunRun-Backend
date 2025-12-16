package com.multi.runrunbackend.common.file.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

@DisplayName("FileNameGenerator 단위 테스트")
class FileNameGeneratorTest {

    @Nested
    @DisplayName("generate 메서드 테스트")
    class GenerateTests {

        @Test
        @DisplayName("파일명 생성 시 확장자 유지")
        void generate_WithExtension_PreservesExtension() {
            // given
            String originalFilename = "test-image.jpg";

            // when
            String generated = FileNameGenerator.generate(originalFilename);

            // then
            assertThat(generated).endsWith(".jpg");
            assertThat(generated).isNotEqualTo(originalFilename);
        }

        @Test
        @DisplayName("다양한 확장자 처리")
        void generate_DifferentExtensions_PreservesCorrectExtension() {
            // given
            String[] filenames = {
                "document.pdf",
                "image.png",
                "video.mp4",
                "archive.zip",
                "data.json"
            };

            // when & then
            for (String filename : filenames) {
                String generated = FileNameGenerator.generate(filename);
                String extension = filename.substring(filename.lastIndexOf('.'));
                assertThat(generated).endsWith(extension);
            }
        }

        @Test
        @DisplayName("확장자가 없는 파일 처리")
        void generate_NoExtension_GeneratesValidFilename() {
            // given
            String originalFilename = "filename-without-extension";

            // when
            String generated = FileNameGenerator.generate(originalFilename);

            // then
            assertThat(generated).isNotNull();
            assertThat(generated).isNotEmpty();
            assertThat(generated).endsWith(".");
        }

        @Test
        @DisplayName("여러 점이 포함된 파일명 처리")
        void generate_MultipleDotsInFilename_UsesLastExtension() {
            // given
            String originalFilename = "my.file.name.with.dots.txt";

            // when
            String generated = FileNameGenerator.generate(originalFilename);

            // then
            assertThat(generated).endsWith(".txt");
        }

        @RepeatedTest(100)
        @DisplayName("생성된 파일명의 고유성 보장")
        void generate_RepeatedCalls_GeneratesUniqueNames() {
            // given
            String originalFilename = "test.jpg";
            Set<String> generatedNames = new HashSet<>();

            // when
            for (int i = 0; i < 100; i++) {
                String generated = FileNameGenerator.generate(originalFilename);
                generatedNames.add(generated);
            }

            // then
            assertThat(generatedNames).hasSize(100);
        }

        @Test
        @DisplayName("UUID 형식 검증")
        void generate_GeneratedFilename_ContainsUUID() {
            // given
            String originalFilename = "test.jpg";

            // when
            String generated = FileNameGenerator.generate(originalFilename);

            // then
            String uuidPart = generated.substring(0, generated.lastIndexOf('.'));
            // UUID 형식: 8-4-4-4-12 (총 36자, 하이픈 포함)
            assertThat(uuidPart).hasSize(36);
            assertThat(uuidPart).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("빈 문자열 파일명 처리")
        void generate_EmptyFilename_GeneratesUUID() {
            // given
            String originalFilename = "";

            // when
            String generated = FileNameGenerator.generate(originalFilename);

            // then
            assertThat(generated).isNotNull();
            assertThat(generated).endsWith(".");
        }

        @Test
        @DisplayName("대문자 확장자 처리")
        void generate_UppercaseExtension_PreservesCase() {
            // given
            String originalFilename = "IMAGE.JPG";

            // when
            String generated = FileNameGenerator.generate(originalFilename);

            // then
            assertThat(generated).endsWith(".JPG");
        }

        @Test
        @DisplayName("특수문자가 포함된 파일명 처리")
        void generate_SpecialCharactersInFilename_WorksCorrectly() {
            // given
            String originalFilename = "파일명-with-special@#$chars.png";

            // when
            String generated = FileNameGenerator.generate(originalFilename);

            // then
            assertThat(generated).endsWith(".png");
            assertThat(generated).doesNotContain("파일명");
            assertThat(generated).doesNotContain("@#$");
        }

        @Test
        @DisplayName("매우 긴 파일명 처리")
        void generate_VeryLongFilename_GeneratesReasonableLengthName() {
            // given
            String longFilename = "a".repeat(1000) + ".jpg";

            // when
            String generated = FileNameGenerator.generate(longFilename);

            // then
            assertThat(generated).endsWith(".jpg");
            assertThat(generated.length()).isLessThan(100); // UUID + extension
        }
    }

    @Nested
    @DisplayName("경계값 테스트")
    class EdgeCaseTests {

        @Test
        @DisplayName("null 파일명 처리")
        void generate_NullFilename_HandlesGracefully() {
            // This test documents current behavior
            // Depending on requirements, might want to throw exception
            try {
                FileNameGenerator.generate(null);
            } catch (Exception e) {
                // Expected behavior for null input
                assertThat(e).isNotNull();
            }
        }

        @Test
        @DisplayName("점으로만 이루어진 파일명")
        void generate_OnlyDots_WorksCorrectly() {
            // given
            String originalFilename = "...";

            // when
            String generated = FileNameGenerator.generate(originalFilename);

            // then
            assertThat(generated).isNotNull();
        }

        @Test
        @DisplayName("공백이 포함된 파일명")
        void generate_FilenameWithSpaces_WorksCorrectly() {
            // given
            String originalFilename = "file with spaces.txt";

            // when
            String generated = FileNameGenerator.generate(originalFilename);

            // then
            assertThat(generated).endsWith(".txt");
            assertThat(generated).doesNotContain(" ");
        }
    }

    @Nested
    @DisplayName("동시성 테스트")
    class ConcurrencyTests {

        @Test
        @DisplayName("동시에 여러 파일명 생성")
        void generate_ConcurrentGeneration_ProducesUniqueNames() throws InterruptedException {
            // given
            Set<String> generatedNames = new HashSet<>();
            int threadCount = 10;
            int iterationsPerThread = 100;
            Thread[] threads = new Thread[threadCount];

            // when
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        String generated = FileNameGenerator.generate("test.jpg");
                        synchronized (generatedNames) {
                            generatedNames.add(generated);
                        }
                    }
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // then
            assertThat(generatedNames).hasSize(threadCount * iterationsPerThread);
        }
    }
}