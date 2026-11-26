<template>
  <div class="flex flex-col gap-3">
    <Button
      type="nav"
      :class="[
        'flex items-center gap-2 px-4 py-2 transition-colors',
        enabled ? 'text-[#ff8fc0]' : 'text-white',
      ]"
      @click="toggleEnabled"
    >
      <HeartLiquid
        :value="average"
        :negative="negativePeak"
        :size="18"
        :wave="waveEnabled"
        :class="{ 'affection-heartbeat': heartbeatEnabled }"
        :style="heartbeatStyle"
      />
      <!-- 顶栏按钮显示当前档位（初识/平淡/熟络…），无数据时回落标题 -->
      <h3 class="m-0 hidden text-lg font-bold xl:block">
        {{ tier ? TIER_LABELS[tier] : "面试印象" }}
      </h3>
    </Button>

    <!-- 日程式弹窗：全屏遮罩 + 居中窗口（双雷达，宽屏 880px / 窄屏近全宽） -->
    <Teleport to="body">
      <Transition
        enter-active-class="transition-all duration-300 cubic-bezier(0.2, 0.8, 0.2, 1)"
        leave-active-class="transition-all duration-300 cubic-bezier(0.2, 0.8, 0.2, 1)"
        enter-from-class="opacity-0"
        leave-to-class="opacity-0"
      >
        <div
          v-if="enabled"
          class="fixed inset-0 z-[1100] flex items-center justify-center bg-black/50 backdrop-blur-sm"
          @click.self="close"
        >
          <Transition
            enter-active-class="transition-all duration-300 cubic-bezier(0.2, 0.8, 0.2, 1)"
            leave-active-class="transition-all duration-200 cubic-bezier(0.6, -0.28, 0.74, 0.05)"
            enter-from-class="opacity-0 scale-95 translate-y-2"
            leave-to-class="opacity-0 scale-95 translate-y-2"
          >
            <div
              v-if="enabled"
              class="relative flex max-h-[85dvh] flex-col overflow-hidden rounded-2xl border border-white/10 shadow-[0_8px_32px_rgba(0,0,0,0.4)]"
              :class="narrow ? 'w-[95vw]' : 'w-168'"
            >
              <!-- Header bar -->
              <div
                class="flex shrink-0 items-center justify-between border-b border-white/10 bg-[#12121c]/90 px-4 py-2.5 backdrop-blur-xl"
              >
                <div class="flex items-center gap-2">
                  <!-- 面板内爱心常跳；心跳开关只控制顶栏按钮 -->
                  <HeartLiquid
                    :value="average"
                    :negative="negativePeak"
                    :size="17"
                    :wave="waveEnabled"
                    class="affection-heartbeat shrink-0"
                    :style="heartbeatStyle"
                  />
                  <h3 class="text-sm font-semibold tracking-wide text-white">面试印象</h3>
                </div>
                <button
                  class="rounded-full p-1.5 text-white/50 transition-colors hover:bg-white/10 hover:text-white"
                  @click="close"
                >
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M6 18L18 6M6 6l12 12"
                    />
                  </svg>
                </button>
              </div>

              <!-- Content area with glass styling -->
              <div
                class="min-h-0 flex-1 scrollbar-thin [scrollbar-color:var(--accent-color)_transparent] overflow-y-auto rounded-b-2xl bg-[#12121c]/75 p-4 text-white backdrop-blur-[20px]"
              >
                <!-- 只显示当前面试官；切换时旧内容淡出、新内容淡入（雷达子组件随 key 重挂载） -->
                <Transition name="affection-role" mode="out-in">
                  <div :key="persona?.code ?? 'none'">
                    <template v-if="affection">
                      <!-- 面试官头部：头像位 + 名字 + 平均值大数字 + 档位徽章 -->
                      <div class="flex items-center gap-2.5">
                        <div
                          class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-white/15 bg-white/5 text-base font-bold"
                        >
                          {{ personaInitial }}
                        </div>
                        <div class="min-w-0 flex-1">
                          <div class="truncate text-base font-semibold">
                            {{ persona?.name ?? "—" }}
                          </div>
                          <div class="text-[11px] text-white/50">综合印象</div>
                        </div>
                        <div class="flex shrink-0 flex-col items-end gap-1">
                          <span
                            class="text-2xl leading-none font-bold tabular-nums"
                            :style="{ color: tierColor }"
                          >
                            {{ average ?? "—" }}
                          </span>
                          <span
                            class="affection-tier-badge"
                            :class="{
                              'affection-tier-pulse': tier === 'overflow',
                              'affection-tier-alert': negativePeak > 60,
                            }"
                            :style="tierBadgeStyle"
                          >
                            {{ tier ? TIER_LABELS[tier] : "" }}
                          </span>
                        </div>
                      </div>

                      <!-- 当前负面观感：由负面六维派生（强度 > 30 的维度），暗红系芯片 -->
                      <div
                        v-if="negativeChips.length > 0"
                        class="mt-2.5 flex flex-wrap items-center gap-1.5"
                      >
                        <span class="flex items-center gap-1 text-[11px] text-white/50">
                          <CloudRain :size="11" />
                          当前负面观感
                        </span>
                        <span
                          v-for="chip in negativeChips"
                          :key="chip.key"
                          class="affection-neg-chip"
                        >
                          {{ NEG_LABELS[chip.key] }} {{ chip.value }}
                        </span>
                      </div>

                      <!-- 距下一档进度 -->
                      <div
                        v-if="nextTierInfo"
                        class="mt-1.5 flex items-center justify-end gap-2 text-[11px] text-white/50"
                      >
                        <div class="h-1 w-20 overflow-hidden rounded-full bg-white/10">
                          <div
                            class="h-full rounded-full transition-[width] duration-700"
                            :style="{ width: `${nextTierInfo.progress}%`, background: tierColor }"
                          ></div>
                        </div>
                        <span>
                          距「{{ TIER_LABELS[nextTierInfo.key] }}」还差 {{ nextTierInfo.points }} 点
                        </span>
                      </div>
                      <div
                        v-else-if="tier === 'overflow'"
                        class="mt-2 text-right text-xs"
                        :style="{ color: tierColor }"
                      >
                        已给出强烈推荐
                      </div>

                      <!-- 双雷达：左正面印象 / 右负面观感；窄屏上下堆叠（窗口内容区可滚动） -->
                      <div class="mt-1 flex flex-col gap-1 md:flex-row md:gap-3">
                        <AffectionRadar
                          title="正面印象"
                          :values="affectionValues"
                          :labels="affectionLabels"
                          :descs="affectionDescs"
                          :palette="affectionPalette"
                        />
                        <AffectionRadar
                          v-if="negative"
                          title="负面观感"
                          :values="negativeValues"
                          :labels="negativeLabels"
                          :descs="negativeDescs"
                          :palette="negativePalette"
                        />
                      </div>

                      <!-- 最近一次评估变化（好感增量 + 负面增量并列；负面向增暗红、向减青绿=消解） -->
                      <div
                        v-if="recentChange"
                        class="mt-2.5 rounded-xl border border-white/10 bg-white/5 p-2.5"
                      >
                        <div class="mb-1 text-[11px] font-semibold text-white/60">最近变化</div>
                        <div class="flex flex-wrap gap-1.5">
                          <span
                            v-for="entry in recentChange.entries"
                            :key="`pos-${entry.key}`"
                            class="affection-delta-chip"
                            :class="entry.delta > 0 ? 'affection-delta-up' : 'affection-delta-down'"
                          >
                            {{ DIM_LABELS[entry.key] }}
                            {{ entry.delta > 0 ? "+" : "" }}{{ entry.delta }}
                          </span>
                          <span
                            v-for="entry in recentChange.negEntries"
                            :key="`neg-${entry.key}`"
                            class="affection-delta-chip"
                            :class="entry.delta > 0 ? 'affection-neg-up' : 'affection-neg-down'"
                          >
                            {{ NEG_LABELS[entry.key] }}
                            {{ entry.delta > 0 ? "+" : "" }}{{ entry.delta }}
                          </span>
                        </div>
                        <div
                          v-if="recentChange.reason"
                          class="mt-1 text-[11px] leading-relaxed text-white/50"
                        >
                          {{ recentChange.reason }}
                        </div>
                      </div>
                    </template>

                    <div v-else class="py-8 text-center text-sm text-white/40">暂无数据</div>

                    <!-- 面试印象介绍（可折叠） -->
                    <div class="mt-3 border-t border-white/10 pt-2.5">
                      <button
                        class="flex w-full cursor-pointer items-center justify-between border-none bg-transparent p-0 text-xs text-white/60 transition-colors hover:text-white"
                        @click="introOpen = !introOpen"
                      >
                        <span>关于面试印象</span>
                        <ChevronDown
                          :size="13"
                          class="transition-transform duration-200"
                          :class="{ 'rotate-180': introOpen }"
                        />
                      </button>
                      <Transition
                        enter-active-class="transition-all duration-200 ease-out"
                        leave-active-class="transition-all duration-150 ease-in"
                        enter-from-class="opacity-0 -translate-y-1"
                        leave-to-class="opacity-0 -translate-y-1"
                      >
                        <div
                          v-if="introOpen"
                          class="mt-2 flex flex-col gap-1 text-[11px] leading-relaxed text-white/50"
                        >
                          <div v-for="dim in dimensions" :key="`desc-${dim.key}`">
                            · <span class="text-white/70">{{ DIM_LABELS[dim.key] }}</span
                            >：{{ DIM_DESCS[dim.key] }}
                          </div>
                          <div v-for="dim in negDimensions" :key="`neg-desc-${dim.key}`">
                            ·
                            <span class="text-white/70">{{ NEG_LABELS[dim.key] }}</span
                            >：{{ NEG_DESCS[dim.key] }}
                          </div>
                          <div class="mt-1">
                            面试印象由 AI 每 3 条回答评估一次，记在「本场面试 + 当前面试官」下，不需要手动操作。
                          </div>
                          <div>新开一场面试会从「待观察」重新开始，不继承上一场。</div>
                          <div>
                            正面六维越高代表面试官越认可；负面观感是面试官当下的疑虑，回答到点上会自行消解。
                          </div>
                          <div>
                            任一维度的负面观感过强，都会影响面试官追问的方向与语气。
                          </div>
                        </div>
                      </Transition>
                    </div>
                  </div>
                </Transition>
              </div>
            </div>
          </Transition>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { ChevronDown, CloudRain } from "@lucide/vue";
