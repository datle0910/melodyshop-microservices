package com.melodyshop.user.repository;

import com.melodyshop.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, String> {
    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(String userId);
    Optional<Address> findByIdAndUserId(String id, String userId);
    Optional<Address> findByUserIdAndIsDefaultTrue(String userId);
    long countByUserId(String userId);
}
