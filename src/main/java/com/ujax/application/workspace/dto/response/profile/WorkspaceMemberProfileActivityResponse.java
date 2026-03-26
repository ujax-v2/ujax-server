package com.ujax.application.workspace.dto.response.profile;

import java.time.LocalDate;
import java.util.List;

public record WorkspaceMemberProfileActivityResponse(
	String mode,
	Integer year,
	LocalDate startDate,
	LocalDate endDate,
	List<WorkspaceMemberProfileActivityDayResponse> days
) {

	public static WorkspaceMemberProfileActivityResponse of(
		String mode,
		Integer year,
		LocalDate startDate,
		LocalDate endDate,
		List<WorkspaceMemberProfileActivityDayResponse> days
	) {
		return new WorkspaceMemberProfileActivityResponse(mode, year, startDate, endDate, days);
	}
}
