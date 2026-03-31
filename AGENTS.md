# 毕设项目开发规范

## 项目定位

本项目为本科毕业设计项目，题目为“基于 SpringBoot 与大模型 API 的考勤异常检测与预警系统设计与实现”。

当前开发目录为 `D:\Graduation project\backend`。

当前毕设文档基线已迁入项目目录，后续编码、数据库设计、接口设计与测试验证默认参考以下本地资料：

- `D:\Graduation project\backend\docs\requirements\需求规格说明书.md`
- `D:\Graduation project\backend\docs\architecture\系统架构设计文档.md`
- `D:\Graduation project\backend\docs\architecture\考勤异常检测与预警系统设计文档.md`
- `D:\Graduation project\backend\docs\architecture\模块划分说明书.md`
- `D:\Graduation project\backend\docs\architecture\用例图与流程图文字说明.md`
- `D:\Graduation project\backend\docs\database\数据库设计文档.md`
- `D:\Graduation project\backend\docs\database\数据库E-R关系说明.md`
- `D:\Graduation project\backend\docs\api\API接口设计文档.md`
- `D:\Graduation project\backend\docs\test\测试用例文档.md`
- `D:\Graduation project\backend\sql\schema\数据库建表SQL.sql`
- `D:\Graduation project\backend\sql\seed\测试数据插入SQL.sql`

补充参考资料：

- `D:\Graduation project\backend\docs\reference\222226209229-屈勇-基于 SpringBoot 与大模型 API 的考勤异常检测与预警系统设计与实现-任务书.docx`
- `D:\Graduation project\backend\docs\reference\开题报告 （最终版）.docx`

## 语言与输出

- 默认使用中文回答，Thinking思考过程用中文表述，除非我明确要求英文。
- 生成的文档、说明、方案、报告使用中文。
- 代码注释使用中文，仅在必要时添加，避免无意义注释。
- 回答尽量简洁，先给结论，再给必要说明。
- 完成任务后，默认按以下结构输出：
  - 改动说明
  - 涉及文件
  - 验证命令
  - 风险点或后续建议

## 工作方式

- 修改代码前，先阅读相关文件，理解现有实现后再动手。
- 优先做最小必要改动，避免无关重构、顺手优化或过度设计。
- 优先沿用项目现有风格、目录结构、命名方式和技术方案。
- 能修改现有文件时，不要额外新建无关文件。
- 需求不明确时，先澄清关键业务规则，不要自行假设。
- 除非任务明确要求，否则不要主动更改已经确定的数据库命名、接口风格和系统边界。

## OpenCode 配置类问题

- 当任务涉及 OpenCode 配置、规则或行为定制时，例如 `AGENTS.md`、`opencode.json`、`instructions`、`rules`、`commands`、`agents`、`skills`、`tools`、`permissions`、`mcp`、`plugins`、`providers`、`models`、`themes`、`keybinds`，先读取 OpenCode 官方文档再回答或修改。
- 至少优先参考：
  - `https://opencode.ai/docs/`
  - `https://opencode.ai/docs/rules/`
- 若无法实时访问官方文档，必须明确说明未实时校验，再给出保守建议。

## 技术栈约束

### 后端

- 使用 `SpringBoot`。
- 构建方式使用 `Maven`。
- 根包名使用 `com.quyong.attendance`。
- 推荐分层结构：`controller`、`service`、`mapper`、`entity`、`dto`、`vo`。
- 接口采用 RESTful 风格。
- 统一返回结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 前端

- 使用 `Vue3 + Element Plus`。
- 推荐目录结构：`views`、`components`、`api`、`store`、`router`。
- 页面应覆盖：登录、员工管理、人脸录入、打卡、异常记录、预警、复核、统计报表。

### 数据库

- 数据库名固定为：`system`。
- 表名使用简化风格，例如：
  - `user`
  - `department`
  - `role`
  - `device`
  - `faceFeature`
  - `rule`
  - `attendanceRecord`
  - `attendanceException`
  - `exceptionAnalysis`
  - `warningRecord`
  - `reviewRecord`
  - `operationLog`
