package com.melodyshop.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melodyshop.notification.dto.EmailRequest;
import com.melodyshop.notification.dto.OtpRequest;
import com.melodyshop.notification.dto.WelcomeRequest;
import com.melodyshop.notification.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.melodyshop.common.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Import;

@WebMvcTest(NotificationController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("NotificationController Integration Tests (MockMvc)")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/notifications/ping
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /ping → 200 OK with service status UP")
    void ping_shouldReturn200AndServiceName() throws Exception {
        mockMvc.perform(get("/api/notifications/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.service").value("notification-service"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/notifications/email
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /email with valid request → 200 OK, returns status:sent")
    void sendEmail_withValidRequest_returns200() throws Exception {
        doNothing().when(emailService).sendEmail(any(EmailRequest.class));

        EmailRequest request = EmailRequest.builder()
                .to("user@test.com")
                .subject("Test Subject")
                .templateName("welcome")
                .variables(Map.of("fullName", "Nguyen Van A"))
                .build();

        mockMvc.perform(post("/api/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("sent"))
                .andExpect(jsonPath("$.data.to").value("user@test.com"));

        verify(emailService).sendEmail(any(EmailRequest.class));
    }

    @Test
    @DisplayName("POST /email with invalid email → 400 Bad Request")
    void sendEmail_withInvalidEmail_returns400() throws Exception {
        EmailRequest request = EmailRequest.builder()
                .to("not-a-valid-email")
                .subject("Test")
                .templateName("welcome")
                .build();

        mockMvc.perform(post("/api/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("POST /email with blank subject → 400 Bad Request")
    void sendEmail_withBlankSubject_returns400() throws Exception {
        EmailRequest request = EmailRequest.builder()
                .to("user@test.com")
                .subject("")
                .templateName("welcome")
                .build();

        mockMvc.perform(post("/api/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /email with missing body → 400 Bad Request")
    void sendEmail_withEmptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/notifications/otp
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /otp with valid request → 200 OK, returns 6-digit OTP")
    void sendOtp_withValidRequest_returns200WithOtp() throws Exception {
        when(emailService.sendOtp(anyString(), anyString(), any())).thenReturn("482931");

        OtpRequest request = OtpRequest.builder()
                .to("user@test.com")
                .recipientName("Nguyen Van A")
                .build();

        mockMvc.perform(post("/api/notifications/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("sent"))
                .andExpect(jsonPath("$.data.otp").value("482931"))
                .andExpect(jsonPath("$.data.to").value("user@test.com"));

        verify(emailService).sendOtp("user@test.com", "Nguyen Van A", null);
    }

    @Test
    @DisplayName("POST /otp with invalid email → 400 Bad Request")
    void sendOtp_withInvalidEmail_returns400() throws Exception {
        OtpRequest request = OtpRequest.builder()
                .to("invalid-email")
                .recipientName("Nguyen Van A")
                .build();

        mockMvc.perform(post("/api/notifications/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(emailService);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/notifications/welcome
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /welcome → 200 OK, calls sendWelcomeEmail")
    void sendWelcome_withValidParams_returns200() throws Exception {
        doNothing().when(emailService).sendWelcomeEmail(anyString(), anyString());

        WelcomeRequest request = WelcomeRequest.builder()
                .email("user@test.com")
                .fullName("Nguyen Van A")
                .build();

        mockMvc.perform(post("/api/notifications/welcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("sent"))
                .andExpect(jsonPath("$.data.to").value("user@test.com"));

        verify(emailService).sendWelcomeEmail("user@test.com", "Nguyen Van A");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/notifications/order-confirmed
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /order-confirmed → 200 OK, returns orderCode in response")
    void sendOrderConfirmed_withValidParams_returns200() throws Exception {
        doNothing().when(emailService)
                .sendOrderConfirmationEmail(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/notifications/order-confirmed")
                        .param("email", "user@test.com")
                        .param("fullName", "Nguyen Van A")
                        .param("orderCode", "ORD-2024-001")
                        .param("totalAmount", "5,000,000 VND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("sent"))
                .andExpect(jsonPath("$.data.orderCode").value("ORD-2024-001"));

        verify(emailService).sendOrderConfirmationEmail(
                "user@test.com", "Nguyen Van A", "ORD-2024-001", "5,000,000 VND");
    }
}
