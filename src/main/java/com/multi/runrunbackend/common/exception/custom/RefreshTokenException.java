package com.multi.runrunbackend.common.exception.custom;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;

public class RefreshTokenException extends CustomException {

    public RefreshTokenException(ErrorCode errorCode) {
        super(errorCode); // 401
    }
}


