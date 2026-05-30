package com.melodyshop.user.client.fallback;

import com.melodyshop.user.client.OrderClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderClientFallback implements OrderClient {

    @Override
    public boolean hasOrdersByUserId(String userId) {
        log.warn("Fallback: Assuming user {} has orders (service unavailable)", userId);
        return true;
    }
}
