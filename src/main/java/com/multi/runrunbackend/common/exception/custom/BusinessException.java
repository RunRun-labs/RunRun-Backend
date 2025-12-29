package com.multi.runrunbackend.common.exception.custom;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;

/**
 * @author : BoKyung
 * @description : 사용자의 행위/요청 흐름이 도메인 규칙을 깨서 실패한 것
 * @filename : BusinessException
 * @since : 25. 12. 17. 수요일
 */
public class BusinessException extends CustomException {

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }
}
