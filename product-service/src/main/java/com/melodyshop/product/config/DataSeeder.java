package com.melodyshop.product.config;

import com.melodyshop.product.entity.Brand;
import com.melodyshop.product.entity.Category;
import com.melodyshop.product.entity.Product;
import com.melodyshop.product.entity.ProductImage;
import com.melodyshop.product.repository.BrandRepository;
import com.melodyshop.product.repository.CategoryRepository;
import com.melodyshop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info("Database already seeded with products. Skipping...");
            return;
        }

        log.info("Seeding initial data for Product Service...");

        // 1. Seed Categories
        Map<String, Category> cats = seedCategories();
        
        // 2. Seed Brands
        Map<String, Brand> brands = seedBrands();

        // 3. Seed Products
        seedProducts(cats, brands);

        log.info("Successfully seeded 20 products with images!");
    }

    private Map<String, Category> seedCategories() {
        List<Category> categories = List.of(
            Category.builder().name("Guitar").slug("guitar").description("Đàn Guitar Acoustic, Electric, Classical").build(),
            Category.builder().name("Piano").slug("piano").description("Đàn Piano Grand, Upright, Digital").build(),
            Category.builder().name("Trống").slug("drums").description("Bộ trống Jazz, Trống điện tử").build(),
            Category.builder().name("Vĩ cầm").slug("violin").description("Đàn Violin, Viola, Cello").build(),
            Category.builder().name("Phụ kiện").slug("accessories").description("Dây đàn, Bao đàn, Capo, Pick").build()
        );
        categoryRepository.saveAll(categories);
        
        return Map.of(
            "guitar", categories.get(0),
            "piano", categories.get(1),
            "drums", categories.get(2),
            "violin", categories.get(3),
            "acc", categories.get(4)
        );
    }

    private Map<String, Brand> seedBrands() {
        List<Brand> brands = List.of(
            Brand.builder().name("Fender").slug("fender").description("Huyền thoại Guitar từ USA").build(),
            Brand.builder().name("Yamaha").slug("yamaha").description("Chất lượng âm thanh tuyệt đỉnh từ Nhật Bản").build(),
            Brand.builder().name("Roland").slug("roland").description("Dẫn đầu công nghệ nhạc cụ điện tử").build(),
            Brand.builder().name("Steinway & Sons").slug("steinway").description("Đẳng cấp Piano thế giới").build(),
            Brand.builder().name("Gibson").slug("gibson").description("Biểu tượng Rock & Roll").build()
        );
        brandRepository.saveAll(brands);
        
        return Map.of(
            "fender", brands.get(0),
            "yamaha", brands.get(1),
            "roland", brands.get(2),
            "steinway", brands.get(3),
            "gibson", brands.get(4)
        );
    }

    private void seedProducts(Map<String, Category> cats, Map<String, Brand> brands) {
        List<Product> products = new ArrayList<>();

        // --- GUITARS ---
        products.add(createProduct("Fender Player Stratocaster", "fender-player-strat", 
            "Dòng Guitar điện phổ biến nhất thế giới.", brands.get("fender"), cats.get("guitar"), 
            21500000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Fender_Player_Stratocaster.jpg", true));

        products.add(createProduct("Yamaha FG800 Acoustic", "yamaha-fg800", 
            "Lựa chọn hoàn hảo cho người mới bắt đầu.", brands.get("yamaha"), cats.get("guitar"), 
            5500000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Yamaha_FG800_Acoustic.jpg", false));

        products.add(createProduct("Gibson Les Paul Standard", "gibson-les-paul-std", 
            "Âm thanh dày, ấm và đầy uy lực.", brands.get("gibson"), cats.get("guitar"), 
            65000000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Gibson_Les_Paul_Standard.jpg", true));

        products.add(createProduct("Fender American Ultra Telecaster", "fender-ultra-tele", 
            "Đỉnh cao công nghệ và thiết kế của Fender.", brands.get("fender"), cats.get("guitar"), 
            48900000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Fender_American_Ultra_Telecaster.jpg", false));

        // --- PIANOS ---
        products.add(createProduct("Steinway Model D Grand Piano", "steinway-model-d", 
            "Chiếc Piano tốt nhất mà con người từng chế tạo.", brands.get("steinway"), cats.get("piano"), 
            4500000000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Steinway_Model_D_Grand_Piano.jpg", true));

        products.add(createProduct("Yamaha U3 Upright Piano", "yamaha-u3", 
            "Tiêu chuẩn vàng cho đàn Piano đứng giáo dục.", brands.get("yamaha"), cats.get("piano"), 
            185000000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Yamaha_U3_Upright_Piano.jpg", false));

        products.add(createProduct("Roland FP-30X Digital Piano", "roland-fp30x", 
            "Đàn piano điện di động with âm thanh siêu thực.", brands.get("roland"), cats.get("piano"), 
            19500000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Roland_FP_30X_Digital_Piano.jpg", false));

        products.add(createProduct("Yamaha Clavinova CLP-745", "yamaha-clavinova-745", 
            "Cảm giác phím gỗ hệt như đàn Grand.", brands.get("yamaha"), cats.get("piano"), 
            52000000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Yamaha_Clavinova_CLP-745.png", true));

        // --- DRUMS ---
        products.add(createProduct("Roland TD-17KVX V-Drums", "roland-td17kvx", 
            "Trống điện tử chuyên nghiệp cho luyện tập tại nhà.", brands.get("roland"), cats.get("drums"), 
            42000000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Roland_TD-17KVX_V-Drums.jpg", true));

        products.add(createProduct("Yamaha Rydeen Acoustic Drum Set", "yamaha-rydeen", 
            "Bộ trống cơ entry-level bền bỉ.", brands.get("yamaha"), cats.get("drums"), 
            18000000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Yamaha_Rydeen_Acoustic_Drum_Set.jpg", false));

        // --- VIOLIN & OTHERS ---
        products.add(createProduct("Yamaha V3S Student Violin", "yamaha-v3s", 
            "Đàn Violin cho sinh viên với chất âm ổn định.", brands.get("yamaha"), cats.get("violin"), 
            8500000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Yamaha_V3S_Student_Violin.jpg", false));

        products.add(createProduct("Steinway Violin Master Edition", "steinway-violin", 
            "Tuyệt tác vĩ cầm phiên bản giới hạn.", brands.get("steinway"), cats.get("violin"), 
            120000000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Steinway_Violin_Master_Edition.jpg", true));

        // Adding more to reach ~20
        products.add(createProduct("Fender Precision Bass", "fender-p-bass", 
            "Chiếc bass đã định hình âm nhạc hiện đại.", brands.get("fender"), cats.get("guitar"), 
            25000000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Fender_Precision_Bass.jpg", false));

        products.add(createProduct("Gibson SG Standard", "gibson-sg-std", 
            "Thiết kế Double-cut mang tính biểu tượng.", brands.get("gibson"), cats.get("guitar"), 
            45000000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Gibson_SG_Standard.jpg", false));

        products.add(createProduct("Roland JUNO-DS88", "roland-juno-ds", 
            "Synthesizer mạnh mẽ cho biểu diễn sân khấu.", brands.get("roland"), cats.get("piano"), 
            23000000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Roland_JUNO-DS88_Synthesizer.png", false));

        products.add(createProduct("Yamaha Saxophone YAS-280", "yamaha-yas-280", 
            "Alto Saxophone tốt nhất cho người học.", brands.get("yamaha"), cats.get("acc"), 
            28000000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Yamaha_Saxophone_YAS-280.jpg", false));

        products.add(createProduct("Fender Mustang Micro", "fender-mustang-micro", 
            "Amp headphone siêu nhỏ gọn.", brands.get("fender"), cats.get("acc"), 
            2800000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Fender_Mustang_Micro.jpg", false));

        products.add(createProduct("Yamaha Drum Stick 5A", "yamaha-stick-5a", 
            "Dù trống gỗ Maple cao cấp.", brands.get("yamaha"), cats.get("acc"), 
            250000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Yamaha_Drum_Stick_5A.jpg", false));

        products.add(createProduct("Steinway Piano Bench", "steinway-bench", 
            "Ghế ngồi Piano đệm nhung sang trọng.", brands.get("steinway"), cats.get("acc"), 
            15000000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Steinway_Piano_Bench.jpg", false));

        products.add(createProduct("Roland RH-5 Headphones", "roland-rh5", 
            "Tai nghe kiểm âm chất lượng phòng thu.", brands.get("roland"), cats.get("acc"), 
            1200000.0, "https://res.cloudinary.com/dulvkmply/image/upload/v1/melodyshop/products/Roland_RH-5_Headphones.jpg", false));

        productRepository.saveAll(products);
    }

    private Product createProduct(String name, String slug, String desc, Brand brand, Category cat, 
                                 Double price, String imageUrl, boolean featured) {
        Product product = Product.builder()
                .name(name)
                .slug(slug)
                .description(desc)
                .shortDesc(desc)
                .basePrice(BigDecimal.valueOf(price))
                .brandId(brand.getId())
                .categoryId(cat.getId())
                .isFeatured(featured)
                .isActive(true)
                .specs("{}")
                .build();

        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(imageUrl)
                .altText(name)
                .isPrimary(true)
                .sortOrder(0)
                .build();

        product.getImages().add(image);
        return product;
    }
}
