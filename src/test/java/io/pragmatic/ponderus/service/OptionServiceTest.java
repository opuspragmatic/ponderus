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

import io.pragmatic.ponderus.domain.Option;
import io.pragmatic.ponderus.domain.Project;
import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.OptionRequest;
import io.pragmatic.ponderus.repository.OptionRepository;
import io.pragmatic.ponderus.web.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class OptionServiceTest {

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private OptionService optionService;

    private final User user = User.builder()
            .id(UUID.randomUUID())
            .email("owner@example.com")
            .firebaseUid("firebase-owner")
            .build();

    private final UUID projectId = UUID.randomUUID();

    private Project ownedProject() {
        Project project = new Project();
        project.setUser(user);
        return project;
    }

    private void projectOwned() {
        when(projectService.findOne(user, projectId)).thenReturn(ownedProject());
    }

    private void projectNotOwned() {
        when(projectService.findOne(user, projectId))
                .thenThrow(new ResourceNotFoundException("Projet introuvable : " + projectId));
    }

    @Test
    void findByProject_returnsOrderedOptions_whenProjectOwned() {
        Option option = new Option();
        projectOwned();
        when(optionRepository.findByProjectIdOrderByPositionAscIdAsc(projectId))
                .thenReturn(List.of(option));

        assertThat(optionService.findByProject(user, projectId)).containsExactly(option);
    }

    @Test
    void findByProject_throwsNotFound_whenProjectNotOwned() {
        projectNotOwned();

        assertThatThrownBy(() -> optionService.findByProject(user, projectId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_appendsAfterMaxPosition_whenPositionOmitted() {
        projectOwned();
        when(optionRepository.findMaxPositionByProjectId(projectId)).thenReturn(1);
        when(optionRepository.save(any(Option.class))).thenAnswer(inv -> inv.getArgument(0));

        Option created = optionService.create(user, projectId, new OptionRequest("Maison C", null));

        assertThat(created.getName()).isEqualTo("Maison C");
        assertThat(created.getPosition()).isEqualTo(2);
    }

    @Test
    void create_startsAtZero_whenProjectHasNoOption() {
        projectOwned();
        when(optionRepository.findMaxPositionByProjectId(projectId)).thenReturn(-1);
        when(optionRepository.save(any(Option.class))).thenAnswer(inv -> inv.getArgument(0));

        Option created = optionService.create(user, projectId, new OptionRequest("Première", null));

        assertThat(created.getPosition()).isEqualTo(0);
    }

    @Test
    void create_usesExplicitPosition_whenProvided() {
        projectOwned();
        when(optionRepository.save(any(Option.class))).thenAnswer(inv -> inv.getArgument(0));

        Option created = optionService.create(user, projectId, new OptionRequest("Maison A", 5));

        assertThat(created.getPosition()).isEqualTo(5);
    }

    @Test
    void create_throwsNotFound_whenProjectNotOwned() {
        projectNotOwned();

        assertThatThrownBy(() -> optionService.create(user, projectId, new OptionRequest("X", null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(optionRepository, never()).save(any());
    }

    @Test
    void update_modifiesOptionOfOwnedProject() {
        UUID optionId = UUID.randomUUID();
        Option existing = new Option();
        existing.setName("Ancien");
        existing.setPosition(0);
        projectOwned();
        when(optionRepository.findByIdAndProjectId(optionId, projectId))
                .thenReturn(Optional.of(existing));
        when(optionRepository.save(any(Option.class))).thenAnswer(inv -> inv.getArgument(0));

        Option updated = optionService.update(user, projectId, optionId, new OptionRequest("Nouveau", 3));

        assertThat(updated.getName()).isEqualTo("Nouveau");
        assertThat(updated.getPosition()).isEqualTo(3);
    }

    @Test
    void update_throwsNotFound_whenOptionNotInProject() {
        UUID optionId = UUID.randomUUID();
        projectOwned();
        when(optionRepository.findByIdAndProjectId(optionId, projectId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> optionService.update(user, projectId, optionId, new OptionRequest("X", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_removesOptionOfOwnedProject() {
        UUID optionId = UUID.randomUUID();
        Option existing = new Option();
        projectOwned();
        when(optionRepository.findByIdAndProjectId(optionId, projectId))
                .thenReturn(Optional.of(existing));

        optionService.delete(user, projectId, optionId);

        ArgumentCaptor<Option> captor = ArgumentCaptor.forClass(Option.class);
        verify(optionRepository).delete(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
    }

    @Test
    void delete_throwsNotFound_whenProjectNotOwned() {
        UUID optionId = UUID.randomUUID();
        projectNotOwned();

        assertThatThrownBy(() -> optionService.delete(user, projectId, optionId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(optionRepository, never()).delete(any());
    }
}
