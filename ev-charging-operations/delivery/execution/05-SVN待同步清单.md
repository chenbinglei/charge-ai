# SVN 待同步清单

> 状态：现行
> 适用：SVN 仅可在公司内网访问时，外网已完成变更的补提与审计。
> 当前状态：**0 项待同步**（2026-08-21 内网直提会话完成三批次补提 r10581–r10583：W2 后端收尾〔Git `f7ca021`〕、W2 前端骨架及三轮 UI 修复〔Git `d1d5753`〕、评审纪要与前端体验基线）。此前 2026-08-20 会话将 SVN-20260820-002 至 006 全部同步至 r10567–r10571，同会话附带三项补正：r10565（移除误入库 node_modules 构建产物并补全全局忽略）、r10566（补登 W1 根构建文件与 .gitignore）、r10572（补登 platform/.env.example 占位模板）。W1 全部变更同步至 r10530–r10533，W2 Step 3/4 同步至 r10554–r10559。本清单自身的证据提交不纳入该口径，实际状态仍以 `svn status -u` 和 `svn log` 为准，避免递归的"最近 revision"表述。

## 使用规则

1. 外网办公时，如果 `svn update` 或 `svn commit` 因无法访问公司内网失败，每一项已经完成验证的功能、配置、测试或文档改动都必须在 Git 提交前登记一行；不得因为 SVN 不可达而跳过 Git、测试、敏感文件检查或文档同步。
2. 每行必须写明本地完成时间、范围、Git commit、验证、预定中文 SVN message 和状态。首次登记时 Git commit 可填“待生成”；Git 成功提交后必须在下一笔聚焦的 Git 证据提交中回填实际 hash。不得登记口令、令牌、证书、内网地址明文或其他敏感信息。
3. 回到公司内网后的首个工作会话，不开始新开发：先执行 `svn update`，再按本清单由旧到新复核范围与测试、提交 SVN。提交说明在可以准确表达时使用中文。
4. SVN 提交成功后，回填实际 revision 和同步时间；为保留此回填证据，清单更新本身也必须以新的 SVN revision 提交。认证、授权、证书、冲突或本地差异问题必须解决或报告，不能标记为“外网不可达”。
5. 待同步项没有实际 SVN revision 时，Git 提交只代表本地/远端 Git 可追溯，不能将对应模块、发布或回滚证据标记为完全完成。

## 待同步项

