package com.ujax.domain.mail;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MailOutboxLogRepository extends JpaRepository<MailOutboxLog, Long> {

	List<MailOutboxLog> findAllByMailOutboxIdOrderByCreatedAtAsc(Long mailOutboxId);
}
