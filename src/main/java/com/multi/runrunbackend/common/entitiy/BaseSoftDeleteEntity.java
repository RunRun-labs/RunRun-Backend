package com.multi.runrunbackend.common.entitiy;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * @author : kyungsoo
 * @description : created_at, isDeleted만 있는 baseEntity
 * @filename : BaseSoftDeleteEntity
 * @since : 2025. 12. 17. Wednesday
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseSoftDeleteEntity extends BaseCreatedEntity {

    @Column(name = "is_deleted", nullable = false)
    protected Boolean isDeleted = false;

    public void delete() {
        this.isDeleted = true;
    }
}