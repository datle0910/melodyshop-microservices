ALTER TABLE inventory_logs
    ADD UNIQUE KEY uk_inventory_action_reference (inventory_id, action, reference_id);
