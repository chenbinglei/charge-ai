# MySQL Flyway Migration Entry

W1 只提供前向迁移入口和校验，未创建任何领域迁移或领域 Schema。领域事实、`outbox_event` 与 `inbox_event` 必须由对应 W2–W11 模块在其 Step 2 设计通过后，以新的前向脚本创建。

每个领域 Schema 使用 `database/mysql/<schema>/migration/VyyyyMMddHHmm__lower_snake_case.sql`。运行前先在受控环境设置 `EVCO_FLYWAY_URL`、`EVCO_FLYWAY_USER`、`EVCO_FLYWAY_PASSWORD` 与 `EVCO_FLYWAY_SCHEMA`，再执行：

```bash
./mvnw -f ev-charging-operations/backend/pom.xml -Pflyway flyway:validate flyway:migrate
```

该入口固定 UTF-8、禁止 clean、禁止乱序和 down migration。执行前先运行 `bash ev-charging-operations/scripts/quality/check-flyway-layout.sh`；回退必须使用新的补偿迁移、代码回退或已演练的恢复流程。
