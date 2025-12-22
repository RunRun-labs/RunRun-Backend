package com.multi.runrunbackend.common.exception.custom;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : ForbiddenException
 * @since : 2025. 12. 20. Saturday
 */
public class ForbiddenException extends CustomException {

    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
