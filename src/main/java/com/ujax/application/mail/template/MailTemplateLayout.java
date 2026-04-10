package com.ujax.application.mail.template;

import org.springframework.web.util.HtmlUtils;

final class MailTemplateLayout {

	private MailTemplateLayout() {
	}

	static String wrap(
		String previewText,
		String title,
		String lead,
		String bodyHtml,
		String actionLabel,
		String actionUrl,
		String footerHtml
	) {
		String previewSection = "";
		if (previewText != null && !previewText.isBlank()) {
			previewSection = """
				<div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">
				  """
				+ escape(previewText) +
				"""
				</div>
				""";
		}

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
			  """
			+ previewSection +
			"""
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

	static String escape(String value) {
		return HtmlUtils.htmlEscape(value);
	}
}
