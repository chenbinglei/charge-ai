import { createPinia } from "pinia";
import { createApp } from "vue";
import "@evco/design-tokens/themes.css";
import App from "./App.vue";

createApp(App).use(createPinia()).mount("#app");
