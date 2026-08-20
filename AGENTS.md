# Project Agent Rules

## Project scope

- Repository: `https://github.com/chenbinglei/charge-ai`
- Current engineering root: `ev-charging-operations/`
- Current architecture and requirement baseline is maintained under `ev-charging-operations/docs/`.

## Mandatory session recovery and delivery governance

Before planning, implementation, review, or a status report for the charging platform, read these files in order:

1. This `AGENTS.md`.
2. `ev-charging-operations/docs/requirements/充电运营平台12周全功能交付实施计划-v3.md` — current approved scope, dates and delivery sequence.
3. `ev-charging-operations/delivery/execution/00-执行总控台账.md` — current module, forecast, blockers and next action.
4. `ev-charging-operations/delivery/execution/01-模块路线图与依赖矩阵.md` — prerequisites and mandatory regression scope.
5. For a change, `02-变更与审批日志.md`; for completed work, `03-模块完成记录.md` and `04-一致性核对清单.md`.
6. `ev-charging-operations/delivery/execution/05-SVN待同步清单.md` — company-intranet SVN availability and any offsite backlog.

The approved target is 2026-08-17 to 2026-11-20, with 60 committed development workdays and no scheduled development from 2026-09-25 through 2026-10-07. This is a target, not authorization to cut scope or test gates. Weekends are not committed capacity; any overtime, schedule delay, new feature, or change to an accepted module requires the user's recorded approval.

Each module must remain serial: requirements/acceptance confirmation → backend, migrations and contracts → API automated plus manual verification → frontend → E2E, permissions and exception verification → completion record, documentation sync, commit and review. Do not start a module's frontend before its API gate passes. Do not mark it complete on the strength of code or a page alone.

DEC-20260814-010 covers DEC-20260814-009: domestic provincial/city regulatory work remains in the current 106-item W1–W12/60-workday commitment and is delivered by the current seventh `ecosystem-service` in W10. Only virtual power plant and other external-ecosystem data/control capabilities are separately approved post-W12 extensions. `ecosystem-service` must not give regulators device control; do not create future VPP/third-party Topic, consumer, protocol endpoint or page before its separate approval.

Only the primary implementation agent may modify production code, migrations, contracts, tests, configuration and project documents. Architecture, testing, security and change-impact agents are read-only reviewers unless the user explicitly changes that authority. Do not use multiple simultaneous writers on the same module.

### Mandatory read-only review-agent countersign

- The single source of reviewer definitions is `ev-charging-operations/.agents/` (see its README). The primary implementation agent must explicitly run reviews from each required reviewer's "专长定位" and "固定判断原则"; it must never claim "review passed" in its own voice without a recorded countersign.
- Step-gate reviewer mapping and rules are governed by [单人AI协作开发与变更治理规范](ev-charging-operations/docs/standards/单人AI协作开发与变更治理规范-v1.md) §2.1: Step 2 → architecture-convergence trio plus domain specialists; Step 3 → consistency/messaging reviewer plus domain specialists; Steps 4/6 → authorization governance specialist plus domain specialists; Step 5 → requirements story mapper. MQTT/EMQX/device/metering/V2G work requires the charging-IOT-V2G integration specialist as a mandatory countersigner.
- Every gate release requires a review minutes file in `ev-charging-operations/delivery/audit/` named `Wx-StepN评审纪要-vN.md`, with each finding marked 已确认/推断/待确认 and backed by evidence (tests, diffs, document locations). P0/P1 findings must be fixed and re-reviewed before the next step. Minutes are a required gate input but never substitute for the user's recorded approval.

M1 is a channel-neutral, publicly available personal charging product. P1 must not configure M1 page or function permissions for individual users: application lifecycle, content, approved business switches and user eligibility are not role/page authorization. WeChat is the required first live channel; Alipay mini-program configuration, identity/payment abstraction and disabled-state support are part of the current plan. A future Alipay client launch or real Alipay payment integration needs the user's approved change record and its own channel E2E; it must not duplicate the underlying personal-user, order, wallet, benefit, invoice or V2G facts. M1 channel authorization in W2 must idempotently create or update the unified personal-user master and its channel identity mapping; the P1 personal-user 360° operations view follows in W6.

Before real WeChat configuration is available, channel adapters may use deterministic mocks and desensitized fixtures for development, API and E2E verification. A mock never completes C1/C2: real WeChat login, payment and original-route refund remain production release gates, and must be retested after the user maintains configuration through P1. V2G settlement posts only to the user's independent V2G earnings balance in this release; cash withdrawal, payout and withdrawal records remain disabled pending a separate user decision.

