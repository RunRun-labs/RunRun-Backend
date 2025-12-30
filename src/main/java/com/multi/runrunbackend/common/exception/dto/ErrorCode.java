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
    /*  ===== 사용자 탈퇴 ===== */
    USER_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "U006", "이미 탈퇴한 사용자입니다."),
    /* ==== 토큰 ====*/
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "만료된 토큰입니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A003", "토큰이 존재하지 않습니다."),
    MISSING_AUTHORIZATION_HEADER(HttpStatus.UNAUTHORIZED, "A004", "Authorization 헤더가 존재하지 않습니다"),
    TOKEN_BAD_REQUEST(HttpStatus.BAD_REQUEST, "A005", "토큰 값이 올바르지 않습니다"),

    /*==== 리프레시 토큰 ====*/
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_101", "리프레시 토큰이 만료되었습니다. 다시 로그인하세요."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_102", "유효하지 않은 리프레시 토큰입니다."),
    /*===== 사용자 차단 =====*/
    SELF_BLOCK_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST,
            "BLOCK_001",
            "자기 자신을 차단할 수 없습니다."
    ),
    USER_BLOCKED(
            HttpStatus.FORBIDDEN,
            "BLOCK_002",
            "차단한 사용자 입니다."
    ),
    BLOCKED_BY_USER(
            HttpStatus.FORBIDDEN,
            "BLOCK_003",
            "차단당한 사용자 입니다."
    ),
    ALREADY_BLOCKED(
            HttpStatus.CONFLICT,
            "BLOCK_004",
            "이미 차단한 사용자 입니다."
    ),


    /* ===== 크루 관련 ===== */
    CREW_NOT_FOUND(HttpStatus.NOT_FOUND, "CR001", "크루를 찾을 수 없습니다."),
    CREW_ALREADY_EXISTS(HttpStatus.CONFLICT, "CR002", "이미 존재하는 크루명입니다."),
    CREW_ALREADY_DISBANDED(HttpStatus.CONFLICT, "CR003", "이미 해체된 크루입니다."),
    NOT_CREW_LEADER(HttpStatus.FORBIDDEN, "CR004", "크루장만 수정/삭제할 수 있습니다."),
    NOT_CREW_LEADER_OR_SUB_LEADER(HttpStatus.FORBIDDEN, "CR005", "크루장 또는 부크루장만 처리할 수 있습니다."),
    ALREADY_CREW_LEADER(HttpStatus.CONFLICT, "CR006", "이미 크루를 생성하셨습니다. 하나의 크루만 생성 가능합니다."),
    NOT_PREMIUM_MEMBER(HttpStatus.FORBIDDEN, "CR007", "프리미엄 멤버십 회원만 크루를 생성할 수 있습니다."),
    INVALID_CREW_STATUS(HttpStatus.BAD_REQUEST, "CR008", "유효하지 않은 크루 상태입니다."),
    ALREADY_CREW_MEMBER(HttpStatus.CONFLICT, "CR009", "이미 가입된 크루입니다."),
    JOIN_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "CR010", "가입 신청을 찾을 수 없습니다."),
    JOIN_REQUEST_NOT_PENDING(HttpStatus.CONFLICT, "CR011", "대기 상태의 요청만 처리할 수 있습니다."),
    ALREADY_REQUESTED(HttpStatus.CONFLICT, "CR012", "이미 가입 신청한 크루입니다."),
    CANNOT_LEAVE_AS_LEADER(HttpStatus.CONFLICT, "CR013",
            "크루장은 탈퇴할 수 없습니다. 부크루장 또는 운영진에게 크루장을 위임하거나 크루를 해체해주세요."),
    /* ===== 크루 가입 ===== */
    ALREADY_JOINED_CREW(HttpStatus.CONFLICT, "CR014", "이미 가입한 크루가 있습니다."),
    CREW_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "CR015", "크루원을 찾을 수 없습니다."),
    CREW_RECRUITMENT_CLOSED(HttpStatus.CONFLICT, "CR016", "모집이 마감된 크루입니다."),
    NOT_CREW_USER(HttpStatus.FORBIDDEN, "CR017", "크루원이 아닙니다."),
    CANNOT_ASSIGN_LEADER_TO_MEMBER(HttpStatus.FORBIDDEN, "CR018", "일반 멤버는 크루장이 될 수 없습니다. 부크루장 또는 운영진에게만 위임 가능합니다."),
    /*==== 파일 ====*/
    FILE_UPLOAD_FAILED(

            HttpStatus.INTERNAL_SERVER_ERROR,
            "F001",
            "파일 업로드에 실패했습니다."
    ),
    FILE_REQUIRED(HttpStatus.BAD_REQUEST, "F002", "필수 파일이 누락되었습니다."),
    FILE_EMPTY(
            HttpStatus.BAD_REQUEST,
            "F002",
            "업로드할 파일이 비어 있습니다."
    ),
    FILE_NOT_IMAGE(
            HttpStatus.BAD_REQUEST,
            "F003",
            "이미지 파일만 업로드할 수 있습니다."
    ),
    FILE_SIZE_EXCEEDED(
            HttpStatus.BAD_REQUEST,
            "F004",
            "파일 크기가 제한을 초과했습니다."
    ),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F005", "파일 삭제에 실패했습니다."),


    /* ===== 챌린지 ===== */
    ALREADY_JOINED(
            HttpStatus.CONFLICT,
            "CH001",
            "이미 참여 중인 챌린지입니다."
    ),
    CHALLENGE_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "CH002",
            "해당 챌린지에 대한 권한이 없습니다."
    ),
    CHALLENGE_CANNOT_UPDATE(
            HttpStatus.BAD_REQUEST,
            "CH003",
            "진행 중이거나 참여자가 있는 챌린지의 주요 정보는 수정할 수 없습니다."
    ),
    CHALLENGE_CANNOT_DELETE(
            HttpStatus.BAD_REQUEST,
            "CH004",
            "진행 중이거나 참여자가 있는 챌린지는 삭제할 수 없습니다."
    ),


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
  INVALID_ROUTE_PATH(
      HttpStatus.BAD_REQUEST,
      "COURSE_400_001",
      "코스 경로 형식이 올바르지 않습니다"
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
  ALREADY_LIKED_COURSE(HttpStatus.CONFLICT, "CRS_005", "이미 좋아요한 코스입니다"),
  COURSE_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "CRS_006", "사용할 수 없는 코스입니다"),
  CANNOT_LIKE_OWN_COURSE(HttpStatus.BAD_REQUEST, "CRS_007",
      "본인의 코스는 좋아요할 수 없습니다"),
  COURSELIKE_NOT_FOUND(
      HttpStatus.NOT_FOUND, "CRS_008", "해당 좋아요를 찾을 수 없습니다"),
  NOT_LIKED(HttpStatus.BAD_REQUEST, "CRS_009", "좋아요를 누르지 않은 코스입니다"),
  ALREADY_FAVORITE_COURSE(HttpStatus.CONFLICT, "CRS_010", "이미 즐겨찾기한 코스입니다"),
  CANNOT_FAVORITE_OWN_COURSE(HttpStatus.BAD_REQUEST, "CRS_011",
      "본인의 코스는 즐겨찾기할 수 없습니다"),
  FAVORITE_NOT_FOUND(
      HttpStatus.NOT_FOUND, "CRS_012", "해당 즐겨찾기를 찾을 수 없습니다"),
  NOT_FAVORITE(HttpStatus.BAD_REQUEST, "CRS_013", "즐겨찾기를 누르지 않은 코스입니다"),
  CANNOT_SIREN_OWN_COURSE(HttpStatus.BAD_REQUEST, "CRS_014", "본인의 코스는 신고할 수 없습니다"),
  ALREADY_SIREN_COURSE(HttpStatus.CONFLICT, "CRS_015", "이미 신고한 코스입니다"),

    /* ==== 모집글 ====*/
    RECRUIT_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "모집글을 찾을 수 없습니다."),
    INVALID_RECRUIT(HttpStatus.BAD_REQUEST, "R002", "삭제된 모집글입니다"),
    RECRUIT_UPDATE_DENIED(HttpStatus.FORBIDDEN, "R003", "모집글 수정 권한이 없습니다."),
    RECRUIT_DELETE_DENIED(HttpStatus.FORBIDDEN, "R004", "모집글 삭제 권한이 없습니다."),
    RECRUIT_HAS_PARTICIPANTS(HttpStatus.BAD_REQUEST, "R005", "참여자가 있어 모집글을 수정할 수 없습니다."),
    ALREADY_PARTICIPATED(HttpStatus.BAD_REQUEST, "R006", "이미 참여 중입니다."),
    NOT_PARTICIPATED(HttpStatus.BAD_REQUEST, "R007", "참여 상태가 아닙니다."),
    INVALID_AGE_RANGE(HttpStatus.BAD_REQUEST, "R008", "최소 나이는 최대 나이보다 클 수 없습니다."),
    INVALID_MEETING_TIME(HttpStatus.BAD_REQUEST, "R009", "모임 날짜는 오늘부터 2주일 이내로만 설정 가능합니다."),
    RECRUIT_FULL(HttpStatus.BAD_REQUEST, "R010", "참여 인원이 다 찼습니다."),
    UNAUTHORIZED_HOST(HttpStatus.FORBIDDEN, "R011", "방장에게만 권한이 있습니다."),
    RECRUIT_TIME_OVER(HttpStatus.BAD_REQUEST, "R012", "모집 신청 기간이 지났습니다."),
    NOT_ENOUGH_PARTICIPANTS(HttpStatus.BAD_REQUEST, "RO13", "참가자가 최소 1명은 더 있어야 출발할 수 있습니다."),
    TOO_EARLY_TO_START(HttpStatus.BAD_REQUEST, "RO14", "매칭 확정은 모임 시간 3시간 전부터 가능합니다."),
    AGE_RESTRICTION(HttpStatus.BAD_REQUEST, "R015", "참여 가능 나이가 아닙니다."),
    GENDER_RESTRICTION(HttpStatus.BAD_REQUEST, "R016", "참여 가능 성별이 아닙니다."),
    BIRTHDATE_REQUIRED(HttpStatus.BAD_REQUEST, "R017", "권한이 없는 나이대 입니다."),
    /*===== 세션/채팅 =====*/
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SES_001", "세션을 찾을 수 없습니다."),
    SESSION_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "SES_002", "해당 세션에 참여하지 않은 사용자입니다."),
    NOT_SESSION_HOST(HttpStatus.FORBIDDEN, "SES_003", "방장만 실행할 수 있습니다."),
    CANNOT_KICK_SELF(HttpStatus.BAD_REQUEST, "SES_004", "자기 자신은 강퇴할 수 없습니다."),
    USER_ALREADY_LEFT(HttpStatus.BAD_REQUEST, "SES_005", "이미 퇴장한 사용자입니다."),
    ALL_USERS_NOT_READY(HttpStatus.BAD_REQUEST, "SES_006", "모든 참가자가 준비완료해야 합니다."),
    HOST_NOT_FOUND(HttpStatus.NOT_FOUND, "SES_007", "방장을 찾을 수 없습니다."),
    /*=====MAPBOX =====*/
    MAPBOX_ACCESS_TOKEN_EMPTY(HttpStatus.INTERNAL_SERVER_ERROR, "M_001",
            "MAPBOX_ACCESS_TOKEN 설정이 비어있습니다."),
    MAPBOX_OVERLAY_EMPTY(HttpStatus.BAD_REQUEST, "M_002",
            "썸네일 생성에 필요한 overlay 값이 비어있습니다."),
    /*=====TTS=====*/
    TTSVOICE_NOT_FOUND(HttpStatus.NOT_FOUND, "T_001", "해당 보이스 타입을 찾을 수 없습니다"),
    TTS_CUE_CODE_INVALID(HttpStatus.BAD_REQUEST, "T_002", "큐 코드가 비어있거나 없습니다."),
    TTS_VOICE_PACK_PREFIX_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "T_003",
            "보이스팩 S3 prefix 설정이 올바르지 않습니다."),
    /*=====쿠폰=====*/
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "CPN_001", "쿠폰을 찾을 수 없습니다"),
    COUPON_CODE_DUPLICATE(HttpStatus.CONFLICT, "CPN_002", "쿠폰 코드가 중복입니다"),
    COUPON_NOT_DRAFT(HttpStatus.FORBIDDEN, "CPN_003", "DRAFT 상태만 변경할 수 있습니다"),
    /* ===== 이용약관 ===== */
    TERMS_ACCESS_DENIED(HttpStatus.FORBIDDEN, "T001", "약관 관리 권한이 없습니다."),
    DUPLICATE_TERMS_VERSION(HttpStatus.CONFLICT, "T002", "이미 존재하는 약관 버전입니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}