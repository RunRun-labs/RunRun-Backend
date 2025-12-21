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


  @GetMapping("/chat")
  public String chatList() {
    return "chat/chat-list";
  }

  @GetMapping("/chat/chat1")
  public String chat1() {
    return "chat/chat1";
  }
}
