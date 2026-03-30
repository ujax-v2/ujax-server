package com.ujax.application.workspace;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
			"no-reply@ujax.kro.kr",
			"UJAX"
		);
	}

	@Test
	@DisplayName("워크스페이스 초대 메일을 발송한다")
	void sendInvitation() throws Exception {
		MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
		when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

		// given
		String email = "invite@example.com";
		String workspaceName = "워크스페이스";
		Long workspaceId = 10L;

		// when
		workspaceInviteMailer.sendInvitation(email, workspaceName, workspaceId);

		// then
		ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
		verify(mailSender).send(captor.capture());
		MimeMessage message = captor.getValue();
		Address[] fromAddresses = message.getFrom();
		InternetAddress from = (InternetAddress)fromAddresses[0];
		Address[] recipients = message.getRecipients(Message.RecipientType.TO);
		InternetAddress to = (InternetAddress)recipients[0];

		assertThat(from.getAddress()).isEqualTo("no-reply@ujax.kro.kr");
		assertThat(from.getPersonal()).isEqualTo("UJAX");
		assertThat(message.getSubject()).isEqualTo("[UJAX] 워크스페이스 초대");
		assertThat(to.getAddress()).isEqualTo(email);
		String rawMessage = rawMessage(message);
		assertThat(rawMessage)
			.contains("text/plain")
			.contains("text/html")
			.contains("Workspace")
			.contains("https://ujax.site/workspaces/10");
	}

	private String rawMessage(MimeMessage message) throws Exception {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		message.writeTo(outputStream);
		return outputStream.toString(StandardCharsets.UTF_8);
	}
}
