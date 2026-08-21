import { createPinia } from "pinia";
import { createApp } from "vue";
import "@evco/design-tokens/themes.css";
import "element-plus/theme-chalk/dark/css-vars.css";
import "element-plus/es/components/button/style/css";
import "element-plus/es/components/form/style/css";
import "element-plus/es/components/form-item/style/css";
import "element-plus/es/components/input/style/css";
import "element-plus/es/components/message/style/css";
import "element-plus/es/components/dropdown/style/css";
import "element-plus/es/components/dropdown-menu/style/css";
import "element-plus/es/components/dropdown-item/style/css";
import "element-plus/es/components/avatar/style/css";
import App from "./App.vue";
import router from "./router";
import { apiClient } from "./api";
import { useAuthStore } from "./stores/auth";

const app = createApp(App);

// Pinia 先装：401 回调与路由守卫均依赖 auth store。
app.use(createPinia());

// 401 统一回调装配（api-client 不做副作用）：静默刷新失败即清除会话，
// 后续导航由路由守卫带回登录页。
const auth = useAuthStore();
apiClient.setOnUnauthorized(() => {
  void auth.handleUnauthorized();
});

app.use(router);
app.mount("#app");
