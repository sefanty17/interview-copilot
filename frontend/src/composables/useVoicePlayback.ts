/**
 * 房间台词的语音播放：单一 `<audio>` 元素复用，切句时 stop 再 play。
 *
 * `voiceUrl` 是后端 `/api/speech/audio/<指纹>.<ext>`，指纹由「文本 + 情绪 + 音色」算出，
 * 内容不可变、可缓存——同一句重播会命中后端缓存、拿到同一个 URL，不会重复合成。
 *
 * 只做「有声叠加」：播不播都不影响房间推进节奏，合成失败 / 浏览器拒绝自动播放时
 * 表现只是「这一句没声音」，页面不能因此报错。
 *
 * <p>{@code onPlayingChange} 用来把「正在播」这一事实同步给 ASR 门控
 * （issue #10 的「按麦克风时朗读立即停止」）：不通知的话，用户按下麦克风时
 * 房间还在播面试官的声音，会被录进自己的输入里。
 */

export interface VoicePlayback {
  /** 播放一段语音；已在播的先停掉（同一时刻只有一句的声音） */
  play(url: string): void;
  /** 停止并回到开头 */
  stop(): void;
  /** 当前是否正在出声（门控用） */
  isPlaying(): boolean;
  /** 组件卸载时释放（断开 src，让浏览器回收解码资源） */
  dispose(): void;
}

/**
 * @param onPlayingChange 播放状态变化的回调：开始播放传 true，停止 / 播完 / 失败传 false。
 *                        房间页用它调 {@code setVoicePlaying}，让 ASR 门控知道此刻不该录音。
 */
export function useVoicePlayback(onPlayingChange?: (playing: boolean) => void): VoicePlayback {
  // 懒建：模块被 import 时不碰 DOM（测试环境 / 首屏都没必要先建一个 audio）
  let el: HTMLAudioElement | null = null;
  /** 当前是否在播。用变量而不是查 el.paused：元素还没建时也该给出确定的 false */
  let playing = false;

  function notify(next: boolean): void {
    if (playing === next) return;
    playing = next;
    onPlayingChange?.(next);
  }

  function element(): HTMLAudioElement {
    if (!el) {
      el = new Audio();
      el.preload = "auto";
      // 自然播完 / 被外部暂停 / 出错，都要把状态复位，否则门控会一直以为还在播
      el.addEventListener("ended", () => notify(false));
      el.addEventListener("pause", () => notify(false));
      el.addEventListener("error", () => notify(false));
    }
    return el;
  }

  function stop(): void {
    if (!el) {
      notify(false);
      return;
    }
    // 已经停了就别再 pause：pause() 会再触发一次 pause 事件 → notify(false)，
    // 虽然 notify 有「值没变就跳过」的守卫，但多一次无意义的门控更新会连带
    // 重新计算 ASR 可用性，按下麦克风时那一下会多一次抖动。
    if (!el.paused) {
      el.pause();
    }
    el.currentTime = 0;
    notify(false);
  }

  return {
    play(url: string): void {
      stop();
      const audio = element();
      audio.src = url;
      // 自动播放策略 / 404 / 解码失败都会 reject，语音出不来绝不能炸页面
      void audio
        .play()
        .then(() => notify(true))
        .catch((error: unknown) => {
          notify(false);
          console.warn("[voice] 播放失败", url, error);
        });
    },
    stop,
    isPlaying: () => playing,
    dispose(): void {
      if (!el) return;
      stop();
      el.src = "";
      el = null;
    },
  };
}
