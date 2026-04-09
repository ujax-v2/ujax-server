package com.ujax.application.mail.outbox;

import com.ujax.application.mail.template.RenderedMailContent;

public interface MailSender {

	void send(String recipientEmail, String subject, RenderedMailContent content);
}
