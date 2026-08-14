# Directory Structure Guide

本文档是当前**物理目录**的唯一说明。服务边界、数据归属、支付退款与 WebSocket 以 [架构基线与服务边界 v2](docs/design/充电运营平台架构基线与服务边界-v2.md) 为准；Kafka Topic 与消费组以 [事件目录](contracts/events/README.md) 为准；工程目录与 schema 落位参考 [工程架构与数据分库设计 v1.6](docs/design/充电运营平台工程架构与数据分库设计-v1.md)，命名以 [平台技术命名规范 v1](docs/standards/平台技术命名规范-v1.md) 为准。

当前项目已完成目录对齐，但仍处于设计与架构跑道阶段：目录中的 README 是工程入口说明，不表示已经存在可运行的业务实现。

```text
ev-charging-operations/
├── README.md                         # 项目总览
├── STRUCTURE.md                      # 本文件
├── .agents/                          # 项目领域评审角色定义
├── docs/                             # 当前有效的需求、设计、API、集成、规范、手册
├── archive/                          # 已替代/退役资料；不作为开发依据
├── backend/
│   ├── services/                     # 七个独立 Maven 服务
│   │   ├── api-gateway/
│   │   ├── platform-service/
│   │   ├── finance-service/
│   │   ├── device-connectivity-service/
│   │   ├── integration-service/
│   │   ├── ecosystem-service/
│   │   └── telemetry-service/
│   └── libraries/                    # shared-kernel、生成的 contracts、testkit
├── frontend/
│   ├── apps/                         # P1、P2、M1–M4、D1 七端
│   └── packages/                     # UI、类型、API 客户端、设计令牌
├── database/                         # MySQL Flyway 与 ClickHouse DDL/视图/备份资料
├── contracts/                        # MQTT、OpenAPI、Kafka/AsyncAPI 契约
├── config/                           # 脱敏环境、数据库、私有化配置模板
├── platform/                         # Compose/未来 Helm 与可观测性资产
├── scripts/                          # init、deploy、backup-restore、quality 自动化
├── tests/                            # unit、integration、contract、e2e、performance、security
└── delivery/                         # execution、releases、deployment、acceptance 交付资料
```

## 目录职责与边界

| 目录 | 保留内容 | 不应放入 |
| --- | --- | --- |
| `docs/` | 当前有效的 Markdown 文档与 Mermaid 源码。 | 可执行脚本、生产凭据、历史草案。 |
| `archive/` | 可追溯的历史需求、设计、接入、交付、旧目录快照。 | 当前开发依据、生产密钥、未脱敏日志。 |
| `backend/services/` | 7 个服务的源代码、测试、服务自身配置。 | 跨服务共享业务实体、其他服务的数据库迁移。 |
| `backend/libraries/` | 稳定复用的 ID、错误模型、审计上下文、传输契约和测试工具。 | 领域实体、Mapper、跨服务事务。 |
| `frontend/apps/` | 七端各自的 Vue/小程序工程。 | 其他应用可复用组件的副本。 |
| `frontend/packages/` | UI、类型、API 客户端、设计令牌等共享包。 | 单一应用业务页面。 |
| `database/` | 受版本控制的迁移、DDL、物化视图、备份恢复说明。 | 生产数据备份、连接密钥。 |
| `contracts/` | 版本化 MQTT、OpenAPI、事件契约及兼容性记录。 | 未定义版本的临时报文。 |
| `config/`、`platform/` | 脱敏配置模板、Compose、未来 Helm、观测资产。 | 真实密码、证书、Token、生产连接串。 |
| `scripts/`、`tests/`、`delivery/` | 自动化、测试资产和可交付证据；`delivery/execution/` 保存现行总控、审批和完成证据。 | 临时个人文件、无归属的产物。 |

## 工程命名

| 范围 | 采用名称 |
| --- | --- |
| 后端服务 | `api-gateway`、`platform-service`、`finance-service`、`device-connectivity-service`、`integration-service`、`ecosystem-service`、`telemetry-service` |
| 前端七端 | `operations-web`、`enterprise-web`、`consumer-miniapp`、`enterprise-miniapp`、`tenant-operations-miniapp`、`enterprise-admin-miniapp`、`operations-dashboard` |
| 归档旧目录 | `archive/code/legacy-placeholder-backend/`、`archive/code/legacy-placeholder-frontend/` |

旧的 `backend/charging-operation-service`、`backend/platform-adapter`、`frontend/web-admin` 等仅含 README 占位，已按原名移动到 `archive/code/`；其内容没有业务源码。`maintenance-miniapp` 不属于当前七端基线，亦已归档。后续不得恢复这些旧路径作为新代码入口。

## 维护规则

1. 新增、移动或废弃一级/二级工程目录时，同步更新本文件和 [文档与代码输出归档规则](docs/standards/文档与代码输出归档规则.md)。
2. 阶段 0 再补 Maven parent、pnpm workspace、Compose、CI、Flyway 迁移入口与契约校验；未完成前不以空路由或空消费者冒充可交付服务。
3. 归档前在文件首部写明原因、日期和现役替代资料，并从 `docs/` 现役入口移除。
