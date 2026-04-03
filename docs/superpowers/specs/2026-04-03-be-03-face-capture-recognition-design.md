# BE-03 人脸采集与识别模块设计文档

## 1. 目标

在现有后端骨架上补齐 `face` 模块的最小必要能力，形成员工人脸录入、特征保存和身份验证闭环，为后续 `BE-04` 打卡模块稳定提供 `registered`、`matched` 和 `faceScore` 等前置验证结果。

本次设计只覆盖 `BE-03` 人脸采集与识别模块，不扩展到打卡记录生成、异常判定、预警、复核和统计分析，也不在本轮引入真实第三方人脸识别 SDK 或云服务。

## 2. 约束与边界

### 2.1 已确认约束

- 开发范围固定为 `src/main/java/com/quyong/attendance/module/face/` 及其直接相关测试。
- 沿用当前项目结构、统一返回、异常处理和安全链路。
- 本轮识别实现采用“本地可插拔识别内核”，同时预留第三方识别服务封装点。
- 人脸重复录入时新增一条 `faceFeature` 记录，业务上只使用“最新一条”作为当前生效模板。
- `POST /api/face/verify` 本轮冻结为最小稳定契约：`userId + imageData -> userId、registered、matched、faceScore、threshold、message`。
- 本轮继续沿用当前管理员权限边界，不调整 `SecurityConfig`，避免越界修改全局安全策略。
- 本轮优先不修改数据库表结构，继续复用现有 `faceFeature` 表。
- 不向前端返回原始特征数据或原始图片内容。

### 2.2 业务规则

- 固定接口范围：`POST /api/face/register`、`POST /api/face/verify`。
- `register` 用于为指定员工录入新的人脸模板。
- `verify` 用于校验指定员工与当前上传图像是否匹配。
- `register`、`verify` 的 `userId` 均必须指向已存在员工。
- `imageData` 为空白时视为参数错误。
- 员工未录入人脸时，`verify` 不抛业务异常，返回 `registered=false`、`matched=false`。
- 员工已录入但匹配失败时，`verify` 不抛业务异常，返回 `registered=true`、`matched=false`。
- 员工已录入且匹配成功时，返回 `registered=true`、`matched=true` 以及本次 `faceScore`。
- 模块内部维护固定阈值，暂不做系统配置化。

### 2.3 非目标

- 不在本模块生成考勤记录。
- 不在本模块做异常判定、复杂异常分析或预警联动。
- 不开放员工自助访问权限。
- 不引入摄像头采集、活体检测等前端或硬件能力。
- 不新增详情、分页、历史列表接口。
- 不调整 `faceFeature` 表结构。

## 3. 数据与契约设计

### 3.1 表结构复用

继续使用现有 `faceFeature` 表：

- `id`
- `userId`
- `featureData`
- `featureHash`
- `encryptFlag`
- `createTime`

字段使用约定：

- `featureData` 保存提取后的人脸特征串，不保存原始 `imageData`
- `featureHash` 保存特征摘要，便于快速校验和追踪
- `encryptFlag` 本轮固定写入 `1`
- 最新一条 `createTime` 对应记录视为当前生效模板

### 3.2 请求与响应模型

- `FaceRegisterDTO`
  - `userId`
  - `imageData`
- `FaceVerifyDTO`
  - `userId`
  - `imageData`
- `FaceRegisterVO`
  - `userId`
  - `registered`
  - `message`
  - `createTime`
- `FaceVerifyVO`
  - `userId`
  - `registered`
  - `matched`
  - `faceScore`
  - `threshold`
  - `message`

### 3.3 返回语义

- 成功：HTTP `200`，响应体 `code=200`、`message=success`
- 参数或业务校验失败：HTTP `200`，响应体 `code=400`
- 未登录、token 无效、token 过期：HTTP `401`
- 已登录但角色不足：HTTP `403`

建议业务消息：

- `用户不存在`
- `用户编号不能为空`
- `人脸图像不能为空`
- `人脸录入成功`
- `该用户未录入人脸`
- `人脸验证通过`
- `人脸验证未通过`

### 3.4 接口说明

#### 3.4.1 `POST /api/face/register`

请求示例：

```json
{
  "userId": 1001,
  "imageData": "base64..."
}
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1001,
    "registered": true,
    "message": "人脸录入成功",
    "createTime": "2026-04-03T10:00:00"
  }
}
```

#### 3.4.2 `POST /api/face/verify`

请求示例：

```json
{
  "userId": 1001,
  "imageData": "base64..."
}
```

已注册且通过示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1001,
    "registered": true,
    "matched": true,
    "faceScore": 96.8,
    "threshold": 85.0,
    "message": "人脸验证通过"
  }
}
```

未注册示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1001,
    "registered": false,
    "matched": false,
    "faceScore": 0,
    "threshold": 85.0,
    "message": "该用户未录入人脸"
  }
}
```

## 4. 模块职责设计

### 4.1 Controller

`FaceController` 负责：

- 接收 `register`、`verify` 请求
- 绑定 DTO
- 调用 `FaceService`
- 使用统一 `Result.success(...)` 返回响应

