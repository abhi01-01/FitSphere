package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityMessageListenerTest {

    @Mock
    private ActivityAIService aiService;

    @Mock
    private RecommendationRepository recommendationRepository;

    @InjectMocks
    private ActivityMessageListener listener;

    @Test
    void skipsDuplicateActivityMessages() {
        Activity activity = new Activity();
        activity.setId("activity-1");

        when(recommendationRepository.existsByActivityId("activity-1")).thenReturn(true);

        listener.processActivity(activity);

        verify(recommendationRepository, never()).save(any());
        verify(aiService, never()).generateRecommendation(any());
    }

    @Test
    void savesRecommendationForNewActivityMessage() {
        Activity activity = new Activity();
        activity.setId("activity-2");

        Recommendation recommendation = Recommendation.builder()
                .activityId("activity-2")
                .userId("user-1")
                .build();

        when(recommendationRepository.existsByActivityId("activity-2")).thenReturn(false);
        when(aiService.generateRecommendation(activity)).thenReturn(recommendation);

        listener.processActivity(activity);

        verify(aiService).generateRecommendation(activity);
        verify(recommendationRepository).save(recommendation);
    }
}
