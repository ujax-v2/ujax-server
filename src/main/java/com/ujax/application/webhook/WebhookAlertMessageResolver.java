package com.ujax.application.webhook;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ujax.domain.problem.WorkspaceProblem;
import com.ujax.domain.problem.WorkspaceProblemRepository;
import com.ujax.domain.webhook.WebhookAlert;
import com.ujax.domain.workspace.WorkspaceRepository;

@Component
public class WebhookAlertMessageResolver {

	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceProblemRepository workspaceProblemRepository;
	private final String baseUrl;

	public WebhookAlertMessageResolver(
		WorkspaceRepository workspaceRepository,
		WorkspaceProblemRepository workspaceProblemRepository,
		@Value("${app.ujax.base-url:https://ujax.site}") String baseUrl
	) {
		this.workspaceRepository = workspaceRepository;
		this.workspaceProblemRepository = workspaceProblemRepository;
		this.baseUrl = baseUrl;
	}

	public WebhookAlertMessage resolve(WebhookAlert alert) {
		String workspaceName = workspaceRepository.findById(alert.getWorkspaceId())
			.map(workspace -> workspace.getName())
			.orElse("워크스페이스 #" + alert.getWorkspaceId());

		Optional<WorkspaceProblem> optionalWorkspaceProblem = workspaceProblemRepository.findById(
			alert.getWorkspaceProblemId()
		);
		if (optionalWorkspaceProblem.isEmpty()) {
			return new WebhookAlertMessage(
				alert.getWorkspaceProblemId(),
				alert.getWorkspaceId(),
				workspaceName,
				"문제 #" + alert.getWorkspaceProblemId(),
				null,
				alert.getScheduledAt(),
				buildWorkspaceLink(alert.getWorkspaceId())
			);
		}

		WorkspaceProblem workspaceProblem = optionalWorkspaceProblem.get();
		return new WebhookAlertMessage(
			alert.getWorkspaceProblemId(),
			alert.getWorkspaceId(),
			workspaceName,
			"%d. %s".formatted(
				workspaceProblem.getProblem().getProblemNumber(),
				workspaceProblem.getProblem().getTitle()
			),
			workspaceProblem.getDeadline(),
			alert.getScheduledAt(),
			buildWorkspaceProblemLink(
				alert.getWorkspaceId(),
				workspaceProblem.getProblem().getProblemNumber()
			)
		);
	}

	private String buildWorkspaceLink(Long workspaceId) {
		return "%s/ws/%d/dashboard".formatted(baseUrl, workspaceId);
	}

	private String buildWorkspaceProblemLink(Long workspaceId, int problemNumber) {
		return "%s/ws/%d/ide/%d".formatted(baseUrl, workspaceId, problemNumber);
	}
}
