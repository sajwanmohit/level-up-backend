package com.levelup.controller;

import com.levelup.dto.ApiDtos.*;
import com.levelup.config.AuthenticatedUser;
import com.levelup.service.WorkoutService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/v1")
public class WorkoutController {
    private final WorkoutService service;
    public WorkoutController(WorkoutService service) { this.service = service; }
    @GetMapping("/workouts")
    public List<WorkoutResponse> list(@RequestParam UUID userId, @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                      @AuthenticationPrincipal Jwt jwt) {
        return service.list(userId, AuthenticatedUser.id(jwt), from, to);
    }
    @GetMapping("/workouts/{id}")
    public WorkoutDetail get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) { return service.detail(id, AuthenticatedUser.id(jwt)); }
    @PostMapping("/workouts")
    public ResponseEntity<WorkoutResponse> create(@Valid @RequestBody WorkoutRequest request, @AuthenticationPrincipal Jwt jwt) {
        WorkoutResponse response = service.create(request, AuthenticatedUser.id(jwt));
        return ResponseEntity.created(URI.create("/api/v1/workouts/" + response.id())).body(response);
    }
    @PutMapping("/workouts/{id}")
    public WorkoutResponse update(@PathVariable UUID id, @Valid @RequestBody WorkoutRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.update(id, request, AuthenticatedUser.id(jwt));
    }
    @DeleteMapping("/workouts/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        service.delete(id, AuthenticatedUser.id(jwt)); return ResponseEntity.noContent().build();
    }
    @PostMapping("/workouts/{id}/exercises")
    public ResponseEntity<UUID> addExercise(@PathVariable UUID id, @Valid @RequestBody SessionRequest request,
                                             @AuthenticationPrincipal Jwt jwt) {
        UUID sessionId = service.addExercise(id, request, AuthenticatedUser.id(jwt));
        return ResponseEntity.created(URI.create("/api/v1/exercise-sessions/" + sessionId)).body(sessionId);
    }
    @DeleteMapping("/workouts/{id}/exercises/{exerciseSessionId}")
    public ResponseEntity<Void> removeExercise(@PathVariable UUID id, @PathVariable UUID exerciseSessionId,
                                                @AuthenticationPrincipal Jwt jwt) {
        service.removeExercise(id, exerciseSessionId, AuthenticatedUser.id(jwt)); return ResponseEntity.noContent().build();
    }
    @PostMapping("/exercise-sessions/{id}/sets")
    public ResponseEntity<SetResponse> addSet(@PathVariable UUID id, @Valid @RequestBody SetRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        SetResponse response = service.addSet(id, request, AuthenticatedUser.id(jwt));
        return ResponseEntity.created(URI.create("/api/v1/sets/" + response.id())).body(response);
    }
    @PutMapping("/sets/{id}")
    public SetResponse updateSet(@PathVariable UUID id, @Valid @RequestBody SetRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.updateSet(id, request, AuthenticatedUser.id(jwt));
    }
    @DeleteMapping("/sets/{id}")
    public ResponseEntity<Void> deleteSet(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        service.deleteSet(id, AuthenticatedUser.id(jwt)); return ResponseEntity.noContent().build();
    }
}
