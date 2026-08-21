<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElAvatar, ElDropdown, ElDropdownItem, ElDropdownMenu } from "element-plus";
import { useAuthStore } from "@/stores/auth";
import { useTabsStore } from "@/stores/tabs";
import { useTheme } from "@/composables/useTheme";
import { menuIconOf } from "@/router/menuIcons";
import LogoMark from "@/components/LogoMark.vue";
import MenuIcon from "@/components/MenuIcon.vue";
import TagsView from "@/components/TagsView.vue";
import type { MenuNodeVO, ThemeName } from "@evco/types";

/** P1 三档主题（DEC-20260820-018）：亮色默认 / 暗色 / 深蓝科技。 */
const THEME_OPTIONS: ReadonlyArray<{ value: ThemeName; label: string; swatch: string }> = [
  { value: "energy-green", label: "亮色主题", swatch: "#00b578" },
  { value: "graphite-night", label: "暗色主题", swatch: "#171b23" },
  { value: "command-deep", label: "科技蓝主题", swatch: "#0d2233" },
];

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const tabs = useTabsStore();
const { current, setTheme } = useTheme();

/* ---------- 实时时钟（北京时间，秒级刷新） ---------- */
const WEEKDAYS = ["日", "一", "二", "三", "四", "五", "六"] as const;
const clockText = ref("");
let clockTimer: ReturnType<typeof setInterval> | undefined;

function refreshClock(): void {
  const now = new Date();
  const pad = (value: number): string => String(value).padStart(2, "0");
  clockText.value =
    `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ` +
    `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())} ` +
    `星期${WEEKDAYS[now.getDay()] ?? ""}`;
}

onMounted(() => {
  refreshClock();
  clockTimer = setInterval(refreshClock, 1000);
});

onBeforeUnmount(() => {
  if (clockTimer !== undefined) {
    clearInterval(clockTimer);
  }
});

/* ---------- 用户信息 ---------- */
const displayName = computed(
  () => auth.profile?.displayName ?? auth.profile?.username ?? "—",
);
const avatarText = computed(() => displayName.value.charAt(0));
const deploymentModeText = computed(() =>
  auth.profile?.deploymentMode === "IOT_COLLABORATIVE" ? "IOT 协同" : "独立部署",
);

async function handleLogout(): Promise<void> {
  await auth.logout();
  tabs.reset();
  await router.replace("/login");
}

/* ---------- 菜单：一级手风琴互斥（可再折叠）+ 二级独立折叠 ---------- */
interface TopGroup {
  key: string;
  title: string;
  path: string;
  children: MenuNodeVO[];
}

const topGroups = computed<TopGroup[]>(() =>
  (auth.profile?.menus ?? []).map((node) => ({
    key: node.key,
    title: node.title,
    path: node.path,
    children: node.children ?? [],
  })),
);

/** 当前展开的一级 key（手风琴互斥；null 表示全部折叠）。 */
const expandedKey = ref<string | null>(null);
/** 已折叠的二级分组 key 集合（独立折叠，默认全展开）。 */
const collapsedSubKeys = ref<Set<string>>(new Set());
/** 默认展开是否已初始化（防止用户手动折叠后被路由 watch 重开）。 */
const expandedInited = ref(false);

/** 节点是否命中目标路径：叶子比 path，分组（path 为 null）递归子级。 */
function nodeMatches(node: MenuNodeVO, target: string): boolean {
  if (node.permission !== null) {
    return node.path === target;
  }
  return (node.children ?? []).some((child) => nodeMatches(child, target));
}

/** 按目标路径定位所属一级分组 key（权限驱动的默认展开依据）。 */
function findGroupKeyByPath(target: string | null | undefined): string | null {
  if (target === null || target === undefined || target === "") {
    return null;
  }
  const match = topGroups.value.find((group) =>
    group.children.some((child) => nodeMatches(child, target)),
  );
  return match?.key ?? null;
}

/** 路由联动：跳转页面时自动展开所属一级分组。 */
function syncExpandedByRoute(): void {
  const key = findGroupKeyByPath(route.path);
  if (key !== null) {
    expandedKey.value = key;
  }
}

watch(
  [topGroups, () => route.path],
  () => {
    // 初始化默认展开：以权限裁剪后菜单的首个可见叶子（登录落点）所属分组为准。
    if (!expandedInited.value && topGroups.value.length > 0) {
      expandedKey.value = findGroupKeyByPath(auth.firstVisiblePath);
      expandedInited.value = true;
    }
    syncExpandedByRoute();
  },
  { immediate: true },
);

/** 一级菜单切换：点击已展开项折叠，点击未展开项展开（互斥手风琴）。 */
function toggleGroup(key: string): void {
  expandedKey.value = expandedKey.value === key ? null : key;
}

