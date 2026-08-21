package io.pragmatic.ponderus.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.pragmatic.ponderus.domain.Option;
import io.pragmatic.ponderus.domain.Project;
import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.OptionRequest;
import io.pragmatic.ponderus.repository.OptionRepository;
import io.pragmatic.ponderus.web.ResourceNotFoundException;
import lombok.AllArgsConstructor;

/**
 * Logique métier des options, rattachées à un projet.
 * Toutes les opérations sont scopées par utilisateur : agir sur un projet
 * (ou une option) qui n'appartient pas à l'appelant renvoie 404.
 * Le contrôle de propriété du projet est délégué à {@link ProjectService}
 * pour ne pas dupliquer la règle d'isolation multi-tenant.
 */
@Service
@AllArgsConstructor
public class OptionService {

    private final OptionRepository optionRepository;
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    public List<Option> findByProject(User user, UUID projectId) {
        projectService.findOne(user, projectId); // 404 si le projet n'est pas à l'utilisateur
        return optionRepository.findByProjectIdOrderByPositionAscIdAsc(projectId);
    }

    @Transactional
    public Option create(User user, UUID projectId, OptionRequest request) {
        Project project = projectService.findOne(user, projectId);

        int position = request.position() != null
                ? request.position()
                : optionRepository.findMaxPositionByProjectId(projectId) + 1;

        Option option = Option.builder()
                .project(project)
                .name(request.name())
                .position(position)
                .build();

        return optionRepository.save(option);
    }

    @Transactional
    public Option update(User user, UUID projectId, UUID optionId, OptionRequest request) {
        projectService.findOne(user, projectId);
        Option option = requireOption(projectId, optionId);

        option.setName(request.name());
        if (request.position() != null) {
            option.setPosition(request.position());
        }
        return optionRepository.save(option);
    }

    @Transactional
    public void delete(User user, UUID projectId, UUID optionId) {
        projectService.findOne(user, projectId);
        Option option = requireOption(projectId, optionId);
        // La suppression cascade sur les scores associés (FK ON DELETE CASCADE).
        optionRepository.delete(option);
    }

    /** Charge l'option si elle appartient bien au projet, sinon 404. */
    private Option requireOption(UUID projectId, UUID optionId) {
        return optionRepository.findByIdAndProjectId(optionId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Option introuvable : " + optionId));
    }
}
