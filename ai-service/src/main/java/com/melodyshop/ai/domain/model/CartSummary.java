package com.melodyshop.ai.domain.model;

public record CartSummary(
    String cartId,
    int totalItems,
    Double totalPrice
) {}
