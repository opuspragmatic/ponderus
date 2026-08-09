package io.pragmatic.ponderus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Une des N options comparées dans un projet (ex-"Maison A/B").
 * Nommée ComparisonOption plutôt que Option pour éviter toute ambiguïté
 * de lecture avec java.util.Optional dans le reste du code.
 */
@Entity
@Table(name = "options")
@Getter
@Setter
@NoArgsConstructor
public class ComparisonOption {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer position;
}
