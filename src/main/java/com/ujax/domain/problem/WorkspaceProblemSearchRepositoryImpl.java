package com.ujax.domain.problem;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;

public class WorkspaceProblemSearchRepositoryImpl implements WorkspaceProblemSearchRepository {

	private static final char LIKE_ESCAPE_CHAR = '\\';

	private final JPAQueryFactory queryFactory;

	public WorkspaceProblemSearchRepositoryImpl(EntityManager entityManager) {
		this.queryFactory = new JPAQueryFactory(entityManager);
	}

	@Override
	public Page<WorkspaceProblem> searchByProblemBoxId(Long problemBoxId, String keyword, Pageable pageable) {
		QWorkspaceProblem workspaceProblem = QWorkspaceProblem.workspaceProblem;
		QProblem problem = QProblem.problem;

		BooleanBuilder predicate = new BooleanBuilder();
		predicate.and(workspaceProblem.problemBox.id.eq(problemBoxId));
		tokenizeKeyword(keyword).forEach(token -> predicate.and(matchesToken(problem, token)));

		List<WorkspaceProblem> content = queryFactory
			.selectFrom(workspaceProblem)
			.join(workspaceProblem.problem, problem).fetchJoin()
			.where(predicate)
			.orderBy(workspaceProblem.createdAt.desc(), workspaceProblem.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		Long total = queryFactory
			.select(workspaceProblem.count())
			.from(workspaceProblem)
			.join(workspaceProblem.problem, problem)
			.where(predicate)
			.fetchOne();

		return new PageImpl<>(content, pageable, total != null ? total : 0L);
	}

	private BooleanExpression matchesToken(QProblem problem, SearchToken token) {
		String pattern = "%" + escapeLike(token.value().toLowerCase(Locale.ROOT)) + "%";
		BooleanExpression titlePredicate = problem.title.lower().like(pattern, LIKE_ESCAPE_CHAR);

		if (!token.isNumeric()) {
			return titlePredicate;
		}

		return problem.problemNumber.stringValue().like(pattern, LIKE_ESCAPE_CHAR).or(titlePredicate);
	}

	private List<SearchToken> tokenizeKeyword(String keyword) {
		if (!StringUtils.hasText(keyword)) {
			return List.of();
		}

		return Arrays.stream(keyword.trim().split("\\s+"))
			.filter(StringUtils::hasText)
			.map(token -> new SearchToken(token, isNumericToken(token)))
			.toList();
	}

	private boolean isNumericToken(String token) {
		return token.chars().allMatch(Character::isDigit);
	}

	private String escapeLike(String token) {
		return token
			.replace("\\", "\\\\")
			.replace("%", "\\%")
			.replace("_", "\\_");
	}

	private record SearchToken(String value, boolean numeric) {

		private boolean isNumeric() {
			return numeric;
		}
	}
}
