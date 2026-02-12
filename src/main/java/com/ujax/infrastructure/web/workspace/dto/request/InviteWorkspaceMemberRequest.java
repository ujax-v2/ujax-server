package com.ujax.infrastructure.web.workspace.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteWorkspaceMemberRequest(
	@NotBlank @Email String email
) {
}
