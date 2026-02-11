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

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(length = 200)
	private String description;

	/** MM 웹훅 URL (미설정 시 null) */
	private String mmWebhookUrl;

	private Workspace(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public static Workspace create(String name, String description) {
		return new Workspace(name, description);
	}

	public void update(String name, String description, String mmWebhookUrl) {
		if (name != null) {
			this.name = name;
		}
		if (description != null) {
			this.description = description;
		}
		if (mmWebhookUrl != null) {
			this.mmWebhookUrl = mmWebhookUrl;
		}
	}
}
