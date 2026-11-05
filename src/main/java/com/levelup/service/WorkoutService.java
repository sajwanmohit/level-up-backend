package com.levelup.service;

import com.levelup.dto.ApiDtos.*;
import com.levelup.entity.*;
import com.levelup.exception.ResourceNotFoundException;
import com.levelup.pr.PRAnalysisService;
import com.levelup.repository.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
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
    public WorkoutResponse create(WorkoutRequest r, UUID authenticatedUserId) {
        requireOwner(r.userId(), authenticatedUserId);
        validateTimes(r.startedAt(), r.completedAt());
        return WorkoutResponse.from(workouts.save(new Workout(catalog.user(authenticatedUserId), r.workoutDate(), r.startedAt(), r.completedAt(), r.name(), r.notes())));
    }
    public WorkoutResponse update(UUID id, WorkoutRequest r, UUID authenticatedUserId) {
        Workout workout = workout(id, authenticatedUserId);
        ensureOpen(workout);
        requireOwner(r.userId(), authenticatedUserId);
        validateTimes(r.startedAt(), r.completedAt());
        workout.update(r.workoutDate(), r.startedAt(), r.completedAt(), r.name(), r.notes());
        return WorkoutResponse.from(workout);
    }
    @Transactional(readOnly = true)
    public List<WorkoutResponse> list(UUID userId, UUID authenticatedUserId, LocalDate from, LocalDate to) {
        requireOwner(userId, authenticatedUserId);
        if ((from == null) != (to == null) || (from != null && to.isBefore(from))) throw new IllegalArgumentException("Provide a valid from/to date range");
        List<Workout> result = from != null ? workouts.findByUserIdAndWorkoutDateBetweenOrderByWorkoutDateDesc(userId, from, to) : workouts.findByUserIdOrderByWorkoutDateDesc(userId);
        return result.stream().map(WorkoutResponse::from).toList();
    }
    @Transactional(readOnly = true)
    public WorkoutDetail detail(UUID id, UUID authenticatedUserId) {
        Workout w = workout(id, authenticatedUserId);
        List<ExerciseSessionResponse> exerciseDetails = sessions.findByWorkoutIdOrderByExerciseOrder(id).stream().map(s ->
                new ExerciseSessionResponse(s.getId(), s.getExercise().getId(), s.getExercise().getName(), s.getExerciseOrder(), s.getNotes(),
                        sets.findByExerciseSessionIdOrderBySetNumber(s.getId()).stream().map(set -> SetResponse.from(set, List.of())).toList())).toList();
        return new WorkoutDetail(WorkoutResponse.from(w), exerciseDetails);
    }
    public void delete(UUID id, UUID authenticatedUserId) {
        workout(id, authenticatedUserId);
        for (ExerciseSession session : sessions.findByWorkoutIdOrderByExerciseOrder(id)) {
            List<WorkoutSet> sessionSets = sets.findByExerciseSessionIdOrderBySetNumber(session.getId());
            sessionSets.forEach(s -> records.deleteAll(records.findByWorkoutSetId(s.getId())));
            sets.deleteAll(sessionSets); sessions.delete(session);
        }
        workouts.deleteById(id);
    }
    public UUID addExercise(UUID workoutId, SessionRequest r, UUID authenticatedUserId) {
        Workout workout = workout(workoutId, authenticatedUserId); ensureOpen(workout);
        return sessions.save(new ExerciseSession(workout, catalog.exercise(r.exerciseId()), r.exerciseOrder(), r.notes())).getId();
    }
    public void removeExercise(UUID workoutId, UUID sessionId, UUID authenticatedUserId) {
        Workout workout = workout(workoutId, authenticatedUserId); ensureOpen(workout);
        ExerciseSession session = sessions.findById(sessionId).orElseThrow(() -> new ResourceNotFoundException("Exercise session", sessionId));
        if (!session.getWorkout().getId().equals(workoutId)) throw new IllegalArgumentException("Exercise session does not belong to workout");
        for (WorkoutSet s : sets.findByExerciseSessionIdOrderBySetNumber(sessionId)) { records.deleteAll(records.findByWorkoutSetId(s.getId())); sets.delete(s); }
        sessions.delete(session);
    }
    public SetResponse addSet(UUID sessionId, SetRequest r, UUID authenticatedUserId) {
        ExerciseSession session = sessions.findById(sessionId).orElseThrow(() -> new ResourceNotFoundException("Exercise session", sessionId));
        requireOwner(session.getWorkout().getUser().getId(), authenticatedUserId);
        ensureOpen(session.getWorkout());
        WorkoutSet set = sets.save(new WorkoutSet(session, r.setNumber(), r.weight(), r.reps(), r.rpe(), r.rir(), r.warmup(), r.failure()));
        return SetResponse.from(set, prAnalysis.analyze(set));
    }
    public SetResponse updateSet(UUID id, SetRequest r, UUID authenticatedUserId) {
        WorkoutSet set = sets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Workout set", id));
        requireOwner(set.getExerciseSession().getWorkout().getUser().getId(), authenticatedUserId);
        ensureOpen(set.getExerciseSession().getWorkout());
        records.deleteAll(records.findByWorkoutSetId(id));
        set.update(r.weight(), r.reps(), r.rpe(), r.rir(), r.warmup(), r.failure());
        return SetResponse.from(set, prAnalysis.analyze(set));
    }
    public void deleteSet(UUID id, UUID authenticatedUserId) {
        WorkoutSet set = sets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Workout set", id));
        requireOwner(set.getExerciseSession().getWorkout().getUser().getId(), authenticatedUserId);
        ensureOpen(set.getExerciseSession().getWorkout()); records.deleteAll(records.findByWorkoutSetId(id)); sets.delete(set);
    }
    private Workout workout(UUID id, UUID authenticatedUserId) {
        Workout workout = workouts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Workout", id));
        requireOwner(workout.getUser().getId(), authenticatedUserId);
        return workout;
    }
    private void requireOwner(UUID ownerId, UUID authenticatedUserId) {
        if (!ownerId.equals(authenticatedUserId)) throw new AccessDeniedException("User does not own this resource");
    }
    private void ensureOpen(Workout workout) { if (workout.getCompletedAt() != null) throw new IllegalArgumentException("Completed workouts cannot be changed"); }
    private void validateTimes(java.time.Instant started, java.time.Instant completed) {
        if (started != null && completed != null && completed.isBefore(started)) throw new IllegalArgumentException("completedAt must not be before startedAt");
    }
}
