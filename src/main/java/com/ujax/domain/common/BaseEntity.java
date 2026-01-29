package com.ujax.domain.common;

import java.time.LocalDateTime;

import org.hibernate.annotations.FilterDef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@MappedSuperclass
@FilterDef(name = "softDeleteFilter")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	/**
	 * 소프트 삭제
	 */
	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
	}

	/**
	 * 소프트 삭제된 엔터티를 복원
	 */
	public void restore() {
		this.deletedAt = null;
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}
}
