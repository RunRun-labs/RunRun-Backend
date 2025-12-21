package com.multi.runrunbackend.common.file.storage;

import com.multi.runrunbackend.common.exception.custom.FileUploadException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.file.FileDomainType;
import com.multi.runrunbackend.common.file.util.FileNameGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalFileStorage implements FileStorage {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Override
    public String upload(
            MultipartFile file,
            FileDomainType domainType,
            Long refId
    ) {
        try {
            String fileName = FileNameGenerator.generate(file.getOriginalFilename());

            Path dirPath = Paths.get(
                    uploadPath,
                    domainType.getDir(),
                    String.valueOf(refId)
            );

            Files.createDirectories(dirPath);

            Path savePath = dirPath.resolve(fileName);
            file.transferTo(savePath.toFile());

            return String.format(
                    "/files/%s/%d/%s",
                    domainType.getDir(),
                    refId,
                    fileName
            );

        } catch (IOException e) {
            throw new FileUploadException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }
}