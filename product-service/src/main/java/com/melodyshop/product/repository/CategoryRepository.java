package com.melodyshop.product.repository;

import com.melodyshop.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    List<Category> findByParentIdIsNullAndIsActiveTrueOrderBySortOrderAsc();
    List<Category> findByParentIdAndIsActiveTrueOrderBySortOrderAsc(String parentId);
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
