package com.ujax.application.mail.outbox;

import com.ujax.application.mail.RenderedMailContent;

public record PreparedMailMessage(
	String subject,
	RenderedMailContent content
) {
}
