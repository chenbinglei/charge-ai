import { readFileSync, readdirSync, existsSync } from "node:fs";
import { join } from "node:path";

const workspaceRoot = new URL("..", import.meta.url).pathname;
const applicationsRoot = join(workspaceRoot, "apps");
const expectedApplications = [
  "consumer-miniapp",
  "enterprise-admin-miniapp",
  "enterprise-miniapp",
  "enterprise-web",
  "operations-dashboard",
  "operations-web",
  "tenant-operations-miniapp",
];
const forbiddenMiniPackages = new Set([
  "element-plus",
  "echarts",
  "@vitejs/plugin-vue",
]);
const forbiddenBrowserGlobals =
  /\b(document|window|localStorage|sessionStorage)\b/;

for (const applicationName of expectedApplications) {
  const packagePath = join(applicationsRoot, applicationName, "package.json");
  if (!existsSync(packagePath)) {
    throw new Error(`缺少七端应用包：${applicationName}`);
  }

  const packageJson = JSON.parse(readFileSync(packagePath, "utf8"));
  const isMiniapp = applicationName.endsWith("-miniapp");
  const dependencies = Object.keys({
    ...(packageJson.dependencies ?? {}),
    ...(packageJson.devDependencies ?? {}),
  });

  if (isMiniapp) {
    for (const dependencyName of dependencies) {
      if (forbiddenMiniPackages.has(dependencyName)) {
        throw new Error(
          `${applicationName} 不得引入 Web 专属依赖：${dependencyName}`,
        );
      }
    }

    const sourceRoot = join(applicationsRoot, applicationName, "src");
    const sourceFiles = readdirSync(sourceRoot, { recursive: true });
    for (const sourceFile of sourceFiles) {
      if (typeof sourceFile !== "string" || !sourceFile.endsWith(".vue")) {
        continue;
      }
      const source = readFileSync(join(sourceRoot, sourceFile), "utf8");
      if (forbiddenBrowserGlobals.test(source)) {
        throw new Error(
          `${applicationName} 不得在小程序源码中使用浏览器 DOM：${sourceFile}`,
        );
      }
    }
  }
}

console.log("七端应用与 Web/uni-app 边界校验通过。");
