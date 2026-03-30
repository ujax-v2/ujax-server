package com.ujax.application.mail;

import org.springframework.web.util.HtmlUtils;

public final class UjaxMailTemplateRenderer {

	private UjaxMailTemplateRenderer() {
	}

	public static RenderedMailContent renderSignupVerification(String code, String expiresAt) {
		String escapedCode = escape(code);
		String escapedExpiresAt = escape(expiresAt);

		String plainText = String.join("\n",
			"안녕하세요.",
			"",
			"UJAX 회원가입 이메일 인증 코드입니다.",
			"인증 코드: " + code,
			"만료 시간: " + expiresAt,
			"",
			"본인이 요청하지 않았다면 이 메일을 무시해 주세요.",
			"감사합니다.",
			"UJAX 팀"
		);

		String bodyHtml = """
			<p style="margin:0 0 18px;color:#475569;font-size:15px;line-height:1.8;">
			  UJAX 회원가입을 거의 마쳤습니다. 아래 인증 코드를 입력하면 가입이 완료됩니다.
			</p>
			<div style="margin:0 0 22px;padding:24px;border-radius:20px;background:#f8fafc;border:1px solid #dbe4ee;text-align:center;">
			  <div style="margin:0 0 10px;color:#64748b;font-size:12px;font-weight:700;letter-spacing:0.16em;text-transform:uppercase;">Verification Code</div>
			  <div style="font-size:34px;line-height:1.1;font-weight:800;letter-spacing:0.22em;color:#0f172a;">"""
			+ escapedCode +
			"""
			  </div>
			</div>
			  <table role="presentation" width="100%" style="margin:0 0 22px;border-collapse:separate;border-spacing:0;">
			  <tr>
			    <td style="padding:18px 20px;border-radius:18px;background:#fff7ed;border:1px solid #fed7aa;">
			      <div style="margin:0 0 8px;color:#9a3412;font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;">Expires At</div>
			      <div style="color:#7c2d12;font-size:17px;font-weight:700;">"""
			+ escapedExpiresAt +
			"""
			      </div>
			    </td>
			  </tr>
			</table>
			<p style="margin:0;color:#64748b;font-size:14px;line-height:1.8;">
			  본인이 요청하지 않았다면 이 메일을 무시해 주세요. 인증 코드는 타인에게 공유하지 않는 것이 안전합니다.
			</p>
			""";

		String htmlText = wrapLayout(
			"회원가입 인증 코드 " + escapedCode,
			"회원가입 인증을 마무리해 주세요",
			"보안 확인을 위해 1회용 인증 코드를 준비했습니다.",
			bodyHtml,
			null,
			null,
			"본 메일은 UJAX 서비스 보안 절차에 따라 자동 발송되었습니다."
		);

		return new RenderedMailContent(plainText, htmlText);
	}

	public static RenderedMailContent renderWorkspaceInvitation(String workspaceName, String link) {
		String escapedWorkspaceName = escape(workspaceName);
		String escapedLink = escape(link);

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

		String htmlText = wrapLayout(
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

	private static String wrapLayout(
		String previewText,
		String title,
		String lead,
		String bodyHtml,
		String actionLabel,
		String actionUrl,
		String footerHtml
	) {
		String actionSection = "";
		if (actionLabel != null && actionUrl != null) {
			actionSection = """
				<div style="margin:0 0 24px;text-align:center;">
				  <a href=\""""
				+ actionUrl +
				"""
				  \" style="display:inline-block;padding:15px 28px;border-radius:999px;background:linear-gradient(135deg,#0f766e,#155e75);color:#ffffff;font-size:15px;font-weight:700;text-decoration:none;">
				    """
				+ escape(actionLabel) +
				"""
				  </a>
				</div>
				""";
		}

		return """
			<!DOCTYPE html>
			<html lang="ko">
			<head>
			  <meta charset="UTF-8">
			  <meta name="viewport" content="width=device-width, initial-scale=1.0">
			  <meta name="color-scheme" content="light">
			  <meta name="supported-color-schemes" content="light">
			  <title>UJAX Mail</title>
			  <style>
			    :root {
			      color-scheme: light;
			      supported-color-schemes: light;
			    }

			    .mail-brand,
			    [data-ogsc] .mail-brand {
			      color: #99f6e4 !important;
			      -webkit-text-fill-color: #99f6e4 !important;
			    }

			    .mail-title,
			    [data-ogsc] .mail-title {
			      color: #ffffff !important;
			      -webkit-text-fill-color: #ffffff !important;
			    }

			    .mail-lead,
			    [data-ogsc] .mail-lead {
			      color: #dbeafe !important;
			      -webkit-text-fill-color: #dbeafe !important;
			    }
			  </style>
			</head>
			<body style="margin:0;padding:0;background-color:#f3f4f6;" bgcolor="#f3f4f6">
			  <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">
			    """
			+ previewText +
			"""
			  </div>
			  <table role="presentation" width="100%" style="width:100%;border-collapse:collapse;background:#f3f4f6;" bgcolor="#f3f4f6">
			    <tr>
			      <td align="center" style="padding:32px 16px;">
			        <table role="presentation" width="100%" style="max-width:640px;width:100%;border-collapse:separate;border-spacing:0;background:#ffffff;border:1px solid #dbe4ee;border-radius:28px;overflow:hidden;" bgcolor="#ffffff">
			          <tr>
			            <td style="padding:28px 32px;background:linear-gradient(135deg,#0f172a,#155e75);" bgcolor="#0f172a">
			              <div class="mail-brand" style="margin:0 0 12px;color:#99f6e4;font-size:12px;font-weight:700;letter-spacing:0.24em;text-transform:uppercase;">UJAX</div>
			              <h1 class="mail-title" style="margin:0;color:#ffffff;font-size:30px;line-height:1.3;font-weight:800;">"""
			+ escape(title) +
			"""
			              </h1>
			              <p class="mail-lead" style="margin:14px 0 0;color:#dbeafe;font-size:15px;line-height:1.8;">"""
			+ escape(lead) +
			"""
			              </p>
			            </td>
			          </tr>
			          <tr>
			            <td style="padding:32px;">
			              """
			+ bodyHtml
			+ actionSection +
			"""
			              <div style="padding-top:22px;border-top:1px solid #e5e7eb;color:#94a3b8;font-size:12px;line-height:1.8;">
			                """
			+ footerHtml +
			"""
			              </div>
			            </td>
			          </tr>
			        </table>
			      </td>
			    </tr>
			  </table>
			</body>
			</html>
			""";
	}

	private static String escape(String value) {
		return HtmlUtils.htmlEscape(value);
	}
}
