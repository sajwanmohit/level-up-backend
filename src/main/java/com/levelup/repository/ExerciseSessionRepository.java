package com.levelup.repository;

import com.levelup.entity.ExerciseSession;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseSessionRepository extends JpaRepository<ExerciseSession, UUID> {
    List<ExerciseSession> findByWorkoutIdOrderByExerciseOrder(UUID workoutId);
    List<ExerciseSession> findByExerciseIdAndWorkoutUserIdOrderByWorkoutWorkoutDateAsc(UUID exerciseId, UUID userId);
    int countByWorkoutId(UUID workoutId);
}
