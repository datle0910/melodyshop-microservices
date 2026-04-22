package com.melodyshop.search.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Elasticsearch document cho Product.
 * Dữ liệu được đồng bộ từ Product Service.
 */
@Document(indexName = "products")
@Setting(settingPath = "elasticsearch/product-settings.json")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductDocument {

    @Id
    private String id;

    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "vi_analyzer"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword),
            @InnerField(suffix = "autocomplete", type = FieldType.Text, analyzer = "autocomplete_analyzer")
        }
    )
    private String name;

    @Field(type = FieldType.Keyword)
    private String slug;

    @Field(type = FieldType.Text, analyzer = "vi_analyzer")
    private String description;

    @Field(type = FieldType.Text, analyzer = "vi_analyzer")
    private String shortDesc;

    @Field(type = FieldType.Double)
    private BigDecimal basePrice;

    @Field(type = FieldType.Keyword)
    private String categoryId;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Keyword)
    private String brandId;

    @Field(type = FieldType.Keyword)
    private String brandName;

    @Field(type = FieldType.Boolean)
    private Boolean isFeatured;

    @Field(type = FieldType.Boolean)
    private Boolean isActive;

    @Field(type = FieldType.Double)
    private BigDecimal avgRating;

    @Field(type = FieldType.Integer)
    private Integer reviewCount;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createdAt;
}
