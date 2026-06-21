-- ══════════════════════════════════════════════════════════════
--  果蔬配送经营管理系统 · 含演示数据初始化
--  用法：mysql -u root -p < database/init_demo.sql
--  演示业务数据均在 tenant_id=2（演示批发部），平台租户 id=1 无业务数据
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

-- 演示数据已移至 database/demo_seed.sql（可选导入，勿与正式注册混用）

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

-- ══════════════════════════════════════════════════════════════
--  演示租户业务数据（tenant_id = 2）
-- ══════════════════════════════════════════════════════════════

INSERT INTO tenants (id, code, name, status) VALUES
    (2, 'demo', '演示批发部', 'ACTIVE');

INSERT INTO site_settings (tenant_id, site_name) VALUES
    (2, '演示批发部');

INSERT INTO tenant_invite_codes (code, status, created_by_admin_id, remark) VALUES
    ('DEMO2026INV01', 'UNUSED', 1, '演示用邀请码');

INSERT INTO warehouses (tenant_id, code, name) VALUES
    (2, 'WH001', '主仓库');

INSERT INTO products (tenant_id, product_code, name, category, unit, specification, status) VALUES
    (2, 'P001', '西红柿', '蔬菜', '斤', '新鲜',   'ENABLED'),
    (2, 'P002', '黄瓜',   '蔬菜', '斤', '顶花',   'ENABLED'),
    (2, 'P003', '土豆',   '蔬菜', '斤', '黄心',   'ENABLED'),
    (2, 'P004', '白菜',   '蔬菜', '斤', '大棵',   'ENABLED'),
    (2, 'P005', '菠菜',   '蔬菜', '斤', '新鲜',   'ENABLED'),
    (2, 'P006', '苹果',   '水果', '斤', '红富士', 'ENABLED'),
    (2, 'P007', '香蕉',   '水果', '斤', '进口',   'ENABLED'),
    (2, 'P008', '橙子',   '水果', '斤', '赣南',   'ENABLED'),
    (2, 'P009', '葡萄',   '水果', '斤', '巨峰',   'ENABLED'),
    (2, 'P010', '西瓜',   '水果', '斤', '无籽',   'ENABLED'),
    (2, 'P011', '大米',   '粮油', '千克', '东北', 'ENABLED'),
    (2, 'P012', '花生油', '粮油', '桶',   '5L',   'ENABLED');

INSERT INTO suppliers (tenant_id, supplier_code, name, contact, phone, address, status) VALUES
    (2, 'S001', '绿色农庄蔬菜供应商', '陈老板', '13800001001', '顺义区李桥镇', 'ACTIVE'),
    (2, 'S002', '百果汇水果批发商',   '王总',   '13800001002', '大兴区瀛海镇', 'ACTIVE'),
    (2, 'S003', '粮油直供商行',       '刘经理', '13800001003', '天津武清区',   'ACTIVE');

INSERT INTO customers (tenant_id, customer_code, name, contact, phone, address, status) VALUES
    (2, 'C001', '好又多生鲜超市',   '张店长', '13900001001', '朝阳区建国路88号', 'ACTIVE'),
    (2, 'C002', '家乐福便民门店',   '李经理', '13900001002', '海淀区中关村',     'ACTIVE'),
    (2, 'C003', '社区生鲜便利店',   '赵老板', '13900001003', '西城区西四北大街', 'ACTIVE'),
    (2, 'C004', '便利连锁配送中心', '孙总监', '13900001004', '通州区梨园镇',     'ACTIVE');

INSERT INTO supplier_product_metrics (tenant_id, supplier_id, product_id, period_type, metric_year, metric_month, target_qty, remark, status) VALUES
    (2, 1, 1,  'MONTH', 2026, 6, 500.000,  '6月西红柿订量', 'ACTIVE'),
    (2, 1, 2,  'MONTH', 2026, 6, 400.000,  '6月黄瓜订量',   'ACTIVE'),
    (2, 2, 6,  'MONTH', 2026, 6, 800.000,  '6月苹果订量',   'ACTIVE'),
    (2, 3, 11, 'MONTH', 2026, 6, 1000.000, '6月大米订量',   'ACTIVE');

