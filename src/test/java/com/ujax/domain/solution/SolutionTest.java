package com.ujax.domain.solution;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ujax.domain.problem.Problem;
import com.ujax.domain.problem.ProblemBox;
import com.ujax.domain.problem.WorkspaceProblem;
import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRole;

class SolutionTest {

	@Test
	@DisplayName("풀이를 생성한다")
	void createSolution() {
		// given
		Workspace workspace = Workspace.create("워크스페이스", "소개");
		User user = User.createLocalUser("user@test.com", Password.ofEncoded("password"), "유저");
		WorkspaceMember member = WorkspaceMember.create(workspace, user, WorkspaceMemberRole.MEMBER);
		ProblemBox problemBox = ProblemBox.create(workspace, "문제집", "설명");
		Problem problem = Problem.create(1000, "A+B", "Bronze V", "2 초", "128 MB",
			null, null, null, null);
		WorkspaceProblem workspaceProblem = WorkspaceProblem.create(problemBox, problem, null, null);

		// when
		Solution solution = Solution.create(
			workspaceProblem, member, 12345L, "맞았습니다!!",
			"0 ms", "2020 KB", "Java 11", "123 B", "System.out.println(1+2);"
		);

		// then
		assertThat(solution).extracting(
			"submissionId", "status",
			"time", "memory", "programmingLanguage", "codeLength"
		).containsExactly(
			12345L, SolutionStatus.ACCEPTED,
			"0 ms", "2020 KB", ProgrammingLanguage.JAVA, "123 B"
		);
		assertThat(solution.getWorkspaceProblem()).isEqualTo(workspaceProblem);
		assertThat(solution.getWorkspaceMember()).isEqualTo(member);
		assertThat(solution.getCode()).isEqualTo("System.out.println(1+2);");
	}
}
