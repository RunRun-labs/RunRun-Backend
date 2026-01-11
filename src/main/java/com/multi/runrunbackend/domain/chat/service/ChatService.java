package com.multi.runrunbackend.domain.chat.service;

import com.multi.runrunbackend.common.exception.custom.BadRequestException;
import com.multi.runrunbackend.common.exception.custom.ForbiddenException;
import com.multi.runrunbackend.common.exception.custom.NotFoundException;
import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.chat.document.OfflineChatMessage;
import com.multi.runrunbackend.domain.chat.dto.ChatMessageDto;
import com.multi.runrunbackend.domain.chat.dto.req.StartRunningReqDto;
import com.multi.runrunbackend.domain.chat.dto.res.ChatRoomListResDto;
import com.multi.runrunbackend.domain.chat.dto.res.UnifiedChatRoomResDto;
import com.multi.runrunbackend.domain.chat.repository.OfflineChatMessageRepository;
import com.multi.runrunbackend.domain.crew.service.CrewChatService;
import com.multi.runrunbackend.domain.match.constant.SessionStatus;
import com.multi.runrunbackend.domain.match.constant.SessionType;
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
    private final CrewChatService crewChatService;


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
        // 현재 시간 설정
        LocalDateTime now = LocalDateTime.now();
        
        // MongoDB에 저장
        OfflineChatMessage chatMessage = OfflineChatMessage.builder()
            .sessionId(messageDto.getSessionId())
            .senderId(messageDto.getSenderId())
            .senderName(messageDto.getSenderName())
            .content(messageDto.getContent())
            .messageType(messageDto.getMessageType())
            .createdAt(now)
            .build();

        chatMessageRepository.save(chatMessage);

        // DTO에 시간 설정 후 Redis Pub/Sub으로 발행
        messageDto.setCreatedAt(now);
        String channel = "chat:" + messageDto.getSessionId();
        redisPublisher.publish(channel, messageDto);

        log.info("메시지 전송: sessionId={}, sender={}, createdAt={}", 
            messageDto.getSessionId(), messageDto.getSenderName(), now);
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
            return chatMessageRepository.findBySessionIdAndCreatedAtAfterOrderByCreatedAtAsc(
                sessionId,
                joinedAt);
        }
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }


    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSessionUsers(Long sessionId) {
        List<SessionUser> sessionUsers = sessionUserRepository.findActiveUsersBySessionId(
            sessionId);

        return sessionUsers.stream()
            .map(su -> {
                User user = su.getUser();
                Map<String, Object> map = new HashMap<>();
                map.put("userId", user.getId());
                map.put("name", user.getName());
                map.put("profileImage", user.getProfileImageUrl());  // ✅ 프로필 이미지 추가
                map.put("isReady", su.isReady());
                // ✅ 평균 페이스 추가 (User.averagePace를 "MM:SS" 형식으로 변환)
                map.put("averagePace", formatAveragePace(user.getAveragePace()));
                return map;
            })
            .collect(Collectors.toList());
    }

    /**
     * 평균 페이스를 MM:SS 형식으로 변환
     * @param averagePace 평균 페이스 (BigDecimal, 분/km)
     * @return "MM:SS" 형식의 문자열 (null이면 "-")
     */
    private String formatAveragePace(java.math.BigDecimal averagePace) {
        if (averagePace == null) {
            return "-";
        }

        // BigDecimal을 double로 변환
        double paceMinutes = averagePace.doubleValue();

        // 분과 초 분리
        int minutes = (int) paceMinutes;
        int seconds = (int) Math.round((paceMinutes - minutes) * 60);

        // 60초 처리 (예: 5.99분 -> 6:00으로)
        if (seconds >= 60) {
            minutes += 1;
            seconds = 0;
        }

        // MM:SS 형식으로 반환
        return String.format("%d:%02d", minutes, seconds);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSessionDetail(Long sessionId) {
        MatchSession session = matchSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

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
            .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_USER_NOT_FOUND));

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
            .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_USER_NOT_FOUND));

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
    public void startRunning(Long sessionId, CustomUser principal, StartRunningReqDto req) {
        User user = getUserFromPrincipal(principal);

        MatchSession session = matchSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

        Long hostId = getHostId(session);

        if (!user.getId().equals(hostId)) {
            throw new ForbiddenException(ErrorCode.NOT_SESSION_HOST);
        }
        if (session.getType() == SessionType.OFFLINE) {
            Map<String, Object> readyStatus = checkAllReady(sessionId);
            if (!(Boolean) readyStatus.get("allReady")) {
                throw new BadRequestException(ErrorCode.ALL_USERS_NOT_READY);
            }
        }

        if (session.getCourse() != null
            && session.getCourse().getStartLat() != null
            && session.getCourse().getStartLng() != null) {

            if (req == null || req.getLatitude() == null || req.getLongitude() == null) {
                throw new BadRequestException(ErrorCode.START_GATE_LOCATION_REQUIRED);
            }

            Double accuracy = req.getAccuracyM();
            if (accuracy == null || !Double.isFinite(accuracy) || accuracy > 30.0) {
                throw new BadRequestException(ErrorCode.START_GATE_LOW_ACCURACY);
            }

            double distM = haversineMeters(
                req.getLatitude(),
                req.getLongitude(),
                session.getCourse().getStartLat(),
                session.getCourse().getStartLng()
            );

            if (distM > 20.0) {
                throw new BadRequestException(ErrorCode.START_GATE_TOO_FAR);
            }
        }

        // 상태 변경
        matchSessionRepository.updateStatus(sessionId, SessionStatus.IN_PROGRESS);

        log.info("런닝 시작: sessionId={}, hostId={}", sessionId, user.getId());
    }

    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
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
     * 특정 세션의 모든 메시지 삭제 (디버깅용)
     */
    @Transactional
    public void deleteAllMessages(Long sessionId) {
        int deletedCount = chatMessageRepository.deleteBySessionId(sessionId);
        log.info("⭐ 메시지 삭제 완료: sessionId={}, 삭제된 메시지 수={}", sessionId, deletedCount);
    }


    /**
     * 로그인한 유저가 참여 중인 오프라인 채팅방 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomListResDto> getMyChatRoomList(CustomUser principal) {
        User user = getUserFromPrincipal(principal);
        log.info("=== 채팅방 목록 조회 시작: userId={} ===", user.getId());

        // 1. 유저가 참여 중인 오프라인 세션 목록 조회
        List<SessionUser> mySessionUsers = sessionUserRepository.findMyOfflineSessions(
            user.getId());
        log.info("참여 중인 세션 수: {}", mySessionUsers.size());

        // 2. 각 세션을 DTO로 변환
        List<ChatRoomListResDto> result = mySessionUsers.stream()
            .map(this::convertToChatRoomDto)
            .sorted((a, b) -> {
                // lastMessageTime 기준 내림차순 정렬 (최신 메시지가 맨 위)
                LocalDateTime timeA = a.getLastMessageTime();
                LocalDateTime timeB = b.getLastMessageTime();

                // null 처리: 메시지 없는 방은 맨 아래로
                if (timeA == null && timeB == null) {
                    return 0;
                }
                if (timeA == null) {
                    return 1;  // a를 뒤로
                }
                if (timeB == null) {
                    return -1; // b를 뒤로
                }

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

        log.info("⭐ DTO 변환 시작: sessionId={}, title={}",
            session.getId(),
            recruit != null ? recruit.getTitle() : "제목 없음");

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

        log.info("⭐ 최근 메시지 조회: sessionId={}, 메시지 존재={}, createdAt={}",
            session.getId(),
            lastMessage != null,
            lastMessage != null ? lastMessage.getCreatedAt() : "null");

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

        ChatRoomListResDto dto = ChatRoomListResDto.builder()
            .sessionId(session.getId())
            .title(recruit != null ? recruit.getTitle() : "제목 없음")
            .meetingPlace(recruit != null ? recruit.getMeetingPlace() : "장소 미정")
            .meetingAt(recruit != null ? recruit.getMeetingAt() : null)
            .targetDistance(
                recruit != null ? recruit.getTargetDistance() : session.getTargetDistance())
            .targetPace(recruit != null ? recruit.getTargetPace() : null)
            .formattedDuration(formattedDuration)
            .currentParticipants(currentParticipants)
            .maxParticipants(recruit != null ? recruit.getMaxParticipants() : currentParticipants)
            .readyCount(readyCount)
            .sessionStatus(session.getStatus().name())
            .lastMessageContent(lastMessage != null ? lastMessage.getContent() : null)
            .lastMessageSender(lastMessage != null ? lastMessage.getSenderName() : null)
            // ⭐ 메시지가 없으면 세션 생성 시간 사용 (또는 null)
            .lastMessageTime(
                lastMessage != null ? lastMessage.getCreatedAt() : session.getCreatedAt())
            .unreadCount(unreadCount)
            .build();

        log.info("⭐ DTO 변환 완료: sessionId={}, lastMessageTime={}",
            session.getId(), dto.getLastMessageTime());

        return dto;
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

    /**
     * 방장 ID 조회
     */
    private Long getHostId(MatchSession session) {
        Recruit recruit = session.getRecruit();
        if (recruit != null) {
            return recruit.getUser().getId();
        } else {
            List<SessionUser> users = sessionUserRepository.findActiveUsersBySessionId(
                session.getId());
            if (!users.isEmpty()) {
                return users.get(0).getUser().getId();
            }
        }
        throw new NotFoundException(ErrorCode.HOST_NOT_FOUND);
    }

    /**
     * 참여자 강퇴 (방장만 가능)
     */
    @Transactional
    public void kickUser(Long sessionId, Long targetUserId, CustomUser principal) {
        User currentUser = getUserFromPrincipal(principal);

        // 1. 세션 조회
        MatchSession session = matchSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_NOT_FOUND));

        // 2. 방장 권한 확인
        Long hostId = getHostId(session);
        if (!currentUser.getId().equals(hostId)) {
            throw new ForbiddenException(ErrorCode.NOT_SESSION_HOST);
        }

        // 3. 자기 자신 강퇴 방지
        if (currentUser.getId().equals(targetUserId)) {
            throw new BadRequestException(ErrorCode.CANNOT_KICK_SELF);
        }

        // 4. 강퇴 대상 사용자 조회
        User targetUser = userRepository.findById(targetUserId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // 5. 참여자인지 확인 및 이미 퇴장했는지 확인
        SessionUser sessionUser = sessionUserRepository.findBySessionIdAndUserId(sessionId,
                targetUserId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SESSION_USER_NOT_FOUND));

        if (sessionUser.getIsDeleted()) {
            throw new BadRequestException(ErrorCode.USER_ALREADY_LEFT);
        }

        // 6. 강퇴 처리 (soft delete)
        sessionUserRepository.softDeleteBySessionIdAndUserId(sessionId, targetUserId);

        // 7. 강퇴 시스템 메시지 전송
        ChatMessageDto kickMessage = ChatMessageDto.builder()
            .sessionId(sessionId)
            .senderName("SYSTEM")
            .content(targetUser.getName() + "님이 강퇴되었습니다.")
            .messageType("KICK")
            .senderId(targetUserId)  // 강퇴된 사용자 ID
            .build();

        sendMessage(kickMessage);

        log.info("사용자 강퇴: sessionId={}, hostId={}, kickedUserId={}",
            sessionId, currentUser.getId(), targetUserId);
    }

    /**
     * 통합 채팅방 목록 조회 (오프라인 + 크루)
     */
    @Transactional(readOnly = true)
    public List<UnifiedChatRoomResDto> getAllChatRooms(
        CustomUser principal) {

        // 1. 오프라인 채팅방 목록
        List<ChatRoomListResDto> offlineRooms = getMyChatRoomList(principal);

        // 2. 크루 채팅방 목록
        List<com.multi.runrunbackend.domain.crew.dto.res.CrewChatRoomListResDto> crewRooms =
            crewChatService.getMyChatRoomList(principal);

        // 3. 통합 DTO 변환
        List<com.multi.runrunbackend.domain.chat.dto.res.UnifiedChatRoomResDto> allRooms =
            new java.util.ArrayList<>();

        offlineRooms.forEach(offline ->
            allRooms.add(com.multi.runrunbackend.domain.chat.dto.res.UnifiedChatRoomResDto
                .fromOffline(offline)));

        crewRooms.forEach(crew ->
            allRooms.add(com.multi.runrunbackend.domain.chat.dto.res.UnifiedChatRoomResDto
                .fromCrew(crew)));

        // 4. 최근 메시지 시간 순 정렬
        allRooms.sort((a, b) -> {
            LocalDateTime timeA = a.getLastMessageTime();
            LocalDateTime timeB = b.getLastMessageTime();

            if (timeA == null && timeB == null) {
                return 0;
            }
            if (timeA == null) {
                return 1;
            }
            if (timeB == null) {
                return -1;
            }

            return timeB.compareTo(timeA);
        });

        log.info("통합 채팅방 조회: 오프라인={}, 크루={}, 총={}개",
            offlineRooms.size(), crewRooms.size(), allRooms.size());

        return allRooms;
    }
}
