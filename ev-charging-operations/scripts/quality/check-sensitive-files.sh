#!/usr/bin/env bash
set -euo pipefail

# 只扫描高置信度密钥特征；命中后不打印文件内容，避免二次扩散潜在敏感信息。
repository_root=$(git rev-parse --show-toplevel)
cd "${repository_root}"

if find . -type f -name '.env' -not -path './.git/*' -print -quit | rg --quiet .; then
  echo "仓库中存在 .env 文件；请移除并仅保留 .env.example。" >&2
  exit 1
fi

if rg --files -g '!**/.git/**' -g '!**/target/**' -g '!**/node_modules/**' \
  | xargs rg --files-with-matches --no-messages \
      '(-----BEGIN (RSA |EC |OPENSSH |)PRIVATE KEY-----|ghp_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}|AKIA[0-9A-Z]{16}|sk_live_[A-Za-z0-9]{16,}|rk_live_[A-Za-z0-9]{16,})' \
  | rg --quiet .; then
  echo "检测到疑似明文密钥；请立即移除、轮换并按凭据规范记录事件。" >&2
  exit 1
fi

echo "敏感文件高置信度扫描通过。"
