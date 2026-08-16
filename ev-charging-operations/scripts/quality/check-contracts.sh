#!/usr/bin/env bash
set -euo pipefail

# 契约只做语法和 W1 边界校验；资源级 OpenAPI/AsyncAPI 在对应模块设计通过后扩充。
repository_root=$(git rev-parse --show-toplevel)
contracts_root="${repository_root}/ev-charging-operations/contracts"

json_count=0
while IFS= read -r json_file; do
  node --input-type=module --eval 'import { readFileSync } from "node:fs"; JSON.parse(readFileSync(process.argv[1], "utf8"))' "${json_file}"
  json_count=$((json_count + 1))
done < <(find "${contracts_root}" -type f -name '*.json' -print | sort)

yaml_count=0
while IFS= read -r yaml_file; do
  ruby -e 'require "yaml"; YAML.safe_load(File.read(ARGV.fetch(0)), aliases: true)' "${yaml_file}"
  yaml_count=$((yaml_count + 1))
done < <(find "${contracts_root}" -type f \( -name '*.yaml' -o -name '*.yml' \) -print | sort)

asyncapi_file="${contracts_root}/events/asyncapi/event-envelope-v1.yaml"
if ! rg --fixed-strings --quiet 'channels: {}' "${asyncapi_file}"; then
  echo "W1 公共 AsyncAPI 不得声明运行时业务 Topic。" >&2
  exit 1
fi

echo "契约语法及 W1 事件边界校验通过：${json_count} 个 JSON、${yaml_count} 个 YAML。"