INSERT INTO purchase_orders (tenant_id, order_no, supplier_id, order_date, total_amount, paid_amount, payment_method, payment_status, status) VALUES
    (2, 'PO20260301001', 1, '2026-03-01',  394.00,  394.00, '转账', 'PAID',    'COMPLETED'),
    (2, 'PO20260315001', 2, '2026-03-15', 1320.00, 1320.00, '转账', 'PAID',    'COMPLETED'),
    (2, 'PO20260401001', 3, '2026-04-01', 3400.00, 1700.00, '转账', 'PARTIAL', 'COMPLETED'),
    (2, 'PO20260420001', 1, '2026-04-20',  560.00,    0.00, '现金', 'UNPAID',  'COMPLETED'),
    (2, 'PO20260505001', 2, '2026-05-05',  900.00,  900.00, '现金', 'PAID',    'COMPLETED'),
    (2, 'PO20260520001', 1, '2026-05-20',  680.00,    0.00, '转账', 'UNPAID',  'COMPLETED'),
    (2, 'PO20260601001', 1, '2026-06-01',  310.00,  310.00, '现金', 'PAID',    'COMPLETED'),
    (2, 'PO20260605001', 2, '2026-06-05', 1080.00,    0.00, '转账', 'UNPAID',  'COMPLETED'),
    (2, 'PO20260610001', 3, '2026-06-10', 2200.00,  800.00, '转账', 'PARTIAL', 'COMPLETED'),
    (2, 'PO20260615001', 1, '2026-06-15',  450.00,    0.00, '现金', 'UNPAID',  'COMPLETED'),
    (2, 'PO20260619001', 2, '2026-06-19',  760.00,    0.00, '转账', 'UNPAID',  'PENDING');

INSERT INTO purchase_order_items (tenant_id, order_id, product_id, quantity, price, amount) VALUES
    (2, 1, 1, 100, 2.50,  250.00),
    (2, 1, 2,  80, 1.80,  144.00),
    (2, 2, 6, 200, 4.50,  900.00),
    (2, 2, 7, 150, 2.80,  420.00),
    (2, 3, 11, 500, 3.20, 1600.00),
    (2, 3, 12, 100, 18.00, 1800.00),
    (2, 4, 3, 300, 1.20,  360.00),
    (2, 4, 4, 250, 0.80,  200.00),
    (2, 5, 6, 150, 4.50,  675.00),
    (2, 5, 8, 100, 2.25,  225.00),
    (2, 6, 1, 200, 2.20,  440.00),
    (2, 6, 5, 100, 2.40,  240.00),
    (2, 7, 1, 100, 1.80,  180.00),
    (2, 7, 2,  80, 1.625, 130.00),
    (2, 8, 6, 200, 4.20,  840.00),
    (2, 8, 9,  80, 3.00,  240.00),
    (2, 9, 11, 400, 3.30, 1320.00),
    (2, 9, 12,  50, 17.60, 880.00),
    (2, 10, 3, 200, 1.50,  300.00),
    (2, 10, 4, 150, 1.00,  150.00),
    (2, 11, 6, 100, 4.60,  460.00),
    (2, 11, 7, 100, 3.00,  300.00);

INSERT INTO sales_orders (tenant_id, order_no, customer_id, order_date, total_amount, received_amount, payment_method, payment_status, status) VALUES
    (2, 'SO20260305001', 1, '2026-03-05',  215.00,  215.00, '现金', 'PAID',    'COMPLETED'),
    (2, 'SO20260318001', 2, '2026-03-18',  650.00,  650.00, '转账', 'PAID',    'COMPLETED'),
    (2, 'SO20260408001', 3, '2026-04-08',  360.00,    0.00, '现金', 'UNPAID',  'COMPLETED'),
    (2, 'SO20260425001', 4, '2026-04-25',  520.00,  200.00, '转账', 'PARTIAL', 'COMPLETED'),
    (2, 'SO20260510001', 1, '2026-05-10',  480.00,  480.00, '现金', 'PAID',    'COMPLETED'),
    (2, 'SO20260522001', 2, '2026-05-22',  375.00,    0.00, '转账', 'UNPAID',  'COMPLETED'),
    (2, 'SO20260602001', 1, '2026-06-02',  280.00,  280.00, '现金', 'PAID',    'COMPLETED'),
    (2, 'SO20260606001', 3, '2026-06-06',  390.00,    0.00, '转账', 'UNPAID',  'COMPLETED'),
    (2, 'SO20260611001', 2, '2026-06-11',  630.00,  300.00, '转账', 'PARTIAL', 'COMPLETED'),
    (2, 'SO20260616001', 4, '2026-06-16',  210.00,    0.00, '现金', 'UNPAID',  'COMPLETED'),
    (2, 'SO20260619001', 1, '2026-06-19',  560.00,    0.00, '转账', 'UNPAID',  'PENDING');

