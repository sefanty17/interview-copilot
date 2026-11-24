/**
 * 面试官（角色卡）只读 API —— 对齐后端 InterviewerController（`interviewer` 表）。
 *
 * - `GET /api/interviewers`         列表：只吐卡面字段（InterviewerSummary）
 * - `GET /api/interviewers/{code}`  详情：卡面 + 人设全文 / 范例 / 称呼（InterviewerDetail）
 *
 * 后端是角色卡的唯一权威源；`data/interviewers.ts` 只作为后端不可用时的离线种子。
 */

/** 列表项（后端 InterviewerSummary）。后端 DDL 列可空，故长字段都用 `| null`。 */
export interface InterviewerCard {
  code: string;
  name: string;
  subtitle: string | null;
  cardTitle: string | null;
  avatarMode: string | null;
  scale: number | null;
  offsetX: number | null;
  offsetY: number | null;
  bubbleTop: number | null;
  bubbleLeft: number | null;
  displayRole: string | null;
  opening: string | null;
  sortOrder: number | null;
}

/** 详情（后端 InterviewerDetail）：列表项 + 长文本与对话参数。 */
export interface InterviewerDetailCard extends InterviewerCard {
  cardInfo: string | null;
  personaPrompt: string | null;
  exampleDialogue: string | null;
  systemPromptExampleOld: string | null;
  thinkingMessage: string | null;
  userAlias: string | null;
  userAliasSubtitle: string | null;
  followUp: number | null;
  speed: number | null;
}

async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new Error(`面试官接口返回 HTTP ${response.status}`);
  }
  return (await response.json()) as T;
}

/** 角色卡列表（按 sort_order 升序，后端已排好） */
export async function fetchInterviewers(): Promise<InterviewerCard[]> {
  const response = await fetch("/api/interviewers");
  return readJson<InterviewerCard[]>(response);
}

/** 单张角色卡详情；404 视为不存在，返回 null 而不抛 */
export async function fetchInterviewerDetail(code: string): Promise<InterviewerDetailCard | null> {
  const response = await fetch(`/api/interviewers/${encodeURIComponent(code)}`);
  if (response.status === 404) return null;
  return readJson<InterviewerDetailCard>(response);
}
