import { defineStore } from "pinia";
import { computed, ref } from "vue";
import type { CaptchaVO, ProfileVO } from "@evco/types";
import { apiClient, authApi } from "@/api";

/** 令牌持久化键；仅本应用命名空间，登出即清除。 */
const ACCESS_TOKEN_KEY = "evco.at";
const REFRESH_TOKEN_KEY = "evco.rt";

/** 认证会话状态：令牌、档案与可见菜单；含验证码/登录/登出/静默续期。 */
export const useAuthStore = defineStore("auth", () => {
  const accessToken = ref<string | null>(localStorage.getItem(ACCESS_TOKEN_KEY));
  const refreshToken = ref<string | null>(localStorage.getItem(REFRESH_TOKEN_KEY));
  const profile = ref<ProfileVO | null>(null);
  const captcha = ref<CaptchaVO | null>(null);

  /** 可见叶子路径集合（后端裁剪后的 menus；路由守卫 403 判定依据）。 */
  const visiblePaths = computed(() => {
    const sink = new Set<string>();
    collect(profile.value?.menus ?? [], sink);
    return sink;
  });

  /** 登录后落点：首个可见叶子菜单。 */
  const firstVisiblePath = computed(() => visiblePaths.value.values().next().value ?? null);

  const isAuthenticated = computed(() => accessToken.value !== null);

  function collect(nodes: ProfileVO["menus"], sink: Set<string>): void {
    for (const node of nodes) {
      if (node.permission !== null) {
        sink.add(node.path);
      }
      if (node.children !== null && node.children.length > 0) {
        collect(node.children, sink);
      }
    }
  }

  /** 同步客户端令牌并持久化（登录/刷新/登出共用）。 */
  function persistTokens(access: string | null, refresh: string | null): void {
    accessToken.value = access;
    refreshToken.value = refresh;
    apiClient.setAccessToken(access);
    if (access === null || refresh === null) {
      localStorage.removeItem(ACCESS_TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
    } else {
      localStorage.setItem(ACCESS_TOKEN_KEY, access);
      localStorage.setItem(REFRESH_TOKEN_KEY, refresh);
    }
  }

  /** 拉取图形验证码；点击图片刷新时重复调用。 */
  async function loadCaptcha(): Promise<void> {
    const resp = await authApi.captcha();
    if (resp.code === "SUCCESS" && resp.data !== null) {
      captcha.value = resp.data;
    }
  }

  /** 账号密码登录：成功后拉档案并落令牌。 */
  async function login(
    username: string,
    password: string,
    captchaCode: string,
  ): Promise<{ ok: boolean; message: string }> {
    if (captcha.value === null) {
      return { ok: false, message: "验证码未加载，请刷新后重试" };
    }
    const resp = await authApi.login({
      username,
      password,
      captchaId: captcha.value.captchaId,
      captchaCode,
    });
    if (resp.code !== "SUCCESS" || resp.data === null) {
      // 验证码一次性消费：失败即换新图。
      await loadCaptcha();
      return { ok: false, message: resp.message || "登录失败" };
    }
    persistTokens(resp.data.accessToken, resp.data.refreshToken);
    await fetchProfile();
    return { ok: true, message: "登录成功" };
  }

  /** IOT 协同模式 SSO 免登录：一次性 ticket 换会话。 */
  async function ssoLogin(ticket: string): Promise<{ ok: boolean; message: string }> {
    const resp = await authApi.ssoLogin(ticket);
    if (resp.code !== "SUCCESS" || resp.data === null) {
      return { ok: false, message: resp.message || "ticket 无效或已过期" };
    }
    persistTokens(resp.data.accessToken, resp.data.refreshToken);
    await fetchProfile();
    return { ok: true, message: "登录成功" };
  }

  /** 拉取当前用户档案（权限码/部署模式/可见菜单树）。 */
  async function fetchProfile(): Promise<boolean> {
    const resp = await authApi.profile();
    if (resp.code === "SUCCESS" && resp.data !== null) {
      profile.value = resp.data;
      return true;
    }
    return false;
  }

  /** 刷新令牌轮换；失败视为会话过期。 */
  async function refreshSession(): Promise<boolean> {
    if (refreshToken.value === null) {
      return false;
    }
    const resp = await authApi.refresh(refreshToken.value);
    if (resp.code === "SUCCESS" && resp.data !== null) {
      persistTokens(resp.data.accessToken, resp.data.refreshToken);
      return true;
    }
    return false;
  }

  /** 路由守卫触发：本地令牌恢复会话（先 profile，401 则静默刷新重试一次）。 */
  async function tryRestoreSession(): Promise<boolean> {
    if (accessToken.value === null) {
      return false;
    }
    apiClient.setAccessToken(accessToken.value);
    if (await fetchProfile()) {
      return true;
    }
    if (await refreshSession()) {
      return await fetchProfile();
    }
    clearSession();
    return false;
  }

  /** 401 统一回调：静默刷新失败即清除会话回登录页。 */
  async function handleUnauthorized(): Promise<void> {
    if (await refreshSession()) {
      return;
    }
    clearSession();
  }

  /** 登出：撤销会话并清除本地令牌。 */
  async function logout(): Promise<void> {
    if (refreshToken.value !== null) {
      await authApi.logout(refreshToken.value);
    }
    clearSession();
  }

  function clearSession(): void {
    persistTokens(null, null);
    profile.value = null;
    captcha.value = null;
  }

  return {
    accessToken,
    refreshToken,
    profile,
    captcha,
    visiblePaths,
    firstVisiblePath,
    isAuthenticated,
    loadCaptcha,
    login,
    ssoLogin,
    fetchProfile,
    refreshSession,
    tryRestoreSession,
    handleUnauthorized,
    logout,
    clearSession,
  };
});
