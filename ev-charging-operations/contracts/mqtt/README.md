# 设备 MQTT 契约

> 状态：W1 Step 2 已冻结公共报文元数据；具体 Topic、业务 payload、ACL 和真实设备联调在 W4 审核。只适用于平台自管 EMQX 与已认证上游。GB/T 27930 约束车—桩通信，**不**替代本目录的平台北向 MQTT 契约。

所有 MQTT 报文先满足 [`schemas/message-envelope.v1.schema.json`](schemas/message-envelope.v1.schema.json) 的 `messageId`、版本、来源、设备/枪口、序列和时间元数据，再由具体 Topic Schema 限定 `payload`。公共元数据不是已开通的设备接入或 ACL。

## Topic 与 ACL

| 方向 | Topic 模板 | QoS | 发布者 | 要求 |
| --- | --- | --- | --- | --- |
| 上行遥测/订单/回执 | `evco/v1/{sourceSystem}/{deviceLifecycleId}/{connectorId}/up` | 1 | 已登记设备或获准上游 | mTLS、来源签名、单调 `sourceSequence`。 |
| 下行控制 | `evco/v1/{sourceSystem}/{deviceLifecycleId}/{connectorId}/down` | 1 | 仅 `device-connectivity-service` | 不使用 retained；只能发送未过期、已授权的控制命令。 |

服务端以 `deviceLifecycleId:connectorId` 为每个控制命令的 Kafka key 和 MQTT 路由顺序键。命令报文引用 [device-control-command v1](../events/schemas/device-control-command.v1.schema.json) 的 `payload`；设备必须回传同一 `deviceCommandId`、`commandSequence`、最终状态、设备状态版本与回执时间。任何过期、低序号或期望状态版本不匹配的命令均拒绝并审计。

未知设备、未经认证来源、超出 ACL 的 Topic 和 V2G 真实放电命令一律拒绝；不得保存未经认证 payload。
