# Kafka 事件目录与消费组清单

> 版本：v1.2
> 状态：**实施权威合同目录**；高风险 JSON Schema 已冻结于 `schemas/`。W1 Step 2 已新增公共事件信封草案；具体业务 Topic 的逐字段 AsyncAPI/Schema 仍须在对应业务模块审核后落入同目录。
> 适用：`evco.*.v1` 内部 Kafka Topic。外部设备协议查 `contracts/mqtt/`；HTTP API 查 `contracts/openapi/`。
> 架构依据：[架构基线与服务边界](../../docs/design/充电运营平台架构基线与服务边界-v2.md)。

> 不熟悉 Kafka、Topic 或消费者组时，先读《[事件协作与 Kafka 通俗说明](../../docs/design/事件协作与Kafka通俗说明-v1.md)》；本文件只保留可执行合同，不重复通俗教程。

## 1. 读表规则

- **发布者**是唯一允许向该 Topic 写业务消息的服务；同一服务内部模块以事件类型区分。
- **消费者组**是订阅单位。不同组各得到一份消息；同组的多实例仅分摊分区，不能用于广播。
- `—` 表示本期没有第二个消费者，不允许“预留订阅”。新增消费者必须补充业务投影、幂等、回归和权限影响。
- 所有 Topic 的保留、分区数和 `max.poll.records` 由 W4 压测冻结；当前单机 Kafka 只作短期缓冲和重放，不是灾备。

## 2. 所有事件共用的信封

每条消息必须使用 JSON `lowerCamelCase`，并使用 [`schemas/event-envelope.v1.schema.json`](schemas/event-envelope.v1.schema.json) 中的公共信封。该草案没有声明运行时 Topic 或消费者；具体 Topic 仍以本目录的逐行登记为准。公共字段至少包含：

```json
{
  "eventId": "全局唯一 ID",
  "eventType": "domain.entity.past-tense.v1",
  "eventVersion": "1",
  "schemaVersion": "1.0",
  "occurredAt": "RFC 3339 UTC",
  "aggregateType": "业务聚合类型",
  "aggregateId": "业务聚合 ID",
  "partitionKey": "与 Kafka key 相同的局部顺序键",
  "traceId": "端到端追踪 ID",
  "idempotencyKey": "业务去重键",
  "producer": "唯一事实生产服务",
  "scope": {"scopeType": "TENANT", "tenantId": "租户标识", "dataScopeVersion": 1},
  "payload": {}
}
```

`eventVersion` 必须与 `eventType` 的大版本一致；`schemaVersion` 用于同大版本内的兼容演进。Kafka key 与 `partitionKey` 必须一致。`scope` 只能是带 `tenantId` 的 `TENANT`，或不带 `tenantId` 的 `PLATFORM`；不得把手机号、支付数据、身份证件、车牌、VIN 或密钥写入公共字段。

AsyncAPI 组件草案位于 [`asyncapi/event-envelope-v1.yaml`](asyncapi/event-envelope-v1.yaml)，其中 `channels: {}` 是刻意的 W1 边界，不能据此创建 Topic。

设备事件的具体 `payload` Schema 另必须包含 `sourceSystem`、`sourceSequence`、`sourceTime`、`receivedAt`、`stationLifecycleId`、`deviceLifecycleId`、`connectorId` 和 `payloadHash`；租户范围以公共 `scope.tenantId` 为准，不重复在顶层保存。金额一律为最小货币单位整数；支付、身份证、手机号、车牌、VIN 和密钥不得放入 Topic 名或未脱敏 payload。

## 3. 设备、遥测与主数据

