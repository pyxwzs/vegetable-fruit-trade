-- 创建数据库
CREATE DATABASE IF NOT EXISTS trade_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE trade_db;
-- 用户表
CREATE TABLE users (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       password VARCHAR(100) NOT NULL,
                       email VARCHAR(100) UNIQUE,
                       phone VARCHAR(20),
                       real_name VARCHAR(50),
                       mfa_login_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '账号是否开启登录邮箱二次验证',
                       login_alert_email_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否接收登录提醒邮件',
                       status ENUM('ENABLED', 'DISABLED', 'LOCKED') DEFAULT 'ENABLED',
                       create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                       update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       INDEX idx_username (username),
                       INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色表
CREATE TABLE roles (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       name VARCHAR(50) NOT NULL UNIQUE,
                       description VARCHAR(200),
                       create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 权限表
CREATE TABLE permissions (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             code VARCHAR(100) NOT NULL UNIQUE,
                             name VARCHAR(100),
                             description VARCHAR(200),
                             module VARCHAR(50),
                             action VARCHAR(50),
                             create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户角色关联表
CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id BIGINT NOT NULL,
                            PRIMARY KEY (user_id, role_id),
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色权限关联表
CREATE TABLE role_permissions (
                                  role_id BIGINT NOT NULL,
                                  permission_id BIGINT NOT NULL,
                                  PRIMARY KEY (role_id, permission_id),
                                  FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
                                  FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品分类表
CREATE TABLE categories (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            code VARCHAR(50) NOT NULL UNIQUE,
                            name VARCHAR(100) NOT NULL,
                            parent_id BIGINT,
                            sort_order INT DEFAULT 0,
                            icon VARCHAR(200),
                            description VARCHAR(500),
                            FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品表
CREATE TABLE products (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          product_code VARCHAR(50) NOT NULL UNIQUE,
                          barcode VARCHAR(64) UNIQUE COMMENT '商超条码，可空',
                          name VARCHAR(100) NOT NULL,
                          category_id BIGINT,
                          unit VARCHAR(20),
                          specification VARCHAR(100),
                          purchase_price DECIMAL(10,2),
                          sale_price DECIMAL(10,2),
                          shelf_life INT,
                          image_url VARCHAR(500),
                          description TEXT,
                          status ENUM('ENABLED', 'DISABLED') DEFAULT 'ENABLED',
                          create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                          update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          FOREIGN KEY (category_id) REFERENCES categories(id),
                          INDEX idx_product_code (product_code),
                          INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品价格变动历史（依赖 products）
CREATE TABLE product_price_history (
                                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                       product_id BIGINT NOT NULL,
                                       prev_purchase_price DECIMAL(10,2),
                                       prev_sale_price DECIMAL(10,2),
                                       new_purchase_price DECIMAL(10,2),
                                       new_sale_price DECIMAL(10,2),
                                       source VARCHAR(20) NOT NULL COMMENT 'MANUAL手工 IMPORT导入',
                                       operator_username VARCHAR(64),
                                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                       FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
                                       INDEX idx_product_time (product_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 仓库表
CREATE TABLE warehouses (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            code VARCHAR(50) NOT NULL UNIQUE,
                            name VARCHAR(100) NOT NULL,
                            address VARCHAR(200),
                            manager VARCHAR(50),
                            phone VARCHAR(20),
                            area DECIMAL(10,2),
                            type ENUM('NORMAL', 'COLD', 'FREEZE') DEFAULT 'NORMAL',
                            status ENUM('ACTIVE', 'INACTIVE', 'MAINTENANCE') DEFAULT 'ACTIVE',
                            create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 库存表
CREATE TABLE inventories (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             product_id BIGINT NOT NULL,
                             warehouse_id BIGINT NOT NULL,
                             batch_no VARCHAR(50),
                             quantity DECIMAL(10,3) DEFAULT 0,
                             available_quantity DECIMAL(10,3) DEFAULT 0,
                             frozen_quantity DECIMAL(10,3) DEFAULT 0,
                             production_date DATE,
                             expiry_date DATE,
                             purchase_price DECIMAL(10,2),
                             location VARCHAR(50),
                             status ENUM('NORMAL', 'EXPIRING', 'EXPIRED', 'FROZEN') DEFAULT 'NORMAL',
                             create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                             update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             FOREIGN KEY (product_id) REFERENCES products(id),
                             FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
                             INDEX idx_product (product_id),
                             INDEX idx_expiry (expiry_date),
                             INDEX idx_batch (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 库存盘点流水（盘盈盘亏记录）
CREATE TABLE inventory_stocktake_logs (
                                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                          inventory_id BIGINT NOT NULL,
                                          qty_before DECIMAL(10,3) NOT NULL,
                                          qty_after DECIMAL(10,3) NOT NULL,
                                          diff_qty DECIMAL(10,3) NOT NULL,
                                          remark VARCHAR(500),
                                          operator_username VARCHAR(64),
                                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                          FOREIGN KEY (inventory_id) REFERENCES inventories(id),
                                          INDEX idx_inv_time (inventory_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 库存调拨流水（仓库间调拨）
CREATE TABLE inventory_transfer_logs (
                                         id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                         source_inventory_id BIGINT NOT NULL COMMENT '源库存行快照ID',
                                         from_warehouse_id BIGINT NOT NULL,
                                         to_warehouse_id BIGINT NOT NULL,
                                         product_id BIGINT NOT NULL,
                                         batch_no VARCHAR(50),
                                         quantity DECIMAL(10,3) NOT NULL,
                                         remark VARCHAR(500),
                                         operator_username VARCHAR(64),
                                         created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                         FOREIGN KEY (from_warehouse_id) REFERENCES warehouses(id),
                                         FOREIGN KEY (to_warehouse_id) REFERENCES warehouses(id),
                                         FOREIGN KEY (product_id) REFERENCES products(id),
                                         INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 供应商表
CREATE TABLE suppliers (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           supplier_code VARCHAR(50) NOT NULL UNIQUE,
                           name VARCHAR(100) NOT NULL,
                           contact VARCHAR(50),
                           phone VARCHAR(20),
                           email VARCHAR(100),
                           address VARCHAR(200),
                           tax_number VARCHAR(50),
                           bank_name VARCHAR(100),
                           bank_account VARCHAR(50),
                           credit_rating DECIMAL(5,2),
                           delivery_on_time_rate INT,
                           quality_pass_rate DECIMAL(5,2),
                           remark TEXT,
                           status ENUM('ACTIVE', 'INACTIVE', 'BLACKLISTED') DEFAULT 'ACTIVE',
                           create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                           update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           INDEX idx_supplier_code (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 客户表
CREATE TABLE customers (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           customer_code VARCHAR(50) NOT NULL UNIQUE,
                           name VARCHAR(100) NOT NULL,
                           contact VARCHAR(50),
                           phone VARCHAR(20),
                           email VARCHAR(100),
                           address VARCHAR(200),
                           tax_number VARCHAR(50),
                           type ENUM('WHOLESALE', 'RETAIL', 'CHAIN', 'OTHER') DEFAULT 'RETAIL',
                           credit_level ENUM('A', 'B', 'C', 'D') DEFAULT 'B',
                           credit_limit DECIMAL(10,2) DEFAULT 0,
                           total_purchase_amount DECIMAL(10,2) DEFAULT 0,
                           purchase_count INT DEFAULT 0,
                           remark TEXT,
                           status ENUM('ACTIVE', 'INACTIVE', 'FROZEN') DEFAULT 'ACTIVE',
                           create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                           update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           INDEX idx_customer_code (customer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 采购订单表
CREATE TABLE purchase_orders (
                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 order_no VARCHAR(50) NOT NULL UNIQUE,
                                 supplier_id BIGINT NOT NULL,
                                 purchaser_id BIGINT,
                                 order_date DATE,
                                 expected_delivery_date DATE,
                                 delivery_date DATE,
                                 total_amount DECIMAL(10,2) DEFAULT 0,
                                 paid_amount DECIMAL(10,2) DEFAULT 0,
                                 discount_amount DECIMAL(10,2) DEFAULT 0,
                                 payment_method VARCHAR(20),
                                 payment_status ENUM('UNPAID', 'PARTIAL', 'PAID') DEFAULT 'UNPAID',
                                 status ENUM('PENDING', 'APPROVED', 'SHIPPED', 'RECEIVED', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
                                 remark TEXT,
                                 create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
                                 FOREIGN KEY (purchaser_id) REFERENCES users(id) ON DELETE SET NULL,
                                 INDEX idx_order_no (order_no),
                                 INDEX idx_supplier (supplier_id),
                                 INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 采购订单明细表
CREATE TABLE purchase_order_items (
                                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                      order_id BIGINT NOT NULL,
                                      product_id BIGINT NOT NULL,
                                      quantity DECIMAL(10,3) NOT NULL,
                                      returned_quantity DECIMAL(10,3) DEFAULT 0 COMMENT '已退货数量',
                                      price DECIMAL(10,2) NOT NULL,
                                      amount DECIMAL(10,2) NOT NULL,
                                      remark VARCHAR(200),
                                      FOREIGN KEY (order_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
                                      FOREIGN KEY (product_id) REFERENCES products(id),
                                      INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 销售订单表
CREATE TABLE sales_orders (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              order_no VARCHAR(50) NOT NULL UNIQUE,
                              customer_id BIGINT NOT NULL,
                              salesman_id BIGINT,
                              order_date DATE,
                              delivery_date DATE,
                              total_amount DECIMAL(10,2) DEFAULT 0,
                              received_amount DECIMAL(10,2) DEFAULT 0,
                              discount_amount DECIMAL(10,2) DEFAULT 0,
                              payment_method VARCHAR(20),
                              payment_status ENUM('UNPAID', 'PARTIAL', 'PAID') DEFAULT 'UNPAID',
                              status ENUM('PENDING', 'APPROVED', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
                              remark TEXT,
                              create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                              update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              FOREIGN KEY (customer_id) REFERENCES customers(id),
                              FOREIGN KEY (salesman_id) REFERENCES users(id) ON DELETE SET NULL,
                              INDEX idx_order_no (order_no),
                              INDEX idx_customer (customer_id),
                              INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 销售订单明细表
CREATE TABLE sales_order_items (
                                   id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                   order_id BIGINT NOT NULL,
                                   product_id BIGINT NOT NULL,
                                   quantity DECIMAL(10,3) NOT NULL,
                                   returned_quantity DECIMAL(10,3) DEFAULT 0 COMMENT '累计退货数量',
                                   price DECIMAL(10,2) NOT NULL,
                                   amount DECIMAL(10,2) NOT NULL,
                                   remark VARCHAR(200),
                                   FOREIGN KEY (order_id) REFERENCES sales_orders(id) ON DELETE CASCADE,
                                   FOREIGN KEY (product_id) REFERENCES products(id),
                                   INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 公告表
CREATE TABLE announcements (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               title VARCHAR(200) NOT NULL,
                               content TEXT,
                               publisher_id BIGINT,
                               priority ENUM('LOW', 'NORMAL', 'HIGH', 'URGENT') DEFAULT 'NORMAL',
                               is_top BOOLEAN DEFAULT FALSE,
                               publish_time DATETIME,
                               expire_time DATETIME,
                               is_timed BOOLEAN DEFAULT FALSE,
                               attachments TEXT,
                               status ENUM('DRAFT', 'PUBLISHED', 'EXPIRED') DEFAULT 'DRAFT',
                               create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                               update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               FOREIGN KEY (publisher_id) REFERENCES users(id) ON DELETE SET NULL,
                               INDEX idx_publish_time (publish_time),
                               INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 公告目标角色表
CREATE TABLE announcement_target_roles (
                                           announcement_id BIGINT NOT NULL,
                                           role_name VARCHAR(50) NOT NULL,
                                           PRIMARY KEY (announcement_id, role_name),
                                           FOREIGN KEY (announcement_id) REFERENCES announcements(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户通知表
CREATE TABLE user_notifications (
                                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                    user_id BIGINT NOT NULL,
                                    announcement_id BIGINT,
                                    title VARCHAR(200),
                                    content TEXT,
                                    type ENUM('ANNOUNCEMENT', 'SYSTEM', 'ALERT', 'TASK') DEFAULT 'SYSTEM',
                                    is_read BOOLEAN DEFAULT FALSE,
                                    read_time DATETIME,
                                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                    FOREIGN KEY (announcement_id) REFERENCES announcements(id) ON DELETE CASCADE,
                                    INDEX idx_user (user_id),
                                    INDEX idx_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 操作日志表
CREATE TABLE operation_logs (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                username VARCHAR(50),
                                ip VARCHAR(50),
                                module VARCHAR(50),
                                action VARCHAR(100),
                                parameters TEXT,
                                result TEXT,
                                execution_time BIGINT,
                                success BOOLEAN,
                                error_message TEXT,
                                user_agent VARCHAR(500),
                                request_url VARCHAR(500),
                                method VARCHAR(10),
                                create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                INDEX idx_username (username),
                                INDEX idx_module (module),
                                INDEX idx_create_time (create_time),
                                INDEX idx_success (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 文件元数据表
CREATE TABLE file_metadata (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               file_id VARCHAR(50) NOT NULL UNIQUE,
                               file_name VARCHAR(255) NOT NULL,
                               file_type VARCHAR(50),
                               file_size BIGINT,
                               file_path VARCHAR(500),
                               url VARCHAR(500),
                               md5 VARCHAR(32),
                               uploader_id BIGINT,
                               business_type VARCHAR(50),
                               business_id BIGINT,
                               category VARCHAR(50),
                               version INT DEFAULT 1,
                               is_latest BOOLEAN DEFAULT TRUE,
                               parent_file_id BIGINT,
                               description TEXT,
                               status ENUM('ACTIVE', 'DELETED', 'EXPIRED') DEFAULT 'ACTIVE',
                               create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                               update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE SET NULL,
                               INDEX idx_file_id (file_id),
                               INDEX idx_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 管理员「数据视角」代查对象（仅存服务端，不写入 JWT）
CREATE TABLE admin_data_scope (
                                  admin_user_id BIGINT PRIMARY KEY,
                                  target_user_id BIGINT NOT NULL,
                                  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  FOREIGN KEY (admin_user_id) REFERENCES users (id) ON DELETE CASCADE,
                                  FOREIGN KEY (target_user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化数据
INSERT INTO roles (name, description) VALUES
                                          ('ADMIN', '系统管理员'),
                                          ('PURCHASER', '采购员'),
                                          ('WAREHOUSE_KEEPER', '仓管员'),
                                          ('SALESMAN', '销售员'),
                                          ('FINANCE', '财务员');

INSERT INTO permissions (code, name, module, action) VALUES
                                                         ('product:view', '查看商品', 'product', 'view'),
                                                         ('product:create', '创建商品', 'product', 'create'),
                                                         ('product:update', '更新商品', 'product', 'update'),
                                                         ('product:delete', '删除商品', 'product', 'delete'),
                                                         ('inventory:view', '查看库存', 'inventory', 'view'),
                                                         ('inventory:inbound', '入库', 'inventory', 'inbound'),
                                                         ('inventory:outbound', '出库', 'inventory', 'outbound'),
                                                         ('purchase:view', '查看采购', 'purchase', 'view'),
                                                         ('purchase:create', '创建采购', 'purchase', 'create'),
                                                         ('purchase:approve', '审核采购', 'purchase', 'approve'),
                                                         ('sales:view', '查看销售', 'sales', 'view'),
                                                         ('sales:create', '创建销售', 'sales', 'create'),
                                                         ('sales:approve', '审核销售', 'sales', 'approve'),
                                                         ('user:manage', '用户管理', 'user', 'manage'),
                                                         ('announcement:manage', '公告管理', 'announcement', 'manage'),
                                                         ('log:view', '查看日志', 'log', 'view'),
                                                         ('file:manage', '文件管理', 'file', 'manage'),
                                                         ('purchase:pay', '采购付款', 'purchase', 'pay'),
                                                         ('sales:collect', '销售收款', 'sales', 'collect');

-- 为管理员角色分配所有权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions;

-- 财务员：查看采购/销售并登记收付款
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'FINANCE' AND p.code IN (
    'purchase:view', 'purchase:pay', 'sales:view', 'sales:collect'
);

-- 采购员：查看/创建/审核采购（含提交采购退货，与 purchase:create、purchase:approve 一致）
--         + 商品查看（选品）、库存查看（查看库存水位）
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'PURCHASER' AND p.code IN (
    'purchase:view', 'purchase:create', 'purchase:approve',
    'product:view', 'inventory:view'
);

-- 销售员：查看/创建/审核销售（含提交销售退货，与 sales:create、sales:approve 一致）
--         + 商品查看（选品）、库存查看（查看可售库存）
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'SALESMAN' AND p.code IN (
    'sales:view', 'sales:create', 'sales:approve',
    'product:view', 'inventory:view'
);

-- 仓管员：库存直接操作（入库/出库/盘库/调拨）及商品查阅、采购/销售订单查看（需确认入库/出库）
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'WAREHOUSE_KEEPER' AND p.code IN (
    'inventory:view', 'inventory:inbound', 'inventory:outbound',
    'product:view', 'product:create', 'product:update',
    'purchase:view', 'sales:view'
);

-- 创建默认管理员账户（密码：admin123；以下为 BCrypt 强度10 摘要，与后端 BCryptPasswordEncoder 一致）
INSERT INTO users (username, password, email, real_name, status) VALUES
    ('admin', '$2b$10$AsObdW0PMNvw/1YgXHbe0O8oB6N6oc845rG/i12KKd54V9D8LVlSW', 'admin@example.com', '系统管理员', 'ENABLED');

-- 为管理员分配角色
INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);

-- 创建默认仓库
INSERT INTO warehouses (code, name, address, type) VALUES
                                                       ('WH001', '主仓库', '北京市朝阳区', 'NORMAL'),
                                                       ('WH002', '冷库', '北京市大兴区', 'COLD');

-- 创建商品分类
INSERT INTO categories (code, name, sort_order) VALUES
    ('VEGETABLE', '蔬菜', 1),
    ('FRUIT',     '水果', 2),
    ('GRAIN',     '粮油', 3),
    ('SEASONING', '调味品', 4);

-- ══════════════════════════════════════════════════════
--  测试数据
-- ══════════════════════════════════════════════════════

-- ── 测试账号（密码均为 admin123）──────────────────────
INSERT INTO users (username, password, email, real_name, status) VALUES
    ('zhangsan', '$2b$10$AsObdW0PMNvw/1YgXHbe0O8oB6N6oc845rG/i12KKd54V9D8LVlSW', 'zhangsan@example.com', '张三',   'ENABLED'),
    ('lisi',     '$2b$10$AsObdW0PMNvw/1YgXHbe0O8oB6N6oc845rG/i12KKd54V9D8LVlSW', 'lisi@example.com',     '李四',   'ENABLED'),
    ('wangwu',   '$2b$10$AsObdW0PMNvw/1YgXHbe0O8oB6N6oc845rG/i12KKd54V9D8LVlSW', 'wangwu@example.com',   '王五',   'ENABLED'),
    ('zhaoliu',  '$2b$10$AsObdW0PMNvw/1YgXHbe0O8oB6N6oc845rG/i12KKd54V9D8LVlSW', 'zhaoliu@example.com',  '赵六',   'ENABLED'),
    ('sunqi',    '$2b$10$AsObdW0PMNvw/1YgXHbe0O8oB6N6oc845rG/i12KKd54V9D8LVlSW', 'sunqi@example.com',    '孙七',   'ENABLED');
-- zhangsan(2)=采购员  lisi(3)=销售员  wangwu(4)=仓管员  zhaoliu(5)=财务员  sunqi(6)=销售员

INSERT INTO user_roles (user_id, role_id) VALUES
    (2, 2),  -- zhangsan → PURCHASER
    (3, 4),  -- lisi     → SALESMAN
    (4, 3),  -- wangwu   → WAREHOUSE_KEEPER
    (5, 5),  -- zhaoliu  → FINANCE
    (6, 4);  -- sunqi    → SALESMAN

-- ── 商品（12 条）──────────────────────────────────────
-- category: 1=蔬菜 2=水果 3=粮油
INSERT INTO products (product_code, name, category_id, unit, specification, purchase_price, sale_price, shelf_life, status) VALUES
    ('P001', '西红柿', 1, '斤', '新鲜',   2.50,  3.80,   7, 'ENABLED'),
    ('P002', '黄瓜',   1, '斤', '顶花',   1.80,  2.60,   5, 'ENABLED'),
    ('P003', '土豆',   1, '斤', '黄心',   1.20,  1.90,  30, 'ENABLED'),
    ('P004', '白菜',   1, '斤', '大棵',   0.80,  1.30,  14, 'ENABLED'),
    ('P005', '菠菜',   1, '斤', '新鲜',   3.00,  4.50,   5, 'ENABLED'),
    ('P006', '苹果',   2, '斤', '红富士', 4.50,  6.50,  30, 'ENABLED'),
    ('P007', '香蕉',   2, '斤', '进口',   2.80,  4.20,   7, 'ENABLED'),
    ('P008', '橙子',   2, '斤', '赣南',   3.50,  5.20,  14, 'ENABLED'),
    ('P009', '葡萄',   2, '斤', '巨峰',   6.00,  9.00,   7, 'ENABLED'),
    ('P010', '西瓜',   2, '斤', '无籽',   1.50,  2.50,  10, 'ENABLED'),
    ('P011', '大米',   3, '千克','东北',  3.20,  4.80, 365, 'ENABLED'),
    ('P012', '花生油', 3, '桶', '5L',    58.00, 79.00, 540, 'ENABLED');

-- ── 供应商（3 家）──────────────────────────────────────
INSERT INTO suppliers (supplier_code, name, contact, phone, address, credit_rating, delivery_on_time_rate, quality_pass_rate, status) VALUES
    ('S001', '绿色农庄蔬菜供应商', '陈老板', '13800001001', '北京市顺义区李桥镇',    4.5, 95, 98.0, 'ACTIVE'),
    ('S002', '百果汇水果批发商',   '王总',   '13800001002', '北京市大兴区瀛海镇',    4.2, 90, 96.5, 'ACTIVE'),
    ('S003', '粮油直供商行',       '刘经理', '13800001003', '天津市武清区粮油批发市场', 4.8, 98, 99.0, 'ACTIVE');

-- ── 客户（4 家）──────────────────────────────────────
INSERT INTO customers (customer_code, name, contact, phone, address, type, credit_level, credit_limit, status) VALUES
    ('C001', '好又多生鲜超市',   '张店长', '13900001001', '北京市朝阳区建国路88号',  'WHOLESALE', 'A', 50000.00, 'ACTIVE'),
    ('C002', '家乐福便民门店',   '李经理', '13900001002', '北京市海淀区中关村大街',  'CHAIN',     'A', 80000.00, 'ACTIVE'),
    ('C003', '社区生鲜便利店',   '赵老板', '13900001003', '北京市西城区西四北大街',  'RETAIL',    'B',  8000.00, 'ACTIVE'),
    ('C004', '便利连锁配送中心', '孙总监', '13900001004', '北京市通州区梨园镇',      'CHAIN',     'B', 30000.00, 'ACTIVE');

-- ══════════════════════════════════════════════════════
--  采购订单（6 张，覆盖各状态）
-- ══════════════════════════════════════════════════════
INSERT INTO purchase_orders
    (order_no,         supplier_id, purchaser_id, order_date,   expected_delivery_date, delivery_date, total_amount, paid_amount, payment_method, payment_status, status)
VALUES
    ('PO20240901001',  1,           2,            '2024-09-01', '2024-09-04',           '2024-09-03',   394.00,  394.00, '转账', 'PAID',    'COMPLETED'),
    ('PO20240915001',  2,           2,            '2024-09-15', '2024-09-18',           '2024-09-18',  1320.00,    0.00, '转账', 'UNPAID',  'RECEIVED'),
    ('PO20241001001',  1,           2,            '2024-10-01', '2024-10-05',           NULL,           680.00,    0.00, '现金', 'UNPAID',  'APPROVED'),
    ('PO20241015001',  2,           2,            '2024-10-15', '2024-10-20',           NULL,          1300.00,    0.00, '转账', 'UNPAID',  'PENDING'),
    ('PO20241101001',  3,           2,            '2024-11-01', '2024-11-04',           '2024-11-04',  3400.00, 1700.00, '转账', 'PARTIAL', 'RECEIVED'),
    ('PO20241115001',  1,           2,            '2024-11-15', '2024-11-18',           NULL,          1350.00,    0.00, '现金', 'UNPAID',  'SHIPPED');

-- 采购订单明细
INSERT INTO purchase_order_items (order_id, product_id, quantity, price, amount) VALUES
    (1, 1,  100, 2.50,  250.00),  -- PO1: 西红柿
    (1, 2,   80, 1.80,  144.00),  -- PO1: 黄瓜
    (2, 6,  200, 4.50,  900.00),  -- PO2: 苹果
    (2, 7,  150, 2.80,  420.00),  -- PO2: 香蕉
    (3, 3,  300, 1.20,  360.00),  -- PO3: 土豆
    (3, 4,  400, 0.80,  320.00),  -- PO3: 白菜
    (4, 9,  100, 6.00,  600.00),  -- PO4: 葡萄
    (4, 8,  200, 3.50,  700.00),  -- PO4: 橙子
    (5, 11, 500, 3.20, 1600.00),  -- PO5: 大米
    (5, 12, 100,18.00, 1800.00),  -- PO5: 花生油（桶）
    (6, 5,  200, 3.00,  600.00),  -- PO6: 菠菜
    (6, 10, 500, 1.50,  750.00);  -- PO6: 西瓜

-- ══════════════════════════════════════════════════════
--  销售订单（4 张，覆盖各状态）
-- ══════════════════════════════════════════════════════
INSERT INTO sales_orders
    (order_no,        customer_id, salesman_id, order_date,   delivery_date, total_amount, received_amount, payment_method, payment_status, status)
VALUES
    ('SO20240910001', 1,           3,           '2024-09-10', '2024-09-11',   215.00,  215.00, '现金', 'PAID',   'COMPLETED'),
    ('SO20240920001', 2,           3,           '2024-09-20', '2024-09-21',   325.00,    0.00, '转账', 'UNPAID', 'SHIPPED'),
    ('SO20241005001', 3,           6,           '2024-10-05', '2024-10-07',   180.00,    0.00, '现金', 'UNPAID', 'APPROVED'),
    ('SO20241020001', 4,           3,           '2024-10-20', '2024-10-22',   400.00,    0.00, '转账', 'UNPAID', 'PENDING');

-- 销售订单明细
INSERT INTO sales_order_items (order_id, product_id, quantity, price, amount) VALUES
    (1, 1,  40, 3.50,  140.00),  -- SO1: 西红柿
    (1, 2,  30, 2.50,   75.00),  -- SO1: 黄瓜
    (2, 6,  50, 6.50,  325.00),  -- SO2: 苹果
    (3, 3, 100, 1.80,  180.00),  -- SO3: 土豆
    (4, 8,  80, 5.00,  400.00);  -- SO4: 橙子

-- ══════════════════════════════════════════════════════
--  库存（已入库订单对应的库存余量）
--  今天 2026-04-18，各效期状态均有覆盖
-- ══════════════════════════════════════════════════════
INSERT INTO inventories
    (product_id, warehouse_id, batch_no,         quantity, available_quantity, frozen_quantity, purchase_price, production_date, expiry_date,  status)
VALUES
    -- 主仓库 WH001 ─────────────────────────────────
    (1,  1, 'PO20240901001',  60,  60, 0, 2.50, '2026-04-15', '2026-04-22', 'EXPIRING'), -- 西红柿 余60斤，4天到期
    (2,  1, 'PO20240901001',  50,  50, 0, 1.80, '2026-04-16', '2026-04-21', 'EXPIRING'), -- 黄瓜   余50斤，3天到期
    (3,  1, 'PO20241001001', 300, 300, 0, 1.20, '2026-04-10', '2026-05-10', 'NORMAL'),   -- 土豆   300斤
    (4,  1, 'PO20241001001', 400, 380,20, 0.80, '2026-04-05', '2026-04-19', 'EXPIRING'), -- 白菜   400斤（20冻结），1天到期
    (5,  1, 'BATCH-2026-001', 30,  30, 0, 3.00, '2026-04-08', '2026-04-13', 'EXPIRED'),  -- 菠菜   已过期！
    (11, 1, 'PO20241101001', 500, 500, 0, 3.20, '2026-01-15', '2027-01-15', 'NORMAL'),   -- 大米   500千克
    (12, 1, 'PO20241101001', 100, 100, 0,18.00, '2025-12-01', '2027-06-01', 'NORMAL'),   -- 花生油 100桶
    -- 冷库 WH002 ───────────────────────────────────
    (6,  2, 'PO20240915001', 150, 150, 0, 4.50, '2026-04-01', '2026-05-01', 'NORMAL'),   -- 苹果   余150斤
    (7,  2, 'PO20240915001', 150, 150, 0, 2.80, '2026-04-12', '2026-04-19', 'EXPIRING'), -- 香蕉   余150斤，1天到期
    (8,  2, 'BATCH-2026-002', 80,  80, 0, 3.50, '2026-04-10', '2026-04-24', 'NORMAL'),   -- 橙子   80斤
    (9,  2, 'BATCH-2026-003', 60,  60, 0, 6.00, '2026-04-14', '2026-04-21', 'EXPIRING'), -- 葡萄   60斤，3天到期
    (10, 2, 'BATCH-2026-004',200, 200, 0, 1.50, '2026-04-15', '2026-04-25', 'NORMAL');   -- 西瓜   200斤

-- ══════════════════════════════════════════════════════
--  公告
-- ══════════════════════════════════════════════════════
INSERT INTO announcements (title, content, publisher_id, priority, is_top, publish_time, status) VALUES
    ('系统正式上线通知',
     '果蔬批发商贸管理系统已正式上线运行。各角色账号均已创建，请各岗位人员使用分配的账号登录并修改初始密码。如遇问题请联系管理员。',
     1, 'HIGH', TRUE, NOW(), 'PUBLISHED'),
    ('近期临期商品提醒',
     '经系统盘查，当前库存中西红柿、黄瓜、白菜、香蕉、葡萄等品类存在7天内到期批次，菠菜已过期，请仓管员及时处理，采购员安排补货计划。',
     1, 'URGENT', TRUE, NOW(), 'PUBLISHED'),
    ('五一劳动节期间业务安排',
     '五一期间（5月1日-5月5日）正常发货，财务付款于5月6日统一处理。各采购员请于4月28日前完成节前备货采购单的提交与审核。',
     1, 'NORMAL', FALSE, NOW(), 'PUBLISHED');