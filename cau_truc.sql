-- 1. Bảng Categories (Danh mục)
CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,

    UNIQUE(slug),
    FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE INDEX idx_category_parent ON categories(parent_id);

-- 2. Bảng Products (Thông tin chung)
CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT,
    short_description VARCHAR(500),
    status ENUM('DRAFT','ACTIVE','ARCHIVED') DEFAULT 'DRAFT',

    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,

    UNIQUE(slug),
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX idx_product_category ON products(category_id);

-- 3. Bảng Attributes (Loại thuộc tính: Color, Size...)
CREATE TABLE attributes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    UNIQUE(name)
);

CREATE TABLE attribute_values (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    attribute_id BIGINT NOT NULL,
    value VARCHAR(100) NOT NULL,

    FOREIGN KEY (attribute_id) REFERENCES attributes(id),
    UNIQUE(attribute_id, value)
);

-- 5. Bảng Product Variants (Các phiên bản thực tế của sản phẩm)
CREATE TABLE product_variants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL,
    price DECIMAL(15,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'VND',
    stock_quantity INT NOT NULL DEFAULT 0,
    version INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,

    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    UNIQUE(sku),
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_variant_product ON product_variants(product_id);

-- 6. Bảng trung gian Variant - Attribute (Mối quan hệ n-n)
CREATE TABLE variant_attribute_values (
    variant_id BIGINT NOT NULL,
    attribute_id BIGINT NOT NULL,
    attribute_value_id BIGINT NOT NULL,

    PRIMARY KEY (variant_id, attribute_id),
    FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
    FOREIGN KEY (attribute_id) REFERENCES attributes(id),
    FOREIGN KEY (attribute_value_id) REFERENCES attribute_values(id)
);

-- 7. Bảng Users (Người dùng)
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    role ENUM('USER','ADMIN') DEFAULT 'USER',
    is_active BOOLEAN DEFAULT TRUE,

    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,

    UNIQUE(email)
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

-- Thêm Index để login/tìm kiếm nhanh bằng Email
CREATE INDEX idx_user_email ON users(email);

-- 8. Bảng Orders (Đơn hàng)
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'VND',
    status ENUM('PENDING','PAID','SHIPPED','COMPLETED','CANCELLED') DEFAULT 'PENDING',

    shipping_name VARCHAR(255),
    shipping_phone VARCHAR(50),
    shipping_address TEXT,

    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),

    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_order_user ON orders(user_id);

-- 9. Bảng Order Items (Chi tiết đơn hàng - Quan trọng)
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

-- 10. Bảng Reviews (Đánh giá)
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

-- 11. Bảng Coupons (Mã giảm giá)
CREATE TABLE coupons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    discount_type ENUM('FIXED','PERCENTAGE') NOT NULL,
    discount_value DECIMAL(15,2) NOT NULL,
    min_order_value DECIMAL(15,2) DEFAULT 0,
    max_usage INT,
    used_count INT DEFAULT 0,
    expiry_date DATETIME(6),
    is_active BOOLEAN DEFAULT TRUE,

    UNIQUE(code)
);

-- 12. Bảng Banners (Quảng cáo)
CREATE TABLE banners (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255),
    description TEXT,
    image_url VARCHAR(500) NOT NULL,
    target_type ENUM('PRODUCT', 'CATEGORY', 'EXTERNAL_LINK') NOT NULL,
    target_id BIGINT, -- ID của Product hoặc Category tương ứng
    link_url VARCHAR(500), -- Dùng nếu là EXTERNAL_LINK
    display_order INT DEFAULT 0,
    is_active BIT(1) DEFAULT b'1',
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)
);

-- 13. Bảng Cart (Giỏ hàng)
CREATE TABLE carts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 14. Bảng Cart Items
CREATE TABLE cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cart_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    quantity INT NOT NULL,

    UNIQUE(cart_id, variant_id),
    FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);

-- 15. Bảng Wishlist
CREATE TABLE wishlists (
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, product_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- 16. Bảng hình ảnh chi tiết
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

-- 17. Lịch sử trạng thái đơn hàng
CREATE TABLE order_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    status ENUM('PENDING', 'PROCESSING', 'SHIPPED', 'COMPLETED', 'CANCELLED'),
    note TEXT, -- Lý do hủy hàng hoặc ghi chú giao hàng
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- 18. Bảng Password Reset Tokens (Mã khôi phục mật khẩu)
CREATE TABLE password_reset_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    expiry_date DATETIME(6) NOT NULL,
    is_used BIT(1) DEFAULT b'0',
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index để tìm kiếm token cực nhanh khi người dùng click vào link email
CREATE INDEX idx_reset_token ON password_reset_tokens(token);

-- 19.
CREATE TABLE refresh_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    expiry_date DATETIME(6) NOT NULL,
    revoked BIT(1) DEFAULT b'0', -- Cho phép admin hoặc user vô hiệu hóa token
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 1. Bổ sung thông tin đối soát cho bảng Orders
ALTER TABLE orders
    ADD COLUMN shipping_fee DECIMAL(15,2) DEFAULT 0 AFTER total_amount,
    ADD COLUMN discount_amount DECIMAL(15,2) DEFAULT 0 AFTER shipping_fee,
    ADD COLUMN coupon_id BIGINT NULL AFTER discount_amount,
    ADD FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE SET NULL;

-- 2. Đồng nhất ENUM của order_status_history với orders
ALTER TABLE order_status_history
    MODIFY COLUMN status ENUM('PENDING', 'PAID', 'SHIPPED', 'COMPLETED', 'CANCELLED');

-- 3. Tạo lại bảng Payments (Bắt buộc cho Prod)
CREATE TABLE payments (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          order_id BIGINT NOT NULL,
                          payment_method ENUM('COD', 'VNPAY', 'MOMO', 'STRIPE') NOT NULL,
                          transaction_id VARCHAR(255), -- Mã giao dịch trả về từ cổng thanh toán
                          amount DECIMAL(15,2) NOT NULL,
                          status ENUM('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED') DEFAULT 'PENDING',
                          created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                          FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- 4. Vá lỗ hổng Coupon
ALTER TABLE coupons
    ADD COLUMN limit_per_user INT DEFAULT 1 AFTER max_usage;