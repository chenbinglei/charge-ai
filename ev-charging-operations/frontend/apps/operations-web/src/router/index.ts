import { createRouter, createWebHistory } from "vue-router";
import { MENU_PHASE } from "./menuPhase";
import { useAuthStore } from "@/stores/auth";

/** 登录态免鉴权路由；其余路由一律要求登录且在可见菜单内。 */
const PUBLIC_PATHS = new Set(["/login", "/sso/login", "/403", "/404"]);

/** 由全量 73 叶子路径生成骨架路由；W2 交付页在后续批次替换为真实组件。 */
const skeletonRoutes = Object.keys(MENU_PHASE).map((path) => ({
  path,
  name: path.slice(1).replaceAll("/", "-"),
  component: () => import("@/views/PlaceholderView.vue"),
  meta: { phase: MENU_PHASE[path] },
}));

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", name: "login", component: () => import("@/views/LoginView.vue") },
    { path: "/sso/login", name: "sso-login", component: () => import("@/views/SsoLoginView.vue") },
    { path: "/403", name: "forbidden", component: () => import("@/views/ForbiddenView.vue") },
    { path: "/404", name: "not-found", component: () => import("@/views/NotFoundView.vue") },
    {
      path: "/",
      component: () => import("@/layouts/MainLayout.vue"),
      children: skeletonRoutes,
    },
    { path: "/:pathMatch(.*)*", redirect: "/404" },
  ],
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();
  // 公开路由直接放行；登录后访问 /login 重定向首个可见菜单。
  if (PUBLIC_PATHS.has(to.path)) {
    if (to.path === "/login" && auth.isAuthenticated) {
      return auth.firstVisiblePath ?? true;
    }
    return true;
  }
  // 会话恢复：未登录，或页面刷新场景（token 在 localStorage 但 profile 未加载）。
  // 后者不恢复时 visiblePaths 为空集合，任何路由都会被误判 403。
  if (!auth.isAuthenticated || auth.profile === null) {
    const restored = await auth.tryRestoreSession();
    if (!restored) {
      return { path: "/login", query: { redirect: to.fullPath } };
    }
  }
  // 首个叶子菜单作为登录后落点。
  if (to.path === "/") {
    return auth.firstVisiblePath ?? "/403";
  }
  // 路由守卫：访问不在可见菜单中的路由重定向 403（W2 页面清单 §0.1.1）。
  if (!auth.visiblePaths.has(to.path)) {
    return "/403";
  }
  return true;
});

export default router;
