-- 面试工坊 · 题库种子（H2）
--
-- 自由面试的题目来源（issue #11）：面试官先问候选人想面什么岗位，
-- 再按 interview_session.free_job_code 从这里随机抽题。
-- 幂等：每次启动都会执行，MERGE ... KEY(code) 保证重复执行不产生重复行。
-- 换 MySQL：MERGE INTO → INSERT ... ON DUPLICATE KEY UPDATE（或维持启动期一次性导入）。

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j1', 'java', 'jvm', '技术知识', '中等', '说说 JVM 的内存区域划分，哪些区域会 OOM，哪些不会。', '程序计数器、虚拟机栈、本地方法栈、堆、方法区/元空间；PC 不会 OOM；栈会 StackOverflowError；堆和元空间会 OOM。', '["栈溢出和堆溢出分别怎么排查？","元空间为什么会取代永久代？"]', 1);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j2', 'java', 'concurrent', '技术知识', '较难', 'ConcurrentHashMap 在 JDK 1.8 里为什么放弃分段锁？它怎么保证线程安全？', 'CAS + synchronized 锁单个桶；减少锁粒度、避免 Segment 的内存与并发瓶颈；扩容协助 transfer。', '["size() 为什么是弱一致的？","扩容时并发读写怎么处理？"]', 2);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j3', 'java', 'spring', '项目深挖', '中等', '你项目里 Spring Bean 的循环依赖是怎么解决的？什么情况下解决不了？', '三级缓存；构造器注入、prototype、@Async 代理等场景解决不了。', '["三级缓存分别存什么？","为什么要 early reference 而不是直接放成品？"]', 3);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j4', 'java', 'mysql', '场景题', '较难', '一张订单表 5000 万行，查询最近 7 天某个用户的订单很慢，你怎么优化？', '联合索引 (user_id, created_at)、覆盖索引、避免函数运算、分页优化、冷热分离/归档。', '["为什么不建议直接加 created_at 单列索引？","深分页怎么处理？"]', 4);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j5', 'java', 'redis', '场景题', '中等', '缓存穿透、击穿、雪崩分别是什么？你项目里怎么处理的？', '穿透：空值缓存/布隆过滤器；击穿：互斥锁/逻辑过期；雪崩：过期时间打散、多级缓存、降级。', '["布隆过滤器有误判怎么办？","逻辑过期和互斥锁怎么选？"]', 5);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j6', 'java', 'micro', '行为题', '中等', '讲一个你和其他同学/同事意见不一致的例子，最后怎么解决的？', 'STAR 结构；突出倾听、数据说服、复盘。', '["如果对方是你的上级，你还会坚持吗？"]', 6);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f1', 'frontend', 'js', '技术知识', '中等', '说说事件循环里宏任务和微任务的执行顺序，并举例说明。', '同步 → 微任务清空 → 宏任务；Promise.then、queueMicrotask 是微任务；setTimeout 是宏任务。', '["async/await 的 await 后面算微任务吗？","Node 和浏览器的事件循环差异？"]', 7);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f2', 'frontend', 'vue', '技术知识', '较难', 'Vue 3 的响应式为什么用 Proxy 替代 Object.defineProperty？', '可监听新增/删除属性、数组索引、Map/Set；惰性代理；缺点是不支持 IE。', '["ref 和 reactive 的区别？","依赖收集是怎么触发的？"]', 8);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f3', 'frontend', 'browser', '场景题', '中等', '从输入 URL 到页面展示，中间发生了什么？尽量完整。', 'DNS、TCP/TLS、HTTP、解析 HTML/CSS、构建 DOM/CSSOM、布局、绘制、合成。', '["哪些阶段会阻塞渲染？","如何优化首屏？"]', 9);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f4', 'frontend', 'perf', '场景题', '较难', '一个长列表页面滚动卡顿，你会怎么定位和优化？', 'Performance 面板、长任务、虚拟列表、避免 layout thrashing、will-change、分片渲染。', '["虚拟列表的坑有哪些？","怎么衡量优化效果？"]', 10);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f5', 'frontend', 'security', '技术知识', '中等', 'XSS 和 CSRF 的区别是什么？分别怎么防？', 'XSS 注入脚本；CSRF 借用 cookie。转义/CSP/HttpOnly；CSRF Token/SameSite/校验 Origin。', '["CSP 能完全防住 XSS 吗？","SameSite=Lax 有什么副作用？"]', 11);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f6', 'frontend', 'build', '行为题', '中等', '你做过哪些工程化的事情？带来了什么可量化的收益？', '构建提速、规范、CI、监控；要有数字。', '["如果团队不愿意配合，你怎么推动？"]', 12);
