import { defineStore } from "pinia";
import { computed, ref } from "vue";

/** 功能标签项：path 唯一键 + 菜单标题。 */
export interface TabItem {
  path: string;
  title: string;
}

/**
 * 多标签页状态（完整版 TagsView，DEC-20260820-019）：
 * 开标签/高亮/单个关闭/关闭其他/关闭全部/刷新当前页；
 * 首页标签固定不可关闭；导航跳转兜底由路由层完成。
 */
export const useTabsStore = defineStore("tabs", () => {
  const tabs = ref<TabItem[]>([]);
  const activePath = ref<string>("");
  /** RouterView 渲染 key 的刷新计数：自增即强制重建当前页组件。 */
  const refreshTick = ref(0);

  /** 固定标签（首页，第一个可见菜单）；tabs[0]。 */
  const fixedTab = computed(() => tabs.value[0] ?? null);

  /** 初始化固定标签；登录成功进入主布局时调用一次。 */
  function initFixed(path: string, title: string): void {
    if (tabs.value.length === 0 && path !== "") {
      tabs.value = [{ path, title, }];
      activePath.value = path;
    }
  }

  /** 路由进入：确保标签存在并高亮。 */
  function ensureTab(path: string, title: string): void {
    activePath.value = path;
    if (!tabs.value.some((tab) => tab.path === path)) {
      tabs.value.push({ path, title });
    }
  }

  /** 关闭单个标签；关闭当前标签时跳相邻标签（优先右侧，无则左侧）。 */
  function removeTab(path: string): string | null {
    if (fixedTab.value?.path === path) {
      return null; // 固定标签不可关闭
    }
    const index = tabs.value.findIndex((tab) => tab.path === path);
    if (index === -1) {
      return null;
    }
    tabs.value.splice(index, 1);
    if (activePath.value === path) {
      const next = tabs.value[index] ?? tabs.value[index - 1];
      activePath.value = next?.path ?? "";
      return activePath.value;
    }
    return null;
  }

  /** 关闭其他：仅保留当前与固定标签；返回应跳转 path（固定标签被关时）。 */
  function closeOthers(path: string): void {
    tabs.value = tabs.value.filter(
      (tab) => tab.path === path || tab.path === fixedTab.value?.path,
    );
    activePath.value = path;
  }

  /** 关闭全部：仅剩固定标签并跳转。 */
  function closeAll(): string | null {
    const fixed = fixedTab.value;
    if (fixed === null) {
      tabs.value = [];
      activePath.value = "";
      return null;
    }
    tabs.value = [fixed];
    activePath.value = fixed.path;
    return fixed.path;
  }

  /** 刷新当前页：自增渲染 key 强制重建组件。 */
  function refreshCurrent(): void {
    refreshTick.value += 1;
  }

  /** 登出清空标签状态。 */
  function reset(): void {
    tabs.value = [];
    activePath.value = "";
    refreshTick.value = 0;
  }

  return {
    tabs,
    activePath,
    refreshTick,
    fixedTab,
    initFixed,
    ensureTab,
    removeTab,
    closeOthers,
    closeAll,
    refreshCurrent,
    reset,
  };
});
