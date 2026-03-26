package com.ujax.application.workspace;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileAccuracyResponse;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileActivityDayResponse;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileActivityResponse;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileAlgorithmStatResponse;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileLanguageStatResponse;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileMemberResponse;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileResponse;
import com.ujax.application.workspace.dto.response.profile.WorkspaceMemberProfileSummaryResponse;
import com.ujax.domain.solution.ProgrammingLanguage;
import com.ujax.domain.solution.SolutionRepository;
import com.ujax.domain.solution.SolutionStatus;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.BadRequestException;
import com.ujax.global.exception.common.ForbiddenException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceMemberProfileService {

	private static final ZoneId PROFILE_ZONE = ZoneId.of("Asia/Seoul");

	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final SolutionRepository solutionRepository;

	public WorkspaceMemberProfileResponse getMyProfile(Long workspaceId, Long userId) {
		WorkspaceMember member = validateMember(workspaceId, userId);
		long totalCount = solutionRepository.countByWorkspaceMemberId(member.getId());
		long acceptedCount = solutionRepository.countByWorkspaceMemberIdAndStatus(member.getId(), SolutionStatus.ACCEPTED);
		long solvedCount = solutionRepository.countSolvedProblemsByWorkspaceMemberId(member.getId(), SolutionStatus.ACCEPTED);
		List<WorkspaceMemberProfileLanguageStatResponse> languageStats = toLanguageStats(
			solutionRepository.countLanguageStatsByWorkspaceMemberId(member.getId()),
			totalCount
		);
		List<WorkspaceMemberProfileAlgorithmStatResponse> algorithmStats = toAlgorithmStats(
			solutionRepository.countAlgorithmStatsByWorkspaceMemberId(member.getId(), SolutionStatus.ACCEPTED),
			solvedCount
		);

		return WorkspaceMemberProfileResponse.of(
			WorkspaceMemberProfileMemberResponse.from(member),
			WorkspaceMemberProfileSummaryResponse.of(
				solvedCount,
				getCurrentStreak(member),
				languageStats.isEmpty() ? null : languageStats.getFirst().name(),
				algorithmStats.isEmpty() ? null : algorithmStats.getFirst().name()
			),
			WorkspaceMemberProfileAccuracyResponse.of(acceptedCount, totalCount),
			algorithmStats,
			languageStats
		);
	}

	public WorkspaceMemberProfileActivityResponse getMyProfileActivity(Long workspaceId, Long userId, Integer year) {
		WorkspaceMember member = validateMember(workspaceId, userId);
		LocalDate today = LocalDate.now(PROFILE_ZONE);
		LocalDate startDate;
		LocalDate endDate;
		String mode;
		Integer responseYear;

		if (year == null) {
			mode = "RECENT";
			responseYear = null;
			endDate = today;
			startDate = today.minusDays(364);
		} else {
			validateYear(year);
			mode = "YEAR";
			responseYear = year;
			startDate = LocalDate.of(year, 1, 1);
			endDate = year == today.getYear() ? today : LocalDate.of(year, 12, 31);
		}

		LocalDate joinedAt = member.getCreatedAt().toLocalDate();
		LocalDate queryStartDate = startDate.isBefore(joinedAt) ? joinedAt : startDate;
		List<WorkspaceMemberProfileActivityDayResponse> days = queryStartDate.isAfter(endDate)
			? List.of()
			: solutionRepository.countSolvedActivityByWorkspaceMemberIdBetween(
					member.getId(),
					SolutionStatus.ACCEPTED,
					queryStartDate.atStartOfDay(),
					endDate.atTime(23, 59, 59)
				).stream()
				.map(this::toActivityDay)
				.toList();

		return WorkspaceMemberProfileActivityResponse.of(mode, responseYear, startDate, endDate, days);
	}

	private WorkspaceMember validateMember(Long workspaceId, Long userId) {
		return workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, userId)
			.orElseThrow(() -> new ForbiddenException(ErrorCode.WORKSPACE_MEMBER_FORBIDDEN));
	}

	private void validateYear(Integer year) {
		int currentYear = LocalDate.now(PROFILE_ZONE).getYear();
		if (year < 2000 || year > currentYear) {
			throw new BadRequestException(ErrorCode.INVALID_PARAMETER);
		}
	}

	private int getCurrentStreak(WorkspaceMember member) {
		LocalDate today = LocalDate.now(PROFILE_ZONE);
		Set<LocalDate> activeDates = solutionRepository.countSolvedActivityByWorkspaceMemberIdBetween(
				member.getId(),
				SolutionStatus.ACCEPTED,
				member.getCreatedAt(),
				LocalDateTime.now(PROFILE_ZONE)
			).stream()
			.map(row -> toLocalDate(row[0]))
			.collect(Collectors.toSet());

		if (activeDates.isEmpty()) {
			return 0;
		}

		LocalDate cursor = today;
		if (!activeDates.contains(today)) {
			cursor = today.minusDays(1);
			if (!activeDates.contains(cursor)) {
				return 0;
			}
		}

		int streak = 0;
		while (activeDates.contains(cursor)) {
			streak++;
			cursor = cursor.minusDays(1);
		}
		return streak;
	}

	private List<WorkspaceMemberProfileLanguageStatResponse> toLanguageStats(List<Object[]> rows, long totalCount) {
		return rows.stream()
			.map(row -> WorkspaceMemberProfileLanguageStatResponse.of(
				((ProgrammingLanguage)row[0]).getText(),
				(Long)row[1],
				totalCount
			))
			.toList();
	}

	private List<WorkspaceMemberProfileAlgorithmStatResponse> toAlgorithmStats(List<Object[]> rows, long totalSolvedCount) {
		return rows.stream()
			.map(row -> WorkspaceMemberProfileAlgorithmStatResponse.of(
				(String)row[0],
				(Long)row[1],
				totalSolvedCount
			))
			.toList();
	}

	private WorkspaceMemberProfileActivityDayResponse toActivityDay(Object[] row) {
		return new WorkspaceMemberProfileActivityDayResponse(toLocalDate(row[0]), (Long)row[1]);
	}

	private LocalDate toLocalDate(Object value) {
		if (value instanceof LocalDate localDate) {
			return localDate;
		}
		if (value instanceof Date date) {
			return date.toLocalDate();
		}
		throw new IllegalArgumentException("지원하지 않는 날짜 값입니다: " + value);
	}
}
