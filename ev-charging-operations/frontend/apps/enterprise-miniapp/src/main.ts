import { createSSRApp } from "vue";
import App from "./App.vue";

export function createApp() {
  const application = createSSRApp(App);
  return { app: application };
}
