package com.ujax.application.problem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.problem.dto.response.WorkspaceProblemResponse;
import com.ujax.domain.problem.Problem;
import com.ujax.domain.problem.ProblemBox;
import com.ujax.domain.problem.ProblemBoxRepository;
import com.ujax.domain.problem.ProblemRepository;
import com.ujax.domain.problem.WorkspaceProblem;
import com.ujax.domain.problem.WorkspaceProblemRepository;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.global.dto.PageResponse;
import com.ujax.global.exception.ErrorCode;
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

	public PageResponse<WorkspaceProblemResponse> listWorkspaceProblems(Long workspaceId, Long problemBoxId,
		Long userId, int page, int size) {
		validateMember(workspaceId, userId);
		findProblemBoxByIdAndWorkspaceId(problemBoxId, workspaceId);

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
		findManagerOrOwner(workspaceId, userId);
		ProblemBox problemBox = findProblemBoxByIdAndWorkspaceId(problemBoxId, workspaceId);

		Problem problem = problemRepository.findById(request.problemId())
			.orElseThrow(() -> new NotFoundException(ErrorCode.PROBLEM_NOT_FOUND));

		if (workspaceProblemRepository.existsByProblemBox_IdAndProblem_Id(problemBoxId, problem.getId())) {
			throw new ConflictException(ErrorCode.DUPLICATE_WORKSPACE_PROBLEM);
		}

		WorkspaceProblem workspaceProblem = WorkspaceProblem.create(
			problemBox, problem, request.deadline(), request.scheduledAt());
		workspaceProblemRepository.save(workspaceProblem);

		return WorkspaceProblemResponse.from(workspaceProblem);
	}

	@Transactional
	public WorkspaceProblemResponse updateWorkspaceProblem(Long workspaceId, Long problemBoxId,
		Long workspaceProblemId, Long userId, UpdateWorkspaceProblemRequest request) {
		findManagerOrOwner(workspaceId, userId);
		ProblemBox problemBox = findProblemBoxByIdAndWorkspaceId(problemBoxId, workspaceId);

		WorkspaceProblem workspaceProblem = findWorkspaceProblem(workspaceProblemId, problemBoxId);
		workspaceProblem.update(request.deadline(), request.scheduledAt());

		return WorkspaceProblemResponse.from(workspaceProblem);
	}

	@Transactional
	public void deleteWorkspaceProblem(Long workspaceId, Long problemBoxId, Long workspaceProblemId, Long userId) {
		findManagerOrOwner(workspaceId, userId);
		ProblemBox problemBox = findProblemBoxByIdAndWorkspaceId(problemBoxId, workspaceId);

		WorkspaceProblem workspaceProblem = findWorkspaceProblem(workspaceProblemId, problemBoxId);
		workspaceProblemRepository.delete(workspaceProblem);
	}

	private void validateMember(Long workspaceId, Long userId) {
		workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
			.orElseThrow(() -> new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));
	}

	private void findManagerOrOwner(Long workspaceId, Long userId) {
		WorkspaceMember member = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
			.orElseThrow(() -> new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));
		member.validateManagerOrOwner();
	}

	private ProblemBox findProblemBoxByIdAndWorkspaceId(Long problemBoxId, Long workspaceId) {
		return problemBoxRepository.findByIdAndWorkspace_Id(problemBoxId, workspaceId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.PROBLEM_BOX_NOT_FOUND));
	}

	private WorkspaceProblem findWorkspaceProblem(Long workspaceProblemId, Long problemBoxId) {
		return workspaceProblemRepository.findByIdAndProblemBox_Id(workspaceProblemId, problemBoxId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.WORKSPACE_PROBLEM_NOT_FOUND));
	}
}
