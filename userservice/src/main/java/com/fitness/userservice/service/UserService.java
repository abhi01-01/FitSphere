package com.fitness.userservice.service;

import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.error.ForbiddenException;
import com.fitness.userservice.error.NotFoundException;
import com.fitness.userservice.model.User;
import com.fitness.userservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserService {
    private static final String ADMIN_ROLE = "FIT_ADMIN";

    @Autowired
    private UserRepository repository;

    public UserResponse register(RegisterRequest request) {

        if (repository.existsByEmail(request.getEmail())) {
            User existingUser = repository.findByEmail(request.getEmail());
            existingUser.setKeycloakId(request.getKeycloakId());
            existingUser.setFirstName(request.getFirstName());
            existingUser.setLastName(request.getLastName());
            User savedUser = repository.save(existingUser);
            log.info("audit_event=user_sync_update keycloak_id={} user_id={} email={}",
                    savedUser.getKeycloakId(),
                    savedUser.getId(),
                    savedUser.getEmail());
            UserResponse userResponse = new UserResponse();
            userResponse.setId(savedUser.getId());
            userResponse.setKeycloakId(savedUser.getKeycloakId());
            userResponse.setEmail(savedUser.getEmail());
            userResponse.setFirstName(savedUser.getFirstName());
            userResponse.setLastName(savedUser.getLastName());
            userResponse.setCreatedAt(savedUser.getCreatedAt());
            userResponse.setUpdatedAt(savedUser.getUpdatedAt());
            return userResponse;
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setKeycloakId(request.getKeycloakId());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        User savedUser = repository.save(user);
        log.info("audit_event=user_sync_create keycloak_id={} user_id={} email={}",
                savedUser.getKeycloakId(),
                savedUser.getId(),
                savedUser.getEmail());
        UserResponse userResponse = new UserResponse();
        userResponse.setKeycloakId(savedUser.getKeycloakId());
        userResponse.setId(savedUser.getId());
        userResponse.setEmail(savedUser.getEmail());
        userResponse.setFirstName(savedUser.getFirstName());
        userResponse.setLastName(savedUser.getLastName());
        userResponse.setCreatedAt(savedUser.getCreatedAt());
        userResponse.setUpdatedAt(savedUser.getUpdatedAt());

        return userResponse;
    }

    public UserResponse getUserProfile(String userId, String callerUserId, String callerRoles) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        if (!isAdmin(callerRoles) && !callerUserId.equals(user.getKeycloakId())) {
            throw new ForbiddenException("You are not allowed to access this user profile");
        }

        log.info("audit_event=user_profile_read keycloak_id={} target_user_id={}", callerUserId, userId);
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setEmail(user.getEmail());
        userResponse.setKeycloakId(user.getKeycloakId());
        userResponse.setFirstName(user.getFirstName());
        userResponse.setLastName(user.getLastName());
        userResponse.setCreatedAt(user.getCreatedAt());
        userResponse.setUpdatedAt(user.getUpdatedAt());

        return userResponse;
    }

    public Boolean existByUserId(String userId) {
        log.info("Calling User Validation API for userId: {}", userId);
        return repository.existsByKeycloakId(userId);
    }

    private boolean isAdmin(String callerRoles) {
        return callerRoles != null
                && java.util.Arrays.stream(callerRoles.split(","))
                .map(String::trim)
                .anyMatch(ADMIN_ROLE::equals);
    }
}
