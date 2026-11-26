<template>
  <div class="relative h-screen w-full overflow-hidden select-none"
    @mousemove="handleMouseMove" @mouseleave="handleMouseLeave">
    <!-- ① 背景层 -->
    <div ref="bgRef"
      class="absolute top-0 left-[-10%] h-full w-[120%] bg-cover bg-center will-change-transform"
      :style="{ backgroundImage: `url(${bgUrl})` }" />
    <!-- ② 立绘层 -->
    <img ref="charRef" :src="alonaUrl" alt="面试官立绘" draggable="false"
      class="pointer-events-none absolute top-1/2 left-1/2 z-[3] max-h-full max-w-full -translate-x-1/2 -translate-y-1/2 will-change-transform select-none"
      @contextmenu.prevent />
    <!-- ③ 文字 Logo（右上角） -->
    <div class="absolute top-0 right-0 z-[6] p-[2vw] text-right transition-transform duration-300 hover:scale-105">
      <div class="font-['Maoken_Assorted_Sans',sans-serif] text-[clamp(28px,3.2vw,56px)] leading-none text-white drop-shadow-[0_4px_8px_rgba(0,0,0,0.3)]">面试工坊</div>
      <div class="mt-1 text-sm tracking-[0.3em] text-white/70">INTERVIEW LAB</div>
    </div>
    <!-- ④ 菜单列 -->
    <div class="pointer-events-none absolute inset-0 z-[5] flex items-center justify-start border-[6vw] border-transparent">
      <nav class="pointer-events-auto flex flex-col items-stretch">
        <Transition :name="slideName" mode="out-in">
          <div v-if="menu === 'main'" key="main" class="flex flex-col items-stretch">
            <StartItem @click="openStart">开始面试</StartItem>
            <StartItem :disabled="!hasActiveSession" @click="resume">继续面试</StartItem>
            <StartItem @click="go('/records')">面试记录</StartItem>
            <StartItem @click="go('/settings')">面试设置</StartItem>
            <StartItem @click="go('/script-editor')">剧本编辑器</StartItem>
            <StartItem @click="go('/workshop')">创意工坊</StartItem>
          </div>
          <div v-else-if="menu === 'start'" key="start" class="flex flex-col items-stretch">
            <StartItem @click="openScripts">剧本面试</StartItem>
            <StartItem @click="startFree">自由面试</StartItem>
            <StartItem @click="backToMain">返回</StartItem>
          </div>
          <!-- 岗位由面试官在房间里自然询问，不再单独设岗位选择菜单 -->
          <div v-else key="script" class="flex max-h-[74vh] flex-col items-stretch overflow-y-auto pr-2">
            <button
              v-for="s in store.scripts"
              :key="s.code"
              class="menu-script-row"
              @click="startScript(s)"
            >
              <span class="truncate">{{ s.script_name }}</span>
              <span class="menu-script-meta">
                {{ jobName(s.job_code) }} · {{ s.chapters.length }} 章{{ s.level ? ` · ${s.level}` : "" }}
              </span>
            </button>
            <p v-if="!store.scripts.length" class="mt-4 px-2 text-base text-white/50">还没有剧本，去剧本编辑器新建或导入一个。</p>
            <StartItem class="mt-6 opacity-80" @click="backToStart">返回</StartItem>
          </div>
        </Transition>
      </nav>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import StartItem from "../components/menu/StartItem.vue";
import bgUrl from "../assets/images/background2.png";
import alonaUrl from "../assets/images/alona.png";
import { hasActiveSession, startSession, store } from "../store/appStore";
import { jobs } from "../data/jobs";
import type { InterviewScript } from "../data/scripts";
import type { PersonaId } from "../types/domain";

const router = useRouter();

const bgRef = ref<HTMLElement | null>(null);
const charRef = ref<HTMLElement | null>(null);

const menu = ref<"main" | "start" | "script">("main");
const slideName = ref("slide-left");

function handleMouseMove(e: MouseEvent) {
  const x = e.clientX / window.innerWidth - 0.5;
  const y = e.clientY / window.innerHeight - 0.5;
  if (bgRef.value) {
    bgRef.value.style.transition = "transform 0.2s ease-out";
    bgRef.value.style.transform = `translate(${x * -10}px, ${y * -10}px)`;
  }
  if (charRef.value) {
    charRef.value.style.transition = "translate 0.2s ease-out";
    charRef.value.style.translate = `calc(-50% + ${x * 20}px) calc(-50% + ${y * 20}px)`;
  }
}

function handleMouseLeave() {
  if (bgRef.value) {
    bgRef.value.style.transition = "transform 0.2s ease-out";
    bgRef.value.style.transform = "translate(0px, 0px)";
  }
  if (charRef.value) {
    charRef.value.style.transition = "translate 0.2s ease-out";
    charRef.value.style.translate = "-50% -50%";
  }
}

function go(path: string) {
  router.push(path);
}

function openStart() {
  slideName.value = "slide-left";
  menu.value = "start";
}

function openScripts() {
  slideName.value = "slide-left";
  menu.value = "script";
}

/** 二级菜单回退：job / script → start */
function backToStart() {
  slideName.value = "slide-right";
  menu.value = "start";
}

function backToMain() {
  slideName.value = "slide-right";
  menu.value = "main";
}

function jobName(code?: string): string {
  return jobs.find((j) => j.id === code)?.name ?? code ?? "通用岗位";
}

/** 自由面试：不预设岗位，进房后面试官先问「你想面什么岗位」，再据此抽题 */
function startFree() {
  startSession(store.settings.jobId, store.settings.personaId, "free");
  router.push("/interview");
}

/** 剧本面试：首页直接选剧本，绑定后进房（等价设置页「剧本冒险」的开演逻辑） */
function startScript(script: InterviewScript) {
  const interviewer = (script.interviewer_code as PersonaId | undefined) ?? store.settings.personaId;
  localStorage.setItem("bondScript", script.code);
  localStorage.setItem("bondCharacter", interviewer);
  // 普通剧本入口是完整流程，清掉练习计划留下的专项，避免只被追着一个维度问
  localStorage.removeItem("bondFocus");
  startSession(script.job_code ?? store.settings.jobId, interviewer, "script");
  router.push("/interview");
}

function resume() {
  router.push("/interview?resume=1");
}
</script>

<style scoped>
.slide-left-enter-active,
.slide-left-leave-active,
.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform 0.4s cubic-bezier(0.7, 0, 0.2, 1), opacity 0.4s;
}
.slide-left-enter-from {
  transform: translateX(120%);
  opacity: 0;
}
.slide-left-leave-to {
  transform: translateX(-120%);
  opacity: 0;
}
.slide-right-enter-from {
  transform: translateX(-120%);
  opacity: 0;
}
.slide-right-leave-to {
  transform: translateX(120%);
  opacity: 0;
}

/* 剧本列表：条目比主菜单小一档，屏幕高度内可滚动 */
.menu-script-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  margin-top: 14px;
  padding: 6px 10px;
  border: none;
  border-radius: 10px;
  background: transparent;
  text-align: left;
  color: #fff;
  font-family: "Maoken Assorted Sans", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-size: clamp(20px, 2vw, 32px);
  line-height: 1.2;
  cursor: pointer;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  transition: transform 0.3s cubic-bezier(0.18, 0.89, 0.32, 1.28), background-color 0.3s;
}
.menu-script-row:hover {
  transform: translateX(6px);
  background: rgba(255, 255, 255, 0.08);
}
.menu-script-meta {
  font-size: 0.5em;
  letter-spacing: 0.04em;
  color: rgba(255, 255, 255, 0.6);
}
</style>
