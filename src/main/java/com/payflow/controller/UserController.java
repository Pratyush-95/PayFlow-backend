
package com.payflow.controller;

import com.payflow.entity.AppUser;
import com.payflow.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            Authentication authentication) {

        UUID userId;

        try {
            userId = UUID.fromString(
                    authentication.getName()
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Invalid authenticated user"
                    ));
        }

        return userRepository.findById(userId)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(
                        Map.of(
                                "id", user.getId().toString(),
                                "email", user.getEmail(),
                                "role", user.getRole(),
                                "message", "User profile fetched successfully"
                        )
                ))
                .orElseGet(() -> ResponseEntity.status(
                        HttpStatus.NOT_FOUND
                ).body(Map.of(
                        "message", "User not found"
                )));
    }
}
