package com.multi.runrunbackend.common.exception.custom;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;

public class DuplicateException extends CustomException {

    public DuplicateException(ErrorCode errorCode) {
        super(errorCode);
    }
}
