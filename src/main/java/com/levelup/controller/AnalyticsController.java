package com.levelup.controller;

import com.levelup.analytics.WorkoutAnalyticsService;
import com.levelup.dto.ApiDtos.*;
import com.levelup.entity.PersonalRecord;
import com.levelup.exception.ResourceNotFoundException;
import com.levelup.repository.PersonalRecordRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AnalyticsController {
    private final WorkoutAnalyticsService analytics;
    private final PersonalRecordRepository records;
    public AnalyticsController(WorkoutAnalyticsService analytics, PersonalRecordRepository records) { this.analytics = analytics; this.records = records; }
    @GetMapping("/prs")
    public List<PRResponse> prs(@RequestParam UUID userId) { return records.findByUserIdOrderByAchievedAtDesc(userId).stream().map(PRResponse::from).toList(); }
    @GetMapping("/prs/exercises/{exerciseId}")
    public List<PRResponse> prsForExercise(@PathVariable UUID exerciseId, @RequestParam UUID userId) {
        return records.findByUserIdAndExerciseIdOrderByAchievedAtDesc(userId, exerciseId).stream().map(PRResponse::from).toList();
    }
    @GetMapping("/analytics/daily")
    public DailyAnalysis daily(@RequestParam UUID workoutId) { return analytics.daily(workoutId); }
    @GetMapping("/analytics/exercises/{exerciseId}")
    public ExerciseAnalytics exercise(@PathVariable UUID exerciseId, @RequestParam UUID userId) { return analytics.exercise(exerciseId, userId); }
    @GetMapping("/analytics/dashboard")
    public Dashboard dashboard(@RequestParam UUID userId) { return analytics.dashboard(userId); }
}
