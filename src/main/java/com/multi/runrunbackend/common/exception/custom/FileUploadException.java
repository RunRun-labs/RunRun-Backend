package com.multi.runrunbackend.common.exception.custom;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;

public class FileUploadException extends CustomException {

    public FileUploadException(ErrorCode errorCode) {
        super(errorCode);
    }
}