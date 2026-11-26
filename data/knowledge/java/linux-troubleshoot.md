# Linux 线上排查 top / jstack / arthas
## top 命令解读
面试官常问：top里 %CPU、load average、%id 含义？load高一定CPU高吗？
答题要点：%CPU是进程CPU使用率；load average是1/5/15分钟系统就绪队列长度，等待CPU的任务数。load高，可能是CPU饱和，也可能是IO阻塞（D状态进程），进程等磁盘，不消耗CPU。
常见误区：load高=CPU占用高；IO等待进程也会拉高load，%id空闲CPU很高。

## jstack 线程栈排查
面试官常问：jstack用来排查什么？死锁、CPU高、线程阻塞？
答题要点：jstack用于抓取JVM**瞬时线程快照**，拿到所有线程调用栈、线程状态、锁信息。典型排查场景：CPU飙升（死循环、密集计算）、线程阻塞/线程池打满、死锁、请求卡死、长时间RT超时。CPU高排查固定流程：top找到Java进程PID → top -Hp PID找到高CPU线程LWP（十进制）→ printf转十六进制 → jstack过滤nid=0xxxx的堆栈；建议间隔3s连续抓3次快照对比，同一个线程一直卡在同一行代码才是真实问题。jstack `-l` 参数会打印锁信息，能识别AQS锁形成的死锁，输出末尾会自动标记Found deadlock。线程状态重点区分：RUNNABLE（正在执行/等待CPU）、BLOCKED（等待监视器锁）、WAITING（park/wait，主动等待）。
常见误区：jstack是性能采样工具，能统计CPU占用；它只是快照，不是采样，单次快照不能直接判定CPU热点；RUNNABLE不等于在跑业务代码，也可能卡在socket、IO；抓到一次堆栈就下结论，很多瞬时阻塞是正常现象，必须多次快照比对；jstack看不到GC线程内部细节，GC风暴优先搭配jstat。