import Button from "../base/widget/Button.vue";
import AffectionRadar from "./AffectionRadar.vue";
import type { RadarPalette } from "./AffectionRadar.vue";
import HeartLiquid from "./HeartLiquid.vue";
import {
  impressionAverage,
  impressionStore,
  negativePeak,
  type ImpressionVector,
  type NegativeVector,
} from "../../store/impressionStore";
import { findInterviewer } from "../../store/interviewers";
import { store } from "../../store/appStore";

/** 心跳/波浪动画（当前常开；接入高级设置后由设置项控制） */
const heartbeatEnabled = ref(true);
const waveEnabled = ref(true);

const enabled = ref(false);
const introOpen = ref(false);

function toggleEnabled() {
  enabled.value = !enabled.value;
}
function close() {
  enabled.value = false;
}

/** 窄屏判定（原 uiStore.isNarrowScreen）：窗口宽 < 768 时面板近全宽 */
const narrow = ref(typeof window !== "undefined" && window.innerWidth < 768);
const onResize = () => {
  narrow.value = window.innerWidth < 768;
};
onMounted(() => window.addEventListener("resize", onResize));
onUnmounted(() => window.removeEventListener("resize", onResize));

const persona = computed(() => findInterviewer(store.session.personaId));
/** 头像位没有图片资源时用名字首字做占位（原 personas 的 avatar 字段已随合并删除） */
const personaInitial = computed(() => persona.value?.name?.slice(0, 1) ?? "");
/** 未评估前面板显示「暂无数据」，不要把全 0 画成雷达 */
const affection = computed(() => (impressionStore.evaluated ? impressionStore.dims : null));
const negative = computed(() => (impressionStore.evaluated ? impressionStore.negDims : null));

