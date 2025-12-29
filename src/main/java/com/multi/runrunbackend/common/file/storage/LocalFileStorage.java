package com.multi.runrunbackend.common.file.storage;

import com.multi.runrunbackend.common.exception.custom.FileUploadException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.FileDomainType;
import com.multi.runrunbackend.common.file.util.FileNameGenerator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class LocalFileStorage implements FileStorage {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Override
    public String upload(MultipartFile file, FileDomainType domainType, Long refId) {
        try {
            String fileName = FileNameGenerator.generate(file.getOriginalFilename());

            Path dirPath = Paths.get(uploadPath, domainType.getDir(), String.valueOf(refId));
            Files.createDirectories(dirPath);

            Path savePath = dirPath.resolve(fileName);
            file.transferTo(savePath.toFile());

            return String.format("/files/%s/%d/%s", domainType.getDir(), refId, fileName);
        } catch (IOException e) {
            log.warn("파일 변경 감지 중 오류 발생, 새 파일 업로드로 대체: {}", e.getMessage());
            throw new FileUploadException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }


    @Override
    public String uploadIfChanged(
        MultipartFile file,
        FileDomainType domainType,
        Long refId,
        String existingUrl
    ) {
        if (file == null || file.isEmpty()) {
            return existingUrl;
        }

        if (existingUrl == null || existingUrl.isBlank()) {
            return upload(file, domainType, refId);
        }

        try {
            String existingFileName = extractFileName(existingUrl);
            if (existingFileName == null) {
                return upload(file, domainType, refId);
            }

            Path existingPath = Paths.get(uploadPath, domainType.getDir(), String.valueOf(refId))
                .resolve(existingFileName);

            if (!Files.exists(existingPath)) {
                return upload(file, domainType, refId);
            }

            String newHash = sha256Hex(file.getInputStream());
            String oldHash;
            try (InputStream is = Files.newInputStream(existingPath)) {
                oldHash = sha256Hex(is);
            }

            if (newHash.equals(oldHash)) {
                return existingUrl;
            }

            String newUrl = upload(file, domainType, refId);

            return newUrl;

        } catch (Exception e) {
            return upload(file, domainType, refId);
        }
    }

    @Override
    public String toHttpsUrl(String key) {
        return "";
    }

    private String extractFileName(String url) {
        String clean = url.split("\\?")[0];
        int idx = clean.lastIndexOf('/');
        if (idx < 0 || idx == clean.length() - 1) {
            return null;
        }
        return clean.substring(idx + 1);
    }

    private String sha256Hex(InputStream is) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[8192];
        int r;
        while ((r = is.read(buf)) != -1) {
            md.update(buf, 0, r);
        }
        return HexFormat.of().formatHex(md.digest());
    }
}
