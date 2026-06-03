package com.melodyshop.ai.application.handler;

import com.melodyshop.ai.application.dto.ChatResponse;
import com.melodyshop.ai.application.dto.ChatResponse.ProductInfoDto;
import com.melodyshop.ai.domain.model.IntentType;
import com.melodyshop.ai.domain.model.ShoppingContext;
import com.melodyshop.ai.domain.service.IntentClassifier;
import com.melodyshop.ai.domain.service.IntentHandler;
import com.melodyshop.ai.infrastructure.client.ProductRestClient;
import com.melodyshop.ai.infrastructure.client.ProductRestClient.ProductListData;
import com.melodyshop.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ProductSearchHandler implements IntentHandler {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchHandler.class);

    private final ProductRestClient productClient;
    private final IntentClassifier classifier;

    public ProductSearchHandler(ProductRestClient productClient, IntentClassifier classifier) {
        this.productClient = productClient;
        this.classifier = classifier;
    }

    @Override
    public IntentType getIntentType() {
        return IntentType.PRODUCT_LIST;
    }

    @Override
    public boolean canHandle(IntentType intentType, String message, ShoppingContext context) {
        return intentType == IntentType.PRODUCT_LIST
            || intentType == IntentType.PRODUCT_SEARCH;
    }

    @Override
    public ChatResponse handle(String userId, String message, ShoppingContext context) {
        try {
            String query = classifier.extractQuery(message);
            String category = extractCategory(message, context);
            String keyword = query.isEmpty() ? category : query;

            log.info("ProductSearchHandler - query: '{}', category: '{}', keyword: '{}'", query, category, keyword);

            ApiResponse<ProductRestClient.ProductListData> response = productClient.searchProducts(
                keyword.isEmpty() ? null : keyword,
                null, null, null, null,
                0, 10, "createdAt", "desc"
            );

            log.info("ProductSearchHandler - API response success: {}, data: {}", response != null ? response.isSuccess() : "null", response != null ? response.getData() : "null");

            if (response == null || response.getData() == null) {
                log.error("ProductSearchHandler - API response or data is null");
                return ChatResponse.text("Xin loi, khong the tim san pham luc nay. Vui long thu lai sau.");
            }

            ProductRestClient.ProductListData listData = response.getData();
            List<ProductRestClient.ProductSummaryDTO> products = listData.content();
            
            log.info("ProductSearchHandler - found {} products", products != null ? products.size() : 0);

            if (products == null || products.isEmpty()) {
                return ChatResponse.text("Rất tiếc, không tìm thấy sản phẩm nào phù hợp với '" + keyword + "'. Bạn có thể thử từ khóa khác hoặc hỏi tôi về các danh mục sản phẩm của MelodyShop.");
            }

            List<ProductInfoDto> productDtos = products.stream()
                .map(this::toProductInfoDto)
                .toList();

            String introText = buildIntroText(keyword, products.size(), context);

            return ChatResponse.productList(introText, productDtos);

        } catch (Exception e) {
            log.error("ProductSearchHandler error: {}", e.getMessage(), e);
            return ChatResponse.text("Xin lỗi, đã có lỗi khi tìm kiếm sản phẩm. Bạn có thể hỏi tôi về danh sách sản phẩm hoặc danh mục của MelodyShop.");
        }
    }

    private String buildIntroText(String keyword, int count, ShoppingContext context) {
        if (keyword == null || keyword.isEmpty()) {
            return "MelodyShop có " + count + " sản phẩm nhạc cụ. Đây là một số gợi ý cho bạn:";
        }
        return "Tìm thấy " + count + " sản phẩm cho '" + keyword + "'. Bạn có thể xem chi tiết hoặc hỏi 'mua sản phẩm này' để thêm vào giỏ.";
    }

    private String extractCategory(String message, ShoppingContext context) {
        String[] categories = {"guitar", "piano", "drum", "violin", "amplifier", "microphone", "ukulele", "accessories"};
        String lower = message.toLowerCase();
        for (String cat : categories) {
            if (lower.contains(cat)) return cat;
        }
        if (context != null && context.getLastCategory() != null) {
            return context.getLastCategory();
        }
        return "";
    }

    private ProductInfoDto toProductInfoDto(ProductRestClient.ProductSummaryDTO p) {
        return new ProductInfoDto(
            p.id(), p.name(), p.slug(), p.image(),
            p.basePrice(), p.categoryName(), p.brandName(),
            p.averageRating(), p.inStock()
        );
    }
}
