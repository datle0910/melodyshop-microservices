package com.melodyshop.ai.application.handler;

import com.melodyshop.ai.application.dto.ChatResponse;
import com.melodyshop.ai.domain.model.IntentType;
import com.melodyshop.ai.domain.model.ShoppingContext;
import com.melodyshop.ai.domain.service.IntentHandler;
import com.melodyshop.ai.domain.service.ProjectKnowledgeService;
import com.melodyshop.ai.infrastructure.langchain.SimpleChatAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GeneralChatHandler implements IntentHandler {

    private static final Logger log = LoggerFactory.getLogger(GeneralChatHandler.class);

    private final SimpleChatAgent simpleChatAgent;
    private final ProjectKnowledgeService projectKnowledge;

    public GeneralChatHandler(SimpleChatAgent simpleChatAgent, ProjectKnowledgeService projectKnowledge) {
        this.simpleChatAgent = simpleChatAgent;
        this.projectKnowledge = projectKnowledge;
    }

    @Override
    public IntentType getIntentType() {
        return IntentType.GENERAL;
    }

    @Override
    public ChatResponse handle(String userId, String message, ShoppingContext context) {
        try {
            // Kiểm tra xem có phải câu hỏi về project không
            if (projectKnowledge.isProjectRelatedQuestion(message)) {
                String contextInfo = buildContextInfo(context);
                String prompt = projectKnowledge.buildProjectPrompt(message, contextInfo);
                String response = simpleChatAgent.chat(prompt, message);
                return ChatResponse.text(response);
            }

            // Fallback: general greeting
            String response = handleGreeting(message, context);
            return ChatResponse.text(response);

        } catch (Exception e) {
            log.error("GeneralChatHandler error: {}", e.getMessage(), e);
            return ChatResponse.text("Xin lỗi, tôi chưa hiểu ý bạn. Bạn có thể hỏi tôi về sản phẩm, danh mục, giá cả, chính sách giao hàng, bảo hành của MelodyShop.");
        }
    }

    private String handleGreeting(String message, ShoppingContext context) {
        String msg = message.toLowerCase().trim();

        // Chào hỏi
        if (msg.contains("xin chao") || msg.contains("xin chào") || msg.contains("chao") || 
            msg.contains("hi") || msg.contains("hello") || msg.contains("hey")) {
            StringBuilder response = new StringBuilder();
            response.append("Xin chào! 👋 Tôi là trợ lý mua sắm của MelodyShop.\n\n");
            response.append("Tôi có thể giúp bạn:\n");
            response.append("🎵 Tìm kiếm sản phẩm nhạc cụ\n");
            response.append("📋 Xem danh mục sản phẩm\n");
            response.append("🛒 Thêm sản phẩm vào giỏ hàng\n");
            response.append("📦 Tìm hiểu về giao hàng, bảo hành\n");
            response.append("💳 Thanh toán và trả góp\n\n");
            response.append("Bạn cần tôi hỗ trợ gì hôm nay?");
            return response.toString();
        }

        // Hỏi về shop
        if (msg.contains("melodyshop") || msg.contains("shop") || msg.contains("cửa hàng") || msg.contains("cua hang")) {
            return buildShopIntro();
        }

        // Hỏi về dịch vụ
        if (msg.contains("dịch vụ") || msg.contains("dich vu") || msg.contains("service")) {
            return ProjectKnowledgeService.SERVICES;
        }

        // Default response
        return buildDefaultResponse();
    }

    private String buildShopIntro() {
        StringBuilder response = new StringBuilder();
        response.append("🏪 **MelodyShop** - Cửa hàng nhạc cụ trực tuyến hàng đầu Việt Nam\n\n");
        response.append("Chúng tôi cung cấp đa dạng nhạc cụ từ các thương hiệu nổi tiếng:\n\n");
        response.append("🎸 **Guitar**: Acoustic, Electric, Classical\n");
        response.append("🎹 **Piano**: Grand, Upright, Digital\n");
        response.append("🥁 **Drum**: Acoustic, Electronic\n");
        response.append("🎻 **Violin**: Acoustic, Electric\n");
        response.append("🎤 **Microphone**: Dynamic, Condenser, Wireless\n");
        response.append("🎸 **Amplifier**: Marshall, Fender, Orange\n");
        response.append("🎸 **Bass**: Electric Bass\n");
        response.append("🎹 **Keyboard/Organ**: Arranger, Workstation\n\n");
        response.append("🌟 **Ưu đãi**: Miễn phí giao hàng, bảo hành chính hãng, trả góp 0%\n\n");
        response.append("Bạn muốn tìm hiểu thêm về sản phẩm nào?");
        return response.toString();
    }

    private String buildDefaultResponse() {
        StringBuilder response = new StringBuilder();
        response.append("Tôi là trợ lý của MelodyShop. Tôi có thể giúp bạn:\n\n");
        response.append("🔍 **Tìm sản phẩm**: \"cho xem guitar\", \"tìm piano\"\n");
        response.append("📋 **Danh mục**: \"danh mục sản phẩm\"\n");
        response.append("🛒 **Mua hàng**: \"thêm vào giỏ\", \"mua sản phẩm này\"\n");
        response.append("📦 **Giao hàng**: \"giao hàng mất bao lâu\"\n");
        response.append("🛡️ **Bảo hành**: \"chính sách bảo hành\"\n");
        response.append("💳 **Thanh toán**: \"phương thức thanh toán\"\n\n");
        response.append("Bạn cần hỗ trợ gì?");
        return response.toString();
    }

    private String buildContextInfo(ShoppingContext context) {
        if (context == null) return "";

        StringBuilder info = new StringBuilder();

        if (context.getLastCategory() != null) {
            info.append("- Danh mục đang xem: ").append(context.getLastCategory()).append("\n");
        }
        if (context.getLastViewedProduct() != null) {
            info.append("- Sản phẩm đang xem: ").append(context.getLastViewedProduct().name()).append("\n");
        }
        if (context.getLastMentionedProducts() != null && !context.getLastMentionedProducts().isEmpty()) {
            info.append("- Sản phẩm đã đề cập: ");
            context.getLastMentionedProducts().forEach(p -> info.append(p.name()).append(", "));
            info.append("\n");
        }
        if (context.getLastAction() != null) {
            info.append("- Hành động gần nhất: ").append(context.getLastAction()).append("\n");
        }

        return info.toString();
    }
}
