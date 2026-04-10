# 后端模块开发指南索引

## 1. 使用方式

- 当前后端文档总入口：`D:\Graduation project\backend\docs\README.md`
- 当前运行、配置、测试、编译和 CI 口径以 `D:\Graduation project\backend\README.md` 为准。
- 开发任一后端模块前，先读取：
  - `D:\Graduation project\backend\AGENTS.md`
  - `D:\Graduation project\backend\docs\progress\后端并发开发模块清单.md`
  - `D:\Graduation project\backend\docs\api\API接口设计文档.md`
- 然后读取本目录对应的 `BE-xx-*.md` 文件。
- 每次只认领一个模块编号，避免跨模块顺手开发。
- 若接口、字段、表结构发生变化，必须同步 API、数据库、测试文档。

## 2. 模块文件清单

1. `BE-01-用户与权限基础模块开发指南.md`
2. `BE-02-设备基础资料模块开发指南.md`
3. `BE-03-人脸采集与识别模块开发指南.md`
4. `BE-04-考勤打卡与记录模块开发指南.md`
5. `BE-05-异常检测与智能分析模块开发指南.md`
6. `BE-06-风险预警模块开发指南.md`
7. `BE-07-人工复核模块开发指南.md`
8. `BE-08-统计分析与审计模块开发指南.md`

## 3. 推荐顺序

1. `BE-01`
2. `BE-02`
3. `BE-03`
4. `BE-04`
5. `BE-05`
6. `BE-06`
7. `BE-07`
8. `BE-08`

## 4. 共同约束

- 接口契约优先以 `API接口设计文档.md` 为准。
- 数据库命名保持：数据库 `system`、表名简化、字段小驼峰。
- `device` 表主键当前为 `id`，接口层统一对外使用 `deviceId`。
- 未经说明，不要擅自更改核心表名、核心字段、系统边界。
- 完成每个模块后，至少执行与该模块直接相关的最小验证。
