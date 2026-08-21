<script setup lang="ts">
/**
 * 菜单语义图标渲染组件（DEC-20260820-020）。
 * 注意：@element-plus/icons-vue 的 SVG 仅带 viewBox（0 0 1024 1024）、无 width/height，
 * 必须以 px 显式指定宽高，否则按替换元素默认尺寸渲染为巨大图标。
 * 内联 style 优先级高于 SVG 属性，可稳定覆盖；颜色继承 currentColor 随文字态。
 */
import type { Component } from "vue";

withDefaults(defineProps<{ icon?: Component | null; size?: number }>(), {
  icon: null,
  size: 14,
});
</script>

<template>
  <component
    :is="icon"
    v-if="icon !== null"
    class="menu-icon"
    :style="{ width: `${size}px`, height: `${size}px` }"
    aria-hidden="true"
  />
</template>

<style scoped>
.menu-icon {
  flex-shrink: 0;
}
</style>
