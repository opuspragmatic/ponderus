package io.pragmatic.ponderus.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.pragmatic.ponderus.domain.Project;
import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.ProjectRequest;
import io.pragmatic.ponderus.repository.ProjectRepository;
import io.pragmatic.ponderus.web.ResourceNotFoundException;
import lombok.AllArgsConstructor;

/**
 * Logique métier des projets. Toutes les opérations sont scopées par
 * utilisateur : un projet n'appartenant pas à l'appelant est traité
 * comme inexistant (404).
 */
@Service
@AllArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<Project> findAll(User user) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public Project findOne(User user, UUID id) {
        return projectRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> notFound(id));
    }

    @Transactional
    public Project create(User user, ProjectRequest request) {
        Project project = new Project();
        project.setUser(user);
        project.setName(request.name());
        project.setElimThreshold(request.elimThreshold());
        return projectRepository.save(project);
    }

    @Transactional
    public Project update(User user, UUID id, ProjectRequest request) {
        Project project = findOne(user, id);
        project.setName(request.name());
        project.setElimThreshold(request.elimThreshold());
        return projectRepository.save(project);
    }

    @Transactional
    public void delete(User user, UUID id) {
        Project project = findOne(user, id);
        projectRepository.delete(project);
    }

    private ResourceNotFoundException notFound(UUID id) {
        return new ResourceNotFoundException("Projet introuvable : " + id);
    }
}
