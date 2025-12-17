package com.multi.runrunbackend.common.exception.dto;


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    /* ===== 공통 ===== */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "C001", "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 오류가 발생했습니다."),

    /* ===== 인증 / 회원 ===== */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U003", "비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "U004", "인증이 필요합니다."),
    DUPLICATE_USER(HttpStatus.CONFLICT, "U005", "이미 사용 중인 아이디 입니다"),

    /* ==== 토큰 ====*/
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "만료된 토큰입니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A003", "토큰이 존재하지 않습니다."),
    MISSING_AUTHORIZATION_HEADER(HttpStatus.UNAUTHORIZED, "A004", "Authorization 헤더가 존재하지 않습니다"),
    TOKEN_BAD_REQUEST(HttpStatus.BAD_REQUEST, "A005", "토큰 값이 올바르지 않습니다"),

    /*==== 리프레시 토큰 ====*/
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_101", "리프레시 토큰이 만료되었습니다. 다시 로그인하세요."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_102", "유효하지 않은 리프레시 토큰입니다."),
    FILE_UPLOAD_FAILED(

            HttpStatus.INTERNAL_SERVER_ERROR,
            "F001",
            "파일 업로드에 실패했습니다."
    ),

    /* ===== 크루 관련 ===== */
    CREW_NOT_FOUND(HttpStatus.NOT_FOUND, "CR001", "크루를 찾을 수 없습니다."),
    CREW_ALREADY_EXISTS(HttpStatus.CONFLICT, "CR002", "이미 존재하는 크루명입니다."),
    CREW_ALREADY_DISBANDED(HttpStatus.BAD_REQUEST, "CR003", "이미 해체된 크루입니다."),
    NOT_CREW_LEADER(HttpStatus.FORBIDDEN, "CR004", "크루장만 수정/삭제할 수 있습니다."),
    NOT_CREW_LEADER_OR_SUB_LEADER(HttpStatus.FORBIDDEN, "CR005", "크루장 또는 부크루장만 처리할 수 있습니다."),
    ALREADY_CREW_LEADER(HttpStatus.CONFLICT, "CR006", "이미 크루를 생성하셨습니다. 하나의 크루만 생성 가능합니다."),
    NOT_PREMIUM_MEMBER(HttpStatus.FORBIDDEN, "CR007", "프리미엄 멤버십 회원만 크루를 생성할 수 있습니다."),
    INVALID_CREW_STATUS(HttpStatus.BAD_REQUEST, "CR008", "유효하지 않은 크루 상태입니다."),
    CREW_NOT_RECRUITING(HttpStatus.BAD_REQUEST, "CR009", "모집중인 크루가 아닙니다."),
    ALREADY_CREW_MEMBER(HttpStatus.CONFLICT, "CR010", "이미 가입된 크루입니다."),
    JOIN_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "CR011", "가입 신청을 찾을 수 없습니다."),
    JOIN_REQUEST_NOT_PENDING(HttpStatus.BAD_REQUEST, "CR012", "대기 상태의 요청만 처리할 수 있습니다."),
    ALREADY_REQUESTED(HttpStatus.CONFLICT, "CR013", "이미 가입 신청한 크루입니다."),
    CANNOT_LEAVE_AS_LEADER(HttpStatus.BAD_REQUEST, "CR014", "크루장은 탈퇴할 수 없습니다. 부크루장 또는 운영진에게 크루장을 위임하거나 크루를 해체해주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}