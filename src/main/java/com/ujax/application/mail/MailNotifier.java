package com.ujax.application.mail;

import java.time.LocalDateTime;

public interface MailNotifier {

	void enqueueSignupVerification(String email, String code, LocalDateTime expiresAt);

	void enqueueWorkspaceInvite(String email, String workspaceName, Long workspaceId);
}
