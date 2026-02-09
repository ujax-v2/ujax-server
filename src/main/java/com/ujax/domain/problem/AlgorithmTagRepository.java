package com.ujax.domain.problem;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlgorithmTagRepository extends JpaRepository<AlgorithmTag, Long> {

	Optional<AlgorithmTag> findByName(String name);
}
