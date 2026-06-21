package com.fitness.aiservice.service;

import com.fitness.aiservice.error.ForbiddenException;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    @Test
    void getActivityRecommendationReturnsNotFoundWhenRecommendationMissing() {
        String activityId = "6a370c248b5fdd05892766a5";
        when(recommendationRepository.findByActivityId(activityId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> recommendationService.getActivityRecommendation(activityId, "user-1", "FIT_USER"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("No recommendation found for this activity: " + activityId, exception.getReason());
    }

    @Test
    void getActivityRecommendationRejectsOtherUsers() {
        String activityId = "activity-1";
        Recommendation recommendation = Recommendation.builder()
                .activityId(activityId)
                .userId("owner-1")
                .build();
        when(recommendationRepository.findByActivityId(activityId)).thenReturn(Optional.of(recommendation));

        assertThrows(
                ForbiddenException.class,
                () -> recommendationService.getActivityRecommendation(activityId, "another-user", "FIT_USER"));
    }
}
