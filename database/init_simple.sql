-- ══════════════════════════════════════════════════════════════
--  果蔬批发商贸管理系统 · 简化版初始化脚本（单管理员模式）
-- ══════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS trade_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE trade_db;

-- 按外键依赖顺序逆向删表，保证重复执行不报错
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS expenses;
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
SET FOREIGN_KEY_CHECKS = 1;

-- ── 用户表（单管理员）────────────────────────────────────────
CREATE TABLE users (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    phone       VARCHAR(20),
    real_name   VARCHAR(50),
    status      ENUM('ENABLED','DISABLED') DEFAULT 'ENABLED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 仓库 ──────────────────────────────────────────────────────
CREATE TABLE warehouses (
    id     BIGINT       PRIMARY KEY AUTO_INCREMENT,
    code   VARCHAR(50)  NOT NULL UNIQUE,
    name   VARCHAR(100) NOT NULL,
    status ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 商品 ──────────────────────────────────────────────────────
CREATE TABLE products (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    product_code  VARCHAR(50)  NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    category      VARCHAR(50),
    unit          VARCHAR(20),
    specification VARCHAR(100),
    status        ENUM('ENABLED','DISABLED') DEFAULT 'ENABLED',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_code (product_code),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 供应商 ────────────────────────────────────────────────────
CREATE TABLE suppliers (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    supplier_code VARCHAR(50)   NOT NULL UNIQUE,
    name          VARCHAR(100)  NOT NULL,
    contact       VARCHAR(50),
    phone         VARCHAR(20),
    address       VARCHAR(200),
    status        ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_supplier_code (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 客户 ──────────────────────────────────────────────────────
CREATE TABLE customers (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    customer_code VARCHAR(50)  NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    contact       VARCHAR(50),
    phone         VARCHAR(20),
    address       VARCHAR(200),
    status        ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_customer_code (customer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 库存 ──────────────────────────────────────────────────────
CREATE TABLE inventories (
    id           BIGINT        PRIMARY KEY AUTO_INCREMENT,
    product_id   BIGINT        NOT NULL,
    warehouse_id BIGINT        NOT NULL,
    quantity     DECIMAL(10,3) DEFAULT 0,
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_product_warehouse (product_id, warehouse_id),
    FOREIGN KEY (product_id)   REFERENCES products(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 采购订单 ──────────────────────────────────────────────────
CREATE TABLE purchase_orders (
    id             BIGINT        PRIMARY KEY AUTO_INCREMENT,
    order_no       VARCHAR(50)   NOT NULL UNIQUE,
    supplier_id    BIGINT        NOT NULL,
    order_date     DATE,
    total_amount   DECIMAL(10,2) DEFAULT 0,
    paid_amount    DECIMAL(10,2) DEFAULT 0,
    payment_method VARCHAR(20),
    payment_status ENUM('UNPAID','PARTIAL','PAID') DEFAULT 'UNPAID',
    status         ENUM('PENDING','COMPLETED','CANCELLED') DEFAULT 'PENDING',
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    INDEX idx_order_no (order_no),
    INDEX idx_supplier (supplier_id),
    INDEX idx_status   (status),
    INDEX idx_date     (order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 采购订单明细 ───────────────────────────────────────────────
CREATE TABLE purchase_order_items (
    id         BIGINT        PRIMARY KEY AUTO_INCREMENT,
    order_id   BIGINT        NOT NULL,
    product_id BIGINT        NOT NULL,
    quantity   DECIMAL(10,3) NOT NULL,
    price      DECIMAL(10,2) NOT NULL,
    amount     DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id)   REFERENCES purchase_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 销售订单 ──────────────────────────────────────────────────
CREATE TABLE sales_orders (
    id              BIGINT        PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(50)   NOT NULL UNIQUE,
    customer_id     BIGINT        NOT NULL,
    order_date      DATE,
    total_amount    DECIMAL(10,2) DEFAULT 0,
    received_amount DECIMAL(10,2) DEFAULT 0,
    payment_method  VARCHAR(20),
    payment_status  ENUM('UNPAID','PARTIAL','PAID') DEFAULT 'UNPAID',
    status          ENUM('PENDING','COMPLETED','CANCELLED') DEFAULT 'PENDING',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    INDEX idx_order_no (order_no),
    INDEX idx_customer (customer_id),
    INDEX idx_status   (status),
    INDEX idx_date     (order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 销售订单明细 ───────────────────────────────────────────────
CREATE TABLE sales_order_items (
    id         BIGINT        PRIMARY KEY AUTO_INCREMENT,
    order_id   BIGINT        NOT NULL,
    product_id BIGINT        NOT NULL,
    quantity   DECIMAL(10,3) NOT NULL,
    price      DECIMAL(10,2) NOT NULL,
    amount     DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id)   REFERENCES sales_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 采购付款记录 ───────────────────────────────────────────────
CREATE TABLE purchase_payments (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id       BIGINT NOT NULL,
    payment_date   DATE NOT NULL,
    amount         DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50),
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pp_order FOREIGN KEY (order_id) REFERENCES purchase_orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 销售收款记录 ───────────────────────────────────────────────
CREATE TABLE sale_payments (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id       BIGINT NOT NULL,
    payment_date   DATE NOT NULL,
    amount         DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50),
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sp_order FOREIGN KEY (order_id) REFERENCES sales_orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 其他支出记录（油费、工费等）──────────────────────────────
CREATE TABLE expenses (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_date DATE NOT NULL,
    category     VARCHAR(50) NOT NULL,
    amount       DECIMAL(10,2) NOT NULL,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ══════════════════════════════════════════════════════════════
--  基础数据初始化
-- ══════════════════════════════════════════════════════════════

-- 管理员账号（密码：admin123，BCrypt 强度10）
INSERT INTO users (username, password, real_name, status) VALUES
    ('admin', '$2b$10$AsObdW0PMNvw/1YgXHbe0O8oB6N6oc845rG/i12KKd54V9D8LVlSW', '管理员', 'ENABLED');

-- 仓库（单仓库）
INSERT INTO warehouses (code, name) VALUES
    ('WH001', '主仓库');

-- ══════════════════════════════════════════════════════════════
--  测试数据（可选，正式环境删除此段）
-- ══════════════════════════════════════════════════════════════

-- 商品
INSERT INTO products (product_code, name, category, unit, specification, status) VALUES
    ('P001', '西红柿', '蔬菜', '斤', '新鲜',   'ENABLED'),
    ('P002', '黄瓜',   '蔬菜', '斤', '顶花',   'ENABLED'),
    ('P003', '土豆',   '蔬菜', '斤', '黄心',   'ENABLED'),
    ('P004', '白菜',   '蔬菜', '斤', '大棵',   'ENABLED'),
    ('P005', '菠菜',   '蔬菜', '斤', '新鲜',   'ENABLED'),
    ('P006', '苹果',   '水果', '斤', '红富士', 'ENABLED'),
    ('P007', '香蕉',   '水果', '斤', '进口',   'ENABLED'),
    ('P008', '橙子',   '水果', '斤', '赣南',   'ENABLED'),
    ('P009', '葡萄',   '水果', '斤', '巨峰',   'ENABLED'),
    ('P010', '西瓜',   '水果', '斤', '无籽',   'ENABLED'),
    ('P011', '大米',   '粮油', '千克','东北',  'ENABLED'),
    ('P012', '花生油', '粮油', '桶', '5L',    'ENABLED');

-- 供应商
INSERT INTO suppliers (supplier_code, name, contact, phone, address, status) VALUES
    ('S001', '绿色农庄蔬菜供应商', '陈老板', '13800001001', '顺义区李桥镇', 'ACTIVE'),
    ('S002', '百果汇水果批发商',   '王总',   '13800001002', '大兴区瀛海镇', 'ACTIVE'),
    ('S003', '粮油直供商行',       '刘经理', '13800001003', '天津武清区',   'ACTIVE');

-- 客户
INSERT INTO customers (customer_code, name, contact, phone, address, status) VALUES
    ('C001', '好又多生鲜超市',   '张店长', '13900001001', '朝阳区建国路88号', 'ACTIVE'),
    ('C002', '家乐福便民门店',   '李经理', '13900001002', '海淀区中关村',     'ACTIVE'),
    ('C003', '社区生鲜便利店',   '赵老板', '13900001003', '西城区西四北大街', 'ACTIVE'),
    ('C004', '便利连锁配送中心', '孙总监', '13900001004', '通州区梨园镇',     'ACTIVE');

-- 采购订单（2026年）
INSERT INTO purchase_orders (order_no, supplier_id, order_date, total_amount, paid_amount, payment_method, payment_status, status) VALUES
    -- 3月
    ('PO20260301001', 1, '2026-03-01',  394.00,  394.00, '转账', 'PAID',    'COMPLETED'),
    ('PO20260315001', 2, '2026-03-15', 1320.00, 1320.00, '转账', 'PAID',    'COMPLETED'),
    -- 4月
    ('PO20260401001', 3, '2026-04-01', 3400.00, 1700.00, '转账', 'PARTIAL', 'COMPLETED'),
    ('PO20260420001', 1, '2026-04-20',  560.00,    0.00, '现金', 'UNPAID',  'COMPLETED'),
    -- 5月
    ('PO20260505001', 2, '2026-05-05',  900.00,  900.00, '现金', 'PAID',    'COMPLETED'),
    ('PO20260520001', 1, '2026-05-20',  680.00,    0.00, '转账', 'UNPAID',  'COMPLETED'),
    -- 6月（本月）
    ('PO20260601001', 1, '2026-06-01',  310.00,  310.00, '现金', 'PAID',    'COMPLETED'),
    ('PO20260605001', 2, '2026-06-05', 1080.00,    0.00, '转账', 'UNPAID',  'COMPLETED'),
    ('PO20260610001', 3, '2026-06-10', 2200.00,  800.00, '转账', 'PARTIAL', 'COMPLETED'),
    ('PO20260615001', 1, '2026-06-15',  450.00,    0.00, '现金', 'UNPAID',  'COMPLETED'),
    ('PO20260619001', 2, '2026-06-19',  760.00,    0.00, '转账', 'UNPAID',  'PENDING');

INSERT INTO purchase_order_items (order_id, product_id, quantity, price, amount) VALUES
    -- PO20260301001 (id=1)
    (1, 1, 100, 2.50,  250.00),
    (1, 2,  80, 1.80,  144.00),
    -- PO20260315001 (id=2)
    (2, 6, 200, 4.50,  900.00),
    (2, 7, 150, 2.80,  420.00),
    -- PO20260401001 (id=3)
    (3, 11, 500, 3.20, 1600.00),
    (3, 12, 100, 18.00, 1800.00),
    -- PO20260420001 (id=4)
    (4, 3, 300, 1.20,  360.00),
    (4, 4, 250, 0.80,  200.00),
    -- PO20260505001 (id=5)
    (5, 6, 150, 4.50,  675.00),
    (5, 8, 100, 2.25,  225.00),
    -- PO20260520001 (id=6)
    (6, 1, 200, 2.20,  440.00),
    (6, 5, 100, 2.40,  240.00),
    -- PO20260601001 (id=7)
    (7, 1, 100, 1.80,  180.00),
    (7, 2,  80, 1.625, 130.00),
    -- PO20260605001 (id=8)
    (8, 6, 200, 4.20,  840.00),
    (8, 9,  80, 3.00,  240.00),
    -- PO20260610001 (id=9)
    (9, 11, 400, 3.30, 1320.00),
    (9, 12,  50, 17.60, 880.00),
    -- PO20260615001 (id=10)
    (10, 3, 200, 1.50,  300.00),
    (10, 4, 150, 1.00,  150.00),
    -- PO20260619001 (id=11)
    (11, 6, 100, 4.60,  460.00),
    (11, 7, 100, 3.00,  300.00);

-- 销售订单（2026年）
INSERT INTO sales_orders (order_no, customer_id, order_date, total_amount, received_amount, payment_method, payment_status, status) VALUES
    -- 3月
    ('SO20260305001', 1, '2026-03-05',  215.00,  215.00, '现金', 'PAID',    'COMPLETED'),
    ('SO20260318001', 2, '2026-03-18',  650.00,  650.00, '转账', 'PAID',    'COMPLETED'),
    -- 4月
    ('SO20260408001', 3, '2026-04-08',  360.00,    0.00, '现金', 'UNPAID',  'COMPLETED'),
    ('SO20260425001', 4, '2026-04-25',  520.00,  200.00, '转账', 'PARTIAL', 'COMPLETED'),
    -- 5月
    ('SO20260510001', 1, '2026-05-10',  480.00,  480.00, '现金', 'PAID',    'COMPLETED'),
    ('SO20260522001', 2, '2026-05-22',  375.00,    0.00, '转账', 'UNPAID',  'COMPLETED'),
    -- 6月（本月）
    ('SO20260602001', 1, '2026-06-02',  280.00,  280.00, '现金', 'PAID',    'COMPLETED'),
    ('SO20260606001', 3, '2026-06-06',  390.00,    0.00, '转账', 'UNPAID',  'COMPLETED'),
    ('SO20260611001', 2, '2026-06-11',  630.00,  300.00, '转账', 'PARTIAL', 'COMPLETED'),
    ('SO20260616001', 4, '2026-06-16',  210.00,    0.00, '现金', 'UNPAID',  'COMPLETED'),
    ('SO20260619001', 1, '2026-06-19',  560.00,    0.00, '转账', 'UNPAID',  'PENDING');

INSERT INTO sales_order_items (order_id, product_id, quantity, price, amount) VALUES
    -- SO20260305001 (id=1)
    (1, 1,  40, 3.50,  140.00),
    (1, 2,  30, 2.50,   75.00),
    -- SO20260318001 (id=2)
    (2, 6,  80, 6.50,  520.00),
    (2, 8,  40, 3.25,  130.00),
    -- SO20260408001 (id=3)
    (3, 3, 100, 1.80,  180.00),
    (3, 4, 100, 1.80,  180.00),
    -- SO20260425001 (id=4)
    (4, 11, 100, 4.20,  420.00),
    (4, 7,  40, 2.50,  100.00),
    -- SO20260510001 (id=5)
    (5, 6, 100, 3.00,  300.00),  -- corrected: 300+180=480
    (5, 9,  60, 3.00,  180.00),
    -- SO20260522001 (id=6)
    (6, 6,  50, 6.50,  325.00),
    (6, 8,  25, 2.00,   50.00),
    -- SO20260602001 (id=7)
    (7, 1,  60, 3.00,  180.00),
    (7, 2,  40, 2.50,  100.00),
    -- SO20260606001 (id=8)
    (8, 6,  60, 6.50,  390.00),
    -- SO20260611001 (id=9)
    (9, 11, 100, 4.30,  430.00),
    (9, 12,  10, 20.00, 200.00),
    -- SO20260616001 (id=10)
    (10, 3,  70, 2.00,  140.00),
    (10, 5,  70, 1.00,   70.00),
    -- SO20260619001 (id=11)
    (11, 6,  60, 7.00,  420.00),
    (11, 9,  70, 2.00,  140.00);

-- 采购付款记录
INSERT INTO purchase_payments (order_id, payment_date, amount, payment_method) VALUES
    (1, '2026-03-01',  394.00, '转账'),
    (2, '2026-03-15', 1320.00, '转账'),
    (3, '2026-04-10', 1700.00, '转账'),
    (5, '2026-05-05',  900.00, '现金'),
    (7, '2026-06-01',  310.00, '现金'),
    (9, '2026-06-12',  800.00, '转账');

-- 销售收款记录
INSERT INTO sale_payments (order_id, payment_date, amount, payment_method) VALUES
    (1, '2026-03-05',  215.00, '现金'),
    (2, '2026-03-18',  650.00, '转账'),
    (5, '2026-05-10',  480.00, '现金'),
    (4, '2026-04-26',  200.00, '转账'),
    (7, '2026-06-02',  280.00, '现金'),
    (9, '2026-06-12',  300.00, '转账');

-- 支出记录
INSERT INTO expenses (expense_date, category, amount) VALUES
    ('2026-03-10', '油费',  320.00),
    ('2026-04-08', '油费',  280.00),
    ('2026-04-15', '工费',  500.00),
    ('2026-05-12', '油费',  310.00),
    ('2026-06-03', '油费',  290.00),
    ('2026-06-15', '工费',  600.00);

-- 库存（全部在主仓库）
INSERT INTO inventories (product_id, warehouse_id, quantity) VALUES
    (1,  1,  60),
    (2,  1,  50),
    (3,  1, 300),
    (4,  1, 400),
    (5,  1, 100),
    (6,  1, 150),
    (7,  1, 150),
    (8,  1,  80),
    (9,  1,  60),
    (10, 1, 200),
    (11, 1, 500),
    (12, 1,  90);
