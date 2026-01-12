package com.multi.runrunbackend.domain.advertisement.spec;

import com.multi.runrunbackend.domain.advertisement.entity.AdPlacement;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdPlacementSpecs
 * @since : 2026. 1. 11. Sunday
 */
public class AdPlacementSpecs {

    private AdPlacementSpecs() {
    }

    public static Specification<AdPlacement> slotId(Long slotId) {
        return (root, query, cb) -> (slotId == null) ? cb.conjunction()
            : cb.equal(root.get("slot").get("id"), slotId);
    }

    public static Specification<AdPlacement> adId(Long adId) {
        return (root, query, cb) -> (adId == null) ? cb.conjunction()
            : cb.equal(root.get("ad").get("id"), adId);
    }

    public static Specification<AdPlacement> active(Boolean isActive) {
        return (root, query, cb) -> (isActive == null) ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }
}
