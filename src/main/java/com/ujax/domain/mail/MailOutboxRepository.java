package com.ujax.domain.mail;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MailOutboxRepository extends JpaRepository<MailOutbox, Long> {

	@Query(
		value = """
			SELECT *
			FROM mail_outbox
			WHERE status = 'PENDING'
			  AND next_attempt_at <= :now
			ORDER BY next_attempt_at ASC, mail_outbox_id ASC
			LIMIT :limit
			FOR UPDATE SKIP LOCKED
			""",
		nativeQuery = true
	)
	List<MailOutbox> findDuePendingOutboxesForUpdate(@Param("now") LocalDateTime now, @Param("limit") int limit);

	List<MailOutbox> findAllByStatusAndUpdatedAtBefore(MailOutboxStatus status, LocalDateTime updatedAt);
}
