import { computed, reactive } from "vue";
import { findInterviewer } from "../data/interviewers";
import { defaultSettings } from "../constants/defaultSettings";
import { jobs } from "../data/jobs";
import { personas } from "../data/personas";
import { scenes as seedScenes } from "../data/scenes";
import type { InterviewRecord, SceneInfo, SiteSettings } from "../types/domain";
import { seedScripts } from "../data/scripts";
import type { InterviewScript } from "../data/scripts";

const STORAGE_KEY = "interview-lab-v1";

export interface ChatMessage {
  id: string;
  role: "ai" | "user";
  text: string;
  time: string;
  kind?: "text" | "question";
  /** 情绪标签，对应 public/avatars/{code}/{emotion}.webp；缺省回落「正常」 */
  emotion?: string;
  meta?: {
    index: number;
    total: number;
    kp: string;
    type: string;
    difficulty?: string;
  };
}

export type SessionMode = "free" | "script";

export interface ActiveSession {
  active: boolean;
  /** free = 自由面试（无固定题量，用户点结束才停）；script = 剧本面试（按章节推进） */
  mode: SessionMode;
  jobId: string;
  personaId: string;
  questionIndex: number;
  messages: ChatMessage[];
  startedAt: number;
  elapsed: number;
}

interface Persisted {
  /** 本地数据结构版本：数据形状变了就 +1，load() 里按版本补迁移（见 SCHEMA_VERSION） */
  schemaVersion: number;
  settings: SiteSettings;
  scripts: InterviewScript[];
  records: InterviewRecord[];
  session: ActiveSession;
  /** 场景库（背景页的场景卡片） */
  scenes: SceneInfo[];
  /** 用户新建的场景子分类（不含「全部 / 根目录 / 插件」三个保留项） */
  sceneCategories: string[];
}

/**
 * 本地存储版本。
 * v3：场景库由 9 条编造场景换成 3 条真实场景（booth / night / day）。旧存档里的假场景
 * 没有背景图（渐变占位已删），会渲染成「背景图缺失」，所以按版本整段重置 scenes。
 * v4：清掉旧存档里残留的 3 条演示面试记录（r-1024 / r-1018 / r-1012）。
 */
const SCHEMA_VERSION = 4;

/** 旧版 mockRecords 的三条演示记录 id：v4 迁移时从本地存档里剔除 */
const DEMO_RECORD_IDS = new Set(["r-1024", "r-1018", "r-1012"]);

/** load() 是否做过迁移：是则启动时立刻写回，避免每次刷新都重跑一遍 */
let migrated = false;


function nowTime(): string {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

export function createEmptySession(
  jobId = "java",
  personaId: SiteSettings["personaId"] = "strict",
  mode: SessionMode = "free",
): ActiveSession {
  return {
    active: false,
    mode,
    jobId,
    personaId,
    questionIndex: 0,
    messages: [],
    startedAt: 0,
    elapsed: 0,
  };
}

const defaultState: Persisted = {
  schemaVersion: SCHEMA_VERSION,
  settings: { ...defaultSettings },
  scripts: JSON.parse(JSON.stringify(seedScripts)),
  records: [],
  session: createEmptySession(),
  scenes: JSON.parse(JSON.stringify(seedScenes)),
  sceneCategories: [],
};

function load(): Persisted {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return JSON.parse(JSON.stringify(defaultState));
    const parsed = JSON.parse(raw) as Partial<Persisted>;
    // 没有版本号 = v1 之前的老存档（场景库还是那 9 条假场景）
    const version = typeof parsed.schemaVersion === "number" ? parsed.schemaVersion : 0;
    const settings = { ...defaultSettings, ...(parsed.settings ?? {}) };

    // v3 迁移：scenes 整段换成真实场景；records / sessions 是用户数据，原样保留
    let scenes: SceneInfo[];
    if (version < 3) {
      scenes = JSON.parse(JSON.stringify(seedScenes));
      // 选中的场景若已不存在（旧假场景被换掉），清空以回落到内置背景图
      if (!scenes.some((s) => s.id === settings.sceneId)) settings.sceneId = "";
    } else {
      scenes = parsed.scenes?.length
        ? (parsed.scenes as SceneInfo[])
        : JSON.parse(JSON.stringify(seedScenes));
    }
    if (version < SCHEMA_VERSION) migrated = true;

    let records = Array.isArray(parsed.records) ? (parsed.records as InterviewRecord[]) : [];
    // v4 迁移：示例记录混在用户真实记录里，按 id 精确剔除，其余原样保留
    if (version < 4) records = records.filter((r) => !DEMO_RECORD_IDS.has(r.id));

    return {
      schemaVersion: SCHEMA_VERSION,
      settings,
      scripts: parsed.scripts?.length && parsed.scripts.every((s: any) => Array.isArray(s?.chapters))
        ? (parsed.scripts as InterviewScript[])
        : JSON.parse(JSON.stringify(seedScripts)),
      // 存档记录以本地为准：没有记录就是空列表，不再回填示例数据
      records,
      session: { ...createEmptySession(), ...(parsed.session ?? {}) },
      scenes,
      sceneCategories: Array.isArray(parsed.sceneCategories) ? parsed.sceneCategories : [],
    };
  } catch {
    return JSON.parse(JSON.stringify(defaultState));
  }
}

