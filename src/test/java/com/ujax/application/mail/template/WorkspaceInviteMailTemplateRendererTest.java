package com.ujax.application.mail.template;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkspaceInviteMailTemplateRendererTest {

	@Test
	@DisplayName("워크스페이스 초대 메일 템플릿은 초대 정보와 이동 링크를 포함한다")
	void renderIncludesWorkspaceAndLink() {
		RenderedMailContent content = WorkspaceInviteMailTemplateRenderer.render(
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
