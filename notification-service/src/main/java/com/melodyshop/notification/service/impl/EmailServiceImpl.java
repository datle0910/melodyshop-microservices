package com.melodyshop.notification.service.impl;

import com.melodyshop.notification.dto.EmailRequest;
import com.melodyshop.notification.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.security.SecureRandom;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${notification.from-email}")
    private String fromEmail;

    @Value("${notification.from-name}")
    private String fromName;

    @Value("${notification.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Override
    public void sendEmail(EmailRequest request) {
        try {
            // Build Thymeleaf context
            Context context = new Context();
            if (request.getVariables() != null) {
                context.setVariables(request.getVariables());
            }

            // Process template
            String htmlContent = templateEngine.process(request.getTemplateName(), context);

            // Send
            sendHtmlEmail(request.getTo(), request.getSubject(), htmlContent);
            log.info("Email sent to {} using template '{}'", request.getTo(), request.getTemplateName());

        } catch (Exception e) {
            log.error("Failed to send email to {} — {}", request.getTo(), e.getMessage(), e);
            throw new RuntimeException("Email sending failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String sendOtp(String toEmail, String recipientName) {
        String otp = generateOtp();

        sendEmail(EmailRequest.builder()
                .to(toEmail)
                .subject("Mã xác thực OTP - MelodyShop")
                .templateName("otp")
                .variables(Map.of(
                        "recipientName", recipientName,
                        "otp", otp,
                        "expiryMinutes", otpExpiryMinutes
                ))
                .build());

        log.info("OTP email sent to {}", toEmail);
        return otp;
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String fullName) {
        sendEmail(EmailRequest.builder()
                .to(toEmail)
                .subject("Chào mừng bạn đến với MelodyShop! 🎵")
                .templateName("welcome")
                .variables(Map.of("fullName", fullName))
                .build());

        log.info("Welcome email sent to {}", toEmail);
    }

    @Override
    public void sendOrderConfirmationEmail(String toEmail, String fullName,
                                           String orderCode, String totalAmount) {
        sendEmail(EmailRequest.builder()
                .to(toEmail)
                .subject("Xác nhận đơn hàng #" + orderCode + " - MelodyShop")
                .templateName("order-confirmed")
                .variables(Map.of(
                        "fullName", fullName,
                        "orderCode", orderCode,
                        "totalAmount", totalAmount
                ))
                .build());

        log.info("Order confirmation email sent to {} for order {}", toEmail, orderCode);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void sendHtmlEmail(String to, String subject, String htmlContent)
            throws MessagingException, java.io.UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = HTML

        mailSender.send(message);
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100_000 + random.nextInt(900_000); // 6-digit OTP
        return String.valueOf(otp);
    }
}
