package com.ujax.application.workspace;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.workspace.dto.response.dashboard.DashboardDeadlineProblemResponse;
import com.ujax.application.workspace.dto.response.dashboard.DashboardDeadlineRateRankingResponse;
import com.ujax.application.workspace.dto.response.dashboard.DashboardHotProblemResponse;
import com.ujax.application.workspace.dto.response.dashboard.DashboardNoticeResponse;
import com.ujax.application.workspace.dto.response.dashboard.DashboardRankingsResponse;
import com.ujax.application.workspace.dto.response.dashboard.DashboardSolvedRankingResponse;
import com.ujax.application.workspace.dto.response.dashboard.DashboardStreakRankingResponse;
import com.ujax.application.workspace.dto.response.dashboard.DashboardSummaryResponse;
import com.ujax.application.workspace.dto.response.dashboard.WorkspaceDashboardResponse;
import com.ujax.application.workspace.stats.dto.WorkspaceDeadlineStats;
import com.ujax.application.workspace.stats.WorkspaceMemberStatsService;
import com.ujax.application.workspace.stats.dto.WorkspaceDeadlineRateStat;
import com.ujax.domain.board.BoardRepository;
import com.ujax.domain.problem.WorkspaceProblemRepository;
import com.ujax.domain.solution.SolutionRepository;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.domain.workspace.WorkspaceRepository;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.ForbiddenException;
import com.ujax.global.exception.common.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceDashboardService {

	private static final ZoneId DASHBOARD_ZONE = ZoneId.of("Asia/Seoul");
	private static final int NOTICE_LIMIT = 3;
	private static final int UPCOMING_DEADLINE_LIMIT = 3;
	private static final int RANKING_LIMIT = 5;

	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final BoardRepository boardRepository;
	private final WorkspaceProblemRepository workspaceProblemRepository;
	private final SolutionRepository solutionRepository;
	private final WorkspaceMemberStatsService workspaceMemberStatsService;

	public WorkspaceDashboardResponse getDashboard(Long workspaceId, Long userId) {
		findWorkspaceById(workspaceId);
		validateMember(workspaceId, userId);

		LocalDate today = LocalDate.now(DASHBOARD_ZONE);
		LocalDateTime now = LocalDateTime.now(DASHBOARD_ZONE);
		LocalDateTime weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
		LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
		List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspace_Id(workspaceId);
		return WorkspaceDashboardResponse.of(
			findRecentNotices(workspaceId),
			findUpcomingDeadlines(workspaceId, now),
			createSummary(workspaceId, weekStart, now),
			createRankings(workspaceId, today, now, monthStart, members)
		);
	}

	private List<DashboardNoticeResponse> findRecentNotices(Long workspaceId) {
		return boardRepository.findDashboardNotices(workspaceId, PageRequest.of(0, NOTICE_LIMIT)).stream()
			.map(DashboardNoticeResponse::from)
			.toList();
	}

	private List<DashboardDeadlineProblemResponse> findUpcomingDeadlines(Long workspaceId, LocalDateTime now) {
		return workspaceProblemRepository.findUpcomingByWorkspaceId(
			workspaceId,
			now,
			PageRequest.of(0, UPCOMING_DEADLINE_LIMIT)
		).stream()
			.map(DashboardDeadlineProblemResponse::from)
			.toList();
	}

	private DashboardSummaryResponse createSummary(Long workspaceId, LocalDateTime weekStart, LocalDateTime now) {
		long weeklySubmissionCount = solutionRepository.countByWorkspaceIdAndCreatedAtBetween(
			workspaceId,
			weekStart,
			now
		);
		return DashboardSummaryResponse.of(
			weeklySubmissionCount,
			findHotProblem(workspaceId, weekStart, now)
		);
	}

	private DashboardRankingsResponse createRankings(
		Long workspaceId,
		LocalDate today,
		LocalDateTime now,
		LocalDateTime monthStart,
		List<WorkspaceMember> members
	) {
		Map<Long, Long> monthlySolvedCounts = workspaceMemberStatsService.getSolvedCounts(workspaceId, monthStart, now);
		WorkspaceDeadlineStats deadlineStats = workspaceMemberStatsService.getDeadlineRateStats(
			workspaceId,
			now
		);

		return DashboardRankingsResponse.of(
			buildMonthlySolvedRankings(members, monthlySolvedCounts),
			buildStreakRankings(members, today),
			buildDeadlineRateRankings(members, deadlineStats)
		);
	}

	private void findWorkspaceById(Long workspaceId) {
		workspaceRepository.findById(workspaceId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.WORKSPACE_NOT_FOUND));
	}

	private void validateMember(Long workspaceId, Long userId) {
		workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
			.orElseThrow(() -> new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));
	}

	private DashboardHotProblemResponse findHotProblem(Long workspaceId, LocalDateTime start, LocalDateTime end) {
		// 이번 주 제출 수가 가장 많은 문제를 고른다.
		// 동률이면 가장 최근 제출 시각이 더 늦은 문제, 그다음은 workspaceProblemId 오름차순이다.
		List<Object[]> hotProblemRows = solutionRepository.countByWorkspaceProblemBetween(workspaceId, start, end);
		if (hotProblemRows.isEmpty()) {
			return null;
		}

		Object[] hotProblemRow = hotProblemRows.getFirst();
		Long workspaceProblemId = (Long)hotProblemRow[0];
		long weeklySubmissionCount = (Long)hotProblemRow[1];

		return workspaceProblemRepository.findDashboardProblemById(workspaceProblemId)
			.map(problem -> DashboardHotProblemResponse.from(problem, weeklySubmissionCount))
			.orElse(null);
	}

	private List<DashboardSolvedRankingResponse> buildMonthlySolvedRankings(
		List<WorkspaceMember> members,
		Map<Long, Long> solvedCounts
	) {
		return members.stream()
			.map(member -> DashboardSolvedRankingResponse.from(member, solvedCounts.getOrDefault(member.getId(), 0L)))
			.sorted(Comparator.comparingLong(DashboardSolvedRankingResponse::solvedCount).reversed()
				.thenComparing(DashboardSolvedRankingResponse::nickname)
				.thenComparing(DashboardSolvedRankingResponse::workspaceMemberId))
			.limit(RANKING_LIMIT)
			.toList();
	}

	private List<DashboardStreakRankingResponse> buildStreakRankings(
		List<WorkspaceMember> members,
		LocalDate today
	) {
		return members.stream()
			.map(member -> DashboardStreakRankingResponse.from(member, member.getCurrentStreakDays(today)))
			.sorted(Comparator.comparingInt(DashboardStreakRankingResponse::streakDays).reversed()
				.thenComparing(DashboardStreakRankingResponse::nickname)
				.thenComparing(DashboardStreakRankingResponse::workspaceMemberId))
			.limit(RANKING_LIMIT)
			.toList();
	}

	private List<DashboardDeadlineRateRankingResponse> buildDeadlineRateRankings(
		List<WorkspaceMember> members,
		WorkspaceDeadlineStats deadlineStats
	) {
		Comparator<DashboardDeadlineRateRankingResponse> comparator = Comparator
			.comparingInt(DashboardDeadlineRateRankingResponse::ratePercent)
			.reversed()
			.thenComparing(Comparator.comparingLong(DashboardDeadlineRateRankingResponse::solvedBeforeDeadlineCount).reversed())
			.thenComparing(DashboardDeadlineRateRankingResponse::nickname)
			.thenComparing(DashboardDeadlineRateRankingResponse::workspaceMemberId);

		return members.stream()
			.map(member -> DashboardDeadlineRateRankingResponse.from(
				member,
				deadlineStats.memberStats().getOrDefault(
					member.getId(),
					WorkspaceDeadlineRateStat.empty(deadlineStats.totalDeadlineProblems())
				)
			))
			.sorted(comparator)
			.limit(RANKING_LIMIT)
			.toList();
	}
}
