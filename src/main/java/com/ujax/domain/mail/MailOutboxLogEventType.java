package com.ujax.domain.mail;

public enum MailOutboxLogEventType {

	ENQUEUED,
	PROCESSING_STARTED,
	RECOVERED,
	SENT,
	RETRY_SCHEDULED,
	FAILED

}
