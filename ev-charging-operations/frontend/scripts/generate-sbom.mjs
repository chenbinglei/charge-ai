import { createHash } from "node:crypto";
import { execFileSync } from "node:child_process";
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

/** 从 pnpm 的实际递归依赖图生成可归档的 CycloneDX 1.6 JSON，不依赖不兼容的 npm CLI 参数。 */
const workspaceRoot = dirname(dirname(fileURLToPath(import.meta.url)));
const outputPath = join(workspaceRoot, "artifacts", "frontend-sbom.json");
const lockfile = readFileSync(join(workspaceRoot, "pnpm-lock.yaml"));
const output = execFileSync(
  "pnpm",
  ["list", "--recursive", "--json", "--depth", "Infinity"],
  { cwd: workspaceRoot, encoding: "utf8" },
);
const workspaceTrees = JSON.parse(output);
const components = new Map();

/** 递归提取 pnpm 已解析依赖，保留包版本和来源 URL。 */
function collectDependencies(dependencies) {
  if (dependencies === undefined) {
    return;
  }

  for (const [packageName, dependency] of Object.entries(dependencies)) {
    if (typeof dependency !== "object" || dependency === null) {
      continue;
    }

    const typedDependency = dependency;
    if (typeof typedDependency.version === "string") {
      const reference = `${packageName}@${typedDependency.version}`;
      components.set(reference, {
        type: "library",
        name: packageName,
        version: typedDependency.version,
        purl: `pkg:npm/${encodeURIComponent(packageName)}@${typedDependency.version}`,
        externalReferences:
          typeof typedDependency.resolved === "string"
            ? [{ type: "distribution", url: typedDependency.resolved }]
            : undefined,
      });
    }

    collectDependencies(typedDependency.dependencies);
    collectDependencies(typedDependency.devDependencies);
    collectDependencies(typedDependency.optionalDependencies);
    collectDependencies(typedDependency.peerDependencies);
  }
}

for (const workspaceTree of workspaceTrees) {
  collectDependencies(workspaceTree.dependencies);
  collectDependencies(workspaceTree.devDependencies);
  collectDependencies(workspaceTree.optionalDependencies);
}

const lockHash = createHash("sha256").update(lockfile).digest("hex");
const rootPackage = JSON.parse(
  readFileSync(join(workspaceRoot, "package.json"), "utf8"),
);
const bom = {
  bomFormat: "CycloneDX",
  specVersion: "1.6",
  serialNumber: `urn:uuid:${lockHash.slice(0, 8)}-${lockHash.slice(8, 12)}-${lockHash.slice(12, 16)}-${lockHash.slice(16, 20)}-${lockHash.slice(20, 32)}`,
  version: 1,
  metadata: {
    component: {
      type: "application",
      name: rootPackage.name,
      version: rootPackage.version,
    },
    properties: [
      { name: "evco:lockfile-sha256", value: lockHash },
      { name: "evco:package-manager", value: rootPackage.packageManager },
    ],
  },
  components: [...components.values()].sort((left, right) =>
    left.purl.localeCompare(right.purl),
  ),
};

mkdirSync(dirname(outputPath), { recursive: true });
writeFileSync(outputPath, `${JSON.stringify(bom, null, 2)}\n`, "utf8");
console.log(`已生成前端 CycloneDX SBOM：${bom.components.length} 个依赖组件。`);
