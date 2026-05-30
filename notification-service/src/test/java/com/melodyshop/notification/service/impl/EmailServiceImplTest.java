package com.melodyshop.notification.service.impl;

import com.melodyshop.notification.dto.EmailRequest;
import com.melodyshop.notification.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailServiceImpl Unit Tests")
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Spy
    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@melodyshop.vn");
        ReflectionTestUtils.setField(emailService, "fromName", "MelodyShop");
        ReflectionTestUtils.setField(emailService, "otpExpiryMinutes", 5);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OTP Generation Tests
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("sendOtp()")
    class SendOtpTests {

        @BeforeEach
        void mockSendEmail() {
            // Spy: mock the internal sendEmail so no real SMTP is needed
            doNothing().when(emailService).sendEmail(any(EmailRequest.class));
        }

        @Test
        @DisplayName("Should return a 6-digit numeric OTP")
        void shouldReturn6DigitNumericOtp() {
            String otp = emailService.sendOtp("user@test.com", "Nguyen Van A", null);

            assertThat(otp).isNotNull()
                    .hasSize(6)
                    .matches("[0-9]{6}");
        }

        @Test
        @DisplayName("Should delegate to sendEmail with 'otp' template")
        void shouldCallSendEmailWithOtpTemplate() {
            ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);

            emailService.sendOtp("user@test.com", "Nguyen Van A", null);

            verify(emailService).sendEmail(captor.capture());
            EmailRequest captured = captor.getValue();

            assertThat(captured.getTo()).isEqualTo("user@test.com");
            assertThat(captured.getTemplateName()).isEqualTo("otp");
            assertThat(captured.getVariables())
                    .containsKey("otp")
                    .containsKey("recipientName")
                    .containsKey("expiryMinutes");
        }

        @Test
        @DisplayName("Should generate different OTPs across multiple calls (randomness)")
        void shouldProduceDifferentOtpsAcrossCalls() {
            Set<String> otps = IntStream.range(0, 20)
                    .mapToObj(i -> emailService.sendOtp("user@test.com", "User " + i, null))
                    .collect(Collectors.toSet());

            // With 20 calls generating 6-digit OTPs, at least 5 should be unique
            assertThat(otps.size()).isGreaterThan(5);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Convenience Method Delegation Tests
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Convenience email methods (sendWelcome, sendOrderConfirmation)")
    class ConvenienceMethodTests {

        @BeforeEach
        void mockSendEmail() {
            doNothing().when(emailService).sendEmail(any(EmailRequest.class));
        }

        @Test
        @DisplayName("sendWelcomeEmail() should use 'welcome' template")
        void sendWelcomeEmail_shouldUseWelcomeTemplate() {
            ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);

            emailService.sendWelcomeEmail("user@test.com", "Nguyen Van A");

            verify(emailService).sendEmail(captor.capture());
            EmailRequest req = captor.getValue();

            assertThat(req.getTo()).isEqualTo("user@test.com");
            assertThat(req.getTemplateName()).isEqualTo("welcome");
            assertThat(req.getVariables()).containsEntry("fullName", "Nguyen Van A");
        }

        @Test
        @DisplayName("sendOrderConfirmationEmail() should use 'order-confirmed' template with orderCode")
        void sendOrderConfirmationEmail_shouldUseOrderTemplate() {
            ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);

            emailService.sendOrderConfirmationEmail(
                    "user@test.com", "Nguyen Van A", "ORD-2024-001", "5,000,000 ₫");

            verify(emailService).sendEmail(captor.capture());
            EmailRequest req = captor.getValue();

            assertThat(req.getTemplateName()).isEqualTo("order-confirmed");
            assertThat(req.getVariables())
                    .containsEntry("orderCode", "ORD-2024-001")
                    .containsEntry("totalAmount", "5,000,000 ₫")
                    .containsEntry("fullName", "Nguyen Van A");
            assertThat(req.getSubject()).contains("ORD-2024-001");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core sendEmail() Tests
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("sendEmail() — core logic")
    class SendEmailCoreTests {

        @Test
        @DisplayName("Should process Thymeleaf template and call mailSender.send()")
        void shouldProcessTemplateAndSendMail() {
            // Arrange
            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(templateEngine.process(eq("welcome"), any(Context.class)))
                    .thenReturn("<html><body>Welcome!</body></html>");
            doNothing().when(mailSender).send(any(MimeMessage.class));

            EmailRequest request = EmailRequest.builder()
                    .to("user@test.com")
                    .subject("Chào mừng bạn!")
                    .templateName("welcome")
                    .variables(Map.of("fullName", "Nguyen Van A"))
                    .build();

            // Act & Assert — should not throw
            assertThatNoException().isThrownBy(() -> emailService.sendEmail(request));

            verify(templateEngine).process(eq("welcome"), any(Context.class));
            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should inject template variables into Thymeleaf context")
        void shouldInjectTemplateVariables() {
            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(any(MimeMessage.class));

            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            when(templateEngine.process(anyString(), contextCaptor.capture()))
                    .thenReturn("<html>body</html>");

            Map<String, Object> vars = Map.of("orderCode", "ORD-001", "amount", "1,000,000");
            emailService.sendEmail(EmailRequest.builder()
                    .to("x@y.com").subject("S").templateName("order-confirmed")
                    .variables(vars).build());

            Context captured = contextCaptor.getValue();
            assertThat(captured.getVariable("orderCode")).isEqualTo("ORD-001");
            assertThat(captured.getVariable("amount")).isEqualTo("1,000,000");
        }

        @Test
        @DisplayName("Should wrap exception in RuntimeException when mail sending fails")
        void shouldThrowRuntimeException_whenMailSenderFails() {
            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            when(templateEngine.process(anyString(), any(Context.class)))
                    .thenReturn("<html>body</html>");
            doThrow(new RuntimeException("SMTP server unreachable"))
                    .when(mailSender).send(any(MimeMessage.class));

            EmailRequest request = EmailRequest.builder()
                    .to("user@test.com").subject("Test").templateName("welcome").build();

            assertThatThrownBy(() -> emailService.sendEmail(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email sending failed");
        }
    }
}
