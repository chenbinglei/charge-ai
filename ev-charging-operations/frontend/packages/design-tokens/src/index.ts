import type { ThemeName } from "@evco/types";

/** 三种主题共享的排版、栅格和响应式语义 Token。 */
export const layoutTokens = {
  /** 受控流式（DEC-20260820-008/009）：内容区不设全局最大宽度，仅语义容器局部封顶。 */
  formMaxWidth: "1200px",
  textMaxWidth: "1000px",
  /** 四档断点（§5.1）：>=1440 桌面 / >=992 笔记本 / >=768 平板 / <768 手机。 */
  breakpoints: {
    desktop: "1440px",
    laptop: "992px",
    tablet: "768px",
  } as const,
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
