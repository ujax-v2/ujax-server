package com.ujax.application.workspace;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class  WorkspaceInviteMailerTest {

	@Mock
	private JavaMailSender mailSender;

	private WorkspaceInviteMailer workspaceInviteMailer;

	@BeforeEach
	void setUp() {
		workspaceInviteMailer = new WorkspaceInviteMailer(
			mailSender,
			"https://ujax.site",
			"noreply@ujax.site"
		);
	}

	@Test
	@DisplayName("워크스페이스 초대 메일을 발송한다")
	void sendInvitation() {
		// given
		String email = "invite@example.com";
		String workspaceName = "워크스페이스";
		Long workspaceId = 10L;

		// when
		workspaceInviteMailer.sendInvitation(email, workspaceName, workspaceId);

		// then
		ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(captor.capture());
		SimpleMailMessage message = captor.getValue();

		assertThat(message).extracting("from", "subject")
			.containsExactly("noreply@ujax.site", "[UJAX] 워크스페이스 초대");
		assertThat(message.getTo()).containsExactly(email);
		assertThat(message.getText())
			.contains("UJAX", workspaceName, "https://ujax.site/workspaces/10");
	}
}
