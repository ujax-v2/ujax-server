package com.ujax.domain.problem;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;

import com.ujax.domain.common.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "problem")
@Filter(
	name = "softDeleteFilter",
	condition = "deleted_at IS NULL"
)
@SQLDelete(sql = "UPDATE problem SET deleted_at = now() WHERE id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Problem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private int problemNumber;

	@Column(nullable = false)
	private String title;

	private String tier;

	private String timeLimit;

	private String memoryLimit;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(columnDefinition = "TEXT")
	private String inputDescription;

	@Column(columnDefinition = "TEXT")
	private String outputDescription;

	private String url;

	@OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Sample> samples = new ArrayList<>();

	@ManyToMany
	@JoinTable(
		name = "problem_algorithm",
		joinColumns = @JoinColumn(name = "problem_id"),
		inverseJoinColumns = @JoinColumn(name = "algorithm_id")
	)
	private List<AlgorithmTag> algorithmTags = new ArrayList<>();

	@Builder
	private Problem(int problemNumber, String title, String tier, String timeLimit, String memoryLimit,
		String description, String inputDescription, String outputDescription, String url) {
		this.problemNumber = problemNumber;
		this.title = title;
		this.tier = tier;
		this.timeLimit = timeLimit;
		this.memoryLimit = memoryLimit;
		this.description = description;
		this.inputDescription = inputDescription;
		this.outputDescription = outputDescription;
		this.url = url;
	}

	public static Problem create(int problemNumber, String title, String tier, String timeLimit,
		String memoryLimit, String description, String inputDescription, String outputDescription, String url) {
		return Problem.builder()
			.problemNumber(problemNumber)
			.title(title)
			.tier(tier)
			.timeLimit(timeLimit)
			.memoryLimit(memoryLimit)
			.description(description)
			.inputDescription(inputDescription)
			.outputDescription(outputDescription)
			.url(url)
			.build();
	}

	public void addSample(Sample sample) {
		this.samples.add(sample);
		sample.assignProblem(this);
	}

	public void addAlgorithmTag(AlgorithmTag algorithmTag) {
		this.algorithmTags.add(algorithmTag);
	}
}
