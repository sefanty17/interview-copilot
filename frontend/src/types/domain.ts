/** 领域类型定义（原先集中在 data/mock.ts，mock 数据删除后迁到这里） */

export type PersonaId = "strict" | "friendly" | "pressure" | "expert";

export interface Persona {
  id: PersonaId;
  name: string;
  title: string;
  style: string;
  tone: string;
  quote: string;
  followUp: number;
  speed: number;
  avatar: string;
  voice: string;
  opening: string;
}

export interface Job {
  id: string;
  name: string;
  en: string;
  desc: string;
  knowledgePoints: KnowledgePoint[];
}

export interface KnowledgePoint {
  id: string;
  name: string;
  group: string;
}

export interface Question {
  id: string;
  jobId: string;
  kp: string;
  type: string;
  difficulty: string;
  text: string;
  reference: string;
  followUps: string[];
}

export interface DimensionScore {
  name: string;
  score: number;
}

export interface QuestionReview {
  id: string;
  question: string;
  kp: string;
  score: number;
  comment: string;
  suggest: string;
}

export interface InterviewRecord {
  id: string;
  /**
   * 本场后端场次 id；有它报告页才能去后端拉 AI 评估。
   * 老存档没有这个字段，报告页会退回本地数据。
   */
  sessionId?: number;
  jobName: string;
  personaName: string;
  date: string;
  duration: string;
  score: number;
  dimensions: DimensionScore[];
  questions: QuestionReview[];
  advice: string[];
}

/** 环境音播放轨道（原版 uiStore.ambientTracks） */
export interface AmbientTrack {
  id: string;
  name: string;
  /** 单轨音量 0~100 */
  volume: number;
  /** 循环模式：single=单曲循环 / list=列表循环 / once=不循环 */
  loop: "single" | "list" | "once";
  paused: boolean;
}

/**
 * 单个大模型服务商配置，字段名沿用原版 api/services/llm-providers.ts 的 LlmProviderConfig。
 * provider 取值为 deepseek / openai / lmstudio / gemini / kimicode / codex。
 */
export interface LlmProviderConfig {
  id: string;
  label: string;
  provider: string;
  model: string;
  api_key: string;
  base_url: string;
  temperature: number | null;
  top_p: number | null;
  enable_thinking: boolean;
  reasoning_effort: string | null;
  fast_mode: boolean;
  /** 该模型是否支持原生多模态识图（勾选后发图直接走对话模型，不再旁白转述） */
  support_vision: boolean;
  /** 原生识图时是否压缩图片（默认 false：原图直发） */
  vision_compress: boolean;
  /** 压缩时图片最大边长（像素），超宽图等比缩放 */
  vision_max_edge: number;
  /** 压缩时 JPEG 编码质量（0-100） */
  vision_jpeg_quality: number;
}

/** 单个服务商预设（对应原版 constants/llm-presets.ts 的 LlmPreset） */
export interface LlmPreset {
  key: string;
  label: string;
  provider: string;
  model: string;
  base_url: string;
}

/** 单组滤镜参数（角色 / 背景共用，对应原版 FilterParams） */
export interface FilterParams {
  brightness: number;
  contrast: number;
  saturation: number;
  sepia: number;
  glow_radius: number;
  glow_color: string;
}

/** 场景光影参数（对应原版 LightingParams，存进 SceneInfo.lighting） */
export interface LightingParams {
  character: FilterParams;
  background: FilterParams;
  overlay_enabled: boolean;
  blend_mode: string;
  light_x: number;
  light_y: number;
  overlay_color1: string;
  overlay_color2: string;
  overlay_radius: number;
  overlay_opacity: number;
  overlay_target: string;
}

