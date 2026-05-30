package com.melodyshop.ai.domain.model;

public record ProductSummary(
    String id,
    String name,
    Double price,
    Boolean inStock
) {}
