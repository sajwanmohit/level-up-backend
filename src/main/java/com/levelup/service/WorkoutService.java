package com.levelup.service;

import com.levelup.dto.ApiDtos.*;
import com.levelup.entity.*;
import com.levelup.exception.ResourceNotFoundException;
import com.levelup.pr.PRAnalysisService;
import com.levelup.repository.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkoutService {
    private final WorkoutRepository workouts;
    private final ExerciseSessionRepository sessions;
    private final WorkoutSetRepository sets;
    private final PersonalRecordRepository records;
    private final CatalogService catalog;
    private final PRAnalysisService prAnalysis;
    public WorkoutService(WorkoutRepository workouts, ExerciseSessionRepository sessions, WorkoutSetRepository sets,
                          PersonalRecordRepository records, CatalogService catalog, PRAnalysisService prAnalysis) {
        this.workouts = workouts; this.sessions = sessions; this.sets = sets; this.records = records; this.catalog = catalog; this.prAnalysis = prAnalysis;
    }
    public WorkoutResponse create(WorkoutRequest r) {
        validateTimes(r.startedAt(), r.completedAt());
        return WorkoutResponse.from(workouts.save(new Workout(catalog.user(r.userId()), r.workoutDate(), r.startedAt(), r.completedAt(), r.name(), r.notes())));
    }
    public WorkoutResponse update(UUID id, WorkoutRequest r) {
        Workout workout = workout(id);
        ensureOpen(workout);
        if (!workout.getUser().getId().equals(r.userId())) throw new IllegalArgumentException("Workout owner cannot be changed");
        validateTimes(r.startedAt(), r.completedAt());
        workout.update(r.workoutDate(), r.startedAt(), r.completedAt(), r.name(), r.notes());
        return WorkoutResponse.from(workout);
    }
    @Transactional(readOnly = true)
    public List<WorkoutResponse> list(UUID userId, LocalDate from, LocalDate to) {
        if ((from == null) != (to == null) || (from != null && to.isBefore(from))) throw new IllegalArgumentException("Provide a valid from/to date range");
        List<Workout> result = from != null ? workouts.findByUserIdAndWorkoutDateBetweenOrderByWorkoutDateDesc(userId, from, to) : workouts.findByUserIdOrderByWorkoutDateDesc(userId);
        return result.stream().map(WorkoutResponse::from).toList();
    }
    @Transactional(readOnly = true)
    public WorkoutResponse get(UUID id) { return WorkoutResponse.from(workout(id)); }
    @Transactional(readOnly = true)
    public WorkoutDetail detail(UUID id) {
        Workout w = workout(id);
        List<ExerciseSessionResponse> exerciseDetails = sessions.findByWorkoutIdOrderByExerciseOrder(id).stream().map(s ->
                new ExerciseSessionResponse(s.getId(), s.getExercise().getId(), s.getExercise().getName(), s.getExerciseOrder(), s.getNotes(),
                        sets.findByExerciseSessionIdOrderBySetNumber(s.getId()).stream().map(set -> SetResponse.from(set, List.of())).toList())).toList();
        return new WorkoutDetail(WorkoutResponse.from(w), exerciseDetails);
    }
    public void delete(UUID id) {
        workout(id);
        for (ExerciseSession session : sessions.findByWorkoutIdOrderByExerciseOrder(id)) {
            List<WorkoutSet> sessionSets = sets.findByExerciseSessionIdOrderBySetNumber(session.getId());
            sessionSets.forEach(s -> records.deleteAll(records.findByWorkoutSetId(s.getId())));
            sets.deleteAll(sessionSets); sessions.delete(session);
        }
        workouts.deleteById(id);
    }
    public UUID addExercise(UUID workoutId, SessionRequest r) {
        Workout workout = workout(workoutId); ensureOpen(workout);
        return sessions.save(new ExerciseSession(workout, catalog.exercise(r.exerciseId()), r.exerciseOrder(), r.notes())).getId();
    }
    public void removeExercise(UUID workoutId, UUID sessionId) {
        Workout workout = workout(workoutId); ensureOpen(workout);
        ExerciseSession session = sessions.findById(sessionId).orElseThrow(() -> new ResourceNotFoundException("Exercise session", sessionId));
        if (!session.getWorkout().getId().equals(workoutId)) throw new IllegalArgumentException("Exercise session does not belong to workout");
        for (WorkoutSet s : sets.findByExerciseSessionIdOrderBySetNumber(sessionId)) { records.deleteAll(records.findByWorkoutSetId(s.getId())); sets.delete(s); }
        sessions.delete(session);
    }
    public SetResponse addSet(UUID sessionId, SetRequest r) {
        ExerciseSession session = sessions.findById(sessionId).orElseThrow(() -> new ResourceNotFoundException("Exercise session", sessionId));
        ensureOpen(session.getWorkout());
        WorkoutSet set = sets.save(new WorkoutSet(session, r.setNumber(), r.weight(), r.reps(), r.rpe(), r.rir(), r.warmup(), r.failure()));
        return SetResponse.from(set, prAnalysis.analyze(set));
    }
    public SetResponse updateSet(UUID id, SetRequest r) {
        WorkoutSet set = sets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Workout set", id));
        ensureOpen(set.getExerciseSession().getWorkout());
        records.deleteAll(records.findByWorkoutSetId(id));
        set.update(r.weight(), r.reps(), r.rpe(), r.rir(), r.warmup(), r.failure());
        return SetResponse.from(set, prAnalysis.analyze(set));
    }
    public void deleteSet(UUID id) {
        WorkoutSet set = sets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Workout set", id));
        ensureOpen(set.getExerciseSession().getWorkout()); records.deleteAll(records.findByWorkoutSetId(id)); sets.delete(set);
    }
    public Workout workout(UUID id) { return workouts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Workout", id)); }
    private void ensureOpen(Workout workout) { if (workout.getCompletedAt() != null) throw new IllegalArgumentException("Completed workouts cannot be changed"); }
    private void validateTimes(java.time.Instant started, java.time.Instant completed) {
        if (started != null && completed != null && completed.isBefore(started)) throw new IllegalArgumentException("completedAt must not be before startedAt");
    }
}
