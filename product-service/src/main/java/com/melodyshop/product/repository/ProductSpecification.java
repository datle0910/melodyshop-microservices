package com.melodyshop.product.repository;

import com.melodyshop.product.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic query specifications for Product filtering.
 */
public class ProductSpecification {

    public static Specification<Product> filter(
            String keyword, String categoryId, String brandId,
            BigDecimal minPrice, BigDecimal maxPrice,
            Boolean isFeatured, Boolean isActive) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("shortDesc")), pattern)
                ));
            }

            if (categoryId != null && !categoryId.isBlank()) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }

            if (brandId != null && !brandId.isBlank()) {
                predicates.add(cb.equal(root.get("brandId"), brandId));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice));
            }

            if (isFeatured != null) {
                predicates.add(cb.equal(root.get("isFeatured"), isFeatured));
            }

            // Default: only show active products
            if (isActive == null || isActive) {
                predicates.add(cb.equal(root.get("isActive"), true));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