type DimKey = keyof ImpressionVector;
const dimensions: { key: DimKey }[] = [
  { key: "professionalism" },
  { key: "clarity" },
  { key: "logic" },
  { key: "depth" },
  { key: "credibility" },
  { key: "potential" },
];
type NegDimKey = keyof NegativeVector;
const negDimensions: { key: NegDimKey }[] = [
  { key: "skepticism" },
  { key: "confusion" },
  { key: "impatience" },
  { key: "disappointment" },
  { key: "wariness" },
  { key: "reservation" },
];

// ── 文案：面试官真正在判断的六个方向 + 六种负面观感 ──
const DIM_LABELS: Record<DimKey, string> = {
  professionalism: "专业性",
  clarity: "表达力",
  logic: "逻辑性",
  depth: "深度",
  credibility: "可信度",
  potential: "潜力",
};
const DIM_DESCS: Record<DimKey, string> = {
  professionalism: "基础是否扎实，概念、术语与结论是否准确",
  clarity: "能否讲清楚，结构是否清楚，有没有把控好时长",
  logic: "拆解问题的顺序、因果与取舍是否站得住",
  depth: "是否讲到原理、边界与权衡，而不是只背结论",
  credibility: "经历与回答是否自洽，不夸大，不会就说不会",
  potential: "学习能力、反思能力与可塑性",
};
const NEG_LABELS: Record<NegDimKey, string> = {
  skepticism: "质疑",
  confusion: "困惑",
  impatience: "不耐",
  disappointment: "失望",
  wariness: "戒备",
  reservation: "顾虑",
};
const NEG_DESCS: Record<NegDimKey, string> = {
  skepticism: "技术结论明显站不住，面试官想当场反驳",
  confusion: "面试官听不懂你的思路，需要反复追问",
  impatience: "绕圈、跑题、抓不住重点",
  disappointment: "该会的基础题没答出来，低于预期",
  wariness: "像在背题、套模板、经历注水",
  reservation: "面试官在犹豫要不要让你过，亮点不够或有硬伤",
};
const TIER_LABELS = {
  estranged: "明确否决",
  acquainted: "待观察",
  plain: "偏弱",
  familiar: "一般",
  deep: "合格",
  blazing: "良好",
  overflow: "强烈推荐",
} as const;

