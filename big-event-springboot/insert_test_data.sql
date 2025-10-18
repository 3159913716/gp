-- Big Event项目测试数据插入脚本
-- 创建日期：2024-01-01
-- 注意：执行前请确保所有表已创建

-- 1. 插入user表数据（基础表，无外键约束）
INSERT INTO `user` (`username`, `password`, `nickname`, `email`, `user_pic`, `create_time`, `update_time`, `role`, `status`)
VALUES 
('admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 'admin@example.com', 'https://example.com/avatar/admin.jpg', '2024-01-01 00:00:00', '2024-01-01 00:00:00', 0, 0),
('author1', 'e10adc3949ba59abbe56e057f20f883e', '技术达人', 'author1@example.com', 'https://example.com/avatar/author1.jpg', '2024-01-02 00:00:00', '2024-01-02 00:00:00', 1, 0),
('author2', 'e10adc3949ba59abbe56e057f20f883e', '编程爱好者', 'author2@example.com', 'https://example.com/avatar/author2.jpg', '2024-01-03 00:00:00', '2024-01-03 00:00:00', 1, 0),
('user1', 'e10adc3949ba59abbe56e057f20f883e', '普通用户1', 'user1@example.com', 'https://example.com/avatar/user1.jpg', '2024-01-04 00:00:00', '2024-01-04 00:00:00', 2, 0),
('user2', 'e10adc3949ba59abbe56e057f20f883e', '普通用户2', 'user2@example.com', 'https://example.com/avatar/user2.jpg', '2024-01-05 00:00:00', '2024-01-05 00:00:00', 2, 0),
('user3', 'e10adc3949ba59abbe56e057f20f883e', '普通用户3', 'user3@example.com', 'https://example.com/avatar/user3.jpg', '2024-01-06 00:00:00', '2024-01-06 00:00:00', 2, 0);

-- 2. 插入category表数据（受user表外键约束）
INSERT INTO `category` (`category_name`, `category_alias`, `create_user`, `create_time`, `update_time`)
VALUES 
('技术博客', 'tech', 1, '2024-01-01 00:00:00', '2024-01-01 00:00:00'),
('生活随笔', 'life', 1, '2024-01-01 00:00:00', '2024-01-01 00:00:00'),
('职场经验', 'career', 2, '2024-01-02 00:00:00', '2024-01-02 00:00:00'),
('学习笔记', 'notes', 3, '2024-01-03 00:00:00', '2024-01-03 00:00:00'),
('行业资讯', 'news', 3, '2024-01-03 00:00:00', '2024-01-03 00:00:00');

-- 3. 插入article表数据（受category表和user表外键约束）
INSERT INTO `article` (`title`, `content`, `cover_img`, `state`, `category_id`, `create_user`, `create_time`, `update_time`, `like_count`, `collect_count`)
VALUES 
('Spring Boot入门', 'Spring Boot是由Pivotal团队提供的全新框架，其设计目的是用来简化新Spring应用的初始搭建以及开发过程。', 'https://example.com/cover/springboot.jpg', '已发布', 1, 2, '2024-01-07 10:00:00', '2024-01-07 10:00:00', 25, 15),
('MySQL优化技巧', '本文介绍了MySQL数据库优化的常用技巧，包括索引优化、查询优化等。', 'https://example.com/cover/mysql.jpg', '已发布', 1, 2, '2024-01-08 14:30:00', '2024-01-08 14:30:00', 18, 10),
('程序员的一天', '记录了一名普通程序员的工作日常，从早到晚的工作内容和感悟。', 'https://example.com/cover/daily.jpg', '已发布', 2, 3, '2024-01-09 09:15:00', '2024-01-09 09:15:00', 32, 20),
('面试经验分享', '分享了最近一次技术面试的经历和准备过程，希望对大家有所帮助。', 'https://example.com/cover/interview.jpg', '已发布', 3, 3, '2024-01-10 16:45:00', '2024-01-10 16:45:00', 45, 30),
('Java学习笔记', '整理了Java语言学习过程中的重要知识点和常见问题。', 'https://example.com/cover/java.jpg', '已发布', 4, 2, '2024-01-11 11:20:00', '2024-01-11 11:20:00', 15, 25),
('AI发展趋势', '探讨了人工智能技术的最新发展趋势和未来展望。', 'https://example.com/cover/ai.jpg', '草稿', 5, 2, '2024-01-12 08:50:00', '2024-01-12 08:50:00', 0, 0);

-- 4. 插入article_collect表数据（受user表和article表外键约束）
INSERT INTO `article_collect` (`user_id`, `article_id`, `collect_time`, `is_deleted`)
VALUES 
(4, 1, '2024-01-13 10:00:00', 0),
(4, 2, '2024-01-13 10:15:00', 0),
(4, 3, '2024-01-13 10:30:00', 1), -- 逻辑删除的收藏记录
(5, 1, '2024-01-13 11:00:00', 0),
(5, 4, '2024-01-13 11:15:00', 0),
(6, 2, '2024-01-13 12:00:00', 0),
(6, 3, '2024-01-13 12:15:00', 0),
(6, 5, '2024-01-13 12:30:00', 0);

-- 5. 插入article_like表数据（受article表和user表外键约束）
INSERT INTO `article_like` (`article_id`, `user_id`, `create_time`, `update_time`, `is_deleted`)
VALUES 
(1, 4, '2024-01-13 09:00:00', '2024-01-13 09:00:00', 0),
(1, 5, '2024-01-13 09:15:00', '2024-01-13 09:15:00', 0),
(2, 4, '2024-01-13 09:30:00', '2024-01-13 09:30:00', 0),
(2, 6, '2024-01-13 09:45:00', '2024-01-13 09:45:00', 1), -- 逻辑删除的点赞记录
(3, 5, '2024-01-13 10:00:00', '2024-01-13 10:00:00', 0),
(3, 6, '2024-01-13 10:15:00', '2024-01-13 10:15:00', 0),
(4, 4, '2024-01-13 10:30:00', '2024-01-13 10:30:00', 0),
(4, 5, '2024-01-13 10:45:00', '2024-01-13 10:45:00', 0);

-- 6. 插入user_follow表数据（受user表外键约束）
INSERT INTO `user_follow` (`follower_id`, `followed_id`, `create_time`, `is_deleted`)
VALUES 
(4, 2, '2024-01-14 08:00:00', 0), -- user1关注author1
(4, 3, '2024-01-14 08:15:00', 0), -- user1关注author2
(5, 2, '2024-01-14 09:00:00', 0), -- user2关注author1
(5, 4, '2024-01-14 09:15:00', 0), -- user2关注user1
(6, 3, '2024-01-14 10:00:00', 0), -- user3关注author2
(6, 4, '2024-01-14 10:15:00', 1), -- 逻辑删除的关注记录（user3取消关注user1）
(2, 3, '2024-01-14 11:00:00', 0); -- author1关注author2

-- 7. 插入author_apply表数据（受user表外键约束）
INSERT INTO `author_apply` (`user_id`, `real_name`, `id_card`, `apply_desc`, `status`, `create_time`, `audit_time`, `audit_user_id`, `reject_reason`)
VALUES 
(4, '张三', '310101199001010001', '我有丰富的技术写作经验，希望成为平台作者。', 1, '2024-01-15 09:00:00', '2024-01-15 10:00:00', 1, NULL),
(5, '李四', '310101199002020002', '希望通过写作分享我的学习心得。', 2, '2024-01-16 09:00:00', '2024-01-16 10:00:00', 1, '申请材料不完整，请补充相关资质证明。'),
(6, '王五', '310101199003030003', '热爱技术分享，有自己的技术博客。', 0, '2024-01-17 09:00:00', NULL, NULL, NULL);

-- 数据插入完成提示
SELECT '测试数据插入完成' AS result;