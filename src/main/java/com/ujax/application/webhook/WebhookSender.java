package com.ujax.application.webhook;

public interface WebhookSender {

	void send(String hookUrl, WebhookAlertMessage message);
}
