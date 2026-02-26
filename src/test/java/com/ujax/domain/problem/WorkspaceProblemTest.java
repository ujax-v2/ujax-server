package com.ujax.domain.problem;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ujax.domain.workspace.Workspace;

class WorkspaceProblemTest {

	private final Workspace workspace = Workspace.create("워크스페이스", "소개");
	private final ProblemBox problemBox = ProblemBox.create(workspace, "문제집", "설명");
	private final Problem problem = Problem.create(1000, "A+B", "Bronze V", "1초", "256MB",
		"설명", "입력", "출력", "https://boj.kr/1000");

	@Test
	@DisplayName("문제집 문제를 생성한다")
	void create() {
		// given
		LocalDateTime deadline = LocalDateTime.of(2026, 3, 1, 0, 0);
		LocalDateTime scheduledAt = LocalDateTime.of(2026, 2, 28, 0, 0);

		// when
		WorkspaceProblem wp = WorkspaceProblem.create(problemBox, problem, deadline, scheduledAt);

		// then
		assertThat(wp).extracting("deadline", "scheduledAt")
			.containsExactly(deadline, scheduledAt);
	}

	@Test
	@DisplayName("문제집 문제의 deadline과 scheduledAt을 수정한다")
	void update() {
		// given
		WorkspaceProblem wp = WorkspaceProblem.create(problemBox, problem,
			LocalDateTime.of(2026, 3, 1, 0, 0), null);
		LocalDateTime newDeadline = LocalDateTime.of(2026, 4, 1, 0, 0);
		LocalDateTime newScheduledAt = LocalDateTime.of(2026, 3, 15, 0, 0);

		// when
		wp.update(newDeadline, newScheduledAt);

		// then
		assertThat(wp).extracting("deadline", "scheduledAt")
			.containsExactly(newDeadline, newScheduledAt);
	}
}
