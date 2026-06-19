-- ══════════════════════════════════════════════════════════════
--  果蔬批发商贸管理系统 · 简化版初始化脚本（单管理员模式）
--  对应分支：feature/single-admin
-- ══════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS trade_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE trade_db;

-- 按外键依赖顺序逆向删表，保证重复执行不报错
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS return_finance_requests;
DROP TABLE IF EXISTS sales_order_items;
DROP TABLE IF EXISTS sales_orders;
DROP TABLE IF EXISTS purchase_order_items;
DROP TABLE IF EXISTS purchase_orders;
DROP TABLE IF EXISTS inventory_stocktake_logs;
DROP TABLE IF EXISTS inventory_transfer_logs;
DROP TABLE IF EXISTS inventories;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS categories;
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

-- ── 商品分类 ──────────────────────────────────────────────────
CREATE TABLE categories (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    parent_id   BIGINT,
    sort_order  INT DEFAULT 0,
    description VARCHAR(500),
    FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 商品 ──────────────────────────────────────────────────────
CREATE TABLE products (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    product_code  VARCHAR(50)  NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    category_id   BIGINT,
    unit          VARCHAR(20),
    specification VARCHAR(100),
    status        ENUM('ENABLED','DISABLED') DEFAULT 'ENABLED',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id),
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
    remark        TEXT,
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
    remark        TEXT,
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
    remark       VARCHAR(500),
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_product_warehouse (product_id, warehouse_id),
    FOREIGN KEY (product_id)   REFERENCES products(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 库存盘点流水（保留记录用）────────────────────────────────
CREATE TABLE inventory_stocktake_logs (
    id                BIGINT        PRIMARY KEY AUTO_INCREMENT,
    inventory_id      BIGINT        NOT NULL,
    qty_before        DECIMAL(10,3) NOT NULL,
    qty_after         DECIMAL(10,3) NOT NULL,
    diff_qty          DECIMAL(10,3) NOT NULL,
    remark            VARCHAR(500),
    operator_username VARCHAR(64),
    created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (inventory_id) REFERENCES inventories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 库存调拨流水（保留记录用）────────────────────────────────
CREATE TABLE inventory_transfer_logs (
    id                  BIGINT        PRIMARY KEY AUTO_INCREMENT,
    source_inventory_id BIGINT        NOT NULL,
    from_warehouse_id   BIGINT        NOT NULL,
    to_warehouse_id     BIGINT        NOT NULL,
    product_id          BIGINT        NOT NULL,
    quantity            DECIMAL(10,3) NOT NULL,
    remark              VARCHAR(500),
    operator_username   VARCHAR(64),
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (from_warehouse_id) REFERENCES warehouses(id),
    FOREIGN KEY (to_warehouse_id)   REFERENCES warehouses(id),
    FOREIGN KEY (product_id)        REFERENCES products(id)
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
    remark         TEXT,
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
    remark     VARCHAR(200),
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
    remark          TEXT,
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
    remark     VARCHAR(200),
    FOREIGN KEY (order_id)   REFERENCES sales_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 退货申请（单阶段审批）──────────────────────────────────
CREATE TABLE return_finance_requests (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    kind          ENUM('PURCHASE','SALES') NOT NULL,
    order_id      BIGINT        NOT NULL,
    order_no      VARCHAR(50),
    lines_json    TEXT          NOT NULL,
    return_amount DECIMAL(10,2),
    status        ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
    reject_reason VARCHAR(500),
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_kind (order_id, kind),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ══════════════════════════════════════════════════════════════
--  基础数据初始化
-- ══════════════════════════════════════════════════════════════

-- 管理员账号（密码：admin123，BCrypt 强度10）
INSERT INTO users (username, password, real_name, status) VALUES
    ('admin', '$2b$10$AsObdW0PMNvw/1YgXHbe0O8oB6N6oc845rG/i12KKd54V9D8LVlSW', '管理员', 'ENABLED');

-- 仓库
INSERT INTO warehouses (code, name) VALUES
    ('WH001', '主仓库'),
    ('WH002', '冷库');

-- 商品分类
INSERT INTO categories (code, name, sort_order) VALUES
    ('VEGETABLE', '蔬菜', 1),
    ('FRUIT',     '水果', 2),
    ('GRAIN',     '粮油', 3),
    ('SEASONING', '调味品', 4);


-- ══════════════════════════════════════════════════════════════
--  测试数据（可选，正式环境删除此段）
-- ══════════════════════════════════════════════════════════════

-- 商品
INSERT INTO products (product_code, name, category_id, unit, specification, status) VALUES
    ('P001', '西红柿', 1, '斤', '新鲜',   'ENABLED'),
    ('P002', '黄瓜',   1, '斤', '顶花',   'ENABLED'),
    ('P003', '土豆',   1, '斤', '黄心',   'ENABLED'),
    ('P004', '白菜',   1, '斤', '大棵',   'ENABLED'),
    ('P005', '菠菜',   1, '斤', '新鲜',   'ENABLED'),
    ('P006', '苹果',   2, '斤', '红富士', 'ENABLED'),
    ('P007', '香蕉',   2, '斤', '进口',   'ENABLED'),
    ('P008', '橙子',   2, '斤', '赣南',   'ENABLED'),
    ('P009', '葡萄',   2, '斤', '巨峰',   'ENABLED'),
    ('P010', '西瓜',   2, '斤', '无籽',   'ENABLED'),
    ('P011', '大米',   3, '千克','东北',  'ENABLED'),
    ('P012', '花生油', 3, '桶', '5L',    'ENABLED');

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

-- 采购订单
INSERT INTO purchase_orders (order_no, supplier_id, order_date, total_amount, paid_amount, payment_method, payment_status, status) VALUES
    ('PO20240901001', 1, '2024-09-01',  394.00,  394.00, '转账', 'PAID',    'COMPLETED'),
    ('PO20240915001', 2, '2024-09-15', 1320.00,    0.00, '转账', 'UNPAID',  'COMPLETED'),
    ('PO20241001001', 1, '2024-10-01',  680.00,    0.00, '现金', 'UNPAID',  'PENDING'),
    ('PO20241101001', 3, '2024-11-01', 3400.00, 1700.00, '转账', 'PARTIAL', 'COMPLETED');

INSERT INTO purchase_order_items (order_id, product_id, quantity, price, amount) VALUES
    (1, 1, 100, 2.50,  250.00),
    (1, 2,  80, 1.80,  144.00),
    (2, 6, 200, 4.50,  900.00),
    (2, 7, 150, 2.80,  420.00),
    (3, 3, 300, 1.20,  360.00),
    (3, 4, 400, 0.80,  320.00),
    (4,11, 500, 3.20, 1600.00),
    (4,12, 100,18.00, 1800.00);

-- 销售订单
INSERT INTO sales_orders (order_no, customer_id, order_date, total_amount, received_amount, payment_method, payment_status, status) VALUES
    ('SO20240910001', 1, '2024-09-10',  215.00,  215.00, '现金', 'PAID',   'COMPLETED'),
    ('SO20240920001', 2, '2024-09-20',  325.00,    0.00, '转账', 'UNPAID', 'COMPLETED'),
    ('SO20241005001', 3, '2024-10-05',  180.00,    0.00, '现金', 'UNPAID', 'PENDING'),
    ('SO20241020001', 4, '2024-10-20',  400.00,    0.00, '转账', 'UNPAID', 'PENDING');

INSERT INTO sales_order_items (order_id, product_id, quantity, price, amount) VALUES
    (1, 1,  40, 3.50, 140.00),
    (1, 2,  30, 2.50,  75.00),
    (2, 6,  50, 6.50, 325.00),
    (3, 3, 100, 1.80, 180.00),
    (4, 8,  80, 5.00, 400.00);

-- 库存
INSERT INTO inventories (product_id, warehouse_id, quantity) VALUES
    (1,  1,  60),
    (2,  1,  50),
    (3,  1, 300),
    (4,  1, 400),
    (11, 1, 500),
    (12, 1, 100),
    (6,  2, 150),
    (7,  2, 150),
    (8,  2,  80),
    (9,  2,  60),
    (10, 2, 200);
