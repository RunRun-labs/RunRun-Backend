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

  @GetMapping("/match/select")
  public String matchSelectView() {
    return "match/match-select";
  }

  @GetMapping("/recruit")
  public String recruitListView() {
    return "recruit/recruit-list";
  }

  @GetMapping("/recruit/create")
  public String recruitCreateView() {
    return "recruit/recruit-create";
  }

  @GetMapping("/recruit/{id}")
  public String recruitDetailView() {
    return "recruit/recruit-detail";
  }

  @GetMapping("/recruit/{id}/update")
  public String recruitUpdateView() {
    return "recruit/recruit-update";
  }


  @GetMapping("/chat")
  public String chatList() {
    return "chat/chat-list";
  }

  @GetMapping("/chat/chat1")
  public String chat1() {
    return "chat/chat1";
  }
    @GetMapping("/myPage")
    public String myPageView() {
        return "mypage/mypage";
    }

    @GetMapping("/myPage/edit")
    public String myPageEdit() {
        return "mypage/mypage-edit";
    }

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
    public String courseDetailView(
        @PathVariable(name = "course_id") Long courseId,
        Model model
    ) {

        model.addAttribute("courseId", courseId);
        return "course/courseDetail";
    }

    @GetMapping("/courseUpdate/{course_id}")
    public String courseUpdateView(
        @PathVariable(name = "course_id") Long courseId,
        Model model
    ) {

        model.addAttribute("courseId", courseId);
        return "course/courseUpdate";
    }

}
