package com.ujax.application.workspace;

import static org.assertj.core.api.Assertions.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.application.workspace.dto.response.dashboard.WorkspaceDashboardResponse;
import com.ujax.domain.board.Board;
import com.ujax.domain.board.BoardRepository;
import com.ujax.domain.board.BoardType;
import com.ujax.domain.problem.AlgorithmTag;
import com.ujax.domain.problem.AlgorithmTagRepository;
import com.ujax.domain.problem.Problem;
import com.ujax.domain.problem.ProblemBox;
import com.ujax.domain.problem.ProblemBoxRepository;
import com.ujax.domain.problem.ProblemRepository;
import com.ujax.domain.problem.WorkspaceProblem;
import com.ujax.domain.problem.WorkspaceProblemRepository;
import com.ujax.domain.solution.Solution;
import com.ujax.domain.solution.SolutionRepository;
import com.ujax.domain.solution.SolutionStatus;
import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.domain.workspace.Workspace;
import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRepository;
import com.ujax.domain.workspace.WorkspaceMemberRole;
import com.ujax.domain.workspace.WorkspaceRepository;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.ForbiddenException;

@SpringBootTest
@ActiveProfiles("test")
class WorkspaceDashboardServiceTest {

	private static final ZoneId TEST_ZONE = ZoneId.of("Asia/Seoul");

	@Autowired
	private WorkspaceDashboardService workspaceDashboardService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private BoardRepository boardRepository;

	@Autowired
	private SolutionRepository solutionRepository;

	@Autowired
	private WorkspaceProblemRepository workspaceProblemRepository;

	@Autowired
	private ProblemBoxRepository problemBoxRepository;

	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;

	@Autowired
	private WorkspaceRepository workspaceRepository;

	@Autowired
	private ProblemRepository problemRepository;

	@Autowired
	private AlgorithmTagRepository algorithmTagRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		boardRepository.deleteAllInBatch();
		solutionRepository.deleteAllInBatch();
		jdbcTemplate.update("DELETE FROM problem_algorithm");
		workspaceProblemRepository.deleteAllInBatch();
		problemBoxRepository.deleteAllInBatch();
		workspaceMemberRepository.deleteAllInBatch();
		workspaceRepository.deleteAllInBatch();
		problemRepository.deleteAllInBatch();
		algorithmTagRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("워크스페이스 대시보드 조회")
	class GetDashboard {

