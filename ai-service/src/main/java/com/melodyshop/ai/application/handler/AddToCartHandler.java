package com.melodyshop.ai.application.handler;

import com.melodyshop.ai.application.dto.ChatResponse;
import com.melodyshop.ai.application.dto.ChatResponse.CartInfoDto;
import com.melodyshop.ai.domain.model.IntentType;
import com.melodyshop.ai.domain.model.ProductSummary;
import com.melodyshop.ai.domain.model.ShoppingContext;
import com.melodyshop.ai.domain.service.IntentHandler;
import com.melodyshop.ai.infrastructure.client.CartRestClient;
import com.melodyshop.ai.infrastructure.client.ProductRestClient;
import com.melodyshop.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AddToCartHandler implements IntentHandler {

    private static final Logger log = LoggerFactory.getLogger(AddToCartHandler.class);

    private final CartRestClient cartClient;
    private final ProductRestClient productClient;

    public AddToCartHandler(CartRestClient cartClient, ProductRestClient productClient) {
        this.cartClient = cartClient;
        this.productClient = productClient;
    }

    @Override
    public IntentType getIntentType() {
        return IntentType.ADD_TO_CART;
    }

    @Override
    public boolean canHandle(IntentType intentType, String message, ShoppingContext context) {
        return intentType == IntentType.ADD_TO_CART;
    }

    private boolean hasProductContext(ShoppingContext context) {
        return (context != null && context.getLastViewedProduct() != null)
            || (context != null && context.getLastMentionedProducts() != null
                && !context.getLastMentionedProducts().isEmpty());
    }

    @Override
    public ChatResponse handle(String userId, String message, ShoppingContext context) {
        try {
            // For anonymous users, we can't add to cart
            if (userId == null || userId.trim().isEmpty() || "anonymous".equals(userId)) {
                return ChatResponse.text("Bạn cần đăng nhập để thêm sản phẩm vào giỏ hàng. Vui lòng đăng nhập trước, sau đó tôi sẽ giúp bạn thêm sản phẩm vào giỏ hàng.");
            }

            String productId = null;
            String productName = null;
            Double unitPrice = null;
            String variantId = null;
            String variantName = null;
            String sku = null;
            int quantity = extractQuantity(message);

            // Strategy 1: Get from context (last viewed product)
            if (context != null && context.getLastViewedProduct() != null) {
                ProductSummary p = context.getLastViewedProduct();
                productId = p.id();
                productName = p.name();
                unitPrice = p.price();
                log.info("AddToCartHandler - using lastViewedProduct: id={}, name={}", productId, productName);
            }
            // Strategy 2: Get from recently mentioned products
            else if (context != null && context.getLastMentionedProducts() != null
                       && !context.getLastMentionedProducts().isEmpty()) {
                ProductSummary p = context.getLastMentionedProducts()
                    .get(context.getLastMentionedProducts().size() - 1);
                productId = p.id();
                productName = p.name();
                unitPrice = p.price();
                log.info("AddToCartHandler - using lastMentionedProduct: id={}, name={}", productId, productName);
            }
            // Strategy 3: Try to extract product name from message and search
            else if (message != null) {
                String extractedName = extractProductNameFromMessage(message);
                if (extractedName != null && !extractedName.isEmpty()) {
                    log.info("AddToCartHandler - searching for product by name: {}", extractedName);
                    ProductSummary found = searchProductByName(extractedName);
                    if (found != null) {
                        productId = found.id();
                        productName = found.name();
                        unitPrice = found.price();
                        log.info("AddToCartHandler - found product by name: id={}, name={}", productId, productName);
                    }
                }
            }

            if (productId == null) {
                return ChatResponse.text("Bạn muốn thêm sản phẩm nào vào giỏ? Vui lòng cho tôi biết tên sản phẩm hoặc hỏi về danh sách sản phẩm trước.");
            }

            // Get full product details to find variant and stock
            ApiResponse<ProductRestClient.ProductDetailDTO> productResp = productClient.getProductById(productId);
            if (productResp == null || productResp.getData() == null) {
                return ChatResponse.text("Không tìm thấy sản phẩm này.");
            }

            ProductRestClient.ProductDetailDTO product = productResp.getData();
            productName = product.name();

            // Use first available variant if any
            if (product.variants() != null && !product.variants().isEmpty()) {
                ProductRestClient.VariantDTO firstVariant = product.variants().get(0);
                variantId = firstVariant.id();
                variantName = firstVariant.variantName();
                sku = firstVariant.sku();
                unitPrice = firstVariant.price();

                // Check stock
                if (!firstVariant.isInStock() || firstVariant.stockQuantity() == null || firstVariant.stockQuantity() <= 0) {
                    return ChatResponse.text("Rất tiếc, sản phẩm '" + productName + "' (" + variantName + ") hiện đang hết hàng. Bạn có thể thử sản phẩm khác hoặc để lại thông tin, chúng tôi sẽ thông báo khi có hàng.");
                }
            } else {
                // Check overall stock
                if (!product.isInStock()) {
                    return ChatResponse.text("Rất tiếc, sản phẩm '" + productName + "' hiện đang hết hàng. Bạn có thể thử sản phẩm khác hoặc để lại thông tin, chúng tôi sẽ thông báo khi có hàng.");
                }
            }

            // Call cart service
            log.info("AddToCartHandler - adding product: id={}, variant={}, quantity={}, price={}", productId, variantId, quantity, unitPrice);

            CartRestClient.AddToCartRequest request = new CartRestClient.AddToCartRequest(
                userId, productId, productName, null,
                variantId, variantName, sku,
                unitPrice, quantity
            );

            ApiResponse<CartRestClient.CartItemDTO> cartResp = cartClient.addToCart(request);

            if (cartResp == null || cartResp.getData() == null) {
                return ChatResponse.text("Đã xảy ra lỗi khi thêm vào giỏ hàng. Vui lòng thử lại.");
            }

            CartRestClient.CartItemDTO item = cartResp.getData();
            CartInfoDto cartInfo = new CartInfoDto(
                item.id(), item.productId(), item.productName(),
                item.quantity(), item.subtotal()
            );

            String msg = "Đã thêm '" + item.productName() + "' (x" + item.quantity() + ") vào giỏ hàng với giá " + formatPrice(item.subtotal()) + "! Bạn có thể tiếp tục mua sắm hoặc thanh toán ngay.";

            return ChatResponse.cartAdded(msg, cartInfo);

        } catch (Exception e) {
            log.error("AddToCartHandler error: {}", e.getMessage(), e);
            return ChatResponse.text("Xin lỗi, đã có lỗi khi thêm vào giỏ hàng. Vui lòng thử lại sau.");
        }
    }

    /**
     * Extracts product name from buy message like "mua Fender", "thêm Roland", "lấy Yamaha"
     */
    private String extractProductNameFromMessage(String message) {
        if (message == null || message.trim().isEmpty()) return null;
        String lower = message.toLowerCase();

        // Pattern: "mua [name]", "thêm [name]", "lay [name]", "dat [name]", "cho vao gio [name]"
        String[] prefixes = {"mua ", "them ", "lay ", "đặt ", "dat ", "cho vao gio ", "cho vào giỏ ", "thêm vào giỏ "};
        for (String prefix : prefixes) {
            int idx = lower.indexOf(prefix);
            if (idx >= 0) {
                String after = message.substring(idx + prefix.length()).trim();
                // Clean up trailing punctuation and pronouns
                String cleaned = after
                    .replaceAll("(?iu)^(di|đi|nay|đó|nào|vào gio|vao gio|)\\s*", "")
                    .replaceAll("[,.!?!;:]+$", "")
                    .trim();
                if (!cleaned.isEmpty() && cleaned.length() > 1) {
                    return cleaned;
                }
            }
        }
        return null;
    }

    /**
     * Searches for a product by partial name match.
     */
    private ProductSummary searchProductByName(String name) {
        try {
            if (name == null || name.trim().isEmpty()) return null;
            log.info("Searching for product with name: {}", name);
            ApiResponse<ProductRestClient.ProductListData> resp = productClient.searchProducts(
                name.trim(), null, null, null, null, 0, 5, "createdAt", "desc"
            );
            if (resp != null && resp.getData() != null && resp.getData().content() != null
                && !resp.getData().content().isEmpty()) {
                ProductRestClient.ProductSummaryDTO p = resp.getData().content().get(0);
                return new ProductSummary(p.id(), p.name(), p.basePrice(), p.isInStock());
            }
        } catch (Exception e) {
            log.warn("Error searching product by name: {}", e.getMessage());
        }
        return null;
    }

    private int extractQuantity(String message) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(message);
        if (m.find()) {
            return Math.min(Integer.parseInt(m.group()), 99);
        }
        return 1;
    }

    private String formatPrice(Double price) {
        if (price == null) return "Liên hệ";
        return String.format("%,.0f đ", price);
    }
}
