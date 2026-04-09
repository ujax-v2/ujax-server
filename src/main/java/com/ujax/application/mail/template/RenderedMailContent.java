package com.ujax.application.mail.template;

public record RenderedMailContent(
	String plainText,
	String htmlText
) {
}
