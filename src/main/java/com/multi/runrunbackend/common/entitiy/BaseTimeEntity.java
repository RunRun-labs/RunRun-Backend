package com.multi.runrunbackend.common.entitiy;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * @author : kyungsoo
 * @description : created_at, updated_at만 있는 baseEntity
 * @filename : BaseTimeEntity
 * @since : 2025. 12. 17. Wednesday
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity extends BaseCreatedEntity {

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    protected LocalDateTime updatedAt;
}