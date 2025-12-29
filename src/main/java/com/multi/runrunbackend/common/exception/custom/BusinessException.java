package com.multi.runrunbackend.common.exception.custom;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;

/**
 * @author : BoKyung
 * @description : Please explain the class!!!
 * @filename : BusinessException
 * @since : 25. 12. 17. 수요일
 */
public class BusinessException extends CustomException {

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }
}
