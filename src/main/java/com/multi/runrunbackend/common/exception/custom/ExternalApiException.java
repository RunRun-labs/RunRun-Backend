package com.multi.runrunbackend.common.exception.custom;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : ExternalApiException
 * @since : 2025. 12. 21. Sunday
 */
public class ExternalApiException extends CustomException {

    public ExternalApiException(ErrorCode errorCode) {
        super(errorCode);
    }
}
