<script setup lang="ts">
import { ref } from "vue";
import { themeNames } from "@evco/design-tokens";
import type { ThemeName } from "@evco/types";

const activeTheme = ref<ThemeName>("command-deep");

function selectTheme(themeName: ThemeName): void {
  activeTheme.value = themeName;
  document.documentElement.dataset.theme = themeName;
}
</script>

<template>
  <main class="foundation-shell" :data-theme="activeTheme">
    <section class="foundation-card" aria-labelledby="foundation-title">
      <p class="foundation-card__eyebrow">监控大屏 · W1 工程样例</p>
      <h1 id="foundation-title" class="foundation-card__title">
        深邃蓝主题基础已就绪
      </h1>
      <p class="foundation-card__description">
        ECharts 仅在大屏工程中依赖；本阶段不绘制模拟业务数据，也不创建监控页面。
      </p>
      <div class="foundation-card__actions" aria-label="切换设计主题">
        <button
          v-for="themeName in themeNames"
          :key="themeName"
          type="button"
          @click="selectTheme(themeName)"
        >
          {{ themeName }}
        </button>
      </div>
    </section>
  </main>
</template>

<style scoped lang="scss">
.foundation-shell {
  box-sizing: border-box;
  display: grid;
  min-height: 100vh;
  padding: var(--evco-spacing-4);
  place-items: center;
  background: var(--evco-background);
  color: var(--evco-text-primary);
}

.foundation-card {
  width: min(100%, var(--evco-content-max-width));
  max-width: 720px;
  padding: var(--evco-spacing-4);
  border: 1px solid var(--evco-border);
  border-radius: 8px;
  background: var(--evco-surface);
  box-shadow: var(--evco-shadow-card);
}

.foundation-card__eyebrow {
  margin: 0 0 var(--evco-spacing-1);
  color: var(--evco-primary);
  font: var(--evco-font-assistive);
}

.foundation-card__title {
  margin: 0;
  font: var(--evco-font-heading-1);
}

.foundation-card__description {
  margin: var(--evco-spacing-2) 0 var(--evco-spacing-3);
  color: var(--evco-text-secondary);
  font: var(--evco-font-body);
}

.foundation-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--evco-spacing-1);
}

.foundation-card__actions button {
  min-height: 40px;
  padding: 0 var(--evco-spacing-2);
  border: 1px solid var(--evco-border);
  border-radius: 6px;
  background: transparent;
  color: inherit;
}

@media (width < 768px) {
  .foundation-shell {
    padding: var(--evco-spacing-2);
  }

  .foundation-card {
    padding: var(--evco-spacing-3);
  }
}
</style>
