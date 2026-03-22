package com.ujax.application.solution.dto.response;

import java.time.LocalDateTime;

import com.ujax.domain.solution.SolutionComment;

public record SolutionCommentResponse(
	Long id,
	String authorName,
	String content,
	LocalDateTime createdAt,
	boolean isMyComment
) {

	public static SolutionCommentResponse from(SolutionComment comment, Long actorWorkspaceMemberId) {
		return new SolutionCommentResponse(
			comment.getId(),
			comment.getAuthor().getNickname(),
			comment.getContent(),
			comment.getCreatedAt(),
			comment.getAuthor().getId().equals(actorWorkspaceMemberId)
		);
	}
}