/** 二级独立折叠切换。 */
function toggleSub(key: string): void {
  const next = new Set(collapsedSubKeys.value);
  if (next.has(key)) {
    next.delete(key);
  } else {
    next.add(key);
  }
  collapsedSubKeys.value = next;
}

/* ---------- 标签页：路由联动开标签 ---------- */
/** 叶子路径 → 菜单标题（标签标题来源）。 */
const leafTitleMap = computed(() => {
  const sink = new Map<string, string>();
  collectLeafTitles(auth.profile?.menus ?? [], sink);
  return sink;
});

function collectLeafTitles(nodes: MenuNodeVO[], sink: Map<string, string>): void {
  for (const node of nodes) {
    if (node.permission !== null) {
      sink.set(node.path, node.title);
    }
    if (node.children !== null && node.children.length > 0) {
      collectLeafTitles(node.children, sink);
    }
  }
}

/** 主布局挂载：初始化固定标签（首个可见菜单）。 */
const fixedInited = ref(false);

watch(
  [() => route.path, leafTitleMap],
  ([path]) => {
    if (typeof path !== "string" || path === "/" || path === "") {
      return;
    }
    if (!fixedInited.value && auth.firstVisiblePath !== null) {
      tabs.initFixed(auth.firstVisiblePath, leafTitleMap.value.get(auth.firstVisiblePath) ?? "首页");
      fixedInited.value = true;
    }
    const title = leafTitleMap.value.get(path);
    if (title !== undefined) {
      tabs.ensureTab(path, title);
    }
  },
  { immediate: true },
);

/* ---------- <992 侧栏抽屉 ---------- */
const sidebarOpen = ref(false);

watch(
  () => route.path,
  () => {
    sidebarOpen.value = false;
  },
);
</script>

<template>
  <div class="shell">
    <header class="shell__header">
      <button
        type="button"
        class="shell__burger"
        aria-label="切换导航菜单"
        @click="sidebarOpen = !sidebarOpen"
      >
        ☰
      </button>
      <div class="shell__brand">
        <LogoMark :size="30" />
        <span class="shell__brandName">充电运营管理平台</span>
      </div>
      <p class="shell__clock" aria-label="当前时间">{{ clockText }}</p>

      <div class="shell__actions">
        <span class="shell__mode" :title="`部署模式：${deploymentModeText}`">
          {{ deploymentModeText }}
        </span>
        <div class="shell__themes" aria-label="切换设计主题">
          <button
            v-for="option in THEME_OPTIONS"
            :key="option.value"
            type="button"
            class="shell__themeDot"
            :class="{ 'is-active': current === option.value }"
            :style="{ background: option.swatch }"
            :title="option.label"
            :aria-label="option.label"
            @click="setTheme(option.value)"
          />
        </div>
        <ElDropdown trigger="click" @command="(command: string) => command === 'logout' && handleLogout()">
          <button type="button" class="shell__user">
            <ElAvatar :size="30" class="shell__avatar">{{ avatarText }}</ElAvatar>
            <span class="shell__userName">{{ displayName }}</span>
          </button>
          <template #dropdown>
            <ElDropdownMenu>
              <ElDropdownItem disabled>
                {{ displayName }}（{{ auth.profile?.username ?? "—" }}）
              </ElDropdownItem>
              <ElDropdownItem divided command="logout">退出登录</ElDropdownItem>
            </ElDropdownMenu>
          </template>
        </ElDropdown>
      </div>
    </header>

    <div class="shell__body">
      <aside class="shell__sidebar" :class="{ 'is-open': sidebarOpen }">
        <nav class="sidebar__nav" aria-label="主导航">
          <template v-for="group in topGroups" :key="group.key">
            <!-- 一级：手风琴头（含语义图标） -->
            <button
              type="button"
              class="sidebar__top"
              :class="{ 'is-expanded': expandedKey === group.key }"
              :aria-expanded="expandedKey === group.key"
              @click="toggleGroup(group.key)"
            >
              <MenuIcon :icon="menuIconOf(group.path, group.key)" :size="16" class="sidebar__topIcon" />
              <span class="sidebar__topTitle">{{ group.title }}</span>
              <!-- 折叠箭头：SVG 矢量替换原 11px 文本 ▾（1920×1080 下不可见，2026-08-21 放大） -->
              <span class="sidebar__arrow" aria-hidden="true">
                <svg width="15" height="15" viewBox="0 0 16 16" fill="none">
                  <path
                    d="M4 6 L8 10 L12 6"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
              </span>
            </button>
            <!-- 展开区：二级（叶子或可折叠分组） + 三级叶子 -->
            <div v-if="expandedKey === group.key" class="sidebar__group">
              <template v-for="child in group.children" :key="child.key">
                <!-- 二级叶子 -->
                <RouterLink
                  v-if="child.permission !== null"
                  :to="child.path"
                  class="sidebar__leaf sidebar__leaf--d2"
                >
                  <MenuIcon :icon="menuIconOf(child.path, child.key)" :size="16" />
                  <span class="sidebar__leafTitle">{{ child.title }}</span>
                </RouterLink>
                <!-- 二级分组：独立折叠 -->
                <template v-else>
                  <button
                    type="button"
                    class="sidebar__sub"
                    :class="{ 'is-collapsed': collapsedSubKeys.has(child.key) }"
                    :aria-expanded="!collapsedSubKeys.has(child.key)"
                    @click="toggleSub(child.key)"
                  >
                    <MenuIcon :icon="menuIconOf(child.path, child.key)" :size="14" />
                    <span class="sidebar__subTitle">{{ child.title }}</span>
                    <span class="sidebar__arrow" aria-hidden="true">
                      <svg width="15" height="15" viewBox="0 0 16 16" fill="none">
                        <path
                          d="M4 6 L8 10 L12 6"
                          stroke="currentColor"
                          stroke-width="1.8"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        />
                      </svg>
                    </span>
                  </button>
                  <div v-if="!collapsedSubKeys.has(child.key)" class="sidebar__subGroup">
                    <RouterLink
                      v-for="leaf in child.children ?? []"
                      :key="leaf.key"
                      :to="leaf.path"
                      class="sidebar__leaf sidebar__leaf--d3"
                    >
                      <MenuIcon :icon="menuIconOf(leaf.path, leaf.key)" :size="14" />
                      <span class="sidebar__leafTitle">{{ leaf.title }}</span>
                    </RouterLink>
                  </div>
                </template>
              </template>
            </div>
          </template>
        </nav>
      </aside>

      <div class="shell__main">
        <!-- 功能标签栏（完整版） -->
        <TagsView />
        <main class="shell__content">
          <RouterView :key="`${route.fullPath}#${tabs.refreshTick}`" />
        </main>
      </div>
    </div>

    <div
      v-if="sidebarOpen"
      class="shell__overlay"
      aria-hidden="true"
      @click="sidebarOpen = false"
    />
  </div>
