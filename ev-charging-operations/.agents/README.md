# 项目子代理定义

以下定义可按名称调用。正式产出必须写入 `ev-charging-operations/` 对应目录，并明确区分“已确认、推断、待确认”。

## 当前架构收敛评审三人组

适用前提：万台以内设备、单台 8 核 32 GB、当前 Docker Compose、未来按触发条件演进多节点。下列“专家视角”是评审方法，不表示现实人物参与或背书。

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

总体架构任务优先由当前架构收敛评审三人组联合评审，并由原总体架构三人组提供领域/IOT/可靠性约束；涉及权限、资金、个人支付或范围拆解时，再由下列专项代理补充评审。

## 专项代理

| 子代理 | 专家视角 | 适用任务 |
|---|---|---|
| `authorization_governance_specialist` | David F. Ferraiolo | 多租户、RBAC、页面权限、数据范围与审计 |
| `carol-coye-benson-enterprise-funds-agent` | Carol Coye Benson | 企业资金、清分、租户结算、出款与对账 |
| `h-david-evans-personal-payments-agent` | H. David Evans | 个人预付、支付渠道、退款与消费者资金保护 |
| `requirements_story_mapper` | Jeff Patton | 需求拆解、场景流程、故事地图、范围与版本规划 |