/** 面试场景（原版 SceneInfo，字段名沿用原版，供 settings/background/SceneCard.vue 使用） */
export interface SceneInfo {
  id: string;
  scene_name: string;
  scene_description: string;
  /** 背景图地址（真实图片，如 /backgrounds/day.webp）；空串表示没有背景图 */
  background: string;
  /** 所属子分类；null = 根目录 */
  category: string | null;
  /** 来源：game = 内置；plugin = 插件 */
  source?: string;
  plugin_id?: string | null;
  /** 室内外标注（原版由 LLM 标注一次，点击可在三者间循环） */
  location_type?: "indoor" | "outdoor" | "unknown";
  /** 收藏（收藏的场景排在前面） */
  favored?: boolean;
  /** 光影参数（原版 SceneEditModal 编辑，可为空） */
  lighting?: LightingParams | null;
}

/** 单个识别服务商的配置：键由后端 config_fields 动态给出，故用索引签名承载任意键 */
export interface AsrProviderConfig {
  [key: string]: string | number | boolean | Record<string, string>;
}

/** 识别模型的本地下载状态 */
export interface AsrModelState {
  installed: boolean;
  /** 下载进度（0–100） */
  progress: number;
}

/** 工具访问审批模式（命名沿用原版 ToolAccessMode）。 */
export type ToolAccessMode = "manual" | "auto_approve" | "full_access";

/** 每轮发言前的工具推荐方式。 */
export type ToolRecommendMode = "off" | "agent" | "rag";

/** 网页搜索工具配置（字段名沿用原版 WebSearchSettings）。 */
export interface WebSearchSettings {
  enabled: boolean;
  provider: string;
  api_key: string;
  base_url: string;
  proxy_enabled: boolean;
  proxy_addr: string;
  max_results: number;
  hide_search_results: boolean;
}

/** 图片/视频识别工具配置（字段名沿用原版 MediaFileSettings）。 */
export interface MediaFileSettings {
  image_enabled: boolean;
  video_enabled: boolean;
  max_file_mb: number;
  image_max_edge: number;
  jpeg_quality: number;
  max_output_tokens: number;
  default_prompt: string;
}

export interface SiteSettings {
  personaId: PersonaId;
  jobId: string;
  kps: string[];
  questionCount: number;
  timeLimit: number;
  followUpDepth: number;
  voiceOn: boolean;
  customName: string;
  tone: string;
  opening: string;
  speed: number;
  model: string;
  asr: string;

  // 通用
  fontFamily: string;
  textSpeed: number;
  autoAdvanceDelay: number;
  mergeThreshold: number;
  mergeDelay: number;
  mergeMode: string;
  /** 台词合并时动作文本的处理方式：append=接在后面显示 / replace=独立显示本次动作 */
  mergeMotionMode: "append" | "replace";
  inlineAction: boolean;
  replyCount: number;
  idleRemind: boolean;
  rememberPreference: boolean;
  soundEffect: boolean;
  /** 启用永久记忆：开启后对话记忆自动压缩存档 */
  permanentMemory: boolean;
  /** 语音缓存占用（字节） */
  ttsCacheSize: number;
  /** 语音缓存文件数 */
  ttsCacheFiles: number;
  /** 局域网数据同步开关 */
  lanSyncEnabled: boolean;

  // 背景
  sceneId: string;
  starOn: boolean;
  meteorOn: boolean;
  particleFps: number;
  weather: string;
  /**
   * 房间粒子特效（原版 GameBackground 的背景特效槽）。
   * 取值：none / baparticles / blizzard / drizzle / fireflies / fireworks /
   * fog / meteorshower / rain / sakura / snow / starfield / thunderstorm
   */
  particleType: string;

