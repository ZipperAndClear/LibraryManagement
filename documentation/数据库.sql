-- --------------------------------------------------------
-- 校园图书借阅管理系统 (毕设版) 数据库初始化脚本
-- 环境要求: MySQL 5.7+
-- 字符集: utf8mb4 (支持表情与生僻字)
-- --------------------------------------------------------

CREATE DATABASE IF NOT EXISTS `library_management` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `library_management`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 系统角色表 (sys_role)
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) NOT NULL COMMENT '角色名称 (如: 超级管理员, 学生)',
  `role_code` varchar(30) NOT NULL COMMENT '角色权限字符串 (如: admin, student)',
  `status` tinyint(1) DEFAULT 1 COMMENT '角色状态 (1正常 0停用)',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除 (0未删除 1已删除)',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者ID',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 初始化基础角色
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'admin', 1, 0, 1, NOW(), 1, NOW());
INSERT INTO `sys_role` VALUES (2, '图书管理员', 'librarian', 1, 0, 1, NOW(), 1, NOW());
INSERT INTO `sys_role` VALUES (3, '普通学生', 'student', 1, 0, 1, NOW(), 1, NOW());

-- ----------------------------
-- 2. 系统用户表 (sys_user) - 合并原admin与reader
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号 (学号/工号)',
  `password` varchar(100) NOT NULL COMMENT '密码 (建议存储BCrypt加密后密文)',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `avatar` varchar(255) DEFAULT NULL COMMENT '用户头像URL',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `status` tinyint(1) DEFAULT 1 COMMENT '帐号状态 (1正常 0禁用)',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除 (0未删除 1已删除)',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者ID',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 初始化超级管理员账号 (密码明文示例为123456，实际开发需加密)
INSERT INTO `sys_user` VALUES (1, 'admin', '123456', '系统管理员', NULL, 'admin@test.com', '13800138000', 1, 0, 1, NOW(), 1, NOW());

-- ----------------------------
-- 3. 用户角色关联表 (sys_user_role)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

INSERT INTO `sys_user_role` VALUES (1, 1);

-- ----------------------------
-- 4. 图书多级分类表 (sys_category)
-- ----------------------------
DROP TABLE IF EXISTS `sys_category`;
CREATE TABLE `sys_category`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `parent_id` bigint(20) DEFAULT 0 COMMENT '父分类ID (0为顶级节点)',
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `sort` int(4) DEFAULT 0 COMMENT '显示顺序',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除标记',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='图书分类表';

-- ----------------------------
-- 5. 图书信息表 (book)
-- ----------------------------
DROP TABLE IF EXISTS `book`;
CREATE TABLE `book`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '图书ID',
  `isbn` varchar(20) NOT NULL COMMENT '国际标准书号 (ISBN)',
  `name` varchar(100) NOT NULL COMMENT '图书名称',
  `author` varchar(100) NOT NULL COMMENT '作者',
  `publisher` varchar(100) DEFAULT NULL COMMENT '出版社',
  `price` decimal(10, 2) DEFAULT 0.00 COMMENT '图书价格',
  `stock` int(11) NOT NULL DEFAULT 0 COMMENT '当前库存数量',
  `category_id` bigint(20) NOT NULL COMMENT '所属分类ID',
  `cover_url` varchar(255) DEFAULT NULL COMMENT '图书封面图片URL',
  `introduction` text COMMENT '图书内容简介',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态 (1-在库 2-全部借出 3-下架 4-遗失)',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除标记 (1已删除)',
  `create_by` bigint(20) DEFAULT NULL COMMENT '录入人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '修改人ID',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_isbn` (`isbn`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书信息表';

-- ----------------------------
-- 6. 图书借阅记录表 (borrow_record) - 核心业务表
-- ----------------------------
DROP TABLE IF EXISTS `borrow_record`;
CREATE TABLE `borrow_record`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '借阅流水ID',
  `user_id` bigint(20) NOT NULL COMMENT '借阅人(用户)ID',
  `book_id` bigint(20) NOT NULL COMMENT '借阅图书ID',
  `borrow_time` datetime NOT NULL COMMENT '借出时间',
  `expect_return_time` datetime NOT NULL COMMENT '应还时间 (由系统参数动态计算)',
  `actual_return_time` datetime DEFAULT NULL COMMENT '实际归还时间',
  `renew_count` int(2) DEFAULT 0 COMMENT '已续借次数',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '借阅状态 (0-借阅中 1-正常归还 2-逾期未还 3-逾期已还 4-图书遗失)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间 (即记录生成时间)',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '状态更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书借阅记录表';

-- ----------------------------
-- 7. 逾期罚款记录表 (fine_record)
-- ----------------------------
DROP TABLE IF EXISTS `fine_record`;
CREATE TABLE `fine_record`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '罚单ID',
  `borrow_record_id` bigint(20) NOT NULL COMMENT '关联的借阅记录ID',
  `user_id` bigint(20) NOT NULL COMMENT '受罚用户ID',
  `fine_amount` decimal(10, 2) NOT NULL COMMENT '罚款金额',
  `reason` varchar(255) DEFAULT '图书逾期' COMMENT '罚款原因',
  `status` tinyint(1) DEFAULT 0 COMMENT '缴费状态 (0-未缴费 1-已缴费 2-已免除)',
  `pay_time` datetime DEFAULT NULL COMMENT '实际缴费时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '罚单生成时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逾期罚款记录表';

-- ----------------------------
-- 8. 系统配置表 (sys_config) - 提高系统灵活性
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='系统参数配置表';

-- 初始化关键业务参数
INSERT INTO `sys_config` VALUES (1, '学生最大借阅数量', 'sys.borrow.max_num', '5', '每个学生最多同时借阅的书籍数量', NOW(), NOW());
INSERT INTO `sys_config` VALUES (2, '默认可借阅天数', 'sys.borrow.max_days', '30', '一本图书默认可以借阅的天数', NOW(), NOW());
INSERT INTO `sys_config` VALUES (3, '逾期每日罚金', 'sys.borrow.daily_fine', '0.50', '逾期后每天产生的罚款金额(元)', NOW(), NOW());

-- ----------------------------
-- 9. 系统公告表 (sys_notice)
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` varchar(100) NOT NULL COMMENT '公告标题',
  `content` text NOT NULL COMMENT '公告正文 (支持富文本)',
  `is_top` tinyint(1) DEFAULT 0 COMMENT '是否置顶 (0-否 1-是)',
  `status` tinyint(1) DEFAULT 1 COMMENT '公告状态 (1-发布 0-草稿)',
  `create_by` bigint(20) DEFAULT NULL COMMENT '发布人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知公告表';

SET FOREIGN_KEY_CHECKS = 1;