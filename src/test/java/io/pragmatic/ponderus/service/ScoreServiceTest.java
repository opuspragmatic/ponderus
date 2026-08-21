package io.pragmatic.ponderus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.pragmatic.ponderus.domain.Criterion;
import io.pragmatic.ponderus.domain.Option;
import io.pragmatic.ponderus.domain.Project;
import io.pragmatic.ponderus.domain.Score;
import io.pragmatic.ponderus.domain.User;
import io.pragmatic.ponderus.dto.ScoreRequest;
import io.pragmatic.ponderus.repository.CriterionRepository;
import io.pragmatic.ponderus.repository.OptionRepository;
import io.pragmatic.ponderus.repository.ScoreRepository;
import io.pragmatic.ponderus.web.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @Mock
    private ScoreRepository scoreRepository;

    @Mock
    private CriterionRepository criterionRepository;

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private ProjectService projectService;

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

    private void projectOwned() {
        Project project = new Project();
        project.setUser(user);
        when(projectService.findOne(user, projectId)).thenReturn(project);
    }

    private void criterionAndOptionResolve() {
        when(criterionRepository.findByIdAndProjectId(criterionId, projectId))
                .thenReturn(Optional.of(new Criterion()));
        when(optionRepository.findByIdAndProjectId(optionId, projectId))
                .thenReturn(Optional.of(new Option()));
    }

    private ScoreRequest request(String value, String note) {
        return new ScoreRequest(value != null ? new BigDecimal(value) : null, note);
    }

    @Test
    void upsert_createsScore_whenNoneExists() {
        projectOwned();
        criterionAndOptionResolve();
        when(scoreRepository.findByCriterionIdAndOptionId(criterionId, optionId))
                .thenReturn(Optional.empty());
        when(scoreRepository.save(any(Score.class))).thenAnswer(inv -> inv.getArgument(0));

        Score result = scoreService.upsert(user, projectId, criterionId, optionId, request("4.5", "correct"));

        assertThat(result.getValue()).isEqualByComparingTo("4.5");
        assertThat(result.getNote()).isEqualTo("correct");
        assertThat(result.getCriterion()).isNotNull();
        assertThat(result.getOption()).isNotNull();
    }

    @Test
    void upsert_updatesExistingScore_inPlace() {
        projectOwned();
        criterionAndOptionResolve();
        Score existing = new Score();
        existing.setValue(new BigDecimal("2.0"));
        existing.setNote("ancien");
        when(scoreRepository.findByCriterionIdAndOptionId(criterionId, optionId))
                .thenReturn(Optional.of(existing));
        when(scoreRepository.save(any(Score.class))).thenAnswer(inv -> inv.getArgument(0));

        Score result = scoreService.upsert(user, projectId, criterionId, optionId, request("3.5", "maj"));

        assertThat(result).isSameAs(existing);
        assertThat(result.getValue()).isEqualByComparingTo("3.5");
        assertThat(result.getNote()).isEqualTo("maj");
    }

    @Test
    void upsert_allowsNullValue_meaningNotYetScored() {
        projectOwned();
        criterionAndOptionResolve();
        Score existing = new Score();
        existing.setValue(new BigDecimal("4.0"));
        when(scoreRepository.findByCriterionIdAndOptionId(criterionId, optionId))
                .thenReturn(Optional.of(existing));
        when(scoreRepository.save(any(Score.class))).thenAnswer(inv -> inv.getArgument(0));

        Score result = scoreService.upsert(user, projectId, criterionId, optionId, request(null, null));

        assertThat(result.getValue()).isNull();
    }

    @Test
    void upsert_throwsNotFound_whenProjectNotOwned() {
        when(projectService.findOne(user, projectId))
                .thenThrow(new ResourceNotFoundException("Projet introuvable"));

        assertThatThrownBy(() -> scoreService.upsert(user, projectId, criterionId, optionId, request("1.0", null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(scoreRepository, never()).save(any());
    }

    @Test
    void upsert_throwsNotFound_whenCriterionNotInProject() {
        projectOwned();
        when(criterionRepository.findByIdAndProjectId(criterionId, projectId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> scoreService.upsert(user, projectId, criterionId, optionId, request("1.0", null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(scoreRepository, never()).save(any());
    }

    @Test
    void upsert_throwsNotFound_whenOptionNotInProject() {
        projectOwned();
        when(criterionRepository.findByIdAndProjectId(criterionId, projectId))
                .thenReturn(Optional.of(new Criterion()));
        when(optionRepository.findByIdAndProjectId(optionId, projectId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> scoreService.upsert(user, projectId, criterionId, optionId, request("1.0", null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(scoreRepository, never()).save(any());
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