| 编号 | 本地完成时间 | 范围 | Git commit | 验证 | 预定 SVN 提交说明 | 状态 / 实际 SVN revision | 同步时间 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| SVN-20260816-001 | 2026-08-16 | 统一 W1 入口状态、建立并按 P0/P1 补充《W1 工程跑道需求与验收包》、增加最终决策表、记录 Step 1 批准、增加 Step 2 技术设计与公共契约草案、同步现役导航、变更日志、完成记录和一致性入口；仅文档/契约草案，无生产代码、业务迁移或运行时 Topic。 | `1216903`, `5f65c60`, `93340c7`, `8656d2c`, `65e19d2`, `a3ccd88`, `3cd8954` | `git diff --check`；JSON Schema 解析；YAML 解析；Markdown 本地链接检查。 | `docs: 增加W1工程跑道技术设计与公共契约草案` | **已同步 r10530** | 2026-08-17 |
| SVN-20260816-002 | 2026-08-16 | W1 无业务工程底座：Maven Wrapper/质量与 SBOM、Flyway 空迁移入口和检查、通用 API/TraceId/幂等/审计、契约检查、Kafka/Outbox/Inbox Testkit、Compose 四类 profile、前端七端 workspace/三主题 Token、CI 与安全扫描入口；同步 SVN 忽略 `node_modules`、`dist`、`artifacts` 本地生成物。无业务页面、业务 DDL/Flyway、业务 API、运行时业务 Topic 或消费者。 | `2572664`, `043b763` | Maven `verify`；前端 `pnpm run verify`、`pnpm run sbom`；契约/迁移/敏感信息检查；Compose `config --quiet` 与实际健康检查通过；Testcontainers 中间件探针通过。Git 已推送至远端 `origin/agent/initial-platform-architecture`。 | `feat: 建立W1工程跑道基础` | **已同步 r10531–r10533** | 2026-08-17 |
| SVN-20260820-001 | 2026-08-19 | W2 后端质量整备：platform-service `common` 抽取为共享库 `backend/libraries/starter`（artifactId evco-starter，基础层＋管理端层，`evco.web.security.enabled` 开关）、10 个 service 接口/impl 分离、controller 瘦身（幂等模板/工具归位）、W1 遗留空包清理、Spotless+Checkstyle 全量格式统一。 | `d31c3b0` | Maven 全量 `verify`：12 模块 BUILD SUCCESS，测试 32/32 通过（独立模式集成 20、IOT 协同模式集成 8、架构守护 3、W0 冒烟 1），Checkstyle/Spotless/JaCoCo/SBOM 门禁通过。Git 已推送至远端。 | `refactor: W2后端质量整备——common抽取evco-starter共享库、service接口实现分离、controller瘦身与格式统一` | **已同步 r10559**（2026-08-20 回填） | 2026-08-19 |
| SVN-20260820-002 | 2026-08-20 | testkit Kafka Testcontainers 修复：探测空闲宿主机端口并固定绑定容器 9092，使 `advertised.listeners` 与映射端口一致，修复客户端回连超时；过时 `withPortBindings(PortBinding...)` 改 `List` 重载。 | `34ed61f` | 12 模块 `mvn install package` BUILD SUCCESS（含 Spotless/Checkstyle）；集成测试 `MiddlewareSmokeIntegrationTest` 通过（Kafka AdminClient clusterId、MySQL/Redis/EMQX 连通）；platform-service 32 项测试通过。Git 已推送至远端。 | `fix: Kafka测试容器探测空闲端口固定绑定，修复advertised.listeners与映射端口不一致导致的回连超时` | **已同步 r10567** | 2026-08-20 |
| SVN-20260820-003 | 2026-08-20 | 文档不一致清单 16 项处置（A 包结构 6、B starter 登记 3、C 契约同步 6、D 确认 1）：全栈研发工程规范 v1.4、研发工程规范基线 v1.5、架构与数据分库设计 v1.9、W1/W2 技术设计包、IAM 接口规范 v1.2、认证与SSO接口规范 v1.2、文档导航 v1.5 升版；归档规则直接修订；新增《P1配置接口规范》《个人用户主档接口规范》；清单回填处置结果升 v1.1。 | `68314a3` | 版本号与清单声明逐文件复核一致（7 份升版 + 2 份直接修订落地 + 2 份新增存在）；Git 已推送至远端。 | `docs: 处置文档不一致清单16项，包结构统一域包直挂、登记evco-starter、接口规范与契约同步` | **已同步 r10568**（归档规则/基线等后续批次文件为最终态，随 r10569–r10571 提交） | 2026-08-20 |
| SVN-20260820-004 | 2026-08-20 | 治理补强：评审子代理只读会签机制强制化——治理规范升 v1.6（§2.1 门禁↔子代理映射、纪要落盘 `delivery/audit/` 规则）、根 AGENTS.md 增设 Mandatory read-only review-agent countersign 章节、`.agents/README.md` 补只读会签工作流、总控台账 v1.38 回填推送与处置状态、审批日志登记 DEC-20260820-001；同批含《开发流程总纲》落盘（DEC-20260820-002）。 | `0ba4247` | 文档链接与版本号复核；机制与 `.agents/` 既有定义一致性核对。 | `docs: 评审子代理只读会签机制强制化并补登台账（治理规范v1.6）` | **已同步 r10569**（含总纲；治理规范最终态 v1.7 随 r10570） | 2026-08-20 |
| SVN-20260820-005 | 2026-08-20 | 文档整理乙1/乙4处置与子代理体系对齐传统分层（DEC-20260820-004/005/006）：研发工程规范基线收敛为基线索引 v1.6、公司级总体架构说明迁入 `delivery/leadership-reports/`（SVN 侧删旧路径+加新路径）、kleppmann 服务名修正、授权专员对齐两档权限模型、domain_boundary 增传统分层约束、新增 `frontend_experience_engineer` 子代理、Step1/Step5 门禁映射补强、07-文档整理清单落盘、治理规范升 v1.7、审批日志/总控台账同步；含甲/丁/戊类处置（A9/A10 版本头、A11 总纲入口）。 | `da4740a` | 9 个链接目标存在性核对；全库 grep 无旧路径残留；门禁映射三处（治理规范/README/项目记忆）口径一致。Git 已推送至远端。 | `docs: 乙1/乙4文档处置与子代理体系对齐传统分层（基线收敛、架构说明迁移、门禁映射补强）` | **已同步 r10570** | 2026-08-20 |
| SVN-20260820-006 | 2026-08-20 | 前端规范三节与能源单位换算（DEC-20260820-007~013）：全栈研发工程规范升 v1.11——§5.1 视口/断点/浏览器/小程序基线（四档受控流式＋八组验证视口＋基础库 3.0.0＋等比方案终局否决）、§5.2 显示格式与充电场景全量指标＋量级换算表＋换算不变量与展示防溢出、§5.3 组件与交互规范；技术选型登记 wot-design-uni；design-tokens 受控流式冲突修复（content-max-width 作废改语义封顶、四档断点/语义色 Token）＋3 个 Web App 壳同步；frontend_experience_engineer 第 6 条同步；导航清单升 v2.3；台账/日志同步至最新。 | `aa42196` | design-tokens vitest 1 passed；全库 grep 无 content-max-width 残留；W1 已批准值与 W1 验收包 §5.1/D-17 逐项核对一致。Git 已推送至远端。 | `docs: 前端规范三节(视口/格式/交互)与能源单位换算基线；修复design-tokens受控流式冲突` | **已同步 r10571** | 2026-08-20 |

