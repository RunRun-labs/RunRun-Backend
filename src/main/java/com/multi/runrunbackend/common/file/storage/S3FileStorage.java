package com.multi.runrunbackend.common.file.storage;

import com.multi.runrunbackend.common.exception.custom.FileUploadException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.FileDomainType;
import com.multi.runrunbackend.common.file.util.FileNameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : S3FileStorage
 * @since : 2025. 12. 24. Wednesday
 */

@Component
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
            String fileName = FileNameGenerator.generate(file.getOriginalFilename());
            String key = buildKey(domainType, refId, fileName);

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
            // 기존 URL에서 key 뽑기
            String existingKey = extractKey(existingUrl);
            if (existingKey == null || existingKey.isBlank()) {
                return upload(file, domainType, refId);
            }

            // 새 파일 해시(작을 때만)
            if (file.getSize() > hashMaxBytes) {
                // 큰 파일은 비교 생략하고 그냥 업로드(실무에서도 많이 이렇게 타협함)
                String newUrl = upload(file, domainType, refId);
                // 필요하면 기존 삭제(정책 선택)
                safeDelete(existingKey);
                return newUrl;
            }

            String newSha = sha256Hex(file.getBytes());

            // 기존 객체 메타데이터 sha256 조회
            String oldSha = null;
            try {
                var head = s3.headObject(HeadObjectRequest.builder()
                        .bucket(bucket)
                        .key(existingKey)
                        .build());
                oldSha = head.metadata() != null ? head.metadata().get("sha256") : null;
            } catch (NoSuchKeyException e) {
                return upload(file, domainType, refId);
            } catch (S3Exception e) {
                // head 실패하면 안전하게 새로 업로드로 폴백
                return upload(file, domainType, refId);
            }

            if (oldSha != null && oldSha.equals(newSha)) {
                return existingUrl; // 변경 없음
            }

            // 변경됨 → 업로드 후 기존 삭제(정책)
            String newUrl = upload(file, domainType, refId);
            safeDelete(existingKey);
            return newUrl;

        } catch (Exception e) {
            // 비교 중 오류면 그냥 업로드로 폴백
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

    private String buildKey(FileDomainType domainType, Long refId, String fileName) {
        // uploads/{domainDir}/{refId}/{yyyyMMdd}/{fileName}
        String date = DateTimeFormatter.ofPattern("yyyyMMdd")
                .withZone(ZoneId.of("Asia/Seoul"))
                .format(Instant.now());

        return "uploads/" + domainType.getDir() + "/" + refId + "/" + date + "/" + fileName;
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
                // 다른 버킷이면 정책에 따라 처리 (여긴 그냥 null)
                return null;
            }
            return trimLeadingSlash(k);
        }

        // https URL
        try {
            URI uri = URI.create(clean);
            String host = uri.getHost();          // runrun-uploads-bucket.s3.mx-central-1.amazonaws.com
            String path = uri.getPath();          // /uploads/...

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
