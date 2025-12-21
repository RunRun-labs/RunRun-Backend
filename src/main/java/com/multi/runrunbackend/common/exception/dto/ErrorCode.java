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

  /* ==== 모집글 ====*/
  RECRUIT_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "모집글을 찾을 수 없습니다."),
  INVALID_RECRUIT(HttpStatus.BAD_REQUEST, "R002", "삭제된 모집글입니다"),
  RECRUIT_UPDATE_DENIED(HttpStatus.FORBIDDEN, "R003", "모집글 수정 권한이 없습니다."),
  RECRUIT_DELETE_DENIED(HttpStatus.FORBIDDEN, "R004", "모집글 삭제 권한이 없습니다."),
  RECRUIT_HAS_PARTICIPANTS(HttpStatus.BAD_REQUEST, "R005", "참여자가 있어 모집글을 수정할 수 없습니다."),
  ALREADY_PARTICIPATED(HttpStatus.BAD_REQUEST, "R006", "이미 참여 중입니다."),
  NOT_PARTICIPATED(HttpStatus.BAD_REQUEST, "R007", "참여 상태가 아닙니다."),
  INVALID_AGE_RANGE(HttpStatus.BAD_REQUEST, "R008", "최소 나이는 최대 나이보다 클 수 없습니다."),
  INVALID_MEETING_TIME(HttpStatus.BAD_REQUEST, "R009", "모임 날짜는 오늘부터 2주일 이내로만 설정 가능합니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  ErrorCode(HttpStatus httpStatus, String code, String message) {
    this.httpStatus = httpStatus;
    this.code = code;
    this.message = message;
  }
}