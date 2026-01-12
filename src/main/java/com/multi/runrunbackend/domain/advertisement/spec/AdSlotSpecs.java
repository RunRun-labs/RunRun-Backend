package com.multi.runrunbackend.domain.advertisement.spec;

import com.multi.runrunbackend.domain.advertisement.constant.AdSlotStatus;
import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import com.multi.runrunbackend.domain.advertisement.entity.AdSlot;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdSlotSpecs
 * @since : 2026. 1. 11. Sunday
 */
public class AdSlotSpecs {

    private AdSlotSpecs() {
    }

    public static Specification<AdSlot> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String like = "%" + keyword.trim() + "%";
            return cb.like(root.get("name"), like);
        };
    }

    public static Specification<AdSlot> slotType(AdSlotType slotType) {
        return (root, query, cb) -> (slotType == null) ? cb.conjunction()
            : cb.equal(root.get("slotType"), slotType);
    }

    public static Specification<AdSlot> status(AdSlotStatus status) {
        return (root, query, cb) -> (status == null) ? cb.conjunction()
            : cb.equal(root.get("status"), status);
    }
}
