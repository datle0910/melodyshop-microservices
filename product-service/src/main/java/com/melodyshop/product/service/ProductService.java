package com.melodyshop.product.service;

import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.product.dto.*;
import com.melodyshop.product.entity.*;
import com.melodyshop.product.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final com.melodyshop.product.client.InventoryClient inventoryClient;

    /**
     * Lấy danh sách sản phẩm với phân trang, lọc, sắp xếp.
     */
    public Page<ProductDTO> getProducts(
            String keyword, String categoryId, String brandId,
            BigDecimal minPrice, BigDecimal maxPrice,
            Boolean isFeatured, String sortBy, String sortDir,
            int page, int size) {

        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> products = productRepository.findAll(
                ProductSpecification.filter(keyword, categoryId, brandId, minPrice, maxPrice, isFeatured, true),
                pageable
        );

        return products.map(this::toDTO);
    }

    /**
     * Lấy sản phẩm nổi bật cho trang chủ.
     */
    public List<ProductDTO> getFeaturedProducts(int limit) {
        return productRepository.findFeaturedProducts(PageRequest.of(0, limit))
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Chi tiết sản phẩm theo ID.
     */
    public ProductDTO getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", id));
        return toDTO(product);
    }

    /**
     * Chi tiết sản phẩm theo slug.
     */
    public ProductDTO getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "slug", slug));
        return toDTO(product);
    }

    /**
     * Thêm sản phẩm mới.
     */
    @Transactional
    public ProductDTO createProduct(CreateProductRequest request) {
        String slug = generateSlug(request.getName());
        if (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        Product product = Product.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .shortDesc(request.getShortDesc())
                .basePrice(request.getBasePrice())
                .categoryId(request.getCategoryId())
                .brandId(request.getBrandId())
                .specs(request.getSpecs())
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .isActive(true)
                .build();

        product = productRepository.save(product);

        // Tạo các biến thể nếu có
        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            for (ProductVariantDTO v : request.getVariants()) {
                if (variantRepository.existsBySku(v.getSku())) {
                    throw new BadRequestException("SKU đã tồn tại: " + v.getSku());
                }
                ProductVariant variant = ProductVariant.builder()
                        .product(product)
                        .variantName(v.getVariantName())
                        .sku(v.getSku())
                        .price(v.getPrice())
                        .color(v.getColor())
                        .size(v.getSize())
                        .isActive(true)
                        .build();
                variant = variantRepository.save(variant);

                // Gọi Inventory Service để khởi tạo kho
                try {
                    inventoryClient.initInventory(product.getId(), variant.getId(), variant.getSku());
                } catch (Exception e) {
                    // Log error but don't fail the whole transaction if inventory fails
                    // In real production, you might want to retry or use a message queue
                    System.err.println("Failed to initialize inventory for SKU " + v.getSku() + ": " + e.getMessage());
                }
            }
        }

        return toDTO(productRepository.findById(product.getId()).orElse(product));
    }

    /**
     * Cập nhật sản phẩm.
     */
    @Transactional
    public ProductDTO updateProduct(String id, CreateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", id));

        product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getShortDesc() != null) product.setShortDesc(request.getShortDesc());
        if (request.getBasePrice() != null) product.setBasePrice(request.getBasePrice());
        if (request.getCategoryId() != null) product.setCategoryId(request.getCategoryId());
        if (request.getBrandId() != null) product.setBrandId(request.getBrandId());
        if (request.getSpecs() != null) product.setSpecs(request.getSpecs());
        if (request.getIsFeatured() != null) product.setIsFeatured(request.getIsFeatured());

        product = productRepository.save(product);
        return toDTO(product);
    }

    /**
     * Soft delete sản phẩm.
     */
    @Transactional
    public void deleteProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", id));
        
        // Xóa các biến thể trước để tránh lỗi khóa ngoại
        if (product.getVariants() != null) {
            variantRepository.deleteAll(product.getVariants());
        }
        
        productRepository.delete(product);
    }

    /**
     * Cập nhật avg_rating và review_count sau khi có review mới.
     */
    @Transactional
    public void updateProductRating(String productId, double avgRating, int reviewCount) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "id", productId));
        product.setAvgRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP));
        product.setReviewCount(reviewCount);
        productRepository.save(product);
    }

    // ======== Private helper methods ========

    private ProductDTO toDTO(Product p) {
        List<ProductVariantDTO> variants = p.getVariants() != null ?
                p.getVariants().stream().map(this::toVariantDTO).collect(Collectors.toList()) : List.of();
        List<ProductImageDTO> images = p.getImages() != null ?
                p.getImages().stream().map(this::toImageDTO).collect(Collectors.toList()) : List.of();

        String categoryName = null;
        if (p.getCategoryId() != null) {
            categoryName = categoryRepository.findById(p.getCategoryId()).map(c -> c.getName()).orElse(null);
        }
        String brandName = null;
        if (p.getBrandId() != null) {
            brandName = brandRepository.findById(p.getBrandId()).map(b -> b.getName()).orElse(null);
        }

        return ProductDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .slug(p.getSlug())
                .description(p.getDescription())
                .shortDesc(p.getShortDesc())
                .basePrice(p.getBasePrice())
                .categoryId(p.getCategoryId())
                .categoryName(categoryName)
                .brandId(p.getBrandId())
                .brandName(brandName)
                .specs(p.getSpecs())
                .isFeatured(p.getIsFeatured())
                .isActive(p.getIsActive())
                .avgRating(p.getAvgRating())
                .reviewCount(p.getReviewCount())
                .variants(variants)
                .images(images)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private ProductVariantDTO toVariantDTO(ProductVariant v) {
        return ProductVariantDTO.builder()
                .id(v.getId())
                .variantName(v.getVariantName())
                .sku(v.getSku())
                .price(v.getPrice())
                .color(v.getColor())
                .size(v.getSize())
                .isActive(v.getIsActive())
                .build();
    }

    private ProductImageDTO toImageDTO(ProductImage i) {
        return ProductImageDTO.builder()
                .id(i.getId())
                .imageUrl(i.getImageUrl())
                .altText(i.getAltText())
                .sortOrder(i.getSortOrder())
                .isPrimary(i.getIsPrimary())
                .build();
    }

    private Sort buildSort(String sortBy, String sortDir) {
        if (sortBy == null || sortBy.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return switch (sortBy.toLowerCase()) {
            case "price", "baseprice" -> Sort.by(direction, "basePrice");
            case "name" -> Sort.by(direction, "name");
            case "rating" -> Sort.by(direction, "avgRating");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "popular" -> Sort.by(Sort.Direction.DESC, "reviewCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[áàảãạăắằẳẵặâấầẩẫậ]", "a")
                .replaceAll("[éèẻẽẹêếềểễệ]", "e")
                .replaceAll("[íìỉĩị]", "i")
                .replaceAll("[óòỏõọôốồổỗộơớờởỡợ]", "o")
                .replaceAll("[úùủũụưứừửữự]", "u")
                .replaceAll("[ýỳỷỹỵ]", "y")
                .replaceAll("[đ]", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
