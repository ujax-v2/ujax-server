package com.ujax.application.mail.outbox;

import com.ujax.domain.mail.MailType;

public interface MailOutboxHandler {

	MailType mailType();

	PreparedMailMessage prepare(String payloadJson);
}
