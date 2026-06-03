package com.melodyshop.ai.application.dto;

import java.util.ArrayList;
import java.util.List;

public class ChatResponse {

    public enum ResponseType {
        text, product_list, product_detail, stock_check, cart_added, category_list, error
    }

    private String response;
    private String type;
    private List<ProductInfoDto> products;
    private ProductDetailDto product;
    private StockInfoDto stockCheck;
    private CartInfoDto cartAdded;
    private List<CategoryInfoDto> categories;

    public ChatResponse() {}

    public ChatResponse(String response, String type) {
        this.response = response;
        this.type = type;
    }

    // Static factory methods for each response type
    public static ChatResponse text(String message) {
        ChatResponse r = new ChatResponse();
        r.setResponse(message);
        r.setType("text");
        return r;
    }

    public static ChatResponse error(String message) {
        ChatResponse r = new ChatResponse();
        r.setResponse(message);
        r.setType("error");
        return r;
    }

    public static ChatResponse productList(String message, List<ProductInfoDto> products) {
        ChatResponse r = new ChatResponse();
        r.setResponse(message);
        r.setType("product_list");
        r.setProducts(products);
        return r;
    }

    public static ChatResponse productDetail(String message, ProductDetailDto product) {
        ChatResponse r = new ChatResponse();
        r.setResponse(message);
        r.setType("product_detail");
        r.setProduct(product);
        return r;
    }

    public static ChatResponse stockCheck(String message, StockInfoDto stockCheck) {
        ChatResponse r = new ChatResponse();
        r.setResponse(message);
        r.setType("stock_check");
        r.setStockCheck(stockCheck);
        return r;
    }

    public static ChatResponse cartAdded(String message, CartInfoDto cartAdded) {
        ChatResponse r = new ChatResponse();
        r.setResponse(message);
        r.setType("cart_added");
        r.setCartAdded(cartAdded);
        return r;
    }

    public static ChatResponse categoryList(String message, List<CategoryInfoDto> categories) {
        ChatResponse r = new ChatResponse();
        r.setResponse(message);
        r.setType("category_list");
        r.setCategories(categories);
        return r;
    }

    // Getters and setters
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<ProductInfoDto> getProducts() { return products; }
    public void setProducts(List<ProductInfoDto> products) { this.products = products; }

    public ProductDetailDto getProduct() { return product; }
    public void setProduct(ProductDetailDto product) { this.product = product; }

    public StockInfoDto getStockCheck() { return stockCheck; }
    public void setStockCheck(StockInfoDto stockCheck) { this.stockCheck = stockCheck; }

    public CartInfoDto getCartAdded() { return cartAdded; }
    public void setCartAdded(CartInfoDto cartAdded) { this.cartAdded = cartAdded; }

    public List<CategoryInfoDto> getCategories() { return categories; }
    public void setCategories(List<CategoryInfoDto> categories) { this.categories = categories; }

    // DTOs matching frontend expectations
    public static class ProductInfoDto {
        private String id;
        private String name;
        private String slug;
        private String image;
        private Double price;
        private String category;
        private String brand;
        private Double rating;
        private Boolean inStock;

        public ProductInfoDto() {}
        public ProductInfoDto(String id, String name, String slug, String image, Double price,
                            String category, String brand, Double rating, Boolean inStock) {
            this.id = id; this.name = name; this.slug = slug; this.image = image;
            this.price = price; this.category = category; this.brand = brand;
            this.rating = rating; this.inStock = inStock;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }
        public Boolean getInStock() { return inStock; }
        public boolean isInStock() { return Boolean.TRUE.equals(inStock); }
        public void setInStock(Boolean inStock) { this.inStock = inStock; }
    }

    public static class VariantInfoDto {
        private String id;
        private String sku;
        private String name;
        private Double price;
        private Integer stockQuantity;
        private Boolean inStock;

        public VariantInfoDto() {}
        public VariantInfoDto(String id, String sku, String name, Double price,
                            Integer stockQuantity, Boolean inStock) {
            this.id = id; this.sku = sku; this.name = name; this.price = price;
            this.stockQuantity = stockQuantity; this.inStock = inStock;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public Integer getStockQuantity() { return stockQuantity; }
        public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
        public Boolean getInStock() { return inStock; }
        public boolean isInStock() { return Boolean.TRUE.equals(inStock); }
        public void setInStock(Boolean inStock) { this.inStock = inStock; }
    }

    public static class ProductDetailDto {
        private String id;
        private String name;
        private String slug;
        private String description;
        private Double basePrice;
        private String category;
        private String brand;
        private Double rating;
        private Integer reviewCount;
        private Boolean inStock;
        private List<VariantInfoDto> variants;

        public ProductDetailDto() {}
        public ProductDetailDto(String id, String name, String slug, String description,
                               Double basePrice, String category, String brand,
                               Double rating, Integer reviewCount, Boolean inStock,
                               List<VariantInfoDto> variants) {
            this.id = id; this.name = name; this.slug = slug; this.description = description;
            this.basePrice = basePrice; this.category = category; this.brand = brand;
            this.rating = rating; this.reviewCount = reviewCount; this.inStock = inStock;
            this.variants = variants;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Double getBasePrice() { return basePrice; }
        public void setBasePrice(Double basePrice) { this.basePrice = basePrice; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }
        public Integer getReviewCount() { return reviewCount; }
        public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
        public Boolean getInStock() { return inStock; }
        public boolean isInStock() { return Boolean.TRUE.equals(inStock); }
        public void setInStock(Boolean inStock) { this.inStock = inStock; }
        public List<VariantInfoDto> getVariants() { return variants; }
        public void setVariants(List<VariantInfoDto> variants) { this.variants = variants; }
    }

    public static class StockInfoDto {
        private Boolean inStock;
        private Integer availableQuantity;

        public StockInfoDto() {}
        public StockInfoDto(Boolean inStock, Integer availableQuantity) {
            this.inStock = inStock; this.availableQuantity = availableQuantity;
        }
        public Boolean getInStock() { return inStock; }
        public boolean isInStock() { return Boolean.TRUE.equals(inStock); }
        public void setInStock(Boolean inStock) { this.inStock = inStock; }
        public Integer getAvailableQuantity() { return availableQuantity; }
        public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
    }

    public static class CartInfoDto {
        private String id;
        private String productId;
        private String productName;
        private Integer quantity;
        private Double subtotal;

        public CartInfoDto() {}
        public CartInfoDto(String id, String productId, String productName, Integer quantity, Double subtotal) {
            this.id = id; this.productId = productId; this.productName = productName;
            this.quantity = quantity; this.subtotal = subtotal;
        }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Double getSubtotal() { return subtotal; }
        public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    }

    public static class CategoryInfoDto {
        private String id;
        private String name;
        private String slug;
        private String image;

        public CategoryInfoDto() {}
        public CategoryInfoDto(String id, String name, String slug, String image) {
            this.id = id; this.name = name; this.slug = slug; this.image = image;
        }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
    }
}
