package com.multi.runrunbackend.common.file.storage;

import com.multi.runrunbackend.common.file.FileDomainType;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {


    String upload(
        MultipartFile file,
        FileDomainType domainType,
        Long refId
    );


    default void delete(String fileUrl) {
    }
}