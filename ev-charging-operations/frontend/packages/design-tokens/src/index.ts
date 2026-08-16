import type { ThemeName } from "@evco/types";

/** 三种主题共享的排版、栅格和响应式语义 Token。 */
export const layoutTokens = {
  contentMaxWidth: "1440px",
  gridColumns: 24,
  headerHeight: "64px",
  sidebarCollapsedWidth: "64px",
  sidebarExpandedWidth: "210px",
  spacingUnit: "8px",
} as const;

/** 所有主题必须存在，主题切换不得改变信息架构或状态语义。 */
export const themeNames: readonly ThemeName[] = [
  "energy-green",
  "graphite-night",
  "command-deep",
];

/** 校验调用方只使用已批准主题。 */
export function isThemeName(value: string): value is ThemeName {
  return themeNames.includes(value as ThemeName);
}