Personal-wallet cash top-ups must be traceable funding lots that retain the original payment transaction, merchant route and refundable balance; non-cash benefits are never a cash-refund source. A merchant change only routes new transactions to the new merchant: the old merchant remains available for historical queries, original-route refunds and reconciliation until its historical obligations are cleared. Tenant settlement follows the charging order's station-settlement snapshot, not the payment merchant or funding lot; unused top-up refunds never enter tenant settlement, while charging-order refunds or corrections create an adjustment and never rewrite a locked or paid bill.

## Git workflow

1. Before changes, run `git status -sb` and inspect the affected files.
2. Use a descriptive branch: `agent/<scope>` for assisted work, or `feat/<scope>`, `fix/<scope>`, `docs/<scope>` for team work.
3. Keep commits focused and use Conventional Commits with a Chinese summary by default, for example `docs: 增加实施计划` or `feat(platform): 增加租户授权`. Retain English only for product names, protocol names, paths, identifiers, or terms that cannot be stated precisely in Chinese.
4. Push branches to `origin`; create a draft pull request to `main` by default. Direct pushes to `main` require the repository owner's explicit instruction.
5. Before push, run the checks appropriate to the change and report the result. Never stage unrelated files.

## Dual-VCS workflow and rollback

- This project is managed concurrently by Git and SVN. Git remains the module-level review and release history; SVN is the required fine-grained backup history at `https://win-eng4s42rbjg/svn/software/elink/trunk/charge-ai`. SVN credentials must remain in the operating-system keychain or SVN credential store and must never be recorded in this repository.
- SVN is available only from the company intranet. Before changing files, inspect both `git status -sb` and `svn status`; before a new work session, run `svn update` and resolve any incoming change before editing. If the update or commit cannot reach the intranet, record every completed change in `delivery/execution/05-SVN待同步清单.md`, commit it to Git, and mark its SVN state as pending. The initial queue row may use `待生成` for the Git commit; immediately after Git returns the hash, backfill that hash into the queue in the next focused Git evidence commit. An authentication, authorization, certificate or merge-conflict failure is not an offsite exception: stop and resolve or report it instead of misclassifying it as a pending sync.
- After every completed functional, configuration, test, or documentation change passes its applicable verification and required documentation sync, add only that change to SVN and commit it immediately with a concise Chinese message when the intranet is reachable. Outside the company, preserve the same scope, test evidence and intended Chinese SVN message in the pending list; do not claim the module or release fully complete until the actual SVN revision is recorded.
- On the next company-intranet session, SVN reconciliation is the first action before new development: run `svn update`, review the pending list oldest first, verify scope and tests, commit each queued change to SVN, then write the resulting revision and completion time back to the list. Do not use an external mirror, force operation or history rewrite as a substitute for the internal SVN commit.
- At the completion of a governed module, make the focused Conventional Commit in Git after its required gates and review. Use Chinese summaries by default. Record both the Git commit and the covered SVN revision(s) in module completion evidence; Git commits may aggregate the already-committed, coherent SVN changes for that module.
- Keep the two trees content-identical except for administrative directories (`.git/`, `.svn/`) and intentionally ignored local artifacts. Before any Git or SVN commit, inspect the corresponding diff and status, run the applicable tests, and scan for sensitive files. Do not commit `.git/`, `.svn/`, credentials, local environments, or build artifacts.
- Roll back committed work through a new Git revert or an SVN reverse merge/revert commit, preserving history and recording the reason, affected revision(s), verification and recovery result. Never make a code change that cannot be traced to a Git commit or SVN revision after it is declared complete.

## Credential safety

- Never commit, print, paste, or store token values, passwords, private keys, certificates, merchant secrets, production connection strings, or device credentials.
- GitHub authentication is provided through the local `gh` session and operating-system keychain. Do not place personal access tokens in remote URLs, Git config, shell history, `.env` files, documentation, issues, or pull requests.
- Record only credential metadata and ownership in [代码仓库与凭据管理规范](ev-charging-operations/docs/standards/代码仓库与凭据管理规范-v1.md). Store actual values in the approved password manager or secret manager.
- If a credential is exposed in chat, source control, logs, or an issue, revoke/rotate it immediately, remove the exposure, and record the incident without including the secret value.

## Change control and completion evidence

- Before changing an accepted module, create a change record that states reason, affected modules/API/database/frontend, compatibility and migration approach, regression scope, schedule impact, and the user's approval. Never rewrite an executed Flyway migration or silently make a breaking API change.
- At every module exit, update the execution ledger with requirement, contract, migration, API test, frontend, E2E, documentation and review evidence. Update affected Runbooks and user/API documents in the same change.
- C1 (M1 WeChat login configuration) and C2 (WeChat payment configuration) are user-maintenance notifications recorded in the execution ledger. Request configuration through P1 only; never request or record secret values in chat, Git, logs, exports or documentation.
