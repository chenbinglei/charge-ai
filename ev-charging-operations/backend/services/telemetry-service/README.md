# Telemetry Service

遥测与查询服务：实时状态投影、分钟归并、ClickHouse 写入、曲线、大屏、分析查询与 WebSocket 状态扇出。

- 消费：已校验的设备遥测和订单关联事件；按设备/枪口局部顺序裁决状态。
- 发布：仅发布当前已登记的告警候选和业务所需遥测汇总事件；按监管频率聚合后的监管状态事件供 `ecosystem-service` 消费；不把原始遥测广播给业务服务。
- 自有数据：ClickHouse `evco_telemetry` 与可重建 Redis 实时 key。
- WebSocket：仅通过 `api-gateway` 对持有短期范围票据的客户端推送状态增量；断线后必须重新取快照。
- 禁止：订单、资金、资产、权限事实写入；不能把 WebSocket 当作订单或资金消息总线。
