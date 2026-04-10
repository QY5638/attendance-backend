# Superpowers 文档说明

本目录保存历史设计稿与实施计划，主要用于回溯需求拆解、设计取舍和实施过程，不作为当前运行口径的唯一来源。

## 当前口径优先级

- 当前数据库默认回退库名以 `src/main/resources/application-dev.yml`、`src/main/resources/application-prod.yml` 和 `sql/schema/数据库建表SQL.sql` 为准；当前默认值已对齐为 `system`。
- 当前后端 CI 以 `.github/workflows/backend-ci.yml` 为准；当前执行 `mvn test` 全量回归。
- 当前本地启动、联调与人工验证口径以仓库根 `README.md` 和 `docs/integration/phase5-local-runbook.md` 为准。

## 阅读建议

- 需要查看历史方案、拆解过程和阶段性取舍时，阅读 `specs/` 与 `plans/`。
- 如果历史文档中的测试命令、配置默认值或环境说明与当前仓库行为不一致，优先采用“当前口径优先级”中的文件，不逐篇回写旧记录。
