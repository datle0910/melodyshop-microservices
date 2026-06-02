package com.melodyshop.ai.domain.service;

import com.melodyshop.ai.domain.model.IntentType;
import com.melodyshop.ai.domain.model.ShoppingContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class IntentClassifier {

    // ─────────────────────────────────────────────────────────────────────────────
    // SMART BUY PATTERNS - unique entries only
    // ─────────────────────────────────────────────────────────────────────────────
    private static final Set<String> BUY_PATTERNS = new HashSet<>(Arrays.asList(
        // Basic
        "mua", "mua ngay",
        // With product reference
        "mua san pham", "mua san pham nay", "mua sp",
        // Actions
        "them vao gio", "them gio", "them vao gio hang",
        "them san pham", "them sp", "them sp vao gio",
        "them san pham vao gio",
        "cho vao gio", "cho vao gio hang",
        // Want to buy
        "toi muon mua", "toi muon", " muon mua",
        "dang muon mua",
        // Order
        "dat hang", "dat don", "dat mua",
        // Buy this
        "mua no", "mua sp nay", "mua sp do", "mua sp này",
        // English
        "add to cart", "add cart", "buy now", "buy this", "buy it"
    ));

    // STOCK patterns
    private static final Set<String> STOCK_PATTERNS = new HashSet<>(Arrays.asList(
        "con hang", "con khong", "con ko",
        "het hang", "ton kho", "kho ton",
        "in stock", "stock", "available",
        "so luong", "bao nhieu", "co bao nhieu"
    ));

    // DETAIL patterns - use with word boundaries to avoid false matches
    private static final Set<String> DETAIL_PATTERNS = new HashSet<>(Arrays.asList(
        "chi tiet", "thong so", "thong so ky thuat",
        "dac diem", "mo ta",
        "xem them", "xem chi tiet",
        "detail", "info", "gia", "price"
    ));

    // CATEGORY patterns
    private static final Set<String> CATEGORY_PATTERNS = new HashSet<>(Arrays.asList(
        "danh muc", "danh muc san pham",
        "phan loai", "loai san pham",
        "category", "categories",
        "co ban nhac gi", "ban nhac gi"
    ));

    // LIST patterns
    private static final Set<String> LIST_PATTERNS = new HashSet<>(Arrays.asList(
        "danh sach san pham", "danh sach", "danh sach sp",
        "co san pham gi", "san pham nao", "san pham gi",
        "xem san pham", "tat ca san pham",
        "list product", "list products", "show products", "all products",
        "cac san pham", "co nhieu san pham",
        "cho xem", "cho toi xem", "xem gi"
    ));

    // THIS PRODUCT patterns
    private static final Set<String> THIS_PRODUCT_PATTERNS = new HashSet<>(Arrays.asList(
        "san pham nay", "san pham do", "san pham này", "san phẩm đó",
        "sp nay", "sp do", "sp này",
        "chi tiet san pham nay", "thong tin san pham nay",
        "sản phẩm vừa xem", "vừa xem",
        "cái này", "cái đó"
    ));

    // GREETING patterns
    private static final Set<String> GREETING_PATTERNS = new HashSet<>(Arrays.asList(
        "xin chao", "xin chào", "chao", "chào", "hi", "hello", "hey",
        "good morning", "good afternoon", "good evening",
        "chao ban", "chào bạn"
    ));

    // ─────────────────────────────────────────────────────────────────────────────
    // REGEX PATTERNS
    // ─────────────────────────────────────────────────────────────────────────────
    private static final Pattern CHO_XEM = Pattern.compile(
        "(?iu).*(cho\\s*(xem|toi\\s*xem|t\\s*xem|ban\\s*xem)|show\\s*me)\\s+([\\p{L}\\s\\d]+)",
        Pattern.UNICODE_CHARACTER_CLASS
    );
    private static final Pattern TIM_KIEM = Pattern.compile(
        "(?iu)^\\s*(tim|kiem|search|find)\\s+(.+)",
        Pattern.UNICODE_CHARACTER_CLASS
    );
    private static final Pattern PRODUCT_NAME = Pattern.compile(
        "(?iu)(fender|yamaha|gibson|steinway|roland|taylor|martin|ibanez|epiphone)",
        Pattern.UNICODE_CHARACTER_CLASS
    );
    private static final Pattern CATEGORY_NAO = Pattern.compile(
        "(?iu)(guitar|piano|drum|violin|amplifier|microphone|ukulele|accessories|bass|organ)\\s+(nao|nào|gi|gì|hay|ko|hêm?|)\\s*$",
        Pattern.UNICODE_CHARACTER_CLASS
    );

    // ─────────────────────────────────────────────────────────────────────────────
    // MAIN CLASSIFICATION
    // ─────────────────────────────────────────────────────────────────────────────
    public IntentType classify(String message, ShoppingContext context) {
        if (message == null || message.trim().isEmpty()) {
            return IntentType.UNKNOWN;
        }

        String msg = message.trim().toLowerCase();
        String norm = stripVietnameseAccents(msg);

        boolean hasContext = context != null && hasProductContext(context);
        boolean hasBuy = containsAny(msg, BUY_PATTERNS) || containsAny(norm, BUY_PATTERNS);
        boolean hasPronoun = hasPronoun(msg, norm);
        boolean hasBrand = PRODUCT_NAME.matcher(message).find();

        // 1. Context + buy → ADD_TO_CART
        if (hasContext && hasBuy) {
            return IntentType.ADD_TO_CART;
        }

        // 2. Brand + buy → ADD_TO_CART
        if (hasBrand && hasBuy) {
            return IntentType.ADD_TO_CART;
        }

        // 3. Short buy with pronoun → ADD_TO_CART
        if (msg.length() < 20 && hasBuy && hasPronoun) {
            return IntentType.ADD_TO_CART;
        }

        // 4. Stock check
        if (containsAny(msg, STOCK_PATTERNS) || containsAny(norm, STOCK_PATTERNS)) {
            return IntentType.STOCK_CHECK;
        }

        // 5. Detail
        if (containsAny(msg, DETAIL_PATTERNS) || containsAny(norm, DETAIL_PATTERNS)) {
            return IntentType.PRODUCT_DETAIL;
        }

        // 6. Category list
        if (containsAny(msg, CATEGORY_PATTERNS) || containsAny(norm, CATEGORY_PATTERNS)) {
            return IntentType.CATEGORY_LIST;
        }

        // 7. Product list
        if (containsAny(msg, LIST_PATTERNS) || containsAny(norm, LIST_PATTERNS)) {
            return IntentType.PRODUCT_LIST;
        }

        // 8. "cho xem X"
        if (CHO_XEM.matcher(message).find()) {
            return IntentType.PRODUCT_LIST;
        }

        // 9. "tim X"
        if (TIM_KIEM.matcher(message).find()) {
            return IntentType.PRODUCT_SEARCH;
        }

        // 10. "guitar nào"
        if (CATEGORY_NAO.matcher(message).find()) {
            return IntentType.PRODUCT_LIST;
        }

        // 11. Context + this product reference
        if (hasContext) {
            if (matchesAny(msg, THIS_PRODUCT_PATTERNS) || matchesAny(norm, THIS_PRODUCT_PATTERNS)) {
                if (hasBuy) return IntentType.ADD_TO_CART;
                if (containsAny(msg, STOCK_PATTERNS)) return IntentType.STOCK_CHECK;
                return IntentType.PRODUCT_DETAIL;
            }
            // Just pronoun, short → likely detail
            if (hasPronoun && msg.length() < 15) {
                return IntentType.PRODUCT_DETAIL;
            }
        }

        // 12. Single category name
        String[] cats = {"guitar", "piano", "drum", "violin", "amplifier", "microphone", "ukulele", "accessories", "bass", "organ"};
        for (String cat : cats) {
            if (msg.equals(cat.trim()) || norm.equals(cat.trim())) {
                return IntentType.PRODUCT_SEARCH;
            }
        }

        // 13. Brand name alone
        if (hasBrand && msg.length() < 50) {
            return IntentType.PRODUCT_SEARCH;
        }

        // 14. Greetings
        if (matchesAny(msg, GREETING_PATTERNS) || matchesAny(norm, GREETING_PATTERNS)) {
            return IntentType.GENERAL;
        }

        // 15. Project-related questions → GENERAL (để dùng knowledge base)
        if (isProjectRelatedQuestion(msg)) {
            return IntentType.GENERAL;
        }

        return IntentType.UNKNOWN;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PROJECT SCOPE CHECK
    // ─────────────────────────────────────────────────────────────────────────────
    private static final Set<String> PROJECT_KEYWORDS = new HashSet<>(Arrays.asList(
        // Shop
        "melodyshop", "shop", "cua hang", "cửa hàng", "ban hang", "bán hàng", "mua sam", "mua sắm",
        // Services
        "giao hang", "giao hàng", "van chuyen", "vận chuyển", "thanh toan", "thanh toán",
        "tra gop", "trả góp", "bao hanh", "bảo hành", "doi tra", "đổi trả",
        "tu van", "tư vấn", "ho tro", "hỗ trợ", "chinh sach", "chính sách",
        // Products
        "san pham", "sản phẩm", "nhac cu", "nhạc cụ", "danh muc", "danh mục",
        "thuong hieu", "thương hiệu", "loai", "loại",
        // Questions
        "la gi", "là gì", "the nao", "thế nào", "như thế nào", "ra sao", "lam sao",
        "co gi", "có gì", "khi nao", "khi nào", "o dau", "ở đâu",
        "bao nhieu", "bao nhiêu", "gian", "giá", "gia"
    ));

    private static final Set<String> QUESTION_STARTERS = new HashSet<>(Arrays.asList(
        "cho hoi", "cho tôi hỏi", "hỏi", "tôi muốn biết", "bạn có", "bạn là", "bạn là gì",
        "melodyshop la", "melodyshop là", "shop nay", "shop này", "cua hang nay", "cửa hàng này",
        "thong tin", "thông tin", "gioi thieu", "giới thiệu", "nói về", "nói cho tôi"
    ));

    private boolean isProjectRelatedQuestion(String msg) {
        // Check question starters
        for (String starter : QUESTION_STARTERS) {
            if (msg.contains(starter)) {
                return true;
            }
        }
        // Check project keywords
        for (String keyword : PROJECT_KEYWORDS) {
            if (msg.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────────
    public String extractQuery(String message) {
        if (message == null || message.trim().isEmpty()) return "";
        String original = message.trim();

        var choXemMatcher = CHO_XEM.matcher(original);
        if (choXemMatcher.find()) {
            String found = choXemMatcher.group(choXemMatcher.groupCount()).trim();
            if (!found.isEmpty()) return found;
        }

        var timKiemMatcher = TIM_KIEM.matcher(original);
        if (timKiemMatcher.find()) {
            String found = timKiemMatcher.group(timKiemMatcher.groupCount()).trim();
            if (!found.isEmpty()) return found;
        }

        String cleaned = original
            .replaceAll("(?iu)^(tim|kiem|search|find)\\s+", "")
            .replaceAll("(?iu)^(cho\\s*xem|cho\\s*toi\\s*xem|show\\s*me|xem)\\s+", "")
            .replaceAll("(?iu)\\s+(nao|nào|gi|gì|hay|ko)\\s*$", "")
            .replaceAll("(?iu)\\s+con\\s*(khong|ko|hang)?\\s*$", "")
            .replaceAll("(?iu)^(cac|các)\\s+", "")
            .trim();

        String[] cats = {"guitar", "piano", "drum", "violin", "amplifier", "microphone", "ukulele", "accessories", "bass", "organ"};
        for (String cat : cats) {
            if (cleaned.equalsIgnoreCase(cat)) return cat;
        }
        return cleaned.isEmpty() ? original : cleaned;
    }

    private boolean hasProductContext(ShoppingContext context) {
        return context != null && (
            context.getLastViewedProduct() != null ||
            (context.getLastMentionedProducts() != null && !context.getLastMentionedProducts().isEmpty())
        );
    }

    private boolean hasPronoun(String msg, String norm) {
        String[] pronouns = {"no", "nó", "đó", "này", "nái", "cái này", "cái đó", "it", "this", "đi"};
        for (String p : pronouns) {
            if (msg.contains(p) || norm.contains(p)) return true;
        }
        return false;
    }

    private boolean containsAny(String text, Set<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private boolean matchesAny(String text, Set<String> keywords) {
        for (String keyword : keywords) {
            String pattern = "(?iu)\\b" + Pattern.quote(keyword) + "\\b";
            if (Pattern.compile(pattern).matcher(text).find()) return true;
        }
        return false;
    }

    private String stripVietnameseAccents(String text) {
        if (text == null) return "";
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }
}
