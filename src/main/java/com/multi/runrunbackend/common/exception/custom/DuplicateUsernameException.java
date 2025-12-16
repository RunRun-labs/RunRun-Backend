package com.multi.runrunbackend.common.exception.custom;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;

public class DuplicateUsernameException extends CustomException {

    public DuplicateUsernameException(ErrorCode errorCode) {
        super(errorCode);
    }
}
