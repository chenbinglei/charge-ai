# API Gateway

> 下列为 W1+ 目标服务边界，不表示 W0 已部署路由、WebSocket 或业务能力。

统一 HTTP/WebSocket API 入口：令牌预校验、路由、限流、CORS、TraceId 透传和统一错误外观。

- 只路由：HTTP 请求到事实所有者；WebSocket 只路由至 `telemetry-service`。
- 不拥有业务 schema、Kafka 消费组或资金/订单/MQTT 事实。
- 对象级权限、支付退款决策和业务编排由下游事实所有者执行。

完整边界见 [架构基线与服务边界](../../../docs/design/充电运营平台架构基线与服务边界-v2.md)。
