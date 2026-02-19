package com.ujax.domain.problem;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.infrastructure.persistence.jpa.JpaAuditingConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class AlgorithmTagRepositoryTest {

	@Autowired
	private AlgorithmTagRepository algorithmTagRepository;

	@BeforeEach
	void setUp() {
		algorithmTagRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("이름으로 알고리즘 태그를 조회한다")
	void findByName() {
		// given
		algorithmTagRepository.save(AlgorithmTag.create("다이나믹 프로그래밍"));

		// when
		var found = algorithmTagRepository.findByName("다이나믹 프로그래밍");

		// then
		assertThat(found).isPresent();
		assertThat(found.get().getName()).isEqualTo("다이나믹 프로그래밍");
	}

	@Test
	@DisplayName("알고리즘 태그를 저장한다")
	void save() {
		// given
		AlgorithmTag tag = AlgorithmTag.create("그래프 탐색");

		// when
		AlgorithmTag saved = algorithmTagRepository.save(tag);

		// then
		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getName()).isEqualTo("그래프 탐색");
	}
}
