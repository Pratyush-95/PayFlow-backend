
package com.payflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("PayFlow - Email Verification OTP");

        message.setText(
                "Welcome to PayFlow!\n\n"
                        + "Your email verification OTP is: " + otp + "\n\n"
                        + "This OTP is valid for 5 minutes.\n\n"
                        + "If you did not request this OTP, please ignore this email."
        );

        mailSender.send(message);
    }

    public void sendPasswordResetOtpEmail(String toEmail, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("PayFlow - Password Reset OTP");

        message.setText(
                "Hello,\n\n"
                        + "Your PayFlow password reset OTP is: " + otp + "\n\n"
                        + "This OTP is valid for 5 minutes.\n"
                        + "Do not share this OTP with anyone.\n\n"
                        + "If you did not request a password reset, "
                        + "please ignore this email.\n\n"
                        + "Team PayFlow"
        );

        mailSender.send(message);
    }
}
