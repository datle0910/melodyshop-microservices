package com.melodyshop.inventory.repository;

import com.melodyshop.inventory.entity.InventoryImportItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryImportItemRepository extends JpaRepository<InventoryImportItem, String> {

    List<InventoryImportItem> findByInventoryImportId(String importId);
}
