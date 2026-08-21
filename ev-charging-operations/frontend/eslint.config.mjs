import js from "@eslint/js";
import vue from "eslint-plugin-vue";
import globals from "globals";
import tseslint from "typescript-eslint";

export default tseslint.config(
  {
    ignores: [
      "**/dist/**",
      "**/coverage/**",
      "**/node_modules/**",
      "artifacts/**",
    ],
  },
  js.configs.recommended,
  ...vue.configs["flat/recommended"],
  ...tseslint.configs.recommended.map((config) => ({
    ...config,
    files: ["**/*.{ts,cts,mts,tsx}"],
  })),
  {
    files: ["**/*.{config,config.*}.mjs", "scripts/**/*.mjs"],
    languageOptions: {
      globals: globals.node,
    },
  },
  {
    files: [
      "apps/enterprise-web/src/**/*.{ts,vue}",
      "apps/operations-web/src/**/*.{ts,vue}",
      "apps/operations-dashboard/src/**/*.{ts,vue}",
    ],
    languageOptions: {
      globals: globals.browser,
    },
  },
  {
    files: ["**/*.{ts,cts,mts,tsx,vue}"],
    languageOptions: {
      parserOptions: {
        extraFileExtensions: [".vue"],
        parser: tseslint.parser,
      },
    },
    plugins: {
      "@typescript-eslint": tseslint.plugin,
      vue,
    },
    rules: {
      "@typescript-eslint/no-explicit-any": "error",
      "vue/html-closing-bracket-newline": "off",
      "vue/html-indent": "off",
      "vue/max-attributes-per-line": "off",
      "vue/multiline-html-element-content-newline": "off",
      "vue/multi-word-component-names": "off",
      "vue/singleline-html-element-content-newline": "off",
    },
  },
);
