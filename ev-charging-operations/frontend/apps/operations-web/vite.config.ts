import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

// 开发代理：本地 backend platform-service（8081）；生产由网关/反代承载。
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    host: true,
    port: 5173,
    proxy: {
      "/api": {
        target: process.env.EVCO_API_PROXY ?? "http://localhost:8081",
        changeOrigin: true,
      },
    },
  },
});
