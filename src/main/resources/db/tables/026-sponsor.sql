CREATE TABLE sponsor_orders
(
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    order_id     CHAR(24)    NOT NULL,
    account      VARCHAR(13) NOT NULL,
    amount_cents INT         NOT NULL,
    expected_nx  INT         NOT NULL,
    awarded_nx   INT                  DEFAULT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    note         VARCHAR(255)         DEFAULT NULL,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NULL     DEFAULT NULL,
    updated_by   INT                  DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY order_id (order_id),
    INDEX idx_sponsor_status_created (status, created_at),
    INDEX idx_sponsor_account_status (account, status),
    CONSTRAINT fk_sponsor_account FOREIGN KEY (account) REFERENCES accounts (`name`)
);
