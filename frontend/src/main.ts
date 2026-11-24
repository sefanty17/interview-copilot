import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import { ensureInterviewersLoaded } from "./store/interviewers";
import "./styles/tokens.css";
import "./styles/base.css";
import "./styles/menu.css";
import "./styles/console.css";
import "./styles/views.css";
import "./styles/lingchat.css";
import "./styles/lingchat-globals.css";
// 立绘情绪动画与气泡样式（全局注入，全项目只准有这一份呼吸关键帧定义）
import "./assets/styles/avatar-animation.css";

// 角色卡以后端 interviewer 表为准：启动拉一次，失败自动回落静态种子（不阻塞首屏）
void ensureInterviewersLoaded();

createApp(App).use(router).mount("#app");
