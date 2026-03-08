-- 1. Bảng Categories (Danh mục đa cấp)
CREATE TABLE categories (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            parent_id BIGINT NULL,
                            name VARCHAR(255) NOT NULL,
                            slug VARCHAR(255) NOT NULL UNIQUE,
                            description TEXT,
                            is_active BOOLEAN DEFAULT TRUE,
                            created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                            updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                            deleted_at DATETIME(6) NULL,
                            FOREIGN KEY (parent_id) REFERENCES categories(id)
);
CREATE INDEX idx_category_parent ON categories(parent_id);

-- 2. Bảng Products (Thông tin chung)
CREATE TABLE products (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          category_id BIGINT NOT NULL,
                          name VARCHAR(255) NOT NULL,
                          slug VARCHAR(255) NOT NULL UNIQUE,
                          description TEXT,
                          short_description VARCHAR(500),
                          status ENUM('DRAFT','ACTIVE','ARCHIVED') DEFAULT 'DRAFT',
                          average_rating DOUBLE DEFAULT 0.0,
                          review_count INT DEFAULT 0,
                          created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                          updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                          deleted_at DATETIME(6) NULL,
                          FOREIGN KEY (category_id) REFERENCES categories(id)
);
CREATE INDEX idx_product_category ON products(category_id);

