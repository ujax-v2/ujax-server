package com.ujax.infrastructure.web.workspace;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ujax.application.workspace.WorkspaceMemberProfileService;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileActivityResponse;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileResponse;
import com.ujax.global.dto.ApiResponse;
import com.ujax.infrastructure.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/members/me/profile")
@RequiredArgsConstructor
public class WorkspaceMemberProfileController {

	private final WorkspaceMemberProfileService workspaceMemberProfileService;

	@GetMapping
	public ApiResponse<WorkspaceMemberProfileResponse> getMyWorkspaceMemberProfile(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.success(workspaceMemberProfileService.getMyProfile(workspaceId, principal.getUserId()));
	}

	@GetMapping("/activity")
	public ApiResponse<WorkspaceMemberProfileActivityResponse> getMyWorkspaceMemberProfileActivity(
		@PathVariable Long workspaceId,
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam(required = false) Integer year
	) {
		return ApiResponse.success(
			workspaceMemberProfileService.getMyProfileActivity(workspaceId, principal.getUserId(), year)
		);
	}
}
