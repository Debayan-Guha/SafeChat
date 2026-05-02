package com.safechat.userservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final String SERVICE_NAME = "EmailService";
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendAccountCreationOtp(String toEmail, int otp) {
        final String METHOD_NAME = "sendAccountCreationOtp";

        log.debug("{} - Preparing to send account creation OTP to email: {}", METHOD_NAME, toEmail);
        log.debug("{} - OTP value: {} for email: {}", METHOD_NAME, otp, toEmail);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Your OTP Code - SafeChat");

            String htmlContent = String.format(
                    """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <style>
                                    @keyframes pulse {
                                        0%% { transform: scale(1); }
                                        50%% { transform: scale(1.1); }
                                        100%% { transform: scale(1); }
                                    }
                                    @keyframes fadeIn {
                                        from { opacity: 0; }
                                        to { opacity: 1; }
                                    }
                                    .otp-code {
                                        font-size: 32px;
                                        font-weight: bold;
                                        color: #4F46E5;
                                        background: #E0E7FF;
                                        padding: 15px;
                                        border-radius: 10px;
                                        display: inline-block;
                                        animation: pulse 1s ease-in-out 3;
                                        letter-spacing: 5px;
                                    }
                                    .container {
                                        font-family: Arial, sans-serif;
                                        max-width: 600px;
                                        margin: 0 auto;
                                        padding: 20px;
                                        animation: fadeIn 0.5s ease-in;
                                    }
                                    .header {
                                        background: linear-gradient(135deg, #4F46E5, #7C3AED);
                                        color: white;
                                        padding: 20px;
                                        text-align: center;
                                        border-radius: 10px 10px 0 0;
                                    }
                                    .content {
                                        background: #f9fafb;
                                        padding: 30px;
                                        border-radius: 0 0 10px 10px;
                                        text-align: center;
                                    }
                                    .countdown {
                                        font-size: 14px;
                                        color: #EF4444;
                                        margin-top: 20px;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <h2>🔐 SafeChat Verification</h2>
                                    </div>
                                    <div class="content">
                                        <h3>Hello!</h3>
                                        <p>Your OTP verification code is:</p>
                                        <div class="otp-code">%06d</div>
                                        <p class="countdown">⏰ This code will expire in 1 minute</p>
                                        <p>If you didn't request this, please ignore this email.</p>
                                        <hr>
                                        <p style="font-size: 12px; color: #6B7280;">SafeChat - Your Privacy-First Messaging App</p>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                    otp);

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

            log.info("{} - Account creation OTP sent successfully to email: {}", METHOD_NAME, toEmail);

        } catch (Exception e) {
            log.warn("{} - Failed to send account creation OTP email to {}: {}", METHOD_NAME, toEmail, e.getMessage());
            log.debug("{} - Exception details: ", METHOD_NAME, e);
        }
    }

    @Async
    public void sendProfileUpdateConfirmation(String toEmail, String displayName) {
        final String METHOD_NAME = "sendProfileUpdateConfirmation";

        log.debug("{} - Preparing to send profile update confirmation to email: {}", METHOD_NAME, toEmail);
        log.debug("{} - Display name: {} for email: {}", METHOD_NAME, displayName, toEmail);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Profile Updated - SafeChat");

            String htmlContent = String.format(
                    """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <style>
                                    @keyframes fadeIn {
                                        from { opacity: 0; transform: translateY(20px); }
                                        to { opacity: 1; transform: translateY(0); }
                                    }
                                    @keyframes checkmark {
                                        0%% { transform: scale(0); }
                                        50%% { transform: scale(1.2); }
                                        100%% { transform: scale(1); }
                                    }
                                    .container {
                                        font-family: Arial, sans-serif;
                                        max-width: 600px;
                                        margin: 0 auto;
                                        padding: 20px;
                                        animation: fadeIn 0.6s ease-in;
                                    }
                                    .header {
                                        background: linear-gradient(135deg, #0891B2, #06B6D4);
                                        color: white;
                                        padding: 20px;
                                        text-align: center;
                                        border-radius: 10px 10px 0 0;
                                    }
                                    .content {
                                        background: #f9fafb;
                                        padding: 30px;
                                        border-radius: 0 0 10px 10px;
                                        text-align: center;
                                    }
                                    .check-icon {
                                        font-size: 64px;
                                        animation: checkmark 0.6s ease-in-out;
                                    }
                                    .info-box {
                                        background: #F0F9FF;
                                        border-left: 4px solid #0891B2;
                                        padding: 15px;
                                        margin: 20px 0;
                                        text-align: left;
                                        border-radius: 8px;
                                    }
                                    .button {
                                        background: linear-gradient(135deg, #0891B2, #06B6D4);
                                        color: white;
                                        padding: 12px 24px;
                                        text-decoration: none;
                                        border-radius: 8px;
                                        display: inline-block;
                                        margin: 20px 0;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <h2>✅ Profile Updated</h2>
                                    </div>
                                    <div class="content">
                                        <div class="check-icon">✅</div>
                                        <h3>Hello %s!</h3>
                                        <p>Your SafeChat profile has been successfully updated.</p>
                                        <div class="info-box">
                                            <strong>📝 Changes Applied:</strong><br>
                                            • Your profile information has been updated<br>
                                            • Your account security remains unchanged<br>
                                            • You can continue using SafeChat as usual
                                        </div>
                                        <p>If you did not make these changes, please contact support immediately.</p>
                                        <hr>
                                        <p style="font-size: 12px; color: #6B7280;">SafeChat - Your Privacy-First Messaging App</p>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                    displayName);

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

            log.info("{} - Profile update confirmation sent successfully to email: {}", METHOD_NAME, toEmail);

        } catch (Exception e) {
            log.warn("{} - Failed to send profile update confirmation email to {}: {}", METHOD_NAME, toEmail,
                    e.getMessage());
            log.debug("{} - Exception details: ", METHOD_NAME, e);
        }
    }

    @Async
    public void sendAccountDeletionRequestOtp(String toEmail, int otp) {
        final String METHOD_NAME = "sendAccountDeletionRequestOtp";

        log.debug("{} - Preparing to send account deletion request OTP to email: {}", METHOD_NAME, toEmail);
        log.debug("{} - OTP value: {} for email: {}", METHOD_NAME, otp, toEmail);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Account Deletion Request - SafeChat");

            String htmlContent = String.format(
                    """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <style>
                                    @keyframes pulse {
                                        0%% { transform: scale(1); }
                                        50%% { transform: scale(1.1); }
                                        100%% { transform: scale(1); }
                                    }
                                    @keyframes fadeIn {
                                        from { opacity: 0; }
                                        to { opacity: 1; }
                                    }
                                    .otp-code {
                                        font-size: 32px;
                                        font-weight: bold;
                                        color: #DC2626;
                                        background: #FEE2E2;
                                        padding: 15px;
                                        border-radius: 10px;
                                        display: inline-block;
                                        animation: pulse 1s ease-in-out 3;
                                        letter-spacing: 5px;
                                    }
                                    .container {
                                        font-family: Arial, sans-serif;
                                        max-width: 600px;
                                        margin: 0 auto;
                                        padding: 20px;
                                        animation: fadeIn 0.5s ease-in;
                                    }
                                    .header {
                                        background: linear-gradient(135deg, #DC2626, #991B1B);
                                        color: white;
                                        padding: 20px;
                                        text-align: center;
                                        border-radius: 10px 10px 0 0;
                                    }
                                    .content {
                                        background: #f9fafb;
                                        padding: 30px;
                                        border-radius: 0 0 10px 10px;
                                        text-align: center;
                                    }
                                    .warning {
                                        background: #FEF2F2;
                                        border-left: 4px solid #DC2626;
                                        padding: 10px;
                                        margin: 20px 0;
                                        text-align: left;
                                    }
                                    .countdown {
                                        font-size: 14px;
                                        color: #EF4444;
                                        margin-top: 20px;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <h2>⚠️ Account Deletion Request</h2>
                                    </div>
                                    <div class="content">
                                        <h3>Hello!</h3>
                                        <p>You have requested to delete your SafeChat account.</p>
                                        <div class="otp-code">%06d</div>
                                        <div class="warning">
                                            <strong>⚠️ Important:</strong> After verification, your account will be scheduled for deletion in 24 hours.
                                            You can cancel this request anytime within 24 hours.
                                        </div>
                                        <p class="countdown">⏰ This code will expire in 1 minute</p>
                                        <p>If you did not request this, please secure your account immediately.</p>
                                        <hr>
                                        <p style="font-size: 12px; color: #6B7280;">SafeChat - Your Privacy-First Messaging App</p>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                    otp);

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

            log.info("{} - Account deletion request OTP sent successfully to email: {}", METHOD_NAME, toEmail);

        } catch (Exception e) {
            log.warn("{} - Failed to send account deletion request OTP email to {}: {}", METHOD_NAME, toEmail,
                    e.getMessage());
            log.debug("{} - Exception details: ", METHOD_NAME, e);
        }
    }

    @Async
    public void sendAccountDeletionInstantOtp(String toEmail, int otp) {
        final String METHOD_NAME = "sendAccountDeletionInstantOtp";

        log.debug("{} - Preparing to send instant account deletion OTP to email: {}", METHOD_NAME, toEmail);
        log.debug("{} - OTP value: {} for email: {}", METHOD_NAME, otp, toEmail);
        log.warn("{} - Sending INSTANT DELETION OTP (IRREVERSIBLE action) to email: {}", METHOD_NAME, toEmail);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Instant Account Deletion OTP - SafeChat");

            String htmlContent = String.format(
                    """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <style>
                                    @keyframes pulse {
                                        0%% { transform: scale(1); }
                                        50%% { transform: scale(1.1); }
                                        100%% { transform: scale(1); }
                                    }
                                    @keyframes fadeIn {
                                        from { opacity: 0; }
                                        to { opacity: 1; }
                                    }
                                    .otp-code {
                                        font-size: 32px;
                                        font-weight: bold;
                                        color: #991B1B;
                                        background: #FEE2E2;
                                        padding: 15px;
                                        border-radius: 10px;
                                        display: inline-block;
                                        animation: pulse 1s ease-in-out 3;
                                        letter-spacing: 5px;
                                    }
                                    .container {
                                        font-family: Arial, sans-serif;
                                        max-width: 600px;
                                        margin: 0 auto;
                                        padding: 20px;
                                        animation: fadeIn 0.5s ease-in;
                                    }
                                    .header {
                                        background: linear-gradient(135deg, #991B1B, #7F1D1D);
                                        color: white;
                                        padding: 20px;
                                        text-align: center;
                                        border-radius: 10px 10px 0 0;
                                    }
                                    .content {
                                        background: #f9fafb;
                                        padding: 30px;
                                        border-radius: 0 0 10px 10px;
                                        text-align: center;
                                    }
                                    .warning-box {
                                        background: #FEE2E2;
                                        border: 2px solid #DC2626;
                                        padding: 15px;
                                        margin: 20px 0;
                                        border-radius: 8px;
                                    }
                                    .countdown {
                                        font-size: 14px;
                                        color: #EF4444;
                                        margin-top: 20px;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <h2>⚠️⚠️ INSTANT DELETION ⚠️⚠️</h2>
                                    </div>
                                    <div class="content">
                                        <h3>Hello!</h3>
                                        <p>You have requested to IMMEDIATELY delete your SafeChat account.</p>
                                        <div class="otp-code">%06d</div>
                                        <div class="warning-box">
                                            <strong>⚠️⚠️ WARNING: This action is IRREVERSIBLE! ⚠️⚠️</strong><br>
                                            All your data will be permanently deleted.
                                        </div>
                                        <p class="countdown">⏰ This code will expire in 1 minute</p>
                                        <p>If you did not request this, please secure your account immediately.</p>
                                        <hr>
                                        <p style="font-size: 12px; color: #6B7280;">SafeChat - Your Privacy-First Messaging App</p>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                    otp);

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

            log.info("{} - Instant account deletion OTP sent successfully to email: {}", METHOD_NAME, toEmail);

        } catch (Exception e) {
            log.warn("{} - Failed to send instant account deletion OTP email to {}: {}", METHOD_NAME, toEmail,
                    e.getMessage());
            log.debug("{} - Exception details: ", METHOD_NAME, e);
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String displayName) {
        final String METHOD_NAME = "sendWelcomeEmail";

        log.debug("{} - Preparing to send welcome email to: {}", METHOD_NAME, toEmail);
        log.debug("{} - Display name: {} for email: {}", METHOD_NAME, displayName, toEmail);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Welcome to SafeChat!");

            String htmlContent = String.format(
                    """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <style>
                                    @keyframes fadeIn {
                                        from { opacity: 0; transform: translateY(20px); }
                                        to { opacity: 1; transform: translateY(0); }
                                    }
                                    @keyframes bounce {
                                        0%% { transform: translateY(0); }
                                        50%% { transform: translateY(-10px); }
                                        100%% { transform: translateY(0); }
                                    }
                                    .container {
                                        font-family: Arial, sans-serif;
                                        max-width: 600px;
                                        margin: 0 auto;
                                        padding: 20px;
                                        animation: fadeIn 0.8s ease-in;
                                    }
                                    .header {
                                        background: linear-gradient(135deg, #10B981, #059669);
                                        color: white;
                                        padding: 20px;
                                        text-align: center;
                                        border-radius: 10px 10px 0 0;
                                    }
                                    .content {
                                        background: #f9fafb;
                                        padding: 30px;
                                        border-radius: 0 0 10px 10px;
                                        text-align: center;
                                    }
                                    .welcome-icon {
                                        font-size: 64px;
                                        animation: bounce 1s ease-in-out;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <h2>🎉 Welcome to SafeChat!</h2>
                                    </div>
                                    <div class="content">
                                        <div class="welcome-icon">🚀</div>
                                        <h3>Hello %s!</h3>
                                        <p>Welcome to SafeChat - your privacy-first messaging app!</p>
                                        <p>Your account has been successfully created.</p>
                                        <p><strong>Start connecting with friends securely.</strong></p>
                                        <hr>
                                        <p style="font-size: 12px; color: #6B7280;">SafeChat - Your Privacy-First Messaging App</p>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                    displayName);

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

            log.info("{} - Welcome email sent successfully to: {}", METHOD_NAME, toEmail);

        } catch (Exception e) {
            log.warn("{} - Failed to send welcome email to {}: {}", METHOD_NAME, toEmail, e.getMessage());
            log.debug("{} - Exception details: ", METHOD_NAME, e);
        }
    }

    @Async
    public void sendAfterDeletionEmail(String toEmail) {
        final String METHOD_NAME = "sendAfterDeletionEmail";

        log.debug("{} - Preparing to send post-deletion email to: {}", METHOD_NAME, toEmail);
        log.warn("{} - Sending account deletion confirmation email to: {}", METHOD_NAME, toEmail);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Account Deleted - SafeChat");

            String htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <style>
                            @keyframes fadeOut {
                                from { opacity: 1; }
                                to { opacity: 0.5; }
                            }
                            @keyframes fadeIn {
                                from { opacity: 0; }
                                to { opacity: 1; }
                            }
                            .container {
                                font-family: Arial, sans-serif;
                                max-width: 600px;
                                margin: 0 auto;
                                padding: 20px;
                                animation: fadeIn 0.5s ease-in;
                            }
                            .header {
                                background: linear-gradient(135deg, #6B7280, #4B5563);
                                color: white;
                                padding: 20px;
                                text-align: center;
                                border-radius: 10px 10px 0 0;
                            }
                            .content {
                                background: #f9fafb;
                                padding: 30px;
                                border-radius: 0 0 10px 10px;
                                text-align: center;
                            }
                            .goodbye-icon {
                                font-size: 64px;
                                animation: fadeOut 2s ease-in-out;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <h2>👋 Account Deleted</h2>
                            </div>
                            <div class="content">
                                <div class="goodbye-icon">😢</div>
                                <h3>Hello!</h3>
                                <p>Your SafeChat account has been successfully deleted.</p>
                                <p>All your data has been removed from our systems.</p>
                                <p><strong>We are sorry to see you go!</strong></p>
                                <hr>
                                <p style="font-size: 12px; color: #6B7280;">SafeChat - Your Privacy-First Messaging App</p>
                            </div>
                        </div>
                    </body>
                    </html>
                    """;

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

            log.info("{} - Post-deletion email sent successfully to: {}", METHOD_NAME, toEmail);

        } catch (Exception e) {
            log.warn("{} - Failed to send post-deletion email to {}: {}", METHOD_NAME, toEmail, e.getMessage());
            log.debug("{} - Exception details: ", METHOD_NAME, e);
        }
    }

    @Async
    public void sendPasswordResetOtp(String toEmail, int otp) {
        final String METHOD_NAME = "sendPasswordResetOtp";

        log.debug("{} - Preparing to send password reset OTP to email: {}", METHOD_NAME, toEmail);
        log.debug("{} - OTP value: {} for email: {}", METHOD_NAME, otp, toEmail);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Password Reset OTP - SafeChat");

            String htmlContent = String.format(
                    """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <style>
                                    @keyframes pulse {
                                        0%% { transform: scale(1); }
                                        50%% { transform: scale(1.1); }
                                        100%% { transform: scale(1); }
                                    }
                                    @keyframes fadeIn {
                                        from { opacity: 0; }
                                        to { opacity: 1; }
                                    }
                                    @keyframes spin {
                                        from { transform: rotate(0deg); }
                                        to { transform: rotate(360deg); }
                                    }
                                    .otp-code {
                                        font-size: 32px;
                                        font-weight: bold;
                                        color: #F59E0B;
                                        background: #FEF3C7;
                                        padding: 15px;
                                        border-radius: 10px;
                                        display: inline-block;
                                        animation: pulse 1s ease-in-out 3;
                                        letter-spacing: 5px;
                                    }
                                    .container {
                                        font-family: Arial, sans-serif;
                                        max-width: 600px;
                                        margin: 0 auto;
                                        padding: 20px;
                                        animation: fadeIn 0.5s ease-in;
                                    }
                                    .header {
                                        background: linear-gradient(135deg, #F59E0B, #D97706);
                                        color: white;
                                        padding: 20px;
                                        text-align: center;
                                        border-radius: 10px 10px 0 0;
                                    }
                                    .content {
                                        background: #f9fafb;
                                        padding: 30px;
                                        border-radius: 0 0 10px 10px;
                                        text-align: center;
                                    }
                                    .reset-icon {
                                        font-size: 48px;
                                        animation: spin 1s ease-in-out;
                                    }
                                    .countdown {
                                        font-size: 14px;
                                        color: #EF4444;
                                        margin-top: 20px;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <h2>🔄 Password Reset</h2>
                                    </div>
                                    <div class="content">
                                        <div class="reset-icon">🔑</div>
                                        <h3>Hello!</h3>
                                        <p>You requested to reset your password.</p>
                                        <div class="otp-code">%06d</div>
                                        <p class="countdown">⏰ This code will expire in 5 minutes</p>
                                        <p>If you did not request this, please ignore this email.</p>
                                        <hr>
                                        <p style="font-size: 12px; color: #6B7280;">SafeChat - Your Privacy-First Messaging App</p>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                    otp);

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

            log.info("{} - Password reset OTP sent successfully to email: {}", METHOD_NAME, toEmail);

        } catch (Exception e) {
            log.warn("{} - Failed to send password reset OTP email to {}: {}", METHOD_NAME, toEmail, e.getMessage());
            log.debug("{} - Exception details: ", METHOD_NAME, e);
        }
    }
}