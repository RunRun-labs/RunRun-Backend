package com.multi.runrunbackend.common.exception.custom;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : BusinessException
 * @since : 2025. 12. 22. Monday
 */
public class BusinessException extends CustomException {

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }
}
