package com.fitness.aiservice.controller;

import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Recommendation>> getUserRecommendation(
            @PathVariable String userId,
            @RequestHeader("X-User-ID") String callerUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String callerRoles) {
        return ResponseEntity.ok(recommendationService.getUserRecommendation(userId, callerUserId, callerRoles));
    }

    @GetMapping("/activity/{activityId}")
    public ResponseEntity<Recommendation> getActivityRecommendation(
            @PathVariable String activityId,
            @RequestHeader("X-User-ID") String callerUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String callerRoles) {
        return ResponseEntity.ok(recommendationService.getActivityRecommendation(activityId, callerUserId, callerRoles));
    }
}
