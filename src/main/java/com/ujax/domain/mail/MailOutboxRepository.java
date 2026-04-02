package com.ujax.domain.mail;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailOutboxRepository extends JpaRepository<MailOutbox, Long> {

	List<MailOutbox> findAllByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
		MailOutboxStatus status,
		LocalDateTime nextAttemptAt,
		Pageable pageable
	);

	List<MailOutbox> findAllByStatusAndUpdatedAtBefore(MailOutboxStatus status, LocalDateTime updatedAt);
}
