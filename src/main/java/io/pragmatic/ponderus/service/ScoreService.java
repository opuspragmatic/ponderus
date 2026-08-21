package io.pragmatic.ponderus.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.pragmatic.ponderus.domain.Criterion;
import io.pragmatic.ponderus.domain.Option;
import io.pragmatic.ponderus.domain.Score;
import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.ScoreRequest;
import io.pragmatic.ponderus.repository.CriterionRepository;
import io.pragmatic.ponderus.repository.OptionRepository;
import io.pragmatic.ponderus.repository.ScoreRepository;
import io.pragmatic.ponderus.web.ResourceNotFoundException;
import lombok.AllArgsConstructor;

/**
 * Logique métier des scores (croisement critère × option d'un projet).
 * Scoping par utilisateur délégué à {@link ProjectService}. Le critère ET
 * l'option doivent appartenir au projet ciblé, sinon 404.
 */
@Service
@AllArgsConstructor
public class ScoreService {

    private final ScoreRepository scoreRepository;
    private final CriterionRepository criterionRepository;
    private final OptionRepository optionRepository;
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    public List<Score> findByProject(User user, UUID projectId) {
        projectService.findOne(user, projectId); // 404 si le projet n'est pas à l'utilisateur
        return scoreRepository.findByProjectId(projectId);
    }

    /**
     * Upsert du score d'un couple (critère, option) : met à jour la ligne
     * existante si elle existe, sinon la crée. Idempotent.
     */
    @Transactional
    public Score upsert(User user, UUID projectId, UUID criterionId, UUID optionId, ScoreRequest request) {
        projectService.findOne(user, projectId);
        Criterion criterion = requireCriterion(projectId, criterionId);
        Option option = requireOption(projectId, optionId);

        Score score = scoreRepository.findByCriterionIdAndOptionId(criterionId, optionId)
                .orElseGet(() -> {
                    Score created = new Score();
                    created.setCriterion(criterion);
                    created.setOption(option);
                    return created;
                });

        // value peut être null volontairement (« pas encore noté »).
        score.setValue(request.value());
        score.setNote(request.note());

        return scoreRepository.save(score);
    }

    private Criterion requireCriterion(UUID projectId, UUID criterionId) {
        return criterionRepository.findByIdAndProjectId(criterionId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Critère introuvable : " + criterionId));
    }

    private Option requireOption(UUID projectId, UUID optionId) {
        return optionRepository.findByIdAndProjectId(optionId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Option introuvable : " + optionId));
    }
}
