ALTER TABLE reservations
DROP CHECK chk_reservations_dates;

ALTER TABLE reservations
    ADD COLUMN start_at DATETIME(6) NULL AFTER car_id,
    ADD COLUMN end_at DATETIME(6) NULL AFTER start_at;

UPDATE reservations
SET start_at = TIMESTAMP(start_date, '10:00:00'),
    end_at = TIMESTAMP(end_date, '10:00:00');

ALTER TABLE reservations
    MODIFY start_at DATETIME(6) NOT NULL,
    MODIFY end_at DATETIME(6) NOT NULL;

CREATE INDEX idx_reservations_car_date_times
    ON reservations (car_id, start_at, end_at);

DROP INDEX idx_reservations_car_dates ON reservations;

ALTER TABLE reservations
DROP COLUMN start_date,
    DROP COLUMN end_date,
    ADD CONSTRAINT chk_reservations_date_times
        CHECK (end_at > start_at);
