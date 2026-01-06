package com.multi.runrunbackend.domain.chat.controller;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.chat.document.OfflineChatMessage;
import com.multi.runrunbackend.domain.chat.dto.req.StartRunningReqDto;
import com.multi.runrunbackend.domain.chat.dto.res.ChatRoomListResDto;
import com.multi.runrunbackend.domain.chat.service.ChatService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

  private final ChatService chatService;


  /**
   * 현재 로그인한 사용자 정보 GET
   */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(
      @AuthenticationPrincipal CustomUser principal) {

    if (principal == null) {
      return ResponseEntity.status(401)
          .body(ApiResponse.error(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    Map<String, Object> userInfo = chatService.getCurrentUserInfo(principal);
    return ResponseEntity.ok(ApiResponse.success("사용자 정보 조회 성공", userInfo));
  }


  /**
   * 내가 참여 중인 오프라인 채팅방 목록 조회 GET
   */
  @GetMapping("/rooms")
  public ResponseEntity<ApiResponse<List<ChatRoomListResDto>>> getMyChatRoomList(
      @AuthenticationPrincipal CustomUser principal) {

    if (principal == null) {
      return ResponseEntity.status(401)
          .body(ApiResponse.error(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    List<ChatRoomListResDto> chatRooms = chatService.getMyChatRoomList(principal);
    return ResponseEntity.ok(ApiResponse.success("채팅방 목록 조회 성공", chatRooms));
  }


  /**
   * 과거 메시지 조회 GET
   */
  @GetMapping("/{sessionId}/messages")
  public ResponseEntity<ApiResponse<List<OfflineChatMessage>>> getMessages(
      @PathVariable Long sessionId,
      @RequestParam(required = false) String joinedAt) {

    LocalDateTime joinedAtTime = null;
    if (joinedAt != null && !joinedAt.isEmpty()) {
      joinedAtTime = LocalDateTime.parse(joinedAt);
    }

    List<OfflineChatMessage> messages = chatService.getMessages(sessionId, joinedAtTime);
    return ResponseEntity.ok(ApiResponse.success("메시지 조회 성공", messages));
  }


  /**
   * 세션 참여자 목록 조회 GET
   */
  @GetMapping("/sessions/{sessionId}/users")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSessionUsers(
      @PathVariable Long sessionId) {
    List<Map<String, Object>> users = chatService.getSessionUsers(sessionId);
    return ResponseEntity.ok(ApiResponse.success("참여자 목록 조회 성공", users));
  }

  /**
   * 세션 상세 조회 GET
   */
  @GetMapping("/sessions/{sessionId}")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getSessionDetail(
      @PathVariable Long sessionId) {
    Map<String, Object> session = chatService.getSessionDetail(sessionId);
    return ResponseEntity.ok(ApiResponse.success("세션 상세 조회 성공", session));
  }

  /**
   * 유저의 세션 입장 시점 조회 (로그인 사용자) GET
   */
  @GetMapping("/sessions/{sessionId}/joined-at")
  public ResponseEntity<ApiResponse<String>> getJoinedAt(
      @PathVariable Long sessionId,
      @AuthenticationPrincipal CustomUser principal) {

    String joinedAt = chatService.getJoinedAt(sessionId, principal);
    return ResponseEntity.ok(ApiResponse.success("입장 시점 조회 성공", joinedAt));
  }

  /**
   * 세션 퇴장 (로그인 사용자) DELETE
   */
  @DeleteMapping("/sessions/{sessionId}/leave")
  public ResponseEntity<ApiResponse<Void>> leaveSession(
      @PathVariable Long sessionId,
      @AuthenticationPrincipal CustomUser principal) {

    try {
      chatService.leaveSession(sessionId, principal);
      return ResponseEntity.ok(ApiResponse.success("세션 퇴장 완료", null));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(ErrorCode.INVALID_REQUEST, e.getMessage()));
    }
  }

  /**
   * 준비완료 토글 (로그인 사용자) POST
   */
  @PostMapping("/sessions/{sessionId}/ready")
  public ResponseEntity<ApiResponse<Map<String, Object>>> toggleReady(
      @PathVariable Long sessionId,
      @AuthenticationPrincipal CustomUser principal) {

    try {
      Map<String, Object> result = chatService.toggleReady(sessionId, principal);
      return ResponseEntity.ok(ApiResponse.success("준비 상태 변경 완료", result));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(ErrorCode.INVALID_REQUEST, e.getMessage()));
    }
  }

  /**
   * 전체 참가자 준비완료 확인 GET
   */
  @GetMapping("/sessions/{sessionId}/all-ready")
  public ResponseEntity<ApiResponse<Map<String, Object>>> checkAllReady(
      @PathVariable Long sessionId) {
    Map<String, Object> result = chatService.checkAllReady(sessionId);
    return ResponseEntity.ok(ApiResponse.success("준비 상태 확인 완료", result));
  }

  /**
   * 런닝 시작 (방장만 가능, 모두 준비완료 시에만)
   */
  @PostMapping("/sessions/{sessionId}/start")
  public ResponseEntity<ApiResponse<Void>> startRunning(
      @PathVariable Long sessionId,
      @RequestBody(required = false) StartRunningReqDto req,
      @AuthenticationPrincipal CustomUser principal) {

    try {
      chatService.startRunning(sessionId, principal, req);
      return ResponseEntity.ok(ApiResponse.success("런닝이 시작되었습니다!", null));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(ErrorCode.INVALID_REQUEST, e.getMessage()));
    }
  }

  /**
   * 채팅방 접속 시 마지막 읽은 시간 업데이트 POST
   */
  @PostMapping("/sessions/{sessionId}/read")
  public ResponseEntity<ApiResponse<Void>> updateLastReadAt(
      @PathVariable Long sessionId,
      @AuthenticationPrincipal CustomUser principal) {

    chatService.updateLastReadAt(sessionId, principal);
    return ResponseEntity.ok(ApiResponse.success("읽음 시간 업데이트 완료", null));
  }

  /**
   * 참여자 강퇴 (방장만 가능)
   */
  @DeleteMapping("/sessions/{sessionId}/kick/{userId}")
  public ResponseEntity<ApiResponse<Void>> kickUser(
      @PathVariable Long sessionId,
      @PathVariable Long userId,
      @AuthenticationPrincipal CustomUser principal) {

    chatService.kickUser(sessionId, userId, principal);
    return ResponseEntity.ok(ApiResponse.success("사용자를 강퇴했습니다.", null));
  }


}