| Topic | 发布者与事件 | 消费者组 | Kafka key / 必填关联 | 消费后的唯一职责 |
| --- | --- | --- | --- | --- |
| `evco.device.telemetry.ingress.v1` | `device-connectivity-service`；`device.telemetry.accepted.v1` | `evco.telemetry.ingestion.v1` | `deviceLifecycleId:connectorId` | 遥测服务裁决实时状态、写 Redis 和 ClickHouse 分钟记录。 |
| `evco.device.order-report.v1` | 设备互联；`device.order-report.accepted.v1` | `evco.platform.trade-order-report.v1`；`evco.telemetry.order-correlation.v1` | `chargingOrderId`；未知时设备会话键 | 交易模块推进订单状态；遥测服务仅建立会话/状态关联，不写订单。 |
| `evco.device.control.command.v1` | 平台运营或交易模块；`device.control-command.requested.v1` | `evco.device.command-execution.v1` | **`deviceLifecycleId:connectorId`**；payload 必含递增 `commandSequence`、`expectedStateVersion`、`expiresAt` | 设备互联按同枪顺序校验、路由 MQTT 并记录准确 payload；Schema 见 `schemas/device-control-command.v1.schema.json`。 |
| `evco.device.control.reply.v1` | 设备互联；`device.control-command.replied.v1` | `evco.platform.operations-command-reply.v1`；`evco.platform.trade-command-reply.v1` | `deviceCommandId` | 运营模块更新人工/策略控制记录；交易模块仅更新其启动/停止命令关联状态。 |
| `evco.master.sync-command.v1` | 设备互联；`master.sync-command.accepted.v1` | `evco.platform.master-sync.v1` | `sourceSystem:externalId`；payload 必含 `entityVersion`、`sequenceNo`、`watermark` | 主数据模块只在 `iot-collaborative` 模式幂等、有序应用受控 IOT 管理同步；Schema 见 `schemas/master-sync-command.v1.schema.json`。 |
| `evco.master.sync-result.v1` | 平台主数据；`master.sync-command.applied.v1`、`master.sync-command.rejected.v1` | `evco.device.master-sync-result.v1` | 原 `eventId` / `sourceSystem:externalId` | 设备互联更新同步收件箱、检查点并向 IOT 查询/回执接口返回最终结果。 |

`telemetry-service` 向 WebSocket 连接扇出实时状态是服务内行为，不创建“浏览器消费 Kafka”的 Topic。需要给运营领域造成业务后果的告警，必须发布独立的告警候选事件，不能让 `platform-service` 直接消费全部遥测。

## 4. 订单、资金、支付与退款

| Topic | 发布者与事件 | 消费者组 | Kafka key / 必填关联 | 消费后的唯一职责 |
| --- | --- | --- | --- | --- |
| `evco.finance.hold-request.v1` | 平台交易；`finance.fund-hold.requested.v1` | `evco.finance.hold.v1` | `chargingOrderId` | 财务校验资金主体并创建冻结/预授权事实。 |
| `evco.finance.hold-result.v1` | 财务；`finance.fund-hold.succeeded.v1`、`finance.fund-hold.failed.v1` | `evco.platform.trade-hold-result.v1` | `chargingOrderId` | 交易模块决定是否发出设备启动命令；不得自行改余额。 |
| `evco.finance.settlement-request.v1` | 平台交易；`finance.order-settlement.requested.v1` | `evco.finance.order-settlement.v1` | `chargingOrderId` | 财务按订单价格、资金和结算快照完成扣款/释放/差额处理。 |
| `evco.finance.settlement-result.v1` | 财务；`finance.order-settlement.completed.v1`、`finance.order-settlement.exceptioned.v1` | `evco.platform.trade-settlement-result.v1` | `chargingOrderId` | 交易模块完成订单或转异常待处理；不得重算财务金额。 |
| `evco.finance.payment-channel-command.v1` | 财务；`finance.payment-channel-submission.requested.v1`、`finance.payment-channel-query.requested.v1` | `evco.integration.payment-execution.v1` | `paymentIntentId` | 集成服务创建/查询渠道支付单并保存外部执行证据。 |
| `evco.integration.payment-channel-result.v1` | 集成；`integration.payment-channel.submitted.v1`、`integration.payment-channel.confirmed.v1`、`integration.payment-channel.failed.v1` | `evco.finance.payment-result.v1` | `paymentIntentId` | 财务幂等写入规范化渠道结果，成功时创建资金批次或确认外部直付。 |
| `evco.finance.refund-request.v1` | 平台交易；`finance.refund.requested.v1` | `evco.finance.refund-eligibility.v1` | **`originalPaymentTransactionId`**；payload 带 `refundRequestId` | 财务按原支付交易串行校验退款资格、原路由、资金批次和累计可退款余额，创建唯一退款交易；Schema 见 `schemas/finance-refund-request.v1.schema.json`。 |
| `evco.finance.refund-channel-command.v1` | 财务；`finance.refund-channel-submission.requested.v1` | `evco.integration.refund-execution.v1` | `refundTransactionId` | 集成服务按原渠道、原商户、原支付路由执行退款。 |
| `evco.integration.refund-channel-result.v1` | 集成；`integration.refund-channel.confirmed.v1`、`integration.refund-channel.failed.v1` | `evco.finance.refund-result.v1` | `refundTransactionId` | 财务最终记账、恢复资金批次或创建待对账状态。 |
| `evco.finance.payment-status.v1` | 财务；`finance.payment.confirmed.v1`、`finance.payment.failed.v1` | `evco.platform.trade-payment-status.v1`；`evco.platform.operations-payment-notification.v1` | `paymentIntentId` | 交易模块更新订单支付可用性；运营模块只创建通知任务，不修改金额。 |
| `evco.finance.refund-status.v1` | 财务；`finance.refund.completed.v1`、`finance.refund.failed.v1` | `evco.platform.trade-refund-status.v1`；`evco.platform.operations-refund-notification.v1` | `refundTransactionId` | 交易模块关闭/升级售后并生成结算调整请求；运营模块只创建通知任务。 |
| `evco.trade.charging-order-event.v1` | 平台交易；`trade.charging-order.created.v1`、`started.v1`、`completed.v1`、`corrected.v1` | `evco.ecosystem.regulatory-task.v1` | `chargingOrderId` + 订单事件序号 | 生态服务创建监管上报任务；监管失败不能回写或阻塞订单。 |