## 历史记录

| 时间 | 事项 | 结果 |
| --- | --- | --- |
| 2026-08-14 | 建立外网 SVN 待同步与回公司补提机制。 | 初始状态无待同步项；后续每项待同步工作均在本文件追加并回填实际 revision。 |
| 2026-08-17 | 回到公司内网，执行 SVN `update`（版本 10517→10528）并补提 W1 全部待同步项。发现并修复 `.svn/wc.db` SQLite journal 模式冲突：每次 SVN 操作前需将 `journal_mode` 设为 `WAL` 以避免 `attempt to write a readonly database` 错误。 | SVN-20260816-001 同步至 r10530；SVN-20260816-002 同步至 r10531–r10533。工作副本干净，0 项待同步。 |
| 2026-08-20 | SVN 规则合规检查（用户指示）：核实 SVN 服务器可达、`svn log` 核对 r10554–r10559 实际内容，发现台账滞后——SVN-20260820-001 已于 2026-08-19 21:18 实际提交为 r10559（消息含 Git `d31c3b0`），但清单未回填且头部"最近完成 r10557"过期；补回填并补登记 SVN-20260820-005/006（Git `da4740a`/`aa42196`）。另发现仓库根构建文件（`mvnw`/`mvnw.cmd`/`.mvn`/`.editorconfig`/`.github`）从未 `svn add`，与规范"同一项目树"存在缺口，待用户决策处置。 | SVN-20260820-001 回填 r10559；新增 005/006 两行；当时 5 项待同步（002–006）。 |
| 2026-08-20 | 内网同步会话（用户批准执行）：WAL 修复后 `svn update`（r10517→r10564，r10560–r10564 与本路径无关）→ 专项清理 r10565（svn delete --keep-local 移除误入库的 19 个 node_modules 构建产物路径＋svn:global-ignores 补 node_modules/dist/coverage/artifacts/target/.vite）→ r10566 补登根构建文件与 .gitignore → 由旧到新提交五项：r10567（002 testkit）/r10568（003 文档16项）/r10569（004 会签机制＋总纲）/r10570（005 乙1乙4＋子代理，架构说明删旧加新完成迁移）/r10571（006 前端规范）→ r10572 补登 platform/.env.example。同步后 `svn status` 仅剩本清单，工作副本干净。文件跨批次累积变更的处置原则：按最终态归入最后一次变更所属批次提交，消息中标注 Git hash 供追溯。 | SVN-20260820-002 至 006 全部同步完成（r10567–r10571）；待同步项清零；Git/SVN 双库恢复"同一项目树"一致。 |
| 2026-08-21 | SVN 提交机制合规检查（用户指示核对「每次改动都需要提交」）：发现 58 项变更未按「完成并验证即提交 SVN」规则落库——批次 A：W2 后端收尾（Git `f7ca021` 已提交但 SVN 漏同步，35 文件）；批次 B：W2 前端骨架＋三轮 UI 修复（Git/SVN 双漏，含 DEC-20260820-018 日志，21 文件）；批次 D：评审纪要 v1-v3 与前端体验基线（4 文件）。用户批准后执行补提：`svn update` r10564→r10580 → 由旧到新 r10581（批次 A，消息标注 Git `f7ca021`）→ r10582（批次 B+C）→ r10583（批次 D）；同会话建立前端 Git 锚点 `d1d5753`（27 文件，+3064/−94）。`src/assets` 下 3 张 176KB 相同占位 PNG（DEC-018 图片 API 调研残留、全库零引用）经查未提交入库，待用户决策删除。 | 三批次补提完成（r10581–r10583）；工作副本仅剩 `src/assets` 未跟踪残留；Git/SVN 双库恢复一致；流程教训：内网会话中「完成并验证」的变更须当次落库，不得累积。 |
