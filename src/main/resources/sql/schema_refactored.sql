-- ============================================
-- NovelHub 重构数据库初始化脚本 (MySQL 8.4.3)
-- 订单与支付分离，统一ID字段命名
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS novelhub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE novelhub;

-- ============================================
-- 用户相关表
-- ============================================

-- 用户表
CREATE TABLE `tb_user` (
    `user_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名（必填，OAuth登录时自动生成）',
    `password` VARCHAR(255) DEFAULT NULL COMMENT '密码（PBKDF2加密，OAuth用户可为空）',
    `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `gender` TINYINT DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
    `birthday` DATE DEFAULT NULL COMMENT '生日',
    `introduction` VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `email_verified` TINYINT DEFAULT 0 COMMENT '邮箱是否验证：0-未验证 1-已验证',
    `verification_token` VARCHAR(255) DEFAULT NULL COMMENT '邮箱验证令牌',
    `token_expiry` DATETIME DEFAULT NULL COMMENT '令牌过期时间',
    `wallet_address` VARCHAR(64) DEFAULT NULL COMMENT '钱包地址',
    `wallet_type` VARCHAR(20) DEFAULT NULL COMMENT '钱包类型：metamask, walletconnect, coinbase',
    `nonce` VARCHAR(255) DEFAULT NULL COMMENT '防重放攻击的nonce',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 社交账号表（OAuth登录）
CREATE TABLE `tb_social_account` (
    `social_account_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '社交账号ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `email` VARCHAR(255) NOT NULL COMMENT '邮箱',
    `provider` INT NOT NULL DEFAULT 1 COMMENT '第三方平台：1-Google 2-Facebook 3-GitHub',
    `provider_id` VARCHAR(255) NOT NULL COMMENT '第三方平台用户ID',
    `extra_data` TEXT COMMENT '额外信息（头像、姓名等JSON数据）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`social_account_id`),
    UNIQUE KEY `uk_user_provider` (`user_id`, `provider`),
    KEY `idx_provider_id` (`provider_id`),
    KEY `idx_email` (`email`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_social_account_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社交账号表';

-- 用户登录日志表
CREATE TABLE `tb_user_login_log` (
    `login_log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `login_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    `login_ip` VARCHAR(50) DEFAULT NULL COMMENT '登录IP',
    `device` VARCHAR(100) DEFAULT NULL COMMENT '设备信息',
    `browser` VARCHAR(100) DEFAULT NULL COMMENT '浏览器信息',
    `os` VARCHAR(100) DEFAULT NULL COMMENT '操作系统',
    `fingerprint` VARCHAR(255) DEFAULT NULL COMMENT '浏览器指纹',
    `login_status` TINYINT DEFAULT 1 COMMENT '登录状态：0-失败 1-成功',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`login_log_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户登录日志表';

-- 管理员表
CREATE TABLE `tb_admin` (
    `admin_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（PBKDF2加密）',
    `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `role` VARCHAR(20) DEFAULT 'admin' COMMENT '角色：admin-管理员 super_admin-超级管理员',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`admin_id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- 用户积分表
CREATE TABLE `tb_user_points` (
    `user_points_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '积分ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `points` INT NOT NULL DEFAULT 0 COMMENT '当前剩余积分（固定积分）',
    `fixed_points` INT NOT NULL DEFAULT 0 COMMENT '一次性购买的固定积分,可累加',
    `sub_points` INT NOT NULL DEFAULT 0 COMMENT '订阅套餐积分',
    `sub_points_left` INT NOT NULL DEFAULT 0 COMMENT '订阅套餐剩余积分',
    `free_points` INT NOT NULL DEFAULT 0 COMMENT '每日领取的免费积分',
    `claimed_days` INT NOT NULL DEFAULT 0 COMMENT '累计领取积分天数',
    `claimed_at` DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '领取积分的时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_points_id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户积分表';

-- 积分明细表
CREATE TABLE `tb_point_detail` (
    `point_detail_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `points` INT NOT NULL DEFAULT 0 COMMENT '变更的积分',
    `type` INT NOT NULL DEFAULT 0 COMMENT '0-消耗积分，1-增加积分',
    `func_type` INT NOT NULL DEFAULT 0 COMMENT '功能类型',
    `points_type` INT NOT NULL DEFAULT 1 COMMENT '0-免费积分消耗，1-永久积分消耗',
    `task_id` VARCHAR(64) NOT NULL COMMENT '任务id',
    `is_api` INT NOT NULL DEFAULT 0 COMMENT '是否通过api消耗的积分， 0-否，1-是',
    `extra_data` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '原因',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`point_detail_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_user_created` (`user_id`, `created_at`),
    KEY `idx_type_created` (`type`, `created_at`),
    KEY `idx_points_type` (`points_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分明细表';

-- ============================================
-- 支付相关表
-- ============================================

-- 支付套餐表
CREATE TABLE `tb_payment_plan` (
    `plan_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
    `stripe_price_id` VARCHAR(128) DEFAULT NULL COMMENT 'Stripe价格ID',
    `plan_name` VARCHAR(100) NOT NULL COMMENT '套餐名称',
    `description` TEXT COMMENT '套餐描述',
    `price` INT NOT NULL COMMENT '价格（分）',
    `currency` VARCHAR(10) DEFAULT 'usd' COMMENT '货币',
    `points_amount` INT NOT NULL COMMENT '包含积分',
    `duration_days` INT DEFAULT NULL COMMENT '有效期（天），NULL表示永久',
    `plan_type` TINYINT NOT NULL COMMENT '套餐类型：1-订阅 2-一次性购买',
    `features` JSON DEFAULT NULL COMMENT '功能特性',
    `status` TINYINT DEFAULT 1 COMMENT '是否启用：0-禁用 1-启用',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`plan_id`),
    UNIQUE KEY `uk_stripe_price_id` (`stripe_price_id`),
    KEY `idx_plan_type` (`plan_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付套餐表';

-- 订单表（购物订单）
CREATE TABLE `tb_order` (
    `order_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `order_number` VARCHAR(64) NOT NULL COMMENT '订单号',
    `plan_id` BIGINT NOT NULL COMMENT '套餐ID',
    `plan_name` VARCHAR(100) NOT NULL COMMENT '套餐名称',
    `amount` INT NOT NULL COMMENT '支付金额（分）',
    `currency` VARCHAR(10) DEFAULT 'usd' COMMENT '货币',
    `points` INT NOT NULL COMMENT '获得积分',
    `status` INT NOT NULL DEFAULT 0 COMMENT '状态：0-待支付 1-已支付 2-已取消 3-已过期 4-已退款',
    `order_type` TINYINT DEFAULT 2 COMMENT '订单类型：1-订阅 2-一次性购买',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`order_id`),
    UNIQUE KEY `uk_order_number` (`order_number`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_plan_id` (`plan_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_order_plan` FOREIGN KEY (`plan_id`) REFERENCES `tb_payment_plan` (`plan_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 支付表（支付订单）
CREATE TABLE `tb_payment` (
    `payment_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `payment_number` VARCHAR(64) NOT NULL COMMENT '支付单号',
    `payment_method` VARCHAR(50) NOT NULL COMMENT '支付方式：stripe, web3',
    `amount` INT NOT NULL COMMENT '支付金额（分）',
    `currency` VARCHAR(10) DEFAULT 'usd' COMMENT '货币',
    `status` INT NOT NULL DEFAULT 0 COMMENT '状态：0-待支付 1-已支付 2-已取消 3-已过期',
    `expires_at` DATETIME NOT NULL COMMENT '过期时间（15分钟）',
    `payment_timeout_at` DATETIME DEFAULT NULL COMMENT '支付超时时间',
    
    -- Stripe相关字段
    `stripe_session_id` VARCHAR(128) DEFAULT NULL COMMENT 'Stripe会话ID',
    `stripe_customer_id` VARCHAR(128) DEFAULT NULL COMMENT 'Stripe客户ID',
    `stripe_subscription_id` VARCHAR(128) DEFAULT NULL COMMENT 'Stripe订阅ID',
    `stripe_payment_intent_id` VARCHAR(128) DEFAULT NULL COMMENT 'Stripe支付意图ID',
    
    -- Web3相关字段
    `tx_hash` VARCHAR(128) DEFAULT NULL COMMENT '区块链交易哈希',
    `from_address` VARCHAR(64) DEFAULT NULL COMMENT '支付地址',
    `to_address` VARCHAR(64) DEFAULT NULL COMMENT '收款地址',
    `token_amount` DECIMAL(36,18) DEFAULT NULL COMMENT '代币数量',
    `token_currency` VARCHAR(20) DEFAULT NULL COMMENT '代币类型',
    `chain_id` INT DEFAULT NULL COMMENT '区块链ID',
    `block_number` BIGINT DEFAULT NULL COMMENT '区块高度',
    `confirmations` INT DEFAULT 0 COMMENT '确认数',
    `price_ttl` BIGINT DEFAULT NULL COMMENT '价格锁定时间',
    
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `paid_at` DATETIME DEFAULT NULL COMMENT '支付完成时间',
    
    PRIMARY KEY (`payment_id`),
    UNIQUE KEY `uk_payment_number` (`payment_number`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_payment_method` (`payment_method`),
    KEY `idx_status` (`status`),
    KEY `idx_tx_hash` (`tx_hash`),
    KEY `idx_expires_at` (`expires_at`),
    CONSTRAINT `fk_payment_order` FOREIGN KEY (`order_id`) REFERENCES `tb_order` (`order_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_payment_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付表';

-- ============================================
-- 小说相关表
-- ============================================

-- 小说分类表
CREATE TABLE `tb_novel_category` (
    `category_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `category_name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID，0表示顶级分类',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '分类描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`category_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小说分类表';

-- 小说表
CREATE TABLE `tb_novel` (
    `novel_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '小说ID',
    `novel_name` VARCHAR(100) NOT NULL COMMENT '小说名称',
    `author` VARCHAR(50) NOT NULL COMMENT '作者',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL',
    `description` TEXT COMMENT '小说简介',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-连载中 1-已完结 2-已停更',
    `word_count` INT DEFAULT 0 COMMENT '总字数',
    `chapter_count` INT DEFAULT 0 COMMENT '章节数',
    `view_count` INT DEFAULT 0 COMMENT '浏览量',
    `favorite_count` INT DEFAULT 0 COMMENT '收藏数',
    `comment_count` INT DEFAULT 0 COMMENT '评论数',
    `rating` DECIMAL(3,2) DEFAULT 0.00 COMMENT '评分（0-10）',
    `rating_count` INT DEFAULT 0 COMMENT '评分人数',
    `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签，逗号分隔',
    `publish_status` TINYINT DEFAULT 0 COMMENT '发布状态：0-草稿 1-已发布',
    `last_chapter_id` BIGINT DEFAULT NULL COMMENT '最新章节ID',
    `last_chapter_time` DATETIME DEFAULT NULL COMMENT '最新章节更新时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`novel_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_author` (`author`),
    KEY `idx_status` (`status`),
    KEY `idx_publish_status` (`publish_status`),
    KEY `idx_view_count` (`view_count`),
    KEY `idx_favorite_count` (`favorite_count`),
    KEY `idx_created_at` (`created_at`),
    FULLTEXT KEY `ft_novel_name` (`novel_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小说表';

-- 章节表
CREATE TABLE `tb_chapter` (
    `chapter_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '章节ID',
    `novel_id` BIGINT NOT NULL COMMENT '小说ID',
    `chapter_number` INT NOT NULL COMMENT '章节序号',
    `chapter_title` VARCHAR(200) NOT NULL COMMENT '章节标题',
    `content` LONGTEXT COMMENT '章节内容',
    `word_count` INT DEFAULT 0 COMMENT '字数',
    `is_vip` TINYINT DEFAULT 0 COMMENT '是否VIP章节：0-否 1-是',
    `price` INT DEFAULT 0 COMMENT '价格（单位：书币）',
    `view_count` INT DEFAULT 0 COMMENT '浏览量',
    `publish_status` TINYINT DEFAULT 0 COMMENT '发布状态：0-草稿 1-已发布',
    `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`chapter_id`),
    UNIQUE KEY `uk_novel_chapter` (`novel_id`, `chapter_number`),
    KEY `idx_novel_id` (`novel_id`),
    KEY `idx_publish_status` (`publish_status`),
    KEY `idx_publish_time` (`publish_time`),
    CONSTRAINT `fk_chapter_novel` FOREIGN KEY (`novel_id`) REFERENCES `tb_novel` (`novel_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='章节表';

-- ============================================
-- 用户交互相关表
-- ============================================

-- 书架表（用户收藏的小说）
CREATE TABLE `tb_bookshelf` (
    `bookshelf_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '书架ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `novel_id` BIGINT NOT NULL COMMENT '小说ID',
    `add_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入书架时间',
    `last_read_chapter_id` BIGINT DEFAULT NULL COMMENT '最后阅读章节ID',
    `last_read_time` DATETIME DEFAULT NULL COMMENT '最后阅读时间',
    `read_progress` INT DEFAULT 0 COMMENT '阅读进度（百分比）',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `is_top` TINYINT DEFAULT 0 COMMENT '是否置顶：0-否 1-是',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`bookshelf_id`),
    UNIQUE KEY `uk_user_novel` (`user_id`, `novel_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_novel_id` (`novel_id`),
    KEY `idx_add_time` (`add_time`),
    CONSTRAINT `fk_bookshelf_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_bookshelf_novel` FOREIGN KEY (`novel_id`) REFERENCES `tb_novel` (`novel_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书架表';

-- 阅读历史表
CREATE TABLE `tb_read_history` (
    `read_history_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '历史记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `novel_id` BIGINT NOT NULL COMMENT '小说ID',
    `chapter_id` BIGINT NOT NULL COMMENT '章节ID',
    `read_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    `read_duration` INT DEFAULT 0 COMMENT '阅读时长（秒）',
    `read_progress` INT DEFAULT 0 COMMENT '章节阅读进度（百分比）',
    `device` VARCHAR(100) DEFAULT NULL COMMENT '阅读设备',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`read_history_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_novel_id` (`novel_id`),
    KEY `idx_read_time` (`read_time`),
    CONSTRAINT `fk_read_history_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_read_history_novel` FOREIGN KEY (`novel_id`) REFERENCES `tb_novel` (`novel_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_read_history_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `tb_chapter` (`chapter_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阅读历史表';

-- 书签表
CREATE TABLE `tb_bookmark` (
    `bookmark_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '书签ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `novel_id` BIGINT NOT NULL COMMENT '小说ID',
    `chapter_id` BIGINT NOT NULL COMMENT '章节ID',
    `position` INT DEFAULT 0 COMMENT '书签位置（章节内的字符位置）',
    `content_snippet` VARCHAR(500) DEFAULT NULL COMMENT '书签内容片段',
    `note` VARCHAR(1000) DEFAULT NULL COMMENT '笔记',
    `bookmark_type` TINYINT DEFAULT 0 COMMENT '书签类型：0-普通书签 1-重点标记 2-疑问',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`bookmark_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_novel_id` (`novel_id`),
    KEY `idx_chapter_id` (`chapter_id`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_bookmark_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_bookmark_novel` FOREIGN KEY (`novel_id`) REFERENCES `tb_novel` (`novel_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_bookmark_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `tb_chapter` (`chapter_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书签表';

-- 评论表
CREATE TABLE `tb_comment` (
    `comment_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `novel_id` BIGINT NOT NULL COMMENT '小说ID',
    `chapter_id` BIGINT DEFAULT NULL COMMENT '章节ID（章节评论）',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父评论ID，0表示顶级评论',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `reply_count` INT DEFAULT 0 COMMENT '回复数',
    `is_top` TINYINT DEFAULT 0 COMMENT '是否置顶：0-否 1-是',
    `is_hot` TINYINT DEFAULT 0 COMMENT '是否热门：0-否 1-是',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-已删除 1-正常 2-审核中',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`comment_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_novel_id` (`novel_id`),
    KEY `idx_chapter_id` (`chapter_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_novel` FOREIGN KEY (`novel_id`) REFERENCES `tb_novel` (`novel_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `tb_chapter` (`chapter_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- 评分表
CREATE TABLE `tb_rating` (
    `rating_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评分ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `novel_id` BIGINT NOT NULL COMMENT '小说ID',
    `rating` DECIMAL(3,2) NOT NULL COMMENT '评分（0-10）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`rating_id`),
    UNIQUE KEY `uk_user_novel` (`user_id`, `novel_id`),
    KEY `idx_novel_id` (`novel_id`),
    KEY `idx_rating` (`rating`),
    CONSTRAINT `fk_rating_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_rating_novel` FOREIGN KEY (`novel_id`) REFERENCES `tb_novel` (`novel_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评分表';

-- ============================================
-- 系统配置表
-- ============================================

-- 系统配置表
CREATE TABLE `tb_system_config` (
    `config_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` TEXT COMMENT '配置值',
    `config_type` VARCHAR(20) DEFAULT 'string' COMMENT '配置类型：string,number,boolean,json',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '配置描述',
    `is_system` TINYINT DEFAULT 0 COMMENT '是否系统配置：0-否 1-是',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`config_id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- Banner表（首页轮播图）
CREATE TABLE `tb_banner` (
    `banner_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Banner ID',
    `title` VARCHAR(200) NOT NULL COMMENT 'Banner标题',
    `image` VARCHAR(500) NOT NULL COMMENT '图片URL',
    `link` VARCHAR(500) DEFAULT NULL COMMENT '链接地址',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号，越小越靠前',
    `active` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`banner_id`),
    KEY `idx_sort_order` (`sort_order`),
    KEY `idx_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Banner表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入默认分类
INSERT INTO `tb_novel_category` (`category_name`, `parent_id`, `sort_order`, `description`) VALUES
('玄幻', 0, 1, '玄幻小说'),
('武侠', 0, 2, '武侠小说'),
('都市', 0, 3, '都市小说'),
('历史', 0, 4, '历史小说'),
('科幻', 0, 5, '科幻小说'),
('游戏', 0, 6, '游戏小说'),
('言情', 0, 7, '言情小说'),
('其他', 0, 99, '其他类型');

-- 插入系统配置
INSERT INTO `tb_system_config` (`config_key`, `config_value`, `config_type`, `description`, `is_system`) VALUES
('site_name', 'NovelHub', 'string', '网站名称', 1),
('site_description', '最好的在线小说阅读平台', 'string', '网站描述', 1),
('default_avatar', 'https://via.placeholder.com/100', 'string', '默认头像', 1),
('max_upload_size', '5242880', 'number', '最大上传大小（字节）', 1),
('chapter_free_count', '10', 'number', '免费章节数', 1);

-- 插入默认管理员（密码：admin123，注意：此处的哈希值为旧BCrypt格式，需使用PBKDF2重新生成）
-- 建议删除此行后通过应用注册功能创建管理员账户，或手动生成PBKDF2格式的哈希值
INSERT INTO `tb_admin` (`username`, `password`, `email`, `nickname`, `role`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@novelhub.com', '系统管理员', 'super_admin', 1);

-- 插入默认Banner数据
INSERT INTO `tb_banner` (`title`, `image`, `link`, `sort_order`, `active`) VALUES
('欢迎来到NovelHub', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=1200&h=400&fit=crop', '/about', 1, 1),
('发现精彩故事', 'https://images.unsplash.com/photo-1519681393784-d120267933ba?w=1200&h=400&fit=crop', '#', 2, 1),
('随时随地阅读', 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200&h=400&fit=crop', '#', 3, 1);

-- 插入示例支付套餐
INSERT INTO `tb_payment_plan` (`stripe_price_id`, `plan_name`, `description`, `price`, `currency`, `points_amount`, `plan_type`, `features`, `status`, `sort_order`) VALUES
('price_basic_monthly', '基础套餐', '适合偶尔阅读的用户', 990, 'usd', 1000, 2, '["1000 积分", "解锁VIP章节", "无广告阅读", "基础客服支持"]', 1, 1),
('price_premium_monthly', '高级套餐', '最受欢迎的选择', 1990, 'usd', 2600, 2, '["2600 积分", "解锁VIP章节", "无广告阅读", "离线下载", "优先客服支持", "专属阅读主题"]', 1, 2),
('price_ultimate_monthly', '终极套餐', '最超值选择', 3990, 'usd', 5999, 2, '["5999 积分", "解锁VIP章节", "无广告阅读", "离线下载", "优先客服支持", "专属阅读主题", "高级功能"]', 1, 3);

-- ============================================
-- 创建视图（可选）
-- ============================================

-- 小说详情视图（包含统计信息）
CREATE OR REPLACE VIEW v_novel_detail AS
SELECT 
    n.novel_id,
    n.novel_name,
    n.author,
    n.category_id,
    c.category_name,
    n.cover_image,
    n.description,
    n.status,
    n.word_count,
    n.chapter_count,
    n.view_count,
    n.favorite_count,
    n.comment_count,
    n.rating,
    n.rating_count,
    n.tags,
    n.publish_status,
    n.last_chapter_time,
    n.created_at,
    n.updated_at
FROM tb_novel n
LEFT JOIN tb_novel_category c ON n.category_id = c.category_id
WHERE n.deleted = 0 AND n.publish_status = 1;

-- 用户书架视图
CREATE OR REPLACE VIEW v_user_bookshelf AS
SELECT 
    b.bookshelf_id,
    b.user_id,
    b.novel_id,
    n.novel_name,
    n.author,
    n.cover_image,
    n.chapter_count,
    n.status AS novel_status,
    b.last_read_chapter_id,
    ch.chapter_number AS last_read_chapter_number,
    ch.chapter_title AS last_read_chapter_title,
    b.last_read_time,
    b.read_progress,
    b.is_top,
    b.add_time,
    n.last_chapter_time
FROM tb_bookshelf b
INNER JOIN tb_novel n ON b.novel_id = n.novel_id
LEFT JOIN tb_chapter ch ON b.last_read_chapter_id = ch.chapter_id
WHERE b.deleted = 0 AND n.deleted = 0;

-- 订单详情视图（包含支付信息）
CREATE OR REPLACE VIEW v_order_detail AS
SELECT 
    o.order_id,
    o.user_id,
    o.order_number,
    o.plan_id,
    p.plan_name,
    o.amount,
    o.currency,
    o.points,
    o.status AS order_status,
    o.order_type,
    o.created_at AS order_created_at,
    pay.payment_id,
    pay.payment_number,
    pay.payment_method,
    pay.status AS payment_status,
    pay.expires_at,
    pay.paid_at
FROM tb_order o
LEFT JOIN tb_payment_plan p ON o.plan_id = p.plan_id
LEFT JOIN tb_payment pay ON o.order_id = pay.order_id
ORDER BY o.created_at DESC, pay.created_at DESC;

-- ============================================
-- 完成
-- ============================================
