package com.melodyshop.product.service;

import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.product.dto.BrandDTO;
import com.melodyshop.product.entity.Brand;
import com.melodyshop.product.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    public List<BrandDTO> getAllBrands() {
        return brandRepository.findByIsActiveTrueOrderByNameAsc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public BrandDTO getBrandById(String id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu", "id", id));
        return toDTO(brand);
    }

    @Transactional
    public BrandDTO createBrand(BrandDTO dto) {
        String slug = generateSlug(dto.getName());
        if (brandRepository.existsBySlug(slug)) {
            throw new BadRequestException("Slug thương hiệu đã tồn tại: " + slug);
        }

        Brand brand = Brand.builder()
                .name(dto.getName())
                .slug(slug)
                .description(dto.getDescription())
                .logoUrl(dto.getLogoUrl())
                .isActive(true)
                .build();

        brand = brandRepository.save(brand);
        return toDTO(brand);
    }

    @Transactional
    public BrandDTO updateBrand(String id, BrandDTO dto) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu", "id", id));

        brand.setName(dto.getName());
        if (dto.getDescription() != null) brand.setDescription(dto.getDescription());
        if (dto.getLogoUrl() != null) brand.setLogoUrl(dto.getLogoUrl());
        if (dto.getIsActive() != null) brand.setIsActive(dto.getIsActive());

        brand = brandRepository.save(brand);
        return toDTO(brand);
    }

    @Transactional
    public void deleteBrand(String id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu", "id", id));
        brand.setIsActive(false);
        brandRepository.save(brand);
    }

    private BrandDTO toDTO(Brand b) {
        return BrandDTO.builder()
                .id(b.getId())
                .name(b.getName())
                .slug(b.getSlug())
                .description(b.getDescription())
                .logoUrl(b.getLogoUrl())
                .isActive(b.getIsActive())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
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
