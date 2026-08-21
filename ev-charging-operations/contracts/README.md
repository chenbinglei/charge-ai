# Contracts

统一登记并版本化 MQTT、OpenAPI 和 Kafka/AsyncAPI 契约。Topic、Bucket 和 API 的生产创建由部署自动化负责，业务服务不得运行时任意创建。

- [events/](events/README.md)：Kafka Topic、发布者、消费者组、分区键、重放与 DLQ 的权威目录；[公共事件信封](events/schemas/event-envelope.v1.schema.json) 与 [AsyncAPI 组件](events/asyncapi/event-envelope-v1.yaml) 不声明运行时 Topic。
- [mqtt/](mqtt/README.md)：统一设备 MQTT Topic、JSON Schema、签名和控制回执契约；[公共报文元数据](mqtt/schemas/message-envelope.v1.schema.json) 不能替代 W4 的具体 Topic/ACL 审核。
- [openapi/](openapi/high-risk-operations-v1.yaml)：前端、网关和 IOT 协同管理同步 API 契约；[平台公共组件](openapi/platform-foundation-v1.yaml) 和 [错误码](openapi/error-codes-v1.yaml) 不声明业务端点。
- [高风险契约与状态机](../docs/design/高风险契约与状态机-v1.md)：设备控制、支付退款、双模式和监管任务的冻结状态机与生产 Go 规则。

任何服务都不得绕过这些契约临时创建 Topic、消费者组或外部回调路由。
