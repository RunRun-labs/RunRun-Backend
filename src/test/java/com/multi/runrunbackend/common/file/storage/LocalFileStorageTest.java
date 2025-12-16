package com.multi.runrunbackend.common.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.multi.runrunbackend.common.exception.custom.FileUploadException;
import com.multi.runrunbackend.common.file.FileDomainType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("LocalFileStorage 단위 테스트")
class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileStorage localFileStorage;
    private String uploadPath;

    @BeforeEach
    void setUp() {
        localFileStorage = new LocalFileStorage();
        uploadPath = tempDir.toString();
        ReflectionTestUtils.setField(localFileStorage, "uploadPath", uploadPath);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up created files and directories
        Files.walk(tempDir)
            .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
            .forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            });
    }

    @Nested
    @DisplayName("upload 메서드 테스트")
    class UploadTests {

        @Test
        @DisplayName("파일 업로드 성공")
        void upload_ValidFile_ReturnsFileUrl() throws IOException {
            // given
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getOriginalFilename()).thenReturn("test-image.jpg");
            when(mockFile.isEmpty()).thenReturn(false);

            FileDomainType domainType = FileDomainType.PROFILE;
            Long refId = 123L;

            // when
            String fileUrl = localFileStorage.upload(mockFile, domainType, refId);

            // then
            assertThat(fileUrl).isNotNull();
            assertThat(fileUrl).startsWith("/files/");
            assertThat(fileUrl).contains("/profile/");
            assertThat(fileUrl).contains("/123/");
            assertThat(fileUrl).endsWith(".jpg");
        }

        @Test
        @DisplayName("다양한 도메인 타입으로 파일 업로드")
        void upload_DifferentDomainTypes_CreatesCorrectPaths() throws IOException {
            // given
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getOriginalFilename()).thenReturn("test.png");
            Long refId = 1L;

            FileDomainType[] domainTypes = {
                FileDomainType.PROFILE,
                FileDomainType.COURSE_THUMBNAIL,
                FileDomainType.COURSE_IMAGE,
                FileDomainType.AD_IMAGE,
                FileDomainType.CREW_IMAGE,
                FileDomainType.PRODUCT_IMAGE
            };

            // when & then
            for (FileDomainType domainType : domainTypes) {
                String fileUrl = localFileStorage.upload(mockFile, domainType, refId);
                assertThat(fileUrl).contains("/" + domainType.getDir() + "/");
            }
        }

        @Test
        @DisplayName("파일 업로드 시 디렉토리 자동 생성")
        void upload_NonExistentDirectory_CreatesDirectory() throws IOException {
            // given
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getOriginalFilename()).thenReturn("test.jpg");

            FileDomainType domainType = FileDomainType.PROFILE;
            Long refId = 999L;

            // when
            String fileUrl = localFileStorage.upload(mockFile, domainType, refId);

            // then
            Path expectedDir = Paths.get(uploadPath, domainType.getDir(), String.valueOf(refId));
            assertThat(Files.exists(expectedDir)).isTrue();
            assertThat(Files.isDirectory(expectedDir)).isTrue();
        }

        @Test
        @DisplayName("다양한 파일 확장자 처리")
        void upload_DifferentExtensions_PreservesExtensions() throws IOException {
            // given
            String[] filenames = {"image.jpg", "doc.pdf", "video.mp4", "data.json"};
            FileDomainType domainType = FileDomainType.PROFILE;
            Long refId = 1L;

            // when & then
            for (String filename : filenames) {
                MultipartFile mockFile = mock(MultipartFile.class);
                when(mockFile.getOriginalFilename()).thenReturn(filename);

                String fileUrl = localFileStorage.upload(mockFile, domainType, refId);

                String extension = filename.substring(filename.lastIndexOf('.'));
                assertThat(fileUrl).endsWith(extension);
            }
        }

        @Test
        @DisplayName("파일 업로드 실패 시 예외 발생")
        void upload_IOExceptionOccurs_ThrowsFileUploadException() throws IOException {
            // given
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getOriginalFilename()).thenReturn("test.jpg");
            when(mockFile.transferTo(any(File.class))).thenThrow(new IOException("Disk full"));

            FileDomainType domainType = FileDomainType.PROFILE;
            Long refId = 1L;

            // when & then
            assertThatThrownBy(() -> localFileStorage.upload(mockFile, domainType, refId))
                .isInstanceOf(FileUploadException.class);
        }

        @Test
        @DisplayName("동일한 도메인과 refId로 여러 파일 업로드")
        void upload_MultipleFilesToSameLocation_CreatesUniqueNames() throws IOException {
            // given
            FileDomainType domainType = FileDomainType.PROFILE;
            Long refId = 1L;

            // when
            String fileUrl1 = createAndUploadMockFile("test1.jpg", domainType, refId);
            String fileUrl2 = createAndUploadMockFile("test2.jpg", domainType, refId);
            String fileUrl3 = createAndUploadMockFile("test3.jpg", domainType, refId);

            // then
            assertThat(fileUrl1).isNotEqualTo(fileUrl2);
            assertThat(fileUrl2).isNotEqualTo(fileUrl3);
            assertThat(fileUrl1).isNotEqualTo(fileUrl3);
        }

        @Test
        @DisplayName("특수문자가 포함된 원본 파일명 처리")
        void upload_SpecialCharactersInFilename_GeneratesValidUrl() throws IOException {
            // given
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getOriginalFilename()).thenReturn("파일명-테스트@#$.jpg");

            FileDomainType domainType = FileDomainType.PROFILE;
            Long refId = 1L;

            // when
            String fileUrl = localFileStorage.upload(mockFile, domainType, refId);

            // then
            assertThat(fileUrl).isNotNull();
            assertThat(fileUrl).endsWith(".jpg");
            // Generated filename should not contain special characters from original
            String filename = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
            assertThat(filename).doesNotContain("@#$");
        }

        @Test
        @DisplayName("refId가 0인 경우 처리")
        void upload_RefIdZero_WorksCorrectly() throws IOException {
            // given
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getOriginalFilename()).thenReturn("test.jpg");

            FileDomainType domainType = FileDomainType.PROFILE;
            Long refId = 0L;

            // when
            String fileUrl = localFileStorage.upload(mockFile, domainType, refId);

            // then
            assertThat(fileUrl).contains("/0/");
        }

        @Test
        @DisplayName("매우 큰 refId 처리")
        void upload_LargeRefId_WorksCorrectly() throws IOException {
            // given
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getOriginalFilename()).thenReturn("test.jpg");

            FileDomainType domainType = FileDomainType.PROFILE;
            Long refId = Long.MAX_VALUE;

            // when
            String fileUrl = localFileStorage.upload(mockFile, domainType, refId);

            // then
            assertThat(fileUrl).contains("/" + Long.MAX_VALUE + "/");
        }

        private String createAndUploadMockFile(String filename, FileDomainType domainType,
            Long refId) throws IOException {
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getOriginalFilename()).thenReturn(filename);
            return localFileStorage.upload(mockFile, domainType, refId);
        }
    }

    @Nested
    @DisplayName("파일 URL 형식 테스트")
    class FileUrlFormatTests {

        @Test
        @DisplayName("생성된 URL이 올바른 형식인지 검증")
        void upload_GeneratedUrl_HasCorrectFormat() throws IOException {
            // given
            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.getOriginalFilename()).thenReturn("test.jpg");

            FileDomainType domainType = FileDomainType.PROFILE;
            Long refId = 123L;

            // when
            String fileUrl = localFileStorage.upload(mockFile, domainType, refId);

            // then
            // Format: /files/{domainType}/{refId}/{uuid}.{extension}
            String[] parts = fileUrl.split("/");
            assertThat(parts).hasSizeGreaterThanOrEqualTo(5);
            assertThat(parts[1]).isEqualTo("files");
            assertThat(parts[2]).isEqualTo(domainType.getDir().split("/")[0]);
            assertThat(fileUrl).matches(".*/files/.*/\\d+/[0-9a-f-]+\\.jpg");
        }
    }
}