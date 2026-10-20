package com.levelup.pr;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.levelup.analytics.OneRepMaxCalculator;
import com.levelup.entity.*;
import com.levelup.repository.PersonalRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PRAnalysisServiceTest {
    private final PersonalRecordRepository repository = mock(PersonalRecordRepository.class);
    private final PRAnalysisService service = new PRAnalysisService(repository, new OneRepMaxCalculator());

    @Test void detectsRepPrWithoutWeightPr() {
        User user = new User("Test", "test@example.com");
        Exercise exercise = new Exercise("Bench Press", null, null, null, null, true);
        Workout workout = new Workout(user, LocalDate.now(), null, null, null, null);
        ExerciseSession session = new ExerciseSession(workout, exercise, 1, null);
        WorkoutSet set = new WorkoutSet(session, 2, new BigDecimal("100"), 6, null, null, false, false);
        when(repository.findTopByUserIdAndExerciseIdAndTypeOrderByValueDesc(nullable(java.util.UUID.class), nullable(java.util.UUID.class), any())).thenAnswer(invocation -> {
            PRType type = invocation.getArgument(2);
            BigDecimal value = type == PRType.MAX_WEIGHT ? new BigDecimal("100") : type == PRType.MAX_REPS ? new BigDecimal("5") :
                    type == PRType.ESTIMATED_ONE_REP_MAX ? new BigDecimal("120") : new BigDecimal("600");
            return Optional.of(new PersonalRecord(user, exercise, set, type, value, null));
        });
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var prs = service.analyze(set);

        assertTrue(prs.stream().anyMatch(pr -> pr.getType() == PRType.MAX_REPS));
        assertFalse(prs.stream().anyMatch(pr -> pr.getType() == PRType.MAX_WEIGHT));
    }

    @Test void warmupNeverCreatesRecords() {
        WorkoutSet warmup = new WorkoutSet(new ExerciseSession(new Workout(new User("Test", "t@example.com"), LocalDate.now(), null, null, null, null),
                new Exercise("Squat", null, null, null, null, true), 1, null), 1, BigDecimal.TEN, 5, null, null, true, false);
        assertTrue(service.analyze(warmup).isEmpty());
        verifyNoInteractions(repository);
    }
}
