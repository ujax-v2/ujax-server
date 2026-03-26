package com.ujax.application.workspace.dto.response.profile;

import java.time.LocalDate;

public record WorkspaceMemberProfileActivityDayResponse(
	LocalDate date,
	long count
) {
}
