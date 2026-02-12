package com.ujax.domain.board;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardLikeId implements Serializable {

	@Column(name = "board_id")
	private Long boardId;

	@Column(name = "workspace_member_id")
	private Long workspaceMemberId;

	public BoardLikeId(Long boardId, Long workspaceMemberId) {
		this.boardId = boardId;
		this.workspaceMemberId = workspaceMemberId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		BoardLikeId that = (BoardLikeId)o;
		return Objects.equals(boardId, that.boardId)
			&& Objects.equals(workspaceMemberId, that.workspaceMemberId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(boardId, workspaceMemberId);
	}
}
