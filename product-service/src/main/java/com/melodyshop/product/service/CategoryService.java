package com.melodyshop.product.service;

import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.product.dto.CategoryDTO;
import com.melodyshop.product.entity.Category;
import com.melodyshop.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Lấy danh sách category dạng cây (tree).
     */
    public List<CategoryDTO> getCategoryTree() {
        List<Category> roots = categoryRepository.findByParentIdIsNullAndIsActiveTrueOrderBySortOrderAsc();
        return roots.stream().map(this::toDTOWithChildren).collect(Collectors.toList());
    }

    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public CategoryDTO getCategoryById(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", id));
        return toDTOWithChildren(category);
    }

    @Transactional
    public CategoryDTO createCategory(CategoryDTO dto) {
        String slug = generateSlug(dto.getName());
        if (categoryRepository.existsBySlug(slug)) {
            throw new BadRequestException("Slug danh mục đã tồn tại: " + slug);
        }

        Category category = Category.builder()
                .name(dto.getName())
                .slug(slug)
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .parentId(dto.getParentId())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .isActive(true)
                .build();

        category = categoryRepository.save(category);
        return toDTO(category);
    }

    @Transactional
    public CategoryDTO updateCategory(String id, CategoryDTO dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", id));

        category.setName(dto.getName());
        if (dto.getDescription() != null) category.setDescription(dto.getDescription());
        if (dto.getImageUrl() != null) category.setImageUrl(dto.getImageUrl());
        if (dto.getParentId() != null) category.setParentId(dto.getParentId());
        if (dto.getSortOrder() != null) category.setSortOrder(dto.getSortOrder());
        if (dto.getIsActive() != null) category.setIsActive(dto.getIsActive());

        category = categoryRepository.save(category);
        return toDTO(category);
    }

    @Transactional
    public void deleteCategory(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục", "id", id));
        // Soft delete
        category.setIsActive(false);
        categoryRepository.save(category);
    }

    private CategoryDTO toDTOWithChildren(Category category) {
        CategoryDTO dto = toDTO(category);
        List<Category> children = categoryRepository.findByParentIdAndIsActiveTrueOrderBySortOrderAsc(category.getId());
        dto.setChildren(children.stream().map(this::toDTOWithChildren).collect(Collectors.toList()));
        return dto;
    }

    private CategoryDTO toDTO(Category c) {
        return CategoryDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .description(c.getDescription())
                .imageUrl(c.getImageUrl())
                .parentId(c.getParentId())
                .sortOrder(c.getSortOrder())
                .isActive(c.getIsActive())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
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
