package com.ujax.application.mail;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UjaxMailTemplateRendererTest {

	@Test
	@DisplayName("회원가입 인증 메일 헤더 색상은 다크모드에서도 유지되도록 고정한다")
	void renderSignupVerificationKeepsHeaderColorsInDarkMode() {
		RenderedMailContent content = UjaxMailTemplateRenderer.renderSignupVerification(
			"123456",
			"2026-03-30 10:30"
		);

		assertThat(content.htmlText())
			.contains("<meta name=\"color-scheme\" content=\"light\">")
			.contains("<meta name=\"supported-color-schemes\" content=\"light\">")
			.contains("class=\"mail-title\"")
			.contains("class=\"mail-lead\"")
			.contains("-webkit-text-fill-color: #ffffff !important;")
			.contains("-webkit-text-fill-color: #dbeafe !important;");
	}

	@Test
	@DisplayName("워크스페이스 초대 메일 템플릿은 초대 정보와 이동 링크를 포함한다")
	void renderWorkspaceInvitationIncludesWorkspaceAndLink() {
		RenderedMailContent content = UjaxMailTemplateRenderer.renderWorkspaceInvitation(
			"알고리즘 스터디",
			"https://ujax.site/workspaces/10"
		);

		assertThat(content.plainText())
			.contains("알고리즘 스터디")
			.contains("https://ujax.site/workspaces/10");

		assertThat(content.htmlText())
			.contains("워크스페이스 초대가 도착했습니다")
			.contains("알고리즘 스터디")
			.contains("워크스페이스 열기")
			.contains("https://ujax.site/workspaces/10")
			.contains("class=\"mail-title\"")
			.contains("class=\"mail-lead\"")
			.contains("background:#f8fafc")
			.contains("border:1px solid #dbe4ee")
			.contains("color:#64748b");
	}
}
