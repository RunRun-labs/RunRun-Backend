package com.multi.runrunbackend.domain.crew.controller;

import com.multi.runrunbackend.common.exception.dto.ErrorCode;
import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.crew.document.CrewChatMessage;
import com.multi.runrunbackend.domain.crew.dto.req.CrewChatNoticeReqDto;
import com.multi.runrunbackend.domain.crew.dto.res.CrewChatNoticeResDto;
import com.multi.runrunbackend.domain.crew.dto.res.CrewChatRoomListResDto;
import com.multi.runrunbackend.domain.crew.service.CrewChatService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : changwoo
 * @description : 크루 채팅 REST API 컨트롤러
 * @filename : CrewChatRestController
 * @since : 2026-01-04
 */
@Slf4j
@RestController
@RequestMapping("/api/crew-chat")
@RequiredArgsConstructor
public class CrewChatRestController {

  private final CrewChatService crewChatService;

  /**
   * 현재 로그인한 사용자 정보 조회
   */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(
      @AuthenticationPrincipal CustomUser principal) {

    if (principal == null) {
      return ResponseEntity.status(401)
          .body(ApiResponse.error(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    Map<String, Object> userInfo = crewChatService.getCurrentUserInfo(principal);
    return ResponseEntity.ok(ApiResponse.success("사용자 정보 조회 성공", userInfo));
  }

  /**
   * 내가 참여 중인 크루 채팅방 목록 조회
   */
  @GetMapping("/rooms")
  public ResponseEntity<ApiResponse<List<CrewChatRoomListResDto>>> getMyChatRoomList(
      @AuthenticationPrincipal CustomUser principal) {

    if (principal == null) {
      return ResponseEntity.status(401)
          .body(ApiResponse.error(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    List<CrewChatRoomListResDto> chatRooms = crewChatService.getMyChatRoomList(principal);
    return ResponseEntity.ok(ApiResponse.success("크루 채팅방 목록 조회 성공", chatRooms));
  }

  /**
   * 과거 메시지 조회 (가입 시점 이후 메시지만 조회)
   */
  @GetMapping("/{roomId}/messages")
  public ResponseEntity<ApiResponse<List<CrewChatMessage>>> getMessages(
      @PathVariable Long roomId,
      @AuthenticationPrincipal CustomUser principal) {

    if (principal == null) {
      return ResponseEntity.status(401)
          .body(ApiResponse.error(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    List<CrewChatMessage> messages = crewChatService.getMessages(roomId, principal);
    return ResponseEntity.ok(ApiResponse.success("메시지 조회 성공", messages));
  }

  /**
   * 채팅방 참여자 목록 조회
   */
  @GetMapping("/rooms/{roomId}/users")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRoomUsers(
      @PathVariable Long roomId) {

    List<Map<String, Object>> users = crewChatService.getRoomUsers(roomId);
    return ResponseEntity.ok(ApiResponse.success("참여자 목록 조회 성공", users));
  }

  /**
   * 채팅방 상세 정보 조회
   */
  @GetMapping("/rooms/{roomId}")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getRoomDetail(
      @PathVariable Long roomId) {

    Map<String, Object> room = crewChatService.getRoomDetail(roomId);
    return ResponseEntity.ok(ApiResponse.success("채팅방 상세 조회 성공", room));
  }


  /**
   * 공지사항 작성
   */
  @PostMapping("/rooms/{roomId}/notices")
  public ResponseEntity<ApiResponse<CrewChatNoticeResDto>> createNotice(
      @PathVariable Long roomId,
      @RequestBody @Valid CrewChatNoticeReqDto reqDto,
      @AuthenticationPrincipal CustomUser principal) {

    if (principal == null) {
      return ResponseEntity.status(401)
          .body(ApiResponse.error(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    CrewChatNoticeResDto notice = crewChatService.createNotice(roomId, principal, reqDto);
    return ResponseEntity.ok(ApiResponse.success("공지사항 작성 성공", notice));
  }

  /**
   * 공지사항 목록 조회
   */
  @GetMapping("/rooms/{roomId}/notices")
  public ResponseEntity<ApiResponse<List<CrewChatNoticeResDto>>> getNotices(
      @PathVariable Long roomId) {

    List<CrewChatNoticeResDto> notices = crewChatService.getNotices(roomId);
    return ResponseEntity.ok(ApiResponse.success("공지사항 목록 조회 성공", notices));
  }

  /**
   * 공지사항 수정
   */
  @PutMapping("/notices/{noticeId}")
  public ResponseEntity<ApiResponse<CrewChatNoticeResDto>> updateNotice(
      @PathVariable Long noticeId,
      @RequestBody @Valid CrewChatNoticeReqDto reqDto,
      @AuthenticationPrincipal CustomUser principal) {

    if (principal == null) {
      return ResponseEntity.status(401)
          .body(ApiResponse.error(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    CrewChatNoticeResDto notice = crewChatService.updateNotice(noticeId, principal, reqDto);
    return ResponseEntity.ok(ApiResponse.success("공지사항 수정 성공", notice));
  }

  /**
   * 공지사항 삭제
   */
  @DeleteMapping("/notices/{noticeId}")
  public ResponseEntity<ApiResponse<Void>> deleteNotice(
      @PathVariable Long noticeId,
      @AuthenticationPrincipal CustomUser principal) {

    if (principal == null) {
      return ResponseEntity.status(401)
          .body(ApiResponse.error(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));
    }

    crewChatService.deleteNotice(noticeId, principal);
    return ResponseEntity.ok(ApiResponse.success("공지사항 삭제 성공", null));
  }

}
