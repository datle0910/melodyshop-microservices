ALTER TABLE inventory_import_items 
ADD COLUMN import_price DECIMAL(12,2) DEFAULT NULL;

UPDATE inventory 
SET quantity = 20
WHERE quantity < 20;


