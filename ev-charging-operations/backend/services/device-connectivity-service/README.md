# Device Connectivity Service

设备互联服务：统一 MQTT 上下行、报文校验与标准化、命令收发、接入审计，以及 IOT 协同模式的管理同步 API。

- 自有 schema：`evco_device_connectivity`；原始协议证据、MQTT 接入安全、IOT 同步收件箱与检查点只在本服务保存。
- 发布：设备遥测、订单上报、控制回执、主数据同步命令；具体 Topic 见 [事件目录](../../../contracts/events/README.md)。
- 订阅：设备控制命令、主数据同步结果。
- 禁止：直接写 `evco_master`、订单状态、资金事实、Redis 实时投影和 ClickHouse 分钟数据。

本服务只接受当前已登记的内部设备控制命令，绝不接受虚拟电厂或其他第三方的直连 MQTT 控制。W12 后外部调控必须先由 `ecosystem-service` 的外部生态模块完成授权、资源约束和计划裁决后再调用本服务。
