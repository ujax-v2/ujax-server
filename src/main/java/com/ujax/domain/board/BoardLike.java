package com.ujax.domain.board;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;

import com.ujax.domain.common.BaseEntity;
import com.ujax.domain.workspace.WorkspaceMember;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "board_likes")
@Filter(
	name = "softDeleteFilter",
	condition = "deleted_at IS NULL"
)
@SQLDelete(sql = "UPDATE board_likes SET deleted_at = now() WHERE board_id = ? AND workspace_member_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardLike extends BaseEntity {

	@EmbeddedId
	private BoardLikeId id;

	@MapsId("boardId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "board_id", nullable = false)
	private Board board;

	@MapsId("workspaceMemberId")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_member_id", nullable = false)
	private WorkspaceMember workspaceMember;

	@Column(name = "is_deleted", nullable = false)
	private boolean deleted;

	@Builder
	private BoardLike(Board board, WorkspaceMember workspaceMember, boolean deleted) {
		this.id = new BoardLikeId(board.getId(), workspaceMember.getId());
		this.board = board;
		this.workspaceMember = workspaceMember;
		this.deleted = deleted;
	}

	public static BoardLike create(Board board, WorkspaceMember workspaceMember) {
		return BoardLike.builder()
			.board(board)
			.workspaceMember(workspaceMember)
			.deleted(false)
			.build();
	}

	public void updateDeleted(boolean deleted) {
		this.deleted = deleted;
	}
}
