package com.levelup.entity;

import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "exercise_session", uniqueConstraints = @UniqueConstraint(name = "uk_session_workout_order", columnNames = {"workout_id", "exercise_order"}), indexes = @Index(name = "idx_session_exercise_workout", columnList = "exercise_id, workout_id"))
public class ExerciseSession {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "workout_id", nullable = false) private Workout workout;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "exercise_id", nullable = false) private Exercise exercise;
    @Column(name = "exercise_order", nullable = false) private int exerciseOrder;
    @Column(length = 1000) private String notes;
    protected ExerciseSession() {}
    public ExerciseSession(Workout workout, Exercise exercise, int exerciseOrder, String notes) {
        this.workout = workout; this.exercise = exercise; this.exerciseOrder = exerciseOrder; this.notes = notes;
    }
    public UUID getId() { return id; }
    public Workout getWorkout() { return workout; }
    public Exercise getExercise() { return exercise; }
    public int getExerciseOrder() { return exerciseOrder; }
    public String getNotes() { return notes; }
}
