package com.ujax.application.mail.template;

public final class WorkspaceInviteMailTemplateRenderer {

	private WorkspaceInviteMailTemplateRenderer() {
	}

	public static RenderedMailContent render(String workspaceName, String link) {
		String escapedWorkspaceName = MailTemplateLayout.escape(workspaceName);
		String escapedLink = MailTemplateLayout.escape(link);

		String plainText = String.join("\n",
			"안녕하세요.",
			"",
			"UJAX에서 \"" + workspaceName + "\" 워크스페이스에 당신을 초대했습니다.",
			"아래 링크를 통해 워크스페이스로 이동할 수 있습니다.",
			link,
			"",
			"감사합니다.",
			"UJAX 팀"
		);

		String bodyHtml = """
			<p style="margin:0 0 18px;color:#475569;font-size:15px;line-height:1.8;">
			  UJAX에서 새로운 협업 초대가 도착했습니다. 아래 워크스페이스에서 함께 문제를 풀고 진행 상황을 공유할 수 있습니다.
			</p>
			<div style="margin:0 0 22px;padding:22px 24px;border-radius:20px;background:#f8fafc;border:1px solid #dbe4ee;text-align:center;">
			  <div style="margin:0 0 8px;color:#64748b;font-size:12px;font-weight:700;letter-spacing:0.16em;text-transform:uppercase;">Workspace</div>
			  <div style="color:#0f172a;font-size:28px;font-weight:800;line-height:1.3;">"""
			+ escapedWorkspaceName +
			"""
			  </div>
			</div>
			<p style="margin:0 0 24px;color:#64748b;font-size:14px;line-height:1.8;">
			  아래 버튼을 누르면 바로 워크스페이스로 이동합니다. 버튼이 열리지 않으면 하단 링크를 그대로 브라우저에 붙여 넣어 주세요.
			</p>
			""";

		String htmlText = MailTemplateLayout.wrap(
			"UJAX 워크스페이스 초대",
			"워크스페이스 초대가 도착했습니다",
			"초대 링크를 통해 바로 협업 공간으로 이동할 수 있습니다.",
			bodyHtml,
			"워크스페이스 열기",
			escapedLink,
			"링크가 열리지 않으면 다음 주소를 복사해 브라우저에서 열어 주세요.<br><span style=\"word-break:break-all;color:#0f172a;\">"
				+ escapedLink + "</span>"
		);

		return new RenderedMailContent(plainText, htmlText);
	}
}
