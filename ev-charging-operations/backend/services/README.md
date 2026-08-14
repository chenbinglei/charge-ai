# Backend Services

七个后端部署单元的唯一源码入口。W0 已完成 Maven parent、七服务无业务骨架、外部化配置、Actuator 健康检查和架构测试；W1 才建立 Flyway、契约校验、CI、Compose 与业务实现。

实施边界见 [架构基线与服务边界](../../docs/design/充电运营平台架构基线与服务边界-v2.md)；每个 Topic 的发布者、消费者组和分区键见 [Kafka 事件目录](../../contracts/events/README.md)。服务 README 只描述本服务的入口，不另建同义架构说明。

国内省市监管是现行 `ecosystem-service` 的 W10 **监管内部就绪**范围，`integration-service` 不承担监管适配。虚拟电厂及其他第三方数据/调控能力才属于 W12 后扩展；当前不得创建其空消费者或预留 Topic。真实目标接入另以 C3-R 生产 Go 验收。
