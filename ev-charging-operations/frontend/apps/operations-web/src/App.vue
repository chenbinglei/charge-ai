<script setup lang="ts">
import { ElButton } from "element-plus";
import { ref } from "vue";
import { themeNames } from "@evco/design-tokens";
import type { ThemeName } from "@evco/types";

const activeTheme = ref<ThemeName>("energy-green");

function selectTheme(themeName: ThemeName): void {
  activeTheme.value = themeName;
  document.documentElement.dataset.theme = themeName;
}
</script>

<template>
  <main class="foundation-shell" :data-theme="activeTheme">
    <section class="foundation-card" aria-labelledby="foundation-title">
      <p class="foundation-card__eyebrow">运营 Web · W1 工程样例</p>
      <h1 id="foundation-title" class="foundation-card__title">
        主题与布局基础已就绪
      </h1>
      <p class="foundation-card__description">
        这是无业务数据、无业务路由的设计 Token
        验证壳；后续页面必须复用同一栅格、间距和状态语义。
      </p>
      <div class="foundation-card__actions" aria-label="切换设计主题">
        <ElButton
          v-for="themeName in themeNames"
          :key="themeName"
          :type="activeTheme === themeName ? 'primary' : 'default'"
          @click="selectTheme(themeName)"
        >
          {{ themeName }}
        </ElButton>
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
  width: 100%;
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

@media (width < 768px) {
  .foundation-shell {
    padding: var(--evco-spacing-2);
  }

  .foundation-card {
    padding: var(--evco-spacing-3);
  }
}
</style>
