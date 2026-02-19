package com.ujax.domain.problem;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;

import com.ujax.domain.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "sample",
	uniqueConstraints = @UniqueConstraint(columnNames = {"problem_id", "sample_index"})
)
@Filter(
	name = "softDeleteFilter",
	condition = "deleted_at IS NULL"
)
@SQLDelete(sql = "UPDATE sample SET deleted_at = now() WHERE id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sample extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "problem_id", nullable = false)
	private Problem problem;

	@Column(nullable = false)
	private int sampleIndex;

	@Column(columnDefinition = "TEXT")
	private String input;

	@Column(columnDefinition = "TEXT")
	private String output;

	public static Sample create(int sampleIndex, String input, String output) {
		Sample sample = new Sample();
		sample.sampleIndex = sampleIndex;
		sample.input = input;
		sample.output = output;
		return sample;
	}

	void assignProblem(Problem problem) {
		this.problem = problem;
	}
}
