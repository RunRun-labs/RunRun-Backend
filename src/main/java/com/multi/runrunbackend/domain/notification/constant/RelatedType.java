package com.multi.runrunbackend.domain.notification.constant;

/**
 * @author : KIMGWANGHO
 * @description : 알림 클릭 시 이동할 대상 타입 관리 Enum (채팅방, 크루)
 * @filename : RelatedType
 * @since : 2025-12-17 수요일
 */
public enum RelatedType {
  OFF_CHAT_ROOM,
  RECRUIT,
  WAITING_ROOM,

  CREW_JOIN_REQUEST,
  CREW,
  CREW_USERS,
  CREW_MAIN,
  CREW_CHAT_ROOM,

  MEMBERSHIP,

  POINT_BALANCE,

  CHALLENGE,
  CHALLENGE_END,

  FEED_RECORD,

  FRIEND_LIST
}