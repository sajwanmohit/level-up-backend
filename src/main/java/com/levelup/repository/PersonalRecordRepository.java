package com.levelup.repository;

import com.levelup.entity.PersonalRecord;
import com.levelup.entity.PRType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalRecordRepository extends JpaRepository<PersonalRecord, UUID> {
    List<PersonalRecord> findByUserIdOrderByAchievedAtDesc(UUID userId);
    List<PersonalRecord> findByUserIdAndExerciseIdOrderByAchievedAtDesc(UUID userId, UUID exerciseId);
    Optional<PersonalRecord> findTopByUserIdAndExerciseIdAndTypeOrderByValueDesc(UUID userId, UUID exerciseId, PRType type);
    long countByUserIdAndExerciseId(UUID userId, UUID exerciseId);
    List<PersonalRecord> findByWorkoutSetExerciseSessionWorkoutId(UUID workoutId);
    List<PersonalRecord> findByWorkoutSetId(UUID setId);
}
