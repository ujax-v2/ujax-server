package com.ujax.application.mail;

public record RenderedMailContent(
	String plainText,
	String htmlText
) {
}
