import { ApiClient, AuthApi } from "@evco/api-client";

/**
 * API 客户端单例：开发态走 Vite 代理（/api → platform-service:8081），生产由反代承载；
 * 401 统一回调由 main.ts 装配（见 stores/auth.ts 的 handleUnauthorized）。
 */
export const apiClient = new ApiClient(
  import.meta.env.DEV || !import.meta.env.VITE_API_BASE
    ? window.location.origin
    : import.meta.env.VITE_API_BASE,
);

export const authApi = new AuthApi(apiClient);
