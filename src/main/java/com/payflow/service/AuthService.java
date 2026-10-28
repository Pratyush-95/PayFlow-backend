
package com.payflow.service;

import com.payflow.entity.AppUser;
import com.payflow.repository.UserRepository;
import com.payflow.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public String register(String email, String password) {

        String normalizedEmail = normalizeEmail(email);
        validatePassword(password);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException(
                    "Email is already registered"
            );
        }

        AppUser user = new AppUser();
        user.setEmail(normalizedEmail);

        // Store hashed password, never the plain password
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("MERCHANT");

        AppUser savedUser = userRepository.save(user);

        return jwtService.generateToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    @Transactional(readOnly = true)
    public String login(String email, String password) {

        String normalizedEmail = normalizeEmail(email);
        validatePassword(password);

        AppUser user = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid email or password"
                ));

        if (!passwordEncoder.matches(
                password,
                user.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "Invalid email or password"
            );
        }

        return jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }

    @Transactional
    public void updatePassword(String email, String newPassword) {

        String normalizedEmail = normalizeEmail(email);
        validatePassword(newPassword);

        AppUser user = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid reset request"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
    }
}