// ── 雷达图输入（labels/descs 随界面语言重算；values 数组重建驱动子组件补间） ──
const affectionValues = computed<number[]>(() => {
  const a = affection.value;
  if (!a) return [0, 0, 0, 0, 0, 0];
  return [a.professionalism, a.clarity, a.logic, a.depth, a.credibility, a.potential];
});
const negativeValues = computed<number[]>(() => {
  const n = negative.value;
  if (!n) return [0, 0, 0, 0, 0, 0];
  return [n.skepticism, n.confusion, n.impatience, n.disappointment, n.wariness, n.reservation];
});
const affectionLabels = computed(() => dimensions.map((d) => DIM_LABELS[d.key]));
const affectionDescs = computed(() => dimensions.map((d) => DIM_DESCS[d.key]));
const negativeLabels = computed(() => negDimensions.map((d) => NEG_LABELS[d.key]));
const negativeDescs = computed(() => negDimensions.map((d) => NEG_DESCS[d.key]));

/** 正面印象雷达：粉色系 */
const affectionPalette: RadarPalette = {
  gradientFrom: "#ff5c8a",
  gradientTo: "#ff9ec7",
  stroke: "#ff5c8a",
  vertex: "#ff9ec7",
  vertexOverflow: "#ffd7e8",
  halo: "rgba(255, 92, 138, 0.35)",
  glow: "rgba(255, 92, 138, 0.9)",
  cold: "#7fc4ff",
};
/** 负面观感雷达：暗红/玫红/暗紫系 */
const negativePalette: RadarPalette = {
  gradientFrom: "#e0416e",
  gradientTo: "#7c5cbf",
  stroke: "#e0416e",
  vertex: "#e88aa8",
  vertexOverflow: "#ffc4d6",
  halo: "rgba(224, 65, 110, 0.4)",
  glow: "rgba(224, 65, 110, 0.9)",
  cold: "#7fc4ff",
};

