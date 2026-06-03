package com.melodyshop.ai.application.dto;

public class ChatRequest {
    private String userId;
    private String message;
    private ProductContextDto productContext;

    public ChatRequest() {}

    public ChatRequest(String userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public ProductContextDto getProductContext() { return productContext; }
    public void setProductContext(ProductContextDto productContext) { this.productContext = productContext; }

    public static class ProductContextDto {
        private String id;
        private String name;
        private String slug;
        private Double price;
        private Boolean inStock;

        public ProductContextDto() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }

        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }

        public Boolean getInStock() { return inStock; }
        public void setInStock(Boolean inStock) { this.inStock = inStock; }
    }
}
