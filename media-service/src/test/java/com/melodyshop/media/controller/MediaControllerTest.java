package com.melodyshop.media.controller;

import com.melodyshop.media.dto.UploadResponse;
import com.melodyshop.media.service.CloudinaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.melodyshop.common.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Import;

@WebMvcTest(MediaController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("MediaController Integration Tests (MockMvc)")
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CloudinaryService cloudinaryService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/media/ping
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /ping → 200 OK with service name")
    void ping_returns200AndServiceName() throws Exception {
        mockMvc.perform(get("/api/media/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.service").value("media-service"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/media/upload
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /upload with valid JPEG file → 201 Created, returns URL and publicId")
    void upload_withValidJpeg_returns201() throws Exception {
        UploadResponse response = UploadResponse.builder()
                .url("https://res.cloudinary.com/demo/melodyshop/products/guitar.jpg")
                .publicId("melodyshop/products/guitar")
                .format("jpg")
                .bytes(54321L)
                .width(1280)
                .height(720)
                .folder("melodyshop/products")
                .build();

        when(cloudinaryService.upload(any(), anyString())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file", "guitar.jpg", "image/jpeg", new byte[1024]);

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("type", "product"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url").value(containsString("cloudinary.com")))
                .andExpect(jsonPath("$.data.publicId").value("melodyshop/products/guitar"))
                .andExpect(jsonPath("$.data.format").value("jpg"))
                .andExpect(jsonPath("$.data.width").value(1280))
                .andExpect(jsonPath("$.data.height").value(720));

        verify(cloudinaryService).upload(any(), eq("product"));
    }

    @Test
    @DisplayName("POST /upload defaults to type=product when no type param given")
    void upload_withoutTypeParam_defaultsToProduct() throws Exception {
        UploadResponse response = UploadResponse.builder()
                .url("https://res.cloudinary.com/demo/image.jpg")
                .publicId("melodyshop/products/image")
                .format("jpg").bytes(100L).width(100).height(100)
                .folder("melodyshop/products")
                .build();

        when(cloudinaryService.upload(any(), anyString())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file", "image.jpg", "image/jpeg", new byte[256]);

        mockMvc.perform(multipart("/api/media/upload").file(file))
                .andExpect(status().isCreated());

        verify(cloudinaryService).upload(any(), eq("product"));
    }

    @Test
    @DisplayName("POST /upload for avatar type → 201 Created with correct folder")
    void upload_avatarType_returns201() throws Exception {
        UploadResponse response = UploadResponse.builder()
                .url("https://res.cloudinary.com/demo/avatar.png")
                .publicId("melodyshop/avatars/user-123")
                .format("png").bytes(20480L).width(200).height(200)
                .folder("melodyshop/avatars")
                .build();

        when(cloudinaryService.upload(any(), eq("avatar"))).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[500]);

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("type", "avatar"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.folder").value("melodyshop/avatars"));
    }

    @Test
    @DisplayName("POST /upload when service throws IllegalArgumentException → 500 (propagated)")
    void upload_whenServiceThrowsValidationError_returns500() throws Exception {
        when(cloudinaryService.upload(any(), anyString()))
                .thenThrow(new IllegalArgumentException("Unsupported file type: application/pdf"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[100]);

        mockMvc.perform(multipart("/api/media/upload").file(file))
                .andExpect(status().is5xxServerError());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/media/delete
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /delete with ADMIN role → 200 OK, returns deleted publicId")
    void delete_withAdminRole_returns200() throws Exception {
        doNothing().when(cloudinaryService).delete(anyString());

        mockMvc.perform(delete("/api/media/delete")
                        .param("publicId", "melodyshop/products/guitar-001")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("deleted"))
                .andExpect(jsonPath("$.data.publicId").value("melodyshop/products/guitar-001"));

        verify(cloudinaryService).delete("melodyshop/products/guitar-001");
    }

    @Test
    @DisplayName("DELETE /delete without ADMIN role → 403 Forbidden")
    void delete_withoutAdminRole_returns403() throws Exception {
        mockMvc.perform(delete("/api/media/delete")
                        .param("publicId", "melodyshop/products/guitar-001")
                        .header("X-User-Role", "ROLE_CUSTOMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Admin")));

        verifyNoInteractions(cloudinaryService);
    }

    @Test
    @DisplayName("DELETE /delete without role header → 403 Forbidden")
    void delete_withNoRoleHeader_returns403() throws Exception {
        mockMvc.perform(delete("/api/media/delete")
                        .param("publicId", "melodyshop/products/guitar-001"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cloudinaryService);
    }

    @Test
    @DisplayName("DELETE /delete when service throws → 500 Internal Server Error")
    void delete_whenServiceThrows_returns500() throws Exception {
        doThrow(new RuntimeException("Cloudinary error"))
                .when(cloudinaryService).delete(anyString());

        mockMvc.perform(delete("/api/media/delete")
                        .param("publicId", "melodyshop/products/guitar-001")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().is5xxServerError());
    }
}
