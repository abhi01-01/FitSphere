package com.fitness.activityservice.service;

import com.fitness.activityservice.ActivityRepository;
import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.error.BadRequestException;
import com.fitness.activityservice.error.ForbiddenException;
import com.fitness.activityservice.error.NotFoundException;
import com.fitness.activityservice.model.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {
    private static final String ADMIN_ROLE = "FIT_ADMIN";

    private final ActivityRepository activityRepository;
    private final UserValidationService userValidationService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    public ActivityResponse trackActivity(ActivityRequest request) {

        boolean isValidUser = userValidationService.validateUser(request.getUserId());
        if (!isValidUser) {
            throw new BadRequestException("Invalid user: " + request.getUserId());
        }

        Activity activity = Activity.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .duration(request.getDuration())
                .caloriesBurned(request.getCaloriesBurned())
                .startTime(request.getStartTime())
                .additionalMetrics(request.getAdditionalMetrics())
                .build();

        Activity savedActivity = activityRepository.save(activity);
        log.info("audit_event=activity_created keycloak_id={} activity_id={} type={}",
                savedActivity.getUserId(),
                savedActivity.getId(),
                savedActivity.getType());

        // Publish to RabbitMQ for AI Processing
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, savedActivity);
        } catch(Exception e) {
            log.error("Failed to publish activity to RabbitMQ : ", e);
        }

        return mapToResponse(savedActivity);
    }

    private ActivityResponse mapToResponse(Activity activity){
        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setUserId(activity.getUserId());
        response.setType(activity.getType());
        response.setDuration(activity.getDuration());
        response.setCaloriesBurned(activity.getCaloriesBurned());
        response.setStartTime(activity.getStartTime());
        response.setAdditionalMetrics(activity.getAdditionalMetrics());
        response.setCreatedAt(activity.getCreatedAt());
        response.setUpdatedAt(activity.getUpdatedAt());
        return response;
    }

    public List<ActivityResponse> getUserActivities(String userId) {
        log.info("audit_event=activity_list_read keycloak_id={}", userId);
        List<Activity> activities = activityRepository.findByUserId(userId);
        return activities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ActivityResponse getActivityById(String activityId, String callerUserId, String callerRoles) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("Activity not found with id: " + activityId));

        if (!isAdmin(callerRoles) && !callerUserId.equals(activity.getUserId())) {
            throw new ForbiddenException("You are not allowed to access this activity");
        }

        log.info("audit_event=activity_read keycloak_id={} activity_id={}", callerUserId, activityId);
        return mapToResponse(activity);
    }

    private boolean isAdmin(String callerRoles) {
        return callerRoles != null
                && java.util.Arrays.stream(callerRoles.split(","))
                .map(String::trim)
                .anyMatch(ADMIN_ROLE::equals);
    }
}
