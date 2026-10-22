package com.levelup.controller;

import com.levelup.dto.ApiDtos.*;
import com.levelup.service.CatalogService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "Create a user", description = "Creates a user with a unique email address.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or email conflicts with existing data",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.levelup.exception.ApiExceptionHandler.ApiError.class)))
    })
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
