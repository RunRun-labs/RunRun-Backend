package com.multi.runrunbackend.domain.course.controller;

import com.multi.runrunbackend.common.response.ApiResponse;
import com.multi.runrunbackend.domain.auth.dto.CustomUser;
import com.multi.runrunbackend.domain.course.dto.req.CourseCreateReqDto;
import com.multi.runrunbackend.domain.course.dto.req.CourseListReqDto;
import com.multi.runrunbackend.domain.course.dto.req.CursorPage;
import com.multi.runrunbackend.domain.course.dto.req.RouteRequestDto;
import com.multi.runrunbackend.domain.course.dto.res.CourseCreateResDto;
import com.multi.runrunbackend.domain.course.dto.res.CourseListResDto;
import com.multi.runrunbackend.domain.course.dto.res.RouteResDto;
import com.multi.runrunbackend.domain.course.service.CourseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : CourseController
 * @since : 2025. 12. 18. Thursday
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/routes")
public class CourseController {


    private final CourseService courseService;

    @PostMapping("/oneway")
    public RouteResDto oneWay(@RequestBody RouteRequestDto req) {
        return courseService.oneWay(req);
    }

    @PostMapping("/round")
    public RouteResDto roundTrip(@RequestBody RouteRequestDto req) {
        return courseService.roundTrip(req);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CourseCreateResDto>> createCourse(
        @AuthenticationPrincipal CustomUser principal,
        @Valid @RequestPart("course") CourseCreateReqDto req,
        @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {

        CourseCreateResDto res = courseService.create(req, imageFile, principal);

        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPage<CourseListResDto>>> getCourses(
        @AuthenticationPrincipal CustomUser principal,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String registerType,
        @RequestParam(required = false) Boolean nearby,
        @RequestParam(required = false) Double lat,
        @RequestParam(required = false) Double lng,
        @RequestParam(required = false) Integer radiusM,
        @RequestParam(required = false) String distanceBucket,
        @RequestParam(required = false) String sortType,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) @Min(1) @Max(50) Integer size
    ) {
        CourseListReqDto req = CourseListReqDto.builder()
            .keyword(keyword)
            .registerType(parseRegisterType(registerType))
            .nearby(nearby)
            .lat(lat)
            .lng(lng)
            .radiusM(radiusM)
            .distanceBucket(distanceBucket)
            .sortType(parseSortType(sortType))
            .cursor(cursor)
            .size(size)
            .build();

        return ResponseEntity.ok(ApiResponse.success(courseService.getCourses(principal, req)));
    }

    private com.multi.runrunbackend.domain.course.constant.CourseRegisterType parseRegisterType(
        String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return com.multi.runrunbackend.domain.course.constant.CourseRegisterType.valueOf(
                v.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private com.multi.runrunbackend.domain.course.constant.CourseSortType parseSortType(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return com.multi.runrunbackend.domain.course.constant.CourseSortType.valueOf(
                v.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
