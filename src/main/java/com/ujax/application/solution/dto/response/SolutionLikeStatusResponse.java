package com.ujax.application.solution.dto.response;

public record SolutionLikeStatusResponse(
	long likes,
	boolean isLiked
) {

	public static SolutionLikeStatusResponse of(long likes, boolean isLiked) {
		return new SolutionLikeStatusResponse(likes, isLiked);
	}
}
