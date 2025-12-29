package com.multi.runrunbackend.common.file.storage;

import com.multi.runrunbackend.common.file.FileDomainType;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {


    String upload(
        MultipartFile file,
        FileDomainType domainType,
        Long refId
    );

    default String uploadIfChanged(
        MultipartFile file,
        FileDomainType domainType,
        Long refId,
        String existingUrl
    ) {
        if (file == null || file.isEmpty()) {
            return existingUrl;
        }
        return upload(file, domainType, refId);
    }

    String toHttpsUrl(String key);


    default void delete(String fileUrl) {
    }
}