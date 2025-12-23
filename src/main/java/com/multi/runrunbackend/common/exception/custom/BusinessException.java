package com.multi.runrunbackend.common.exception.custom;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;

/**
 *
 * @author : kimyongwon
 * @description : Please explain the class!!!
 * @filename : BusinessException
 * @since : 25. 12. 23. 오후 12:58 화요일
 */
public class BusinessException extends CustomException {
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }
}
