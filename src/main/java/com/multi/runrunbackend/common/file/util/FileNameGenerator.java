package com.multi.runrunbackend.common.file.util;

import java.util.UUID;
import org.apache.commons.io.FilenameUtils;

public final class FileNameGenerator {

    private FileNameGenerator() {
    }

    public static String generate(String originalFilename) {
        String ext = FilenameUtils.getExtension(originalFilename);
        return UUID.randomUUID() + "." + ext;
    }
}
