package io.pragmatic.ponderus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import io.pragmatic.ponderus.domain.Option;
import io.pragmatic.ponderus.domain.Project;
import io.pragmatic.ponderus.domain.Score;
import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.ScoreRequest;
import io.pragmatic.ponderus.repository.ScoreRepository;
import io.pragmatic.ponderus.web.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @Mock
    private ScoreRepository scoreRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private CriterionService criterionService;

    @Mock
    private OptionService optionService;

    @InjectMocks
    private ScoreService scoreService;

    private final User user = User.builder()
            .id(UUID.randomUUID())
            .email("owner@example.com")
            .firebaseUid("firebase-owner")
            .build();

    private final UUID projectId = UUID.randomUUID();
    private final UUID criterionId = UUID.randomUUID();
    private final UUID optionId = UUID.randomUUID();

    private void criterionAndOptionResolve() {
        when(criterionService.findOne(user, projectId, criterionId)).thenReturn(new Criterion());
        when(optionService.findOne(user, projectId, optionId)).thenReturn(new Option());
    }

    private ScoreRequest request(String value, String note) {
        return new ScoreRequest(value != null ? new BigDecimal(value) : null, note);
    }

    @Test
    void upsert_delegatesToAtomicUpsert_andReturnsPersistedScore() {
        criterionAndOptionResolve();
        Score persisted = new Score();
        persisted.setValue(new BigDecimal("4.5"));
        persisted.setNote("correct");
        when(scoreRepository.findByCriterionIdAndOptionId(criterionId, optionId))
                .thenReturn(Optional.of(persisted));

        Score result = scoreService.upsert(user, projectId, criterionId, optionId, request("4.5", "correct"));

        assertThat(result).isSameAs(persisted);
        ArgumentCaptor<BigDecimal> value = ArgumentCaptor.forClass(BigDecimal.class);
        verify(scoreRepository).upsert(any(UUID.class), eq(criterionId), eq(optionId),
                value.capture(), eq("correct"));
        assertThat(value.getValue()).isEqualByComparingTo("4.5");
    }

    @Test
    void upsert_passesNullValue_meaningNotYetScored() {
        criterionAndOptionResolve();
        when(scoreRepository.findByCriterionIdAndOptionId(criterionId, optionId))
                .thenReturn(Optional.of(new Score()));

        scoreService.upsert(user, projectId, criterionId, optionId, request(null, null));

        verify(scoreRepository).upsert(any(UUID.class), eq(criterionId), eq(optionId),
                eq(null), eq(null));
    }

    @Test
    void upsert_throwsNotFound_whenCriterionCheckFails() {
        when(criterionService.findOne(user, projectId, criterionId))
                .thenThrow(new ResourceNotFoundException("Critère introuvable"));

        assertThatThrownBy(() -> scoreService.upsert(user, projectId, criterionId, optionId, request("1.0", null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(scoreRepository, never()).upsert(any(), any(), any(), any(), any());
    }

    @Test
    void upsert_throwsNotFound_whenOptionCheckFails() {
        when(criterionService.findOne(user, projectId, criterionId)).thenReturn(new Criterion());
        when(optionService.findOne(user, projectId, optionId))
                .thenThrow(new ResourceNotFoundException("Option introuvable"));

        assertThatThrownBy(() -> scoreService.upsert(user, projectId, criterionId, optionId, request("1.0", null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(scoreRepository, never()).upsert(any(), any(), any(), any(), any());
    }

    @Test
    void findByProject_returnsScores_whenOwned() {
        Score score = new Score();
        Project project = new Project();
        project.setUser(user);
        when(projectService.findOne(user, projectId)).thenReturn(project);
        when(scoreRepository.findByProjectId(projectId)).thenReturn(List.of(score));

        assertThat(scoreService.findByProject(user, projectId)).containsExactly(score);
    }

    @Test
    void findByProject_throwsNotFound_whenNotOwned() {
        when(projectService.findOne(user, projectId))
                .thenThrow(new ResourceNotFoundException("Projet introuvable"));

        assertThatThrownBy(() -> scoreService.findByProject(user, projectId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
