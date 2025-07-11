CREATE DATABASE IF NOT EXISTS `liteisle_db`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `liteisle_db`;

-- =================================================================
-- 用户核心模块 (User Core Module)
-- 作用: 管理所有与用户账户、身份、配置和游戏化激励相关的数据。
-- =================================================================

-- ----------------------------
-- 1. 用户表 (users)
-- 描述: 存储用户的核心账户信息、认证凭据和云盘空间配额。
-- ----------------------------
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户唯一标识ID (主键)',
  `username` varchar(50) NOT NULL COMMENT '用户名，用于登录，全系统唯一',
  `email` varchar(100) NOT NULL COMMENT '用户邮箱，用于登录和接收通知，全系统唯一',
  `password` varchar(255) NOT NULL COMMENT '加密后的用户密码 (例如使用BCrypt)',
  `avatar` varchar(512) DEFAULT NULL COMMENT '用户头像的URL地址',
  `storage_quota` bigint DEFAULT 5368709120 COMMENT '用户总存储空间配额，单位：字节 (Bytes)，默认5GB',
  `storage_used` bigint DEFAULT 0 COMMENT '用户已使用的存储空间，单位：字节 (Bytes)，通过异步消息队列在文件操作后更新',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '账户创建时间',
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '账户信息最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账户与基本信息表';

