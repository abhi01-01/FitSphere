package com.fitness.activityservice.service;

import com.fitness.activityservice.ActivityRepository;
import com.fitness.activityservice.error.ForbiddenException;
import com.fitness.activityservice.model.Activity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserValidationService userValidationService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ActivityService activityService;

    @Test
    void getActivityByIdRejectsDifferentCallerWithoutAdminRole() {
        Activity activity = Activity.builder()
                .id("activity-1")
                .userId("owner-keycloak-id")
                .build();

        when(activityRepository.findById("activity-1")).thenReturn(Optional.of(activity));

        assertThrows(
                ForbiddenException.class,
                () -> activityService.getActivityById("activity-1", "another-keycloak-id", "FIT_USER"));
    }
}
