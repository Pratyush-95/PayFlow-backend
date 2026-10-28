
package com.payflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private final RestClient restClient;

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from}")
    private String fromEmail;

    public EmailService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .build();
    }

    public void sendOtpEmail(String toEmail, String otp) {

        String subject = "PayFlow - Email Verification OTP";

        String text = """
                Welcome to PayFlow!

                Your email verification OTP is: %s

                This OTP is valid for 5 minutes.

                If you did not request this OTP, please ignore this email.
                """.formatted(otp);

        sendEmail(toEmail, subject, text);
    }

    public void sendPasswordResetOtpEmail(String toEmail, String otp) {

        String subject = "PayFlow - Password Reset OTP";

        String text = """
                Hello,

                Your PayFlow password reset OTP is: %s

                This OTP is valid for 5 minutes.
                Do not share this OTP with anyone.

                If you did not request a password reset,
                please ignore this email.

                Team PayFlow
                """.formatted(otp);

        sendEmail(toEmail, subject, text);
    }

    private void sendEmail(String toEmail, String subject, String text) {

        Map<String, Object> payload = Map.of(
                "from", fromEmail,
                "to", List.of(toEmail),
                "subject", subject,
                "text", text
        );

        try {
            restClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException ex) {
            throw new MailSendException(
                    "Failed to send email using Resend API", ex);
        }
    }
}
