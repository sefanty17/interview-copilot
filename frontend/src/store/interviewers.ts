/**
 * 角色卡（面试官）状态中枢 —— 以后端 `interviewer` 表为唯一权威源。
 *
 * 背景：#11「角色卡与 interviewer 表合并」。此前前端有两套人设数据：
 * `data/interviewers.ts`（静态角色卡）与 `data/personas.ts`（面试人设，字段重复）。
 * 合并后只剩一份：启动时拉 `GET /api/interviewers` + 逐张详情，覆盖到下面的
 * `interviewers` 数组；后端没起 / 请求失败时保留静态种子，房间照常演出。
 *
 * 数组用 `reactive` 且**原地替换**（splice），所以各组件导入后直接 `interviewers`
 * 使用即可拿到更新，不需要 `computed` / `.value`。
 */

import { reactive } from "vue";
import type { Interviewer, InterviewerCode } from "../data/interviewers";
import { interviewers as seedInterviewers } from "../data/interviewers";
import { fetchInterviewerDetail, fetchInterviewers, type InterviewerCard, type InterviewerDetailCard } from "../api/interviewers";

const seedByCode = new Map<string, Interviewer>(seedInterviewers.map((p) => [p.code, p]));

/** 当前生效的角色卡（初值 = 静态种子，加载成功后整段替换） */
export const interviewers = reactive<Interviewer[]>([...seedInterviewers]);

export const interviewerState = reactive({
  loaded: false,
  loading: false,
  error: null as string | null,
});

/** 按前端 code 取角色卡；找不到回落第一张（与旧的 findInterviewer 行为一致） */
export function findInterviewer(code: string): Interviewer {
  return interviewers.find((p) => p.code === code) ?? interviewers[0];
}

/**
 * 后端卡片 → 前端 Interviewer。
 *
 * 后端列可空，前端接口字段必填：缺什么就退回静态种子的同名字段；
 * 种子里也没有的新角色（库里新增的卡）走兜底默认值，保证房间能演出。
 */
function toInterviewer(card: InterviewerCard, detail: InterviewerDetailCard | null): Interviewer {
  const seed = seedByCode.get(card.code);
  const str = (v: string | null | undefined, fallback: string): string =>
    typeof v === "string" && v.trim() ? v : fallback;
  const num = (v: number | null | undefined, fallback: number): number =>
    typeof v === "number" && Number.isFinite(v) ? v : fallback;

  return {
    code: card.code as InterviewerCode,
    name: str(card.name, seed?.name ?? card.code),
    subtitle: str(card.subtitle, seed?.subtitle ?? ""),
    cardTitle: str(card.cardTitle, seed?.cardTitle ?? card.name),
    // 卡片信息：展示用单行文案取种子；多行原文优先取库里的 cardInfo
    cardInfo: seed?.cardInfo ?? "",
    displayRole: str(card.displayRole, seed?.displayRole ?? ""),
    opening: str(card.opening, seed?.opening ?? ""),
    followUp: num(detail?.followUp, seed?.followUp ?? 2),
    speed: num(detail?.speed, seed?.speed ?? 1),
    scale: num(card.scale, seed?.scale ?? 1),
    offsetX: num(card.offsetX, seed?.offsetX ?? 0),
    offsetY: num(card.offsetY, seed?.offsetY ?? 0),
    bubbleTop: num(card.bubbleTop, seed?.bubbleTop ?? 5),
    bubbleLeft: num(card.bubbleLeft, seed?.bubbleLeft ?? 25),
    userAlias: str(detail?.userAlias, seed?.userAlias ?? ""),
    systemPromptExampleOld: str(detail?.systemPromptExampleOld, seed?.systemPromptExampleOld ?? ""),
    thinkingMessage: str(detail?.thinkingMessage, seed?.thinkingMessage ?? ""),
    personaPrompt: str(detail?.personaPrompt, seed?.personaPrompt ?? ""),
    exampleDialogue: str(detail?.exampleDialogue, seed?.exampleDialogue ?? ""),
    cardInfoText: str(detail?.cardInfo, seed?.cardInfoText ?? ""),
  };
}

/**
 * 幂等懒加载：拉列表 + 逐张详情（详情补人设全文/范例/称呼）。
 *
 * 失败只写 `interviewerState.error`、不抛：角色卡是显示层数据，
 * 缺了它房间应照常演出（用静态种子），不能因为接口挂了就转圈。
 */
export async function ensureInterviewersLoaded(): Promise<void> {
  if (interviewerState.loaded || interviewerState.loading) return;
  interviewerState.loading = true;
  try {
    const cards = await fetchInterviewers();
    const merged = await Promise.all(
      cards.map(async (card) => {
        const detail = await fetchInterviewerDetail(card.code).catch(() => null);
        return toInterviewer(card, detail);
      }),
    );
    if (merged.length) interviewers.splice(0, interviewers.length, ...merged);
    interviewerState.loaded = true;
    interviewerState.error = null;
  } catch (e) {
    interviewerState.error = e instanceof Error ? e.message : String(e);
  } finally {
    interviewerState.loading = false;
  }
}
