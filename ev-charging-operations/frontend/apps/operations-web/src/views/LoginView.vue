<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElButton, ElForm, ElFormItem, ElInput, ElMessage } from "element-plus";
import { useAuthStore } from "@/stores/auth";
import { useTheme } from "@/composables/useTheme";
import LogoMark from "@/components/LogoMark.vue";
import LoginBrandArt from "@/components/LoginBrandArt.vue";
import type { ThemeName } from "@evco/types";

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();
const { current, setTheme } = useTheme();

/** 三档主题切换（DEC-20260820-018）：登录页即可预览底图随主题变化。 */
const THEME_OPTIONS: ReadonlyArray<{ value: ThemeName; label: string; swatch: string }> = [
  { value: "energy-green", label: "亮色主题", swatch: "#00b578" },
  { value: "graphite-night", label: "暗色主题", swatch: "#171b23" },
  { value: "command-deep", label: "科技蓝主题", swatch: "#0d1b2a" },
];

const submitting = ref(false);
const formRef = ref();
const form = reactive({
  username: "",
  password: "",
  captchaCode: "",
});

const rules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, max: 64, message: "长度 3-64 个字符", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 8, max: 64, message: "长度 8-64 个字符", trigger: "blur" },
  ],
  captchaCode: [
    { required: true, message: "请输入验证码", trigger: "blur" },
    { min: 4, max: 6, message: "4-6 位字母数字", trigger: "blur" },
  ],
};

onMounted(() => {
  void auth.loadCaptcha();
});

