# 前端工程底座（W1）

本目录是七端前端的 W1 工程底座，不包含业务页面、业务权限、模拟业务数据或对后端业务端点的调用。七端应用位于 [`apps/`](apps/README.md)，共享类型、API Client 与设计 Token 位于 [`packages/`](packages/README.md)。

## 固定工具链

- Node 版本以 [`.nvmrc`](.nvmrc) 固定为 `22.14.0`；pnpm 以根 [`package.json`](package.json) 的 `packageManager` 固定为 `11.19.0`。
- 安装必须使用 `corepack enable && pnpm install --frozen-lockfile`；不得以本机全局 pnpm 或浮动依赖替代锁文件。
- 完整质量验证使用 `pnpm run verify`，生成前端 CycloneDX SBOM 使用 `pnpm run sbom`。生成物位于被 Git 忽略的 `artifacts/`。
- Web 端才可使用 Element Plus、ECharts 与浏览器 DOM；uni-app 包和共享包的边界由 `pnpm run check:boundaries` 检查。

## W1 主题与响应式验证

`@evco/design-tokens` 提供 `energy-green`、`graphite-night`、`command-deep` 三种主题的同语义 Token。W1 样例只验证 Token、主题切换、24 栅格与断点骨架；业务信息架构和业务组件只能在对应模块 API 门禁通过后实现。

已进行浏览器实际渲染检查：在 `1920×1080` 与 `768×1024` 视口切换三主题均生效，且样例容器无横向溢出。完整六视口视觉回归将在首个真实业务页面进入对应模块验收时执行。

## 已知的上游依赖说明

当前 DCloud 的 `3.0.0-5020420260813002` uni-app 依赖树中，间接 `unplugin-auto-import/@vueuse/core` 声明 Vue `^3.5.0` peer，而 DCloud 同时固定 Vue `3.4.21`。pnpm 安装会给出该上游 peer 警告；锁文件、类型检查、Vitest 和 Web/uni-app 构建均已通过。该警告不得以 `--no-strict-peer-dependencies`、忽略 lockfile 或隐式升级 Vue 的方式掩盖；后续仅在 DCloud 发布兼容版本后，经变更记录和跨端回归再升级。
