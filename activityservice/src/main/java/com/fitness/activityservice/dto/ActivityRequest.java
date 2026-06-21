package com.fitness.activityservice.dto;

import com.fitness.activityservice.model.ActivityType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ActivityRequest {
    private String userId;

    @NotNull(message = "Activity type is required")
    private ActivityType type;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be greater than zero")
    private Integer duration;

    @NotNull(message = "Calories burned is required")
    @PositiveOrZero(message = "Calories burned must be zero or greater")
    private Integer caloriesBurned;

    private LocalDateTime startTime;
    private Map<String, Object> additionalMetrics;
}