</template>

<style scoped lang="scss">
/* 固定框架：外层不滚动，菜单与内容各自独立滚动（§5.1） */
.shell {
  display: flex;
  height: 100vh;
  flex-direction: column;
  overflow: hidden;
  background: var(--evco-background);
  color: var(--evco-text-primary);
}

.shell__header {
  box-sizing: border-box;
  display: flex;
  height: var(--evco-header-height);
  flex-shrink: 0;
  align-items: center;
  gap: var(--evco-spacing-2);
  padding: 0 var(--evco-spacing-3);
  border-bottom: 1px solid var(--evco-border);
  background: var(--evco-surface);
}

.shell__burger {
  display: none;
  width: 32px;
  height: 32px;
  border: 1px solid var(--evco-border);
  border-radius: 4px;
  background: transparent;
  color: var(--evco-text-primary);
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
}

.shell__brand {
  display: flex;
  align-items: center;
  gap: var(--evco-spacing-1);
}

.shell__brandName {
  font: var(--evco-font-heading-2);
  color: var(--evco-primary);
  white-space: nowrap;
}

.shell__clock {
  margin: 0 0 0 var(--evco-spacing-2);
  padding-left: var(--evco-spacing-2);
  border-left: 1px solid var(--evco-border);
  color: var(--evco-text-secondary);
  font: var(--evco-font-assistive);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.shell__actions {
  display: flex;
  align-items: center;
  gap: var(--evco-spacing-2);
  margin-left: auto;
}

.shell__mode {
  padding: 2px 8px;
  border: 1px solid var(--evco-border);
  border-radius: 4px;
  color: var(--evco-text-secondary);
  font: var(--evco-font-assistive);
  white-space: nowrap;
}

.shell__themes {
  display: flex;
  gap: 6px;
  padding: 5px;
  border: 1px solid var(--evco-border);
  border-radius: 999px;
}

.shell__themeDot {
  width: 18px;
  height: 18px;
  border: 2px solid var(--evco-border);
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.15);
  }

  &.is-active {
    border-color: var(--evco-primary);
    box-shadow: 0 0 0 2px color-mix(in srgb, var(--evco-primary) 35%, transparent);
  }
}

.shell__user {
  display: flex;
  align-items: center;
  gap: var(--evco-spacing-1);
  padding: 3px 8px 3px 3px;
  border: 1px solid var(--evco-border);
  border-radius: 999px;
  background: transparent;
  cursor: pointer;
}

