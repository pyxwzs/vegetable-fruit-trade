-- ══════════════════════════════════════════════════════════════
--  果蔬配送经营管理系统 · 空库初始化（仅表结构 + 平台管理员）
--  用法：mysql -u root -p < database/init.sql
--  需要演示数据请改用：database/init_demo.sql
-- ══════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS trade_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE trade_db;

-- 按外键依赖顺序逆向删表，保证重复执行不报错
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS expenses;
DROP TABLE IF EXISTS supplier_product_metrics;
DROP TABLE IF EXISTS sale_payments;
DROP TABLE IF EXISTS purchase_payments;
DROP TABLE IF EXISTS sales_order_items;
DROP TABLE IF EXISTS sales_orders;
DROP TABLE IF EXISTS purchase_order_items;
DROP TABLE IF EXISTS purchase_orders;
DROP TABLE IF EXISTS inventories;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS suppliers;
DROP TABLE IF EXISTS warehouses;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS admins;
DROP TABLE IF EXISTS site_settings;
DROP TABLE IF EXISTS tenant_invite_codes;
DROP TABLE IF EXISTS tenants;
SET FOREIGN_KEY_CHECKS = 1;

-- ── 租户 ──────────────────────────────────────────────────────
CREATE TABLE tenants (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    status      ENUM('ACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO tenants (id, code, name, status) VALUES
    (1, 'default', '默认租户', 'ACTIVE');

-- ── 平台管理员（仅 Web /platform/login）────────────────────────
CREATE TABLE admins (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    real_name   VARCHAR(50),
    status      ENUM('ENABLED','DISABLED') DEFAULT 'ENABLED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 租户用户（仅微信小程序授权登录）────────────────────────────
CREATE TABLE users (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    tenant_id   BIGINT       NOT NULL DEFAULT 1,
    login_key   VARCHAR(64)  NOT NULL COMMENT 'JWT 内部标识',
    phone       VARCHAR(20)  NOT NULL,
    real_name   VARCHAR(50),
    wx_openid   VARCHAR(64)  NOT NULL,
    wx_nickname VARCHAR(100),
    avatar_url  VARCHAR(500),
    status      ENUM('ENABLED','DISABLED') DEFAULT 'ENABLED',
    menu_keys   TEXT         COMMENT '启用的菜单 key JSON 数组，NULL 表示全部启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_login_key (login_key),
    UNIQUE KEY uk_phone (phone),
    UNIQUE KEY uk_wx_openid (wx_openid),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 站点设置（每租户一行）────────────────────────────────────
CREATE TABLE site_settings (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    tenant_id   BIGINT       NOT NULL,
    site_name   VARCHAR(100) NOT NULL DEFAULT '果蔬批发',
    logo_path   VARCHAR(500),
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_site_tenant (tenant_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO site_settings (tenant_id, site_name) VALUES
    (1, '平台管理');

-- ── 租户邀请码（一次性）────────────────────────────────────────
CREATE TABLE tenant_invite_codes (
    id                 BIGINT       PRIMARY KEY AUTO_INCREMENT,
    code               VARCHAR(32)  NOT NULL UNIQUE,
    status             ENUM('UNUSED','USED') NOT NULL DEFAULT 'UNUSED',
    created_by_admin_id BIGINT,
    used_by_tenant_id  BIGINT,
    remark             VARCHAR(200),
    used_at            DATETIME,
    create_time        DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_invite_status (status),
    INDEX idx_invite_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 仓库 ──────────────────────────────────────────────────────
CREATE TABLE warehouses (
    id        BIGINT       PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT       NOT NULL DEFAULT 1,
    code      VARCHAR(50)  NOT NULL,
    name      VARCHAR(100) NOT NULL,
    status    ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE',
    UNIQUE KEY uk_tenant_warehouse_code (tenant_id, code),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 商品 ──────────────────────────────────────────────────────
CREATE TABLE products (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    tenant_id     BIGINT       NOT NULL DEFAULT 1,
    product_code  VARCHAR(50)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    category      VARCHAR(50),
    unit          VARCHAR(20),
    specification VARCHAR(100),
    status        ENUM('ENABLED','DISABLED') DEFAULT 'ENABLED',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_product_code (tenant_id, product_code),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 供应商 ────────────────────────────────────────────────────
CREATE TABLE suppliers (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    tenant_id     BIGINT        NOT NULL DEFAULT 1,
    supplier_code VARCHAR(50)   NOT NULL,
    name          VARCHAR(100)  NOT NULL,
    contact       VARCHAR(50),
    phone         VARCHAR(20),
    address       VARCHAR(200),
    status        ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_supplier_code (tenant_id, supplier_code),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 供货订量指标（按月向供应商订购某商品的目标量，超量用于后续奖励）──
CREATE TABLE supplier_product_metrics (
    id              BIGINT         PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT         NOT NULL DEFAULT 1,
    supplier_id     BIGINT         NOT NULL,
    product_id      BIGINT         NOT NULL,
    period_type     ENUM('YEAR','MONTH') NOT NULL DEFAULT 'MONTH' COMMENT '年指标/月指标',
    metric_year     INT            NOT NULL COMMENT '指标年份',
    metric_month    INT            NOT NULL DEFAULT 0 COMMENT '月指标 1-12；年指标为 0',
    target_qty      DECIMAL(10,3)  NOT NULL COMMENT '订量指标',
    remark          VARCHAR(500)   DEFAULT NULL COMMENT '备注（可记录奖励说明）',
    status          ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_supplier_product_period (tenant_id, supplier_id, product_id, period_type, metric_year, metric_month),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 客户 ──────────────────────────────────────────────────────
CREATE TABLE customers (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    tenant_id     BIGINT       NOT NULL DEFAULT 1,
    customer_code VARCHAR(50)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    contact       VARCHAR(50),
    phone         VARCHAR(20),
    address       VARCHAR(200),
    status        ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_customer_code (tenant_id, customer_code),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 库存 ──────────────────────────────────────────────────────
CREATE TABLE inventories (
    id           BIGINT        PRIMARY KEY AUTO_INCREMENT,
    tenant_id    BIGINT        NOT NULL DEFAULT 1,
    product_id   BIGINT        NOT NULL,
    warehouse_id BIGINT        NOT NULL,
    quantity     DECIMAL(10,3) DEFAULT 0,
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_inventory (tenant_id, product_id, warehouse_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (product_id)   REFERENCES products(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 采购订单 ──────────────────────────────────────────────────
CREATE TABLE purchase_orders (
    id             BIGINT        PRIMARY KEY AUTO_INCREMENT,
    tenant_id      BIGINT        NOT NULL DEFAULT 1,
    order_no       VARCHAR(50)   NOT NULL,
    supplier_id    BIGINT        NOT NULL,
    order_date     DATE,
    total_amount   DECIMAL(10,2) DEFAULT 0,
    paid_amount    DECIMAL(10,2) DEFAULT 0,
    payment_method VARCHAR(20),
    payment_status ENUM('UNPAID','PARTIAL','PAID') DEFAULT 'UNPAID',
    status         ENUM('PENDING','COMPLETED','CANCELLED') DEFAULT 'PENDING',
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_purchase_order_no (tenant_id, order_no),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    INDEX idx_supplier (supplier_id),
    INDEX idx_status   (status),
    INDEX idx_date     (order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 采购订单明细 ───────────────────────────────────────────────
CREATE TABLE purchase_order_items (
    id         BIGINT        PRIMARY KEY AUTO_INCREMENT,
    tenant_id  BIGINT        NOT NULL DEFAULT 1,
    order_id   BIGINT        NOT NULL,
    product_id BIGINT        NOT NULL,
    quantity   DECIMAL(10,3) NOT NULL,
    price      DECIMAL(10,2) NOT NULL,
    amount     DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (order_id)   REFERENCES purchase_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 销售订单 ──────────────────────────────────────────────────
CREATE TABLE sales_orders (
    id              BIGINT        PRIMARY KEY AUTO_INCREMENT,
    tenant_id       BIGINT        NOT NULL DEFAULT 1,
    order_no        VARCHAR(50)   NOT NULL,
    customer_id     BIGINT        NOT NULL,
    order_date      DATE,
    total_amount    DECIMAL(10,2) DEFAULT 0,
    received_amount DECIMAL(10,2) DEFAULT 0,
    payment_method  VARCHAR(20),
    payment_status  ENUM('UNPAID','PARTIAL','PAID') DEFAULT 'UNPAID',
    status          ENUM('PENDING','COMPLETED','CANCELLED') DEFAULT 'PENDING',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_sales_order_no (tenant_id, order_no),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    INDEX idx_customer (customer_id),
    INDEX idx_status   (status),
    INDEX idx_date     (order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 销售订单明细 ───────────────────────────────────────────────
CREATE TABLE sales_order_items (
    id         BIGINT        PRIMARY KEY AUTO_INCREMENT,
    tenant_id  BIGINT        NOT NULL DEFAULT 1,
    order_id   BIGINT        NOT NULL,
    product_id BIGINT        NOT NULL,
    quantity   DECIMAL(10,3) NOT NULL,
    price      DECIMAL(10,2) NOT NULL,
    amount     DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (order_id)   REFERENCES sales_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 采购付款记录 ───────────────────────────────────────────────
CREATE TABLE purchase_payments (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id      BIGINT NOT NULL DEFAULT 1,
    order_id       BIGINT NOT NULL,
    payment_date   DATE NOT NULL,
    amount         DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50),
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pp_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_pp_order FOREIGN KEY (order_id) REFERENCES purchase_orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 销售收款记录 ───────────────────────────────────────────────
CREATE TABLE sale_payments (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id      BIGINT NOT NULL DEFAULT 1,
    order_id       BIGINT NOT NULL,
    payment_date   DATE NOT NULL,
    amount         DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50),
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sp_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_sp_order FOREIGN KEY (order_id) REFERENCES sales_orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 其他支出记录（油费、工费等）──────────────────────────────
CREATE TABLE expenses (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id    BIGINT NOT NULL DEFAULT 1,
    expense_date DATE NOT NULL,
    category     VARCHAR(50) NOT NULL,
    amount       DECIMAL(10,2) NOT NULL,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ══════════════════════════════════════════════════════════════
--  基础数据初始化
-- ══════════════════════════════════════════════════════════════

-- admin：平台管理员，密码 admin123
INSERT INTO admins (username, password, real_name, status) VALUES
    ('admin', '$2b$10$AsObdW0PMNvw/1YgXHbe0O8oB6N6oc845rG/i12KKd54V9D8LVlSW', '平台管理员', 'ENABLED');
