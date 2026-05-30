package com.melodyshop.user.repository;

import com.melodyshop.user.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    @Query("SELECT up FROM UserProfile up WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(up.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(up.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<UserProfile> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
