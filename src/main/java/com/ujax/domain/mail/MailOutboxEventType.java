package com.ujax.domain.mail;

public enum MailOutboxEventType {

	ENQUEUED,
	PROCESSING_STARTED,
	RECOVERED,
	SENT,
	RETRY_SCHEDULED,
	FAILED

}