-- 3. Bảng Attributes & Values (Color, Size...)
CREATE TABLE attributes (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE attribute_values (
                                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                  attribute_id BIGINT NOT NULL,
                                  value VARCHAR(100) NOT NULL,
                                  FOREIGN KEY (attribute_id) REFERENCES attributes(id),
                                  UNIQUE(attribute_id, value)
);

-- 4. Bảng Product Variants (Biến thể thực tế)
CREATE TABLE product_variants (
                                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                  product_id BIGINT NOT NULL,
                                  sku VARCHAR(100) NOT NULL UNIQUE,
                                  price DECIMAL(15,2) NOT NULL,
                                  currency VARCHAR(10) DEFAULT 'VND',
                                  stock_quantity INT NOT NULL DEFAULT 0,
                                  version INT DEFAULT 0,
                                  is_active BOOLEAN DEFAULT TRUE,
                                  created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                                  updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- 5. Bảng trung gian Variant - Attribute (Đã loại bỏ attribute_id thừa)
CREATE TABLE variant_attribute_values (
                                          variant_id BIGINT NOT NULL,
                                          attribute_value_id BIGINT NOT NULL,
                                          PRIMARY KEY (variant_id, attribute_value_id),
                                          FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
                                          FOREIGN KEY (attribute_value_id) REFERENCES attribute_values(id) ON DELETE CASCADE
);

-- 6. Bảng Users & Auth
CREATE TABLE users (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       name VARCHAR(255),
                       role ENUM('USER','ADMIN') DEFAULT 'USER',
                       is_active BOOLEAN DEFAULT TRUE,
                       avatar VARCHAR(500),
                       created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                       updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                       deleted_at DATETIME(6) NULL
);

CREATE TABLE user_providers (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                user_id BIGINT NOT NULL,
                                provider ENUM('LOCAL','GOOGLE','FACEBOOK','APPLE') NOT NULL,
                                provider_user_id VARCHAR(255),
                                password_hash VARCHAR(255),
                                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                UNIQUE(provider, provider_user_id)
);
CREATE INDEX idx_user_email ON users(email);

-- 7. Bảng Coupons & Marketing
CREATE TABLE coupons (
                         id BIGINT PRIMARY KEY AUTO_INCREMENT,
                         code VARCHAR(50) NOT NULL UNIQUE,
                         discount_type ENUM('FIXED','PERCENTAGE') NOT NULL,
                         discount_value DECIMAL(15,2) NOT NULL,
                         min_order_value DECIMAL(15,2) DEFAULT 0,
                         max_usage INT,
                         limit_per_user INT DEFAULT 1,
                         used_count INT DEFAULT 0,
                         expiry_date DATETIME(6),
                         is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE banners (
                         id BIGINT PRIMARY KEY AUTO_INCREMENT,
                         title VARCHAR(255),
                         description TEXT,
                         image_url VARCHAR(500) NOT NULL,
                         target_type ENUM('PRODUCT', 'CATEGORY', 'EXTERNAL_LINK') NOT NULL,
                         target_id BIGINT,
                         link_url VARCHAR(500),
                         display_order INT DEFAULT 0,
                         is_active BIT(1) DEFAULT b'1',
                         created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)
);

-- 8. Bảng Orders, Items & Payments
CREATE TABLE orders (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        user_id BIGINT NOT NULL,
                        total_amount DECIMAL(15,2) NOT NULL,
                        shipping_fee DECIMAL(15,2) DEFAULT 0,
                        discount_amount DECIMAL(15,2) DEFAULT 0,
                        coupon_id BIGINT NULL,
                        currency VARCHAR(10) DEFAULT 'VND',
                        status ENUM('PENDING','PAID','SHIPPED','COMPLETED','CANCELLED') DEFAULT 'PENDING',
                        shipping_name VARCHAR(255),
                        shipping_phone VARCHAR(50),
                        shipping_address TEXT,
                        created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                        FOREIGN KEY (user_id) REFERENCES users(id),
                        FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE SET NULL
);

CREATE TABLE order_items (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             order_id BIGINT NOT NULL,
                             product_id BIGINT,
                             variant_id BIGINT,
                             product_name VARCHAR(255) NOT NULL,
                             sku VARCHAR(100),
                             attributes_json JSON,
                             quantity INT NOT NULL,
                             unit_price DECIMAL(15,2) NOT NULL,
                             currency VARCHAR(10),
                             FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE TABLE payments (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          order_id BIGINT NOT NULL,
                          payment_method ENUM('COD', 'VNPAY', 'MOMO', 'STRIPE') NOT NULL,
                          transaction_id VARCHAR(255),
                          amount DECIMAL(15,2) NOT NULL,
                          status ENUM('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED') DEFAULT 'PENDING',
                          created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                          FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE TABLE order_status_history (
                                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                      order_id BIGINT NOT NULL,
                                      status ENUM('PENDING', 'PAID', 'SHIPPED', 'COMPLETED', 'CANCELLED'),
                                      note TEXT,
                                      created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                                      FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- 9. Cart & Wishlist
CREATE TABLE carts (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       user_id BIGINT NOT NULL UNIQUE,
                       updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE cart_items (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            cart_id BIGINT NOT NULL,
                            variant_id BIGINT NOT NULL,
                            quantity INT NOT NULL,
                            UNIQUE(cart_id, variant_id),
                            FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
                            FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);

CREATE TABLE wishlists (
                           user_id BIGINT NOT NULL,
                           product_id BIGINT NOT NULL,
                           created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                           PRIMARY KEY (user_id, product_id),
                           FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                           FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- 10. Images & Reviews
CREATE TABLE product_images (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                product_id BIGINT NOT NULL,
                                variant_id BIGINT NULL,
                                url VARCHAR(500) NOT NULL,
                                is_main BOOLEAN DEFAULT FALSE,
                                display_order INT DEFAULT 0,
                                FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
                                FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE SET NULL
);

CREATE TABLE reviews (
                         id BIGINT PRIMARY KEY AUTO_INCREMENT,
                         user_id BIGINT NOT NULL,
                         product_id BIGINT NOT NULL,
                         rating INT NOT NULL,
                         comment TEXT,
                         created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                         UNIQUE(user_id, product_id),
                         FOREIGN KEY (user_id) REFERENCES users(id),
                         FOREIGN KEY (product_id) REFERENCES products(id)
);

-- 11. Auth Tokens
CREATE TABLE password_reset_tokens (
                                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                       token VARCHAR(255) UNIQUE NOT NULL,
                                       user_id BIGINT NOT NULL,
                                       expiry_date DATETIME(6) NOT NULL,
                                       is_used BIT(1) DEFAULT b'0',
                                       created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                                       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_reset_token ON password_reset_tokens(token);

CREATE TABLE refresh_tokens (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                token VARCHAR(255) UNIQUE NOT NULL,
                                user_id BIGINT NOT NULL,
                                expiry_date DATETIME(6) NOT NULL,
                                revoked BIT(1) DEFAULT b'0',
                                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);