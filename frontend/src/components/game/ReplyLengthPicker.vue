<template>
  <!--
    回复长度档位选择（issue #16 / A4）。

    为什么放在房间右上角工具条而不是设置页：原版这个档位是「聊着聊着随手切」的
    会话级控件（`chat.rs:58-75`），不是全局配置；设置页那种需要跳页再回来的位置
    会让它失去「随时调」的意义。
  -->
  <div ref="rootRef" class="relative inline-flex">
    <button
      type="button"
      class="nav-btn"
      :aria-expanded="open"
      :title="currentHint"
      @click="toggle"
    >
      <span class="text-white/60">档位</span>
      <span class="text-[#79d9ff]">{{ currentLabel }}</span>
      <ChevronDown
        :size="14"
        class="transition-transform duration-200"
        :class="{ 'rotate-180': open }"
      />
    </button>

    <Transition name="mode-pop">
      <div v-if="open" class="mode-menu" @click.stop>
        <button
          v-for="item in options"
          :key="item.value"
          type="button"
          class="mode-item"
          :class="{ 'mode-item-active': item.value === modelValue }"
          :disabled="busy"
          @click="pick(item.value)"
        >
          <span class="flex w-full items-baseline gap-2">
            <span class="font-bold">{{ item.label }}</span>
            <span class="text-[11px] text-white/45">{{ item.hint }}</span>
            <span v-if="item.value === modelValue" class="ml-auto text-[#79d9ff]">✓</span>
          </span>
        </button>

        <p class="mode-foot">
          剧本面试的节奏由剧本写死，档位只在自由对话生效。
        </p>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
/**
 * 回复长度档位选择器（A4）。
 *
 * <p>职责边界：只管「选哪一档 + 把改动通知父组件」，不发请求、不管错误提示——
 * 请求失败的表现（弹窗还是静默）由房间页决定，避免组件里掺业务决策。
 */
import { computed, onMounted, onUnmounted, ref } from "vue";
import { ChevronDown } from "@lucide/vue";
import { REPLY_LENGTH_LABELS, type ReplyLengthMode } from "../../api/chat";

const props = withDefaults(
  defineProps<{
    /** 当前档位；null = 还没读到（试玩会话或后端不可用） */
    modelValue: ReplyLengthMode | null;
    /** 切换请求进行中，期间禁用点击避免连点 */
    busy?: boolean;
  }>(),
  { busy: false },
);

const emit = defineEmits<{
  (e: "update:modelValue", value: ReplyLengthMode): void;
}>();

const open = ref(false);
const rootRef = ref<HTMLElement | null>(null);

const options = computed(() =>
  (Object.keys(REPLY_LENGTH_LABELS) as ReplyLengthMode[]).map((value) => ({
    value,
    ...REPLY_LENGTH_LABELS[value],
  })),
);

/** 没读到档位时按默认值展示，避免右上角出现「档位 ?」这种半成品状态。 */
const currentMode = computed<ReplyLengthMode>(() => props.modelValue ?? "normal");
const currentLabel = computed(() => REPLY_LENGTH_LABELS[currentMode.value].label);
const currentHint = computed(() => REPLY_LENGTH_LABELS[currentMode.value].hint);

function toggle(): void {
  open.value = !open.value;
}

function pick(mode: ReplyLengthMode): void {
  open.value = false;
  // 选中的就是当前值时不重复发请求
  if (mode !== props.modelValue) emit("update:modelValue", mode);
}

/** 点空白处收起，避免菜单挡住下面的「结束面试」按钮。 */
function onDocumentClick(event: MouseEvent): void {
  if (open.value && !rootRef.value?.contains(event.target as Node)) {
    open.value = false;
  }
}
onMounted(() => document.addEventListener("click", onDocumentClick));
onUnmounted(() => document.removeEventListener("click", onDocumentClick));
</script>

<style scoped>
/* 与设置页弹层同一套观感：深色半透明 + 模糊 + 白 10% 描边 */
.mode-menu {
  position: absolute;
  top: calc(100% + 0.25rem);
  right: 0;
  z-index: 50;
  min-width: 15rem;
  overflow: hidden;
  border-radius: 0.75rem;
  padding: 0.375rem;
  background: rgba(15, 15, 15, 0.88);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6);
}

.mode-item {
  display: flex;
  width: 100%;
  align-items: center;
  border-radius: 0.5rem;
  padding: 0.5rem 0.625rem;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.875rem;
  transition:
    background-color 0.15s,
    color 0.15s;
}
.mode-item:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}
.mode-item:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
.mode-item-active {
  color: #fff;
  background: rgba(121, 217, 255, 0.12);
}

.mode-foot {
  margin-top: 0.375rem;
  padding: 0.375rem 0.625rem 0.25rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 0.6875rem;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.4);
}

.mode-pop-enter-active,
.mode-pop-leave-active {
  transition: all 0.18s ease;
}
.mode-pop-enter-from,
.mode-pop-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.96);
}
</style>
