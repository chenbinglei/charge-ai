# Platform Foundation

本目录提供 W1 本地工程依赖，不承载业务服务、业务 Topic、业务消费者或真实外部渠道配置。

## 本地启动

1. 复制 [`.env.example`](.env.example) 为本机未跟踪的 `.env`，替换其中仅用于本机的占位值。
2. 在仓库根目录执行 `docker compose -f ev-charging-operations/platform/compose.yaml --profile core up -d`。
3. 按需追加 `iot`、`telemetry` 或 `observability` profile；各 profile 的服务均有健康检查。
4. 停止时执行相同 profile 的 `down`；若需清除本地开发数据，应另行确认后才可删除具名卷。

`core` 至少需要 4 vCPU/6 GiB 可用内存；同时启用四个 profile 建议 8 vCPU/12 GiB。所有镜像固定到 tag 与 digest，Kafka 使用单节点 KRaft 且禁止自动创建 Topic。MySQL 只创建 `evco_foundation` 启动库，领域 Schema 与业务迁移必须等相应模块通过设计审核后再创建。

本地示例密码、端口和管理员账号均不得复用于共享、测试或生产环境；真实密钥只可由获批的密钥管理系统注入。
