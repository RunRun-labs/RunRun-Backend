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

}
