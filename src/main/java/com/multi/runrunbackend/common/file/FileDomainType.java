package com.multi.runrunbackend.common.file;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileDomainType {

    PROFILE("profile"),
    COURSE_THUMBNAIL("course/thumbnail"),
    COURSE_IMAGE("course/image"),
    AD_IMAGE("ad"),
    CREW_IMAGE("crew"),
    PRODUCT_IMAGE("product"),
    CHALLENGE_IMAGE("challenge"),
    POINT_PRODUCT("point-products"),
    FEED_IMAGE("feed");

    private final String dir;
}


