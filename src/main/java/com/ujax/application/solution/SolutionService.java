package com.ujax.application.solution;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.solution.dto.response.SolutionResponse;
import com.ujax.application.solution.dto.response.SolutionMemberSummaryResponse;
import com.ujax.domain.problem.WorkspaceProblem;
import com.ujax.domain.problem.WorkspaceProblemRepository;
import com.ujax.domain.solution.SolutionCommentRepository;
import com.ujax.domain.solution.Solution;
import com.ujax.domain.solution.SolutionLikeRepository;
import com.ujax.domain.solution.SolutionRepository;
import com.ujax.application.solution.dto.response.SolutionVersionResponse;
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

	private static final ZoneId ACTIVITY_ZONE = ZoneId.of("Asia/Seoul");

	private final SolutionRepository solutionRepository;
	private final SolutionLikeRepository solutionLikeRepository;
	private final SolutionCommentRepository solutionCommentRepository;
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
		workspaceMember.recordActivity(LocalDate.now(ACTIVITY_ZONE));
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

	public List<SolutionMemberSummaryResponse> getSolutionMembers(
		Long workspaceId,
		Long problemBoxId,
		Long workspaceProblemId,
		Long userId
	) {
		WorkspaceProblem workspaceProblem = findAccessibleWorkspaceProblem(
			workspaceId,
			problemBoxId,
			workspaceProblemId,
			userId
		);

		List<Solution> solutions = solutionRepository.findAllByWorkspaceProblemIdOrderByCreatedAtDescIdDesc(
			workspaceProblem.getId()
		);

		LinkedHashMap<Long, Solution> latestSolutionsByMember = new LinkedHashMap<>();
		LinkedHashMap<Long, Long> submissionCountsByMember = new LinkedHashMap<>();
		for (Solution solution : solutions) {
			Long workspaceMemberId = solution.getWorkspaceMember().getId();
			if (!latestSolutionsByMember.containsKey(workspaceMemberId)) {
				latestSolutionsByMember.put(workspaceMemberId, solution);
			}
			submissionCountsByMember.merge(workspaceMemberId, 1L, Long::sum);
		}
		if (latestSolutionsByMember.isEmpty()) {
			return List.of();
		}

		List<Long> latestSolutionIds = latestSolutionsByMember.values().stream()
			.map(Solution::getId)
			.toList();
		LinkedHashMap<Long, Long> likeCountsBySolution = toCountMap(
			solutionLikeRepository.countBySolutionIds(latestSolutionIds)
		);

		return latestSolutionsByMember.entrySet().stream()
			.map(entry -> SolutionMemberSummaryResponse.from(
				entry.getValue(),
				submissionCountsByMember.getOrDefault(entry.getKey(), 0L),
				likeCountsBySolution.getOrDefault(entry.getValue().getId(), 0L)
			))
			.toList();
	}

	public PageResponse<SolutionVersionResponse> getSolutionVersions(
		Long workspaceId,
		Long problemBoxId,
		Long workspaceProblemId,
		Long workspaceMemberId,
		Long userId,
		int page,
		int size
	) {
		WorkspaceMember me = findWorkspaceMember(workspaceId, userId);
		WorkspaceProblem workspaceProblem = findWorkspaceProblem(workspaceId, workspaceProblemId, problemBoxId);

		Page<Solution> result = solutionRepository.findByWorkspaceProblemIdAndWorkspaceMemberId(
			workspaceProblem.getId(),
			workspaceMemberId,
			PageRequest.of(page, size, Sort.by(
				Sort.Order.desc("createdAt"),
				Sort.Order.desc("id")
			))
		);

		if (result.isEmpty()) {
			return PageResponse.of(List.of(), result);
		}

		List<Long> solutionIds = result.getContent().stream()
			.map(Solution::getId)
			.toList();
		LinkedHashMap<Long, Long> likeCountsBySolution = toCountMap(solutionLikeRepository.countBySolutionIds(solutionIds));
		LinkedHashMap<Long, Long> commentCountsBySolution = toCountMap(solutionCommentRepository.countBySolutionIds(solutionIds));
		HashSet<Long> myLikedSolutionIds = new HashSet<>(
			solutionLikeRepository.findMyLikedSolutionIds(solutionIds, me.getId())
		);

		List<SolutionVersionResponse> content = result.getContent().stream()
			.map(solution -> SolutionVersionResponse.from(
				solution,
				likeCountsBySolution.getOrDefault(solution.getId(), 0L),
				myLikedSolutionIds.contains(solution.getId()),
				commentCountsBySolution.getOrDefault(solution.getId(), 0L)
			))
			.toList();

		return PageResponse.of(content, result);
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

	private LinkedHashMap<Long, Long> toCountMap(List<Object[]> rows) {
		LinkedHashMap<Long, Long> counts = new LinkedHashMap<>();
		for (Object[] row : rows) {
			counts.put((Long)row[0], (Long)row[1]);
		}
		return counts;
	}
}
