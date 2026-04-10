package com.ujax.application.mail.template;

public final class SignupVerificationMailTemplateRenderer {

	private SignupVerificationMailTemplateRenderer() {
	}

	public static RenderedMailContent render(String code) {
		String escapedCode = MailTemplateLayout.escape(code);

		String plainText = String.join("\n",
			"안녕하세요.",
			"",
			"UJAX 회원가입 이메일 인증 코드입니다.",
			"인증 코드: " + code,
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
			<p style="margin:0;color:#64748b;font-size:14px;line-height:1.8;">
			  본인이 요청하지 않았다면 이 메일을 무시해 주세요. 인증 코드는 타인에게 공유하지 않는 것이 안전합니다.
			</p>
			""";

		String htmlText = MailTemplateLayout.wrap(
			null,
			"회원가입 인증을 마무리해 주세요",
			"보안 확인을 위해 1회용 인증 코드를 준비했습니다.",
			bodyHtml,
			null,
			null,
			"본 메일은 UJAX 서비스 보안 절차에 따라 자동 발송되었습니다."
		);

		return new RenderedMailContent(plainText, htmlText);
	}
}
