<script setup lang="ts">
import { nextTick, ref } from "vue";
import { useRouter } from "vue-router";
import { ElDropdown, ElDropdownItem, ElDropdownMenu } from "element-plus";
import { useTabsStore } from "@/stores/tabs";

/**
 * 完整版功能标签栏（DEC-20260820-019）：
 * 标签切换/单个关闭/溢出横向滚动 + 右侧下拉（刷新当前页/关闭当前/关闭其他/关闭全部）；
 * 首个标签（首页）固定不可关闭。
 */
const router = useRouter();
const tabs = useTabsStore();

const scrollRef = ref<HTMLElement | null>(null);

/** 切换标签。 */
function switchTab(path: string): void {
  if (path !== tabs.activePath) {
    void router.push(path);
  }
}

/** 关闭标签；若关闭当前标签，removeTab 返回需跳转的相邻标签。 */
function handleClose(path: string): void {
  const next = tabs.removeTab(path);
  if (next !== null && next !== "") {
    void router.push(next);
  }
}

/** 标签进入可视区（新增标签在末尾时保证可见）。 */
function scrollActiveIntoView(): void {
  void nextTick(() => {
    const el = scrollRef.value?.querySelector(".tagsview__tag.is-active");
    el?.scrollIntoView({ block: "nearest", inline: "nearest" });
  });
}

function handleCommand(command: string): void {
  const active = tabs.activePath;
  if (command === "refresh") {
    tabs.refreshCurrent();
    return;
  }
  if (active === "") {
    return;
  }
  if (command === "close-current") {
    handleClose(active);
    return;
  }
  if (command === "close-others") {
    tabs.closeOthers(active);
    return;
  }
  if (command === "close-all") {
    const fixed = tabs.closeAll();
    if (fixed !== null && fixed !== "") {
      void router.push(fixed);
    }
  }
}

defineExpose({ scrollActiveIntoView });
</script>

<template>
  <div class="tagsview">
    <div ref="scrollRef" class="tagsview__scroll">
      <button
        v-for="tab in tabs.tabs"
        :key="tab.path"
        type="button"
        class="tagsview__tag"
        :class="{ 'is-active': tab.path === tabs.activePath }"
        :title="tab.title"
        @click="switchTab(tab.path)"
        @transitionend="scrollActiveIntoView"
      >
        <span class="tagsview__tagDot" aria-hidden="true" />
        <span class="tagsview__tagTitle">{{ tab.title }}</span>
        <span
          v-if="tabs.fixedTab?.path !== tab.path"
          class="tagsview__tagClose"
          role="button"
          aria-label="关闭标签"
          @click.stop="handleClose(tab.path)"
        >
          ×
        </span>
        <span v-else class="tagsview__tagPin" title="首页，固定不可关闭"> Home </span>
      </button>
    </div>
    <ElDropdown class="tagsview__actions" trigger="click" @command="handleCommand">
      <!-- 下拉箭头：内联 SVG 替换文本 ▾（高分屏文本箭头细小不可见，2026-08-21） -->
      <button type="button" class="tagsview__more" aria-label="标签操作">
        <svg width="14" height="14" viewBox="0 0 16 16" fill="none">
          <path
            d="M4 6 L8 10 L12 6"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>
      <template #dropdown>
        <ElDropdownMenu>
          <ElDropdownItem command="refresh">刷新当前页</ElDropdownItem>
          <ElDropdownItem command="close-current">关闭当前</ElDropdownItem>
          <ElDropdownItem command="close-others">关闭其他</ElDropdownItem>
          <ElDropdownItem command="close-all">关闭全部</ElDropdownItem>
        </ElDropdownMenu>
      </template>
    </ElDropdown>
  </div>
</template>

<style scoped lang="scss">
.tagsview {
  box-sizing: border-box;
  display: flex;
  height: 34px;
  flex-shrink: 0;
  align-items: stretch;
  border-bottom: 1px solid var(--evco-border);
  background: var(--evco-surface);
}

.tagsview__scroll {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 4px;
  padding: 0 8px;
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.tagsview__tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 26px;
  padding: 0 8px;
  border: 1px solid var(--evco-border);
  border-radius: 4px;
  background: transparent;
  color: var(--evco-text-secondary);
  font: var(--evco-font-assistive);
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.2s ease, border-color 0.2s ease, background 0.2s ease;

  &:hover {
    color: var(--evco-primary);
  }

  &.is-active {
    border-color: var(--evco-primary);
    background: color-mix(in srgb, var(--evco-primary) 10%, transparent);
    color: var(--evco-primary);
  }
}

.tagsview__tagDot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--evco-text-secondary);
  opacity: 0.4;
}

.tagsview__tag.is-active .tagsview__tagDot {
  background: var(--evco-primary);
  opacity: 1;
}

.tagsview__tagTitle {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tagsview__tagClose {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  border-radius: 2px;
  font-size: 12px;
  line-height: 1;

  &:hover {
    background: color-mix(in srgb, var(--evco-status-error, #e5484d) 14%, transparent);
    color: var(--evco-status-error, #e5484d);
  }
}

.tagsview__tagPin {
  color: var(--evco-text-secondary);
  font-size: 10px;
  opacity: 0.7;
}

.tagsview__actions {
  display: flex;
  align-items: center;
  border-left: 1px solid var(--evco-border);
  padding: 0 4px;
}

.tagsview__more {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 26px;
  border: 0;
  background: transparent;
  color: var(--evco-text-secondary);
  cursor: pointer;

  &:hover {
    color: var(--evco-primary);
  }
}
</style>
