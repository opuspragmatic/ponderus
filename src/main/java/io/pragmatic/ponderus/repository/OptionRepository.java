package io.pragmatic.ponderus.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import io.pragmatic.ponderus.domain.Option;

public interface OptionRepository extends CrudRepository<Option, UUID> {

    List<Option> findByProjectIdOrderByPositionAsc(UUID projectId);

    Optional<Option> findByIdAndProjectId(UUID id, UUID projectId);

    long countByProjectId(UUID projectId);
}
