CREATE TABLE IF NOT EXISTS products (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL,
    brewery         VARCHAR(100),
    price           DECIMAL(10, 2)  NOT NULL,
    volume          INT,
    alcohol_content DECIMAL(3, 1),
    description     TEXT,
    image_path      VARCHAR(255),
    stock_quantity  INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number     VARCHAR(20)     UNIQUE NOT NULL,
    customer_name    VARCHAR(100)    NOT NULL,
    customer_email   VARCHAR(255)    NOT NULL,
    customer_phone   VARCHAR(20),
    delivery_address TEXT,
    total_amount     DECIMAL(10, 2)  NOT NULL,
    status           VARCHAR(20)     NOT NULL,
    created_at       TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT          NOT NULL,
    product_id  BIGINT          NOT NULL,
    quantity    INT             NOT NULL,
    unit_price  DECIMAL(10, 2)  NOT NULL,
    CONSTRAINT fk_oi_order   FOREIGN KEY (order_id)   REFERENCES orders(id),
    CONSTRAINT fk_oi_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS admins (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50)  UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);
