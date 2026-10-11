CREATE TABLE reservations
(
    id          BIGINT AUTO_INCREMENT,
    user_id     BIGINT         NOT NULL,
    car_id      BIGINT         NOT NULL,
    start_date  DATE           NOT NULL,
    end_date    DATE           NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    status      VARCHAR(30)    NOT NULL,
    created_at  DATETIME(6)    NOT NULL,

    CONSTRAINT pk_reservations
        PRIMARY KEY (id),

    CONSTRAINT fk_reservations_user
        FOREIGN KEY (user_id)
            REFERENCES users (id),

    CONSTRAINT fk_reservations_car
        FOREIGN KEY (car_id)
            REFERENCES cars (id),

    CONSTRAINT chk_reservations_dates
        CHECK (end_date > start_date),

    CONSTRAINT chk_reservations_total_price
        CHECK (total_price > 0)
);

CREATE INDEX idx_reservations_user
    ON reservations (user_id);

CREATE INDEX idx_reservations_car_dates
    ON reservations (car_id, start_date, end_date);