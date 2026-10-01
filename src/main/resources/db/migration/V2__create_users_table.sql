CREATE TABLE users
(
    id                  BIGINT AUTO_INCREMENT,
    first_name          VARCHAR(30) NOT NULL,
    last_name           VARCHAR(50) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    phone_number        VARCHAR(20) NOT NULL,
    role                VARCHAR(255) NOT NULL,
    status              VARCHAR(255) NOT NULL,
    created_at          DATETIME(6) NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);