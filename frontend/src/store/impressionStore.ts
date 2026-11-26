/**
 * 面试印象状态：正面六维 + 负面六维 + 变化历史。
 *
 * <p>数据来自后端 {@code session_impression}（按场次 + 面试官存），
 * 每 3 条候选人回答评估一次，通过 {@code ai:impression} 事件推送过来；
 * 刷新或重进房间时用 {@code GET /api/sessions/{id}/impression} 恢复。
 *
 * <p>早前从 LingChat 迁过来的「好感 / 亲密 / 思念」那套恋爱维度已废弃：
 * 这里换成招聘场景真正在判断的方向（专业性 / 表达力 / 逻辑性 / 深度 / 可信度 / 潜力）。
 */
import { computed, reactive } from "vue";
import type { ImpressionHistoryEntry } from "../types/domain";
import type { ImpressionView } from "../api/chat";

export interface ImpressionVector {
  professionalism: number;
  clarity: number;
  logic: number;
  depth: number;
  credibility: number;
  potential: number;
}

export interface NegativeVector {
  skepticism: number;
  confusion: number;
  impatience: number;
  disappointment: number;
  wariness: number;
  reservation: number;
}

/** 后端 ImpressionView 的镜像（正向 / 负向各六维、增量与评估依据）。 */
export type ImpressionViewPayload = ImpressionView;

export interface ImpressionChangeRecord {
  roleId: number;
  deltas: Partial<ImpressionVector>;
  negativeDeltas: Partial<NegativeVector>;
  reason: string;
}

function zeroPositive(): ImpressionVector {
  return { professionalism: 0, clarity: 0, logic: 0, depth: 0, credibility: 0, potential: 0 };
}

function zeroNegative(): NegativeVector {
  return {
    skepticism: 0,
    confusion: 0,
    impatience: 0,
    disappointment: 0,
    wariness: 0,
    reservation: 0,
  };
}

export const impressionStore = reactive({
  roleId: 0,
  /** 是否已经产生过至少一次评估；未评估时界面显示「待观察」而不是 0 分 */
  evaluated: false,
  evaluatedUserCount: 0,
  reason: "",
  dims: zeroPositive(),
  negDims: zeroNegative(),
  history: [] as ImpressionHistoryEntry[],
  lastChange: null as ImpressionChangeRecord | null,
});

/** 正面六维平均；未评估时为 null */
export const impressionAverage = computed<number | null>(() => {
  if (!impressionStore.evaluated) return null;
  const d = impressionStore.dims;
  return Math.round(
    (d.professionalism + d.clarity + d.logic + d.depth + d.credibility + d.potential) / 6,
  );
});

export const negativePeak = computed(() => Math.max(...Object.values(impressionStore.negDims)));

function nowTime(): string {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

/**
 * 应用一次后端评估结果：覆盖六维当前值，并把本次增量交给飘出标签组件。
 *
 * <p>用覆盖而不是累加：后端返回的就是权威值，前端自己加会和刷新后的状态不一致。
 */
export function applyImpression(view: ImpressionViewPayload): void {
  const before = impressionAverage.value ?? 0;
  const deltas: Partial<ImpressionVector> = {};
  for (const [key, value] of Object.entries(view.positiveDeltas ?? {})) {
    (deltas as Record<string, number>)[key] = value;
  }
  const negativeDeltas: Partial<NegativeVector> = {};
  for (const [key, value] of Object.entries(view.negativeDeltas ?? {})) {
    (negativeDeltas as Record<string, number>)[key] = value;
  }

  Object.assign(impressionStore.dims, zeroPositive(), view.positive ?? {});
  Object.assign(impressionStore.negDims, zeroNegative(), view.negative ?? {});
  impressionStore.roleId = view.roleId;
  impressionStore.evaluated = view.evaluated;
  impressionStore.evaluatedUserCount = view.evaluatedUserCount;
  impressionStore.reason = view.reason ?? "";

  if (Object.keys(deltas).length === 0 && Object.keys(negativeDeltas).length === 0) {
    return;
  }
  impressionStore.lastChange = {
    roleId: view.roleId,
    deltas,
    negativeDeltas,
    reason: view.reason ?? "",
  };
  impressionStore.history.push({
    time: nowTime(),
    delta: (impressionAverage.value ?? 0) - before,
    value: impressionAverage.value ?? 0,
    reason: view.reason ?? "",
  });
}

/** 新开一场面试时清空：印象属于「这一局」，不跨场次继承。 */
export function resetImpression(): void {
  impressionStore.roleId = 0;
  impressionStore.evaluated = false;
  impressionStore.evaluatedUserCount = 0;
  impressionStore.reason = "";
  Object.assign(impressionStore.dims, zeroPositive());
  Object.assign(impressionStore.negDims, zeroNegative());
  impressionStore.history.splice(0, impressionStore.history.length);
  impressionStore.lastChange = null;
}