.shell__avatar {
  background: var(--evco-primary);
  color: #fff;
  font-weight: 600;
}

.shell__userName {
  color: var(--evco-text-primary);
  font: var(--evco-font-body);
  white-space: nowrap;
}

.shell__body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.shell__sidebar {
  box-sizing: border-box;
  display: flex;
  width: var(--evco-sidebar-expanded-width);
  flex-shrink: 0;
  flex-direction: column;
  overflow: hidden;
  border-right: 1px solid var(--evco-border);
  background: var(--evco-surface);
}

/* 菜单区独立滚动 */
.sidebar__nav {
  flex: 1;
  overflow-y: auto;
  padding: var(--evco-spacing-1) 0 var(--evco-spacing-2);
  scrollbar-width: thin;
}

/* 一级菜单：14px/600，主文字色；展开态主色高亮（字体层级体系 DEC-20260820-020） */
.sidebar__top {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 10px;
  padding: 10px var(--evco-spacing-2);
  border: 0;
  border-left: 3px solid transparent;
  background: transparent;
  color: var(--evco-text-primary);
  font: var(--evco-font-body);
  cursor: pointer;
  text-align: left;

  &:hover {
    background: var(--evco-background);
  }

  &.is-expanded {
    border-left-color: var(--evco-primary);
    background: var(--evco-background);
    color: var(--evco-primary);
    font-weight: 600;

    .sidebar__topIcon {
      color: var(--evco-primary);
    }
  }
}

.sidebar__topIcon {
  color: var(--evco-text-secondary);
}

.sidebar__topTitle {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 折叠箭头：内联 SVG chevron（15px），随展开态旋转 180° */
.sidebar__arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--evco-text-secondary);
  transition: transform 0.2s ease;
}

.sidebar__top.is-expanded .sidebar__arrow,
.sidebar__sub:not(.is-collapsed) .sidebar__arrow {
  transform: rotate(180deg);
}

.sidebar__group {
  padding-bottom: 4px;
}

/* 二级分组头：12px/600 次级色（小节标题，与三级叶子 12px/400 形成字重层级） */
.sidebar__sub {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 7px;
  padding: 7px var(--evco-spacing-2) 7px calc(var(--evco-spacing-2) + 12px);
  border: 0;
  background: transparent;
  color: var(--evco-text-secondary);
  font: var(--evco-font-assistive);
  font-weight: 600;
  cursor: pointer;
  text-align: left;

  &:hover {
    color: var(--evco-text-primary);
  }
}

.sidebar__subTitle {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar__subGroup {
  display: flex;
  flex-direction: column;
}

/* 菜单叶子：二级 14px 主文字色 / 三级 12px 次级色（层级字体区分） */
.sidebar__leaf {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px var(--evco-spacing-2);
  border-left: 3px solid transparent;
  color: var(--evco-text-primary);
  font: var(--evco-font-body);
  text-decoration: none;

  &:hover {
    background: var(--evco-background);
  }

  &.router-link-active {
    border-left-color: var(--evco-primary);
    background: var(--evco-background);
    color: var(--evco-primary);
    font-weight: 600;
  }
}

.sidebar__leafTitle {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar__leaf--d2 {
  padding-left: calc(var(--evco-spacing-2) + 12px);
}

/* 三级叶子：assistive 12px/400 次级色，active 转主色 */
.sidebar__leaf--d3 {
  gap: 6px;
  padding-left: calc(var(--evco-spacing-2) + 28px);
  color: var(--evco-text-secondary);
  font: var(--evco-font-assistive);
}

/* 主区：标签栏 + 内容（各自独立滚动） */
.shell__main {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
}

.shell__content {
  box-sizing: border-box;
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: var(--evco-spacing-3);
}

.shell__overlay {
  position: fixed;
  inset: 0;
  background: rgb(0 0 0 / 40%);
  z-index: 10;
}

/* max-width 传统语法（兼容 Safari 15~16.3，禁 range syntax——评审 P1-1 修复） */
@media (max-width: 991.98px) {
  .shell__burger {
    display: block;
  }

  .shell__clock {
    display: none;
  }

  .shell__sidebar {
    position: fixed;
    top: var(--evco-header-height);
    bottom: 0;
    left: 0;
    z-index: 20;
    transform: translateX(-100%);
    transition: transform 0.2s ease;
  }

  .shell__sidebar.is-open {
    transform: translateX(0);
  }
}

/* max-width 传统语法（兼容 Safari 15~16.3，禁 range syntax——评审 P1-1 修复） */
@media (max-width: 767.98px) {
  .shell__userName {
    display: none;
  }
}
</style>