// ── 平均值与档位 ─────────────────────────────────────
const average = computed(() => impressionAverage.value);

const TIER_ORDER = [
  "estranged",
  "acquainted",
  "plain",
  "familiar",
  "deep",
  "blazing",
  "overflow",
] as const;
type TierKey = (typeof TIER_ORDER)[number];
/** 各档位下限（综合印象 0-100）：明确否决 0-19 / 待观察 20-39 / 偏弱 40-54 / 一般 55-69 / 合格 70-79 / 良好 80-89 / 强烈推荐 90-100 */
const TIER_FLOOR: Record<TierKey, number> = {
  estranged: 0,
  acquainted: 20,
  plain: 40,
  familiar: 55,
  deep: 70,
  blazing: 80,
  overflow: 90,
};
const TIER_COLORS: Record<TierKey, string> = {
  estranged: "#7fc4ff",
  acquainted: "#8ec5ff",
  plain: "#b8c4d6",
  familiar: "#ffd479",
  deep: "#ff9ec7",
  blazing: "#ff5c8a",
  overflow: "#ff3d71",
};

const tier = computed<TierKey | null>(() => {
  const v = average.value;
  if (v === null) return null;
  if (v < 20) return "estranged";
  if (v < 40) return "acquainted";
  if (v < 55) return "plain";
  if (v < 70) return "familiar";
  if (v < 80) return "deep";
  if (v < 90) return "blazing";
  return "overflow";
});
const tierColor = computed(() => (tier.value ? TIER_COLORS[tier.value] : "#ff9ec7"));
const tierBadgeStyle = computed(() => {
  const c = tierColor.value;
  return { color: c, borderColor: `${c}80`, background: `${c}1f` };
});

// ── 爱心心跳：心动周期随综合印象加快（无数据 1.8s → 满分 0.7s） ──
const heartbeatStyle = computed(() => {
  const v = average.value;
  const duration = v === null ? 1.8 : Math.min(2.2, Math.max(0.7, 1.9 - v / 125));
  return { "--heartbeat-duration": `${duration.toFixed(2)}s` };
});

/** 距下一档的点数与本档内进度（满溢/无数据时为 null） */
const nextTierInfo = computed((): { key: TierKey; points: number; progress: number } | null => {
  const v = average.value;
  const t = tier.value;
  if (v === null || t === null || t === "overflow") return null;
  const nextKey = TIER_ORDER[TIER_ORDER.indexOf(t) + 1];
  const need = TIER_FLOOR[nextKey];
  const start = TIER_FLOOR[t];
  const progress = Math.min(100, Math.max(0, ((v - start) / (need - start)) * 100));
  return { key: nextKey, points: Math.max(0, need - v), progress };
});

