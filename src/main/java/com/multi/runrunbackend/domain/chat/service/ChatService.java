package com.multi.runrunbackend.domain.chat.service;

import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.chat.document.OfflineChatMessage;
import com.multi.runrunbackend.domain.chat.dto.ChatMessageDto;
import com.multi.runrunbackend.domain.chat.dto.res.ChatRoomListResDto;
import com.multi.runrunbackend.domain.chat.repository.OfflineChatMessageRepository;
import com.multi.runrunbackend.domain.match.constant.SessionStatus;
import com.multi.runrunbackend.domain.match.entity.MatchSession;
import com.multi.runrunbackend.domain.match.entity.SessionUser;
import com.multi.runrunbackend.domain.match.repository.MatchSessionRepository;
import com.multi.runrunbackend.domain.match.repository.SessionUserRepository;
import com.multi.runrunbackend.domain.recruit.entity.Recruit;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

  private final RedisPublisher redisPublisher;
  private final OfflineChatMessageRepository chatMessageRepository;
  private final SessionUserRepository sessionUserRepository;
  private final MatchSessionRepository matchSessionRepository;
  private final UserRepository userRepository;


  private User getUserFromPrincipal(CustomUser principal) {
    String loginId = principal.getEmail();  // getEmail()이지만 실제로는 loginId가 들어있음
    return userRepository.findByLoginId(loginId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
  }


  @Transactional(readOnly = true)
  public Map<String, Object> getCurrentUserInfo(CustomUser principal) {
    User user = getUserFromPrincipal(principal);

    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put("userId", user.getId());
    userInfo.put("loginId", user.getLoginId());
    userInfo.put("name", user.getName());

    return userInfo;
  }


  public void sendMessage(ChatMessageDto messageDto) {
    // MongoDB에 저장
    OfflineChatMessage chatMessage = OfflineChatMessage.builder()
        .sessionId(messageDto.getSessionId())
        .senderId(messageDto.getSenderId())
        .senderName(messageDto.getSenderName())
        .content(messageDto.getContent())
        .messageType(messageDto.getMessageType())
        .createdAt(LocalDateTime.now())
        .build();

    chatMessageRepository.save(chatMessage);

    // Redis Pub/Sub으로 발행
    String channel = "chat:" + messageDto.getSessionId();
    redisPublisher.publish(channel, messageDto);

    log.info("메시지 전송: sessionId={}, sender={}", messageDto.getSessionId(),
        messageDto.getSenderName());
  }

  public void sendEnterMessage(Long sessionId, String userName) {
    ChatMessageDto systemMessage = ChatMessageDto.builder()
        .sessionId(sessionId)
        .senderName("SYSTEM")
        .content(userName + "님이 입장했습니다.")
        .messageType("SYSTEM")
        .build();

    sendMessage(systemMessage);
  }

  public void sendLeaveMessage(Long sessionId, String userName) {
    ChatMessageDto systemMessage = ChatMessageDto.builder()
        .sessionId(sessionId)
        .senderName("SYSTEM")
        .content(userName + "님이 퇴장했습니다.")
        .messageType("SYSTEM")
        .build();

    sendMessage(systemMessage);
  }

  @Transactional(readOnly = true)
  public List<OfflineChatMessage> getMessages(Long sessionId, LocalDateTime joinedAt) {
    if (joinedAt != null) {
      return chatMessageRepository.findBySessionIdAndCreatedAtAfterOrderByCreatedAtAsc(sessionId,
          joinedAt);
    }
    return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
  }


  @Transactional(readOnly = true)
  public List<Map<String, Object>> getSessionUsers(Long sessionId) {
    List<SessionUser> sessionUsers = sessionUserRepository.findActiveUsersBySessionId(sessionId);

    return sessionUsers.stream()
        .map(su -> {
          Map<String, Object> map = new HashMap<>();
          map.put("userId", su.getUser().getId());
          map.put("name", su.getUser().getName());
          map.put("isReady", su.isReady());
          return map;
        })
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getSessionDetail(Long sessionId) {
    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

    Map<String, Object> result = new HashMap<>();
    result.put("sessionId", session.getId());
    result.put("type", session.getType().name());
    result.put("status", session.getStatus().name());
    result.put("targetDistance", session.getTargetDistance());

    // Recruit 정보
    Recruit recruit = session.getRecruit();
    if (recruit != null) {
      result.put("hostId", recruit.getUser().getId());
      result.put("meetingPlace", recruit.getMeetingPlace());
      result.put("meetingTime", recruit.getMeetingAt());
      result.put("title", recruit.getTitle());
    } else {
      // Recruit 없는 경우 첫 번째 참여자가 방장
      List<SessionUser> users = sessionUserRepository.findActiveUsersBySessionId(sessionId);
      if (!users.isEmpty()) {
        result.put("hostId", users.get(0).getUser().getId());
      }
      result.put("title", "제목 없음");
    }

    return result;
  }

  @Transactional(readOnly = true)
  public String getJoinedAt(Long sessionId, CustomUser principal) {
    User user = getUserFromPrincipal(principal);

    SessionUser sessionUser = sessionUserRepository.findBySessionIdAndUserId(sessionId,
            user.getId())
        .orElseThrow(() -> new IllegalArgumentException("참가자를 찾을 수 없습니다."));

    return sessionUser.getCreatedAt().toString();
  }

  @Transactional
  public void leaveSession(Long sessionId, CustomUser principal) {
    User user = getUserFromPrincipal(principal);

    sessionUserRepository.softDeleteBySessionIdAndUserId(sessionId, user.getId());
    log.info("세션 퇴장: sessionId={}, userId={}", sessionId, user.getId());
  }

  @Transactional
  public Map<String, Object> toggleReady(Long sessionId, CustomUser principal) {
    User user = getUserFromPrincipal(principal);

    SessionUser sessionUser = sessionUserRepository.findBySessionIdAndUserId(sessionId,
            user.getId())
        .orElseThrow(() -> new IllegalArgumentException("참가자를 찾을 수 없습니다."));

    boolean newReadyState = !sessionUser.isReady();
    sessionUserRepository.updateReadyStatus(sessionId, user.getId(), newReadyState);

    Map<String, Object> result = new HashMap<>();
    result.put("userId", user.getId());
    result.put("isReady", newReadyState);

    log.info("준비 상태 변경: sessionId={}, userId={}, isReady={}", sessionId, user.getId(),
        newReadyState);

    return result;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> checkAllReady(Long sessionId) {
    long totalCount = sessionUserRepository.countActiveUsersBySessionId(sessionId);
    long readyCount = sessionUserRepository.countReadyUsersBySessionId(sessionId);

    Map<String, Object> result = new HashMap<>();
    result.put("totalCount", totalCount);
    result.put("readyCount", readyCount);
    result.put("allReady", totalCount > 0 && totalCount == readyCount);

    return result;
  }

  @Transactional
  public void startRunning(Long sessionId, CustomUser principal) {
    User user = getUserFromPrincipal(principal);

    MatchSession session = matchSessionRepository.findById(sessionId)
        .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

    Long hostId = null;
    Recruit recruit = session.getRecruit();
    if (recruit != null) {
      hostId = recruit.getUser().getId();
    } else {
      List<SessionUser> users = sessionUserRepository.findActiveUsersBySessionId(sessionId);
      if (!users.isEmpty()) {
        hostId = users.get(0).getUser().getId();
      }
    }

    if (!user.getId().equals(hostId)) {
      throw new IllegalArgumentException("방장만 런닝을 시작할 수 있습니다.");
    }

    Map<String, Object> readyStatus = checkAllReady(sessionId);
    if (!(Boolean) readyStatus.get("allReady")) {
      throw new IllegalArgumentException("모든 참가자가 준비완료해야 합니다.");
    }

    // 상태 변경
    matchSessionRepository.updateStatus(sessionId, SessionStatus.IN_PROGRESS);

    log.info("런닝 시작: sessionId={}, hostId={}", sessionId, user.getId());
  }

  /**
   * 채팅방 접속 시 마지막 읽은 시간 업데이트
   */
  @Transactional
  public void updateLastReadAt(Long sessionId, CustomUser principal) {
    User user = getUserFromPrincipal(principal);
    sessionUserRepository.updateLastReadAt(sessionId, user.getId(), LocalDateTime.now());
    log.info("마지막 읽은 시간 업데이트: sessionId={}, userId={}", sessionId, user.getId());
  }


  /**
   * 로그인한 유저가 참여 중인 오프라인 채팅방 목록 조회
   */
  @Transactional(readOnly = true)
  public List<ChatRoomListResDto> getMyChatRoomList(CustomUser principal) {
    User user = getUserFromPrincipal(principal);
    log.info("=== 채팅방 목록 조회 시작: userId={} ===", user.getId());

    // 1. 유저가 참여 중인 오프라인 세션 목록 조회
    List<SessionUser> mySessionUsers = sessionUserRepository.findMyOfflineSessions(user.getId());
    log.info("참여 중인 세션 수: {}", mySessionUsers.size());

    // 2. 각 세션을 DTO로 변환
    List<ChatRoomListResDto> result = mySessionUsers.stream()
        .map(this::convertToChatRoomDto)
        .sorted((a, b) -> {
          // lastMessageTime 기준 내림차순 정렬 (최신 메시지가 맨 위)
          LocalDateTime timeA = a.getLastMessageTime();
          LocalDateTime timeB = b.getLastMessageTime();
          
          // null 처리: 메시지 없는 방은 맨 아래로
          if (timeA == null && timeB == null) return 0;
          if (timeA == null) return 1;  // a를 뒤로
          if (timeB == null) return -1; // b를 뒤로
          
          // 내림차순: 최신 시간이 먼저
          return timeB.compareTo(timeA);
        })
        .toList();

    log.info("=== 채팅방 목록 조회 완료: {} 개 ===", result.size());
    return result;
  }

  /**
   * SessionUser를 ChatRoomListResponseDto로 변환
   */
  private ChatRoomListResDto convertToChatRoomDto(SessionUser sessionUser) {
    MatchSession session = sessionUser.getMatchSession();
    Recruit recruit = session.getRecruit();

    // 참가자 정보 조회
    List<SessionUser> participants = sessionUserRepository.findActiveUsersBySessionId(
        session.getId());
    int currentParticipants = participants.size();
    int readyCount = (int) participants.stream()
        .filter(SessionUser::isReady)
        .count();

    // 최근 메시지 조회
    OfflineChatMessage lastMessage = chatMessageRepository.findTopBySessionIdOrderByCreatedAtDesc(
        session.getId());

    // 읽지 않은 메시지 개수 계산
    int unreadCount = 0;
    if (sessionUser.getLastReadAt() != null) {
      unreadCount = chatMessageRepository.countBySessionIdAndCreatedAtAfter(
          session.getId(),
          sessionUser.getLastReadAt()
      );
    } else {
      // lastReadAt이 null이면 모든 메시지가 읽지 않은 것
      unreadCount = chatMessageRepository.countBySessionId(session.getId());
    }

    // 소요시간 포맷팅
    String formattedDuration = formatDuration(session.getDuration());

    return ChatRoomListResDto.builder()
        .sessionId(session.getId())
        .title(recruit != null ? recruit.getTitle() : "제목 없음")
        .meetingPlace(recruit != null ? recruit.getMeetingPlace() : "장소 미정")
        .meetingAt(recruit != null ? recruit.getMeetingAt() : null)
        .targetDistance(recruit != null ? recruit.getTargetDistance() : session.getTargetDistance())
        .targetPace(recruit != null ? recruit.getTargetPace() : null)
        .formattedDuration(formattedDuration)
        .currentParticipants(currentParticipants)
        .maxParticipants(recruit != null ? recruit.getMaxParticipants() : currentParticipants)
        .readyCount(readyCount)
        .sessionStatus(session.getStatus().name())
        .lastMessageContent(lastMessage != null ? lastMessage.getContent() : null)
        .lastMessageSender(lastMessage != null ? lastMessage.getSenderName() : null)
        .lastMessageTime(lastMessage != null ? lastMessage.getCreatedAt() : null)
        .unreadCount(unreadCount)
        .build();
  }

  /**
   * 소요시간 포맷팅 (분 단위 → "X시간 Y분")
   */
  private String formatDuration(Integer durationMinutes) {
    if (durationMinutes == null || durationMinutes == 0) {
      return "미정";
    }

    int hours = durationMinutes / 60;
    int minutes = durationMinutes % 60;

    if (hours > 0 && minutes > 0) {
      return String.format("%d시간 %d분", hours, minutes);
    } else if (hours > 0) {
      return String.format("%d시간", hours);
    } else {
      return String.format("%d분", minutes);
    }
  }
}
