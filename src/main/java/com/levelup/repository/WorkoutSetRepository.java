package com.levelup.repository;

import com.levelup.entity.WorkoutSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, UUID> {
    List<WorkoutSet> findByExerciseSessionIdOrderBySetNumber(UUID sessionId);
    List<WorkoutSet> findByExerciseSessionExerciseIdAndExerciseSessionWorkoutUserId(UUID exerciseId, UUID userId);
}
