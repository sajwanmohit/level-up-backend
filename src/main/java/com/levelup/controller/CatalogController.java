package com.levelup.controller;

import com.levelup.dto.ApiDtos.*;
import com.levelup.service.CatalogService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService catalog;
    public CatalogController(CatalogService catalog) { this.catalog = catalog; }
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = catalog.createUser(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
    }
    @GetMapping("/exercises")
    public List<ExerciseResponse> exercises() { return catalog.exercises(); }
    @PostMapping("/exercises")
    public ResponseEntity<ExerciseResponse> createExercise(@Valid @RequestBody ExerciseRequest request) {
        ExerciseResponse response = catalog.createExercise(request);
        return ResponseEntity.created(URI.create("/api/v1/exercises/" + response.id())).body(response);
    }
}
