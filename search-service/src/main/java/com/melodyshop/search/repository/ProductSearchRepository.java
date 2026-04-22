package com.melodyshop.search.repository;

import com.melodyshop.search.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    Page<ProductDocument> findByIsActiveTrue(Pageable pageable);

    Page<ProductDocument> findByCategoryIdAndIsActiveTrue(String categoryId, Pageable pageable);

    Page<ProductDocument> findByBrandIdAndIsActiveTrue(String brandId, Pageable pageable);

    Page<ProductDocument> findByBasePriceBetweenAndIsActiveTrue(
            BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    List<ProductDocument> findByIsFeaturedTrueAndIsActiveTrue();
}
