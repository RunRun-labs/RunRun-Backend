package com.multi.runrunbackend.common.exception.custom;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;

/**
 * @author : KIMGWANGHO
 * @description : 요청 데이터 및 비즈니스 로직 검증 실패 시 사용하는 전용 예외 클래스
 * @filename : ValidationException
 * @since : 2025-12-21 일요일
 */
public class ValidationException extends CustomException {

  public ValidationException(ErrorCode errorCode) {
    super(errorCode);
  }
}
