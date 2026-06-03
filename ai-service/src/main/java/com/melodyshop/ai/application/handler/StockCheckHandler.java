package com.melodyshop.ai.application.handler;

import com.melodyshop.ai.application.dto.ChatResponse;
import com.melodyshop.ai.application.dto.ChatResponse.StockInfoDto;
import com.melodyshop.ai.domain.model.IntentType;
import com.melodyshop.ai.domain.model.ProductSummary;
import com.melodyshop.ai.domain.model.ShoppingContext;
import com.melodyshop.ai.domain.service.IntentHandler;
import com.melodyshop.ai.infrastructure.client.InventoryRestClient;
import com.melodyshop.ai.infrastructure.client.ProductRestClient;
import com.melodyshop.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StockCheckHandler implements IntentHandler {

    private static final Logger log = LoggerFactory.getLogger(StockCheckHandler.class);

    private final InventoryRestClient inventoryClient;
    private final ProductRestClient productClient;

    public StockCheckHandler(InventoryRestClient inventoryClient, ProductRestClient productClient) {
        this.inventoryClient = inventoryClient;
        this.productClient = productClient;
    }

    @Override
    public IntentType getIntentType() {
        return IntentType.STOCK_CHECK;
    }

    @Override
    public boolean canHandle(IntentType intentType, String message, ShoppingContext context) {
        if (intentType != IntentType.STOCK_CHECK) return false;
        String msg = message.toLowerCase();
        // Has context → can always check stock
        if (hasProductContext(context)) return true;
        // Has stock keyword
        if (msg.contains("hang") || msg.contains("stock") || msg.contains("ton kho")
            || msg.contains("còn") || msg.contains("hết") || msg.contains("kho") || msg.contains("con")) return true;
        // Short questions about availability
        if (msg.length() < 20) return true;
        return false;
    }

    private boolean hasProductContext(ShoppingContext context) {
        return (context != null && context.getLastViewedProduct() != null)
            || (context != null && context.getLastMentionedProducts() != null
                && !context.getLastMentionedProducts().isEmpty());
    }

    @Override
    public ChatResponse handle(String userId, String message, ShoppingContext context) {
        try {
            // Get the product to check
            String productId = null;
            String productName = null;
            String sku = null;

            if (context != null && context.getLastViewedProduct() != null) {
                productId = context.getLastViewedProduct().id();
                productName = context.getLastViewedProduct().name();
            } else if (context != null && context.getLastMentionedProducts() != null
                       && !context.getLastMentionedProducts().isEmpty()) {
                ProductSummary last = context.getLastMentionedProducts()
                    .get(context.getLastMentionedProducts().size() - 1);
                productId = last.id();
                productName = last.name();
            }

            if (productId == null) {
                return ChatResponse.text("Bạn muốn kiểm tra tồn kho của sản phẩm nào? Vui lòng cho tôi biết tên sản phẩm trước.");
            }

            // Get product details to find SKU
            ApiResponse<ProductRestClient.ProductDetailDTO> productResp = productClient.getProductById(productId);
            if (productResp == null || productResp.getData() == null) {
                return ChatResponse.text("Không tìm thấy sản phẩm này.");
            }

            ProductRestClient.ProductDetailDTO product = productResp.getData();
            // Use first variant's SKU if available
            if (product.variants() != null && !product.variants().isEmpty()) {
                sku = product.variants().get(0).sku();
            }

            if (sku == null || sku.isEmpty()) {
                // Return stock info from product itself
                StockInfoDto stockInfo = new StockInfoDto(
                    product.inStock(),
                    product.inStock() ? 10 : 0 // Estimate if no inventory data
                );
                String msg = productName + ": " + (product.inStock() ? "Còn hàng" : "Hết hàng");
                return ChatResponse.stockCheck(msg, stockInfo);
            }

            // Call inventory service
            log.info("StockCheckHandler - checking SKU: {}", sku);
            ApiResponse<InventoryRestClient.StockCheckDTO> stockResp = inventoryClient.checkStock(sku, 1);

            if (stockResp == null || stockResp.getData() == null) {
                StockInfoDto fallback = new StockInfoDto(product.isInStock(), product.isInStock() ? 10 : 0);
                String msg = productName + ": " + (product.isInStock() ? "Còn hàng" : "Hết hàng") + " (thông tin chi tiết không khả dụng)";
                return ChatResponse.stockCheck(msg, fallback);
            }

            InventoryRestClient.StockCheckDTO stock = stockResp.getData();
            StockInfoDto stockInfo = new StockInfoDto(stock.isInStock(), stock.availableQuantity());

            String msg = productName + ": " + (stock.inStock() ? "Còn hàng - " + stock.availableQuantity() + " sản phẩm có sẵn"
                : "Hết hàng") + ". Bạn có muốn thêm vào giỏ hàng?";

            return ChatResponse.stockCheck(msg, stockInfo);

        } catch (Exception e) {
            log.error("StockCheckHandler error: {}", e.getMessage(), e);
            return ChatResponse.text("Xin lỗi, không thể kiểm tra tồn kho lúc này. Vui lòng thử lại sau.");
        }
    }
}
