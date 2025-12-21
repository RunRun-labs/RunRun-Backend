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

    /*==== 코스 ====*/
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "CRS_001", "코스를 찾을 수 없습니다"),

    COURSE_FORBIDDEN(
        HttpStatus.FORBIDDEN,
        "CRS_002",
        "해당 코스에 대한 권한이 없습니다."
    ),

    COURSE_IMAGE_TOO_LARGE(
        HttpStatus.BAD_REQUEST,
        "CRS_003",
        "이미지 파일 용량이 너무 큽니다."
    ),
    COURSE_NOT_ACTIVE(HttpStatus.FORBIDDEN, "CRS_004", "코스가 ACTIVE 상태가 아닙니다"),

    /*==== TMAP====*/

    ROUTE_DISTANCE_EXCEEDED(
        HttpStatus.BAD_REQUEST,
        "ROUTE_001",
        "요청한 경로 거리가 허용 범위를 초과했습니다."
    ),

    ROUTE_END_POINT_REQUIRED(
        HttpStatus.BAD_REQUEST,
        "ROUTE_002",
        "도착 좌표(endLat, endLng)는 필수입니다."
    ),

    ROUTE_DISTANCE_INVALID(
        HttpStatus.BAD_REQUEST,
        "ROUTE_003",
        "요청 거리 값이 올바르지 않습니다."
    ),

    ROUTE_INVALID_POINTS(
        HttpStatus.BAD_REQUEST,
        "ROUTE_004",
        "경로 포인트는 최소 2개 이상 필요합니다."
    ),

    ROUTE_NO_VALID_SEGMENT(
        HttpStatus.BAD_REQUEST,
        "ROUTE_005",
        "유효한 경로 구간을 생성할 수 없습니다."
    ),


    /*==== 경로 ====*/
    TMAP_API_FAILED(
        HttpStatus.BAD_GATEWAY,
        "EXT_001",
        "TMAP 경로 API 호출에 실패했습니다."
    ),

    TMAP_EMPTY_RESPONSE(
        HttpStatus.BAD_GATEWAY,
        "EXT_002",
        "TMAP API 응답이 비어있습니다."
    ),

    TMAP_NO_ROUTE(
        HttpStatus.BAD_GATEWAY,
        "EXT_003",
        "TMAP에서 유효한 경로를 반환하지 않았습니다."
    ),
    /*=====코스 서치 ====*/
    COURSE_SEARCH_LAT_LNG_REQUIRED(
        HttpStatus.BAD_REQUEST,
        "CRS_S_001",
        "nearby 검색 또는 거리순 정렬에는 위도(lat)와 경도(lng)가 필수입니다."
    ),

    COURSE_SEARCH_INVALID_DISTANCE_BUCKET(
        HttpStatus.BAD_REQUEST,
        "CRS_S_002",
        "거리 필터 값이 올바르지 않습니다."
    ),

    COURSE_SEARCH_INVALID_ENUM(
        HttpStatus.BAD_REQUEST,
        "CRS_S_003",
        "검색 조건 값이 올바르지 않습니다."
    ),

    COURSE_SEARCH_MAPPING_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "CRS_S_004",
        "코스 목록 조회 중 데이터 처리 오류가 발생했습니다."
    );


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}