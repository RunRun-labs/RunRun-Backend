package com.multi.runrunbackend.common.exception.custom;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : InvalidRequestException
 * @since : 2025. 12. 20. Saturday
 */
public class InvalidRequestException extends CustomException {

    public InvalidRequestException(ErrorCode errorCode) {
        super(errorCode);
    }
}
