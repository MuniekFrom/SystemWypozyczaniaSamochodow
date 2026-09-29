CREATE TABLE cars
(
    id                  BIGINT AUTO_INCREMENT,
    brand               VARCHAR(50)    NOT NULL,
    model               VARCHAR(50)    NOT NULL,
    production_year     INT            NOT NULL,
    registration_number VARCHAR(20)    NOT NULL,
    vin                 VARCHAR(17)    NOT NULL,
    fuel_type           VARCHAR(30)    NOT NULL,
    transmission_type   VARCHAR(30)    NOT NULL,
    body_type           VARCHAR(30)    NOT NULL,
    number_of_seats     INT            NOT NULL,
    category            VARCHAR(30)    NOT NULL,
    color               VARCHAR(30)    NOT NULL,
    daily_price         DECIMAL(10, 2) NOT NULL,
    status              VARCHAR(30)    NOT NULL,
    description         VARCHAR(1000),

    CONSTRAINT pk_cars PRIMARY KEY (id),
    CONSTRAINT uk_cars_registration_number UNIQUE (registration_number),
    CONSTRAINT uk_cars_vin UNIQUE (vin)
);