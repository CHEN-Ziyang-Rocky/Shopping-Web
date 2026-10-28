-- New databases only; legacy SQLite data is deliberately not imported.
CREATE TABLE categories (catid INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
CREATE TABLE users (userid INT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(100) NOT NULL UNIQUE,
 password VARCHAR(255) NOT NULL, role VARCHAR(255) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
CREATE TABLE products (pid INT AUTO_INCREMENT PRIMARY KEY, catid INT NOT NULL, name VARCHAR(255) NOT NULL,
 price DECIMAL(12,2) NOT NULL, description TEXT, thumbnail_image VARCHAR(255), large_image VARCHAR(255),
 stock INT NOT NULL DEFAULT 0, CONSTRAINT products_stock CHECK(stock>=0), CONSTRAINT products_price CHECK(price>0),
 CONSTRAINT products_category FOREIGN KEY(catid) REFERENCES categories(catid)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
CREATE TABLE vouchers (id INT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(50) COLLATE utf8mb4_0900_as_cs NOT NULL UNIQUE,
 discount DECIMAL(12,2) NOT NULL, quota INT NOT NULL, version INT,
 CONSTRAINT vouchers_quota CHECK(quota>=0), CONSTRAINT vouchers_discount CHECK(discount>=0)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
CREATE TABLE orders (id INT AUTO_INCREMENT PRIMARY KEY, userid INT NOT NULL,
 total_price DECIMAL(12,2), amount_total_minor BIGINT, currency VARCHAR(255), merchant_email VARCHAR(255),
 salt VARCHAR(255), digest VARCHAR(255), status VARCHAR(255), transaction_id VARCHAR(255) COLLATE utf8mb4_0900_as_cs UNIQUE,
 created_at BIGINT, voucher_code VARCHAR(255), discount_amount DECIMAL(12,2),
 INDEX orders_user_created(userid,created_at), CONSTRAINT orders_user FOREIGN KEY(userid) REFERENCES users(userid)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
CREATE TABLE order_items (id INT AUTO_INCREMENT PRIMARY KEY, pid INT NOT NULL, quantity INT NOT NULL,
 price DECIMAL(12,2) NOT NULL, order_id INT, UNIQUE KEY order_product(order_id,pid),
 CONSTRAINT item_quantity CHECK(quantity>0), CONSTRAINT item_order FOREIGN KEY(order_id) REFERENCES orders(id),
 CONSTRAINT item_product FOREIGN KEY(pid) REFERENCES products(pid)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
CREATE TABLE user_sessions (id INT AUTO_INCREMENT PRIMARY KEY, userid INT NOT NULL,
 session_id VARCHAR(255) COLLATE utf8mb4_0900_as_cs NOT NULL UNIQUE, ip_address VARCHAR(255) NOT NULL,
 login_time BIGINT NOT NULL, is_valid BIT NOT NULL,
 CONSTRAINT session_user FOREIGN KEY(userid) REFERENCES users(userid)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
CREATE TABLE payment_events (id VARCHAR(255) COLLATE utf8mb4_0900_as_cs PRIMARY KEY,
 order_id INT NOT NULL, event_type VARCHAR(255) NOT NULL, processed_at BIGINT NOT NULL,
 CONSTRAINT payment_order FOREIGN KEY(order_id) REFERENCES orders(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
