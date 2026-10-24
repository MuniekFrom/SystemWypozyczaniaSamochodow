ALTER TABLE cars
    ADD COLUMN engine_capacity_cm3 INT NULL,
    ADD COLUMN power_hp INT NULL,
    ADD COLUMN drive_type VARCHAR(30) NULL,
    ADD COLUMN acceleration_to_100_seconds DECIMAL(4, 2) NULL;