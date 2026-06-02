package com.melodyshop.ai.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ShoppingContext {
    private String userId;
    private String sessionId;
    /** Last product the user viewed in detail */
    private ProductSummary lastViewedProduct;
    /** Products mentioned/recommended in the last few turns */
    private List<ProductSummary> lastMentionedProducts;
    /** Last category the user browsed */
    private String lastCategory;
    private CartSummary currentCart;
    /** Last action: viewed_product, searched, added_to_cart, general */
    private String lastAction;
    private LocalDateTime lastUpdated;

    public ShoppingContext() {
        this.lastMentionedProducts = new ArrayList<>();
        this.lastUpdated = LocalDateTime.now();
    }

    public ShoppingContext(String userId, String sessionId, ProductSummary lastViewedProduct,
                          List<ProductSummary> lastMentionedProducts, String lastCategory,
                          CartSummary currentCart, String lastAction, LocalDateTime lastUpdated) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.lastViewedProduct = lastViewedProduct;
        this.lastMentionedProducts = lastMentionedProducts != null ? lastMentionedProducts : new ArrayList<>();
        this.lastCategory = lastCategory;
        this.currentCart = currentCart;
        this.lastAction = lastAction;
        this.lastUpdated = lastUpdated != null ? lastUpdated : LocalDateTime.now();
    }

    // Builder-style setters that return this
    public ShoppingContext userId(String userId) { this.userId = userId; return this; }
    public ShoppingContext sessionId(String sessionId) { this.sessionId = sessionId; return this; }
    public ShoppingContext lastViewedProduct(ProductSummary p) { this.lastViewedProduct = p; return this; }
    public ShoppingContext lastMentionedProducts(List<ProductSummary> p) { this.lastMentionedProducts = p; return this; }
    public ShoppingContext lastCategory(String c) { this.lastCategory = c; return this; }
    public ShoppingContext currentCart(CartSummary c) { this.currentCart = c; return this; }
    public ShoppingContext lastAction(String a) { this.lastAction = a; return this; }
    public ShoppingContext lastUpdated(LocalDateTime t) { this.lastUpdated = t; return this; }

    public ShoppingContext withLastMentionedProduct(ProductSummary p) {
        if (this.lastMentionedProducts == null) this.lastMentionedProducts = new ArrayList<>();
        // Keep only last 5 mentioned products
        if (this.lastMentionedProducts.size() >= 5) this.lastMentionedProducts.remove(0);
        this.lastMentionedProducts.add(p);
        return this;
    }

    public String getUserId() { return userId; }
    public String getSessionId() { return sessionId; }
    public ProductSummary getLastViewedProduct() { return lastViewedProduct; }
    public List<ProductSummary> getLastMentionedProducts() { return lastMentionedProducts; }
    public String getLastCategory() { return lastCategory; }
    public CartSummary getCurrentCart() { return currentCart; }
    public String getLastAction() { return lastAction; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setLastViewedProduct(ProductSummary lastViewedProduct) { this.lastViewedProduct = lastViewedProduct; }
    public void setLastMentionedProducts(List<ProductSummary> lastMentionedProducts) { this.lastMentionedProducts = lastMentionedProducts; }
    public void setLastCategory(String lastCategory) { this.lastCategory = lastCategory; }
    public void setCurrentCart(CartSummary currentCart) { this.currentCart = currentCart; }
    public void setLastAction(String lastAction) { this.lastAction = lastAction; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public String userId() { return userId; }
    public String sessionId() { return sessionId; }
    public ProductSummary lastViewedProduct() { return lastViewedProduct; }
    public List<ProductSummary> lastMentionedProducts() { return lastMentionedProducts; }
    public String lastCategory() { return lastCategory; }
    public CartSummary currentCart() { return currentCart; }
    public String lastAction() { return lastAction; }
    public LocalDateTime lastUpdated() { return lastUpdated; }
}
