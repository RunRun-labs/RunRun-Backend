package com.multi.runrunbackend.common.path;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class PathController {


    @GetMapping("/login")
    public String loginView() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupView() {
        return "auth/signup";
    }

    @GetMapping("/test")
    public String testView() {
        return "test";
    }

    @GetMapping("/test2")
    public String test2View() {
        return "test2";
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
