package com.ujax.domain.board;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;

import com.ujax.domain.common.BaseEntity;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceMember;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "boards",
	indexes = {
		@Index(name = "idx_boards_workspace_created_id", columnList = "workspace_id,created_at,id")
	}
)
@Filter(
	name = "softDeleteFilter",
	condition = "deleted_at IS NULL"
)
@SQLDelete(sql = "UPDATE boards SET deleted_at = now() WHERE id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_member_id", nullable = false)
	private WorkspaceMember author;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private BoardType type;

	@Column(name = "is_pinned", nullable = false)
	private boolean pinned;

	@Column(nullable = false, length = 50)
	private String title;

	@Column(nullable = false, length = 2000)
	private String content;

	@Column(name = "view_count", nullable = false)
	private long viewCount;

	@Builder
	private Board(Workspace workspace, WorkspaceMember author, BoardType type, boolean pinned, String title, String content) {
		this.workspace = workspace;
		this.author = author;
		this.type = type;
		this.pinned = pinned;
		this.title = title;
		this.content = content;
		this.viewCount = 0L;
	}

	public static Board create(Workspace workspace, WorkspaceMember author, BoardType type, boolean pinned, String title, String content) {
		return Board.builder()
			.workspace(workspace)
			.author(author)
			.type(type)
			.pinned(pinned)
			.title(title)
			.content(content)
			.build();
	}

	public void update(BoardType type, String title, String content, Boolean pinned) {
		if (type != null) {
			this.type = type;
		}
		if (title != null) {
			this.title = title;
		}
		if (content != null) {
			this.content = content;
		}
		if (pinned != null) {
			this.pinned = pinned;
		}
	}

	public void updatePinned(boolean pinned) {
		this.pinned = pinned;
	}
}
