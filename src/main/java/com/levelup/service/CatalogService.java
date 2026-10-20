package com.levelup.service;

import com.levelup.dto.ApiDtos.*;
import com.levelup.entity.*;
import com.levelup.exception.ResourceNotFoundException;
import com.levelup.repository.*;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CatalogService {
    private final UserRepository users;
    private final ExerciseRepository exercises;
    public CatalogService(UserRepository users, ExerciseRepository exercises) { this.users = users; this.exercises = exercises; }
    public UserResponse createUser(UserRequest request) { return UserResponse.from(users.save(new User(request.name(), request.email()))); }
    public ExerciseResponse createExercise(ExerciseRequest request) {
        return ExerciseResponse.from(exercises.save(new Exercise(request.name(), request.description(), request.muscleGroup(), request.equipment(), request.movementType(), request.compound())));
    }
    @Transactional(readOnly = true)
    public List<ExerciseResponse> exercises() { return exercises.findAllByOrderByNameAsc().stream().map(ExerciseResponse::from).toList(); }
    public User user(UUID id) { return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id)); }
    public Exercise exercise(UUID id) { return exercises.findById(id).orElseThrow(() -> new ResourceNotFoundException("Exercise", id)); }
}