		@Test
		@DisplayName("최근 공지, 임박 문제, 요약 통계, 랭킹을 함께 조회한다")
		void getDashboard() {
			User aliceUser = createUser("alice@example.com", "Alice");
			User bobUser = createUser("bob@example.com", "Bob");
			User carolUser = createUser("carol@example.com", "Carol");

			Workspace workspace = workspaceRepository.save(Workspace.create("알고리즘 스터디", "소개"));
			WorkspaceMember alice = createMember(workspace, aliceUser, WorkspaceMemberRole.OWNER);
			WorkspaceMember bob = createMember(workspace, bobUser, WorkspaceMemberRole.MEMBER);
			WorkspaceMember carol = createMember(workspace, carolUser, WorkspaceMemberRole.MEMBER);

			ProblemBox mainBox = problemBoxRepository.save(ProblemBox.create(workspace, "메인 문제집", "메인"));
			ProblemBox archiveBox = problemBoxRepository.save(ProblemBox.create(workspace, "지난 마감", "기록"));

			LocalDateTime now = LocalDateTime.now(TEST_ZONE).withSecond(0).withNano(0);
			LocalDateTime weekStart = now.toLocalDate()
				.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
				.atStartOfDay();
			LocalDateTime previousMonthEnd = now.withDayOfMonth(1).minusDays(1).withHour(12).withMinute(0);

			WorkspaceProblem hotProblem = createWorkspaceProblem(
				mainBox,
				createProblem(1697, "숨바꼭질", "Silver 1", "BFS", "Graph"),
				now.plusDays(1)
			);
			WorkspaceProblem secondProblem = createWorkspaceProblem(
				mainBox,
				createProblem(11047, "동전 0", "Silver 4", "Greedy"),
				now.plusDays(2)
			);
			WorkspaceProblem thirdProblem = createWorkspaceProblem(
				mainBox,
				createProblem(2178, "미로 탐색", "Silver 1", "BFS"),
				now.plusDays(3)
			);
			WorkspaceProblem pastProblemOne = createWorkspaceProblem(
				archiveBox,
				createProblem(1000, "A+B", "Bronze 5", "Math"),
				previousMonthEnd.minusDays(5).withHour(18).withMinute(0)
			);
			WorkspaceProblem pastProblemTwo = createWorkspaceProblem(
				archiveBox,
				createProblem(1001, "A-B", "Bronze 5", "Math"),
				previousMonthEnd.minusDays(3).withHour(18).withMinute(0)
			);

			createBoard(workspace, alice, "예전 공지", true, now.minusDays(5));
			createBoard(workspace, bob, "가장 최신 공지", false, now.minusHours(1));
			createBoard(workspace, alice, "중간 공지", false, now.minusDays(1));
			createBoard(workspace, alice, "제외될 공지", false, now.minusDays(10));
			createGeneralBoard(workspace, alice, "자유글", now.minusMinutes(5));

			LocalDateTime hotAcceptedByAliceAt = now.minusMinutes(10);
			LocalDateTime secondAcceptedByAliceAt = now.minusMinutes(20);
			LocalDateTime hotAcceptedByBobAt = now.minusMinutes(30);
			LocalDateTime hotWrongByAliceAt = now.minusMinutes(40);
			LocalDateTime hotWrongByBobAt = now.minusMinutes(50);
			LocalDateTime hotYesterdayByAliceAt = now.minusDays(1).withHour(9).withMinute(0);
			LocalDateTime hotTwoDaysAgoByAliceAt = now.minusDays(2).withHour(9).withMinute(0);
			LocalDateTime hotYesterdayByBobAt = now.minusDays(1).withHour(11).withMinute(0);

			createSolution(hotProblem, alice, 10007L, SolutionStatus.WRONG_ANSWER, hotTwoDaysAgoByAliceAt);
			createSolution(hotProblem, alice, 10006L, SolutionStatus.WRONG_ANSWER, hotYesterdayByAliceAt);
			createSolution(hotProblem, bob, 10008L, SolutionStatus.WRONG_ANSWER, hotYesterdayByBobAt);
			createSolution(hotProblem, bob, 10005L, SolutionStatus.WRONG_ANSWER, hotWrongByBobAt);
			createSolution(hotProblem, alice, 10004L, SolutionStatus.WRONG_ANSWER, hotWrongByAliceAt);
			createSolution(hotProblem, bob, 10003L, SolutionStatus.ACCEPTED, hotAcceptedByBobAt);
			createSolution(secondProblem, alice, 10002L, SolutionStatus.ACCEPTED, secondAcceptedByAliceAt);
			createSolution(hotProblem, alice, 10001L, SolutionStatus.ACCEPTED, hotAcceptedByAliceAt);

			createSolution(pastProblemOne, alice, 20001L, SolutionStatus.ACCEPTED, previousMonthEnd.minusDays(6).withHour(10).withMinute(0));
			createSolution(pastProblemTwo, alice, 20002L, SolutionStatus.ACCEPTED, previousMonthEnd.minusDays(4).withHour(10).withMinute(0));
			createSolution(pastProblemOne, bob, 20003L, SolutionStatus.ACCEPTED, previousMonthEnd.minusDays(6).withHour(12).withMinute(0));
			createSolution(pastProblemTwo, bob, 20004L, SolutionStatus.ACCEPTED, previousMonthEnd.minusDays(2).withHour(10).withMinute(0));

			long expectedWeeklySubmissionCount = List.of(
				hotAcceptedByAliceAt,
				secondAcceptedByAliceAt,
				hotAcceptedByBobAt,
				hotWrongByAliceAt,
				hotWrongByBobAt,
				hotYesterdayByAliceAt,
				hotTwoDaysAgoByAliceAt,
				hotYesterdayByBobAt
			).stream()
				.filter(timestamp -> !timestamp.isBefore(weekStart))
				.count();
			long expectedHotProblemSubmissionCount = List.of(
				hotAcceptedByAliceAt,
				hotAcceptedByBobAt,
				hotWrongByAliceAt,
				hotWrongByBobAt,
				hotYesterdayByAliceAt,
				hotTwoDaysAgoByAliceAt,
				hotYesterdayByBobAt
			).stream()
				.filter(timestamp -> !timestamp.isBefore(weekStart))
				.count();

			WorkspaceDashboardResponse response = workspaceDashboardService.getDashboard(workspace.getId(), aliceUser.getId());

			assertThat(response.recentNotices()).extracting("title")
				.containsExactly("예전 공지", "가장 최신 공지", "중간 공지");
			assertThat(response.upcomingDeadlines()).extracting("title")
				.containsExactly("숨바꼭질", "동전 0", "미로 탐색");
			assertThat(response.upcomingDeadlines().getFirst().algorithmTags()).containsExactly("BFS", "Graph");

			assertThat(response.summary().weeklySubmissionCount()).isEqualTo(expectedWeeklySubmissionCount);
			assertThat(response.summary().hotProblem()).isNotNull();
			assertThat(response.summary().hotProblem().title()).isEqualTo("숨바꼭질");
			assertThat(response.summary().hotProblem().weeklySubmissionCount()).isEqualTo(expectedHotProblemSubmissionCount);

			assertThat(response.rankings().monthlySolved()).extracting("nickname", "solvedCount")
				.containsExactly(
					tuple("Alice", 2L),
					tuple("Bob", 1L),
					tuple("Carol", 0L)
				);
			assertThat(response.rankings().streak()).extracting("nickname", "streakDays")
				.containsExactly(
					tuple("Alice", 3),
					tuple("Bob", 2),
					tuple("Carol", 0)
				);
			assertThat(response.rankings().deadlineRate()).extracting("nickname", "ratePercent")
				.containsExactly(
					tuple("Alice", 100),
					tuple("Bob", 50),
					tuple("Carol", 0)
				);
		}

