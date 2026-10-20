package com.levelup.pr;

import com.levelup.analytics.OneRepMaxCalculator;
import com.levelup.entity.*;
import com.levelup.repository.PersonalRecordRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class PRAnalysisService {
    private final PersonalRecordRepository records;
    private final OneRepMaxCalculator calculator;
    public PRAnalysisService(PersonalRecordRepository records, OneRepMaxCalculator calculator) { this.records = records; this.calculator = calculator; }
    public List<PersonalRecord> analyze(WorkoutSet set) {
        if (set.isWarmup()) return List.of();
        Exercise exercise = set.getExerciseSession().getExercise();
        User user = set.getExerciseSession().getWorkout().getUser();
        BigDecimal volume = set.getWeight().multiply(BigDecimal.valueOf(set.getReps()));
        Map<PRType, BigDecimal> candidates = new EnumMap<>(PRType.class);
        candidates.put(PRType.MAX_WEIGHT, set.getWeight());
        candidates.put(PRType.MAX_REPS, BigDecimal.valueOf(set.getReps()));
        candidates.put(PRType.ESTIMATED_ONE_REP_MAX, calculator.calculate(set.getWeight(), set.getReps()));
        candidates.put(PRType.VOLUME, volume);
        List<PersonalRecord> achieved = new ArrayList<>();
        candidates.forEach((type, value) -> {
            BigDecimal best = records.findTopByUserIdAndExerciseIdAndTypeOrderByValueDesc(user.getId(), exercise.getId(), type)
                    .map(PersonalRecord::getValue).orElse(null);
            if (best == null || value.compareTo(best) > 0) achieved.add(records.save(new PersonalRecord(user, exercise, set, type, value, null)));
        });
        return achieved;
    }
}
