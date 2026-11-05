package com.levelup.controller;

import com.levelup.analytics.WorkoutAnalyticsService;
import com.levelup.config.AuthenticatedUser;
import com.levelup.dto.ApiDtos.*;
import com.levelup.repository.PersonalRecordRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/v1")
public class AnalyticsController {
    private final WorkoutAnalyticsService analytics;
    private final PersonalRecordRepository records;
    public AnalyticsController(WorkoutAnalyticsService analytics, PersonalRecordRepository records) { this.analytics = analytics; this.records = records; }
    @GetMapping("/prs")
    public List<PRResponse> prs(@RequestParam UUID userId, @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser.requireMatch(userId, jwt);
        return records.findByUserIdOrderByAchievedAtDesc(userId).stream().map(PRResponse::from).toList();
    }
    @GetMapping("/prs/exercises/{exerciseId}")
    public List<PRResponse> prsForExercise(@PathVariable UUID exerciseId, @RequestParam UUID userId, @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser.requireMatch(userId, jwt);
        return records.findByUserIdAndExerciseIdOrderByAchievedAtDesc(userId, exerciseId).stream().map(PRResponse::from).toList();
    }
    @GetMapping("/analytics/daily")
    public DailyAnalysis daily(@RequestParam UUID workoutId, @AuthenticationPrincipal Jwt jwt) {
        return analytics.daily(workoutId, AuthenticatedUser.id(jwt));
    }
    @GetMapping("/analytics/exercises/{exerciseId}")
    public ExerciseAnalytics exercise(@PathVariable UUID exerciseId, @RequestParam UUID userId, @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser.requireMatch(userId, jwt);
        return analytics.exercise(exerciseId, userId);
    }
    @GetMapping("/analytics/dashboard")
    public Dashboard dashboard(@RequestParam UUID userId, @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser.requireMatch(userId, jwt);
        return analytics.dashboard(userId);
    }
}