INSERT INTO sales_order_items (tenant_id, order_id, product_id, quantity, price, amount) VALUES
    (2, 1, 1,  40, 3.50, 140.00),
    (2, 1, 2,  30, 2.50,  75.00),
    (2, 2, 6,  80, 6.50, 520.00),
    (2, 2, 8,  40, 3.25, 130.00),
    (2, 3, 3, 100, 1.80, 180.00),
    (2, 3, 4, 100, 1.80, 180.00),
    (2, 4, 11, 100, 4.20, 420.00),
    (2, 4, 7,  40, 2.50, 100.00),
    (2, 5, 6, 100, 3.00, 300.00),
    (2, 5, 9,  60, 3.00, 180.00),
    (2, 6, 6,  50, 6.50, 325.00),
    (2, 6, 8,  25, 2.00,  50.00),
    (2, 7, 1,  60, 3.00, 180.00),
    (2, 7, 2,  40, 2.50, 100.00),
    (2, 8, 6,  60, 6.50, 390.00),
    (2, 9, 11, 100, 4.30, 430.00),
    (2, 9, 12,  10, 20.00, 200.00),
    (2, 10, 3,  70, 2.00, 140.00),
    (2, 10, 5,  70, 1.00,  70.00),
    (2, 11, 6,  60, 7.00, 420.00),
    (2, 11, 9,  70, 2.00, 140.00);

INSERT INTO purchase_payments (tenant_id, order_id, payment_date, amount, payment_method) VALUES
    (2, 1, '2026-03-01',  394.00, '转账'),
    (2, 2, '2026-03-15', 1320.00, '转账'),
    (2, 3, '2026-04-10', 1700.00, '转账'),
    (2, 5, '2026-05-05',  900.00, '现金'),
    (2, 7, '2026-06-01',  310.00, '现金'),
    (2, 9, '2026-06-12',  800.00, '转账');

INSERT INTO sale_payments (tenant_id, order_id, payment_date, amount, payment_method) VALUES
    (2, 1, '2026-03-05', 215.00, '现金'),
    (2, 2, '2026-03-18', 650.00, '转账'),
    (2, 5, '2026-05-10', 480.00, '现金'),
    (2, 4, '2026-04-26', 200.00, '转账'),
    (2, 7, '2026-06-02', 280.00, '现金'),
    (2, 9, '2026-06-12', 300.00, '转账');

INSERT INTO expenses (tenant_id, expense_date, category, amount) VALUES
    (2, '2026-03-10', '油费', 320.00),
    (2, '2026-04-08', '油费', 280.00),
    (2, '2026-04-15', '工费', 500.00),
    (2, '2026-05-12', '油费', 310.00),
    (2, '2026-06-03', '油费', 290.00),
    (2, '2026-06-15', '工费', 600.00);

INSERT INTO inventories (tenant_id, product_id, warehouse_id, quantity) VALUES
    (2, 1,  1,  60),
    (2, 2,  1,  50),
    (2, 3,  1, 300),
    (2, 4,  1, 400),
    (2, 5,  1, 100),
    (2, 6,  1, 150),
    (2, 7,  1, 150),
    (2, 8,  1,  80),
    (2, 9,  1,  60),
    (2, 10, 1, 200),
    (2, 11, 1, 500),
    (2, 12, 1,  90);
