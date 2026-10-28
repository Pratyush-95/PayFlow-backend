
package com.payflow.controller;

import com.payflow.dto.auth.AuthResponse;
import com.payflow.dto.auth.LoginRequest;
import com.payflow.dto.auth.RegisterRequest;
import com.payflow.dto.auth.VerifyOtpRequest;
import com.payflow.dto.auth.ForgotPasswordRequest;
import com.payflow.dto.auth.VerifyResetOtpRequest;
import com.payflow.dto.auth.ResetPasswordRequest;

import com.payflow.service.AuthService;
import com.payflow.service.OtpService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    public AuthController(
            AuthService authService,
            OtpService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    // Step 1: Register - send OTP to email
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request) {

        try {
            otpService.startRegistration(
                    request.email(),
                    request.password()
            );

            return ResponseEntity.ok(Map.of(
                    "message",
                    "OTP sent to your email. Verify it within 5 minutes."
            ));

        } catch (IllegalArgumentException ex) {
            HttpStatus status =
                    "Email is already registered".equals(ex.getMessage())
                            ? HttpStatus.CONFLICT
                            : HttpStatus.BAD_REQUEST;

            return ResponseEntity.status(status)
                    .body(Map.of("message", ex.getMessage()));

        } catch (MailException ex) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "message",
                            "Unable to send OTP email. Please try again."
                    ));
        }
    }

    // Step 2: Verify registration OTP and create account
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        try {
            String token = otpService.verifyOtp(
                    request.email(),
                    request.otp()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AuthResponse(
                            token,
                            "Bearer",
                            "Registration successful"
                    ));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    // Step 3: Login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        try {
            String token = authService.login(
                    request.email(),
                    request.password()
            );

            return ResponseEntity.ok(
                    new AuthResponse(
                            token,
                            "Bearer",
                            "Login successful"
                    )
            );

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(
                            null,
                            null,
                            "Invalid email or password"
                    ));
        }
    }

    // Step 4: Request password-reset OTP
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        try {
            otpService.startPasswordReset(request.email());

            // Same response whether the email exists or not.
            return ResponseEntity.ok(Map.of(
                    "message",
                    "If the email is registered, a password reset OTP has been sent."
            ));

        } catch (MailException ex) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "message",
                            "Unable to process the email request. Please try again later."
                    ));
        }
    }

    // Step 5: Verify reset OTP and return a reset token
    @PostMapping("/verify-reset-otp")
    public ResponseEntity<?> verifyResetOtp(
            @Valid @RequestBody VerifyResetOtpRequest request) {

        try {
            String resetToken =
                    otpService.verifyPasswordResetOtp(
                            request.email(),
                            request.otp()
                    );

            return ResponseEntity.ok(Map.of(
                    "message", "OTP verified successfully",
                    "resetToken", resetToken
            ));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    // Step 6: Reset password using the verified reset token
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        try {
            otpService.resetPassword(
                    request.email(),
                    request.resetToken(),
                    request.newPassword()
            );

            return ResponseEntity.ok(Map.of(
                    "message",
                    "Password reset successful. Please login with your new password."
            ));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }
    }
}
