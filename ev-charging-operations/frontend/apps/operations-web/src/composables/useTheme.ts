import { ref, watchEffect } from "vue";
import type { ThemeName } from "@evco/types";

/** 主题持久化键（仅本应用命名空间）。 */
const STORAGE_KEY = "evco.theme";

function loadTheme(): ThemeName {
  const saved = localStorage.getItem(STORAGE_KEY);
  return saved === "graphite-night" || saved === "command-deep" ? saved : "energy-green";
}

/**
 * 全局主题状态（三档，DEC-20260820-018）：
 * energy-green 亮色默认 / graphite-night 暗色 / command-deep 深蓝科技（原 D1 专用，现开放 P1）。
 * 挂载点统一在 <html data-theme>；两档暗色同时挂 Element Plus dark class 联动组件配色。
 */
const current = ref<ThemeName>(loadTheme());

watchEffect(() => {
  document.documentElement.dataset.theme = current.value;
  document.documentElement.classList.toggle("dark", current.value !== "energy-green");
  localStorage.setItem(STORAGE_KEY, current.value);
});

/** 三档主题选择器公共逻辑；视图层直接消费 current/setTheme。 */
export function useTheme() {
  function setTheme(theme: ThemeName): void {
    current.value = theme;
  }

  return { current, setTheme };
}
