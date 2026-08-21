<script setup lang="ts">
import { computed } from "vue";
import { useTheme } from "@/composables/useTheme";

/**
 * 登录页矢量底图（DEC-20260820-019/022）：只负责地平线以上区域（viewBox 高 742、
 * YMax 锚定场景基座），地面由 LoginView 的 CSS 渐变带承接（地平线＝带顶边框）。
 *
 * 场景安全区（2026-08-21 二次修订）：①充电站主体（雨棚/充电桩/前车/后车）
 * 位于 viewBox x 480-1020（用户要求整体左移 50px），主视口 1920×1080 全景完整；
 * 768×1024 竖屏极端视口下（容器 90% 高，可见区 [490.8,1109.2]）雨棚左端约
 * 13px 渐变裁切（氛围元素，桩/车仍完整）、左右氛围楼整体出画属允许取舍。
 * ②场景组整体上移 20px——楼底/车轮与地平线（viewBox 底边）保留约 22-23px
 * 间距（用户要求「楼层、车辆和底部保持一点间距」）。③能量流双曲线整体上移
 * 55px，与雨棚顶（上移后 y=450）保留约 35px 间距（用户要求「线依次往上平移
 * 和车棚留点间隔」，原第二条线 y≈470 恰被雨棚顶盖住）。④楼栋窗户网格水平
 * 对称边距 ≥11（原左楼1 右列边距 1px 贴框）。三档主题差异化渲染：暗色/科技蓝
 * 下楼房、车辆采用提亮填充＋描边，与背景保持可辨对比。
 */
const { current } = useTheme();
const theme = computed(() => current.value);

/** 三主题场景配色：楼房/桩/车分色阶＋描边，保证暗色主题下轮廓可见。 */
const palette = computed(() => {
  if (theme.value === "energy-green") {
    return {
      buildingFill: "#a9cfba",
      buildingStroke: "#8fbda6",
      roof: "#8fbda6",
      windowFill: "#ffffff",
      windowOpacity: 0.65,
      pillar: "#9cc2ad",
      slot: "#c9ded4",
      pileStroke: "#b8d4c6",
      carStroke: "#a8c9b8",
      wheel: "#3c4a56",
      hub: "#8fa3b8",
    };
  }
  if (theme.value === "graphite-night") {
    return {
      buildingFill: "#1c2e42",
      buildingStroke: "#33506e",
      roof: "#2a4258",
      windowFill: "var(--evco-primary)",
      windowOpacity: 0.6,
      pillar: "#2a3d55",
      slot: "#2c3f56",
      pileStroke: "#45596f",
      carStroke: "#566d8c",
      wheel: "#0c141d",
      hub: "#3a4c62",
    };
  }
  return {
    buildingFill: "#16334e",
    buildingStroke: "#2c5a82",
    roof: "#235080",
    windowFill: "var(--evco-primary)",
    windowOpacity: 0.6,
    pillar: "#1f4666",
    slot: "#1e3a52",
    pileStroke: "#2c5a82",
    carStroke: "#3d6e94",
    wheel: "#081221",
    hub: "#2a4a66",
  };
});
</script>

