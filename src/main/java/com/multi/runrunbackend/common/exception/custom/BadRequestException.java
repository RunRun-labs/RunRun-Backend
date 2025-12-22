package com.multi.runrunbackend.common.exception.custom;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : BadRequestException
 * @since : 2025. 12. 21. Sunday
 */
public class BadRequestException extends CustomException {

    public BadRequestException(ErrorCode errorCode) {
        super(errorCode);
    }
}
