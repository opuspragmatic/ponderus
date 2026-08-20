package io.pragmatic.ponderus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.pragmatic.ponderus.domain.Project;
import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.ProjectRequest;
import io.pragmatic.ponderus.repository.ProjectRepository;
import io.pragmatic.ponderus.web.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    private final User user = User.builder()
            .id(UUID.randomUUID())
            .email("owner@example.com")
            .firebaseUid("firebase-owner")
            .build();

    @Test
    void findAll_scopesByUserId() {
        Project project = new Project();
        when(projectRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(project));

        List<Project> result = projectService.findAll(user);

        assertThat(result).containsExactly(project);
        verify(projectRepository).findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Test
    void findOne_returnsProject_whenOwnedByUser() {
        UUID id = UUID.randomUUID();
        Project project = new Project();
        when(projectRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.of(project));

        assertThat(projectService.findOne(user, id)).isSameAs(project);
    }

    @Test
    void findOne_throwsNotFound_whenMissingOrOtherUser() {
        UUID id = UUID.randomUUID();
        when(projectRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.findOne(user, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_assignsUserAndPersists() {
        ProjectRequest request = new ProjectRequest("Maisons", 60);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        Project created = projectService.create(user, request);

        assertThat(created.getUser()).isSameAs(user);
        assertThat(created.getName()).isEqualTo("Maisons");
        assertThat(created.getElimThreshold()).isEqualTo(60);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
    }

    @Test
    void update_modifiesOwnedProject() {
        UUID id = UUID.randomUUID();
        Project existing = new Project();
        existing.setName("Ancien");
        when(projectRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        Project updated = projectService.update(user, id, new ProjectRequest("Nouveau", 70));

        assertThat(updated.getName()).isEqualTo("Nouveau");
        assertThat(updated.getElimThreshold()).isEqualTo(70);
    }

    @Test
    void update_throwsNotFound_whenNotOwned() {
        UUID id = UUID.randomUUID();
        when(projectRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.update(user, id, new ProjectRequest("X", null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void delete_removesOwnedProject() {
        UUID id = UUID.randomUUID();
        Project existing = new Project();
        when(projectRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.of(existing));

        projectService.delete(user, id);

        verify(projectRepository).delete(existing);
    }

    @Test
    void delete_throwsNotFound_whenNotOwned() {
        UUID id = UUID.randomUUID();
        when(projectRepository.findByIdAndUserId(id, user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.delete(user, id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(projectRepository, never()).delete(any());
    }
}
