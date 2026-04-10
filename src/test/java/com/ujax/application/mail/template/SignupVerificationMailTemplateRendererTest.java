package com.ujax.application.mail.template;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SignupVerificationMailTemplateRendererTest {

	@Test
	@DisplayName("회원가입 인증 메일 헤더 색상은 다크모드에서도 유지되고 만료 시간 없이 전체 본문을 노출한다")
	void renderKeepsHeaderColorsInDarkMode() {
		RenderedMailContent content = SignupVerificationMailTemplateRenderer.render("123456");

		assertThat(content.plainText())
			.contains("인증 코드: 123456")
			.doesNotContain("만료 시간");

		assertThat(content.htmlText())
			.contains("<meta name=\"color-scheme\" content=\"light\">")
			.contains("<meta name=\"supported-color-schemes\" content=\"light\">")
			.contains("class=\"mail-title\"")
			.contains("class=\"mail-lead\"")
			.contains("-webkit-text-fill-color: #ffffff !important;")
			.contains("-webkit-text-fill-color: #dbeafe !important;")
			.contains("123456")
			.doesNotContain("Expires At")
			.doesNotContain("display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;");
	}
}
