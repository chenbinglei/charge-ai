<template>
  <RouterView />
</template>

<style>
/* 全局基础：html/body 占满视口，滚动一律由布局容器内部承担（外层不出滚动条）。 */
html,
body,
#app {
  height: 100%;
}

body {
  margin: 0;
  font-family: Inter, "PingFang SC", sans-serif;
}
</style>
