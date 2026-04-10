USE `system`;
SET NAMES utf8mb4;

-- 说明：
-- 1. 本脚本用于给“已存在数据”的数据库增量补充部门与演示员工。
-- 2. 使用 INSERT IGNORE，重复执行不会报主键冲突。
-- 3. 如当前数据库中不存在这些角色、设备或表结构，请先执行建表脚本与基础种子脚本。

INSERT IGNORE INTO `department` (`id`, `name`, `description`) VALUES
(4, '财务部', '负责财务核算与预算控制'),
(5, '人力资源部', '负责招聘培训与员工关系'),
(6, '市场部', '负责市场推广与品牌活动'),
(7, '运营部', '负责运营协调与流程优化'),
(8, '客服部', '负责客户咨询与工单处理'),
(9, '法务合规部', '负责合同审核与合规管理');

INSERT IGNORE INTO `user` (`id`, `username`, `password`, `realName`, `gender`, `phone`, `deptId`, `roleId`, `status`, `createTime`) VALUES
(1003, 'wangwu', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '王五', '男', '13800000004', 4, 2, 1, '2026-03-01 08:30:00'),
(1004, 'zhaoliu', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '赵六', '女', '13800000005', 5, 2, 1, '2026-03-01 08:40:00'),
(1005, 'sunqi', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '孙七', '男', '13800000006', 6, 2, 1, '2026-03-01 08:50:00'),
(1006, 'zhouba', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '周八', '女', '13800000007', 7, 2, 1, '2026-03-01 09:00:00'),
(1007, 'wuxiao', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '吴晓', '女', '13800000008', 8, 2, 1, '2026-03-01 09:10:00'),
(1008, 'zhengjiu', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '郑久', '男', '13800000009', 9, 2, 1, '2026-03-01 09:20:00'),
(1009, 'chenyu', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '陈宇', '男', '13800000010', 2, 2, 1, '2026-03-01 09:30:00'),
(1010, 'liuyang', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '刘洋', '男', '13800000011', 2, 2, 1, '2026-03-01 09:40:00'),
(1011, 'hejing', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '何静', '女', '13800000012', 3, 2, 1, '2026-03-01 09:50:00'),
(1012, 'pengfei', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '彭飞', '男', '13800000013', 4, 2, 1, '2026-03-01 10:00:00'),
(1013, 'denglin', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '邓琳', '女', '13800000014', 5, 2, 1, '2026-03-01 10:10:00'),
(1014, 'xiaomin', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '晓敏', '女', '13800000015', 6, 2, 1, '2026-03-01 10:20:00'),
(1015, 'gaoyuan', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '高远', '男', '13800000016', 7, 2, 1, '2026-03-01 10:30:00'),
(1016, 'tianxin', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '田欣', '女', '13800000017', 8, 2, 1, '2026-03-01 10:40:00'),
(1017, 'guole', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '国乐', '男', '13800000018', 9, 2, 1, '2026-03-01 10:50:00'),
(1018, 'linfan', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '林帆', '男', '13800000019', 6, 2, 1, '2026-03-01 11:00:00'),
(1019, 'yaonan', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '姚楠', '女', '13800000020', 7, 2, 1, '2026-03-01 11:10:00'),
(1020, 'qiaorui', '$2a$10$TiGjwQPnktIxrPqz6JoTq..Ur4rmqn9zDtlvRvbZWIqcSDXGLQJZm', '乔蕊', '女', '13800000021', 5, 2, 1, '2026-03-01 11:20:00');

INSERT IGNORE INTO `attendanceRecord` (`id`, `userId`, `checkTime`, `checkType`, `deviceId`, `ipAddr`, `location`, `faceScore`, `status`, `createTime`) VALUES
(2006, 1003, '2026-03-26 08:57:00', 'IN', 'DEV-002', '192.168.1.103', '办公区B', 97.30, 'NORMAL', '2026-03-26 08:57:00'),
(2007, 1004, '2026-03-26 08:59:00', 'IN', 'DEV-001', '192.168.1.104', '办公区A', 96.90, 'NORMAL', '2026-03-26 08:59:00'),
(2008, 1005, '2026-03-26 09:01:00', 'IN', 'DEV-002', '192.168.1.105', '办公区B', 95.80, 'NORMAL', '2026-03-26 09:01:00'),
(2009, 1006, '2026-03-26 08:56:00', 'IN', 'DEV-001', '192.168.1.106', '办公区A', 97.60, 'NORMAL', '2026-03-26 08:56:00'),
(2010, 1007, '2026-03-26 08:55:30', 'IN', 'DEV-002', '192.168.1.107', '办公区B', 98.10, 'NORMAL', '2026-03-26 08:55:30'),
(2011, 1008, '2026-03-26 08:58:40', 'IN', 'DEV-001', '192.168.1.108', '办公区A', 96.70, 'NORMAL', '2026-03-26 08:58:40');