		@Test
		@DisplayName("워크스페이스 멤버가 아니면 조회할 수 없다")
		void getDashboardForbidden() {
			User owner = createUser("owner@example.com", "Owner");
			User outsider = createUser("outsider@example.com", "Outsider");
			Workspace workspace = workspaceRepository.save(Workspace.create("워크스페이스", "소개"));
			createMember(workspace, owner, WorkspaceMemberRole.OWNER);

			assertThatThrownBy(() -> workspaceDashboardService.getDashboard(workspace.getId(), outsider.getId()))
				.isInstanceOf(ForbiddenException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.WORKSPACE_MEMBER_FORBIDDEN);
		}
	}

	private User createUser(String email, String name) {
		return userRepository.save(User.createLocalUser(email, Password.ofEncoded("password"), name));
	}

	private WorkspaceMember createMember(Workspace workspace, User user, WorkspaceMemberRole role) {
		return workspaceMemberRepository.save(WorkspaceMember.create(workspace, user, role));
	}

	private Problem createProblem(int problemNumber, String title, String tier, String... tagNames) {
		Problem problem = Problem.create(problemNumber, title, tier, "1초", "256MB", null, null, null, null);
		for (String tagName : tagNames) {
			AlgorithmTag tag = algorithmTagRepository.findByName(tagName)
				.orElseGet(() -> algorithmTagRepository.save(AlgorithmTag.create(tagName)));
			problem.addAlgorithmTag(tag);
		}
		return problemRepository.save(problem);
	}

	private WorkspaceProblem createWorkspaceProblem(ProblemBox problemBox, Problem problem, LocalDateTime deadline) {
		return workspaceProblemRepository.save(WorkspaceProblem.create(problemBox, problem, deadline, null));
	}

	private Board createBoard(
		Workspace workspace,
		WorkspaceMember author,
		String title,
		boolean pinned,
		LocalDateTime createdAt
	) {
		Board board = boardRepository.save(Board.create(workspace, author, BoardType.NOTICE, pinned, title, "내용"));
		updateCreatedAt("boards", board.getId(), createdAt);
		return board;
	}

	private Board createGeneralBoard(
		Workspace workspace,
		WorkspaceMember author,
		String title,
		LocalDateTime createdAt
	) {
		Board board = boardRepository.save(Board.create(workspace, author, BoardType.FREE, false, title, "자유글"));
		updateCreatedAt("boards", board.getId(), createdAt);
		return board;
	}

	private Solution createSolution(
		WorkspaceProblem workspaceProblem,
		WorkspaceMember member,
		Long submissionId,
		SolutionStatus status,
		LocalDateTime createdAt
	) {
		Solution solution = solutionRepository.save(Solution.create(
			workspaceProblem,
			member,
			submissionId,
			status.getText(),
			null,
			null,
			"Java 21",
			null,
			null
		));
		member.recordActivity(createdAt.toLocalDate());
		workspaceMemberRepository.save(member);
		updateCreatedAt("solution", solution.getId(), createdAt);
		return solution;
	}

	private void updateCreatedAt(String tableName, Long id, LocalDateTime createdAt) {
		Timestamp timestamp = Timestamp.valueOf(createdAt);
		jdbcTemplate.update(
			"UPDATE " + tableName + " SET created_at = ?, updated_at = ? WHERE id = ?",
			timestamp,
			timestamp,
			id
		);
	}
}