- 字段名使用小驼峰风格，例如：`deptId`、`roleId`、`createTime`、`checkTime`。
- 所有表和字段必须保留中文注释。

## 数据库变更规则

- 任何数据库结构变更，必须同步更新：
  - `D:\Graduation project\backend\sql\schema\数据库建表SQL.sql`
  - `D:\Graduation project\backend\sql\seed\测试数据插入SQL.sql`
  - `D:\Graduation project\backend\docs\database\数据库设计文档.md`
  - `D:\Graduation project\backend\docs\database\数据库E-R关系说明.md`
- 不允许只改代码不改 SQL。
- 不允许只改 SQL 不同步文档。
- 未经说明，不要擅自更改数据库名、核心表名、核心字段命名风格。

## 业务实现重点

系统核心模块必须围绕以下内容展开：

- 用户与权限管理
- 人脸采集与识别
- 考勤打卡
- 基础异常检测
- 复杂异常智能分析
- 风险预警
- 人工复核
- 统计分析与报表

复杂异常识别是本项目重点，必须突出以下场景：

- 代打卡或替打卡
- 多设备异常打卡
- 多地点异常打卡
- 异常行为组合判定

异常检测逻辑采用：

- 规则引擎处理基础异常
- 大模型处理复杂异常
- 人工复核兜底

## 安全要求

- 使用 `Spring Security` 或等价机制完成认证授权。
- 管理员与员工权限必须区分。
- 敏感数据应考虑加密存储，尤其是人脸特征和用户敏感信息。
- 大模型调用时注意脱敏，不直接暴露不必要的敏感原始数据。
- 不要为了调试方便绕过权限和安全校验。

## 开发顺序建议

如从零开始编码，优先按以下顺序推进：

1. 数据库与建表 SQL
2. 后端基础骨架（用户、权限、统一返回、异常处理）
3. 前端基础骨架（登录、布局、路由）
4. 用户管理与部门管理
5. 人脸录入与打卡功能
6. 基础异常检测
7. 复杂异常分析
8. 风险预警与人工复核
9. 统计分析与报表
10. 联调、测试、优化

## 工具与改动原则

- 读文件优先用 Read。
- 搜内容优先用 Grep。
- 搜文件优先用 Glob。
- 编辑优先做精确修改。
- 只有确实需要终端能力时再用 Bash。
- 修改代码前先读相关实现。
- 优先复用已有结构，不随意换框架、换库、换目录。

## 风险操作

以下操作属于高风险动作，执行前必须先确认：

- 删除文件
- 批量替换
- 覆盖已有大段改动
- 安装或卸载依赖
- 数据库结构大改
- 执行可能清空数据的 SQL
- 创建提交、推送、PR
- 修改 CI/CD、权限、安全配置

除非我明确要求，否则不要主动：

- `git commit`
- `git push`
- 创建 PR
- 发布版本
- 修改 CI/CD

## 验证规则

- 完成改动后，优先运行与本次改动直接相关的最小验证。
- 未执行验证命令，不得声称“已完成”“已修复”“可用”。
- 如果无法实际验证，必须明确说明原因，并给出可执行的验证命令。
- 验证应优先覆盖：
  - 后端编译或启动检查
  - 前端构建或页面相关检查
  - 接口调用
  - SQL 文件结构检查
  - 关键业务流程联调

## 文档同步规则

以下变更必须同步文档：

- 改数据库结构：同步数据库文档与 SQL
- 改接口：同步 `D:\Graduation project\backend\docs\api\API接口设计文档.md`
- 改核心模块边界：同步 `D:\Graduation project\backend\docs\architecture\系统架构设计文档.md` 或 `D:\Graduation project\backend\docs\architecture\模块划分说明书.md`
- 改测试范围或验证方式：同步 `D:\Graduation project\backend\docs\test\测试用例文档.md`

## 编码目标

所有实现都优先满足以下目标：

- 能跑通
- 易维护
- 命名一致
- 结构清晰
- 贴合毕设文档
- 便于论文撰写与答辩展示

如文档与代码实现冲突，先分析差异，再用最小代价保持一致，不要直接推翻现有设计。
