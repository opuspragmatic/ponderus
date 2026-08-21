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

import io.pragmatic.ponderus.domain.Criterion;
import io.pragmatic.ponderus.domain.Project;
import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.CriterionRequest;
import io.pragmatic.ponderus.repository.CriterionRepository;
import io.pragmatic.ponderus.web.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class CriterionServiceTest {

    @Mock
    private CriterionRepository criterionRepository;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private CriterionService criterionService;

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
    void findByProject_returnsOrderedCriteria_whenProjectOwned() {
        Criterion criterion = new Criterion();
        projectOwned();
        when(criterionRepository.findByProjectIdOrderByPositionAscIdAsc(projectId))
                .thenReturn(List.of(criterion));

        assertThat(criterionService.findByProject(user, projectId)).containsExactly(criterion);
    }

    @Test
    void findByProject_throwsNotFound_whenProjectNotOwned() {
        projectNotOwned();

        assertThatThrownBy(() -> criterionService.findByProject(user, projectId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_appendsAfterMaxPosition_andDefaultsElimToFalse() {
        projectOwned();
        when(criterionRepository.findMaxPositionByProjectId(projectId)).thenReturn(0);
        when(criterionRepository.save(any(Criterion.class))).thenAnswer(inv -> inv.getArgument(0));

        Criterion created = criterionService.create(user, projectId,
                new CriterionRequest("Prix", 3, null, null));

        assertThat(created.getLabel()).isEqualTo("Prix");
        assertThat(created.getWeight()).isEqualTo(3);
        assertThat(created.isElim()).isFalse();
        assertThat(created.getPosition()).isEqualTo(1);
    }

    @Test
    void create_keepsElimTrue_andExplicitPosition() {
        projectOwned();
        when(criterionRepository.save(any(Criterion.class))).thenAnswer(inv -> inv.getArgument(0));

        Criterion created = criterionService.create(user, projectId,
                new CriterionRequest("Surface", 5, true, 4));

        assertThat(created.isElim()).isTrue();
        assertThat(created.getPosition()).isEqualTo(4);
    }

    @Test
    void create_throwsNotFound_whenProjectNotOwned() {
        projectNotOwned();

        assertThatThrownBy(() -> criterionService.create(user, projectId,
                new CriterionRequest("X", 3, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(criterionRepository, never()).save(any());
    }

    @Test
    void update_modifiesCriterionOfOwnedProject() {
        UUID criterionId = UUID.randomUUID();
        Criterion existing = new Criterion();
        existing.setLabel("Ancien");
        existing.setWeight(2);
        existing.setElim(false);
        existing.setPosition(0);
        projectOwned();
        when(criterionRepository.findByIdAndProjectId(criterionId, projectId))
                .thenReturn(Optional.of(existing));
        when(criterionRepository.save(any(Criterion.class))).thenAnswer(inv -> inv.getArgument(0));

        Criterion updated = criterionService.update(user, projectId, criterionId,
                new CriterionRequest("Nouveau", 4, true, 2));

        assertThat(updated.getLabel()).isEqualTo("Nouveau");
        assertThat(updated.getWeight()).isEqualTo(4);
        assertThat(updated.isElim()).isTrue();
        assertThat(updated.getPosition()).isEqualTo(2);
    }

    @Test
    void update_throwsNotFound_whenCriterionNotInProject() {
        UUID criterionId = UUID.randomUUID();
        projectOwned();
        when(criterionRepository.findByIdAndProjectId(criterionId, projectId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> criterionService.update(user, projectId, criterionId,
                new CriterionRequest("X", 3, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_removesCriterionOfOwnedProject() {
        UUID criterionId = UUID.randomUUID();
        Criterion existing = new Criterion();
        projectOwned();
        when(criterionRepository.findByIdAndProjectId(criterionId, projectId))
                .thenReturn(Optional.of(existing));

        criterionService.delete(user, projectId, criterionId);

        ArgumentCaptor<Criterion> captor = ArgumentCaptor.forClass(Criterion.class);
        verify(criterionRepository).delete(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
    }

    @Test
    void delete_throwsNotFound_whenProjectNotOwned() {
        UUID criterionId = UUID.randomUUID();
        projectNotOwned();

        assertThatThrownBy(() -> criterionService.delete(user, projectId, criterionId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(criterionRepository, never()).delete(any());
    }
}
