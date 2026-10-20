package com.levelup.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "workout", indexes = @Index(name = "idx_workout_user_date", columnList = "user_id, workout_date"))
public class Workout {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "workout_date", nullable = false) private LocalDate workoutDate;
    @Column(name = "started_at") private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(length = 120) private String name;
    @Column(length = 2000) private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected Workout() {}
    public Workout(User user, LocalDate workoutDate, Instant startedAt, Instant completedAt, String name, String notes) {
        this.user = user; this.workoutDate = workoutDate; this.startedAt = startedAt;
        this.completedAt = completedAt; this.name = name; this.notes = notes;
    }
    @PrePersist void createTimestamps() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void updateTimestamp() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public User getUser() { return user; }
    public LocalDate getWorkoutDate() { return workoutDate; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getName() { return name; }
    public String getNotes() { return notes; }
    public void update(LocalDate date, Instant started, Instant completed, String name, String notes) {
        if (completed != null && started != null && completed.isBefore(started)) throw new IllegalArgumentException("completedAt must not be before startedAt");
        this.workoutDate = date; this.startedAt = started; this.completedAt = completed; this.name = name; this.notes = notes;
    }
}