-- ----------------------------
-- 2. 用户专注记录表 (user_focus_records)
-- 描述: 记录用户的每一次专注会话。
-- ----------------------------
CREATE TABLE `user_focus_records` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '专注记录唯一ID (主键)',
  `user_id` bigint NOT NULL COMMENT '外键，关联到 users.id，标识该记录所属的用户',
  `focus_minutes` int DEFAULT 0 COMMENT '单次完整专注会话的时长（单位：分钟）',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '专注完成并记录的时间点',
  PRIMARY KEY (`id`),
  KEY `idx_user_focus_time` (`user_id`, `create_time` DESC),
  CONSTRAINT `fk_focus_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户单次专注会话记录表';

-- ----------------------------
-- 3. 用户岛屿收集表 (user_islands)
-- 描述: 记录用户通过专注等行为解锁收集到的所有岛屿。
-- ----------------------------
CREATE TABLE `user_islands` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收集记录的唯一ID (主键)',
  `user_id` bigint NOT NULL COMMENT '外键，关联到 users.id，标识该岛屿的拥有者',
  `island_url` varchar(255) NOT NULL COMMENT '岛屿的唯一业务代码，与后端代码中的岛屿配置相对应',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '岛屿的获取时间',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_user_islands_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='记录用户已解锁的岛屿表';


-- =================================================================
-- 文件系统模块 (File System Module)
-- 作用: 管理所有与文件、文件夹相关的物理存储和逻辑结构。
-- =================================================================

-- ----------------------------
-- 4. 物理文件存储表 (storages)
-- 描述: 存储物理文件的唯一实体信息。通过文件哈希实现秒传和存储去重。
-- ----------------------------
CREATE TABLE `storages` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '物理存储记录ID (主键)',
  `file_hash` varchar(64) NOT NULL COMMENT '文件内容的哈希值 (SHA-256)，用于秒传功能的核心校验',
  `file_size` bigint NOT NULL COMMENT '文件大小，单位：字节 (Bytes)',
  `mime_type` varchar(100) NOT NULL COMMENT '文件的MIME类型，例如 "audio/mpeg", "application/pdf"',
  `storage_path` varchar(512) NOT NULL COMMENT '文件在对象存储服务(如R2)中的唯一路径或Key',
  `reference_count` int DEFAULT 1 COMMENT '引用计数。当计数值为0时，可由后台垃圾回收任务安全删除物理文件',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '该物理文件首次被上传到系统的时间',
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录的最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_hash` (`file_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存储唯一文件实体，实现秒传功能';

-- ----------------------------
-- 5. 文件夹表 (folders)
-- 描述: 存储用户可见的逻辑文件夹结构。
-- ----------------------------
CREATE TABLE `folders` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件夹唯一ID (主键)',
  `user_id` bigint NOT NULL COMMENT '外键，关联到 users.id，标识此文件夹的拥有者',
  `parent_id` bigint DEFAULT 0 COMMENT '父文件夹ID。值为0表示根目录下的文件夹',
  `folder_name` varchar(255) NOT NULL COMMENT '文件夹在UI上显示的名称',
  `folder_type` enum('system', 'playlist', 'notebook') NOT NULL COMMENT '只有 system, playlist, notebook 三类。',
  `sorted_order` double DEFAULT 0 COMMENT '用于用户自定义排序的浮点数值',
  `delete_time` timestamp NULL DEFAULT NULL COMMENT '软删除标记。非NULL表示已移入回收站',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '文件夹创建时间',
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '文件夹最后更新时间（如重命名）',
  PRIMARY KEY (`id`),
  KEY `idx_user_parent` (`user_id`, `parent_id`),
  CONSTRAINT `fk_folders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户逻辑文件夹结构表';

-- ----------------------------
-- 6. 文件基础信息表 (files)
-- 描述: 存储所有文件的通用基础信息，作为文件实体的核心。
-- ----------------------------
CREATE TABLE `files` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件唯一ID (主键)',
  `user_id` bigint NOT NULL COMMENT '外键，关联到 users.id，标识此文件的拥有者',
  `folder_id` bigint NOT NULL COMMENT '外键，关联到 folders.id，标识文件所在的文件夹',
  `storage_id` bigint DEFAULT NULL COMMENT '外键，关联到 storages.id，文件处理成功后填充',
  `file_name` varchar(255) NOT NULL COMMENT '文件名（通常包含扩展名）',
  `file_extension` varchar(20) DEFAULT NULL COMMENT '文件扩展名，如 "mp3", "pdf", 用于前端显示',
  `file_type` enum('music', 'document') NOT NULL COMMENT '文件的业务大类，用于甄别应关联哪个元数据表',
  `file_status` enum('processing', 'available', 'failed') NOT NULL DEFAULT 'processing' COMMENT '文件状态。processing:后台处理中; available:完全可用; failed:处理失败',
  `sorted_order` double DEFAULT 0 COMMENT '用于用户自定义排序的浮点数值',
  `delete_time` timestamp NULL DEFAULT NULL COMMENT '软删除标记。非NULL表示已移入回收站',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '文件逻辑记录的创建时间',
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '文件元数据的最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_folder_status` (`user_id`, `folder_id`, `file_status`),
  CONSTRAINT `fk_files_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_files_folder` FOREIGN KEY (`folder_id`) REFERENCES `folders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_files_storage` FOREIGN KEY (`storage_id`) REFERENCES `storages` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件通用基础信息表';

-- ----------------------------
-- 7. 音乐文件元数据表 (music_metadata)
-- 描述: 存储音乐文件专属的元数据。
-- ----------------------------
CREATE TABLE `music_metadata` (
  `file_id` bigint NOT NULL COMMENT '主键，同时也是外键，关联到 files.id',
  `artist` varchar(255) DEFAULT NULL COMMENT '歌手名，由后台FFmpeg等工具解析填充',
  `album` varchar(255) DEFAULT NULL COMMENT '专辑名',
  `duration` int NOT NULL COMMENT '时长，单位：秒',
  `cover_art_url` varchar(512) DEFAULT NULL COMMENT '专辑封面图的URL',
  PRIMARY KEY (`file_id`),
  CONSTRAINT `fk_music_file` FOREIGN KEY (`file_id`) REFERENCES `files` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='音乐文件专属元数据表';

-- ----------------------------
-- 8. 文档文件元数据表 (document_metadata)
-- 描述: 存储文档文件专属的元数据。
-- ----------------------------
CREATE TABLE `document_metadata` (
  `file_id` bigint NOT NULL COMMENT '主键，同时也是外键，关联到 files.id',
  `content` longtext COMMENT 'Markdown文件的纯文本内容，用于全文搜索',
  `version` bigint NOT NULL DEFAULT 1 COMMENT '用于Markdown文件的乐观锁版本号',
  PRIMARY KEY (`file_id`),
  CONSTRAINT `fk_document_file` FOREIGN KEY (`file_id`) REFERENCES `files` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档文件专属元数据表';

-- =================================================================
-- 分享与传输模块 (Sharing & Transfer Module)
-- 作用: 管理所有与文件分享、上传下载任务相关的业务数据和日志。
-- =================================================================

-- ----------------------------
-- 9. 分享表 (share_links)
-- 描述: 管理用户创建的所有公开分享链接。
-- ----------------------------
CREATE TABLE `share_links` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分享记录唯一ID (主键)',
  `user_id` bigint NOT NULL COMMENT '外键，关联到 users.id，标识分享的创建者',
  `share_token` varchar(64) NOT NULL COMMENT '分享链接的唯一公开凭证，是URL的一部分',
  `share_password` varchar(255) DEFAULT NULL COMMENT '加密后的分享提取码（如果分享是加密的）',
  `file_id` bigint DEFAULT NULL COMMENT '外键，关联到 files.id，当分享目标是单个文件时填充',
  `folder_id` bigint DEFAULT NULL COMMENT '外键，关联到 folders.id，当分享目标是文件夹时填充',
  `expire_time` timestamp NULL DEFAULT NULL COMMENT '分享链接的过期时间，NULL表示永久有效',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '分享创建时间',
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '分享信息更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_share_token` (`share_token`),
  CONSTRAINT `fk_share_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_share_target` CHECK ((`file_id` IS NOT NULL AND `folder_id` IS NULL) OR (`file_id` IS NULL AND `folder_id` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理公开分享链接';

-- ----------------------------
-- 10. 传输日志表 (transfer_log)
-- 描述: 记录每一次上传和下载操作，为“传输列表”页面提供数据。
-- ----------------------------
CREATE TABLE `transfer_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '传输日志唯一ID (主键)',
  `user_id` bigint NOT NULL COMMENT '外键，关联到 users.id，标识操作用户',
  `transfer_type` enum('upload', 'download') NOT NULL COMMENT '传输类型：上传或下载',
  `file_id` bigint DEFAULT NULL COMMENT '外键，关联到 files.id (如果传输对象是单个文件)',
  `folder_id` bigint DEFAULT NULL COMMENT '外键，关联到 folders.id (如果传输对象是文件夹)',
  `item_name` varchar(255) NOT NULL COMMENT '被传输项目（文件/文件夹）的名称，做冗余以备查询',
  `item_size` bigint NOT NULL COMMENT '被传输项目的总大小，单位：字节 (Bytes)',
  `log_status` enum('processing', 'success', 'failed', 'canceled') NOT NULL DEFAULT 'processing' COMMENT '传输行为状态: 处理中, 成功, 失败, 已取消',
  `error_message` varchar(512) DEFAULT NULL COMMENT '当status为failed或canceled时，记录相关信息',
  `transfer_duration_ms` int DEFAULT NULL COMMENT '传输总耗时，单位：毫秒 (ms)',
  `client_ip` varchar(45) DEFAULT NULL COMMENT '发起传输的客户端IP地址，用于安全审计',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '传输任务创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '传输任务状态的最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status_time` (`user_id`, `log_status`, `create_time` DESC),
  CONSTRAINT `fk_tlog_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tlog_file` FOREIGN KEY (`file_id`) REFERENCES `files` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_tlog_folder` FOREIGN KEY (`folder_id`) REFERENCES `folders` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件上传下载行为日志表';