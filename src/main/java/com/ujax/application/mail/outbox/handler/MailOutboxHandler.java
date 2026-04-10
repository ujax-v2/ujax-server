package com.ujax.application.mail.outbox.handler;

import com.ujax.application.mail.outbox.message.PreparedMailMessage;
import com.ujax.domain.mail.MailType;

public interface MailOutboxHandler {

	MailType mailType();

	PreparedMailMessage prepare(String payloadJson);
}
