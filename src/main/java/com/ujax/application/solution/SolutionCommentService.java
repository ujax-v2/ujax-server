package com.ujax.application.solution;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.solution.dto.response.SolutionCommentResponse;
import com.ujax.domain.problem.WorkspaceProblem;
import com.ujax.domain.problem.WorkspaceProblemRepository;
import com.ujax.domain.solution.Solution;
import com.ujax.domain.solution.SolutionComment;
import com.ujax.domain.solution.SolutionCommentRepository;
import com.ujax.domain.solution.SolutionRepository;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.BadRequestException;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolutionCommentService {

	private static final int CONTENT_MAX = 255;

	private final SolutionCommentRepository solutionCommentRepository;
	private final SolutionRepository solutionRepository;
	private final WorkspaceProblemRepository workspaceProblemRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;

	public List<SolutionCommentResponse> getComments(
		Long workspaceId,
		Long problemBoxId,
		Long workspaceProblemId,
		Long workspaceMemberId,
		Long submissionId,
		Long userId
	) {
		WorkspaceMember actor = findWorkspaceMember(workspaceId, userId);
		WorkspaceProblem workspaceProblem = findWorkspaceProblem(workspaceId, problemBoxId, workspaceProblemId);
		Solution solution = findSubmission(workspaceProblem.getId(), workspaceMemberId, submissionId);

		return solutionCommentRepository.findBySolutionId(solution.getId()).stream()
			.map(comment -> SolutionCommentResponse.from(comment, actor.getId()))
			.toList();
	}

	@Transactional
	public SolutionCommentResponse createComment(
		Long workspaceId,
		Long problemBoxId,
		Long workspaceProblemId,
		Long workspaceMemberId,
		Long submissionId,
		Long userId,
		String content
	) {
		WorkspaceMember author = findWorkspaceMember(workspaceId, userId);
		WorkspaceProblem workspaceProblem = findWorkspaceProblem(workspaceId, problemBoxId, workspaceProblemId);
		Solution solution = findSubmission(workspaceProblem.getId(), workspaceMemberId, submissionId);

		validateContent(content);
		SolutionComment saved = solutionCommentRepository.save(SolutionComment.create(solution, author, content));
		return SolutionCommentResponse.from(saved, author.getId());
	}

	@Transactional
	public void deleteComment(
		Long workspaceId,
		Long problemBoxId,
		Long workspaceProblemId,
		Long workspaceMemberId,
		Long submissionId,
		Long commentId,
		Long userId
	) {
		WorkspaceMember actor = findWorkspaceMember(workspaceId, userId);
		WorkspaceProblem workspaceProblem = findWorkspaceProblem(workspaceId, problemBoxId, workspaceProblemId);
		Solution solution = findSubmission(workspaceProblem.getId(), workspaceMemberId, submissionId);
		SolutionComment comment = solutionCommentRepository.findByIdAndSolutionId(commentId, solution.getId())
			.orElseThrow(() -> new NotFoundException(ErrorCode.SOLUTION_COMMENT_NOT_FOUND));

		if (!comment.getAuthor().getId().equals(actor.getId())) {
			throw new ForbiddenException(ErrorCode.FORBIDDEN_RESOURCE, "작성자만 이 작업을 수행할 수 있습니다.");
		}

		solutionCommentRepository.delete(comment);
	}

	private WorkspaceMember findWorkspaceMember(Long workspaceId, Long userId) {
		return workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
			.orElseThrow(() -> new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));
	}

	private WorkspaceProblem findWorkspaceProblem(Long workspaceId, Long problemBoxId, Long workspaceProblemId) {
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

	private void validateContent(String content) {
		if (content == null || content.isBlank()) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT);
		}
		if (content.length() > CONTENT_MAX) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT);
		}
	}
}
