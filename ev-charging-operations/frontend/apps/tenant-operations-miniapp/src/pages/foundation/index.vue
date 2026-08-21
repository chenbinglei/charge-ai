<template>
  <view class="foundation-shell">
    <view class="foundation-card">
      <text class="foundation-card__eyebrow">租户运营小程序 · W1 工程样例</text>
      <text class="foundation-card__title">主题与跨端基础已就绪</text>
      <text class="foundation-card__description"
        >此壳不含租户管理、人员、工单或运营业务功能。</text
      >
    </view>
  </view>
</template>

<style scoped lang="scss">
.foundation-shell {
  box-sizing: border-box;
  min-height: 100vh;
  padding: var(--evco-spacing-3);
  background: var(--evco-background);
}

.foundation-card {
  padding: var(--evco-spacing-3);
  border: 1px solid var(--evco-border);
  border-radius: 8px;
  background: var(--evco-surface);
  box-shadow: var(--evco-shadow-card);
}

.foundation-card__eyebrow,
.foundation-card__title,
.foundation-card__description {
  display: block;
}

.foundation-card__eyebrow {
  color: var(--evco-primary);
  font: var(--evco-font-assistive);
}

.foundation-card__title {
  margin-top: var(--evco-spacing-1);
  font: var(--evco-font-heading-2);
}

.foundation-card__description {
  margin-top: var(--evco-spacing-2);
  color: var(--evco-text-secondary);
  font: var(--evco-font-body);
}
</style>
