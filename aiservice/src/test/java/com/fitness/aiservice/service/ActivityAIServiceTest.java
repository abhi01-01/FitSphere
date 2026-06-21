package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityAIServiceTest {

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private ActivityAIService activityAIService;

    @Test
    void generateRecommendationFallsBackWhenGeminiTimesOut() {
        Activity activity = new Activity();
        activity.setId("activity-1");
        activity.setUserId("user-1");
        activity.setType("RUNNING");
        activity.setDuration(30);
        activity.setCaloriesBurned(300);

        when(geminiService.getAnswer(anyString()))
                .thenThrow(new GeminiServiceException("Gemini API timed out", null));

        Recommendation recommendation = activityAIService.generateRecommendation(activity);

        assertEquals("activity-1", recommendation.getActivityId());
        assertEquals("user-1", recommendation.getUserId());
        assertEquals("Unable to generate detailed analysis", recommendation.getRecommendation());
    }
}
