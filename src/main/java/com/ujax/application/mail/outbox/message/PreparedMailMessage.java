package com.ujax.application.mail.outbox.message;

import com.ujax.application.mail.template.RenderedMailContent;

public record PreparedMailMessage(
	String subject,
	RenderedMailContent content
) {
}
