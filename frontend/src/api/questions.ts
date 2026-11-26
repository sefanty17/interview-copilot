/**
 * 岗位题库 API（课设指标 ① / issue #1：题目从后端取，不再只写在前端）。
 *
 * 端点：
 * - `GET /api/questions?jobCode=java&type=technical&limit=20&offset=0`
 *
 * 字段与后端 `QuestionView` 一一对应。`type` 是中文题型（技术知识/项目深挖/场景题/行为题），
 * `typeCode` 是英文 code，两个都给：展示用中文，过滤参数用英文。
 *
 * 写法与 `api/knowledge.ts` 一致：原生 fetch、失败抛带后端中文文案的 Error，不引第三方库。
 */

/** 题库中的一道题，对应后端 `QuestionView`。 */
export interface QuestionItem {
  id: number;
  /** 稳定业务编码（j1 / fe-vue-01），幂等种子按它 MERGE */
  code: string;
  jobCode: string;
  /** 知识点编码，与 data/jobs.ts 的 knowledgePoints.id 同一口径 */
  kp: string | null;
  /** 中文题型，可直接展示 */
  type: string | null;
  /** 英文题型 code，做过滤参数用 */
  typeCode: string | null;
  difficulty: string | null;
  text: string;
  reference: string | null;
  followUps: string[];
}

/** 从失败响应里挖出后端的 `detail` / `message` 文案（拿不到就退回状态码）。 */
async function readError(response: Response): Promise<string> {
  try {
    const text = await response.text();
    if (text) {
      try {
        const body = JSON.parse(text) as { error?: string; detail?: string; message?: string };
        // Spring 的 ResponseStatusException 落成 ProblemDetail，中文原因在 detail 里
        const message = body.detail ?? body.message ?? body.error;
        if (message) return message;
      } catch {
        return text.slice(0, 200);
      }
    }
  } catch {
    // 读 body 失败就退回状态码
  }
  return `HTTP ${response.status}`;
}

/**
 * 取某岗位的题库。
 *
 * @param jobCode 岗位编码（java / frontend）
 * @param opts.type 题型，可选；英文 code 或中文标签都认
 * @param opts.limit 条数，后端默认 20、上限 100
 * @param opts.offset 跳过条数，配合 limit 分页
 */
export async function fetchQuestions(
  jobCode: string,
  opts?: { type?: string; limit?: number; offset?: number },
): Promise<QuestionItem[]> {
  const params = new URLSearchParams({ jobCode });
  if (opts?.type) params.set("type", opts.type);
  if (opts?.limit != null) params.set("limit", String(opts.limit));
  if (opts?.offset != null) params.set("offset", String(opts.offset));

  let response: Response;
  try {
    response = await fetch(`/api/questions?${params.toString()}`);
  } catch (e) {
    throw new Error(e instanceof Error ? e.message : "无法连接后端");
  }
  if (!response.ok) throw new Error(await readError(response));
  return (await response.json()) as QuestionItem[];
}
