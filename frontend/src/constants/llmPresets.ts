import type { LlmPreset } from "../types/domain";

/**
 * LLM 提供商预设（快速配置），照搬原版 constants/llm-presets.ts。
 * 前端 SettingsLlmProviders.vue 遍历本数组渲染预设按钮，点击即填充表单字段。
 */
export const llmPresets: LlmPreset[] = [
  {
    key: "deepseek-flash",
    label: "DeepSeek V4.1 Flash",
    provider: "deepseek",
    model: "deepseek-flash",
    base_url: "https://api.deepseek.com",
  },
  {
    key: "deepseek-v4-pro",
    label: "DeepSeek V4 Pro",
    provider: "deepseek",
    model: "deepseek-v4-pro",
    base_url: "https://api.deepseek.com",
  },
  {
    key: "qwen-max",
    label: "通义千问 Max",
    provider: "openai",
    model: "qwen3.8-max",
    base_url: "https://dashscope.aliyuncs.com/compatible-mode/v1",
  },
  {
    key: "qwen-plus",
    label: "通义千问 Plus",
    provider: "openai",
    model: "qwen3.7-plus",
    base_url: "https://dashscope.aliyuncs.com/compatible-mode/v1",
  },
  {
    key: "qwen-flash",
    label: "通义千问 Flash",
    provider: "openai",
    model: "qwen3.8-flash",
    base_url: "https://dashscope.aliyuncs.com/compatible-mode/v1",
  },
  {
    key: "kimi",
    label: "Kimi K2.6",
    provider: "openai",
    model: "kimi-k2.6",
    base_url: "https://api.moonshot.cn/v1",
  },
  {
    key: "zhipu-glm",
    label: "智谱 GLM",
    provider: "openai",
    model: "glm-4.7",
    base_url: "https://open.bigmodel.cn/api/paas/v4",
  },
  {
    key: "ollama",
    label: "Ollama",
    provider: "openai",
    model: "",
    base_url: "http://localhost:11434/v1",
  },
  {
    key: "lmstudio",
    label: "LM Studio",
    provider: "lmstudio",
    model: "",
    base_url: "http://localhost:1234/v1",
  },
  {
    key: "codex",
    label: "OpenAI Codex",
    provider: "codex",
    model: "gpt-5.6-sol",
    base_url: "",
  },
];
