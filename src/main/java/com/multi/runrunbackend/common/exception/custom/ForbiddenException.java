package com.multi.runrunbackend.common.exception.custom;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;

/**
 * @author : KIMGWANGHO
 * @description : 권한이 없는 요청(수정/삭제 등)을 했을 때 발생하는 공통 예외
 * @filename : ForbiddenException
 * @since : 2025-12-19 금요일
 */
public class ForbiddenException extends CustomException {

  public ForbiddenException(ErrorCode errorCode) {
    super(errorCode);
  }
}