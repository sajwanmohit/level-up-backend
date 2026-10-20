package com.levelup.controller;

import com.levelup.dto.ApiDtos.*;
import com.levelup.service.WorkoutService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class WorkoutController {
    private final WorkoutService service;
    public WorkoutController(WorkoutService service) { this.service = service; }
    @GetMapping("/workouts")
    public List<WorkoutResponse> list(@RequestParam UUID userId, @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) { return service.list(userId, from, to); }
    @GetMapping("/workouts/{id}")
    public WorkoutDetail get(@PathVariable UUID id) { return service.detail(id); }
    @PostMapping("/workouts")
    public ResponseEntity<WorkoutResponse> create(@Valid @RequestBody WorkoutRequest request) {
        WorkoutResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/workouts/" + response.id())).body(response);
    }
    @PutMapping("/workouts/{id}")
    public WorkoutResponse update(@PathVariable UUID id, @Valid @RequestBody WorkoutRequest request) { return service.update(id, request); }
    @DeleteMapping("/workouts/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { service.delete(id); return ResponseEntity.noContent().build(); }
    @PostMapping("/workouts/{id}/exercises")
    public ResponseEntity<UUID> addExercise(@PathVariable UUID id, @Valid @RequestBody SessionRequest request) {
        UUID sessionId = service.addExercise(id, request);
        return ResponseEntity.created(URI.create("/api/v1/exercise-sessions/" + sessionId)).body(sessionId);
    }
    @DeleteMapping("/workouts/{id}/exercises/{exerciseSessionId}")
    public ResponseEntity<Void> removeExercise(@PathVariable UUID id, @PathVariable UUID exerciseSessionId) {
        service.removeExercise(id, exerciseSessionId); return ResponseEntity.noContent().build();
    }
    @PostMapping("/exercise-sessions/{id}/sets")
    public ResponseEntity<SetResponse> addSet(@PathVariable UUID id, @Valid @RequestBody SetRequest request) {
        SetResponse response = service.addSet(id, request);
        return ResponseEntity.created(URI.create("/api/v1/sets/" + response.id())).body(response);
    }
    @PutMapping("/sets/{id}")
    public SetResponse updateSet(@PathVariable UUID id, @Valid @RequestBody SetRequest request) { return service.updateSet(id, request); }
    @DeleteMapping("/sets/{id}")
    public ResponseEntity<Void> deleteSet(@PathVariable UUID id) { service.deleteSet(id); return ResponseEntity.noContent().build(); }
}
