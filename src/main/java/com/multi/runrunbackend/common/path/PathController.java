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
    public String crewEditPage() {
        return "crew/updateCrew";
    }
}