控制器不承载校验、特征提取、比对和阈值判断逻辑。

### 4.2 Service

`FaceService` / `FaceServiceImpl` 负责：

- 编排人脸录入流程
- 编排人脸验证流程
- 查询并使用用户最新的人脸模板
- 完成实体与 VO 的映射

### 4.3 校验支撑组件

新增轻量组件 `FaceValidationSupport`，负责：

- 校验 `userId` 非空
- 校验 `imageData` 非空白
- 校验用户存在
- 查询用户最新有效人脸模板
- 统一参数归一化

该组件只承载校验与查询，不直接落库存储。

### 4.4 识别提供器

新增可插拔接口 `FaceRecognitionProvider`，负责：

- 从 `imageData` 提取特征
- 基于输入图像与已存模板执行比对
- 产出 `faceScore`

默认提供 `LocalFaceRecognitionProvider` 本地实现：

- 基于稳定、可重复的字符串特征提取规则生成特征串
- 通过本地算法计算相似度分值
- 不依赖第三方 SDK

后续如接第三方服务，只需替换该提供器实现，不改控制器和主业务编排。

### 4.5 Mapper

- `FaceFeatureMapper` 基于 `BaseMapper<FaceFeature>`
- 额外提供“按用户查询最新一条人脸模板”的方法
- 优先使用注解 SQL 或 MyBatis-Plus 能力，不新增 XML

## 5. 业务流程

### 5.1 人脸录入流程

1. 控制器接收 `FaceRegisterDTO`
2. 校验 `userId`、`imageData`
3. 校验用户存在
4. 调用 `FaceRecognitionProvider.extractFeature(imageData)` 提取特征
5. 生成 `featureHash`
6. 组装 `FaceFeature` 并插入数据库
7. 返回 `FaceRegisterVO`

重复录入时不覆盖旧记录，而是新增一条记录；后续验证统一读取最新一条。

### 5.2 人脸验证流程

1. 控制器接收 `FaceVerifyDTO`
2. 校验 `userId`、`imageData`
3. 校验用户存在
4. 查询该用户最新一条 `faceFeature`
5. 若不存在，直接返回 `registered=false`、`matched=false`
6. 若存在，调用 `FaceRecognitionProvider.compare(imageData, featureData)` 计算 `faceScore`
7. 将 `faceScore` 与固定阈值比较，得到 `matched`
8. 返回 `FaceVerifyVO`

## 6. 本地识别策略

### 6.1 设计原则

- 结果必须稳定、可重复，便于测试与联调
- 不引入额外依赖
- 对外暴露方式保持可替换
- 不假装是真实生产级识别算法

### 6.2 建议实现方式

- 先对 `imageData` 做去空白归一化
- 使用摘要算法生成稳定特征串
- 以摘要结果构造可存储的 `featureData`
- 比对时基于归一化后摘要的相似度规则计算 `faceScore`
- 统一使用固定阈值，例如 `85.0`

该策略的核心目的是提供稳定工程契约，而不是模拟真实生物识别精度。

## 7. 错误处理与风险说明

- 参数缺失、用户不存在：抛出 `BusinessException`
- 畸形 JSON：沿用 `GlobalExceptionHandler` 返回 `请求参数错误`
- 未授权或权限不足：沿用现有安全链路返回 `401/403`
- 未录入和比对失败：正常返回结果对象，不抛异常

本次明确不处理以下扩展问题：

- 多模板并行比对取最高分
- 活体检测
- 跨用户自动识别归属
- 特征加密算法增强
- 打卡流程联动鉴权调整

## 8. 测试设计

### 8.1 测试方式

优先使用接口级集成测试覆盖完整链路：

- `@SpringBootTest`
- `@AutoConfigureMockMvc`
- `@ActiveProfiles("test")`
- `JdbcTemplate` 准备与清理测试数据

### 8.2 核心通过用例

- 管理员为存在用户录入人脸成功
- 同一用户重复录入后新增新记录成功
- 已录入用户验证成功并返回 `matched=true`
- 验证成功时返回 `faceScore` 与 `threshold`

### 8.3 核心失败或边界用例

- 录入时 `userId` 为空
- 录入时 `imageData` 为空白
- 录入时用户不存在
- 验证时用户未注册，返回 `registered=false`
- 验证时已注册但分数不足，返回 `matched=false`
- 畸形 JSON 返回 `请求参数错误`
- 未登录访问返回 `401`
- 员工访问返回 `403`

## 9. 验证策略

最小验证命令：

```bash
mvn -Dtest=FaceManagementIntegrationTest test
```

如集成测试通过，再视实际改动决定是否补跑 `mvn -DskipTests compile`。

## 10. 工作树建议

当前主仓库已存在与本次任务无关的工作区变动。为避免后续编码阶段受到其他模块影响，进入实现前建议创建独立 worktree，例如：

- 分支名：`feature/be-03-face-module`
- worktree 目录：`D:\Graduation project\backend\.worktrees\be-03-face-module`

本设计文档阶段不执行该操作；如用户确认进入实现，再在实现计划中将其作为首步落地。
