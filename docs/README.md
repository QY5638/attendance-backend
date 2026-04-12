# 项目文档索引

## 1. 文档说明

本目录用于集中维护“基于 SpringBoot 与大模型 API 的考勤异常检测与预警系统”项目资料。

- 当前运行、配置、测试、编译和 CI 口径，优先查看 [../README.md](../README.md)。
- 开发入口：先看需求、架构、模块边界，再看数据库、接口、测试文档。
- 模块开发指南入口：查看 [module-guides/README.md](module-guides/README.md)。
- 本地联调与人工验收入口：查看 [integration/phase5-local-runbook.md](integration/phase5-local-runbook.md) 和 [integration/phase5-acceptance-checklist.md](integration/phase5-acceptance-checklist.md)。
- 联调入口：优先查看 API 文档、测试文档、建表 SQL、测试数据 SQL。
- 论文与答辩入口：优先查看任务书、开题报告、需求文档、系统设计文档、统计与测试相关材料。
- 历史设计与计划入口：查看 [superpowers/README.md](superpowers/README.md)，用于回溯设计稿与实施计划；若与当前仓库行为不一致，以仓库根 `README.md` 和实际配置文件为准。

## 2. 当前口径优先级

- 本地启动、配置叠加、测试、编译和 CI 命令以 [../README.md](../README.md)、`../.github/workflows/backend-ci.yml`、`../src/main/resources/` 下的当前实现为准。
- 数据库初始化与默认库名以 `../sql/schema/数据库建表SQL.sql`、`../sql/seed/测试数据插入SQL.sql`、`../src/main/resources/application-dev.yml`、`../src/main/resources/application-prod.yml` 为准。
- 本地联调和人工验收口径以 `integration/phase5-local-runbook.md`、`integration/phase5-acceptance-checklist.md` 中的当前补充说明为准。

## 3. 推荐阅读顺序

1. [requirements/需求规格说明书.md](requirements/需求规格说明书.md)：明确系统目标、角色和核心功能。
2. [architecture/系统架构设计文档.md](architecture/系统架构设计文档.md)：了解系统分层、总体架构和技术选型。
3. [architecture/考勤异常检测与预警系统设计文档.md](architecture/考勤异常检测与预警系统设计文档.md)：了解整体系统设计与核心实现思路。
4. [architecture/模块划分说明书.md](architecture/模块划分说明书.md)：明确模块职责边界和模块关系。
5. [architecture/用例图与流程图文字说明.md](architecture/用例图与流程图文字说明.md)：了解主要业务流程与交互场景。
6. [database/数据库设计文档.md](database/数据库设计文档.md)：查看数据库对象设计与字段说明。
7. [database/数据库E-R关系说明.md](database/数据库E-R关系说明.md)：查看实体关系与表之间的关联。
8. [api/API接口设计文档.md](api/API接口设计文档.md)：查看接口路径、请求参数和统一返回结构。
9. [test/测试用例文档.md](test/测试用例文档.md)：查看测试目标、范围、环境与核心用例。
10. [../sql/schema/数据库建表SQL.sql](../sql/schema/数据库建表SQL.sql)：初始化数据库结构。
11. [../sql/seed/测试数据插入SQL.sql](../sql/seed/测试数据插入SQL.sql)：导入测试数据。

## 4. 分类索引

### 4.1 需求文档

- [requirements/需求规格说明书.md](requirements/需求规格说明书.md)
  - 用途：定义项目背景、目标、角色、功能需求和非功能需求。
  - 何时查看：开始开发新模块、核对需求边界、撰写论文需求分析章节时。

### 4.2 架构与设计文档

- [architecture/系统架构设计文档.md](architecture/系统架构设计文档.md)
  - 用途：说明系统总体架构、分层设计和技术选型。
  - 何时查看：确定前后端边界、服务职责、缓存与数据库关系时。
- [architecture/考勤异常检测与预警系统设计文档.md](architecture/考勤异常检测与预警系统设计文档.md)
  - 用途：补充系统级设计说明，便于整体把握方案。
  - 何时查看：整理系统设计章节、统一实现思路时。
- [architecture/模块划分说明书.md](architecture/模块划分说明书.md)
  - 用途：定义核心模块、子功能和职责边界。
  - 何时查看：拆分任务、设计 controller/service/mapper 分工时。
- [architecture/用例图与流程图文字说明.md](architecture/用例图与流程图文字说明.md)
  - 用途：补充业务流程和典型交互说明。
  - 何时查看：梳理登录、打卡、异常检测、复核流程时。

### 4.3 数据库文档

- [database/数据库设计文档.md](database/数据库设计文档.md)
  - 用途：说明核心表、字段和数据库选型。
  - 何时查看：新增实体、调整字段、实现 mapper 或 SQL 时。
- [database/数据库E-R关系说明.md](database/数据库E-R关系说明.md)
  - 用途：说明表之间的实体关系。
  - 何时查看：处理关联查询、外键关系、模块间数据流时。

### 4.4 接口文档

- [api/API接口设计文档.md](api/API接口设计文档.md)
  - 用途：定义 RESTful 接口路径、请求参数和响应结构。
  - 何时查看：新增接口、前后端联调、接口验收时。

