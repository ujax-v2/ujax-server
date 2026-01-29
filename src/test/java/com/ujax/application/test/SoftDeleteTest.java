package com.ujax.application.test;

import static org.assertj.core.api.Assertions.*;

import com.ujax.infrastructure.persistence.jpa.IncludeDeleted;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ActiveProfiles("test")
@SpringBootTest
@EnableJpaRepositories(considerNestedRepositories = true)
public class SoftDeleteTest {

	@Autowired
	TestRepo repo;

	@Autowired
	TestUseCase useCase;

	@AfterEach
	void tearDown() {
		repo.deleteAllInBatch();
	}

	@Test
	@DisplayName("delete를 호출하면 deleted_at이 설정된다. (softDelete)")
	void Delete() {
		//given
		TempTestUser user1 = new TempTestUser("사람1");

		repo.save(user1);
		repo.deleteById(user1.getId());

		//when
		TempTestUser deleted = useCase.findByIdIncludingDeleted(user1.getId()).orElseThrow();

		//then
		assertThat(deleted.getDeletedAt()).isNotNull();
		assertThat(deleted.getName()).isEqualTo("사람1");

	}

	@Test
	@DisplayName("기본 조회는 삭제된 행을 제외하고 조회한다.")
	void FindExceptDeleteRaws() {
		//given
		TempTestUser user1 = new TempTestUser("사람1");
		TempTestUser user2 = new TempTestUser("사람2");
		repo.saveAll(List.of(user1, user2));
		repo.deleteById(user1.getId());

		//when
		List<TempTestUser> active = useCase.findActive();

		//then
		assertThat(active).hasSize(1);
		assertThat(active.getFirst().getName()).isEqualTo("사람2");
	}

	@Test
	@DisplayName("@IncludeDeleted를 포함한 조회는 삭제된 행도 포함해서 조회한다.")
	void FindIncludeDeletedContainsDeleteRaws() {
		//given
		TempTestUser user1 = new TempTestUser("사람1");
		TempTestUser user2 = new TempTestUser("사람2");
		repo.saveAll(List.of(user1, user2));
		repo.deleteById(user1.getId());

		//when
		List<TempTestUser> includingDeleted = useCase.findAllIncludingDeleted();

		//then
		assertThat(includingDeleted).hasSize(2)
			.extracting(TempTestUser::getName)
			.containsExactlyInAnyOrder("사람1", "사람2");
	}

	@Entity
	@Getter
	@NoArgsConstructor
	@Table(name = "temp_test_user")
	@SQLDelete(sql = "UPDATE temp_test_user SET deleted_at = now() WHERE id = ?")
	@Filter(name = "softDeleteFilter", condition = "deleted_at IS NULL")
	static class TempTestUser {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		@Column(name = "deleted_at")
		private LocalDateTime deletedAt;

		public TempTestUser(String name) {
			this.name = name;
		}
	}

	interface TestRepo extends JpaRepository<TempTestUser, Long> {
	}

	@TestConfiguration
	static class TestConfig {
		@Bean
		TestUseCase testUseCase(TestRepo repo) {
			return new TestUseCase(repo);
		}
	}

	static class TestUseCase {
		private final TestRepo repo;

		TestUseCase(TestRepo repo) {
			this.repo = repo;
		}

		@Transactional(readOnly = true)
		public List<TempTestUser> findActive() {
			return repo.findAll();
		}

		@IncludeDeleted
		@Transactional(readOnly = true)
		public List<TempTestUser> findAllIncludingDeleted() {
			return repo.findAll();
		}

		@IncludeDeleted
		@Transactional(readOnly = true)
		public Optional<TempTestUser> findByIdIncludingDeleted(Long id) {
			return repo.findById(id);
		}
	}
}
