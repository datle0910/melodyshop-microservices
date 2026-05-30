package com.melodyshop.auth.repository;

import com.melodyshop.auth.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, String> {

    Optional<VerificationCode> findTopByEmailAndPurposeAndIsUsedFalseOrderByCreatedAtDesc(
            String email, String purpose);

    @Query("SELECT v FROM VerificationCode v WHERE v.email = :email AND v.purpose = :purpose AND v.isUsed = false ORDER BY v.createdAt DESC")
    List<VerificationCode> findAllActiveByEmailAndPurpose(
            @Param("email") String email, @Param("purpose") String purpose);

    @Modifying
    @Query("UPDATE VerificationCode v SET v.isUsed = true WHERE v.email = :email AND v.purpose = :purpose")
    void markAllAsUsed(@Param("email") String email, @Param("purpose") String purpose);

    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpiredCodes();
}
