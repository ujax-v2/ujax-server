package com.ujax.application.solution;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.solution.dto.response.SolutionLikeStatusResponse;
import com.ujax.domain.problem.WorkspaceProblem;
import com.ujax.domain.problem.WorkspaceProblemRepository;
import com.ujax.domain.solution.Solution;
import com.ujax.domain.solution.SolutionLike;
import com.ujax.domain.solution.SolutionLikeId;
import com.ujax.domain.solution.SolutionLikeRepository;
import com.ujax.domain.solution.SolutionRepository;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolutionLikeService {

	private final SolutionRepository solutionRepository;
	private final SolutionLikeRepository solutionLikeRepository;
	private final WorkspaceProblemRepository workspaceProblemRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	@Transactional
	public SolutionLikeStatusResponse like(
		Long workspaceId,
		Long problemBoxId,
		Long workspaceProblemId,
		Long workspaceMemberId,
		Long submissionId,
		Long userId
	) {
		WorkspaceMember me = findWorkspaceMember(workspaceId, userId);
		WorkspaceProblem workspaceProblem = findWorkspaceProblem(
			workspaceId,
			problemBoxId,
			workspaceProblemId
		);
		Solution solution = findSubmission(workspaceProblem.getId(), workspaceMemberId, submissionId);

		int updated = solutionLikeRepository.updateDeleted(solution.getId(), me.getId(), false);
		if (updated == 0) {
			try {
				solutionLikeRepository.saveAndFlush(SolutionLike.create(solution, me));
			} catch (DataIntegrityViolationException ex) {
				solutionLikeRepository.updateDeleted(solution.getId(), me.getId(), false);
			}
		}

		return getLikeStatus(solution.getId(), me.getId());
	}

	@Transactional
	public SolutionLikeStatusResponse unlike(
		Long workspaceId,
		Long problemBoxId,
		Long workspaceProblemId,
		Long workspaceMemberId,
		Long submissionId,
		Long userId
	) {
		WorkspaceMember me = findWorkspaceMember(workspaceId, userId);
		WorkspaceProblem workspaceProblem = findWorkspaceProblem(
			workspaceId,
			problemBoxId,
			workspaceProblemId
		);
		Solution solution = findSubmission(workspaceProblem.getId(), workspaceMemberId, submissionId);

		SolutionLikeId id = new SolutionLikeId(solution.getId(), me.getId());
		solutionLikeRepository.findById(id)
			.ifPresent(like -> like.updateDeleted(true));

		return getLikeStatus(solution.getId(), me.getId());
	}

	private SolutionLikeStatusResponse getLikeStatus(Long solutionId, Long workspaceMemberId) {
		long likeCount = extractSingleCount(solutionLikeRepository.countBySolutionIds(List.of(solutionId)));
		boolean isLiked = !solutionLikeRepository.findMyLikedSolutionIds(List.of(solutionId), workspaceMemberId).isEmpty();
		return SolutionLikeStatusResponse.of(likeCount, isLiked);
	}

	private WorkspaceMember findWorkspaceMember(Long workspaceId, Long userId) {
		return workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
			.orElseThrow(() -> new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));
	}

	private WorkspaceProblem findWorkspaceProblem(
		Long workspaceId,
		Long problemBoxId,
		Long workspaceProblemId
	) {
		return workspaceProblemRepository.findByIdAndProblemBox_IdAndProblemBox_Workspace_Id(
			workspaceProblemId,
			problemBoxId,
			workspaceId
		).orElseThrow(() -> new NotFoundException(ErrorCode.WORKSPACE_PROBLEM_NOT_FOUND));
	}

	private Solution findSubmission(Long workspaceProblemId, Long workspaceMemberId, Long submissionId) {
		return solutionRepository.findBySubmissionIdAndWorkspaceProblem_IdAndWorkspaceMember_Id(
			submissionId,
			workspaceProblemId,
			workspaceMemberId
		).orElseThrow(() -> new NotFoundException(ErrorCode.SOLUTION_NOT_FOUND));
	}

	private long extractSingleCount(List<Object[]> results) {
		if (results.isEmpty()) {
			return 0L;
		}
		return (Long)results.getFirst()[1];
	}
}
