package com.ujax.domain.solution;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;

import com.ujax.domain.common.BaseEntity;
import com.ujax.domain.workspace.WorkspaceMember;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "solution_comments")
@Filter(
	name = "softDeleteFilter",
	condition = "deleted_at IS NULL"
)
@SQLDelete(sql = "UPDATE solution_comments SET deleted_at = now() WHERE id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SolutionComment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "solution_id", nullable = false)
	private Solution solution;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_member_id", nullable = false)
	private WorkspaceMember author;

	@Column(nullable = false, length = 255)
	private String content;

	@Builder
	private SolutionComment(Solution solution, WorkspaceMember author, String content) {
		this.solution = solution;
		this.author = author;
		this.content = content;
	}

	public static SolutionComment create(Solution solution, WorkspaceMember author, String content) {
		return SolutionComment.builder()
			.solution(solution)
			.author(author)
			.content(content)
			.build();
	}
}
