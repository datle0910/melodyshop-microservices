package com.melodyshop.auth.repository;

import com.melodyshop.auth.entity.FaceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FaceDataRepository extends JpaRepository<FaceData, String> {

    Optional<FaceData> findByUserId(String userId);

    Optional<FaceData> findByUserIdAndIsActiveTrue(String userId);

    boolean existsByUserId(String userId);

    boolean existsByUserIdAndIsActiveTrue(String userId);

    void deleteByUserId(String userId);
}
