package com.melodyshop.auth.entity;

import com.melodyshop.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "face_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceData extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true, length = 36)
    private String userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Double> embedding;

    @Column(name = "quality_score")
    private Integer qualityScore;

    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] thumbnail;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
