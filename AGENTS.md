# Project Agent Rules

## Project scope

- Repository: `https://github.com/chenbinglei/charge-ai`
- Current engineering root: `ev-charging-operations/`
- Current architecture and requirement baseline is maintained under `ev-charging-operations/docs/`.

## Git workflow

1. Before changes, run `git status -sb` and inspect the affected files.
2. Use a descriptive branch: `agent/<scope>` for assisted work, or `feat/<scope>`, `fix/<scope>`, `docs/<scope>` for team work.
3. Keep commits focused and use Conventional Commits, for example `docs: add implementation plan` or `feat(platform): add tenant authorization`.
4. Push branches to `origin`; create a draft pull request to `main` by default. Direct pushes to `main` require the repository owner's explicit instruction.
5. Before push, run the checks appropriate to the change and report the result. Never stage unrelated files.

## Credential safety

- Never commit, print, paste, or store token values, passwords, private keys, certificates, merchant secrets, production connection strings, or device credentials.
- GitHub authentication is provided through the local `gh` session and operating-system keychain. Do not place personal access tokens in remote URLs, Git config, shell history, `.env` files, documentation, issues, or pull requests.
- Record only credential metadata and ownership in [代码仓库与凭据管理规范](ev-charging-operations/docs/standards/代码仓库与凭据管理规范-v1.md). Store actual values in the approved password manager or secret manager.
- If a credential is exposed in chat, source control, logs, or an issue, revoke/rotate it immediately, remove the exposure, and record the incident without including the secret value.
