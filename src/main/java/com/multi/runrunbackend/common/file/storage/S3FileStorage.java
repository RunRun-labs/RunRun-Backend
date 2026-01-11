package com.multi.runrunbackend.common.file.storage;

import com.multi.runrunbackend.common.exception.custom.FileUploadException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.FileDomainType;
import com.multi.runrunbackend.common.file.util.FileNameGenerator;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @author : kyungsoo
 * @description : S3 기반 파일 업로드/삭제 구현체. - 저장 경로: uploads/{domainDir}/{refKey}/{yyyyMMdd}/{fileName} -
 * refKey: refId(Long) 또는 refId가 없으면 UUID - uploadIfChanged: existingUrl이 있으면 existingUrl의 refKey를
 * 우선 재사용(폴더 섞임 방지)
 * @filename : S3FileStorage
 * @since : 2025. 12. 24. Wednesday
 */
@Component
@Profile("s3")
@RequiredArgsConstructor
@Slf4j
public class S3FileStorage implements FileStorage {

    @Value("${file.hash-max-bytes}")
    private long hashMaxBytes;

    private final S3Client s3;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.region}")
    private String region;

    @Override
    public String upload(MultipartFile file, FileDomainType domainType, Long refId) {
        try {
            String refKey = (refId != null) ? String.valueOf(refId) : UUID.randomUUID().toString();
            return uploadInternal(file, domainType, refKey);
        } catch (FileUploadException e) {
            throw e;
        } catch (Exception e) {
            throw new FileUploadException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }


    @Override
    public String uploadIfChanged(MultipartFile file, FileDomainType domainType, Long refId,
        String existingUrl) {
        if (file == null || file.isEmpty()) {
            return existingUrl;
        }

        if (existingUrl == null || existingUrl.isBlank()) {
            return upload(file, domainType, refId);
        }

        try {
            String existingKey = extractKey(existingUrl);
            if (existingKey == null || existingKey.isBlank()) {
                return upload(file, domainType, refId);
            }

            // ✅ 기존 key에서 refKey(UUID/adId)를 우선 추출해서 "그 폴더"에 업로드
            String existingRefKey = extractRefKeyFromKey(existingKey, domainType);
            if (existingRefKey == null || existingRefKey.isBlank()) {
                // 파싱 실패 시 폴백
                return upload(file, domainType, refId);
            }

            // 큰 파일은 비교 생략하고 업로드(다만 폴더는 기존 refKey 유지)
            if (file.getSize() > hashMaxBytes) {
                String newKey = uploadInternal(file, domainType, existingRefKey);
                safeDelete(existingKey);
                return newKey;
            }

            String newSha = sha256Hex(file.getBytes());

            // 기존 객체 메타데이터 sha256 조회
            String oldSha;
            try {
                var head = s3.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(existingKey)
                    .build());
                oldSha = head.metadata() != null ? head.metadata().get("sha256") : null;
            } catch (NoSuchKeyException e) {
                return uploadInternal(file, domainType, existingRefKey);
            } catch (S3Exception e) {
                return uploadInternal(file, domainType, existingRefKey);
            }

            if (oldSha != null && oldSha.equals(newSha)) {
                return existingUrl; // 변경 없음
            }

            // 변경됨 → 업로드 후 기존 삭제
            String newKey = uploadInternal(file, domainType, existingRefKey);
            safeDelete(existingKey);
            return newKey;

        } catch (Exception e) {
            // 비교 중 오류면 안전하게 폴백
            return upload(file, domainType, refId);
        }
    }

    @Override
    public void delete(String fileUrl) {
        String key = extractKey(fileUrl);
        if (key == null || key.isBlank()) {
            return;
        }
        safeDelete(key);
    }

    private void safeDelete(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
        } catch (S3Exception e) {
            throw new FileUploadException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    private String uploadInternal(MultipartFile file, FileDomainType domainType, String refKey) {
        try {
            String fileName = FileNameGenerator.generate(file.getOriginalFilename());
            String key = buildKey(domainType, refKey, fileName);

            // 작은 파일은 sha256 메타데이터까지 넣어두면 uploadIfChanged에 유리
            if (file.getSize() <= hashMaxBytes) {
                byte[] bytes = file.getBytes();
                String sha256 = sha256Hex(bytes);

                PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(safeContentType(file))
                    .metadata(Map.of("sha256", sha256))
                    .build();

                s3.putObject(req, RequestBody.fromBytes(bytes));
            } else {
                // 큰 파일은 스트리밍 업로드(해시 비교 생략)
                PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(safeContentType(file))
                    .build();

                try (InputStream in = file.getInputStream()) {
                    s3.putObject(req, RequestBody.fromInputStream(in, file.getSize()));
                }
            }

            // 기존 코드와 호환 위해 key 반환 (URL 저장 원하면 toHttpsUrl(key)로 바꿔도 됨)
            return key;

        } catch (IOException e) {
            throw new FileUploadException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (S3Exception e) {
            log.warn("S3 업로드 실패: {}",
                e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage());
            throw new FileUploadException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (Exception e) {
            throw new FileUploadException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private String buildKey(FileDomainType domainType, String refKey, String fileName) {

        String date = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneId.of("Asia/Seoul"))
            .format(Instant.now());

        return "uploads/" + domainType.getDir() + "/" + refKey + "/" + date + "/" + fileName;
    }

    /**
     * ✅ existingKey (uploads/{domainDir}/{refKey}/...) 에서 refKey만 추출
     */
    private String extractRefKeyFromKey(String key, FileDomainType domainType) {
        // expected: uploads/{domainDir}/{refKey}/yyyyMMdd/filename
        if (key == null) {
            return null;
        }

        String prefix = "uploads/" + domainType.getDir() + "/";
        if (!key.startsWith(prefix)) {
            return null;
        }

        String rest = key.substring(prefix.length()); // {refKey}/yyyyMMdd/filename
        int slash = rest.indexOf('/');
        if (slash < 0) {
            return null;
        }
        return rest.substring(0, slash);
    }

    private String safeContentType(MultipartFile file) {
        String ct = file.getContentType();
        return (ct == null || ct.isBlank()) ? "application/octet-stream" : ct;
    }

    @Override
    public String toHttpsUrl(String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    /**
     * existingUrl이 아래 중 무엇이든 key만 뽑아줌 - key 자체: uploads/... -
     * https://{bucket}.s3.{region}.amazonaws.com/uploads/... - s3://{bucket}/uploads/...
     */
    private String extractKey(String urlOrKey) {
        if (urlOrKey == null) {
            return null;
        }

        String clean = urlOrKey.split("\\?")[0];

        // key 자체
        if (!clean.startsWith("http") && !clean.startsWith("s3://")) {
            return trimLeadingSlash(clean);
        }

        // s3://bucket/key
        if (clean.startsWith("s3://")) {
            String noScheme = clean.substring("s3://".length());
            int firstSlash = noScheme.indexOf('/');
            if (firstSlash < 0) {
                return null;
            }
            String b = noScheme.substring(0, firstSlash);
            String k = noScheme.substring(firstSlash + 1);
            if (!bucket.equals(b)) {
                return null;
            }
            return trimLeadingSlash(k);
        }

        // https URL
        try {
            URI uri = URI.create(clean);
            String host = uri.getHost();
            String path = uri.getPath();

            if (host == null || path == null) {
                return null;
            }

            // 가상 호스트 스타일 체크
            if (!host.startsWith(bucket + ".")) {
                return null;
            }

            return trimLeadingSlash(path);
        } catch (Exception e) {
            return null;
        }
    }

    private String trimLeadingSlash(String s) {
        if (s == null) {
            return null;
        }
        while (s.startsWith("/")) {
            s = s.substring(1);
        }
        return s;
    }

    private String sha256Hex(byte[] bytes) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(bytes);
        return HexFormat.of().formatHex(md.digest());
    }
}