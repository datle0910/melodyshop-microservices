package com.melodyshop.user.client;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.user.config.feign.MediaServiceClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@FeignClient(name = "media-service", fallbackFactory = MediaServiceClientFallbackFactory.class)
public interface MediaServiceClient {

    @PostMapping(value = "/api/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<Map<String, Object>> uploadFile(@RequestParam("type") String type, @RequestPart("file") MultipartFile file);

}
