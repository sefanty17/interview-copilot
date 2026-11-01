import type { Job } from "../types/domain";

/** 岗位与知识点（面试配置、剧本编辑、记录筛选共用） */
export const jobs: Job[] = [
  {
    id: "java",
    name: "Java 后端",
    en: "JAVA BACKEND",
    desc: "JVM、并发、Spring、MySQL、Redis",
    knowledgePoints: [
      { id: "jvm", name: "JVM 与内存模型", group: "Java 基础" },
      { id: "collection", name: "集合与源码", group: "Java 基础" },
      { id: "concurrent", name: "并发编程", group: "Java 基础" },
      { id: "spring", name: "Spring / Spring Boot", group: "框架" },
      { id: "mysql", name: "MySQL 与索引", group: "存储" },
      { id: "redis", name: "Redis 与缓存", group: "存储" },
      { id: "mq", name: "消息队列", group: "中间件" },
      { id: "micro", name: "微服务与分布式", group: "架构" },
    ],
  },
  {
    id: "frontend",
    name: "Web 前端",
    en: "WEB FRONTEND",
    desc: "JavaScript、Vue、浏览器、工程化",
    knowledgePoints: [
      { id: "js", name: "JavaScript 基础", group: "语言" },
      { id: "ts", name: "TypeScript", group: "语言" },
      { id: "vue", name: "Vue 原理", group: "框架" },
      { id: "browser", name: "浏览器与网络", group: "基础" },
      { id: "css", name: "CSS 与布局", group: "基础" },
      { id: "build", name: "工程化与构建", group: "工程" },
      { id: "perf", name: "性能优化", group: "进阶" },
      { id: "security", name: "前端安全", group: "进阶" },
    ],
  },
];