  // 声音
  outputDevice: string;
  inputDevice: string;
  volumeSpeaker: number;
  volumeSfx: number;
  volumeBgm: number;
  volumeAchievement: number;
  volumeAmbient: number;
  bgmOn: boolean;
  /** 音频频谱开关（右下角迷你频谱，字段沿用原版 audio.spectrum*） */
  spectrumEnabled: boolean;
  /** 频谱形态：mirror=镜像 / bars=柱状 / ring=圆环 */
  spectrumStyle: string;
  /** 频谱配色 id（constants/spectrum.ts） */
  spectrumPalette: string;
  /** 自定义配色主色 */
  spectrumColor1: string;
  /** 自定义配色辅色 */
  spectrumColor2: string;
  bgmTrack: string;
  /** 背景音乐播放模式：loop-list=列表循环 / loop-single=单曲循环 / random=随机播放 */
  bgmMode: "loop-list" | "loop-single" | "random";
  ambientOn: boolean;
  ambientTrack: string;
  /** 环境音播放轨道列表 */
  ambientTracks: AmbientTrack[];
  ttsOn: boolean;
  ttsType: string;
  ttsSpeed: number;
  ttsVoice: string;
  ttsPitch: number;
  ttsVolume: number;
  ttsLanguage: string;
  ttsEmotion: string;
  ttsApiUrl: string;
  ttsApiKey: string;
  ttsPort: number;
  ttsModel: string;
  ttsSpeakerId: number;
  ttsStyleId: number;
  ttsReferenceAudio: string;
  ttsReferenceText: string;
  ttsAudioFormat: string;
  ttsDialect: string;
  ttsLengthScale: number;
  ttsSdpRatio: number;
  ttsNoiseScale: number;
  ttsTimeout: number;
  ttsConcurrency: number;
  ttsCacheEnabled: boolean;
  ttsCacheLimit: number;
  ttsRetryCount: number;
  ttsCloneName: string;
  ttsCloneSample: string;
  ttsCloneText: string;
  ttsCloneLanguage: string;
  ttsImportVoiceId: string;
  ttsStyleVectorsTarget: string;
  ttsPreviewText: string;
  ttsPreviewSdp: number;
  ttsEngineConfigs: Record<string, Record<string, string | number | boolean>>;
  asrOn: boolean;

  // 语音识别（SettingsAsr）：命名沿用原版 AsrSettings
  /** 语音输入总开关：关闭后自动监听与手动麦克风一并停用 */
  voice_input_enabled: boolean;
  /** 语音快捷键绑定（JSON 字符串，如 {"key":"f8"}） */
  ptt_key: string;
  /** 失去焦点时快捷键仍可用（全局快捷键） */
  ptt_global: boolean;
  /** 自动监听麦克风，识别到说话后自动转为文本 */
  auto_listen: boolean;
  /** 连续对话：识别完成后继续监听下一轮 */
  asr_continuous: boolean;
  /** 唤醒词开关 */
  wake_word_enabled: boolean;
  /** 唤醒词文本 */
  wake_word: string;
  /** 停止说话后等待该时长才结束录音（毫秒） */
  vad_silence_ms: number;
  /** AI 说完话后等待该时长才恢复语音触发（毫秒） */
  energy_warmup_ms: number;
  /** 详细 VAD 能量检测日志开关 */
  vad_debug_log: boolean;
  /** 识别完成后：fill_only=填入输入框 / auto_send=自动发送 */
  send_mode: "fill_only" | "auto_send";
  /** 流式识别 */
  stream_enabled: boolean;
  /** 当前识别服务商（引擎） */
  active_provider: string;
  /** 各识别服务商配置，键为 provider id */
  provider_configs: Record<string, AsrProviderConfig>;
  /** 麦克风设备 */
  asr_mic_device: string;
  /** 麦克风音量阈值（0–1） */
  asr_mic_threshold: number;
  /** 静音检测（低于阈值不送识别） */
  asr_mic_gate: boolean;
  /** 模型下载状态，键为模型 id */
  asr_model_states: Record<string, AsrModelState>;
  /** 最近一次测试识别的结果文本 */
  asr_last_result: string;

  // 工具配置（SettingsTools）：字段名沿用原版 ToolSettings
  web_search: WebSearchSettings;
  media_file: MediaFileSettings;
  groups: Record<string, boolean>;
  access_mode: ToolAccessMode;
  max_tool_rounds: number;
  tool_rag: boolean;
  tool_recommend_mode: ToolRecommendMode;

  // 投屏设置（SettingsCast）：字段名沿用原版页面状态
  enabled: boolean;
  port: number;
  fps: number;
  quality: number;
  castWidth: number;
  castHeight: number;
  vivid: boolean;
  charScale: number;
  charOffsetX: number;
  charOffsetY: number;
  dialogWidth: number;
  dialogHeight: number;
  dialogFontSize: number;
  dialogBgOpacity: number;
  dialogHidden: boolean;

