package com.multi.runrunbackend.domain.util;

import com.multi.runrunbackend.domain.advertisement.entity.AdPlacement;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : WeightedRandom
 * @since : 2026. 1. 11. Sunday
 */
public final class WeightedRandom {

    private WeightedRandom() {
    }

    public static AdPlacement pick(List<AdPlacement> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        int total = 0;
        for (AdPlacement p : candidates) {
            int w = (p.getWeight() == null || p.getWeight() < 1) ? 1 : p.getWeight();
            total += w;
        }
        int r = ThreadLocalRandom.current().nextInt(total);
        int acc = 0;
        for (AdPlacement p : candidates) {
            int w = (p.getWeight() == null || p.getWeight() < 1) ? 1 : p.getWeight();
            acc += w;
            if (r < acc) {
                return p;
            }
        }
        return candidates.get(0);
    }
}

