<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();

/** 骨架占位（DEC-20260820-015）：未交付功能页显示「本功能计划 Wx 交付」。 */
const phase = computed(() => {
  const meta = route.meta.phase;
  return typeof meta === "string" ? meta : "—";
});

const title = computed(() => {
  const matched = route.matched.find((record) => record.path === route.path);
  return matched?.name?.toString() ?? route.path;
});
</script>

<template>
  <section class="placeholder" aria-labelledby="placeholder-title">
    <p class="placeholder__eyebrow">{{ title }}</p>
    <h1 id="placeholder-title" class="placeholder__title">本功能计划 {{ phase }} 交付</h1>
    <p class="placeholder__description">
      当前为菜单骨架占位页（DEC-20260820-015 全量权限初始化）；功能将在对应交付周按页面清单实现。
    </p>
  </section>
</template>

<style scoped lang="scss">
.placeholder {
  box-sizing: border-box;
  display: grid;
  min-height: calc(100vh - var(--evco-header-height) - var(--evco-spacing-6));
  padding: var(--evco-spacing-4);
  place-items: center;
  border: 1px dashed var(--evco-border);
  border-radius: 8px;
  background: var(--evco-surface);
  text-align: center;
}

.placeholder__eyebrow {
  margin: 0 0 var(--evco-spacing-1);
  color: var(--evco-text-secondary);
  font: var(--evco-font-assistive);
}

.placeholder__title {
  margin: 0 0 var(--evco-spacing-2);
  font: var(--evco-font-heading-2);
  color: var(--evco-text-primary);
}

.placeholder__description {
  margin: 0;
  color: var(--evco-text-secondary);
  font: var(--evco-font-body);
  max-width: 560px;
}
</style>
