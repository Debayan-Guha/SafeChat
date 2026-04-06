package com.safechat.userservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendOtpEmail(String toEmail, int otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Your OTP Code - SafeChat");
            message.setText(String.format(
                    "Hello,\n\n" +
                            "Your OTP code is: %d\n\n" +
                            "This code will expire in 1 minute.\n\n" +
                            "If you did not request this, please ignore this email.\n\n" +
                            "Best regards,\n" +
                            "SafeChat Team",
                    otp));
            mailSender.send(message);
        } catch (Exception e) {
            logger.warn("Failed to send OTP email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Async
    public void sendAccountDeletionRequestOtp(String toEmail, int otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Account Deletion Request - SafeChat");
            message.setText(String.format(
                    "Hello,\n\n" +
                            "You have requested to delete your SafeChat account.\n\n" +
                            "Your OTP code is: %d\n\n" +
                            "This code will expire in 1 minute.\n\n" +
                            "After verification, your account will be scheduled for deletion in 24 hours.\n" +
                            "You can cancel this request anytime within 24 hours.\n\n" +
                            "If you did not request this, please secure your account immediately.\n\n" +
                            "Best regards,\n" +
                            "SafeChat Team",
                    otp));
            mailSender.send(message);
        } catch (Exception e) {
            logger.warn("Failed to send deletion request OTP email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Async
    public void sendAccountDeletionInstantOtp(String toEmail, int otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Instant Account Deletion OTP - SafeChat");
            message.setText(String.format(
                    "Hello,\n\n" +
                            "You have requested to IMMEDIATELY delete your SafeChat account.\n\n" +
                            "Your OTP code is: %d\n\n" +
                            "This code will expire in 1 minute.\n\n" +
                            "⚠️ WARNING: This action is IRREVERSIBLE. All your data will be permanently deleted.\n\n" +
                            "If you did not request this, please secure your account immediately.\n\n" +
                            "Best regards,\n" +
                            "SafeChat Team",
                    otp));
            mailSender.send(message);
        } catch (Exception e) {
            logger.warn("Failed to send instant deletion OTP email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String displayName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Welcome to SafeChat!");
            message.setText(String.format(
                    "Hello %s,\n\n" +
                            "Welcome to SafeChat - your privacy-first messaging app!\n\n" +
                            "Your account has been successfully created.\n\n" +
                            "Start connecting with friends securely.\n\n" +
                            "Best regards,\n" +
                            "SafeChat Team",
                    displayName));
            mailSender.send(message);
        } catch (Exception e) {
            logger.warn("Failed to send welcome email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Async
    public void sendAfterDeletionEmail(String toEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Account Deleted - SafeChat");
            message.setText(
                    "Hello,\n\n" +
                            "Your SafeChat account has been successfully deleted.\n\n" +
                            "All your data has been removed from our systems. We are sorry to see you go!\n\n" +
                            "Best regards,\n" +
                            "SafeChat Team");
            mailSender.send(message);
        } catch (Exception e) {
            logger.warn("Failed to send post-deletion email to " + toEmail + ": " + e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetOtp(String toEmail, int otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Password Reset OTP - SafeChat");
            message.setText(String.format(
                    "Hello,\n\n" +
                            "You requested to reset your password.\n\n" +
                            "Your OTP code is: %d\n\n" +
                            "This code will expire in 5 minutes.\n\n" +
                            "If you did not request this, please ignore this email.\n\n" +
                            "Best regards,\n" +
                            "SafeChat Team",
                    otp));
            mailSender.send(message);
        } catch (Exception e) {
            logger.warn("Failed to send password reset OTP email to " + toEmail + ": " + e.getMessage());
        }
    }
}