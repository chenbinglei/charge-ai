<script setup lang="ts">
import { useRouter } from "vue-router";
import { ElButton } from "element-plus";

const router = useRouter();
</script>

<template>
  <main class="status-shell">
    <section class="status-card" aria-labelledby="not-found-title">
      <p class="status-card__code">404</p>
      <h1 id="not-found-title" class="status-card__title">页面不存在</h1>
      <p class="status-card__description">访问的地址未在平台注册，请从左侧菜单进入功能页面。</p>
      <ElButton type="primary" @click="router.replace('/')">返回首页</ElButton>
    </section>
  </main>
</template>

<style scoped lang="scss">
.status-shell {
  box-sizing: border-box;
  display: grid;
  min-height: 100vh;
  padding: var(--evco-spacing-4);
  place-items: center;
  background: var(--evco-background);
  color: var(--evco-text-primary);
}

.status-card {
  width: 100%;
  max-width: 480px;
  padding: var(--evco-spacing-4);
  border: 1px solid var(--evco-border);
  border-radius: 8px;
  background: var(--evco-surface);
  box-shadow: var(--evco-shadow-card);
  text-align: center;
}

.status-card__code {
  margin: 0;
  font: var(--evco-font-heading-1);
  color: var(--evco-primary);
}

.status-card__title {
  margin: 0 0 var(--evco-spacing-2);
  font: var(--evco-font-heading-2);
}

.status-card__description {
  margin: 0 0 var(--evco-spacing-3);
  color: var(--evco-text-secondary);
  font: var(--evco-font-body);
}
</style>
