package com.melodyshop.ai.application.usecase;

import com.melodyshop.ai.application.dto.ChatResponse;
import com.melodyshop.ai.domain.model.ShoppingContext;
import com.melodyshop.ai.domain.repository.ContextStore;
import com.melodyshop.ai.infrastructure.langchain.MelodyShopAiAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatInteractionUseCase {

    private final MelodyShopAiAgent aiAgent;
    private final ContextStore contextStore;

    public ChatResponse processUserMessage(String userId, String message) {
        // 1. Fetch Context from Redis
        ShoppingContext context = contextStore.getContext(userId);

        // 2. Build System Prompt dynamically
        String systemPrompt = buildSystemMessage(context);

        // 3. Call AI Agent (LangChain4j handles memory and tool calling)
        String responseText = aiAgent.chat(userId, systemPrompt, message);

        // 4. Return response
        return new ChatResponse(responseText);
    }

    private String buildSystemMessage(ShoppingContext context) {
        // This is a simplified dynamic prompt. In production, consider using a template engine or LangChain4j ChatMemory parameters
        String lastViewed = context != null && context.lastViewedProduct() != null 
                            ? context.lastViewedProduct().name() 
                            : "Không có";
        
        return "Bạn là MelodyShop AI Assistant, trợ lý mua sắm thông minh của website bán nhạc cụ trực tuyến MelodyShop.\n" +
               "Luôn trả lời thân thiện, ngắn gọn, chính xác và hướng khách hàng đến việc mua hàng.\n\n" +
               "[NGỮ CẢNH HIỆN TẠI]\n" +
               "- Sản phẩm xem gần nhất (lastViewedProduct): " + lastViewed + "\n" +
               "[HƯỚNG DẪN XỬ LÝ NGỮ CẢNH]\n" +
               "Khi người dùng dùng các từ tham chiếu như \"sản phẩm đó\", \"nó\", \"cái đó\", \"thêm 2 cái\":\n" +
               "1. Ưu tiên map với [lastViewedProduct].\n" +
               "2. Nếu không có lastViewedProduct, ưu tiên [lastMentionedProducts].\n" +
               "3. Nếu không đủ thông tin, HÃY HỎI LẠI khách hàng.\n" +
               "KHÔNG BAO GIỜ TỰ ĐOÁN ID SẢN PHẨM HOẶC BỊA THÔNG TIN. MỌI DỮ LIỆU PHẢI LẤY TỪ TOOLS.";
    }
}
