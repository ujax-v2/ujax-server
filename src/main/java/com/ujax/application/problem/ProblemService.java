package com.ujax.application.problem;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.problem.dto.response.ProblemResponse;
import com.ujax.domain.problem.AlgorithmTag;
import com.ujax.domain.problem.AlgorithmTagRepository;
import com.ujax.domain.problem.Problem;
import com.ujax.domain.problem.ProblemRepository;
import com.ujax.domain.problem.Sample;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.ConflictException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.infrastructure.web.problem.dto.request.ProblemIngestRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemService {

	private final ProblemRepository problemRepository;
	private final AlgorithmTagRepository algorithmTagRepository;

	public ProblemResponse getProblem(Long problemId) {
		return ProblemResponse.from(findProblemById(problemId));
	}

	public ProblemResponse getProblemByNumber(int problemNumber) {
		Problem problem = problemRepository.findByProblemNumber(problemNumber)
			.orElseThrow(() -> new NotFoundException(ErrorCode.PROBLEM_NOT_FOUND));
		return ProblemResponse.from(problem);
	}

	@Transactional
	public ProblemResponse createProblem(ProblemIngestRequest request) {
		if (problemRepository.existsByProblemNumber(request.problemNumber())) {
			throw new ConflictException(ErrorCode.DUPLICATE_PROBLEM);
		}

		Problem problem = Problem.create(
			request.problemNumber(), cleanTitle(request.title()), request.tier(),
			request.timeLimit(), request.memoryLimit(),
			request.description(), request.inputDescription(), request.outputDescription(),
			request.url()
		);

		addSamples(problem, request.samples());
		linkAlgorithmTags(problem, request.tags());

		problemRepository.save(problem);
		return ProblemResponse.from(problem);
	}

	private void addSamples(Problem problem, List<ProblemIngestRequest.SampleDto> samples) {
		if (samples == null) {
			return;
		}
		for (ProblemIngestRequest.SampleDto s : samples) {
			problem.addSample(Sample.create(s.sampleIndex(), s.input(), s.output()));
		}
	}

	/** 기존 태그가 있으면 재사용하고, 없으면 새로 생성하여 연결 */
	private void linkAlgorithmTags(Problem problem, List<ProblemIngestRequest.TagDto> tags) {
		if (tags == null) {
			return;
		}
		for (ProblemIngestRequest.TagDto t : tags) {
			if (t.name() == null || t.name().isBlank()) {
				continue;
			}
			AlgorithmTag tag = algorithmTagRepository.findByName(t.name())
				.orElseGet(() -> algorithmTagRepository.save(AlgorithmTag.create(t.name())));
			problem.addAlgorithmTag(tag);
		}
	}

	private Problem findProblemById(Long problemId) {
		return problemRepository.findById(problemId)
			.orElseThrow(() -> new NotFoundException(ErrorCode.PROBLEM_NOT_FOUND));
	}

	/** 크롤러에서 제목에 탭(티어 정보)·개행이 섞여 들어오는 경우를 정리 */
	private String cleanTitle(String title) {
		if (title == null) {
			return null;
		}
		int tabIdx = title.indexOf('\t');
		if (tabIdx >= 0) {
			title = title.substring(0, tabIdx);
		}
		return title.replace('\r', ' ').replace('\n', ' ')
			.replaceAll("\\s+", " ").trim();
	}
}
