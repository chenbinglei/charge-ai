# Platform Service

> 下列为 W1+ 目标服务边界，不表示 W0 已发布或订阅事件、创建 Schema 或实现业务能力。

平台业务服务：IAM、主数据、企业客户、交易、营销、运营与 V2G 模块。

- 自有 schema：`evco_iam`、`evco_master`、`evco_customer`、`evco_trade`、`evco_marketing`、`evco_operations`、`evco_v2g`。
- 发布：资金冻结/结算/退款请求、现行设备控制命令、订单生命周期、通知命令；具体 Topic 见 [事件目录](../../../contracts/events/README.md)。
- 订阅：设备订单/控制回执、资金结果、主数据同步命令与外部投递结果。
- 禁止：MQTT 长连接、资金账本、支付/银行/监管协议 I/O、ClickHouse 分钟遥测写入。

`trade` 只创建售后退款申请；财务是否退款及最终结果由 `finance-service` 决定。

后续虚拟电厂调控不是本服务直接接收的外部协议：本服务只提供订单、资产、授权、人工/自动策略等权威约束；W12 后 `ecosystem-service` 的外部生态模块才对外受理控制意图并裁决计划。当前不新增该能力或 Topic。