  // 高级设置
  llmBaseUrl: string;
  llmKey: string;
  llmModel: string;
  temperature: number;
  maxTokens: number;
  promptPreset: string;

  // 大模型接入（SettingsLlmProviders）
  /** 已配置的服务商列表 */
  llmProviders: LlmProviderConfig[];
  /** 角色 → 服务商，null 表示对话模型未选 / 其余角色跟随对话模型 */
  llmChatProviderId: string | null;
  llmTranslateProviderId: string | null;
  llmGodAgentProviderId: string | null;
  llmVisionProviderId: string | null;
}

/** 单条面试印象变化记录 */
export interface ImpressionHistoryEntry {
  time: string;
  /** 本次变化量（正=印象变好，负=变差） */
  delta: number;
  /** 变化后的六维平均值 */
  value: number;
  reason: string;
}

export interface InterviewAchievement {
  id: string;
  title: string;
  description: string;
  /** normal=普通 / rare=稀有 */
  type: "normal" | "rare";
  /** 隐藏成就：未解锁时只显示 ??? */
  hidden: boolean;
  unlocked: boolean;
  current_progress: number;
  target_progress: number;
}

/**
 * 永久记忆调试（SettingsMemoryDebug）
 * 字段命名沿用原版 references/lingchat/src/api/services/memory-debug.ts：
 * MemoryDebugRole / MemorySectionStat / MemorySystemSnapshot / RoleMemorySnapshot
 */

/** 记忆库分区 key，与原版 GameMemoryBankData 的四个字段一致 */
export type MemorySectionKey = "short_term" | "long_term" | "user_info" | "promises";

/** 条目重要度：原版按重要度过滤记忆条目 */
export type MemoryImportance = "high" | "medium" | "low";

export interface MemoryDebugRole {
  roleId: number;
  displayName: string;
  /** 运行时是否已创建 */
  runtimePresent: boolean;
  enabled: boolean;
}

export interface MemorySectionStat {
  key: MemorySectionKey;
  /** 注入上限（字），0 = 不截断 */
  limit: number;
  /** 注入时会被截断 */
  truncated: boolean;
}

export interface MemoryEntry {
  id: string;
  /** 所属分区 */
  section: MemorySectionKey;
  /** 摘要：列表与注入视图里显示的那段 */
  summary: string;
  /** 原文：展开后看到的压缩前原文 */
  content: string;
  importance: MemoryImportance;
  /** 权重 0–1：注入时的排序权重 */
  weight: number;
  /** 命中次数：被检索命中的次数 */
  hitCount: number;
  /** 创建时间 */
  createdAt: string;
  /** 该条记忆占用的 token */
  tokens: number;
}

export interface MemoryDebugSnapshot {
  displayName: string;
  /** 已累积的可见台词数（压缩触发进度） */
  accumulatedVisibleCount: number;
  updateInterval: number;
  enabled: boolean;
  aiName: string;
  isUpdating: boolean;
  hasPending: boolean;
  failCount: number;
  cooldownRemainingMs: number;
  recentWindow: number;
  pointerRaw: number;
  pointerEffective: number;
  lineCount: number;
  historyRevision: number;
  pointerOutOfRange: boolean;
  sections: MemorySectionStat[];
  bank: {
    schema_version: number;
    meta: { last_processed_global_idx: number; updated_at: string };
    data: Record<MemorySectionKey, string>;
  };
  injectedSystemText: string;
  injectedShortTermText: string;
  /** 压缩次数 */
  compressCount: number;
  /** 记忆占用 token */
  tokenUsed: number;
  /** 上次压缩时间 */
  lastCompressedAt: string;
  /** 真实上下文（下一次请求的基础） */
  context: { role: string; content: string; tool_call_id?: string; tool_calls?: unknown[] }[];
  entries: MemoryEntry[];
}

export interface MemoryDebugOverview {
  currentRoleId: number | null;
  activeSavePersistent: boolean;
  activeSaveId: string;
  roles: MemoryDebugRole[];
}
