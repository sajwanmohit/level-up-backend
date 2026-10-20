package com.levelup.dto;

import com.levelup.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class ApiDtos {
    private ApiDtos() {}
    public record UserRequest(@NotBlank @Size(max = 120) String name, @NotBlank @Email @Size(max = 254) String email) {}
    public record UserResponse(UUID id, String name, String email) {
        public static UserResponse from(User u) { return new UserResponse(u.getId(), u.getName(), u.getEmail()); }
    }
    public record ExerciseRequest(@NotBlank @Size(max = 120) String name, @Size(max = 1000) String description,
                                  @Size(max = 80) String muscleGroup, @Size(max = 80) String equipment,
                                  @Size(max = 40) String movementType, boolean compound) {}
    public record ExerciseResponse(UUID id, String name, String description, String muscleGroup, String equipment, String movementType, boolean compound) {
        public static ExerciseResponse from(Exercise e) { return new ExerciseResponse(e.getId(), e.getName(), e.getDescription(), e.getMuscleGroup(), e.getEquipment(), e.getMovementType(), e.isCompound()); }
    }
    public record WorkoutRequest(@NotNull UUID userId, @NotNull LocalDate workoutDate, Instant startedAt, Instant completedAt,
                                 @Size(max = 120) String name, @Size(max = 2000) String notes) {}
    public record WorkoutResponse(UUID id, UUID userId, LocalDate workoutDate, Instant startedAt, Instant completedAt, String name, String notes) {
        public static WorkoutResponse from(Workout w) { return new WorkoutResponse(w.getId(), w.getUser().getId(), w.getWorkoutDate(), w.getStartedAt(), w.getCompletedAt(), w.getName(), w.getNotes()); }
    }
    public record ExerciseSessionResponse(UUID id, UUID exerciseId, String exerciseName, int exerciseOrder, String notes, List<SetResponse> sets) {}
    public record WorkoutDetail(WorkoutResponse workout, List<ExerciseSessionResponse> exercises) {}
    public record SessionRequest(@NotNull UUID exerciseId, @Positive int exerciseOrder, @Size(max = 1000) String notes) {}
    public record SetRequest(@Positive int setNumber, @NotNull @DecimalMin("0.00") @Digits(integer=6, fraction=2) BigDecimal weight,
                             @Positive int reps, @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer=2, fraction=2) BigDecimal rpe,
                             @DecimalMin("0.0") @Digits(integer=2, fraction=2) BigDecimal rir, boolean warmup, boolean failure) {}
    public record SetResponse(UUID id, UUID exerciseSessionId, int setNumber, BigDecimal weight, int reps, BigDecimal rpe, BigDecimal rir,
                              boolean warmup, boolean failure, List<PRResponse> prs) {
        public static SetResponse from(WorkoutSet s, List<PersonalRecord> prs) {
            return new SetResponse(s.getId(), s.getExerciseSession().getId(), s.getSetNumber(), s.getWeight(), s.getReps(), s.getRpe(), s.getRir(), s.isWarmup(), s.isFailure(), prs.stream().map(PRResponse::from).toList());
        }
    }
    public record PRResponse(UUID id, UUID userId, UUID exerciseId, String exerciseName, UUID workoutSetId, PRType type, BigDecimal value, Instant achievedAt) {
        public static PRResponse from(PersonalRecord p) { return new PRResponse(p.getId(), p.getUser().getId(), p.getExercise().getId(), p.getExercise().getName(), p.getWorkoutSet().getId(), p.getType(), p.getValue(), p.getAchievedAt()); }
    }
    public record ExerciseBreakdown(UUID exerciseId, String exerciseName, int sets, int workingSets, int reps, BigDecimal volume, BigDecimal averageRpe) {}
    public record DailyAnalysis(UUID workoutId, LocalDate date, int totalExercises, int totalWorkingSets, int totalReps, BigDecimal totalVolume,
                                int totalWarmupSets, BigDecimal averageRpe, BigDecimal averageRir, List<ExerciseBreakdown> exercises,
                                List<PRResponse> prs, List<OneRmChange> estimatedOneRepMaxChanges) {}
    public record OneRmChange(UUID exerciseId, String exerciseName, BigDecimal estimatedOneRepMax) {}
    public record ExerciseSummary(long sessions, long totalSets, BigDecimal totalVolume, BigDecimal bestWeight,
                                  Integer bestReps, BigDecimal bestEstimatedOneRepMax, BigDecimal latestEstimatedOneRepMax, long prCount) {}
    public record PerformancePoint(LocalDate date, BigDecimal estimatedOneRepMax, BigDecimal volume) {}
    public record ExerciseAnalytics(ExerciseResponse exercise, ExerciseSummary summary, List<PerformancePoint> history) {}
    public record Dashboard(long totalWorkouts, long totalPrs, BigDecimal totalVolume, long exerciseCount) {}
}
