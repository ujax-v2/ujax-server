package com.ujax.infrastructure.web.problem.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProblemIngestRequest(
	@Positive @JsonProperty("problemNum") int problemNumber,
	@NotBlank String title,
	String tier,
	String timeLimit,
	String memoryLimit,
	@JsonProperty("problemDesc") String description,
	@JsonProperty("problemInput") String inputDescription,
	@JsonProperty("problemOutput") String outputDescription,
	String url,
	List<SampleDto> samples,
	List<TagDto> tags
) {

	public record SampleDto(int sampleIndex, String input, String output) {
	}

	public record TagDto(String name) {
	}
}
