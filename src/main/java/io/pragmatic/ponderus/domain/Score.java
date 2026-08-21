package io.pragmatic.ponderus.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Croisement critère × option : c'est cette table qui permet de passer
 * de 2 options fixes à N options sans changer le modèle.
 * value est nullable pour distinguer "pas encore noté" de "noté à 0"
 * (important pour ne pas fausser le calcul de sensibilité).
 */
@Entity
@Table(
    name = "scores",
    uniqueConstraints = @UniqueConstraint(columnNames = {"criterion_id", "option_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class Score {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private Criterion criterion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private Option option;

    private BigDecimal value;

    private String note;
}
