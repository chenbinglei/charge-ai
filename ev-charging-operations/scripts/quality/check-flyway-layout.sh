#!/usr/bin/env bash
set -euo pipefail

# 仅校验已进入仓库的前向迁移；W1 不创建领域迁移，后续周通过该脚本阻止命名和回退违规。
repository_root=$(git rev-parse --show-toplevel)
database_root="${repository_root}/ev-charging-operations/database/mysql"

if [[ ! -d "${database_root}" ]]; then
  echo "缺少 MySQL Flyway 根目录：${database_root}" >&2
  exit 1
fi

allowed_schemas=(
  evco_iam
  evco_master
  evco_customer
  evco_trade
  evco_marketing
  evco_operations
  evco_v2g
  evco_finance
  evco_device_connectivity
  evco_integration
  evco_ecosystem
)

migration_count=0
while IFS= read -r migration_file; do
  migration_count=$((migration_count + 1))
  relative_path=${migration_file#"${database_root}/"}
  schema_name=${relative_path%%/*}
  filename=${migration_file##*/}

  schema_registered=false
  for allowed_schema in "${allowed_schemas[@]}"; do
    if [[ "${schema_name}" == "${allowed_schema}" ]]; then
      schema_registered=true
      break
    fi
  done

  if [[ "${schema_registered}" != true ]]; then
    echo "迁移位于未登记的 Schema：${relative_path}" >&2
    exit 1
  fi

  if [[ "${relative_path}" != "${schema_name}/migration/${filename}" ]]; then
    echo "迁移必须位于 <schema>/migration 目录：${relative_path}" >&2
    exit 1
  fi

  if [[ ! "${filename}" =~ ^V[0-9]{12}__[a-z0-9_]+\.sql$ ]]; then
    echo "迁移命名不符合 VyyyyMMddHHmm__lower_snake_case.sql：${relative_path}" >&2
    exit 1
  fi

  if [[ ! -s "${migration_file}" ]]; then
    echo "禁止空迁移：${relative_path}" >&2
    exit 1
  fi

  if rg --ignore-case --quiet '(^|[[:space:];])(drop[[:space:]]+(table|schema|database)|truncate[[:space:]]+table|delete[[:space:]]+from)[[:space:]]' "${migration_file}"; then
    echo "迁移不得包含破坏性清理语句；请以补偿迁移或恢复演练处理：${relative_path}" >&2
    exit 1
  fi
done < <(find "${database_root}" -type f -path '*/migration/*.sql' -print | sort)

if find "${database_root}" -type f -name 'U*.sql' -print -quit | rg --quiet .; then
  echo "禁止 down migration；修复须使用新的前向补偿迁移。" >&2
  exit 1
fi

echo "Flyway 目录校验通过：已检查 ${migration_count} 个前向迁移。"
