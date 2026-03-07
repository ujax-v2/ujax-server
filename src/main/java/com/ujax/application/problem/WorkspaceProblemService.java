package com.ujax.application.problem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ujax.application.problem.dto.response.WorkspaceProblemResponse;
import com.ujax.application.webhook.WebhookAlertService;
import com.ujax.domain.problem.Problem;
import com.ujax.domain.problem.ProblemBox;
import com.ujax.domain.problem.ProblemBoxRepository;
import com.ujax.domain.problem.ProblemRepository;
import com.ujax.domain.problem.WorkspaceProblem;
import com.ujax.domain.problem.WorkspaceProblemRepository;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.global.dto.PageResponse;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.BusinessRuleViolationException;
import com.ujax.global.exception.common.ConflictException;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.infrastructure.web.problem.dto.request.CreateWorkspaceProblemRequest;
import com.ujax.infrastructure.web.problem.dto.request.UpdateWorkspaceProblemRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceProblemService {

	private final WorkspaceProblemRepository workspaceProblemRepository;
	private final ProblemBoxRepository problemBoxRepository;
	private final ProblemRepository problemRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WebhookAlertService webhookAlertService;

	public PageResponse<WorkspaceProblemResponse> listWorkspaceProblems(Long workspaceId, Long problemBoxId,
		Long userId, int page, int size) {
		findWorkspaceMember(workspaceId, userId);
		findProblemBox(problemBoxId, workspaceId);

		Page<WorkspaceProblem> result = workspaceProblemRepository.findByProblemBoxIdWithProblem(
			problemBoxId, PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));

		return PageResponse.of(
			result.getContent().stream().map(WorkspaceProblemResponse::from).toList(),
			result
		);
	}

	@Transactional
	public WorkspaceProblemResponse createWorkspaceProblem(Long workspaceId, Long problemBoxId, Long userId,
		CreateWorkspaceProblemRequest request) {
		WorkspaceMember member = findManagerOrOwner(workspaceId, userId);

		ProblemBox problemBox = findProblemBox(problemBoxId, workspaceId);
		Problem problem = problemRepository.findById(request.problemId())
			.orElseThrow(() -> new NotFoundException(ErrorCode.PROBLEM_NOT_FOUND));

		if (workspaceProblemRepository.existsByProblemBox_IdAndProblem_Id(problemBoxId, problem.getId())) {
			throw new ConflictException(ErrorCode.DUPLICATE_WORKSPACE_PROBLEM);
		}
		if (request.scheduledAt() != null) {
			validateWorkspaceHookUrl(member.getWorkspace());
		}

		WorkspaceProblem workspaceProblem = WorkspaceProblem.create(
			problemBox, problem, request.deadline(), request.scheduledAt());
		workspaceProblemRepository.save(workspaceProblem);

		if (workspaceProblem.getScheduledAt() != null) {
			webhookAlertService.reserveOrUpdate(
				workspaceProblem.getId(),
				workspaceId,
				workspaceProblem.getScheduledAt(),
				userId
			);
		}

		return WorkspaceProblemResponse.from(workspaceProblem);
	}

	@Transactional
	public WorkspaceProblemResponse updateWorkspaceProblem(Long workspaceId, Long problemBoxId,
		Long workspaceProblemId, Long userId, UpdateWorkspaceProblemRequest request) {
		WorkspaceMember member = findManagerOrOwner(workspaceId, userId);

		WorkspaceProblem workspaceProblem = findWorkspaceProblem(workspaceId, workspaceProblemId, problemBoxId);
		workspaceProblem.update(request.deadline(), request.scheduledAt());

		if (workspaceProblem.getScheduledAt() != null) {
			validateWorkspaceHookUrl(member.getWorkspace());
			webhookAlertService.reserveOrUpdate(
				workspaceProblem.getId(),
				workspaceId,
				workspaceProblem.getScheduledAt(),
				userId
			);
		} else {
			webhookAlertService.deactivate(workspaceProblem.getId(), userId);
		}

		return WorkspaceProblemResponse.from(workspaceProblem);
	}

	@Transactional
	public void deleteWorkspaceProblem(Long workspaceId, Long problemBoxId, Long workspaceProblemId, Long userId) {
		findManagerOrOwner(workspaceId, userId);

		WorkspaceProblem workspaceProblem = findWorkspaceProblem(workspaceId, workspaceProblemId, problemBoxId);
		workspaceProblemRepository.delete(workspaceProblem);
		webhookAlertService.cancel(workspaceProblemId, userId);
	}

	private WorkspaceMember findWorkspaceMember(Long workspaceId, Long userId) {
		return workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
			.orElseThrow(() -> new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));
	}

	private WorkspaceMember findManagerOrOwner(Long workspaceId, Long userId) {
		WorkspaceMember member = findWorkspaceMember(workspaceId, userId);
		member.validateManagerOrOwner();
		return member;
	}

	private ProblemBox findProblemBox(Long problemBoxId, Long workspaceId) {
		return problemBoxRepository.findByIdAndWorkspace_Id(problemBoxId, workspaceId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.PROBLEM_BOX_NOT_FOUND));
	}

	private WorkspaceProblem findWorkspaceProblem(Long workspaceId, Long workspaceProblemId, Long problemBoxId) {
		findProblemBox(problemBoxId, workspaceId);
		return workspaceProblemRepository.findByIdAndProblemBox_Id(workspaceProblemId, problemBoxId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.WORKSPACE_PROBLEM_NOT_FOUND));
	}

	private void validateWorkspaceHookUrl(Workspace workspace) {
		if (!StringUtils.hasText(workspace.getHookUrl())) {
			throw new BusinessRuleViolationException("scheduledAt 사용 시 workspace hookUrl이 필요합니다.");
		}
	}
}
