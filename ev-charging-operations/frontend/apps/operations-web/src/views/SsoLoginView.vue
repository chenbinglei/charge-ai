<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElButton } from "element-plus";
import { useAuthStore } from "@/stores/auth";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const failed = ref(false);
const failMessage = ref("");

/** 自动换会话：仅 IOT 协同模式；独立部署模式后端直接 403 关闭。 */
onMounted(async () => {
  const ticket = typeof route.query.ticket === "string" ? route.query.ticket : "";
  if (ticket === "") {
    failed.value = true;
    failMessage.value = "缺少 ticket 参数，请从 IOT 平台重新进入";
    return;
  }
  const result = await auth.ssoLogin(ticket);
  if (!result.ok) {
    failed.value = true;
    failMessage.value = result.message;
    return;
  }
  await router.replace(auth.firstVisiblePath ?? "/403");
});
</script>

<template>
  <main class="sso-shell">
    <section class="sso-card" aria-labelledby="sso-title">
      <template v-if="!failed">
        <p class="sso-card__eyebrow">SSO 免登录</p>
        <h1 id="sso-title" class="sso-card__title">正在换取会话…</h1>
        <p class="sso-card__description">请稍候，正在校验 IOT 平台跳转凭证。</p>
      </template>
      <template v-else>
        <p class="sso-card__eyebrow">SSO 免登录</p>
        <h1 id="sso-title" class="sso-card__title">凭证无效</h1>
        <p class="sso-card__description">{{ failMessage }}</p>
        <p class="sso-card__description">请返回 IOT 平台重新进入；如持续失败请联系管理员。</p>
        <div class="sso-card__actions">
          <ElButton type="primary" @click="router.replace('/login')">
            前往账号登录
          </ElButton>
        </div>
      </template>
    </section>
  </main>
</template>

<style scoped lang="scss">
.sso-shell {
  box-sizing: border-box;
  display: grid;
  min-height: 100vh;
  padding: var(--evco-spacing-4);
  place-items: center;
  background: var(--evco-background);
  color: var(--evco-text-primary);
}

.sso-card {
  width: 100%;
  max-width: 480px;
  padding: var(--evco-spacing-4);
  border: 1px solid var(--evco-border);
  border-radius: 8px;
  background: var(--evco-surface);
  box-shadow: var(--evco-shadow-card);
}

.sso-card__eyebrow {
  margin: 0 0 var(--evco-spacing-1);
  color: var(--evco-primary);
  font: var(--evco-font-assistive);
}

.sso-card__title {
  margin: 0 0 var(--evco-spacing-2);
  font: var(--evco-font-heading-2);
}

.sso-card__description {
  margin: 0 0 var(--evco-spacing-2);
  color: var(--evco-text-secondary);
  font: var(--evco-font-body);
}

.sso-card__actions {
  display: flex;
  gap: var(--evco-spacing-1);
}
</style>
