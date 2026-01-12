package com.multi.runrunbackend.domain.advertisement.dto.req.adplacement;

import com.multi.runrunbackend.domain.advertisement.constant.AdSlotType;
import java.time.LocalDateTime;

/**
 * @author : kyungsoo
 * @description : Please explain the class!!!
 * @filename : AdPlacementAdminListItemProjection
 * @since : 2026. 1. 11. Sunday
 */
public interface AdPlacementAdminListItemProjection {

    Long getPlacementId();

    Long getSlotId();

    String getSlotName();

    AdSlotType getSlotType();

    Long getAdId();

    String getAdName();


    Integer getWeight();

    LocalDateTime getStartAt();

    LocalDateTime getEndAt();

    Boolean getIsActive();

    Integer getTotalImpressions();

    Integer getTotalClicks();

}
