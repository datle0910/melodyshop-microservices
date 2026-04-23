package com.melodyshop.engagement.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.engagement.dto.OrderCompletedEventRequest;
import com.melodyshop.engagement.dto.OrderCompletedEventResponse;
import com.melodyshop.engagement.service.PurchaseEligibilityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/engagement/internal/events")
public class EngagementEventController {

    private final PurchaseEligibilityService purchaseEligibilityService;

    public EngagementEventController(PurchaseEligibilityService purchaseEligibilityService) {
        this.purchaseEligibilityService = purchaseEligibilityService;
    }

    @PostMapping("/order-completed")
    public ResponseEntity<ApiResponse<OrderCompletedEventResponse>> handleOrderCompletedEvent(
            @Valid @RequestBody OrderCompletedEventRequest request) {
        int processedProducts = purchaseEligibilityService.handleOrderCompletedEvent(
                request.getOrderId(),
                request.getUserId(),
                request.getProductIds()
        );

        OrderCompletedEventResponse response = new OrderCompletedEventResponse(
                request.getOrderId(),
                request.getUserId(),
                processedProducts
        );

        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật purchased_products", response));
    }
}