export const store = reactive<Persisted & { toast: string }>({
  ...load(),
  toast: "",
});

// 迁移过的旧存档立刻按新结构 + 新版本号写回
if (migrated) persist();

let toastTimer: number | undefined;

export function persist(): void {
  const data: Persisted = {
    schemaVersion: SCHEMA_VERSION,
    settings: store.settings,
    scripts: store.scripts,
    records: store.records,
    session: store.session,
    scenes: store.scenes,
    sceneCategories: store.sceneCategories,
  };
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
}

export function notify(message: string): void {
  store.toast = message;
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => {
    store.toast = "";
  }, 2200);
}

export function updateSettings(patch: Partial<SiteSettings>): void {
  Object.assign(store.settings, patch);
  persist();
}

export function resetSettings(): void {
  store.settings = { ...defaultSettings };
  persist();
}

export function setPersona(id: SiteSettings["personaId"]): void {
  store.settings.personaId = id;
  persist();
}

export function toggleKp(id: string): void {
  const list = store.settings.kps;
  const idx = list.indexOf(id);
  if (idx >= 0) list.splice(idx, 1);
  else list.push(id);
  persist();
}

export function startSession(
  jobId: string,
  personaId: SiteSettings["personaId"],
  mode: SessionMode = "free",
): void {
  const persona = findInterviewer(personaId);
  store.session = {
    active: true,
    mode,
    jobId,
    personaId,
    questionIndex: 0,
    messages: [
      {
        id: `m-${Date.now()}`,
        role: "ai",
        // 原卡人设自带的开场（R1b）：不追加「—— 当前岗位：X · Y」后缀，
        // 那句是面试语义，自由对话里角色就是 LingChat 原版的角色卡。
        // 后端在线时这句会被 ensureServerSession 用后端开场白整条换掉。
        text: persona.opening,
        time: nowTime(),
      },
    ],
    startedAt: Date.now(),
    elapsed: 0,
  };
  persist();
}

export function resumeSession(): void {
  store.session.active = true;
  if (!store.session.startedAt) store.session.startedAt = Date.now();
  persist();
}

export function clearSession(): void {
  store.session = createEmptySession(store.settings.jobId, store.settings.personaId);
  persist();
}

export function pushMessage(
  role: ChatMessage["role"],
  text: string,
  extra: Pick<ChatMessage, "kind" | "meta" | "emotion"> = {},
): void {
  store.session.messages.push({
    id: `m-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
    role,
    text,
    time: nowTime(),
    ...extra,
  });
  persist();
}

export function addRecord(record: InterviewRecord): void {
  store.records.unshift(record);
  persist();
}

export function saveScripts(): void {
  persist();
}

export function createScript(): InterviewScript {
  const n = store.scripts.length + 1;
  const s: InterviewScript = {
    code: `custom-${Date.now()}`,
    script_name: `未命名面试剧本 ${n}`,
    intro_chapter: "intro",
    description: "新的面试流程",
    order_num: n,
    job_code: store.settings.jobId,
    chapters: [
      {
        code: "intro",
        name: "开场",
        order_num: 1,
        events: [
          { type: "ai_dialogue", character: "MAIN", prompt: "以面试官身份开场，请候选人做一分钟自我介绍。" },
          { type: "input", hint: "一分钟自我介绍" },
          { type: "chapter_end", end_type: "linear", next_chapter: "" },
        ],
      },
    ],
  };
  store.scripts.push(s);
  persist();
  return s;
}

export function deleteScript(code: string): void {
  const idx = store.scripts.findIndex((s) => s.code === code);
  if (idx >= 0) {
    store.scripts.splice(idx, 1);
    persist();
  }
}

export const currentPersona = computed(
  () => personas.find((p) => p.id === store.settings.personaId) ?? personas[0],
);
export const currentJob = computed(() => jobs.find((j) => j.id === store.settings.jobId) ?? jobs[0]);
export const hasActiveSession = computed(() => store.session.active && store.session.messages.length > 0);
