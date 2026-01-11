package com.multi.runrunbackend.common.path;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PathController {

    @GetMapping("/login")
    public String loginView() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupView() {
        return "auth/signup";
    }

    @GetMapping("/home")
    public String homeView() {
        return "home/home";
    }

    /* ===================== MATCH ===================== */

    @GetMapping("/match/select")
    public String matchSelectView() {
        return "match/match-select";
    }

  @GetMapping("/match/online")
  public String onlineMatchView() {
    return "match/online-match";
  }


  @GetMapping("/match/waiting")
  public String matchWaitingView() {
    return "match/match-waiting";
  }

  @GetMapping("/match/battle")
  public String matchBattleView() {
    return "match/match-battle";
  }

  @GetMapping("/match/result")
  public String matchResultView() {
    return "match/match-result";
  }

  @GetMapping("/match/ghost")
  public String ghostView() {
    return "match/ghost";
  }

    @GetMapping("/match/ghost-run")
    public String ghostRunView() {
        return "match/ghost-run";
    }

    @GetMapping("/match/ghost-result")
    public String ghostResultView() {
        return "match/ghost-result";
    }

    @GetMapping("/match/solo")
    public String soloView() {
        return "match/solo";
    }

  @GetMapping("/match/battleList")
  public String battleListView() {
    return "match/battle-list";
  }

  @GetMapping("/match/battleDetail/{sessionId}")
  public String battleDetailView(@PathVariable Long sessionId, Model model) {
    model.addAttribute("sessionId", sessionId);
    return "match/battle-detail";
  }

    /* ===================== RECRUIT ===================== */

    @GetMapping("/recruit")
    public String recruitListView() {
        return "recruit/recruit-list";
    }

  @GetMapping("/recruit/create")
  public String recruitCreateView() {
    return "recruit/recruit-create";
  }

  @GetMapping("/recruit/{id}")
  public String recruitDetailView(@PathVariable Long id, Model model) {
    model.addAttribute("recruitId", id);
    return "recruit/recruit-detail";
  }

  @GetMapping("/recruit/{id}/update")
  public String recruitUpdateView(@PathVariable Long id, Model model) {
    model.addAttribute("recruitId", id);
    return "recruit/recruit-update";
  }

    /* ===================== CREW ===================== */

    @GetMapping("/crews")
    public String crewListPage() {
        return "crew/crewList";
    }

    @GetMapping("/crews/new")
    public String crewCreatePage() {
        return "crew/createCrew";
    }

    @GetMapping("/crews/{crewId}")
    public String crewDetailPage() {
        return "crew/crewDetailList";
    }

    @GetMapping("/crews/{crewId}/edit")
    public String crewEditPage(@PathVariable Long crewId, Model model) {
        model.addAttribute("crewId", crewId);
        return "crew/updateCrew";
    }

  @GetMapping("/crews/{crewId}/join")
  public String crewJoinPage(@PathVariable Long crewId, Model model) {
    model.addAttribute("crewId", crewId);
    return "crew/crewJoin";
  }

  @GetMapping("/crews/{crewId}/join-requests")
  public String crewJoinRequestListPage(@PathVariable Long crewId, Model model) {
    model.addAttribute("crewId", crewId);
    return "crew/crewJoinRequestList";
  }

  @GetMapping("/crews/{crewId}/users")
  public String crewUserPage(@PathVariable Long crewId, Model model) {
    model.addAttribute("crewId", crewId);
    return "crew/crewUser";
  }

  @GetMapping("/crews/main")
  public String crewMain() {
    return "crew/crewMain";
  }

    @GetMapping("/crews/{crewId}/activities/create")
    public String crewActivityCreatePage(@PathVariable Long crewId, Model model) {
        model.addAttribute("crewId", crewId);
        return "crew/crewActivityCreate";
    }

    @GetMapping("/crews/{crewId}/activities/{activityId}/edit")
    public String crewActivityEditPage(@PathVariable Long crewId, @PathVariable Long activityId, Model model) {
        model.addAttribute("crewId", crewId);
        model.addAttribute("activityId", activityId);
        return "crew/crewActivityEdit";
    }

    @GetMapping("/membership")
    public String membership() {
        return "membership/membership";
    }

    /* ===================== PAYMENT / POINT ===================== */

    @GetMapping("/payment/pay")
    public String paymentPayView() {
        return "payment/pay";
    }

    @GetMapping("/payment/history")
    public String paymentHistoryView() {
        return "payment/history";
    }

    @GetMapping("/payment/success")
    public String paymentSuccessView() {
        return "payment/success";
    }

    @GetMapping("/payment/free-success")
    public String freePaymentSuccess() {
        return "payment/free-success";
    }

    @GetMapping("/payment/fail")
    public String paymentFailView() {
        return "payment/fail";
    }

    @GetMapping("/points")
    public String point() {
        return "point/point";
    }

    @GetMapping("/points/balance")
    public String pointBalance() {
        return "point/pointBalance";
    }

    @GetMapping("/points/history")
    public String pointHistory() {
        return "point/pointHistory";
    }

    @GetMapping("/points/shop")
    public String pointShop() {
        return "point/pointShop";
    }

    @GetMapping("/admin/points/products")
    public String pointProductAdminView() {
        return "point/pointAdmin";
    }

    /* ===================== CHAT ===================== */

    @GetMapping("/chat")
    public String chatList() {
        return "chat/chat-list";
    }

    @GetMapping("/chat/chat1")
    public String chat1() {
        return "chat/chat1";
    }

    @GetMapping("/chat/crew")
    public String crewChat() {
        return "chat/crew-chat";
    }

    /* ===================== NOTIFICATION ===================== */

    @GetMapping("/notification")
    public String notificationListView() {
        return "notification/notification-list";
    }
    /* ===================== MY PAGE ===================== */

    @GetMapping("/myPage")
    public String myPageView() {
        return "mypage/mypage";
    }

    @GetMapping("/myPage/edit")
    public String myPageEdit() {
        return "mypage/mypage-edit";
    }

    /* ===================== FEED ===================== */

    @GetMapping("/feed")
    public String feedView() {
        return "feed/feed";
    }

    @GetMapping("/feed/records")
    public String myRecordView() {
        return "feed/myrecord-list";
    }

    @GetMapping("/feed/post")
    public String feedPostCreateView() {
        return "feed/feed-create";
    }

    @GetMapping("/feed/update")
    public String feedUpdateView() {
        return "feed/feed-update";
    }

    /* ===================== CHALLENGE ===================== */

    @GetMapping("/challenge")
    public String challengeView() {
        return "challenge/challenge";
    }

  @GetMapping("/challenge/{id}")
  public String challengeDetailView(@PathVariable Long id, Model model) {
    model.addAttribute("challengeId", id);
    return "challenge/challenge-detail";
  }

  @GetMapping("/challenge/create")
  public String challengeCreateView() {
    return "challenge/challenge-create";
  }

  @GetMapping("/challenge/{id}/edit")
  public String challengeEditView(@PathVariable Long id, Model model) {
    model.addAttribute("challengeId", id);
    return "challenge/challenge-edit";
  }

  @GetMapping("/challenge/end")
  public String challengeEndView() {
    return "challenge/challenge-end";
  }

    /* ===================== COURSE ===================== */

    @GetMapping("/course_auto")
  public String testView() {
    return "courseAuto";
  }

    @GetMapping("/course_manual")
    public String test2View() {
        return "courseManual";
    }

    @GetMapping("/course")
    public String courseView() {
        return "course/courseList";
    }

    @GetMapping("/courseCreate")
    public String courseCreateView() {
        return "course/courseCreate";
    }

    @GetMapping("/courseDetail/{course_id}")
    public String courseDetailView(@PathVariable(name = "course_id") Long courseId, Model model) {
        model.addAttribute("courseId", courseId);
        return "course/courseDetail";
    }

    @GetMapping("/courseUpdate/{course_id}")
    public String courseUpdateView(@PathVariable(name = "course_id") Long courseId, Model model) {
        model.addAttribute("courseId", courseId);
        return "course/courseUpdate";
    }

    @GetMapping("/test/gps")
    public String gpsTestView() {
        return "test/gps-test";
    }

  @GetMapping("/tts-test")
  public String ttsTestView() {
    return "tts-test";
  }

    @GetMapping("/setting")
    public String settingView() {
        return "setting/setting";
    }

    @GetMapping("/setting/blocked-users")
    public String blockedUsersView() {
        return "setting/blocked-users";
    }

    @GetMapping("/terms/view")
    public String termsView() {
        return "terms/terms-detail";
    }

    @GetMapping("/profile/{userId}")
    public String userProfileView(@PathVariable Long userId, Model model) {
        model.addAttribute("userId", userId);
        return "user/user-profile";
    }

    @GetMapping("/friends/list")
    public String friendListView() {
        return "friend/friend-list";
    }


    @GetMapping("/admin/coupon/inquiry")
    public String adminCouponInquiryView() {
        return "admin/coupon-inquiry";
    }

    @GetMapping("/admin/coupon/create")
    public String adminCouponCreateView() {
        return "admin/coupon-create";
    }

    @GetMapping("/admin/coupon/update/{coupon_id}")
    public String adminCouponUpdateView(
            @PathVariable(name = "coupon_id") Long couponId,
            Model model
    ) {
        model.addAttribute("couponId", couponId);
        return "admin/coupon-update";
    }

    @GetMapping("/admin/coupon-role/inquiry")
    public String adminCouponRoleInquiryView() {
        return "admin/coupon-role-inquiry";
    }

    @GetMapping("/admin/coupon-role/create")
    public String adminCouponRoleCreateView() {
        return "admin/coupon-role-create";
    }

    @GetMapping("/admin/coupon-role/update/{coupon_role_id}")
    public String adminCouponRoleUpdateView(
            @PathVariable(name = "coupon_role_id") Long couponRoleId,
            Model model
    ) {
        model.addAttribute("couponRoleId", couponRoleId);
        return "admin/coupon-role-update";
    }

    @GetMapping("/admin/coupon/select")
    public String adminCouponSelectView() {
        return "admin/coupon-select";
    }

    @GetMapping("/coupon/my")
    public String myCouponsView() {
        return "coupon/my-coupons";
    }

    @GetMapping("/coupon/event")
    public String couponEventView() {
        return "coupon/coupon-event";
    }

    /* ===================== RUNNING ===================== */

    @GetMapping("/running/{sessionId}")
    public String runningView(@PathVariable Long sessionId, Model model) {
        model.addAttribute("sessionId", sessionId);
        return "running/running";
    }

    @GetMapping("/attendance-event")
    public String attendanceEventPage() {
        return "attendance/attendance-event";
    }

}
