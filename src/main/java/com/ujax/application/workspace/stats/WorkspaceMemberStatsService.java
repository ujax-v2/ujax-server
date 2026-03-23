package com.ujax.application.workspace.stats;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.workspace.stats.dto.WorkspaceDeadlineStats;
import com.ujax.application.workspace.stats.dto.WorkspaceDeadlineRateStat;
import com.ujax.domain.problem.WorkspaceProblemRepository;
import com.ujax.domain.solution.SolutionRepository;
import com.ujax.domain.solution.SolutionStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceMemberStatsService {

	private final SolutionRepository solutionRepository;
	private final WorkspaceProblemRepository workspaceProblemRepository;

	public Map<Long, Long> getSolvedCounts(Long workspaceId, LocalDateTime start, LocalDateTime end) {
		return toLongCountMap(solutionRepository.countSolvedProblemsByMemberBetween(
			workspaceId,
			SolutionStatus.ACCEPTED,
			start,
			end
		));
	}

	public WorkspaceDeadlineStats getDeadlineRateStats(Long workspaceId, LocalDateTime now) {
		long totalClosedDeadlineProblems = workspaceProblemRepository.countClosedDeadlineProblems(workspaceId, now);
		Map<Long, WorkspaceDeadlineRateStat> stats = new HashMap<>();

		for (Object[] row : solutionRepository.countOnTimeSolvedProblemsByMember(
			workspaceId,
			SolutionStatus.ACCEPTED,
			now
		)) {
			Long workspaceMemberId = (Long)row[0];
			long solvedBeforeDeadlineCount = (Long)row[1];
			stats.put(workspaceMemberId, WorkspaceDeadlineRateStat.of(
				solvedBeforeDeadlineCount,
				totalClosedDeadlineProblems
			));
		}

		return new WorkspaceDeadlineStats(totalClosedDeadlineProblems, stats);
	}

	private Map<Long, Long> toLongCountMap(List<Object[]> rows) {
		Map<Long, Long> counts = new HashMap<>();
		for (Object[] row : rows) {
			counts.put((Long)row[0], (Long)row[1]);
		}
		return counts;
	}
}
