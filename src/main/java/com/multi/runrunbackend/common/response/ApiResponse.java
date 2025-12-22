package com.multi.runrunbackend.common.response;


import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;


    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .code("SUCCESS")
            .message("요청이 성공적으로 처리되었습니다.")
            .data(data)
            .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .code("SUCCESS")
            .message(message)
            .data(data)
            .build();
    }

    public static <T> ApiResponse<T> successNoData(String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .code("SUCCESS")
            .message(message)
            .build();
    }


    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
            .success(false)
            .code(errorCode.getCode())
            .message(errorCode.getMessage())
            .data(null)
            .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String customMessage) {
        return ApiResponse.<T>builder()
            .success(false)
            .code(errorCode.getCode())
            .message(customMessage)
            .data(null)
            .build();
    }
}