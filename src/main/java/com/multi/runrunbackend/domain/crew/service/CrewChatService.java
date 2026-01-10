package com.multi.runrunbackend.domain.crew.service;

import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.chat.service.RedisPublisher;
import com.multi.runrunbackend.domain.crew.document.CrewChatMessage;
import com.multi.runrunbackend.domain.crew.dto.req.CrewChatMessageDto;
import com.multi.runrunbackend.domain.crew.dto.req.CrewChatNoticeReqDto;
import com.multi.runrunbackend.domain.crew.dto.res.CrewChatNoticeResDto;
import com.multi.runrunbackend.domain.crew.dto.res.CrewChatRoomListResDto;
import com.multi.runrunbackend.domain.crew.entity.Crew;
import com.multi.runrunbackend.domain.crew.entity.CrewChatNotice;
import com.multi.runrunbackend.domain.crew.entity.CrewChatRoom;
import com.multi.runrunbackend.domain.crew.entity.CrewChatUser;
import com.multi.runrunbackend.domain.crew.entity.CrewUser;
import com.multi.runrunbackend.domain.crew.constant.CrewRole;
import com.multi.runrunbackend.common.exception.custom.BusinessException;
import com.multi.runrunbackend.domain.crew.repository.CrewChatMessageRepository;
import com.multi.runrunbackend.domain.crew.repository.CrewChatRoomRepository;
import com.multi.runrunbackend.domain.crew.repository.CrewChatUserRepository;
import com.multi.runrunbackend.domain.user.entity.User;
import com.multi.runrunbackend.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : changwoo
 * @description : 크루 채팅 서비스
 * @filename : CrewChatService
 * @since : 2026-01-04
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CrewChatService {

  private final RedisPublisher redisPublisher;
  private final CrewChatMessageRepository chatMessageRepository;
  private final CrewChatRoomRepository chatRoomRepository;
  private final CrewChatUserRepository chatUserRepository;
  private final UserRepository userRepository;
  private final com.multi.runrunbackend.domain.crew.repository.CrewUserRepository crewUserRepository;  // ⭐ 추가
  private final com.multi.runrunbackend.domain.crew.repository.CrewChatNoticeRepository chatNoticeRepository;


  /**
   * 로그인 정보에서 User 엔티티 조회
   */
  private User getUserFromPrincipal(CustomUser principal) {
    String loginId = principal.getEmail();
    return userRepository.findByLoginId(loginId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
  }

  /**
   * 현재 로그인 사용자 정보 조회
   */
  @Transactional(readOnly = true)
  public Map<String, Object> getCurrentUserInfo(CustomUser principal) {
    User user = getUserFromPrincipal(principal);

    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put("userId", user.getId());
    userInfo.put("loginId", user.getLoginId());
    userInfo.put("name", user.getName());

    return userInfo;
  }

  /**
   * 메시지 전송 (MongoDB 저장 + Redis Pub/Sub 발행)
   */
  public void sendMessage(CrewChatMessageDto messageDto) {
    // 현재 시간 설정
    LocalDateTime now = LocalDateTime.now();
    
    // MongoDB에 저장
    CrewChatMessage chatMessage = CrewChatMessage.builder()
        .roomId(messageDto.getRoomId())
        .senderId(messageDto.getSenderId())
        .senderName(messageDto.getSenderName())
        .content(messageDto.getContent())
        .messageType(messageDto.getMessageType())
        .createdAt(now)
        .build();

    chatMessageRepository.save(chatMessage);

    // DTO에 시간 설정 후 Redis Pub/Sub으로 발행
    messageDto.setCreatedAt(now);
    String channel = "crew-chat:" + messageDto.getRoomId();
    redisPublisher.publishObject(channel, messageDto);

    log.info("크루 메시지 전송: roomId={}, sender={}, createdAt={}", 
        messageDto.getRoomId(), messageDto.getSenderName(), now);
  }

  /**
   * 크루 가입 시스템 메시지 전송
   */
  public void sendJoinMessage(Long roomId, String userName) {
    CrewChatMessageDto systemMessage = CrewChatMessageDto.builder()
        .roomId(roomId)
        .senderName("SYSTEM")
        .content(userName + "님이 크루에 가입했습니다.")
        .messageType("SYSTEM")
        .build();

    sendMessage(systemMessage);
    log.info("크루 가입 시스템 메시지 전송: roomId={}, userName={}", roomId, userName);
  }

  /**
   * 크루 탈퇴 시스템 메시지 전송
   */
  public void sendLeaveMessage(Long roomId, String userName) {
    CrewChatMessageDto systemMessage = CrewChatMessageDto.builder()
        .roomId(roomId)
        .senderName("SYSTEM")
        .content(userName + "님이 크루를 탈퇴했습니다.")
        .messageType("SYSTEM")
        .build();

    sendMessage(systemMessage);
    log.info("크루 탈퇴 시스템 메시지 전송: roomId={}, userName={}", roomId, userName);
  }

  /**
   * 과거 메시지 조회 (가입 시점 이후 메시지만 조회)
   */
  @Transactional(readOnly = true)
  public List<CrewChatMessage> getMessages(Long roomId, CustomUser principal) {
    User user = getUserFromPrincipal(principal);
    
    // 사용자의 채팅방 가입 시점 조회
    CrewChatUser chatUser = chatUserRepository.findByRoomIdAndUserId(roomId, user.getId())
        .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_CREW_USER));
    
    LocalDateTime joinedAt = chatUser.getCreatedAt();
    
    log.info("크루 메시지 조회: roomId={}, userId={}, joinedAt={}", 
        roomId, user.getId(), joinedAt);
    
    // 가입 시점 이후 메시지만 조회
    return chatMessageRepository.findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(roomId, joinedAt);
  }

  /**
   * 채팅방 참여자 목록 조회 (크루 역할 포함)
   */
  @Transactional(readOnly = true)
  public List<Map<String, Object>> getRoomUsers(Long roomId) {
    List<CrewChatUser> chatUsers = chatUserRepository.findActiveUsersByRoomId(roomId);

    // 크루 ID 조회
    if (chatUsers.isEmpty()) {
      return List.of();
    }
    
    Long crewId = chatUsers.get(0).getRoom().getCrew().getId();

    return chatUsers.stream()
        .map(cu -> {
          Map<String, Object> map = new HashMap<>();
          User user = cu.getUser();
          map.put("userId", user.getId());
          map.put("name", user.getName());
          
          // ⭐ 크루 역할 조회
          crewUserRepository.findByCrewIdAndUserIdAndIsDeletedFalse(crewId, user.getId())
              .ifPresent(crewUser -> {
                map.put("role", crewUser.getRole().name());
              });
          
          return map;
        })
        .collect(Collectors.toList());
  }

  /**
   * 채팅방 상세 정보 조회
   */
  @Transactional(readOnly = true)
  public Map<String, Object> getRoomDetail(Long roomId) {
    CrewChatRoom room = chatRoomRepository.findById(roomId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.CHAT_ROOM_NOT_FOUND));

    Crew crew = room.getCrew();

    Map<String, Object> result = new HashMap<>();
    result.put("roomId", room.getId());
    result.put("roomName", room.getCrewRoomName());
    result.put("crewId", crew.getId());
    result.put("crewName", crew.getCrewName());
    result.put("crewDescription", crew.getCrewDescription());  // ⭐ 추가

    return result;
  }

  /**
   * 유저가 참여 중인 크루 채팅방 목록 조회
   */
  @Transactional(readOnly = true)
  public List<CrewChatRoomListResDto> getMyChatRoomList(CustomUser principal) {
    User user = getUserFromPrincipal(principal);
    log.info("=== 크루 채팅방 목록 조회 시작: userId={} ===", user.getId());

    // 1. 유저가 참여 중인 크루 채팅방 목록 조회
    List<CrewChatUser> myCrewChatUsers = chatUserRepository.findMyCrewChatRooms(user.getId());
    log.info("참여 중인 크루 채팅방 수: {}", myCrewChatUsers.size());

    // 2. 각 채팅방을 DTO로 변환
    List<CrewChatRoomListResDto> result = myCrewChatUsers.stream()
        .map(this::convertToCrewChatRoomDto)
        .sorted((a, b) -> {
          // lastMessageTime 기준 내림차순 정렬 (최신 메시지가 맨 위)
          LocalDateTime timeA = a.getLastMessageTime();
          LocalDateTime timeB = b.getLastMessageTime();

          if (timeA == null && timeB == null) return 0;
          if (timeA == null) return 1;
          if (timeB == null) return -1;

          return timeB.compareTo(timeA);
        })
        .toList();

    log.info("=== 크루 채팅방 목록 조회 완료: {} 개 ===", result.size());
    return result;
  }

  /**
   * CrewChatUser를 CrewChatRoomListResDto로 변환
   */
  private CrewChatRoomListResDto convertToCrewChatRoomDto(CrewChatUser chatUser) {
    CrewChatRoom room = chatUser.getRoom();
    Crew crew = room.getCrew();

    // 참가자 수 조회
    Long currentMembers = chatUserRepository.countActiveUsersByRoomId(room.getId());

    // 최근 메시지 조회
    CrewChatMessage lastMessage = chatMessageRepository.findTopByRoomIdOrderByCreatedAtDesc(
        room.getId());

    // 읽지 않은 메시지 개수 계산 (TODO: lastReadAt 필드 추가 후 구현)
    int unreadCount = 0;

    return CrewChatRoomListResDto.builder()
        .roomId(room.getId())
        .roomName(room.getCrewRoomName())
        .crewId(crew.getId())
        .crewName(crew.getCrewName())
        .crewDescription(crew.getCrewDescription())
        .currentMembers(currentMembers.intValue())
        .lastMessageContent(lastMessage != null ? lastMessage.getContent() : null)
        .lastMessageSender(lastMessage != null ? lastMessage.getSenderName() : null)
        .lastMessageTime(lastMessage != null ? lastMessage.getCreatedAt() : null)
        .unreadCount(unreadCount)
        .build();
  }

  // ============================================
  // 크루 기능 연동 메서드
  // ============================================

  /**
   * 크루 생성 시 채팅방 자동 생성
   */
  @Transactional
  public CrewChatRoom createChatRoomForCrew(Crew crew, User leader) {
    // 채팅방 생성
    CrewChatRoom chatRoom = CrewChatRoom.builder()
        .crewRoomName(crew.getCrewName() + " 채팅방")
        .crew(crew)
        .build();
    chatRoomRepository.save(chatRoom);

    // 크루장을 채팅방 참여자로 추가
    CrewChatUser chatUser = CrewChatUser.builder()
        .room(chatRoom)
        .user(leader)
        .build();
    chatUserRepository.save(chatUser);

    log.info("크루 채팅방 생성 완료: crewId={}, roomId={}, leaderId={}",
        crew.getId(), chatRoom.getId(), leader.getId());

    return chatRoom;
  }

  /**
   * 가입 승인 시 채팅방 참여자 추가 + 시스템 메시지 전송
   */
  @Transactional
  public void addUserToChatRoom(Long crewId, User user) {
    // 크루의 채팅방 조회
    CrewChatRoom chatRoom = chatRoomRepository.findByCrewId(crewId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.CHAT_ROOM_NOT_FOUND));

    // 이미 참여 중인지 확인 (soft delete된 경우 재활성화)
    chatUserRepository.findByRoomIdAndUserId(chatRoom.getId(), user.getId())
        .ifPresentOrElse(
            existingChatUser -> {
              // 삭제된 상태면 재활성화 (향후 필요 시 구현)
              log.info("이미 존재하는 채팅방 참여자: roomId={}, userId={}",
                  chatRoom.getId(), user.getId());
            },
            () -> {
              // 새로 추가
              CrewChatUser chatUser = CrewChatUser.builder()
                  .room(chatRoom)
                  .user(user)
                  .build();
              chatUserRepository.save(chatUser);

              log.info("채팅방 참여자 추가 완료: roomId={}, userId={}",
                  chatRoom.getId(), user.getId());
              
              // ⭐ 크루 가입 시스템 메시지 전송
              sendJoinMessage(chatRoom.getId(), user.getName());
            }
        );
  }

  /**
   * 크루 탈퇴 시 채팅방 참여자 제거 + 시스템 메시지 전송
   */
  @Transactional
  public void removeUserFromChatRoom(Long crewId, User user) {
    // 크루의 채팅방 조회
    CrewChatRoom chatRoom = chatRoomRepository.findByCrewId(crewId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.CHAT_ROOM_NOT_FOUND));

    // 채팅방 참여자 조회
    CrewChatUser chatUser = chatUserRepository.findByRoomIdAndUserId(
            chatRoom.getId(), user.getId())
        .orElse(null);

    if (chatUser != null) {
      // ⭐ 탈퇴 시스템 메시지 전송 (delete 전에 호출)
      sendLeaveMessage(chatRoom.getId(), user.getName());
      
      // Soft delete 처리 (BaseEntity의 isDeleted 사용)
      chatUserRepository.delete(chatUser);

      log.info("채팅방 참여자 제거 완료: roomId={}, userId={}",
          chatRoom.getId(), user.getId());
    }
  }

  /**
   * 크루 해체 시 채팅방 삭제
   */
  @Transactional
  public void deleteChatRoom(Long crewId) {
    // 크루의 채팅방 조회
    chatRoomRepository.findByCrewId(crewId).ifPresent(chatRoom -> {
      Long roomId = chatRoom.getId();
      
      // 1. MongoDB 채팅 메시지 삭제
      chatMessageRepository.deleteByRoomId(roomId);
      log.info("채팅 메시지 삭제 완료: roomId={}", roomId);
      
      // 2. 모든 참여자 제거
      List<CrewChatUser> chatUsers = chatUserRepository.findActiveUsersByRoomId(roomId);
      chatUserRepository.deleteAll(chatUsers);

      // 3. 채팅방 삭제
      chatRoomRepository.delete(chatRoom);

      log.info("크루 채팅방 삭제 완료: crewId={}, roomId={}", crewId, roomId);
    });
  }

  // ============================================
  // 공지사항 기능
  // ============================================

  /**
   * 공지사항 작성
   */
  @Transactional
  public CrewChatNoticeResDto createNotice(Long roomId, CustomUser principal, CrewChatNoticeReqDto reqDto) {
    User user = getUserFromPrincipal(principal);
    
    // 채팅방 조회
    CrewChatRoom chatRoom = chatRoomRepository.findById(roomId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    
    // 권한 검증: STAFF 이상
    validateStaffOrAbove(chatRoom.getCrew().getId(), user.getId());
    
    // 공지사항 생성
    CrewChatNotice notice = CrewChatNotice.create(chatRoom, user, reqDto.getContent());
    chatNoticeRepository.save(notice);
    
    // WebSocket으로 실시간 알림
    sendNoticeMessage(roomId, "NOTICE_CREATED", user.getName());
    
    log.info("공지사항 작성 완료: roomId={}, userId={}, noticeId={}", 
        roomId, user.getId(), notice.getId());
    
    return CrewChatNoticeResDto.fromEntity(notice);
  }

  /**
   * 공지사항 목록 조회
   */
  @Transactional(readOnly = true)
  public List<CrewChatNoticeResDto> getNotices(Long roomId) {
    List<CrewChatNotice> notices = chatNoticeRepository
        .findByRoomIdAndIsDeletedFalseOrderByCreatedAtDesc(roomId);
    
    return notices.stream()
        .map(CrewChatNoticeResDto::fromEntity)
        .collect(Collectors.toList());
  }

  /**
   * 공지사항 수정
   */
  @Transactional
  public CrewChatNoticeResDto updateNotice(Long noticeId, CustomUser principal, CrewChatNoticeReqDto reqDto) {
    User user = getUserFromPrincipal(principal);
    
    // 공지사항 조회
    CrewChatNotice notice = chatNoticeRepository.findById(noticeId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.NOTICE_NOT_FOUND));
    
    // 권한 검증: 작성자 본인 또는 STAFF 이상
    Long crewId = notice.getRoom().getCrew().getId();
    if (!notice.getCreatedBy().getId().equals(user.getId())) {
      validateStaffOrAbove(crewId, user.getId());
    }
    
    // 공지사항 수정
    notice.update(reqDto.getContent());
    
    // WebSocket으로 실시간 알림
    sendNoticeMessage(notice.getRoom().getId(), "NOTICE_UPDATED", user.getName());
    
    log.info("공지사항 수정 완료: noticeId={}, userId={}", noticeId, user.getId());
    
    return CrewChatNoticeResDto.fromEntity(notice);
  }

  /**
   * 공지사항 삭제
   */
  @Transactional
  public void deleteNotice(Long noticeId, CustomUser principal) {
    User user = getUserFromPrincipal(principal);
    
    // 공지사항 조회
    CrewChatNotice notice = chatNoticeRepository.findById(noticeId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.NOTICE_NOT_FOUND));
    
    // 권한 검증: 작성자 본인 또는 STAFF 이상
    Long crewId = notice.getRoom().getCrew().getId();
    Long roomId = notice.getRoom().getId();
    if (!notice.getCreatedBy().getId().equals(user.getId())) {
      validateStaffOrAbove(crewId, user.getId());
    }
    
    // Soft delete
    chatNoticeRepository.delete(notice);
    
    // WebSocket으로 실시간 알림
    sendNoticeMessage(roomId, "NOTICE_DELETED", user.getName());
    
    log.info("공지사항 삭제 완료: noticeId={}, userId={}", noticeId, user.getId());
  }

  /**
   * STAFF 이상 권한 검증
   */
  private void validateStaffOrAbove(Long crewId, Long userId) {
    CrewUser crewUser = crewUserRepository
        .findByCrewIdAndUserIdAndIsDeletedFalse(crewId, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_CREW_USER));
    
    CrewRole role = crewUser.getRole();
    if (role != CrewRole.LEADER && role != CrewRole.SUB_LEADER && role != CrewRole.STAFF) {
      throw new BusinessException(ErrorCode.INSUFFICIENT_PERMISSION);
    }
  }

  /**
   * 공지사항 변경 실시간 알림
   */
  private void sendNoticeMessage(Long roomId, String action, String userName) {
    String message = "";
    switch (action) {
      case "NOTICE_CREATED":
        message = userName + "님이 공지사항을 등록했습니다.";
        break;
      case "NOTICE_UPDATED":
        message = userName + "님이 공지사항을 수정했습니다.";
        break;
      case "NOTICE_DELETED":
        message = userName + "님이 공지사항을 삭제했습니다.";
        break;
    }

    // WebSocket 전송을 위한 DTO (⭐ CrewChatMessageDto 사용)
    CrewChatMessageDto noticeDto = CrewChatMessageDto.builder()
        .roomId(roomId)
        .senderName("SYSTEM")
        .content(message)
        .messageType("NOTICE")
        .build();

    // Redis로 publish (채널 이름을 일반 메시지와 동일하게)
    String channel = "crew-chat:" + roomId;
    redisPublisher.publishObject(channel, noticeDto);  // ⭐ DTO로 전송
    log.info("📤 공지사항 실시간 알림 발송: channel={}, action={}", channel, action);

    // MongoDB에도 저장 (⭐ CrewChatMessage로 저장)
    CrewChatMessage noticeMsg = CrewChatMessage.builder()
        .roomId(roomId)
        .senderName("SYSTEM")
        .content(message)
        .messageType("NOTICE")
        .createdAt(LocalDateTime.now())
        .build();
    chatMessageRepository.save(noticeMsg);
  }

}
