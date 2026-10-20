package com.levelup.analytics;

import com.levelup.dto.ApiDtos.*;
import com.levelup.entity.*;
import com.levelup.exception.ResourceNotFoundException;
import com.levelup.repository.*;
import java.math.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkoutAnalyticsService {
    private final WorkoutRepository workouts;
    private final ExerciseSessionRepository sessions;
    private final WorkoutSetRepository sets;
    private final PersonalRecordRepository records;
    private final ExerciseRepository exercises;
    private final OneRepMaxCalculator calculator;
    public WorkoutAnalyticsService(WorkoutRepository workouts, ExerciseSessionRepository sessions, WorkoutSetRepository sets,
                                   PersonalRecordRepository records, ExerciseRepository exercises, OneRepMaxCalculator calculator) {
        this.workouts = workouts; this.sessions = sessions; this.sets = sets; this.records = records; this.exercises = exercises; this.calculator = calculator;
    }
    public DailyAnalysis daily(UUID workoutId) {
        Workout workout = workouts.findById(workoutId).orElseThrow(() -> new ResourceNotFoundException("Workout", workoutId));
        if (workout.getCompletedAt() == null) throw new IllegalArgumentException("Workout is not completed");
        List<ExerciseSession> sessionList = sessions.findByWorkoutIdOrderByExerciseOrder(workoutId);
        int workingSets = 0, warmups = 0, totalReps = 0;
        BigDecimal volume = BigDecimal.ZERO;
        List<BigDecimal> rpes = new ArrayList<>(), rirs = new ArrayList<>();
        List<ExerciseBreakdown> breakdowns = new ArrayList<>();
        List<OneRmChange> oneRmChanges = new ArrayList<>();
        for (ExerciseSession session : sessionList) {
            List<WorkoutSet> exerciseSets = sets.findByExerciseSessionIdOrderBySetNumber(session.getId());
            int exerciseReps = 0, exerciseWorking = 0;
            BigDecimal exerciseVolume = BigDecimal.ZERO;
            List<BigDecimal> exerciseRpes = new ArrayList<>();
            BigDecimal bestOneRm = null;
            for (WorkoutSet set : exerciseSets) {
                if (set.isWarmup()) { warmups++; continue; }
                workingSets++; exerciseWorking++; totalReps += set.getReps(); exerciseReps += set.getReps();
                BigDecimal setVolume = set.getWeight().multiply(BigDecimal.valueOf(set.getReps()));
                volume = volume.add(setVolume); exerciseVolume = exerciseVolume.add(setVolume);
                if (set.getRpe() != null) { rpes.add(set.getRpe()); exerciseRpes.add(set.getRpe()); }
                if (set.getRir() != null) rirs.add(set.getRir());
                BigDecimal e1rm = calculator.calculate(set.getWeight(), set.getReps());
                if (bestOneRm == null || e1rm.compareTo(bestOneRm) > 0) bestOneRm = e1rm;
            }
            breakdowns.add(new ExerciseBreakdown(session.getExercise().getId(), session.getExercise().getName(), exerciseSets.size(), exerciseWorking,
                    exerciseReps, exerciseVolume, average(exerciseRpes)));
            if (bestOneRm != null) oneRmChanges.add(new OneRmChange(session.getExercise().getId(), session.getExercise().getName(), bestOneRm));
        }
        return new DailyAnalysis(workoutId, workout.getWorkoutDate(), sessionList.size(), workingSets, totalReps, volume,
                warmups, average(rpes), average(rirs), breakdowns, records.findByWorkoutSetExerciseSessionWorkoutId(workoutId).stream().map(PRResponse::from).toList(), oneRmChanges);
    }
    public ExerciseAnalytics exercise(UUID exerciseId, UUID userId) {
        Exercise exercise = exercises.findById(exerciseId).orElseThrow(() -> new ResourceNotFoundException("Exercise", exerciseId));
        List<ExerciseSession> historySessions = sessions.findByExerciseIdAndWorkoutUserIdOrderByWorkoutWorkoutDateAsc(exerciseId, userId);
        long totalSets = 0; BigDecimal totalVolume = BigDecimal.ZERO, bestWeight = BigDecimal.ZERO, bestOneRm = BigDecimal.ZERO, latestOneRm = BigDecimal.ZERO;
        Integer bestReps = null;
        List<PerformancePoint> points = new ArrayList<>();
        for (ExerciseSession session : historySessions) {
            BigDecimal dayVolume = BigDecimal.ZERO, dayBest = null;
            for (WorkoutSet set : sets.findByExerciseSessionIdOrderBySetNumber(session.getId())) {
                if (set.isWarmup()) continue;
                totalSets++;
                BigDecimal setVolume = set.getWeight().multiply(BigDecimal.valueOf(set.getReps()));
                totalVolume = totalVolume.add(setVolume); dayVolume = dayVolume.add(setVolume);
                if (set.getWeight().compareTo(bestWeight) > 0) bestWeight = set.getWeight();
                if (bestReps == null || set.getReps() > bestReps) bestReps = set.getReps();
                BigDecimal e1rm = calculator.calculate(set.getWeight(), set.getReps());
                if (e1rm.compareTo(bestOneRm) > 0) bestOneRm = e1rm;
                if (dayBest == null || e1rm.compareTo(dayBest) > 0) dayBest = e1rm;
            }
            if (dayBest != null) { latestOneRm = dayBest; points.add(new PerformancePoint(session.getWorkout().getWorkoutDate(), dayBest, dayVolume)); }
        }
        ExerciseSummary summary = new ExerciseSummary(historySessions.size(), totalSets, totalVolume, bestWeight, bestReps,
                bestOneRm, latestOneRm, records.countByUserIdAndExerciseId(userId, exerciseId));
        return new ExerciseAnalytics(ExerciseResponse.from(exercise), summary, points);
    }
    public Dashboard dashboard(UUID userId) {
        List<Workout> userWorkouts = workouts.findByUserIdOrderByWorkoutDateDesc(userId);
        BigDecimal totalVolume = BigDecimal.ZERO;
        Set<UUID> exerciseIds = new HashSet<>();
        for (Workout workout : userWorkouts) for (ExerciseSession session : sessions.findByWorkoutIdOrderByExerciseOrder(workout.getId())) {
            exerciseIds.add(session.getExercise().getId());
            for (WorkoutSet set : sets.findByExerciseSessionIdOrderBySetNumber(session.getId())) if (!set.isWarmup())
                totalVolume = totalVolume.add(set.getWeight().multiply(BigDecimal.valueOf(set.getReps())));
        }
        return new Dashboard(userWorkouts.size(), records.findByUserIdOrderByAchievedAtDesc(userId).size(), totalVolume, exerciseIds.size());
    }
    private BigDecimal average(List<BigDecimal> values) {
        return values.isEmpty() ? null : values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }
}
