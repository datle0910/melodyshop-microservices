package com.melodyshop.media.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.melodyshop.media.dto.UploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CloudinaryServiceImpl Unit Tests")
class CloudinaryServiceImplTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryServiceImpl cloudinaryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cloudinaryService, "allowedTypes",
                List.of("image/jpeg", "image/png", "image/webp", "image/gif"));
        ReflectionTestUtils.setField(cloudinaryService, "folders",
                Map.of(
                        "product", "melodyshop/products",
                        "avatar",  "melodyshop/avatars",
                        "review",  "melodyshop/reviews"
                ));
        // Default: cloudinary.uploader() returns mock uploader
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Upload — Happy path
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("upload() — success scenarios")
    class UploadSuccessTests {

        private Map<Object, Object> buildCloudinaryResult(String publicId, String folder) {
            Map<Object, Object> result = new HashMap<>();
            result.put("secure_url", "https://res.cloudinary.com/demo/" + publicId + ".jpg");
            result.put("public_id",  folder + "/" + publicId);
            result.put("format",     "jpg");
            result.put("bytes",      54321);
            result.put("width",      1280);
            result.put("height",     720);
            return result;
        }

        @Test
        @DisplayName("Should return UploadResponse with correct URL and publicId for product")
        void upload_validJpeg_product_shouldReturnCorrectResponse() throws IOException {
            when(uploader.upload(any(), anyMap()))
                    .thenReturn(buildCloudinaryResult("guitar-001", "melodyshop/products"));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "guitar.jpg", "image/jpeg", new byte[1024]);

            UploadResponse response = cloudinaryService.upload(file, "product");

            assertThat(response).isNotNull();
            assertThat(response.getUrl()).startsWith("https://res.cloudinary.com");
            assertThat(response.getPublicId()).contains("melodyshop/products");
            assertThat(response.getFormat()).isEqualTo("jpg");
            assertThat(response.getBytes()).isEqualTo(54321L);
            assertThat(response.getWidth()).isEqualTo(1280);
            assertThat(response.getHeight()).isEqualTo(720);
            assertThat(response.getFolder()).isEqualTo("melodyshop/products");
        }

        @Test
        @DisplayName("PNG avatar should route to 'melodyshop/avatars' folder")
        void upload_validPng_avatarType_shouldRouteToAvatarFolder() throws IOException {
            when(uploader.upload(any(), anyMap()))
                    .thenReturn(buildCloudinaryResult("user-123", "melodyshop/avatars"));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.png", "image/png", new byte[512]);

            UploadResponse response = cloudinaryService.upload(file, "avatar");

            assertThat(response.getFolder()).isEqualTo("melodyshop/avatars");
        }

        @Test
        @DisplayName("Unknown type should fall back to product folder")
        void upload_unknownType_shouldFallBackToProductFolder() throws IOException {
            when(uploader.upload(any(), anyMap()))
                    .thenReturn(buildCloudinaryResult("img-001", "melodyshop/products"));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "image.jpg", "image/jpeg", new byte[256]);

            UploadResponse response = cloudinaryService.upload(file, "unknown-type");

            assertThat(response.getFolder()).isEqualTo("melodyshop/products");
        }

        @Test
        @DisplayName("Null type should fall back to product folder")
        void upload_nullType_shouldFallBackToProductFolder() throws IOException {
            when(uploader.upload(any(), anyMap()))
                    .thenReturn(buildCloudinaryResult("img-001", "melodyshop/products"));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "image.jpg", "image/jpeg", new byte[256]);

            UploadResponse response = cloudinaryService.upload(file, null);

            assertThat(response.getFolder()).isEqualTo("melodyshop/products");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Upload — Validation failures
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("upload() — validation failures")
    class UploadValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {"application/pdf", "text/plain", "video/mp4", "application/zip"})
        @DisplayName("Should throw IllegalArgumentException for unsupported MIME types")
        void upload_unsupportedMimeType_shouldThrow(String mimeType) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "malicious.bin", mimeType, new byte[100]);

            assertThatThrownBy(() -> cloudinaryService.upload(file, "product"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported file type");

            verifyNoInteractions(uploader);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for empty file")
        void upload_emptyFile_shouldThrow() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "empty.jpg", "image/jpeg", new byte[0]); // ← 0 bytes

            assertThatThrownBy(() -> cloudinaryService.upload(file, "product"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not be empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for null file")
        void upload_nullFile_shouldThrow() {
            assertThatThrownBy(() -> cloudinaryService.upload(null, "product"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Upload — Cloudinary SDK error
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("upload() — Cloudinary SDK errors")
    class UploadCloudinaryErrorTests {

        @Test
        @DisplayName("Should wrap IOException in RuntimeException when Cloudinary fails")
        void upload_cloudinaryThrowsIOException_shouldWrapInRuntime() throws IOException {
            when(uploader.upload(any(), anyMap()))
                    .thenThrow(new IOException("Network timeout"));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "img.jpg", "image/jpeg", new byte[200]);

            assertThatThrownBy(() -> cloudinaryService.upload(file, "product"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Upload to Cloudinary failed")
                    .hasCauseInstanceOf(IOException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete Tests
    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Should call Cloudinary destroy with the given publicId")
        void delete_validPublicId_shouldCallDestroy() throws IOException {
            when(uploader.destroy(anyString(), anyMap()))
                    .thenReturn(Map.of("result", "ok"));

            cloudinaryService.delete("melodyshop/products/guitar-001");

            verify(uploader).destroy(eq("melodyshop/products/guitar-001"), anyMap());
        }

        @Test
        @DisplayName("Should log warning (not throw) when Cloudinary returns 'not found'")
        void delete_notFound_shouldNotThrow() throws IOException {
            when(uploader.destroy(anyString(), anyMap()))
                    .thenReturn(Map.of("result", "not found"));

            assertThatNoException()
                    .isThrownBy(() -> cloudinaryService.delete("non-existent/asset"));
        }

        @Test
        @DisplayName("Should wrap IOException in RuntimeException when Cloudinary fails")
        void delete_cloudinaryThrowsIOException_shouldWrapInRuntime() throws IOException {
            when(uploader.destroy(anyString(), anyMap()))
                    .thenThrow(new IOException("Connection refused"));

            assertThatThrownBy(() -> cloudinaryService.delete("some/publicId"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Delete from Cloudinary failed")
                    .hasCauseInstanceOf(IOException.class);
        }
    }
}
