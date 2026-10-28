
package com.payflow.entity;

import java.time.Instant;

public class PendingRegistration {

    private final String email;
    private final String passwordHash;
    private final String otpHash;
    private final Instant expiresAt;

    private int attempts;

    public PendingRegistration(
            String email,
            String passwordHash,
            String otpHash,
            Instant expiresAt) {

        this.email = email;
        this.passwordHash = passwordHash;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
        this.attempts = 0;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void incrementAttempts() {
        this.attempts++;
    }
}