纯钱包退款由财务在 `refund_transaction` 中完成资金批次恢复，并照常发布 `finance.refund-status.v1`；它不发布 `refund-channel-command`。未消费现金充值退款不订阅或生成任何租户结算调整；充电订单退款、补差和修正必须以订单结算方案快照生成调整。

## 5. 监管来源与 V2G

| Topic | 发布者与事件 | 消费者组 | Kafka key / 必填关联 | 消费后的唯一职责 |
| --- | --- | --- | --- | --- |
| `evco.master.regulatory-archive-event.v1` | 平台主数据；`master.station.changed.v1`、`master.charger.changed.v1`、`master.connector.changed.v1` | `evco.ecosystem.regulatory-task.v1` | `aggregateType:aggregateId` + 业务版本 | 生态服务创建档案上报任务；不得写回资产。 |
| `evco.trade.tariff-event.v1` | 平台交易；`trade.tariff-version.published.v1`、`trade.tariff-version.expired.v1` | `evco.ecosystem.regulatory-task.v1` | `tariffRuleVersionId` | 生态服务创建价格上报任务。 |
| `evco.operations.alarm-event.v1` | 平台运营；`operations.alarm.raised.v1`、`operations.alarm.recovered.v1` | `evco.ecosystem.regulatory-task.v1` | `alarmEventId` | 生态服务创建安全/告警上报任务。 |
| `evco.telemetry.regulatory-state.v1` | 遥测；`telemetry.regulatory-state.aggregated.v1` | `evco.ecosystem.regulatory-task.v1` | `deviceLifecycleId:connectorId` | 生态服务按目标省市频率创建状态/功率上报任务；不消费原始遥测。 |
| `evco.finance.v2g-settlement-request.v1` | 平台 V2G；`finance.v2g-earning-settlement.requested.v1` | `evco.finance.v2g-earning-settlement.v1` | `v2gOrderId` | **当前不允许生产者创建**；仅在真实现场试点另行获批后启用。 |
| `evco.finance.v2g-settlement-result.v1` | 财务；`finance.v2g-earning-settlement.completed.v1`、`finance.v2g-earning-settlement.exceptioned.v1` | `evco.platform.v2g-settlement-result.v1` | `v2gOrderId` | **当前不允许生产者创建**；模拟收益不进入财务或个人余额。 |
| `evco.v2g.order-event.v1` | 平台 V2G；`v2g.order.started.v1`、`v2g.order.completed.v1`、`v2g.order.corrected.v1` | `evco.ecosystem.regulatory-task.v1` | `v2gOrderId` + 事件序号 | **当前不允许生产者创建**；真实 V2G 和监管上报只在获批现场试点中启用。 |

## 6. 通知、监管结果与租户付款

