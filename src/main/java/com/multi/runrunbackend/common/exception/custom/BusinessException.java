package com.multi.runrunbackend.common.exception.custom;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;

/**
 * @description : Please explain the class!!!
 * @filename : BusinessException
 */
public class BusinessException extends CustomException {
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }
}