/** 提交登录；成功跳首个可见菜单或 redirect 参数。 */
async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  submitting.value = true;
  try {
    const result = await auth.login(form.username, form.password, form.captchaCode);
    if (!result.ok) {
      ElMessage.error(result.message);
      return;
    }
    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "";
    await router.replace(redirect || auth.firstVisiblePath || "/403");
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <main class="login" :data-theme="current">
    <!-- 矢量底图：地平线以上区域（场景基座锚定底边，三主题差异化）。
         必须用独立容器约束 90% 高度：svg 自身 .brand-art 的 height:100% 与
         绝对定位 top+bottom 叠加会使 bottom 失效，底图溢出到地面带之下，
         曾导致车辆下半身被地面带遮挡（2026-08-21 修复） -->
    <div class="login__art">
      <LoginBrandArt />
    </div>

    <!-- 地面带：CSS 渐变与底图末档色无缝衔接，顶边框即地平线（三主题恒定可见） -->
    <div class="login__ground">
      <!-- 左：一行式产品定位（地平线下方 10px，对标星星充电副标题模式） -->
      <p class="login__tagline">场站资产 · 智能运维 · 交易结算 · V2G 车网互动一体化运营平台</p>
      <!-- 右：服务支持链接（与定位语同水平线，平衡底部构图） -->
      <nav class="login__support" aria-label="服务支持">
        <a class="login__supportLink" href="#">帮助中心</a>
        <span aria-hidden="true">|</span>
        <a class="login__supportLink" href="#">服务支持</a>
      </nav>
    </div>

    <!-- 左上角品牌标识（增大图标与标题，提升品牌识别度） -->
    <div class="login__brand">
      <LogoMark :size="46" />
      <span class="login__brandName">充电运营管理平台</span>
    </div>

    <!-- 右上角主题切换 -->
    <div class="login__themeSwitch">
      <button
        v-for="option in THEME_OPTIONS"
        :key="option.value"
        type="button"
        class="login__themeDot"
        :class="{ 'is-active': current === option.value }"
        :style="{ background: option.swatch }"
        :title="option.label"
        :aria-label="option.label"
        :aria-pressed="current === option.value"
        @click="setTheme(option.value)"
      />
    </div>

    <!-- 右侧悬浮登录卡片 -->
    <section class="login__card" aria-labelledby="login-title">
      <h1 id="login-title" class="login__title">欢迎使用</h1>
      <p class="login__subtitle">充电运营管理平台</p>
      <ElForm
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @submit.prevent="submit"
      >
        <ElFormItem label="用户名" prop="username">
          <ElInput
            v-model="form.username"
            name="username"
            autocomplete="username"
            placeholder="请输入用户名"
          />
        </ElFormItem>
        <ElFormItem label="密码" prop="password">
          <ElInput
            v-model="form.password"
            name="password"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
            show-password
          />
        </ElFormItem>
        <ElFormItem label="验证码" prop="captchaCode">
          <div class="login__captcha">
            <ElInput
              v-model="form.captchaCode"
              name="captchaCode"
              placeholder="4-6 位字母数字"
              @keyup.enter="submit"
            />
            <button
              v-if="auth.captcha"
              type="button"
              class="login__captchaBtn"
              title="点击刷新验证码"
              aria-label="点击刷新验证码"
              @click="auth.loadCaptcha()"
            >
              <img
                class="login__captchaImage"
                :src="`data:image/png;base64,${auth.captcha.imageBase64}`"
                alt="图形验证码"
              />
            </button>
          </div>
        </ElFormItem>
        <ElButton
          type="primary"
          class="login__submit"
          :loading="submitting"
          native-type="submit"
        >
          登 录
        </ElButton>
      </ElForm>
    </section>

    <!-- 底部导航栏：版权 + 备案 + 协议链接（B 端合规基线） -->
    <footer class="login__footer">
      <span>© 2026 充电运营管理平台</span>
      <span class="login__footerDivider" aria-hidden="true">·</span>
      <!-- TODO(上线前)：替换为真实 ICP 备案号并接入协议页路由 -->
      <a class="login__footerLink" href="#">ICP备XXXXXXXX号</a>
      <span class="login__footerDivider" aria-hidden="true">·</span>
      <a class="login__footerLink" href="#">用户协议</a>
      <span class="login__footerDivider" aria-hidden="true">·</span>
      <a class="login__footerLink" href="#">隐私政策</a>
    </footer>
  </main>
</template>

<style scoped lang="scss">
.login {
  position: relative;
  height: 100vh;
  overflow: hidden;
  background: var(--evco-background);
  color: var(--evco-text-primary);
}

/* 底图容器：地平线以上区域（90%），底边即场景基座，与地面带精确衔接 */
.login__art {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 10%;
}

/* 地面带：CSS 渐变延续底图末档色；地平线用低强度同系色弱化（原高对比线
   在暗色主题过亮显突兀——2026-08-21 调整为约 25% 存在感） */
.login__ground {
  --ground-from: #bfe9d6;
  --ground-to: #b3ddc8;
  --ground-line: #a9d0ba;
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 10%;
  border-top: 1px solid var(--ground-line);
  background: linear-gradient(180deg, var(--ground-from), var(--ground-to));
}

.login[data-theme="graphite-night"] .login__ground {
  --ground-from: #111c26;
  --ground-to: #0d151d;
  --ground-line: #1c2f3f;
}

.login[data-theme="command-deep"] .login__ground {
  --ground-from: #0c2030;
  --ground-to: #081624;
  --ground-line: #14324a;
}

/* 亮色主题地面带文字（评审 P2-1 分档）：定位语/服务链接位于带顶部（背景 #bfe9d6），
   #4d6757 约 4.67:1 满足 AA；页脚位于带渐变深处（背景约 #b4dfca~#b3ddc8，更深），
   须回调 #44604f 约 5.0:1 才达 AA（全局 text-secondary #667085 在浅绿底不达标） */
.login[data-theme="energy-green"] :is(
    .login__tagline,
    .login__support,
    .login__supportLink
  ) {
  color: #4d6757;
}

.login[data-theme="energy-green"] :is(.login__footer, .login__footerLink) {
  color: #44604f;
}

/* 暗色主题地面带文字：与背景混色降对比（原 text-secondary 约 9:1 过亮），
   #7f8a97 于 #0d151d 上约 5.2:1，满足 WCAG AA */
.login[data-theme="graphite-night"] :is(
    .login__tagline,
    .login__support,
    .login__supportLink,
    .login__footer,
    .login__footerLink
  ) {
  color: #7f8a97;
}

/* 科技蓝主题地面带文字：#6f8396 于 #081624 上约 4.7:1，满足 WCAG AA */
.login[data-theme="command-deep"] :is(
    .login__tagline,
    .login__support,
    .login__supportLink,
    .login__footer,
    .login__footerLink
  ) {
  color: #6f8396;
}

/* 一行式产品定位：地平线下方 12px，与左上品牌区同用 spacing-4 左边距对齐 */
.login__tagline {
  position: absolute;
  top: 12px;
  left: var(--evco-spacing-4);
  margin: 0;
  color: var(--evco-text-secondary);
  font: var(--evco-font-body);
  letter-spacing: 1px;
}

/* 服务支持链接：右下与定位语光学同水平线（评审 P3-3：12px 字行中心≈23px，
   与 tagline 14px/22px top:12px 的中心 23px 对齐），平衡底部构图 */
.login__support {
  position: absolute;
  top: 14px;
  right: var(--evco-spacing-4);
  display: flex;
  align-items: center;
  gap: var(--evco-spacing-2);
  color: var(--evco-text-secondary);
  font: var(--evco-font-assistive);
}

.login__supportLink {
  color: var(--evco-text-secondary);
  text-decoration: none;

  &:hover {
    color: var(--evco-primary);
  }
}

/* 左上角品牌标识：46px 图标 + 20px 标题；边距与底部定位语统一 spacing-4 */
.login__brand {
  position: absolute;
  top: var(--evco-spacing-4);
  left: var(--evco-spacing-4);
  z-index: 1;
  display: flex;
  align-items: center;
  gap: var(--evco-spacing-2);
}

.login__brandName {
  font: var(--evco-font-heading-1);
  color: var(--evco-text-primary);
  letter-spacing: 1px;
}

/* 右上角主题切换 */
.login__themeSwitch {
  position: absolute;
  top: var(--evco-spacing-4);
  right: var(--evco-spacing-4);
  z-index: 1;
  display: flex;
  gap: var(--evco-spacing-1);
  padding: 6px;
  border-radius: 999px;
  background: var(--evco-surface);
  box-shadow: var(--evco-shadow-card);
}

.login__themeDot {
  width: 22px;
  height: 22px;
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

/* 右侧悬浮登录卡片 */
.login__card {
  position: absolute;
  top: 50%;
  right: clamp(var(--evco-spacing-3), 8vw, 140px);
  z-index: 1;
  box-sizing: border-box;
  width: min(400px, calc(100vw - 48px));
  padding: var(--evco-spacing-4) var(--evco-spacing-3);
  border: 1px solid var(--evco-border);
  border-radius: 8px;
  background: var(--evco-surface);
  box-shadow: var(--evco-shadow-card);
  transform: translateY(-50%);
}

/* 暗色主题：卡片加边框亮度与投影深度，输入框边框提亮（评审 P1：深背景上主体性不足） */
.login[data-theme="graphite-night"] .login__card,
.login[data-theme="command-deep"] .login__card {
  border-color: color-mix(in srgb, var(--evco-border) 55%, #ffffff 45%);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.45);

  &:deep(.el-input__wrapper) {
    box-shadow: 0 0 0 1px #5a6b80 inset;
  }
}

.login__title {
  margin: 0 0 var(--evco-spacing-1);
  font: var(--evco-font-heading-1);
}

.login__subtitle {
  margin: 0 0 var(--evco-spacing-4);
  color: var(--evco-text-secondary);
  font: var(--evco-font-assistive);
}

.login__captcha {
  display: flex;
  width: 100%;
  gap: var(--evco-spacing-2);
  align-items: center;
}

/* 验证码刷新按钮（img 的键盘可达容器）：重置按钮默认外观，仅透传点击 */
.login__captchaBtn {
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
}

.login__captchaImage {
  display: block;
  height: 40px;
  border: 1px solid var(--evco-border);
  border-radius: 4px;
}

.login__submit {
  width: 100%;
  margin-top: var(--evco-spacing-1);
}

/* 底部导航栏：版权＋备案＋协议，居中一行、可换行（B 端合规基线）；
   间距放宽至 spacing-2、加字距提升呼吸感（2026-08-21 排版优化） */
.login__footer {
  position: absolute;
  bottom: var(--evco-spacing-3);
  left: 0;
  right: 0;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: var(--evco-spacing-2);
  margin: 0;
  color: var(--evco-text-secondary);
  font: var(--evco-font-assistive);
  letter-spacing: 0.5px;
}

.login__footerLink {
  color: var(--evco-text-secondary);
  text-decoration: none;

  &:hover {
    color: var(--evco-primary);
  }
}

/* <992：登录卡片改为居中，隐藏定位语与服务链接（max-width 传统语法，兼容 Safari 15） */
@media (max-width: 991.98px) {
  .login__tagline,
  .login__support {
    display: none;
  }

  .login__card {
    right: 50%;
    transform: translate(50%, -50%);
  }
}

/* <768：移动端——品牌区缩小，卡片贴边占满（max-width 传统语法，兼容 Safari 15） */
@media (max-width: 767.98px) {
  .login__brand {
    gap: var(--evco-spacing-1);
  }

  .login__brandName {
    font: var(--evco-font-heading-2);
    letter-spacing: 0.5px;
  }

  .login__card {
    width: calc(100vw - 32px);
  }
}
</style>
