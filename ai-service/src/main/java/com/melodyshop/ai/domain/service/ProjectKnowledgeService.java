package com.melodyshop.ai.domain.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ProjectKnowledgeService - Chứa toàn bộ kiến thức về MelodyShop
 * giúp chatbot trả lời các câu hỏi trong phạm vi project
 */
@Service
public class ProjectKnowledgeService {

    // ─────────────────────────────────────────────────────────────────────────────
    // THÔNG TIN CƠ BẢN VỀ MELODYSHOP
    // ─────────────────────────────────────────────────────────────────────────────
    public static final String SHOP_NAME = "MelodyShop";
    public static final String SHOP_DESCRIPTION = "Cửa hàng nhạc cụ trực tuyến hàng đầu Việt Nam";
    public static final String SHOP_TAGLINE = "Âm nhạc trong tầm tay";

    // ─────────────────────────────────────────────────────────────────────────────
    // CÁC DANH MỤC SẢN PHẨM
    // ─────────────────────────────────────────────────────────────────────────────
    public static final Map<String, CategoryInfo> CATEGORIES = new LinkedHashMap<>();

    static {
        // Guitar
        CATEGORIES.put("guitar", new CategoryInfo(
            "guitar",
            "Guitar",
            Arrays.asList("acoustic guitar", "electric guitar", "classical guitar", "bass guitar"),
            "Guitar là nhạc cụ phổ biến nhất, có 3 loại chính: acoustic (dây steel), classical (dây nylon), và electric. Các thương hiệu nổi tiếng: Fender, Gibson, Yamaha, Taylor, Martin, Ibanez, Epiphone.",
            Arrays.asList("Fender", "Gibson", "Yamaha", "Taylor", "Martin", "Ibanez", "Epiphone")
        ));

        // Piano
        CATEGORIES.put("piano", new CategoryInfo(
            "piano",
            "Piano / Đàn Piano",
            Arrays.asList("grand piano", "upright piano", "digital piano", "keyboard"),
            "Piano có nhiều loại: grand piano (đại phong cầm), upright piano (dương cầm đứng), digital piano (điện tử). Giá từ 15 triệu (digital) đến hàng tỷ đồng (grand piano). Thương hiệu: Steinway, Yamaha, Roland, Kawai.",
            Arrays.asList("Steinway", "Yamaha", "Roland", "Kawai", "Casio")
        ));

        // Drum
        CATEGORIES.put("drum", new CategoryInfo(
            "drum",
            "Drum / Trống",
            Arrays.asList("acoustic drum", "electronic drum", "cymbals", "drum kit"),
            "Drum có 2 loại chính: acoustic drum (trống acoustic) và electronic drum (trống điện tử). Phù hợp cho biểu diễn live hoặc ghi âm trong phòng studio. Thương hiệu: Roland, Yamaha, Pearl, DW, Tama.",
            Arrays.asList("Roland", "Yamaha", "Pearl", "DW", "Tama")
        ));

        // Violin
        CATEGORIES.put("violin", new CategoryInfo(
            "violin",
            "Violin / Vĩ cầm",
            Arrays.asList("violin acoustic", "violin electric", "viola", "cello"),
            "Violin là nhạc cụ dây cổ điển, phù hợp cho nhạc cổ điển và jazz. Có các loại: violin 4/4 (người lớn), 3/4, 1/2 (trẻ em). Thương hiệu: Yamaha, Stentor, Cremona.",
            Arrays.asList("Yamaha", "Stentor", "Cremona", "Antonio Stradivari")
        ));

        // Amplifier
        CATEGORIES.put("amplifier", new CategoryInfo(
            "amplifier",
            "Amplifier / Amply",
            Arrays.asList("guitar amp", "bass amp", "keyboard amp", "mixer"),
            "Amplifier (amply) dùng để khuếch đại âm thanh cho guitar, bass, keyboard. Có các loại: tube amp (ống), solid state amp, modeling amp. Thương hiệu: Marshall, Fender, Orange, Boss, Vox.",
            Arrays.asList("Marshall", "Fender", "Orange", "Boss", "Vox", "Blackstar")
        ));

        // Microphone
        CATEGORIES.put("microphone", new CategoryInfo(
            "microphone",
            "Microphone / Micro",
            Arrays.asList("dynamic mic", "condenser mic", "wireless mic", "usb mic"),
            "Microphone có nhiều loại: dynamic (trường, sân khấu), condenser (studio, thu âm), wireless (không dây), USB (podcast). Thương hiệu: Shure, Sennheiser, Audio-Technica, Rode.",
            Arrays.asList("Shure", "Sennheiser", "Audio-Technica", "Rode", "Blue")
        ));

        // Ukulele
        CATEGORIES.put("ukulele", new CategoryInfo(
            "ukulele",
            "Ukulele",
            Arrays.asList("soprano ukulele", "concert ukulele", "tenor ukulele", "baritone ukulele"),
            "Ukulele là nhạc cụ dây nhỏ gọn, dễ học, phù hợp cho người mới bắt đầu. Có 4 loại kích thước: soprano (nhỏ nhất), concert, tenor, baritone. Thương hiệu: Kala, Yamaha, Lanikai, Cordoba.",
            Arrays.asList("Kala", "Yamaha", "Lanikai", "Cordoba", "Ohana")
        ));

        // Accessories
        CATEGORIES.put("accessories", new CategoryInfo(
            "accessories",
            "Phụ kiện",
            Arrays.asList("pick", "dây đàn", "strap", "capo", "tuner", "cáp", "hộp đàn", "ghế đàn"),
            "Phụ kiện nhạc cụ bao gồm: pick (miếng gảy), dây đàn, strap (dây đeo), capo (kẹp cổ đàn), tuner (bộ lên dây), cáp kết nối, hộp đàn, ghế đàn. Phụ kiện chính hãng giúp bảo vệ và tăng trải nghiệm chơi nhạc.",
            Arrays.asList("D'Addario", "Elixir", "Fender", "Dunlop", "Kyser")
        ));

        // Bass
        CATEGORIES.put("bass", new CategoryInfo(
            "bass",
            "Bass / Đàn Bass",
            Arrays.asList("electric bass", "acoustic bass", "bass guitar"),
            "Bass là nhạc cụ dây quan trọng trong band, tạo nền âm trầm. Electric bass phổ biến nhất trong nhạc pop, rock, jazz. Thương hiệu: Fender, Ibanez, Music Man, Warwick.",
            Arrays.asList("Fender", "Ibanez", "Music Man", "Warwick", "Squier")
        ));

        // Organ
        CATEGORIES.put("organ", new CategoryInfo(
            "organ",
            "Organ / Đàn Organ",
            Arrays.asList("keyboard organ", "arranger keyboard", "workstation"),
            "Organ và keyboard là nhạc cụ đa năng, phù hợp cho nhạc gia đình, nhà thờ, band. Có các loại: arranger keyboard (tự động hát), workstation (chuyên nghiệp), synthesizer. Thương hiệu: Yamaha, Korg, Roland, Casio.",
            Arrays.asList("Yamaha", "Korg", "Roland", "Casio", " Kurzweil")
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // CÁC THƯƠNG HIỆU NỔI TIẾNG
    // ─────────────────────────────────────────────────────────────────────────────
    public static final Map<String, BrandInfo> BRANDS = new LinkedHashMap<>();

    static {
        BRANDS.put("fender", new BrandInfo(
            "Fender",
            "USA",
            "Thương hiệu guitar và bass nổi tiếng nhất thế giới, được thành lập năm 1946. Nổi tiếng với dòng Stratocaster, Telecaster, Precision Bass.",
            Arrays.asList("guitar", "bass", "amplifier")
        ));
        BRANDS.put("gibson", new BrandInfo(
            "Gibson",
            "USA",
            "Thương hiệu guitar hàng đầu, nổi tiếng với Les Paul, SG, Firebird. Được thành lập năm 1902 tại Nashville, Tennessee.",
            Arrays.asList("guitar", "bass")
        ));
        BRANDS.put("yamaha", new BrandInfo(
            "Yamaha",
            "Nhật Bản",
            "Tập đoàn Yamaha sản xuất đa dạng nhạc cụ từ piano, guitar đến drum. Nổi tiếng với độ bền và chất lượng cao cấp.",
            Arrays.asList("piano", "guitar", "drum", "keyboard", "violin")
        ));
        BRANDS.put("steinway", new BrandInfo(
            "Steinway & Sons",
            "USA",
            "Thương hiệu grand piano sang trọng nhất thế giới, được thành lập năm 1853. Được sử dụng trong các nhạc viện và phòng hòa nhạc trên toàn thế giới.",
            Arrays.asList("piano")
        ));
        BRANDS.put("roland", new BrandInfo(
            "Roland",
            "Nhật Bản",
            "Chuyên về nhạc cụ điện tử, nổi tiếng với electronic drum (TD series), digital piano, synthesizer. Được các nghệ sĩ chuyên nghiệp ưa chuộng.",
            Arrays.asList("piano", "drum", "keyboard", "synthesizer")
        ));
        BRANDS.put("taylor", new BrandInfo(
            "Taylor Guitars",
            "USA",
            "Thương hiệu guitar acoustic cao cấp, nổi tiếng với thiết kế hiện đại và âm thanh phong phú. Được thành lập năm 1974 tại California.",
            Arrays.asList("guitar")
        ));
        BRANDS.put("martin", new BrandInfo(
            "C.F. Martin & Co.",
            "USA",
            "Thương hiệu guitar lâu đời nhất tại Mỹ, được thành lập năm 1833. Nổi tiếng với dòng D-28, Martin dreadnought.",
            Arrays.asList("guitar")
        ));
        BRANDS.put("shure", new BrandInfo(
            "Shure",
            "USA",
            "Thương hiệu microphone chuyên nghiệp hàng đầu, được sử dụng trong các buổi biểu diễn và studio trên toàn thế giới. Nổi tiếng với dòng SM58, SM57.",
            Arrays.asList("microphone")
        ));
        BRANDS.put("marshall", new BrandInfo(
            "Marshall Amplification",
            "Anh",
            "Thương hiệu amplifier huyền thoại, được các ban nhạc rock sử dụng. Nổi tiếng với stack amplifier và âm thanh distortion đặc trưng.",
            Arrays.asList("amplifier")
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // CÁC CÂU HỎI THƯỜNG GẶP (FAQ)
    // ─────────────────────────────────────────────────────────────────────────────
    public static final Map<String, String> FAQ_ANSWERS = new LinkedHashMap<>();

    static {
        // Về giao hàng
        FAQ_ANSWERS.put("giao hang", "MelodyShop giao hàng toàn quốc. Thời gian giao:\n- Nội thành HCM/HN: 1-2 ngày\n- Các tỉnh: 2-5 ngày\nPhí giao hàng: Miễn phí cho đơn từ 500,000đ");
        FAQ_ANSWERS.put("phi van chuyen", "Phí vận chuyển:\n- Miễn phí cho đơn từ 500,000đ\n- 30,000đ cho đơn dưới 500,000đ\n- Giao hàng nhanh (24h): +30,000đ");
        FAQ_ANSWERS.put("thoi gian giao", "Thời gian giao hàng:\n- HCM/HN nội thành: 1-2 ngày\n- Các thành phố lớn: 2-3 ngày\n- Tỉnh khác: 3-5 ngày\n- Giao hàng nhanh 24h có thêm phí");

        // Về thanh toán
        FAQ_ANSWERS.put("thanh toan", "MelodyShop chấp nhận:\n- Thanh toán khi nhận hàng (COD)\n- Chuyển khoản ngân hàng\n- Thẻ tín dụng/ghi nợ\n- Ví điện tử (MoMo, ZaloPay, VNPay)");
        FAQ_ANSWERS.put("tra gop", "MelodyShop hỗ trợ trả góp 0% lãi suất với thẻ tín dụng của các ngân hàng: Vietcombank, Techcombank, BIDV, VPBank. Thời hạn: 3, 6, 9, 12 tháng.");

        // Về bảo hành
        FAQ_ANSWERS.put("bao hanh", "Chính sách bảo hành MelodyShop:\n- Bảo hành chính hãng theo quy định nhà sản xuất\n- Guitar/Piano: Bảo hành 2-5 năm tùy thương hiệu\n- Thiết bị điện tử: Bảo hành 1-2 năm\n- Phụ kiện: Bảo hành theo từng sản phẩm");
        FAQ_ANSWERS.put("doi tra", "Chính sách đổi trả:\n- Đổi trả trong 7 ngày nếu sản phẩm lỗi từ nhà sản xuất\n- Sản phẩm phải còn nguyên vẹn, đầy đủ phụ kiện\n- Không áp dụng cho phụ kiện đã qua sử dụng");

        // Về dịch vụ
        FAQ_ANSWERS.put("tu van", "MelodyShop có đội ngũ tư vấn chuyên nghiệp:\n- Tư vấn qua hotline: 1900.xxxx\n- Chat trực tuyến 24/7\n- Email: support@melodyshop.vn\nHỗ trợ chọn nhạc cụ phù hợp với nhu cầu và ngân sách của bạn.");
        FAQ_ANSWERS.put("ho tro", "MelodyShop hỗ trợ khách hàng qua:\n- Hotline: 1900.xxxx (8h-22h)\n- Chat website 24/7\n- Zalo OA: MelodyShop\n- Email: support@melodyshop.vn");

        // Về cửa hàng
        FAQ_ANSWERS.put("cua hang", "MelodyShop có cửa hàng tại:\n- HCM: 123 Nguyễn Trãi, Q.1\n- HN: 456 Trần Duy Hưng, Cầu Giấy\n- ĐN: 789 Nguyễn Văn Linh\nMở cửa: 8h-21h (Thứ 2 - CN)");
        FAQ_ANSWERS.put("showroom", "MelodyShop có showroom trưng bày sản phẩm tại HCM và HN. Bạn có thể đến trực tiếp để:\n- Trải nghiệm chơi thử nhạc cụ\n- Được tư vấn 1-1 với chuyên gia\n- Nghe demo âm thanh");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // CÁC DỊCH VỤ CỦA MELODYSHOP
    // ─────────────────────────────────────────────────────────────────────────────
    public static final String SERVICES = """
        MelodyShop cung cấp các dịch vụ:
        
        1. MUA SẮM NHẠC CỤ
        - Hơn 10,000 sản phẩm từ các thương hiệu nổi tiếng
        - Guitar, Piano, Drum, Violin, Amplifier, Microphone...
        
        2. GIAO HÀNG TOÀN QUỐC
        - Giao nhanh 1-5 ngày
        - Miễn phí vận chuyển đơn từ 500,000đ
        
        3. BẢO HÀNH CHÍNH HÃNG
        - Bảo hành 1-5 năm tùy sản phẩm
        - Hỗ trợ sửa chữa tại các chi nhánh
        
        4. ĐỔI TRẢ DỄ DÀNG
        - Đổi trả trong 7 ngày nếu lỗi nhà sản xuất
        - Hoàn tiền 100% hoặc đổi sản phẩm khác
        
        5. TRẢ GÓP 0%
        - Hỗ trợ trả góp với thẻ tín dụng
        - Thời hạn linh hoạt: 3-12 tháng
        
        6. TƯ VẤN CHUYÊN NGHIỆP
        - Đội ngũ tư vấn có kinh nghiệm
        - Hỗ trợ chọn nhạc cụ phù hợp
        """;

    // ─────────────────────────────────────────────────────────────────────────────
    // KIỂM TRA CÓ PHẢI CÂU HỎI VỀ PROJECT KHÔNG
    // ─────────────────────────────────────────────────────────────────────────────
    private static final Set<String> PROJECT_KEYWORDS = new HashSet<>(Arrays.asList(
        // Từ khóa về shop
        "melodyshop", "shop", "cua hang", "cửa hàng", "ban", "bán", "mua sam", "mua sắm",
        // Từ khóa về dịch vụ
        "giao hang", "giao hàng", "van chuyen", "vận chuyển", "thanh toan", "thanh toán",
        "tra gop", "trả góp", "bao hanh", "bảo hành", "doi tra", "đổi trả",
        "tu van", "tư vấn", "ho tro", "hỗ trợ",
        // Từ khóa về sản phẩm
        "san pham", "sản phẩm", "nhac cu", "nhạc cụ", "danh muc", "danh mục",
        "thuong hieu", "thương hiệu", "gia", "giá", "giai", "giải",
        // Từ khóa về hỏi thông tin
        "la gi", "là gì", "the nao", "thế nào", "như thế nào", "ra sao",
        "co gi", "có gì", "có gì mới", "những gì",
        // Từ khóa về chính sách
        "chinh sach", "chính sách", "quy dinh", "quy định", "khuyen mai", "khuyến mãi"
    ));

    private static final Set<String> PROJECT_QUESTION_STARTERS = new HashSet<>(Arrays.asList(
        "cho hoi", "cho tôi hỏi", "hỏi", "tôi muốn biết", "bạn có", "bạn là", "bạn là gì",
        "melodyshop la", "melodyshop là", "shop nay", "shop này", "cua hang nay", "cửa hàng này",
        "thong tin", "thông tin", "gioi thieu", "giới thiệu", "nói về", "nói cho tôi",
        "khi nao", "khi nào", "o dau", "ở đâu", "bao nhieu", "bao nhiêu"
    ));

    /**
     * Kiểm tra xem message có phải là câu hỏi về project không
     */
    public boolean isProjectRelatedQuestion(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String msg = message.toLowerCase().trim();

        // Check question starters
        for (String starter : PROJECT_QUESTION_STARTERS) {
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

        // Check if it's a short question (likely project-related)
        if (msg.length() < 50 && (msg.contains("?") || msg.contains("khong") || msg.contains("ko")
            || msg.endsWith("hong") || msg.endsWith("hok") || msg.endsWith("hả"))) {
            return true;
        }

        return false;
    }

    /**
     * Tạo prompt cho Gemini để trả lời câu hỏi về project
     */
    public String buildProjectPrompt(String userMessage, String contextInfo) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Ban la tro ly AI cua MelodyShop - cua hang nhac cu truc tuyen hang dau Viet Nam.\n\n");

        prompt.append("## THONG TIN VE MELODYSHOP\n");
        prompt.append(SHOP_NAME).append(" - ").append(SHOP_DESCRIPTION).append("\n");
        prompt.append("Tagline: ").append(SHOP_TAGLINE).append("\n\n");

        prompt.append("## CAC DANH MUC SAN PHAM\n");
        CATEGORIES.forEach((key, cat) -> {
            prompt.append("- ").append(cat.name).append(": ").append(cat.description.substring(0, Math.min(100, cat.description.length()))).append("...\n");
        });
        prompt.append("\n");

        prompt.append("## CAC THUONG HIEU NOI TIENG\n");
        BRANDS.forEach((key, brand) -> {
            prompt.append("- ").append(brand.name).append(" (").append(brand.country).append("): ").append(brand.description.substring(0, Math.min(80, brand.description.length()))).append("...\n");
        });
        prompt.append("\n");

        prompt.append("## CAC DICH VU\n");
        prompt.append(SERVICES).append("\n");

        prompt.append("## CAC CAU HOI THUONG GAP\n");
        FAQ_ANSWERS.forEach((key, answer) -> {
            prompt.append("Q: ").append(key).append("\n");
            prompt.append("A: ").append(answer).append("\n\n");
        });

        if (contextInfo != null && !contextInfo.isEmpty()) {
            prompt.append("\n## NGU CANH HIEN TAI\n");
            prompt.append(contextInfo);
        }

        prompt.append("\n\n## YEU CAU\n");
        prompt.append("1. Tra loi chi tiet, day du thong tin\n");
        prompt.append("2. Neu cau hoi ve san pham, goi y cac san pham cu the\n");
        prompt.append("3. Neu cau hoi ve dich vu, giai thich ro rang\n");
        prompt.append("4. Neu khong biet, noi 'Xin loi, toi chua co thong tin ve dieu nay. Ban co the lien he hotline 1900.xxxx de duoc ho tro.'\n");
        prompt.append("5. Chi tra loi ve nhung gi lien quan den MelodyShop\n");
        prompt.append("6. Tra loi bang tieng Viet\n");
        prompt.append("7. Neu nguoi dung hoi ve gia, hay noi 'Ban co the xem gia chinh xac tren trang san pham' hoac goi y hoi 'ban muon tim san pham nao?'\n\n");

        prompt.append("Cau hoi: ").append(userMessage).append("\n");

        return prompt.toString();
    }

    /**
     * Lấy thông tin về một danh mục
     */
    public CategoryInfo getCategoryInfo(String categoryName) {
        if (categoryName == null) return null;
        String key = categoryName.toLowerCase().trim();
        return CATEGORIES.get(key);
    }

    /**
     * Lấy thông tin về một thương hiệu
     */
    public BrandInfo getBrandInfo(String brandName) {
        if (brandName == null) return null;
        String key = brandName.toLowerCase().trim();
        return BRANDS.get(key);
    }

    /**
     * Lấy câu trả lời cho câu hỏi thường gặp
     */
    public String getFaqAnswer(String keyword) {
        if (keyword == null) return null;
        String key = keyword.toLowerCase().trim();
        return FAQ_ANSWERS.get(key);
    }

    /**
     * Tìm kiếm category hoặc brand từ query
     */
    public String findRelatedInfo(String query) {
        if (query == null) return null;
        String q = query.toLowerCase();

        // Check brands
        for (Map.Entry<String, BrandInfo> entry : BRANDS.entrySet()) {
            if (q.contains(entry.getKey()) || q.contains(entry.getValue().name.toLowerCase())) {
                BrandInfo brand = entry.getValue();
                return String.format("Thuong hieu %s (%s): %s. %s co tai MelodyShop.",
                    brand.name, brand.country, brand.description, brand.name);
            }
        }

        // Check categories
        for (Map.Entry<String, CategoryInfo> entry : CATEGORIES.entrySet()) {
            if (q.contains(entry.getKey()) || q.contains(entry.getValue().name.toLowerCase())) {
                CategoryInfo cat = entry.getValue();
                return String.format("Danh muc %s: %s. Cac loai: %s. Thuong hieu noi tieng: %s.",
                    cat.name, cat.description, String.join(", ", cat.types), String.join(", ", cat.topBrands));
            }
        }

        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // INNER CLASSES
    // ─────────────────────────────────────────────────────────────────────────────
    public static class CategoryInfo {
        public final String key;
        public final String name;
        public final List<String> types;
        public final String description;
        public final List<String> topBrands;

        public CategoryInfo(String key, String name, List<String> types, String description, List<String> topBrands) {
            this.key = key;
            this.name = name;
            this.types = types;
            this.description = description;
            this.topBrands = topBrands;
        }
    }

    public static class BrandInfo {
        public final String name;
        public final String country;
        public final String description;
        public final List<String> categories;

        public BrandInfo(String name, String country, String description, List<String> categories) {
            this.name = name;
            this.country = country;
            this.description = description;
            this.categories = categories;
        }
    }
}
