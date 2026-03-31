# 项目文档索引

## 1. 文档说明

本目录用于集中维护“基于 SpringBoot 与大模型 API 的考勤异常检测与预警系统”项目资料。

- 开发入口：先看需求、架构、模块边界，再看数据库、接口、测试文档。
- 联调入口：优先查看 API 文档、测试文档、建表 SQL、测试数据 SQL。
- 论文与答辩入口：优先查看任务书、开题报告、需求文档、系统设计文档、统计与测试相关材料。

## 2. 推荐阅读顺序

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

## 3. 分类索引

### 3.1 需求文档

- [requirements/需求规格说明书.md](requirements/需求规格说明书.md)
  - 用途：定义项目背景、目标、角色、功能需求和非功能需求。
  - 何时查看：开始开发新模块、核对需求边界、撰写论文需求分析章节时。

### 3.2 架构与设计文档

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

### 3.3 数据库文档

- [database/数据库设计文档.md](database/数据库设计文档.md)
  - 用途：说明核心表、字段和数据库选型。
  - 何时查看：新增实体、调整字段、实现 mapper 或 SQL 时。
- [database/数据库E-R关系说明.md](database/数据库E-R关系说明.md)
  - 用途：说明表之间的实体关系。
  - 何时查看：处理关联查询、外键关系、模块间数据流时。

### 3.4 接口文档

- [api/API接口设计文档.md](api/API接口设计文档.md)
  - 用途：定义 RESTful 接口路径、请求参数和响应结构。
  - 何时查看：新增接口、前后端联调、接口验收时。

### 3.5 测试文档

- [test/测试用例文档.md](test/测试用例文档.md)
  - 用途：说明测试目标、测试范围、测试环境与核心测试用例。
  - 何时查看：编写测试、联调验证、整理测试章节时。

### 3.6 参考资料

- [reference/222226209229-屈勇-基于 SpringBoot 与大模型 API 的考勤异常检测与预警系统设计与实现-任务书.docx](reference/222226209229-%E5%B1%88%E5%8B%87-%E5%9F%BA%E4%BA%8E%20SpringBoot%20%E4%B8%8E%E5%A4%A7%E6%A8%A1%E5%9E%8B%20API%20%E7%9A%84%E8%80%83%E5%8B%A4%E5%BC%82%E5%B8%B8%E6%A3%80%E6%B5%8B%E4%B8%8E%E9%A2%84%E8%AD%A6%E7%B3%BB%E7%BB%9F%E8%AE%BE%E8%AE%A1%E4%B8%8E%E5%AE%9E%E7%8E%B0-%E4%BB%BB%E5%8A%A1%E4%B9%A6.docx)
  - 用途：记录课题来源、任务要求和阶段目标。
  - 何时查看：核对毕设范围、答辩材料准备时。
- [reference/开题报告 （最终版）.docx](reference/%E5%BC%80%E9%A2%98%E6%8A%A5%E5%91%8A%20%EF%BC%88%E6%9C%80%E7%BB%88%E7%89%88%EF%BC%89.docx)
  - 用途：记录研究背景、研究内容、技术路线和进度安排。
  - 何时查看：整理研究思路、答辩陈述和论文绪论时。

## 4. 文档同步规则

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

## 5. SQL 入口

- [../sql/schema/数据库建表SQL.sql](../sql/schema/数据库建表SQL.sql)：数据库结构初始化脚本。
- [../sql/seed/测试数据插入SQL.sql](../sql/seed/测试数据插入SQL.sql)：测试数据初始化脚本。

## 6. 使用建议

- 开始新模块前，先确认需求文档、模块划分说明书和数据库文档是否一致。
- 改动代码、SQL、接口后，按本页同步规则检查对应文档是否需要更新。
- 如果后续新增文档目录或关键资料，应先补充本索引，再开始扩展实现。
