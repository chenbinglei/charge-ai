# Backend Services

七个后端部署单元的唯一源码入口。当前仅完成目录对齐；架构跑道阶段将补 Maven parent、服务骨架、Flyway、契约校验、健康检查与测试。

实施边界见 [架构基线与服务边界](../../docs/design/充电运营平台架构基线与服务边界-v2.md)；每个 Topic 的发布者、消费者组和分区键见 [Kafka 事件目录](../../contracts/events/README.md)。服务 README 只描述本服务的入口，不另建同义架构说明。

国内省市监管是现行 `ecosystem-service` 的 W10 **监管内部就绪**范围，`integration-service` 不承担监管适配。虚拟电厂及其他第三方数据/调控能力才属于 W12 后扩展；当前不得创建其空消费者或预留 Topic。真实目标接入另以 C3-R 生产 Go 验收。
