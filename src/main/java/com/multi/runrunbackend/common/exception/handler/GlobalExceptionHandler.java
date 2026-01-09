package com.multi.runrunbackend.common.exception.handler;

import com.multi.runrunbackend.common.exception.custom.CustomException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(
      CustomException e
  ) {
    ErrorCode errorCode = e.getErrorCode();

    return ResponseEntity
        .status(errorCode.getHttpStatus())
        .body(ApiResponse.error(errorCode));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(HttpServletRequest req, Exception e) {
    e.printStackTrace();
    String accept = req.getHeader("Accept");
    if (accept != null && accept.contains("text/event-stream")) {
      log.debug("[SSE] ignore exception: {}", e.toString());
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity
        .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
        .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
  }

  @ExceptionHandler(AsyncRequestNotUsableException.class)
  public ResponseEntity<Void> handleAsyncDisconnect(Exception e) {
    e.printStackTrace();
    return ResponseEntity.noContent().build();
  }

}