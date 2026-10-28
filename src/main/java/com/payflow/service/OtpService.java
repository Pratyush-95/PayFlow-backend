
package com.payflow.service;

import com.payflow.entity.AppUser;
import com.payflow.entity.PendingRegistration;
import com.payflow.repository.UserRepository;
import com.payflow.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    private final ConcurrentMap<String, PendingRegistration>
            pendingRegistrations = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, PasswordResetOtp>
            passwordResetOtps = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, PasswordResetToken>
            passwordResetTokens = new ConcurrentHashMap<>();

    private final SecureRandom secureRandom = new SecureRandom();

    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthService authService;

    public OtpService(
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            JwtService jwtService,
            AuthService authService) {

        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.authService = authService;
    }

    // ---------------- REGISTRATION OTP ----------------

    public void startRegistration(String email, String password) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (password == null || password.length() < 8
                || password.length() > 72) {
            throw new IllegalArgumentException(
                    "Password must be between 8 and 72 characters"
            );
        }

        String normalizedEmail = normalizeEmail(email);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException(
                    "Email is already registered"
            );
        }

        String otp = generateOtp();

        PendingRegistration pending = new PendingRegistration(
                normalizedEmail,
                passwordEncoder.encode(password),
                passwordEncoder.encode(otp),
                Instant.now().plusSeconds(OTP_EXPIRY_MINUTES * 60L)
        );

        emailService.sendOtpEmail(normalizedEmail, otp);
        pendingRegistrations.put(normalizedEmail, pending);
    }

    public String verifyOtp(String email, String otp) {

        if (email == null || email.isBlank()
                || otp == null || !otp.matches("\\d{6}")) {
            throw new IllegalArgumentException("Invalid email or OTP");
        }

        String normalizedEmail = normalizeEmail(email);

        PendingRegistration pending =
                pendingRegistrations.get(normalizedEmail);

        if (pending == null) {
            throw new IllegalArgumentException(
                    "Registration not found. Please request a new OTP."
            );
        }

        if (Instant.now().isAfter(pending.getExpiresAt())) {
            pendingRegistrations.remove(normalizedEmail, pending);
            throw new IllegalArgumentException(
                    "OTP expired. Please request a new OTP."
            );
        }

        if (pending.getAttempts() >= MAX_ATTEMPTS) {
            pendingRegistrations.remove(normalizedEmail, pending);
            throw new IllegalArgumentException(
                    "Too many incorrect attempts. Please register again."
            );
        }

        if (!passwordEncoder.matches(otp, pending.getOtpHash())) {
            pending.incrementAttempts();
            throw new IllegalArgumentException("Invalid OTP");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            pendingRegistrations.remove(normalizedEmail, pending);
            throw new IllegalArgumentException(
                    "Email is already registered"
            );
        }

        AppUser user = new AppUser();
        user.setEmail(pending.getEmail());
        user.setPasswordHash(pending.getPasswordHash());
        user.setRole("MERCHANT");

        AppUser savedUser = userRepository.save(user);

        pendingRegistrations.remove(normalizedEmail, pending);

        return jwtService.generateToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    // ---------------- FORGOT PASSWORD ----------------

    public void startPasswordReset(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        String normalizedEmail = normalizeEmail(email);

        // Return the same outward response for registered and
        // unregistered emails from the controller.
        if (!userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }

        String otp = generateOtp();

        PasswordResetOtp resetOtp = new PasswordResetOtp(
                passwordEncoder.encode(otp),
                Instant.now().plusSeconds(OTP_EXPIRY_MINUTES * 60L),
                0
        );

        // Send email before storing the new OTP.
        emailService.sendPasswordResetOtpEmail(normalizedEmail, otp);
        passwordResetOtps.put(normalizedEmail, resetOtp);

        // A fresh OTP invalidates older reset authorization.
        passwordResetTokens.entrySet().removeIf(
                entry -> entry.getValue().email().equals(normalizedEmail)
        );
    }

    public String verifyPasswordResetOtp(String email, String otp) {

        if (email == null || email.isBlank()
                || otp == null || !otp.matches("\\d{6}")) {
            throw new IllegalArgumentException("Invalid email or OTP");
        }

        String normalizedEmail = normalizeEmail(email);

        PasswordResetOtp resetOtp =
                passwordResetOtps.get(normalizedEmail);

        if (resetOtp == null) {
            throw new IllegalArgumentException(
                    "Invalid or expired reset OTP"
            );
        }

        if (Instant.now().isAfter(resetOtp.expiresAt())) {
            passwordResetOtps.remove(normalizedEmail, resetOtp);
            throw new IllegalArgumentException(
                    "OTP expired. Please request a new OTP."
            );
        }

        if (resetOtp.attempts() >= MAX_ATTEMPTS) {
            passwordResetOtps.remove(normalizedEmail, resetOtp);
            throw new IllegalArgumentException(
                    "Too many incorrect attempts. Request a new OTP."
            );
        }

        if (!passwordEncoder.matches(otp, resetOtp.otpHash())) {
            passwordResetOtps.computeIfPresent(
                    normalizedEmail,
                    (key, current) -> current == resetOtp
                            ? new PasswordResetOtp(
                            current.otpHash(),
                            current.expiresAt(),
                            current.attempts() + 1
                    )
                            : current
            );

            throw new IllegalArgumentException("Invalid OTP");
        }

        if (!userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            passwordResetOtps.remove(normalizedEmail, resetOtp);
            throw new IllegalArgumentException("Invalid reset request");
        }

        // OTP can only be used once.
        if (!passwordResetOtps.remove(normalizedEmail, resetOtp)) {
            throw new IllegalArgumentException(
                    "OTP has already been used. Request a new OTP."
            );
        }

        String resetToken = UUID.randomUUID().toString();

        passwordResetTokens.put(
                resetToken,
                new PasswordResetToken(
                        normalizedEmail,
                        Instant.now().plusSeconds(OTP_EXPIRY_MINUTES * 60L)
                )
        );

        return resetToken;
    }

    public void resetPassword(
            String email,
            String resetToken,
            String newPassword) {

        if (email == null || email.isBlank()
                || resetToken == null || resetToken.isBlank()) {
            throw new IllegalArgumentException("Invalid reset request");
        }

        if (newPassword == null
                || newPassword.length() < 8
                || newPassword.length() > 72) {
            throw new IllegalArgumentException(
                    "Password must be between 8 and 72 characters"
            );
        }

        String normalizedEmail = normalizeEmail(email);

        PasswordResetToken token =
                passwordResetTokens.get(resetToken);

        if (token == null
                || !token.email().equals(normalizedEmail)
                || Instant.now().isAfter(token.expiresAt())) {
            throw new IllegalArgumentException(
                    "Invalid or expired reset token"
            );
        }

        // Remove the token before updating, so it cannot be reused.
        if (!passwordResetTokens.remove(resetToken, token)) {
            throw new IllegalArgumentException(
                    "Reset token has already been used"
            );
        }

        authService.updatePassword(normalizedEmail, newPassword);
    }

    // ---------------- HELPERS ----------------

    private String generateOtp() {
        return String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record PasswordResetOtp(
            String otpHash,
            Instant expiresAt,
            int attempts) {
    }

    private record PasswordResetToken(
            String email,
            Instant expiresAt) {
    }
}
