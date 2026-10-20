package com.levelup.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "workout_set", uniqueConstraints = @UniqueConstraint(name = "uk_set_session_number", columnNames = {"exercise_session_id", "set_number"}), indexes = @Index(name = "idx_set_session", columnList = "exercise_session_id, set_number"))
public class WorkoutSet {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "exercise_session_id", nullable = false) private ExerciseSession exerciseSession;
    @Column(name = "set_number", nullable = false) private int setNumber;
    @Column(nullable = false, precision = 8, scale = 2) private BigDecimal weight;
    @Column(nullable = false) private int reps;
    @Column(precision = 4, scale = 2) private BigDecimal rpe;
    @Column(precision = 4, scale = 2) private BigDecimal rir;
    @Column(name = "is_warmup", nullable = false) private boolean warmup;
    @Column(name = "is_failure", nullable = false) private boolean failure;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected WorkoutSet() {}
    public WorkoutSet(ExerciseSession session, int setNumber, BigDecimal weight, int reps, BigDecimal rpe, BigDecimal rir, boolean warmup, boolean failure) {
        this.exerciseSession = session; this.setNumber = setNumber; this.weight = weight; this.reps = reps;
        this.rpe = rpe; this.rir = rir; this.warmup = warmup; this.failure = failure;
    }
    @PrePersist void createTimestamp() { createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public ExerciseSession getExerciseSession() { return exerciseSession; }
    public int getSetNumber() { return setNumber; }
    public BigDecimal getWeight() { return weight; }
    public int getReps() { return reps; }
    public BigDecimal getRpe() { return rpe; }
    public BigDecimal getRir() { return rir; }
    public boolean isWarmup() { return warmup; }
    public boolean isFailure() { return failure; }
    public Instant getCreatedAt() { return createdAt; }
    public void update(BigDecimal weight, int reps, BigDecimal rpe, BigDecimal rir, boolean warmup, boolean failure) {
        this.weight = weight; this.reps = reps; this.rpe = rpe; this.rir = rir; this.warmup = warmup; this.failure = failure;
    }
}
