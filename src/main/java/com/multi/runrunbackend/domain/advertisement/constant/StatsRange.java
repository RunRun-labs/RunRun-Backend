package com.multi.runrunbackend.domain.advertisement.constant;

import java.time.LocalDate;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : StatsRange
 * @since : 2026. 1. 9. Friday
 */
public enum StatsRange {
    D7(7),
    D30(30),
    D90(90);

    private final int days;

    StatsRange(int days) {
        this.days = days;
    }

    public LocalDate fromDate() {
        return LocalDate.now().minusDays(days - 1L);
    }

    public LocalDate toDate() {
        return LocalDate.now();
    }
}
