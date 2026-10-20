package com.levelup.analytics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.levelup.entity.*;
import com.levelup.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;

class WorkoutAnalyticsServiceTest {
    private final WorkoutRepository workouts = mock(WorkoutRepository.class);
    private final ExerciseSessionRepository sessions = mock(ExerciseSessionRepository.class);
    private final WorkoutSetRepository sets = mock(WorkoutSetRepository.class);
    private final PersonalRecordRepository records = mock(PersonalRecordRepository.class);
    private final ExerciseRepository exercises = mock(ExerciseRepository.class);
    private final WorkoutAnalyticsService service = new WorkoutAnalyticsService(workouts, sessions, sets, records, exercises, new OneRepMaxCalculator());
    private final UUID workoutId = UUID.randomUUID();
    private final UUID exerciseId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test void dailyAnalysisExcludesWarmupVolumeAndCountsWorkingMetrics() {
        Exercise exercise = exercise();
        Workout workout = new Workout(new User("Athlete", "athlete@example.com"), LocalDate.of(2026, 10, 1), null, java.time.Instant.now(), "Push", null);
        ExerciseSession session = new ExerciseSession(workout, exercise, 1, null);
        when(workouts.findById(workoutId)).thenReturn(Optional.of(workout));
        when(sessions.findByWorkoutIdOrderByExerciseOrder(workoutId)).thenReturn(List.of(session));
        when(sets.findByExerciseSessionIdOrderBySetNumber(nullable(UUID.class))).thenReturn(List.of(
                new WorkoutSet(session, 1, new BigDecimal("40"), 10, null, null, true, false),
                new WorkoutSet(session, 2, new BigDecimal("60"), 8, new BigDecimal("8"), new BigDecimal("2"), false, false)));
        when(records.findByWorkoutSetExerciseSessionWorkoutId(workoutId)).thenReturn(List.of());

        var analysis = service.daily(workoutId);

        assertEquals(1, analysis.totalExercises());
        assertEquals(1, analysis.totalWorkingSets());
        assertEquals(1, analysis.totalWarmupSets());
        assertEquals(8, analysis.totalReps());
        assertEquals(new BigDecimal("480"), analysis.totalVolume());
        assertEquals(new BigDecimal("480"), analysis.exercises().getFirst().volume());
        assertEquals(new BigDecimal("8.00"), analysis.averageRpe());
    }

    @Test void dailyAnalysisRequiresCompletedWorkout() {
        Workout workout = new Workout(new User("Athlete", "athlete@example.com"), LocalDate.now(), null, null, null, null);
        when(workouts.findById(workoutId)).thenReturn(Optional.of(workout));
        assertThrows(IllegalArgumentException.class, () -> service.daily(workoutId));
    }

    @Test void progressionAggregatesHistoryAndLatestEstimatedOneRepMax() {
        Exercise exercise = exercise();
        Workout firstWorkout = new Workout(new User("Athlete", "athlete@example.com"), LocalDate.of(2026, 10, 1), null, null, null, null);
        Workout secondWorkout = new Workout(new User("Athlete", "athlete@example.com"), LocalDate.of(2026, 10, 8), null, null, null, null);
        ExerciseSession first = new ExerciseSession(firstWorkout, exercise, 1, null);
        ExerciseSession second = new ExerciseSession(secondWorkout, exercise, 1, null);
        when(exercises.findById(exerciseId)).thenReturn(Optional.of(exercise));
        when(sessions.findByExerciseIdAndWorkoutUserIdOrderByWorkoutWorkoutDateAsc(exerciseId, userId)).thenReturn(List.of(first, second));
        when(sets.findByExerciseSessionIdOrderBySetNumber(nullable(UUID.class))).thenReturn(
                List.of(new WorkoutSet(first, 1, new BigDecimal("80"), 5, null, null, false, false)),
                List.of(new WorkoutSet(second, 1, new BigDecimal("90"), 3, null, null, false, false)));
        when(records.countByUserIdAndExerciseId(userId, exerciseId)).thenReturn(1L);

        var result = service.exercise(exerciseId, userId);

        assertEquals(2, result.summary().sessions());
        assertEquals(2, result.summary().totalSets());
        assertEquals(new BigDecimal("670"), result.summary().totalVolume());
        assertEquals(new BigDecimal("90"), result.summary().bestWeight());
        assertEquals(5, result.summary().bestReps());
        assertEquals(new BigDecimal("99.00"), result.summary().latestEstimatedOneRepMax());
        assertEquals(2, result.history().size());
    }

    private Exercise exercise() { return new Exercise("Bench Press", null, "Chest", "Barbell", "PUSH", true); }
}
