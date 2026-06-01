package com.melodyshop.product.service;

import com.melodyshop.product.client.InventoryClient;
import com.melodyshop.product.client.OrderClient;
import com.melodyshop.product.dto.CreateProductRequest;
import com.melodyshop.product.dto.ProductDTO;
import com.melodyshop.product.dto.ProductVariantDTO;
import com.melodyshop.product.entity.Product;
import com.melodyshop.product.entity.ProductVariant;
import com.melodyshop.product.repository.BrandRepository;
import com.melodyshop.product.repository.CategoryRepository;
import com.melodyshop.product.repository.ProductImageRepository;
import com.melodyshop.product.repository.ProductRepository;
import com.melodyshop.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private ProductRepository productRepository;
    private ProductVariantRepository variantRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        variantRepository = mock(ProductVariantRepository.class);
        productService = new ProductService(
                productRepository,
                variantRepository,
                mock(ProductImageRepository.class),
                mock(CategoryRepository.class),
                mock(BrandRepository.class),
                mock(InventoryClient.class),
                mock(OrderClient.class));
    }

    @Test
    void updateProduct_shouldSyncGeneratedDefaultVariantPriceWithBasePrice() {
        ProductVariant variant = variant("variant-default", "YAMAHA-CLAVINOVA-745-DEFAULT", "Mac dinh", "52000000");
        Product product = product("product-001", "Yamaha Clavinova CLP-745", "3000", variant);
        when(productRepository.findById("product-001")).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(variantRepository.save(variant)).thenReturn(variant);

        ProductDTO result = productService.updateProduct("product-001", request("3000", List.of(toDTO(variant))));

        assertEquals(0, new BigDecimal("3000").compareTo(variant.getPrice()));
        assertEquals(0, new BigDecimal("3000").compareTo(result.getVariants().get(0).getPrice()));
        verify(variantRepository).save(variant);
    }

    @Test
    void updateProduct_shouldPersistExplicitVariantPrice() {
        ProductVariant variant = variant("variant-black", "FENDER-BLACK", "Black", "25000000");
        Product product = product("product-002", "Fender Stratocaster", "25000000", variant);
        when(productRepository.findById("product-002")).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(variantRepository.save(variant)).thenReturn(variant);
        ProductVariantDTO updatedVariant = toDTO(variant);
        updatedVariant.setPrice(new BigDecimal("26000000"));

        ProductDTO result = productService.updateProduct("product-002", request("25000000", List.of(updatedVariant)));

        assertEquals(0, new BigDecimal("26000000").compareTo(variant.getPrice()));
        assertEquals(0, new BigDecimal("26000000").compareTo(result.getVariants().get(0).getPrice()));
        verify(variantRepository).save(variant);
    }

    private Product product(String id, String name, String basePrice, ProductVariant variant) {
        Product product = Product.builder()
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .basePrice(new BigDecimal(basePrice))
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .build();
        product.setId(id);
        variant.setProduct(product);
        product.getVariants().add(variant);
        return product;
    }

    private ProductVariant variant(String id, String sku, String name, String price) {
        ProductVariant variant = ProductVariant.builder()
                .variantName(name)
                .sku(sku)
                .price(new BigDecimal(price))
                .isActive(true)
                .build();
        variant.setId(id);
        return variant;
    }

    private CreateProductRequest request(String basePrice, List<ProductVariantDTO> variants) {
        return CreateProductRequest.builder()
                .name("Updated product")
                .basePrice(new BigDecimal(basePrice))
                .variants(variants)
                .build();
    }

    private ProductVariantDTO toDTO(ProductVariant variant) {
        return ProductVariantDTO.builder()
                .id(variant.getId())
                .variantName(variant.getVariantName())
                .sku(variant.getSku())
                .price(variant.getPrice())
                .isActive(variant.getIsActive())
                .build();
    }
}
