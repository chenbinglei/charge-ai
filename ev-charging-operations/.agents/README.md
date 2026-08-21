# 项目子代理定义

以下定义可按名称调用。正式产出必须写入 `ev-charging-operations/` 对应目录，并明确区分“已确认、推断、待确认”。

## 只读会签工作流（强制，2026-08-20 起）

本目录是评审子代理定义的唯一来源。所有子代理一律**只读**：可运行验证、审阅 diff、提出问题清单，但不得直接修改任何文件；纪要由主实现智能体以子代理视角代笔落盘，结论必须源于各子代理的「固定判断原则」，不得篡改或选择性引用。

- **门禁映射**（详见[单人AI协作开发与变更治理规范](../docs/standards/单人AI协作开发与变更治理规范-v1.md) §2.1）：Step 1 → `requirements_story_mapper`＋域专项；Step 2 → 架构收敛三人组＋域专项；Step 3 → `martin_kleppmann_consistency_messaging_agent`＋域专项；Step 4/6 → `authorization_governance_specialist`＋域专项；Step 5 → `requirements_story_mapper`＋`frontend_experience_engineer`。涉设备/计量/V2G/MQTT 必须 `charging_iot_v2g_integration_specialist` 会签；涉部署编排/备份恢复/可观测性/CI 供应链/安全与性能测试门禁设计必须 `platform_reliability_data_architect` 会签。
- **落盘**：每门禁一份 `delivery/audit/Wx-StepN评审纪要-vN.md`，未落盘纪要不得宣称"评审通过"。
- **问题分级**：P0/P1 修复并复评后方可进入下一 Step；P2 可延后但须显式登记。
- **边界**：评审纪要是门禁必要输入，不替代用户批准；用户结论以《02-变更与审批日志》为准。

## 当前架构收敛评审三人组

适用前提：万台以内设备、单台 8 核 32 GB、当前 Docker Compose、未来按触发条件演进多节点。下列“专家视角”是评审方法，不表示现实人物参与或背书。

当前代码架构基线（2026-08-20 固化引用）：7 个部署单元（`api-gateway`、`platform-service`、`finance-service`、`integration-service`、`ecosystem-service`、`device-connectivity-service`、`telemetry-service`）；服务内为业务域包直挂传统分层（controller→service→mapper→entity，Service 接口与实现分离，实现位于 service/impl）；跨服务 web 通用能力归共享库 `backend/libraries/starter`（evco-starter，`evco.web.security.enabled` 开关控制）。各子代理评审一律以该基线为前提，不得要求 DDD 战术模式分层或恢复 `module/` 中间层。

| 子代理 | 专家视角 | 在本项目中的职责 |
|---|---|---|
| `martin_fowler_service_topology_agent` | Martin Fowler | 收敛服务部署单元、识别必须隔离的边界、定义何时再拆分。 |
| `martin_kleppmann_consistency_messaging_agent` | Martin Kleppmann | 审核 MySQL/Kafka/Redis/ClickHouse 的事实边界、事件一致性、回放和幂等。 |
| `charity_majors_single_node_reliability_agent` | Charity Majors | 审核 8 核 32 GB 资源、可观测性、降级、备份恢复与真实 HA 演进门槛。 |

`charging_iot_v2g_integration_specialist` 是该三人组的强制会签方：凡涉及 MQTT、EMQX、设备接入、控制、计量、遥测与 V2G，必须由其补充协议和设备侧约束。三人组不得重新定义设备协议或臆测设备能力。

## 原总体架构三人组

| 子代理 | 专家视角 | 在本项目中的职责 |
|---|---|---|
| `domain_boundary_architect` | Eric Evans：平台与领域架构 | 统筹七应用、IOT、业务服务、主数据和权限/服务边界；与 `martin_fowler_service_topology_agent` 共同决定服务收敛。 |
| `charging_iot_v2g_integration_specialist` | 充电设备、IOT 与 V2G 集成 | 统筹设备接入、遥测、控制、计量、监管和 V2G 的集成安全边界。 |
| `platform_reliability_data_architect` | DevSecOps、SRE 与数据可靠性 | 统筹数据库、中间件、云原生运行、安全、可观测性、灾备和测试质量。 |

总体架构任务优先由当前架构收敛评审三人组联合评审，并由原总体架构三人组提供领域/IOT/可靠性约束；涉及权限、资金、个人支付或范围拆解时，再由下列专项代理补充评审。`platform_reliability_data_architect` 除总体架构联合评审外，在涉部署编排、备份恢复、可观测性、CI 供应链、安全与性能测试门禁设计时为强制会签方（2026-08-20 起，DEC-20260820-006）。

## 专项代理

| 子代理 | 专家视角 | 适用任务 |
|---|---|---|
| `authorization_governance_specialist` | David F. Ferraiolo | 多租户、RBAC（资源＋读/写两档）、站点级数据范围与审计 |
| `carol-coye-benson-enterprise-funds-agent` | Carol Coye Benson | 企业资金、清分、租户结算、出款与对账 |
| `h-david-evans-personal-payments-agent` | H. David Evans | 个人预付、支付渠道、退款与消费者资金保护 |
| `requirements_story_mapper` | Jeff Patton | 需求拆解、场景流程、故事地图、范围与版本规划 |
| `frontend_experience_engineer` | 前端工程与多端交付 | 七端目录结构、设计令牌、页面状态覆盖、API 客户端调用边界、路由与按钮权限显隐（Step 5 必邀） |
