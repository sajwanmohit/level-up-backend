package com.levelup.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "personal_record", indexes = @Index(name = "idx_pr_user_exercise_date", columnList = "user_id, exercise_id, achieved_at"))
public class PersonalRecord {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "exercise_id", nullable = false) private Exercise exercise;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "workout_set_id", nullable = false) private WorkoutSet workoutSet;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private PRType type;
    @Column(nullable = false, precision = 12, scale = 4) private BigDecimal value;
    @Column(name = "achieved_at", nullable = false) private Instant achievedAt;
    protected PersonalRecord() {}
    public PersonalRecord(User user, Exercise exercise, WorkoutSet set, PRType type, BigDecimal value, Instant achievedAt) {
        this.user = user; this.exercise = exercise; this.workoutSet = set; this.type = type; this.value = value; this.achievedAt = achievedAt;
    }
    @PrePersist void setTimestamp() { if (achievedAt == null) achievedAt = Instant.now(); }
    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Exercise getExercise() { return exercise; }
    public WorkoutSet getWorkoutSet() { return workoutSet; }
    public PRType getType() { return type; }
    public BigDecimal getValue() { return value; }
    public Instant getAchievedAt() { return achievedAt; }
}
