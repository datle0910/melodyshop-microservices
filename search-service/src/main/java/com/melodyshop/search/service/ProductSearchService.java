package com.melodyshop.search.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.melodyshop.search.document.ProductDocument;
import com.melodyshop.search.dto.ProductIndexRequest;
import com.melodyshop.search.dto.ProductSearchResult;
import com.melodyshop.search.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductSearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * Tìm kiếm sản phẩm siêu tốc theo tên, giá, loại.
     */
    public List<ProductSearchResult> search(
            String keyword, String categoryId, String brandId,
            BigDecimal minPrice, BigDecimal maxPrice,
            String sortBy, String sortDir,
            int page, int size) {

        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // Chỉ tìm sản phẩm active
        boolQuery.must(m -> m.term(t -> t.field("isActive").value(true)));

        // Tìm theo keyword (tên, mô tả)
        if (keyword != null && !keyword.isBlank()) {
            boolQuery.must(m -> m.multiMatch(mm -> mm
                    .query(keyword)
                    .fields("name^3", "name.autocomplete^2", "shortDesc", "description", "brandName", "categoryName")
                    .fuzziness("AUTO")
            ));
        }

        // Lọc theo category
        if (categoryId != null && !categoryId.isBlank()) {
            boolQuery.filter(f -> f.term(t -> t.field("categoryId").value(categoryId)));
        }

        // Lọc theo brand
        if (brandId != null && !brandId.isBlank()) {
            boolQuery.filter(f -> f.term(t -> t.field("brandId").value(brandId)));
        }

        // Lọc theo giá
        if (minPrice != null || maxPrice != null) {
            boolQuery.filter(f -> f.range(r -> {
                var rangeQuery = r.field("basePrice");
                if (minPrice != null) rangeQuery.gte(co.elastic.clients.json.JsonData.of(minPrice));
                if (maxPrice != null) rangeQuery.lte(co.elastic.clients.json.JsonData.of(maxPrice));
                return rangeQuery;
            }));
        }

        // Sorting
        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, size, sort);

        NativeQuery query = new NativeQueryBuilder()
                .withQuery(q -> q.bool(boolQuery.build()))
                .withPageable(pageable)
                .build();

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(query, ProductDocument.class);

        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toSearchResult)
                .collect(Collectors.toList());
    }

    /**
     * Gợi ý tìm kiếm (autocomplete).
     */
    public List<String> suggest(String prefix, int limit) {
        NativeQuery query = new NativeQueryBuilder()
                .withQuery(q -> q.match(m -> m
                        .field("name.autocomplete")
                        .query(prefix)
                ))
                .withPageable(PageRequest.of(0, limit))
                .build();

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);

        return hits.getSearchHits().stream()
                .map(h -> h.getContent().getName())
                .distinct()
                .collect(Collectors.toList());
    }

    // ==================== Index Management (Product Service gọi) ====================

    /**
     * Đồng bộ 1 sản phẩm vào Elasticsearch.
     */
    public void indexProduct(ProductIndexRequest request) {
        ProductDocument doc = ProductDocument.builder()
                .id(request.getId())
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .shortDesc(request.getShortDesc())
                .basePrice(request.getBasePrice())
                .categoryId(request.getCategoryId())
                .categoryName(request.getCategoryName())
                .brandId(request.getBrandId())
                .brandName(request.getBrandName())
                .isFeatured(request.getIsFeatured())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .avgRating(request.getAvgRating())
                .reviewCount(request.getReviewCount())
                .imageUrl(request.getImageUrl())
                .createdAt(LocalDateTime.now())
                .build();

        searchRepository.save(doc);
        log.info("Indexed product: {} ({})", doc.getName(), doc.getId());
    }

    /**
     * Đồng bộ nhiều sản phẩm (bulk).
     */
    public void indexProducts(List<ProductIndexRequest> requests) {
        List<ProductDocument> docs = requests.stream().map(r -> ProductDocument.builder()
                .id(r.getId())
                .name(r.getName())
                .slug(r.getSlug())
                .description(r.getDescription())
                .shortDesc(r.getShortDesc())
                .basePrice(r.getBasePrice())
                .categoryId(r.getCategoryId())
                .categoryName(r.getCategoryName())
                .brandId(r.getBrandId())
                .brandName(r.getBrandName())
                .isFeatured(r.getIsFeatured())
                .isActive(r.getIsActive() != null ? r.getIsActive() : true)
                .avgRating(r.getAvgRating())
                .reviewCount(r.getReviewCount())
                .imageUrl(r.getImageUrl())
                .createdAt(LocalDateTime.now())
                .build()
        ).collect(Collectors.toList());

        searchRepository.saveAll(docs);
        log.info("Bulk indexed {} products", docs.size());
    }

    /**
     * Xóa sản phẩm khỏi index.
     */
    public void deleteProduct(String id) {
        searchRepository.deleteById(id);
        log.info("Deleted product from index: {}", id);
    }

    // ==================== Private helpers ====================

    private ProductSearchResult toSearchResult(ProductDocument doc) {
        return ProductSearchResult.builder()
                .id(doc.getId())
                .name(doc.getName())
                .slug(doc.getSlug())
                .shortDesc(doc.getShortDesc())
                .basePrice(doc.getBasePrice())
                .categoryId(doc.getCategoryId())
                .categoryName(doc.getCategoryName())
                .brandId(doc.getBrandId())
                .brandName(doc.getBrandName())
                .isFeatured(doc.getIsFeatured())
                .avgRating(doc.getAvgRating())
                .reviewCount(doc.getReviewCount())
                .imageUrl(doc.getImageUrl())
                .build();
    }

    private Sort buildSort(String sortBy, String sortDir) {
        if (sortBy == null || sortBy.isBlank() || "relevance".equalsIgnoreCase(sortBy)) {
            return Sort.unsorted();
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return switch (sortBy.toLowerCase()) {
            case "price" -> Sort.by(direction, "basePrice");
            case "name" -> Sort.by(direction, "name.keyword");
            case "rating" -> Sort.by(Sort.Direction.DESC, "avgRating");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "popular" -> Sort.by(Sort.Direction.DESC, "reviewCount");
            default -> Sort.unsorted();
        };
    }
}
