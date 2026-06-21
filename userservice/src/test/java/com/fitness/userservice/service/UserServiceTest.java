package com.fitness.userservice.service;

import com.fitness.userservice.dto.RegisterRequest;
import com.fitness.userservice.dto.UserResponse;
import com.fitness.userservice.error.ForbiddenException;
import com.fitness.userservice.model.User;
import com.fitness.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUpdatesKeycloakIdForExistingEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("athlete@example.com");
        request.setKeycloakId("new-keycloak-sub");
        request.setFirstName("Fit");
        request.setLastName("Sphere");

        User existingUser = new User();
        existingUser.setId("user-1");
        existingUser.setEmail("athlete@example.com");
        existingUser.setKeycloakId("old-keycloak-sub");
        existingUser.setFirstName("Old");
        existingUser.setLastName("Name");

        when(repository.existsByEmail(request.getEmail())).thenReturn(true);
        when(repository.findByEmail(request.getEmail())).thenReturn(existingUser);
        when(repository.save(existingUser)).thenReturn(existingUser);

        UserResponse response = userService.register(request);

        assertEquals("new-keycloak-sub", existingUser.getKeycloakId());
        assertEquals("Fit", existingUser.getFirstName());
        assertEquals("Sphere", existingUser.getLastName());
        assertEquals("new-keycloak-sub", response.getKeycloakId());
        verify(repository).save(existingUser);
    }

    @Test
    void getUserProfileRejectsDifferentCallerWithoutAdminRole() {
        User user = new User();
        user.setId("internal-user-id");
        user.setKeycloakId("owner-keycloak-id");
        user.setEmail("owner@example.com");

        when(repository.findById("internal-user-id")).thenReturn(Optional.of(user));

        assertThrows(
                ForbiddenException.class,
                () -> userService.getUserProfile("internal-user-id", "another-keycloak-id", "FIT_USER"));
    }
}
