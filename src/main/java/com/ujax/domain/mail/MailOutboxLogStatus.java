package com.ujax.domain.mail;

public enum MailOutboxLogStatus {

	PENDING,
	PROCESSING,
	SENT,
	FAILED;

	public static MailOutboxLogStatus from(MailOutboxStatus status) {
		if (status == null) {
			return null;
		}
		return switch (status) {
			case PENDING -> PENDING;
			case PROCESSING -> PROCESSING;
		};
	}
}
