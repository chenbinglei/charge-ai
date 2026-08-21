<script setup lang="ts">
/**
 * 平台 LOGO 标识：圆角方块 + 闪电（能源）+ 枪线（充电）。
 * 颜色跟随当前主题主色（--evco-primary），任意尺寸不失真。
 */
withDefaults(defineProps<{ size?: number }>(), { size: 28 });
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 48 48"
    role="img"
    aria-label="充电运营平台 LOGO"
  >
    <defs>
      <linearGradient id="logo-grad" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="var(--evco-primary)" />
        <stop offset="1" stop-color="var(--evco-link)" />
      </linearGradient>
    </defs>
    <rect x="2" y="2" width="44" height="44" rx="12" fill="url(#logo-grad)" />
    <!-- 闪电主体 -->
    <path
      d="M27 8 L15 27 h8 l-3 13 L34 21 h-8 z"
      fill="#ffffff"
      opacity="0.96"
    />
    <!-- 充电枪线点缀 -->
    <path
      d="M11 36 q5 4 10 1"
      fill="none"
      stroke="#ffffff"
      stroke-width="2.4"
      stroke-linecap="round"
      opacity="0.65"
    />
  </svg>
</template>