| Topic | 发布者与事件 | 消费者组 | Kafka key / 必填关联 | 消费后的唯一职责 |
| --- | --- | --- | --- | --- |
| `evco.operations.notification-command.v1` | 平台运营；`operations.notification.requested.v1` | `evco.integration.notification-delivery.v1` | `notificationTaskId` | 集成服务调用已启用的渠道适配器，保存投递证据和回执。 |
| `evco.integration.notification-result.v1` | 集成；`integration.notification.delivered.v1`、`failed.v1` | `evco.platform.operations-notification-result.v1` | `notificationTaskId` | 运营模块更新发送、频控、回执和失败处置记录。 |
| `evco.ecosystem.regulatory-result.v1` | 生态服务；`ecosystem.regulatory-task.succeeded.v1`、`failed.v1` | `evco.platform.operations-regulatory-view.v1` | `regulatoryTaskId` | 运营模块只更新可见状态/告警投影；监管任务、尝试和原文仍归生态服务。 |
| `evco.finance.tenant-payout-command.v1` | 财务；`finance.tenant-payout.requested.v1` | `evco.integration.banking-execution.v1` | `payoutRequestId` | 集成服务调用受控银行/结算通道并保存回单证据。 |
| `evco.integration.tenant-payout-result.v1` | 集成；`integration.tenant-payout.confirmed.v1`、`failed.v1` | `evco.finance.tenant-payout-result.v1` | `payoutRequestId` | 财务更新付款、回单关联和对账状态。 |

停车、门禁和道闸不使用资金 Topic：它们只能订阅/发布权益校验、核销、撤销和协同结果。停车费的支付、退款和发票永远留在停车合法主体。

## 7. 失败、重放与 DLQ

| 场景 | 正确动作 | 禁止动作 |
| --- | --- | --- |
| Outbox 未能发布 | 保持待发、指数退避、告警；恢复后允许重复投递。 | 标记成功、删除事件或转 DLQ 后不再处理。 |
| 消费者本地事务失败 | 不提交 offset，由 Kafka 重投。 | 先提交 offset 再补写业务事实。 |
| 超过业务重试预算 | 由当前消费者发布至 `evco.<producer-domain>.dlq.v1`，记录原 `eventId`、失败原因和人工处置状态。 | 把 DLQ 当作生产者发送失败的通用垃圾箱。 |
| 重放 | 以 Inbox `eventId`、业务幂等键和业务状态机吸收重复。 | 以“Kafka 不会重复”或缓存锁假设正确性。 |
| 支付/退款外部超时 | 保持待确认，执行渠道查询/对账；只有财务结果能结束交易。 | 仅因 HTTP 超时就认定失败或再次发起不带幂等键的扣款/退款。 |

## 8. 已确认但不在当前计划的外部能源协同

国内省市监管是当前 `ecosystem-service` 的 W10 **监管内部就绪**范围，相关 Topic 和消费者组已在本目录登记；真实目标接入另以 C3-R 生产 Go 验收。只有虚拟电厂和其他第三方平台的数据共享、控制意图、站点功率计划与设备调控属于 W12 后扩展能力：当前**不得**创建对应的未来 Topic、消费者组或空消费者。现有 `evco.device.control.command.v1` 的发布者仍是平台运营或交易模块。

外部能源协同扩展启动时，必须先按《[监管与外部生态能源协同方案](../../docs/design/监管与外部生态能源协同方案-v1.md)》完成单独变更、AsyncAPI/Schema、生产者/消费者组、Outbox/Inbox、幂等、DLQ 和控制 E2E，再以本目录的后续版本登记实际 Topic。监管与第三方均不得直连 Kafka 或向设备 MQTT Topic 发送控制报文。

## 9. 交付门禁

开发者不得在没有对应 AsyncAPI/JSON Schema、生产者 Outbox、消费者 Inbox、消费组、幂等测试和失败处置用例的情况下创建生产者或消费者。每新增一个 Topic，都必须更新本目录、[架构基线](../../docs/design/充电运营平台架构基线与服务边界-v2.md)、服务 README 与 E2E 覆盖清单。

高风险状态机、拒绝条件和生产 Go 证据见《[高风险契约与状态机](../../docs/design/高风险契约与状态机-v1.md)》。监管任务实体的 Schema 为 `schemas/regulatory-task.v1.schema.json`；W10 模拟成功只能标记为监管内部就绪，不能标记 C3-R 完成。
