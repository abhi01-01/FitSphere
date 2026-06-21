package com.fitness.aiservice.service;

import com.fitness.aiservice.error.ForbiddenException;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {
    private static final String ADMIN_ROLE = "FIT_ADMIN";
    private final RecommendationRepository recommendationRepository;

    public List<Recommendation> getUserRecommendation(String userId, String callerUserId, String callerRoles) {
        if (!isAdmin(callerRoles) && !callerUserId.equals(userId)) {
            throw new ForbiddenException("You are not allowed to access these recommendations");
        }
        log.info("audit_event=recommendation_list_read keycloak_id={} target_user_id={}", callerUserId, userId);
        return recommendationRepository.findByUserId(userId);
    }

    public Recommendation getActivityRecommendation(String activityId, String callerUserId, String callerRoles) {
        Recommendation recommendation = recommendationRepository.findByActivityId(activityId)
                .orElseThrow(() -> {
                    log.warn("No recommendation found for activityId={}", activityId);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "No recommendation found for this activity: " + activityId);
                });
        return authorizeRecommendation(recommendation, callerUserId, callerRoles);
    }

    private Recommendation authorizeRecommendation(Recommendation recommendation, String callerUserId, String callerRoles) {
        if (!isAdmin(callerRoles) && !callerUserId.equals(recommendation.getUserId())) {
            throw new ForbiddenException("You are not allowed to access this recommendation");
        }
        log.info("audit_event=recommendation_read keycloak_id={} activity_id={}", callerUserId, recommendation.getActivityId());
        return recommendation;
    }

    private boolean isAdmin(String callerRoles) {
        return callerRoles != null
                && java.util.Arrays.stream(callerRoles.split(","))
                .map(String::trim)
                .anyMatch(ADMIN_ROLE::equals);
    }
}
