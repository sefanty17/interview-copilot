import type { SceneInfo } from "../types/domain";

/**
 * 场景（面试间背景）。图片取自原版 lingchat 的 data/game_data/backgrounds/：
 *   占卜摊2.webp -> booth.webp、夜晚.webp -> night.webp、白天.webp -> day.webp
 * （改成 ASCII 文件名避免 URL 编码问题，文件放在 frontend/public/backgrounds/）
 * 分类表里的「室内 / 室外」落在 location_type；category 为子文件夹，均为根目录。
 */
export const scenes: SceneInfo[] = [
  {
    id: "sc-01",
    scene_name: "占卜摊2",
    scene_description: "原版默认场景",
    background: "/backgrounds/booth.webp",
    category: null,
    source: "game",
    location_type: "indoor",
    favored: false,
  },
  {
    id: "sc-02",
    scene_name: "夜晚",
    scene_description: "夜色下的场景",
    background: "/backgrounds/night.webp",
    category: null,
    source: "game",
    location_type: "outdoor",
    favored: false,
  },
  {
    id: "sc-03",
    scene_name: "白天",
    scene_description: "白天场景",
    background: "/backgrounds/day.webp",
    category: null,
    source: "game",
    location_type: "outdoor",
    favored: false,
  },
];
