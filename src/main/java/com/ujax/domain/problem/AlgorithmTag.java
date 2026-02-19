package com.ujax.domain.problem;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;

import com.ujax.domain.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "algorithm_tag")
@Filter(
	name = "softDeleteFilter",
	condition = "deleted_at IS NULL"
)
@SQLDelete(sql = "UPDATE algorithm_tag SET deleted_at = now() WHERE id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlgorithmTag extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String name;

	public static AlgorithmTag create(String name) {
		AlgorithmTag tag = new AlgorithmTag();
		tag.name = name;
		return tag;
	}
}
