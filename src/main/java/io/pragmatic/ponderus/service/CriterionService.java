package io.pragmatic.ponderus.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.pragmatic.ponderus.domain.Criterion;
import io.pragmatic.ponderus.domain.Project;
import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.CriterionRequest;
import io.pragmatic.ponderus.repository.CriterionRepository;
import io.pragmatic.ponderus.web.ResourceNotFoundException;
import lombok.AllArgsConstructor;

/**
 * Logique métier des critères, rattachés à un projet.
 * Toutes les opérations sont scopées par utilisateur : le contrôle de propriété
 * du projet est délégué à {@link ProjectService} pour ne pas dupliquer la règle
 * d'isolation multi-tenant. Un projet (ou critère) qui n'appartient pas à
 * l'appelant renvoie 404.
 */
@Service
@AllArgsConstructor
public class CriterionService {

    private final CriterionRepository criterionRepository;
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    public List<Criterion> findByProject(User user, UUID projectId) {
        projectService.findOne(user, projectId); // 404 si le projet n'est pas à l'utilisateur
        return criterionRepository.findByProjectIdOrderByPositionAscIdAsc(projectId);
    }

    /**
     * Charge un critère scopé par utilisateur : vérifie que le projet appartient
     * à l'appelant, puis que le critère appartient bien au projet. 404 sinon.
     * Point d'entrée unique réutilisé par les autres opérations et services.
     */
    @Transactional(readOnly = true)
    public Criterion findOne(User user, UUID projectId, UUID criterionId) {
        projectService.findOne(user, projectId);
        return criterionRepository.findByIdAndProjectId(criterionId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Critère introuvable : " + criterionId));
    }

    @Transactional
    public Criterion create(User user, UUID projectId, CriterionRequest request) {
        Project project = projectService.findOne(user, projectId);

        int position = request.position() != null
                ? request.position()
                : criterionRepository.findMaxPositionByProjectId(projectId) + 1;

        Criterion criterion = Criterion.builder()
                .project(project)
                .label(request.label())
                .weight(request.weight())
                .elim(Boolean.TRUE.equals(request.elim()))
                .position(position)
                .build();

        return criterionRepository.save(criterion);
    }

    @Transactional
    public Criterion update(User user, UUID projectId, UUID criterionId, CriterionRequest request) {
        Criterion criterion = findOne(user, projectId, criterionId);

        criterion.setLabel(request.label());
        criterion.setWeight(request.weight());
        criterion.setElim(Boolean.TRUE.equals(request.elim()));
        if (request.position() != null) {
            criterion.setPosition(request.position());
        }
        return criterionRepository.save(criterion);
    }

    @Transactional
    public void delete(User user, UUID projectId, UUID criterionId) {
        Criterion criterion = findOne(user, projectId, criterionId);
        // La suppression cascade sur les scores associés (FK ON DELETE CASCADE).
        criterionRepository.delete(criterion);
    }
}
