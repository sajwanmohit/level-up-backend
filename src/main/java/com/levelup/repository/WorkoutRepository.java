package com.levelup.repository;

import com.levelup.entity.Workout;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutRepository extends JpaRepository<Workout, UUID> {
    List<Workout> findByUserIdAndWorkoutDateBetweenOrderByWorkoutDateDesc(UUID userId, LocalDate from, LocalDate to);
    List<Workout> findByUserIdOrderByWorkoutDateDesc(UUID userId);
}
