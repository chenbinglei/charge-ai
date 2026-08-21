#!/usr/bin/env bash
set -euo pipefail

# 汇总 W1 无业务工程底座的离线可验证门禁。
repository_root=$(git rev-parse --show-toplevel)
cd "${repository_root}"

for required_path in \
  .github/dependency-review-config.yml \
  .mvn/wrapper/maven-wrapper.properties \
  mvnw \
  ev-charging-operations/platform/compose.yaml \
  ev-charging-operations/contracts/openapi/platform-foundation-v1.yaml; do
  if [[ ! -e "${required_path}" ]]; then
    echo "缺少 W1 工程资产：${required_path}" >&2
    exit 1
  fi
done

if rg --ignore-case --quiet 'image:[[:space:]]+[^[:space:]@]+(:latest|@latest)' ev-charging-operations/platform/compose.yaml; then
  echo "Compose 禁止 latest 镜像。" >&2
  exit 1
fi

while IFS= read -r image_line; do
  if [[ "${image_line}" != *'@sha256:'* ]]; then
    echo "Compose 镜像必须同时固定 tag 和 digest：${image_line}" >&2
    exit 1
  fi
done < <(rg '^    image:' ev-charging-operations/platform/compose.yaml)

bash ev-charging-operations/scripts/quality/check-flyway-layout.sh
bash ev-charging-operations/scripts/quality/check-contracts.sh
bash ev-charging-operations/scripts/quality/check-sensitive-files.sh

echo "W1 工程底座离线校验通过。"
