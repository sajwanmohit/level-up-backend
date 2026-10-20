package com.levelup.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.levelup.dto.ApiDtos.*;
import com.levelup.entity.*;
import com.levelup.pr.PRAnalysisService;
import com.levelup.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;

class WorkoutServiceTest {
    private final WorkoutRepository workouts = mock(WorkoutRepository.class);
    private final ExerciseSessionRepository sessions = mock(ExerciseSessionRepository.class);
    private final WorkoutSetRepository sets = mock(WorkoutSetRepository.class);
    private final PersonalRecordRepository records = mock(PersonalRecordRepository.class);
    private final CatalogService catalog = mock(CatalogService.class);
    private final PRAnalysisService prAnalysis = mock(PRAnalysisService.class);
    private final WorkoutService service = new WorkoutService(workouts, sessions, sets, records, catalog, prAnalysis);

    @Test void createsWorkoutForExistingUser() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(catalog.user(userId)).thenReturn(user);
        when(workouts.save(any(Workout.class))).thenAnswer(call -> call.getArgument(0));

        WorkoutResponse response = service.create(new WorkoutRequest(userId, LocalDate.of(2026, 10, 20), null, null, "Leg day", null));

        assertEquals(userId, response.userId());
        assertEquals(LocalDate.of(2026, 10, 20), response.workoutDate());
    }

    @Test void createsSetAndReturnsDetectedRecords() {
        UUID sessionId = UUID.randomUUID();
        ExerciseSession session = new ExerciseSession(new Workout(new User("Athlete", "athlete@example.com"), LocalDate.now(), null, null, null, null),
                new Exercise("Squat", null, null, null, null, true), 1, null);
        when(sessions.findById(sessionId)).thenReturn(Optional.of(session));
        when(sets.save(any(WorkoutSet.class))).thenAnswer(call -> call.getArgument(0));
        when(prAnalysis.analyze(any(WorkoutSet.class))).thenReturn(List.of());

        SetResponse response = service.addSet(sessionId, new SetRequest(1, new BigDecimal("85"), 8, new BigDecimal("8"), new BigDecimal("2"), false, false));

        assertEquals(1, response.setNumber());
        assertEquals(new BigDecimal("85"), response.weight());
        assertEquals(8, response.reps());
        verify(prAnalysis).analyze(any(WorkoutSet.class));
    }

    @Test void rejectsAddingASetToCompletedWorkout() {
        UUID sessionId = UUID.randomUUID();
        ExerciseSession session = new ExerciseSession(new Workout(new User("Athlete", "athlete@example.com"), LocalDate.now(), null,
                java.time.Instant.now(), null, null), new Exercise("Squat", null, null, null, null, true), 1, null);
        when(sessions.findById(sessionId)).thenReturn(Optional.of(session));
        SetRequest request = new SetRequest(1, BigDecimal.ONE, 1, null, null, false, false);

        assertThrows(IllegalArgumentException.class, () -> service.addSet(sessionId, request));
        verifyNoInteractions(sets);
    }
}
