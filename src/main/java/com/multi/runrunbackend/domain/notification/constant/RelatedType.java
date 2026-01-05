package com.multi.runrunbackend.domain.notification.constant;

/**
 * @author : KIMGWANGHO
 * @description : 알림 클릭 시 이동할 대상 타입 관리 Enum (채팅방, 크루)
 * @filename : RelatedType
 * @since : 2025-12-17 수요일
 */
public enum RelatedType {
  CREW_CHAT_ROOM,

  RECRUIT,

  OFFLINE_CHAT_ROOM,
  ONLINE_BATTLE,
  SOLORUN,
  GHOSTRUN,

  BATTLE_RESULT,
  RUNNING_RESULT,

  CHALLENGE,
  FEED_POST,

  CREW,
  CREW_JOIN_REQUEST,
  CREW_USER,
  CREW_ACTIVITY,
  CREW_ACTIVITY_USER,

  MEMBERSHIP,

  PAYMENT,

  USER_POINT,
  POINT_HISTORY,
  POINT_EXPIRATION,
  POINT_PRODUCT,

  RATING,

  AD,
  COUPON,
  COURSE,

  USERS
}