### 4.5 测试文档

- [test/测试用例文档.md](test/测试用例文档.md)
  - 用途：说明测试目标、测试范围、测试环境与核心测试用例。
  - 何时查看：编写测试、联调验证、整理测试章节时。

### 4.6 模块开发指南

- [module-guides/README.md](module-guides/README.md)
  - 用途：汇总 `BE-01` 到 `BE-08` 的模块开发指南与共同约束。
  - 何时查看：准备实现某个后端业务模块、确认接口契约、表结构约束和最小验证范围时。

### 4.7 本地联调与验收

- [integration/phase5-local-runbook.md](integration/phase5-local-runbook.md)
  - 用途：说明前后端本地启动顺序、最小联调主链、单机摄像头部署口径、活体链路验证与阈值调优模板。
  - 何时查看：准备联调、排查本地环境、验证最小主链或校准活体挑战时。
- [integration/phase5-acceptance-checklist.md](integration/phase5-acceptance-checklist.md)
  - 用途：保留 Phase 5 验收清单与历史证据，同时补充单机摄像头方案的最终验收与活体调优记录项。
  - 何时查看：回溯历史验收证据、整理验收记录、补单机摄像头链路证据或对比当前口径与历史记录差异时。

### 4.8 参考资料

- [reference/222226209229-屈勇-基于 SpringBoot 与大模型 API 的考勤异常检测与预警系统设计与实现-任务书.docx](reference/222226209229-%E5%B1%88%E5%8B%87-%E5%9F%BA%E4%BA%8E%20SpringBoot%20%E4%B8%8E%E5%A4%A7%E6%A8%A1%E5%9E%8B%20API%20%E7%9A%84%E8%80%83%E5%8B%A4%E5%BC%82%E5%B8%B8%E6%A3%80%E6%B5%8B%E4%B8%8E%E9%A2%84%E8%AD%A6%E7%B3%BB%E7%BB%9F%E8%AE%BE%E8%AE%A1%E4%B8%8E%E5%AE%9E%E7%8E%B0-%E4%BB%BB%E5%8A%A1%E4%B9%A6.docx)
  - 用途：记录课题来源、任务要求和阶段目标。
  - 何时查看：核对毕设范围、答辩材料准备时。
- [reference/开题报告 （最终版）.docx](reference/%E5%BC%80%E9%A2%98%E6%8A%A5%E5%91%8A%20%EF%BC%88%E6%9C%80%E7%BB%88%E7%89%88%EF%BC%89.docx)
  - 用途：记录研究背景、研究内容、技术路线和进度安排。
  - 何时查看：整理研究思路、答辩陈述和论文绪论时。

### 4.9 历史设计与计划归档

- [superpowers/README.md](superpowers/README.md)
  - 用途：说明 `superpowers/` 目录中的历史设计稿与实施计划如何阅读，以及当前口径应以哪些文件为准。
  - 何时查看：需要回溯历史方案、阶段性实现计划或对比旧口径与当前口径差异时。

## 5. 文档同步规则

- 修改数据库结构时，必须同步更新：
  - [database/数据库设计文档.md](database/数据库设计文档.md)
  - [database/数据库E-R关系说明.md](database/数据库E-R关系说明.md)
  - [../sql/schema/数据库建表SQL.sql](../sql/schema/数据库建表SQL.sql)
  - [../sql/seed/测试数据插入SQL.sql](../sql/seed/测试数据插入SQL.sql)
- 修改接口时，必须同步更新：
  - [api/API接口设计文档.md](api/API接口设计文档.md)
- 修改核心模块边界或系统结构时，必须同步更新：
  - [architecture/系统架构设计文档.md](architecture/系统架构设计文档.md)
  - [architecture/模块划分说明书.md](architecture/模块划分说明书.md)
- 修改测试范围、测试环境或验证方式时，必须同步更新：
  - [test/测试用例文档.md](test/测试用例文档.md)

- 修改模块实现边界、任务拆分或模块级开发约束时，必须同步更新：
  - [module-guides/README.md](module-guides/README.md)

- 修改本地联调主链、启动顺序、最小人工验收步骤或当前口径说明时，必须同步更新：
  - [integration/phase5-local-runbook.md](integration/phase5-local-runbook.md)
  - [integration/phase5-acceptance-checklist.md](integration/phase5-acceptance-checklist.md)

## 6. SQL 入口

- [../sql/schema/数据库建表SQL.sql](../sql/schema/数据库建表SQL.sql)：数据库结构初始化脚本。
- [../sql/seed/测试数据插入SQL.sql](../sql/seed/测试数据插入SQL.sql)：测试数据初始化脚本。

## 7. 使用建议

- 开始新模块前，先确认需求文档、模块划分说明书和数据库文档是否一致。
- 进入具体后端模块开发前，先看 [module-guides/README.md](module-guides/README.md) 和对应的 `BE-xx` 指南。
- 做联调或人工验收前，先看 `integration/` 下的当前说明，再执行命令。
- 改动代码、SQL、接口后，按本页同步规则检查对应文档是否需要更新。
- 如果后续新增文档目录或关键资料，应先补充本索引，再开始扩展实现。
