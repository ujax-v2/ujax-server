package com.ujax.application.solution;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.solution.dto.response.SolutionResponse;
import com.ujax.domain.problem.WorkspaceProblem;
import com.ujax.domain.problem.WorkspaceProblemRepository;
import com.ujax.domain.solution.Solution;
import com.ujax.domain.solution.SolutionRepository;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.global.dto.PageResponse;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.ConflictException;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.infrastructure.web.solution.dto.request.SolutionIngestRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolutionService {

	private final SolutionRepository solutionRepository;
	private final WorkspaceProblemRepository workspaceProblemRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	@Transactional
	public SolutionResponse ingest(SolutionIngestRequest request, Long userId) {
		WorkspaceProblem workspaceProblem = findWorkspaceProblemForIngest(request.workspaceProblemId());
		Long workspaceId = workspaceProblem.getProblemBox().getWorkspace().getId();
		WorkspaceMember workspaceMember = findWorkspaceMember(workspaceId, userId);

		if (solutionRepository.existsBySubmissionId(request.submissionId())) {
			throw new ConflictException(ErrorCode.DUPLICATE_SOLUTION);
		}

		Solution solution = Solution.create(
			workspaceProblem,
			workspaceMember,
			request.submissionId(),
			request.verdict(),
			request.time(),
			request.memory(),
			request.language(),
			request.codeLength(),
			request.code()
		);
		solutionRepository.save(solution);

		return SolutionResponse.from(solution);
	}

	public PageResponse<SolutionResponse> getSolutions(Long workspaceId, Long problemBoxId,
		Long workspaceProblemId, Long userId, int page, int size) {
		WorkspaceProblem workspaceProblem = findAccessibleWorkspaceProblem(
			workspaceId,
			problemBoxId,
			workspaceProblemId,
			userId
		);

		Page<Solution> result = solutionRepository.findByWorkspaceProblemId(
			workspaceProblem.getId(),
			PageRequest.of(page, size, Sort.by(
				Sort.Order.desc("createdAt"),
				Sort.Order.desc("id")
			))
		);

		return PageResponse.of(
			result.getContent().stream().map(SolutionResponse::from).toList(),
			result
		);
	}

	private WorkspaceProblem findWorkspaceProblemForIngest(Long workspaceProblemId) {
		return workspaceProblemRepository.findByIdWithProblemBoxAndWorkspace(workspaceProblemId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.WORKSPACE_PROBLEM_NOT_FOUND));
	}

	private WorkspaceProblem findAccessibleWorkspaceProblem(
		Long workspaceId,
		Long problemBoxId,
		Long workspaceProblemId,
		Long userId
	) {
		findWorkspaceMember(workspaceId, userId);
		return findWorkspaceProblem(workspaceId, workspaceProblemId, problemBoxId);
	}

	private WorkspaceMember findWorkspaceMember(Long workspaceId, Long userId) {
		return workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
			.orElseThrow(() -> new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));
	}

	private WorkspaceProblem findWorkspaceProblem(Long workspaceId, Long workspaceProblemId, Long problemBoxId) {
		return workspaceProblemRepository.findByIdAndProblemBox_IdAndProblemBox_Workspace_Id(
			workspaceProblemId,
			problemBoxId,
			workspaceId
		)
			.orElseThrow(() -> new NotFoundException(ErrorCode.WORKSPACE_PROBLEM_NOT_FOUND));
	}
}
