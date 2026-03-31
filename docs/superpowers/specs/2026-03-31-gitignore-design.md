# Git 忽略规则设计说明

## 1. 目标

在项目根目录新增 `D:\Graduation project\.gitignore`，统一忽略当前已经出现的构建产物、IDE 元数据和近期高概率出现的前端生成文件，避免无关文件污染版本管理记录。

## 2. 设计范围

本次设计仅覆盖根目录 `.gitignore` 的规则定义，不涉及业务代码、SQL、接口、测试逻辑或目录重构。

包含内容：

- 在项目根目录新增 `.gitignore`。
- 忽略后端 Maven 构建产物与 IntelliJ IDEA 元数据。
- 忽略前端近期高概率出现的依赖目录、打包输出和本地环境文件。
- 忽略常见日志、临时文件与系统缓存文件。

不包含内容：

- 不删除任何已有文件。
- 不自动清理已经存在于工作区的生成物目录。
- 不修改 `.git` 配置。
- 不引入全量通用模板中当前项目暂时用不到的大量规则。

## 3. 项目现状

当前仓库已初始化为本地 Git 仓库，且已观察到以下内容：

- 已出现：`backend/target/`
- 已出现：`backend/.idea/`
- 前端目录已存在：`frontend/src/`、`frontend/public/`
- 前端尚未出现但近期高概率生成：`frontend/node_modules/`、`frontend/dist/`

因此采用“当前+近期”策略最合适：既覆盖已出现的无关文件，也预留前端初始化后的常见生成物规则。

## 4. 文件设计

### 4.1 新增文件

- `D:\Graduation project\.gitignore`

职责：作为仓库根级统一忽略规则文件，集中管理当前项目的版本忽略策略。

## 5. 规则设计

`.gitignore` 采用分组写法，保持可读、可维护。

### 5.1 后端构建与 IDE 产物

忽略以下内容：

- `backend/target/`
- `backend/.idea/`

原因：二者均属于本地构建或 IDE 生成内容，不应纳入版本管理。

### 5.2 前端近期生成物

忽略以下内容：

- `frontend/node_modules/`
- `frontend/dist/`
- `frontend/.env.local`
- `frontend/.env.*.local`

原因：这些内容属于依赖安装产物、本地打包输出或开发机私有环境变量，不适合提交到仓库。

### 5.3 通用日志、临时文件与系统缓存

忽略以下内容：

- `*.log`
- `*.tmp`
- `.DS_Store`
- `Thumbs.db`

原因：这些文件与项目源码无关，容易造成提交噪声。

## 6. 明确保留跟踪的内容

以下内容应继续纳入版本管理，不写入 `.gitignore`：

- `AGENTS.md`
- `backend/src/`
- `backend/pom.xml`
- `frontend/src/`
- `frontend/public/`
- `docs/`
- `sql/`
- `scripts/`

## 7. 设计原则

- 优先最小必要规则，不引入与当前项目无关的大而全模板。
- 根目录统一管理，避免后续在子目录分散新增忽略文件。
- 先解决已经出现的噪声，再覆盖近期高概率出现的前端生成物。
- 保持规则语义清晰，便于后续维护和审查。

## 8. 验证方式

实现完成后，至少执行以下检查：

- 确认 `D:\Graduation project\.gitignore` 已创建。
- 使用 `git status --short` 检查 `backend/target/` 与 `backend/.idea/` 不再显示为未跟踪内容。
- 核对 `.gitignore` 未误伤 `AGENTS.md`、`docs/`、`sql/`、`backend/src/` 等应保留的项目文件。
- 如前端后续生成 `node_modules/` 或 `dist/`，再次使用 `git status --short` 验证忽略规则生效。

## 9. 预期结果

实现完成后，仓库状态应更聚焦于真实项目文件：

- 提交列表不再混入后端构建产物。
- IDEA 元数据不再出现在版本控制视图中。
- 前端后续初始化后，无需再次补充最常见的忽略规则。
- 仓库根目录的忽略策略清晰、集中、便于后续扩展。
