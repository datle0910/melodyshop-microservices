package com.melodyshop.inventory.repository;

import com.melodyshop.inventory.entity.InventoryImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryImportRepository extends JpaRepository<InventoryImport, String> {

    Optional<InventoryImport> findByImportCode(String importCode);

    boolean existsByImportCode(String importCode);
}