<template>
  <svg
    class="brand-art"
    viewBox="-200 0 2000 742"
    preserveAspectRatio="xMidYMax slice"
    aria-hidden="true"
  >
    <defs>
      <!-- 三套主题背景渐变：止于地平线（y=742），末档色与 CSS 地面带起始色严格衔接 -->
      <linearGradient id="ba-bg-green" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#eaf8f1" />
        <stop offset="0.55" stop-color="#d8f1e6" />
        <stop offset="1" stop-color="#bfe9d6" />
      </linearGradient>
      <linearGradient id="ba-bg-night" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#101823" />
        <stop offset="0.55" stop-color="#152230" />
        <stop offset="1" stop-color="#111c26" />
      </linearGradient>
      <linearGradient id="ba-bg-deep" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#0a1a29" />
        <stop offset="0.55" stop-color="#0d2334" />
        <stop offset="1" stop-color="#0c2030" />
      </linearGradient>
      <!-- 雨棚 -->
      <linearGradient id="ba-canopy" x1="0" y1="0" x2="0" y2="1">
        <stop
          offset="0"
          :stop-color="
            theme === 'energy-green' ? '#ffffff' : theme === 'graphite-night' ? '#22334a' : '#1b3a55'
          "
        />
        <stop
          offset="1"
          :stop-color="
            theme === 'energy-green' ? '#eef6f1' : theme === 'graphite-night' ? '#16233a' : '#12283c'
          "
        />
      </linearGradient>
      <!-- 充电桩（与车身分色阶，避免车桩融为一体） -->
      <linearGradient id="ba-pile" x1="0" y1="0" x2="0" y2="1">
        <stop
          offset="0"
          :stop-color="
            theme === 'energy-green' ? '#ffffff' : theme === 'graphite-night' ? '#2b3d54' : '#24455f'
          "
        />
        <stop
          offset="1"
          :stop-color="
            theme === 'energy-green' ? '#ddeee6' : theme === 'graphite-night' ? '#1d2c3f' : '#183148'
          "
        />
      </linearGradient>
      <!-- 车身（独立色阶＋描边：暗色主题下比桩更亮一档，轮廓可辨） -->
      <linearGradient id="ba-car" x1="0" y1="0" x2="0" y2="1">
        <stop
          offset="0"
          :stop-color="
            theme === 'energy-green' ? '#ffffff' : theme === 'graphite-night' ? '#3d526e' : '#375a76'
          "
        />
        <stop
          offset="1"
          :stop-color="
            theme === 'energy-green' ? '#e3f2ea' : theme === 'graphite-night' ? '#2a3c52' : '#26445c'
          "
        />
      </linearGradient>
      <!-- 能量流 -->
      <linearGradient id="ba-flow" x1="0" y1="0" x2="1" y2="0">
        <stop offset="0" stop-color="var(--evco-primary)" stop-opacity="0" />
        <stop offset="0.5" stop-color="var(--evco-primary)" stop-opacity="0.9" />
        <stop offset="1" stop-color="var(--evco-link)" stop-opacity="0.2" />
      </linearGradient>
      <!-- 充电桩光晕 -->
      <radialGradient id="ba-glow">
        <stop offset="0" stop-color="var(--evco-primary)" stop-opacity="0.55" />
        <stop offset="1" stop-color="var(--evco-primary)" stop-opacity="0" />
      </radialGradient>
      <!-- 右侧渐隐遮罩：保证登录框区域可读 -->
      <linearGradient id="ba-fade" x1="0" y1="0" x2="1" y2="0">
        <stop offset="0" stop-color="var(--evco-surface)" stop-opacity="0" />
        <stop offset="0.72" stop-color="var(--evco-surface)" stop-opacity="0.35" />
        <stop offset="1" stop-color="var(--evco-surface)" stop-opacity="0.75" />
      </linearGradient>
    </defs>

    <!-- 背景（止于地平线） -->
    <rect
      x="-200"
      width="2000"
      height="742"
      :fill="
        theme === 'energy-green'
          ? 'url(#ba-bg-green)'
          : theme === 'graphite-night'
            ? 'url(#ba-bg-night)'
            : 'url(#ba-bg-deep)'
      "
    />

    <!-- command-deep：数据网格与节点连线（评审 P1 后减淡，避免纹理过重抢视觉） -->
    <g v-if="theme === 'command-deep'" opacity="0.45">
      <g stroke="#215073" stroke-width="1">
        <path v-for="x in 26" :key="`gx${x}`" :d="`M ${x * 80 - 200} 0 V 742`" />
        <!-- 横线止于 y=668，避开 viewBox 底边（地平线唯一来源＝CSS 顶边框） -->
        <path v-for="y in 9" :key="`gy${y}`" :d="`M -200 ${y * 74.2} H 1800`" />
      </g>
      <g fill="var(--evco-link)">
        <circle
          v-for="n in 18"
          :key="`nd${n}`"
          :cx="(n * 163) % 1560 + 20"
          :cy="(n * 217) % 700 + 20"
          r="3"
          opacity="0.7"
        />
      </g>
      <g stroke="var(--evco-link)" stroke-width="1.2" opacity="0.35">
        <path d="M 120 200 L 360 140 L 580 300" fill="none" />
        <path d="M 180 660 L 420 540 L 700 620" fill="none" />
      </g>
    </g>

    <!-- graphite-night：星点（月晕已按用户要求移除，2026-08-21） -->
    <g v-if="theme === 'graphite-night'">
      <circle
        v-for="s in 30"
        :key="`st${s}`"
        :cx="(s * 173) % 1560 + 20"
        :cy="(s * 97) % 500 + 20"
        r="1.6"
        fill="#cfe3ff"
        opacity="0.75"
      />
    </g>

    <!-- energy-green：日光晕 -->
    <g v-if="theme === 'energy-green'">
      <circle cx="1280" cy="160" r="170" fill="#ffffff" opacity="0.22" />
      <circle cx="1280" cy="160" r="100" fill="#ffffff" opacity="0.3" />
    </g>

    <!-- 能量流动曲线（整体上移 55px：与雨棚顶留 ~35px 间隔，不再被车棚盖住） -->
    <g fill="none" stroke-width="2.5">
      <path d="M -240 345 C 260 285, 480 405, 760 325 S 1180 265, 1860 325" stroke="url(#ba-flow)" />
      <path d="M -240 405 C 300 355, 540 465, 820 385 S 1240 325, 1860 385" stroke="url(#ba-flow)" opacity="0.6" />
    </g>

    <!-- 充电站场景：整体 translate(-50,-20)——左移 50px＋底部留 20px 间距（2026-08-21 用户要求），
         主体落位安全区 x 480-1020，楼底/车轮距地平线约 22-23px -->
    <g transform="translate(-50, -20)">
      <!-- 远景建筑（左）：氛围元素，窄视口下允许左缘渐变裁切 -->
      <g :fill="palette.buildingFill" :stroke="palette.buildingStroke" stroke-width="1.5">
        <rect x="210" y="520" width="100" height="220" rx="4" />
        <rect x="330" y="570" width="76" height="170" rx="4" />
      </g>
      <!-- 左楼顶檐口 -->
      <g :fill="palette.roof">
        <rect x="210" y="520" width="100" height="9" rx="3" />
        <rect x="330" y="570" width="76" height="8" rx="3" />
      </g>
      <!-- 左楼窗：4×4＋2×3 楼层网格（水平对称边距：楼1 11/11、楼2 17/17；
           垂直对称：楼1 顶37/底38、楼2 顶32/底33。v-for 从 1 起算须用 (w-1)
           分组——直接用 w 会使末行错位越出楼底框〔2026-08-21 修复楼外悬浮窗〕） -->
      <g :fill="palette.windowFill" :opacity="palette.windowOpacity">
        <rect
          v-for="w in 16"
          :key="`wl${w}`"
          :x="221 + ((w - 1) % 4) * 23"
          :y="557 + Math.floor((w - 1) / 4) * 44"
          width="9"
          height="13"
          rx="1.5"
        />
        <rect
          v-for="w in 6"
          :key="`wl2${w}`"
          :x="347 + ((w - 1) % 2) * 33"
          :y="602 + Math.floor((w - 1) / 2) * 46"
          width="9"
          height="13"
          rx="1.5"
        />
      </g>

      <!-- 远景建筑（右）：位于场景主体与登录卡片之间，宽视口完整可见 -->
      <g :fill="palette.buildingFill" :stroke="palette.buildingStroke" stroke-width="1.5">
        <rect x="1130" y="540" width="110" height="200" rx="4" />
        <rect x="1260" y="580" width="64" height="160" rx="4" />
      </g>
      <!-- 右楼顶檐口 -->
      <g :fill="palette.roof">
        <rect x="1130" y="540" width="110" height="9" rx="3" />
        <rect x="1260" y="580" width="64" height="8" rx="3" />
      </g>
      <!-- 右楼窗：4×4＋2×3 楼层网格（水平对称边距：楼1 12/11、楼2 14/15；
           垂直对称：楼1 顶21/底22、楼2 顶25/底26。(w-1) 分组同左楼注释） -->
      <g :fill="palette.windowFill" :opacity="palette.windowOpacity">
        <rect
          v-for="w in 16"
          :key="`wr${w}`"
          :x="1142 + ((w - 1) % 4) * 26"
          :y="561 + Math.floor((w - 1) / 4) * 48"
          width="9"
          height="13"
          rx="1.5"
        />
        <rect
          v-for="w in 6"
          :key="`wr2${w}`"
          :x="1274 + ((w - 1) % 2) * 26"
          :y="605 + Math.floor((w - 1) / 2) * 48"
          width="9"
          height="13"
          rx="1.5"
        />
      </g>

      <!-- 雨棚（支柱提亮一档保证暗色主题下可见） -->
      <g>
        <rect x="530" y="470" width="480" height="28" rx="14" fill="url(#ba-canopy)" />
        <rect
          x="530"
          y="498"
          width="480"
          height="9"
          :fill="theme === 'energy-green' ? '#cfe8dc' : '#0f1a26'"
          opacity="0.8"
        />
        <rect x="566" y="507" width="13" height="150" :fill="palette.pillar" />
        <rect x="941" y="507" width="13" height="150" :fill="palette.pillar" />
      </g>

      <!-- 充电桩 ×2（连线垂向车位，光晕标记能量点） -->
      <g>
        <rect x="620" y="560" width="56" height="108" rx="9" fill="url(#ba-pile)" :stroke="palette.pileStroke" stroke-width="1" />
        <rect x="629" y="573" width="38" height="26" rx="4" fill="var(--evco-primary)" opacity="0.85" />
        <rect x="629" y="609" width="38" height="7" rx="3" :fill="palette.slot" />
        <rect x="629" y="622" width="38" height="7" rx="3" :fill="palette.slot" />
        <path d="M 676 612 q 28 8 24 36" fill="none" stroke="var(--evco-link)" stroke-width="5" stroke-linecap="round" />
        <circle cx="648" cy="586" r="56" fill="url(#ba-glow)" />
      </g>
      <g>
        <rect x="864" y="560" width="56" height="108" rx="9" fill="url(#ba-pile)" :stroke="palette.pileStroke" stroke-width="1" />
        <rect x="873" y="573" width="38" height="26" rx="4" fill="var(--evco-primary)" opacity="0.85" />
        <rect x="873" y="609" width="38" height="7" rx="3" :fill="palette.slot" />
        <rect x="873" y="622" width="38" height="7" rx="3" :fill="palette.slot" />
        <path d="M 864 612 q -28 8 -24 36" fill="none" stroke="var(--evco-link)" stroke-width="5" stroke-linecap="round" />
        <circle cx="892" cy="586" r="56" fill="url(#ba-glow)" />
      </g>

      <!-- 电动车（前车：桩间车位充电中；描边＋独立色阶与桩区分） -->
      <g>
        <path
          d="M 710 674 q 6 -28 32 -32 l 50 0 q 24 4 32 26 l 9 7 q 9 2 9 11 l 0 15 q 0 6 -6 6 l -9 0 a 13 13 0 0 0 -26 0 l -56 0 a 13 13 0 0 0 -26 0 l -9 0 q -6 0 -6 -6 l 0 -9 q 0 -6 6 -9 z"
          fill="url(#ba-car)"
          :stroke="palette.carStroke"
          stroke-width="1.5"
        />
        <circle cx="740" cy="707" r="12" :fill="palette.wheel" />
        <circle cx="830" cy="707" r="12" :fill="palette.wheel" />
        <circle cx="740" cy="707" r="4.5" :fill="palette.hub" />
        <circle cx="830" cy="707" r="4.5" :fill="palette.hub" />
        <rect x="766" y="648" width="26" height="11" rx="3" fill="var(--evco-primary)" opacity="0.85" />
      </g>
      <!-- 电动车（后车：雨棚右端 0.7 缩放，车轮底线控制在 y≤742 不越地平线） -->
      <g transform="translate(570, 235) scale(0.7)" opacity="0.92">
        <path
          d="M 560 674 q 6 -28 32 -32 l 50 0 q 24 4 32 26 l 9 7 q 9 2 9 11 l 0 15 q 0 6 -6 6 l -9 0 a 13 13 0 0 0 -26 0 l -56 0 a 13 13 0 0 0 -26 0 l -9 0 q -6 0 -6 -6 l 0 -9 q 0 -6 6 -9 z"
          fill="url(#ba-car)"
          :stroke="palette.carStroke"
          stroke-width="1.5"
        />
        <circle cx="590" cy="707" r="12" :fill="palette.wheel" />
        <circle cx="680" cy="707" r="12" :fill="palette.wheel" />
        <circle cx="590" cy="707" r="4.5" :fill="palette.hub" />
        <circle cx="680" cy="707" r="4.5" :fill="palette.hub" />
        <rect x="616" y="648" width="26" height="11" rx="3" fill="var(--evco-link)" opacity="0.6" />
      </g>

      <!-- 贴地投影：物体落地感，消除场景与地面带的衔接断层 -->
      <g
        :fill="theme === 'energy-green' ? '#2e503e' : '#000000'"
        :opacity="theme === 'energy-green' ? 0.16 : 0.35"
      >
        <ellipse cx="648" cy="735" rx="40" ry="6" />
        <ellipse cx="892" cy="735" rx="40" ry="6" />
        <ellipse cx="785" cy="737" rx="78" ry="8" />
        <ellipse cx="1006" cy="737" rx="62" ry="7" />
      </g>
      <!-- 地面与地平线由 LoginView 的 CSS 地面带承接（渐变衔接＋顶边框线），场景基座即 viewBox 底边 -->
    </g>

    <!-- 右侧渐隐遮罩：登录框悬浮区可读性 -->
    <rect x="1040" y="0" width="640" height="742" fill="url(#ba-fade)" />
  </svg>
</template>

<style scoped>
.brand-art {
  display: block;
  width: 100%;
  height: 100%;
}
</style>
