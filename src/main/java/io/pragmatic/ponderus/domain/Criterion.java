package io.pragmatic.ponderus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "criteria")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Criterion {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String label;

    /** Importance du critère, de 1 à 5. */
    @Column(nullable = false)
    private Integer weight;

    /** Si vrai, un score sous le seuil du projet disqualifie l'option. */
    @Column(nullable = false)
    private boolean elim;

    @Column(nullable = false)
    private Integer position;
}
