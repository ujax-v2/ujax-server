package com.ujax.domain.workspace;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;

import com.ujax.domain.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "workspaces")
@Filter(
	name = "softDeleteFilter",
	condition = "deleted_at IS NULL"
)
@SQLDelete(sql = "UPDATE workspaces SET deleted_at = now() WHERE id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workspace extends BaseEntity {

	public static final String DEFAULT_WORKSPACE_IMAGE_URL =
		"https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(length = 200)
	private String description;

	/** 알림 Hook URL (미설정 시 null) */
	private String hookUrl;

	@Column(length = 500)
	private String imageUrl;

	private Workspace(String name, String description, String imageUrl) {
		this.name = name;
		this.description = description;
		this.imageUrl = imageUrl != null ? imageUrl : DEFAULT_WORKSPACE_IMAGE_URL;
	}

	public static Workspace create(String name, String description) {
		return create(name, description, null);
	}

	public static Workspace create(String name, String description, String imageUrl) {
		return new Workspace(name, description, imageUrl);
	}

	public void update(String name, String description, String hookUrl, String imageUrl) {
		if (name != null) {
			this.name = name;
		}
		if (description != null) {
			this.description = description;
		}
		if (hookUrl != null) {
			this.hookUrl = hookUrl;
		}
		if (imageUrl != null) {
			this.imageUrl = imageUrl;
		}
	}
}
