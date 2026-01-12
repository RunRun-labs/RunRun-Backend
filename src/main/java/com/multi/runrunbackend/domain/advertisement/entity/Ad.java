package com.multi.runrunbackend.domain.advertisement.entity;

import com.multi.runrunbackend.common.entitiy.BaseEntity;
import com.multi.runrunbackend.domain.advertisement.dto.req.ad.AdAdminCreateReqDto;
import com.multi.runrunbackend.domain.advertisement.dto.req.ad.AdAdminUpdateReqDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "ad"
)
@SQLRestriction("is_deleted = false")
public class Ad extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "redirect_url", length = 500)
    private String redirectUrl;


    public static Ad create(AdAdminCreateReqDto req, String uploadKey) {
        Ad ad = new Ad();
        ad.name = req.getName();
        ad.imageUrl = uploadKey;
        ad.redirectUrl = req.getRedirectUrl();
        return ad;

    }

    public void update(AdAdminUpdateReqDto dto, String imageUrl) {
        this.name = dto.getName();
        this.redirectUrl = dto.getRedirectUrl();
        this.imageUrl = imageUrl;
    }

    public void delete() {
        this.isDeleted = true;
    }
}