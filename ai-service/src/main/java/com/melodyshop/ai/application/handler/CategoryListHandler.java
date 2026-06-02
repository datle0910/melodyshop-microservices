package com.melodyshop.ai.application.handler;

import com.melodyshop.ai.application.dto.ChatResponse;
import com.melodyshop.ai.application.dto.ChatResponse.CategoryInfoDto;
import com.melodyshop.ai.domain.model.IntentType;
import com.melodyshop.ai.domain.model.ShoppingContext;
import com.melodyshop.ai.domain.service.IntentHandler;
import com.melodyshop.ai.infrastructure.client.CategoryClient;
import com.melodyshop.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CategoryListHandler implements IntentHandler {

    private static final Logger log = LoggerFactory.getLogger(CategoryListHandler.class);

    private final CategoryClient categoryClient;

    public CategoryListHandler(CategoryClient categoryClient) {
        this.categoryClient = categoryClient;
    }

    @Override
    public IntentType getIntentType() {
        return IntentType.CATEGORY_LIST;
    }

    @Override
    public ChatResponse handle(String userId, String message, ShoppingContext context) {
        try {
            ApiResponse<List<CategoryClient.CategoryDTO>> response = categoryClient.getCategories();

            if (response == null || response.getData() == null) {
                return ChatResponse.text("Xin lỗi, không thể tải danh sách danh mục lúc này. MelodyShop có các danh mục chính: Guitar, Piano, Drum, Violin, Amplifier, Microphone, Ukulele và Phụ kiện.");
            }

            List<CategoryClient.CategoryDTO> categories = response.getData();
            List<CategoryInfoDto> categoryDtos = flattenCategories(categories);

            String intro = "MelodyShop có " + categoryDtos.size() + " danh mục sản phẩm. Bạn có thể hỏi tôi về danh sách sản phẩm trong từng danh mục, ví dụ: 'cho xem guitar' hoặc 'piano nào có giá tốt nhất'.";

            return ChatResponse.categoryList(intro, categoryDtos);

        } catch (Exception e) {
            log.error("CategoryListHandler error: {}", e.getMessage(), e);
            return ChatResponse.text("MelodyShop có các danh mục chính: Guitar, Piano, Drum, Violin, Amplifier, Microphone, Ukulele và Phụ kiện nhạc cụ. Bạn muốn xem sản phẩm trong danh mục nào?");
        }
    }

    private List<CategoryInfoDto> flattenCategories(List<CategoryClient.CategoryDTO> categories) {
        List<CategoryInfoDto> result = new ArrayList<>();
        for (CategoryClient.CategoryDTO cat : categories) {
            result.add(new CategoryInfoDto(cat.id(), cat.name(), cat.slug(), cat.imageUrl()));
            if (cat.children() != null && !cat.children().isEmpty()) {
                result.addAll(flattenCategories(cat.children()));
            }
        }
        return result;
    }
}
