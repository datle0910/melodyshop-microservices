package com.melodyshop.ai.application.handler;

import com.melodyshop.ai.application.dto.ChatResponse;
import com.melodyshop.ai.application.dto.ChatResponse.ProductDetailDto;
import com.melodyshop.ai.application.dto.ChatResponse.VariantInfoDto;
import com.melodyshop.ai.domain.model.IntentType;
import com.melodyshop.ai.domain.model.ProductSummary;
import com.melodyshop.ai.domain.model.ShoppingContext;
import com.melodyshop.ai.domain.service.IntentHandler;
import com.melodyshop.ai.infrastructure.client.ProductRestClient;
import com.melodyshop.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ProductDetailHandler implements IntentHandler {

    private static final Logger log = LoggerFactory.getLogger(ProductDetailHandler.class);

    private final ProductRestClient productClient;

    public ProductDetailHandler(ProductRestClient productClient) {
        this.productClient = productClient;
    }

    @Override
    public IntentType getIntentType() {
        return IntentType.PRODUCT_DETAIL;
    }

    @Override
    public boolean canHandle(IntentType intentType, String message, ShoppingContext context) {
        if (intentType != IntentType.PRODUCT_DETAIL) return false;
        // Can handle if:
        // 1. Has product context (last viewed or mentioned)
        // 2. Message contains slug/id
        // 3. Short message with detail keyword (likely asking about product in context)
        if (hasContext(context)) return true;
        if (extractProductSlug(message) != null || extractProductId(message) != null) return true;
        // Short detail requests like "chi tiết", "thông tin" - likely about context product
        String msg = message.toLowerCase();
        if (msg.length() < 25 && (msg.contains("chi") || msg.contains("thong") || msg.contains("tin") || msg.contains("giá") || msg.contains("price"))) {
            return true;
        }
        return false;
    }

    private boolean hasContext(ShoppingContext context) {
        return (context != null && context.getLastViewedProduct() != null)
            || (context != null && context.getLastMentionedProducts() != null && !context.getLastMentionedProducts().isEmpty());
    }

    @Override
    public ChatResponse handle(String userId, String message, ShoppingContext context) {
        try {
            String productSlug = extractProductSlug(message);
            String productId = extractProductId(message);

            ProductDetailDto productDetail = null;

            if (productSlug != null) {
                log.info("ProductDetailHandler - looking up by slug: {}", productSlug);
            ApiResponse<ProductRestClient.ProductDetailDTO> resp = productClient.getProductBySlug(productSlug);
            if (resp != null && resp.getData() != null) {
                productDetail = toProductDetailDto(resp.getData());
            }
        } else if (productId != null) {
            log.info("ProductDetailHandler - looking up by id: {}", productId);
            ApiResponse<ProductRestClient.ProductDetailDTO> resp = productClient.getProductById(productId);
                if (resp != null && resp.getData() != null) {
                    productDetail = toProductDetailDto(resp.getData());
                }
            } else if (context != null && context.getLastViewedProduct() != null) {
                // Fall back to last viewed product
                String lastId = context.getLastViewedProduct().id();
                log.info("ProductDetailHandler - using last viewed product: {}", lastId);
                ApiResponse<ProductRestClient.ProductDetailDTO> resp = productClient.getProductById(lastId);
                if (resp != null && resp.getData() != null) {
                    productDetail = toProductDetailDto(resp.getData());
                }
            } else if (context != null && context.getLastMentionedProducts() != null
                       && !context.getLastMentionedProducts().isEmpty()) {
                // Use most recent mentioned product
                ProductSummary last = context.getLastMentionedProducts()
                    .get(context.getLastMentionedProducts().size() - 1);
                String lastId = last.id();
                log.info("ProductDetailHandler - using last mentioned product: {}", lastId);
                ApiResponse<ProductRestClient.ProductDetailDTO> resp = productClient.getProductById(lastId);
                if (resp != null && resp.getData() != null) {
                    productDetail = toProductDetailDto(resp.getData());
                }
            }

            if (productDetail == null) {
                return ChatResponse.text("Tôi không tìm thấy sản phẩm bạn yêu cầu. Vui lòng cho tôi biết tên sản phẩm cụ thể, hoặc hỏi tôi về danh sách sản phẩm trước.");
            }

            String responseText = buildDetailIntro(productDetail);

            return ChatResponse.productDetail(responseText, productDetail);

        } catch (Exception e) {
            log.error("ProductDetailHandler error: {}", e.getMessage(), e);
            return ChatResponse.text("Xin lỗi, đã có lỗi khi lấy chi tiết sản phẩm. Vui lòng thử lại sau.");
        }
    }

    private String buildDetailIntro(ProductDetailDto p) {
        StringBuilder sb = new StringBuilder();
        sb.append("**").append(p.getName()).append("** - ").append(formatPrice(p.getBasePrice())).append("\n\n");
        sb.append("📦 ").append(p.isInStock() ? "Còn hàng" : "Hết hàng");
        if (p.getRating() != null && p.getRating() > 0) {
            sb.append(" | ⭐ ").append(String.format("%.1f", p.getRating()));
            if (p.getReviewCount() != null) {
                sb.append(" (").append(p.getReviewCount()).append(" đánh giá)");
            }
        }
        sb.append("\n");
        sb.append("🏷️ ").append(p.getBrand()).append(" | ").append(p.getCategory()).append("\n\n");
        if (p.getVariants() != null && !p.getVariants().isEmpty()) {
            sb.append("📋 **Các phiên bản:**\n");
            for (VariantInfoDto v : p.getVariants()) {
                String stock = v.isInStock() ? "Còn hàng" : "Hết hàng";
                sb.append("- ").append(v.getName()).append(": ").append(formatPrice(v.getPrice())).append(" (").append(stock).append(")\n");
            }
            sb.append("\nBạn có thể nói 'mua sản phẩm này' hoặc 'thêm vào giỏ' để đặt hàng ngay!");
        } else {
            sb.append("\nBạn có thể nói 'mua sản phẩm này' hoặc 'thêm vào giỏ' để đặt hàng ngay!");
        }
        return sb.toString();
    }

    private String formatPrice(Double price) {
        if (price == null) return "Liên hệ";
        return String.format("%,.0f đ", price);
    }

    private String extractProductSlug(String message) {
        // Match slug-like patterns: guitar-yamaha-f310, etc.
        Matcher m = Pattern.compile("([a-z0-9]+(?:-[a-z0-9]+)+)").matcher(message.toLowerCase());
        // Return the longest match that looks like a product slug
        String longest = null;
        while (m.find()) {
            String match = m.group(1);
            if (longest == null || match.length() > longest.length()) {
                longest = match;
            }
        }
        return longest;
    }

    private String extractProductId(String message) {
        Matcher m = Pattern.compile("[a-f0-9]{24,}").matcher(message.toLowerCase());
        return m.find() ? m.group() : null;
    }

    private ProductDetailDto toProductDetailDto(ProductRestClient.ProductDetailDTO d) {
        List<VariantInfoDto> variants = null;
        if (d.variants() != null) {
            variants = d.variants().stream()
                .map(v -> new VariantInfoDto(
                    v.id(), v.sku(), v.variantName(),
                    v.price(), v.stockQuantity(), v.inStock()))
                .collect(Collectors.toList());
        }
        return new ProductDetailDto(
            d.id(), d.name(), d.slug(), d.description(),
            d.basePrice(), d.categoryName(), d.brandName(),
            d.averageRating(), d.reviewCount(), d.inStock(),
            variants
        );
    }
}