// ── 当前负面观感 chips：强度 > 30 的负面维度 ──
const negativeChips = computed(() => {
  const n = negative.value;
  if (!n) return [];
  return negDimensions
    .filter((d) => n[d.key] > 30)
    .map((d) => ({ key: d.key, value: Math.round(n[d.key]) }));
});

// ── 最近一次评估变化（正面增量 + 负面增量） ──
const recentChange = computed(() => {
  const c = impressionStore.lastChange;
  if (!c) return null;
  const entries = dimensions
    .filter((d) => typeof c.deltas[d.key] === "number" && c.deltas[d.key] !== 0)
    .map((d) => ({ key: d.key, delta: c.deltas[d.key] as number }));
  const negEntries = negDimensions
    .filter((d) => typeof c.negativeDeltas[d.key] === "number" && c.negativeDeltas[d.key] !== 0)
    .map((d) => ({ key: d.key, delta: c.negativeDeltas[d.key] as number }));
  if (entries.length === 0 && negEntries.length === 0 && !c.reason) return null;
  return { entries, negEntries, reason: c.reason };
});
</script>

<style scoped>
/* 爱心心跳：经典「怦-怦」双跳节奏，周期由 --heartbeat-duration 内联控制（随好感加快） */
.affection-heartbeat {
  transform-box: fill-box;
  transform-origin: center;
  animation: affection-heartbeat var(--heartbeat-duration, 1.8s) ease-in-out infinite;
}
@keyframes affection-heartbeat {
  0%,
  100% {
    transform: scale(1);
  }
  14% {
    transform: scale(1.25);
  }
  28% {
    transform: scale(1);
  }
  42% {
    transform: scale(1.16);
  }
  60% {
    transform: scale(1);
  }
}

/* 切换面试官：旧内容轻微上浮淡出，新内容自下淡入 */
.affection-role-enter-active,
.affection-role-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}
.affection-role-enter-from {
  opacity: 0;
  transform: translateY(4px);
}
.affection-role-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* 档位徽章：pill，颜色由档位内联样式决定；满溢档粉色呼吸光晕，负面峰值高时红色警示环 */
.affection-tier-badge {
  padding: 1px 8px;
  border-radius: 9999px;
  font-size: 11px;
  line-height: 1.5;
  border: 1px solid;
  white-space: nowrap;
}
.affection-tier-pulse {
  animation: affection-tier-glow 1.6s ease-in-out infinite;
}
@keyframes affection-tier-glow {
  0%,
  100% {
    box-shadow: 0 0 2px rgba(255, 61, 113, 0.3);
  }
  50% {
    box-shadow: 0 0 10px rgba(255, 61, 113, 0.7);
  }
}
.affection-tier-alert {
  animation: affection-tier-alert 1.2s ease-in-out infinite;
}
@keyframes affection-tier-alert {
  0%,
  100% {
    box-shadow: 0 0 2px rgba(255, 77, 109, 0.35);
  }
  50% {
    box-shadow: 0 0 12px rgba(255, 77, 109, 0.85);
  }
}

/* 最近变化的增量小芯片：好感正粉负蓝；负面向增暗红、向减青绿（消解） */
.affection-delta-chip {
  padding: 1px 8px;
  border-radius: 9999px;
  font-size: 11px;
  line-height: 1.5;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  white-space: nowrap;
}
.affection-delta-up {
  color: #ff9ec7;
}
.affection-delta-down {
  color: #7fc4ff;
}
.affection-neg-up {
  color: #f06292;
}
.affection-neg-down {
  color: #8fd6a8;
}

/* 「当前负面情绪」芯片：暗红/玫红系，与好感正增量粉色芯片区分 */
.affection-neg-chip {
  padding: 1px 8px;
  border-radius: 9999px;
  font-size: 11px;
  line-height: 1.5;
  color: #f5a8c4;
  background: rgba(224, 80, 126, 0.14);
  border: 1px solid rgba(224, 80, 126, 0.35);
  white-space: nowrap;
}
</style>
