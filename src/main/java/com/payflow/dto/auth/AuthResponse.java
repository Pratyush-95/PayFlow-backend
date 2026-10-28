
package com.payflow.dto.auth;

public record AuthResponse(
        String token,
        String tokenType,
        String message
) {
}
