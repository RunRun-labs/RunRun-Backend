package com.multi.runrunbackend.common.path;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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

}
