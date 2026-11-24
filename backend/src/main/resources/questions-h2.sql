-- 面试工坊 · 题库种子（H2）
--
-- 自由面试的题目来源（issue #11）：面试官先问候选人想面什么岗位，
-- 再按 interview_session.free_job_code 从这里随机抽题。
-- 幂等：每次启动都会执行，MERGE ... KEY(code) 保证重复执行不产生重复行。
-- 换 MySQL：MERGE INTO → INSERT ... ON DUPLICATE KEY UPDATE（或维持启动期一次性导入）。
--
-- 题型（type）取值固定四种，与 QuestionType 枚举的 label 逐字一致：
--   技术知识 / 项目深挖 / 场景题 / 行为题
-- 课程指标 ① 要求「至少两种岗位，每岗位四类题型」。
-- 当前配额：java 与 frontend 各 20 题，每类题型各 5 题（见下方分组注释）。
-- 加题只需追加一行并保证 code 唯一；sort_order 用于稳定展示顺序。

-- ============================ Java 后端 · 技术知识（5）============================

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j1', 'java', 'jvm', '技术知识', '中等', '说说 JVM 的内存区域划分，哪些区域会 OOM，哪些不会。', '程序计数器、虚拟机栈、本地方法栈、堆、方法区/元空间；PC 不会 OOM；栈会 StackOverflowError；堆和元空间会 OOM。', '["栈溢出和堆溢出分别怎么排查？","元空间为什么会取代永久代？"]', 1);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j2', 'java', 'concurrent', '技术知识', '较难', 'ConcurrentHashMap 在 JDK 1.8 里为什么放弃分段锁？它怎么保证线程安全？', 'CAS + synchronized 锁单个桶；减少锁粒度、避免 Segment 的内存与并发瓶颈；扩容协助 transfer。', '["size() 为什么是弱一致的？","扩容时并发读写怎么处理？"]', 2);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j7', 'java', 'collection', '技术知识', '中等', 'HashMap 的扩容机制是怎样的？为什么链表长度到 8 才转红黑树，而且还要判断数组容量？', '负载因子 0.75，size 超过 capacity×0.75 时扩容为两倍；扩容用 hash & oldCap 把桶拆成低位链（下标不变）和高位链（下标 +oldCap），不重算 hash。树化阈值 8 来自泊松分布下该桶达到 8 个元素的概率极低（约千万分之六）；退化阈值 6 是缓冲，避免频繁来回转换；容量 <64 时优先扩容而不是树化，因为小表扩容比树化更划算。', '["为什么容量必须是 2 的幂？","JDK7 头插法与 JDK8 尾插法分别带来什么问题？"]', 3);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j8', 'java', 'jvm', '技术知识', '较难', 'G1 和 CMS 的回收思路有什么本质区别？为什么大堆场景更推荐 G1？', 'CMS 是标记-清除，按新生代/老年代物理分区，老年代回收与年轻代独立，容易产生碎片且并发模式失败时会退化成单线程 Full GC。G1 把堆切成等大 Region（逻辑分代），按「回收收益」优先收集（Garbage First），用 Remembered Set 处理跨 Region 引用，可预测停顿模型（-XX:MaxGCPauseMillis），碎片问题靠 Region 复制解决。大堆下 CMS 的碎片与并发失败代价被放大，G1 的停顿可预测性更关键。', '["G1 的 Remembered Set 是干什么的？","ZGC 相比 G1 在停顿上强在哪？"]', 4);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j9', 'java', 'mysql', '技术知识', '中等', 'MySQL 的索引为什么用 B+ 树，而不是 B 树、红黑树或哈希表？', 'B+ 树非叶子节点只存键不存数据，同样磁盘页能放更多键，树更矮（3~4 层可撑千万级）；叶子节点用链表相连，范围查询和排序只需顺序扫描叶子，不用回到上层。B 树每个节点都存数据，扇出小、树更高，范围查询要中序遍历。红黑树是二叉树，高度是对数级的「深」，每次节点访问几乎都是一次随机 IO，磁盘场景不可接受。哈希表等值查询 O(1) 但不支持范围查询和排序，也无法做最左前缀匹配，所以 InnoDB 只在自适应哈希等局部场景用。', '["为什么说 B+ 树的树高决定了 IO 次数？","聚簇索引和二级索引的叶子分别存什么？"]', 5);

-- ============================ Java 后端 · 项目深挖（5）============================

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j3', 'java', 'spring', '项目深挖', '中等', '你项目里 Spring Bean 的循环依赖是怎么解决的？什么情况下解决不了？', '三级缓存；构造器注入、prototype、@Async 代理等场景解决不了。', '["三级缓存分别存什么？","为什么要 early reference 而不是直接放成品？"]', 6);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j10', 'java', 'spring', '项目深挖', '较难', '你说项目里用了全局异常处理，具体怎么分层的？业务异常和系统异常的返回有什么不同？', '分层：@RestControllerAdvice 捕获 Controller 层抛出的异常；业务异常（如 BusinessException 带 code）映射成 4xx + 业务码，前端能按 code 做具体提示；参数校验异常（MethodArgumentNotValidException）统一整理成字段级错误列表；系统异常（NPE、SQL 异常）映射成 5xx 但**不把堆栈和 SQL 原文返回给前端**，只返回一个 traceId，完整堆栈打到日志里按 traceId 关联。这样既不泄露内部实现，又能靠 traceId 定位线上问题。', '["为什么不能把 SQL 异常原文返回给前端？","traceId 是怎么在全链路串起来的？"]', 7);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j11', 'java', 'redis', '项目深挖', '较难', '你提到用延迟双删解决缓存不一致，那个延迟时间是怎么定的？为什么不用先删缓存再更新数据库？', '延迟时间靠压测观测：JMeter 模拟并发读+更新，统计「查库→拿到结果→写回 Redis」整条链路的最大耗时，取覆盖绝大多数请求的值（如 100ms 内为主、极少数 250ms，则设 300ms）。不用「先删缓存再更新库」是因为：删完缓存到更新库完成之间若有读请求，会把旧库值重新写回缓存，窗口更长且更容易被放大。先更新库再删缓存（Cache Aside）把不一致窗口压缩到「删除动作本身」的耗时。也知道强一致场景要靠 Canal 监听 binlog 异步更新。', '["Thread.sleep 阻塞为什么不能用在生产？","Canal 方案的一致性边界在哪？"]', 8);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j12', 'java', 'micro', '项目深挖', '中等', '你项目里有哪些地方是「 knowingly 用了最终一致」的？出问题时怎么兜底？', '典型是 MQ 异步处理订单通知：落库与发消息不在同一事务，可能「库成功但消息没发」或「消息发了但重复消费」。兜底三件套：一是本地消息表（业务写库时同事务写一条待发表，定时任务补发）；二是消费端幂等（业务唯一键去重表或状态机只允许单向流转）；三是对账任务定期比对业务表和消息状态，不一致就补偿。要说明取舍：这套换来的是吞吐与解耦，代价是「一段时间内读到的可能不是最新」。', '["本地消息表和事务消息的区别？","对账任务的执行频率怎么定？"]', 9);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j13', 'java', 'mq', '项目深挖', '较难', '你项目里 RabbitMQ 的交换机、队列、路由键是怎么设计的？消息积压过吗？', '设计上按业务域拆直连/主题交换机：订单类用 topic 交换机按 order.created / order.cancelled 路由，通知类用 direct 绑定具体队列；每个队列配死信交换机承接重试耗尽的消息，避免无限重投。积压排查顺序：先看消费者是否宕机或消费逻辑变慢（日志里单条耗时），再看队列是否有大量 unacked（消费者拿了不 ack），再看是否生产者突增。应急是扩消费者并提高 prefetch，根治是优化消费逻辑、把非核心动作再异步一层。', '["prefetch 设太大会有什么问题？","死信队列里的消息最后谁来处理？"]', 10);

-- ============================ Java 后端 · 场景题（5）============================

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j4', 'java', 'mysql', '场景题', '较难', '一张订单表 5000 万行，查询最近 7 天某个用户的订单很慢，你怎么优化？', '联合索引 (user_id, created_at)、覆盖索引、避免函数运算、分页优化、冷热分离/归档。', '["为什么不建议直接加 created_at 单列索引？","深分页怎么处理？"]', 11);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j5', 'java', 'redis', '场景题', '中等', '缓存穿透、击穿、雪崩分别是什么？你项目里怎么处理的？', '穿透：空值缓存/布隆过滤器；击穿：互斥锁/逻辑过期；雪崩：过期时间打散、多级缓存、降级。', '["布隆过滤器有误判怎么办？","逻辑过期和互斥锁怎么选？"]', 12);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j14', 'java', 'concurrent', '场景题', '较难', '线上接口偶发卡死，你怀疑是死锁，怎么确认？确认后怎么修？', '确认：jstack 导出线程栈，搜「Found one Java-level deadlock」，看互相持有的锁与等待链；也可以用 jconsole 的检测死锁，或 arthas 的 thread -b 直接找阻塞其他线程的那一个。要注意区分 JVM 死锁和数据库死锁：数据库死锁看 information_schema.innodb_trx 与 SHOW ENGINE INNODB STATUS 的 LATEST DETECTED DEADLOCK。修：统一加锁顺序（按 id 排序后再锁）、缩小锁范围、用 tryLock 带超时并在失败时降级或重试，避免无限等待。', '["为什么统一加锁顺序能破环？","tryLock 超时后业务该怎么处理？"]', 13);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j15', 'java', 'jvm', '场景题', '较难', '线上服务 CPU 打满，你怎么一步步定位到具体代码？', 'top 找高 CPU 进程 → top -Hp 找高 CPU 线程 → printf 把线程号转十六进制 → jstack 导出后按 nid=0x… 搜到对应线程栈，看它卡在哪个方法。同时用 jstat -gcutil 看是否 GC 线程占满：若 GC 时间占比极高，说明是内存问题导致的 CPU 高，转去看堆。建议间隔几秒连抓三次快照对比，同一线程持续停在同一行才是真热点。定位到后用 arthas 的 trace 看方法耗时分布。', '["如果是 GC 引起的 CPU 高，各项指标有什么特征？","arthas 的 trace 和 watch 分别解决什么问题？"]', 14);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j16', 'java', 'micro', '场景题', '较难', '设计一个支持十万并发的秒杀下单接口，讲清限流、库存扣减、超卖防护。', '分层拦截，把压力挡在 MySQL 之外：① 前端置灰+验证码防重复提交；② 网关按商品维度令牌桶限流；③ 接口层用户维度限流 + 布隆过滤器挡非法商品 ID；④ Redis 预存库存，用 Lua 脚本原子执行「判断库存>0→扣减→返回结果」，天然防超卖，扣成功生成秒杀凭证投递 MQ；⑤ MQ 异步消费落单、扣 DB 库存，数据库不直面洪峰；⑥ 兜底：死信队列重试、定时对账校正 Redis 与 DB、超时未支付回补库存。核心区分：Redis 只负责抢资格，订单持久化交给 MQ。', '["Lua 脚本为什么能保证原子性？","超时未支付回补库存会不会超卖？"]', 15);

-- ============================ Java 后端 · 行为题（5）============================

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j6', 'java', 'micro', '行为题', '中等', '讲一个你和其他同学/同事意见不一致的例子，最后怎么解决的？', 'STAR 结构；突出倾听、数据说服、复盘。', '["如果对方是你的上级，你还会坚持吗？"]', 16);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j17', 'java', 'micro', '行为题', '中等', '说一个你做过的最有技术挑战的事，难在哪，你具体做了什么？', '要讲清「难」的客观来源（数据量、并发、历史包袱、时间紧），而不是「我不熟」；再讲你拆解问题的过程：怎么定位、试过哪几条路、为什么否掉其他方案；最后给可验证的结果（耗时从多少到多少、错误率变化）。关键是让听的人相信这件事换个人做不一定能做成，而你的具体动作是决定因素。', '["如果重做一次，你会从哪里改进？","这件事里哪部分不是你做的？"]', 17);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j18', 'java', 'micro', '行为题', '中等', '线上出了故障，你是怎么排查和复盘的？举一个真实例子。', '排查讲顺序：先止血（回滚/降级/限流）再定位，避免边查边扩大影响；定位靠「最近变更」优先原则（发布、配置、数据），再看监控指标哪条先异常。复盘讲机制而非追责：时间线、根因、为什么没提前发现（监控缺口）、为什么影响面这么大（缺降级）、以及落地的改进项和责任人。要能说出具体改了什么，比如补了哪个告警、加了哪个兜底。', '["为什么说「先止血再定位」很重要？","怎么避免复盘变成走过场？"]', 18);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j19', 'java', 'micro', '行为题', '中等', '你怎么保证自己写的代码质量？举一个你主动加的质量措施。', '从三个层面答：写之前（接口契约先定、边界条件先列）、写的时候（小步提交、异常路径也想清楚、不吞异常）、写之后（补单测覆盖边界、自测走一遍真实链路）。举一个具体措施，比如「给缓存一致性那段逻辑补了并发场景的用例」「把重复的分页参数收敛成一个对象减少出错」。避免只说「我很认真」这种无法验证的话。', '["单测覆盖率你是怎么看的？","你写过最难测的代码是什么？"]', 19);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('j20', 'java', 'micro', '行为题', '中等', '如果给你一个你没接触过的技术方向，两周内要上线，你怎么安排？', '先划清「必须懂」和「可以不懂」的边界：只学能支撑当前需求的最小集，其余靠文档和求助。安排上分三段：前两天跑通官方示例建立体感；中段做一个小垂直切片打通全链路；后段补异常与边界并留出 buffer。同时明确风险上报节点——第二周末如果核心链路还不通，要提前说要调整范围，而不是硬扛到 deadline 才暴露。', '["你怎么判断自己「学够了」？","遇到文档很差的技术你会怎么办？"]', 20);

-- ============================ Web 前端 · 技术知识（5）============================

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f1', 'frontend', 'js', '技术知识', '中等', '说说事件循环里宏任务和微任务的执行顺序，并举例说明。', '同步 → 微任务清空 → 宏任务；Promise.then、queueMicrotask 是微任务；setTimeout 是宏任务。', '["async/await 的 await 后面算微任务吗？","Node 和浏览器的事件循环差异？"]', 21);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f2', 'frontend', 'vue', '技术知识', '较难', 'Vue 3 的响应式为什么用 Proxy 替代 Object.defineProperty？', '可监听新增/删除属性、数组索引、Map/Set；惰性代理；缺点是不支持 IE。', '["ref 和 reactive 的区别？","依赖收集是怎么触发的？"]', 22);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f5', 'frontend', 'security', '技术知识', '中等', 'XSS 和 CSRF 的区别是什么？分别怎么防？', 'XSS 注入脚本；CSRF 借用 cookie。转义/CSP/HttpOnly；CSRF Token/SameSite/校验 Origin。', '["CSP 能完全防住 XSS 吗？","SameSite=Lax 有什么副作用？"]', 23);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f7', 'frontend', 'ts', '技术知识', '中等', 'interface 和 type 有什么区别？什么场景下必须用 type？', '两者大多可互换。interface 支持声明合并（同名自动合并，适合扩展第三方类型）、更适合描述对象契约；type 能表达联合、交叉、映射类型、条件类型、模板字面量类型，这些场景 interface 做不到。另外 interface 报错信息通常更可读，团队里常见约定是「对象形状用 interface，其余用 type」。', '["类型声明合并有什么实际用途？","泛型约束 extends 是什么含义？"]', 24);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f8', 'frontend', 'css', '技术知识', '中等', '说说 BFC 是什么，它能解决哪些布局问题？怎么触发？', 'BFC（块级格式化上下文）是一块独立的渲染区域，内部布局不影响外部。能解决：清除浮动导致的高度塌陷、阻止相邻元素的 margin 合并、防止元素被浮动元素覆盖、避免与浮动元素重叠。触发方式：float 非 none、position 为 absolute/fixed、overflow 非 visible、display 为 flow-root/inline-block/flex/grid/table-cell。其中 display: flow-root 是专为此设计的，没有 overflow:hidden 带来的裁剪副作用。', '["margin 塌陷有哪几种情况？","flow-root 相比 overflow:hidden 好在哪？"]', 25);

-- ============================ Web 前端 · 项目深挖（5）============================

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f9', 'frontend', 'ts', '项目深挖', '中等', '你项目里 TypeScript 是怎么用的？有没有遇到过类型写不对反而拖慢开发的情况？', '要讲真实取舍：接口返回值先定类型再写组件，避免 any 扩散；用 unknown + 类型守卫处理外部数据（fetch 的 JSON 本质是 any）；用泛型约束收敛重复的请求封装。踩过的坑要具体，比如「给后端 DTO 手写了一份镜像类型，后端改字段前端没报错，运行时才炸」，后续做法是把类型放在 api 层单点维护、后端加字段时同步。避免答成「用了 TS 就很安全」。', '["unknown 和 any 的区别？","怎么防止类型定义和后端脱节？"]', 26);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f10', 'frontend', 'vue', '项目深挖', '较难', '你项目里组件之间怎么通信？状态管理是怎么选的，为什么不把状态都放全局？', '分层：父子用 props/emit；跨层用 provide/inject；真正的全局状态（用户信息、当前会话）用一个轻量 store。不把状态全放全局的理由：全局状态隐式耦合，任何组件都能改，改一处影响面不可控；而且会绕过组件的输入输出契约，单测时无法通过传参构造场景。判断标准是「这份状态的生命周期是否跨越多个不相邻的组件」，否则留在组件内部更简单。', '["什么时候不该引入状态管理库？","provide/inject 怎么做成响应式？"]', 27);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f11', 'frontend', 'js', '项目深挖', '中等', '你项目里怎么处理异步请求的取消和竞态？举一个真实场景。', '典型场景是快速切换 tab 或搜索输入：旧请求晚于新请求返回，会把新结果覆盖成旧数据。做法有两种：一是用 AbortController 取消旧请求；二是给每次请求打一个递增序号，回来时比对序号不符就丢弃。前者省流量但要注意服务端已执行的情况，后者更简单可靠。另外输入类场景要配合防抖，避免每敲一个字都发请求。', '["AbortController 取消后服务端还会执行吗？","防抖和节流分别适合什么场景？"]', 28);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f12', 'frontend', 'browser', '项目深挖', '较难', '你项目里怎么做前端路由和权限控制的？刷新后状态怎么恢复？', '路由用 history 模式，配合后端的 fallback 保证刷新不 404。权限上分「能否进入某页面」和「页面内某按钮是否可见」两层：前者在路由守卫里按角色判断并重定向，后者用指令或计算属性控制渲染。刷新恢复的关键是不要只依赖内存：把必要的标识（如当前会话 id、当前角色 code）持久化，进入页面时先拉一次后端状态再渲染，避免出现「刷新后看起来是 A 角色、实际请求是 B」的不一致。', '["前端权限控制能替代后端鉴权吗？","为什么刷新后不能只看本地存储？"]', 29);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f13', 'frontend', 'build', '项目深挖', '中等', '你项目里构建和开发环境的配置有哪些优化？热更新慢的时候怎么排查？', '优化方向：依赖预构建（Vite 的 optimizeDeps）、按需引入组件库、路由级代码分割、开启 gzip/brotli、图片走 CDN。热更新慢的排查顺序：先看是「单个模块慢」还是「整页刷新」——如果改一个小文件也整页刷新，通常是模块边界被打破（比如 barrel 文件 index.ts 全量导出，改一个会牵连整个 barrel）；再看是否有大量循环依赖；最后看是否是插件（如全量 TS 类型检查）在开发期跑。', '["为什么 barrel 文件会拖慢热更新？","开发环境和生产环境的构建目标为什么不同？"]', 30);

-- ============================ Web 前端 · 场景题（5）============================

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f3', 'frontend', 'browser', '场景题', '中等', '从输入 URL 到页面展示，中间发生了什么？尽量完整。', 'DNS、TCP/TLS、HTTP、解析 HTML/CSS、构建 DOM/CSSOM、布局、绘制、合成。', '["哪些阶段会阻塞渲染？","如何优化首屏？"]', 31);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f4', 'frontend', 'perf', '场景题', '较难', '一个长列表页面滚动卡顿，你会怎么定位和优化？', 'Performance 面板、长任务、虚拟列表、避免 layout thrashing、will-change、分片渲染。', '["虚拟列表的坑有哪些？","怎么衡量优化效果？"]', 32);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f14', 'frontend', 'browser', '场景题', '中等', '用户反馈「改了代码发版后还是看到旧页面」，你怎么排查和解决？', '先确认是哪一层缓存：① 强缓存命中（Cache-Control max-age 太长）——看响应头是否根本没有发请求；② 协商缓存仍返回 304，说明 ETag/Last-Modified 没变（可能是文件内容没变或代理层缓存）；③ Service Worker 缓存了旧资源。根治方案是静态资源文件名带内容哈希 + 长强缓存，HTML 用 no-cache 走协商，保证发版后 index.html 一定拿到新的、进而引用新的哈希文件。', '["为什么 index.html 不能设长强缓存？","Service Worker 的缓存怎么更新？"]', 33);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f15', 'frontend', 'vue', '场景题', '较难', '一个页面要渲染 1 万条数据，滚动很卡，你怎么优化？', '先判断瓶颈是「渲染量」还是「响应式开销」：如果只是 DOM 太多，用虚拟列表只渲染可视区；如果数据放在深响应式对象里，改一个字段会触发大量依赖，改用 shallowRef/shallowReactive 或 Object.freeze。再配合分页或无限滚动减少一次性数据量、用 v-memo 减少不必要重渲染、把高频更新拆成独立小组件缩小 diff 范围。优化后要用 Performance 面板对比长任务数量与滚动帧率来说明收益。', '["虚拟列表怎么处理不定高列表？","为什么大列表不要放在一个 reactive 里？"]', 34);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f16', 'frontend', 'security', '场景题', '较难', '产品要支持用户粘贴富文本展示，你怎么在「能显示样式」和「不被 XSS」之间取舍？', '核心原则是不信任任何用户输入：先用白名单 sanitize（如 DOMPurify 配置允许的标签和属性），而不是黑名单过滤 script；禁止内联事件属性（onerror/onclick）和 javascript: 协议；富文本渲染容器加 CSP 限制 script-src，即使有漏网也难执行；图片等外链资源走代理或白名单域，防止通过 src 探测。要说明取舍：白名单会损失一部分样式表现力，这是安全成本，需要和产品对齐哪些标签必须支持。', '["为什么黑名单过滤不可靠？","CSP 能完全防住 XSS 吗？"]', 35);

-- ============================ Web 前端 · 行为题（5）============================

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f6', 'frontend', 'build', '行为题', '中等', '你做过哪些工程化的事情？带来了什么可量化的收益？', '构建提速、规范、CI、监控；要有数字。', '["如果团队不愿意配合，你怎么推动？"]', 36);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f17', 'frontend', 'build', '行为题', '中等', '讲一次你和设计师（产品）在方案上分歧较大的经历，最后怎么落地的？', '重点讲怎么把主观分歧转成可判断的标准：先对齐目标用户和场景，再给出两个可验证的方案与实现成本，用小范围原型或数据验证，最后记录决策依据与复盘。避免答成「我说服了他」或者「我妥协了」，要体现的是「找到了双方都能接受的判断依据」。', '["如果对方职位比你高，你怎么表达不同意见？","方案上线后效果不好，你怎么复盘？"]', 37);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f18', 'frontend', 'perf', '行为题', '中等', '线上页面出现白屏，你怎么排查？举一个你处理过的例子。', '顺序：先确认影响范围（全量还是部分用户、哪个版本开始），再止血（回滚或切流量），然后定位。技术手段：看控制台报错（资源加载失败 / JS 执行异常）、看 Network 是否有 404 或 CORS、看是否是新版本引入的兼容问题（如用了不支持的语法）、看是否是接口超时导致首屏数据拿不到而渲染阻塞。复盘要落到机制：补错误监控上报、加资源加载失败的兜底、发版前加灰度。', '["怎么区分「白屏」是资源问题还是 JS 报错？","前端错误监控怎么接入？"]', 38);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f19', 'frontend', 'js', '行为题', '中等', '你怎么保证自己写的代码质量？举一个你主动加的质量措施。', '分三层：写之前先定接口契约和边界条件；写的时候小步提交、异常路径也想清楚、不吞错误；写之后补测试覆盖边界并自测真实链路。举一个具体例子，比如「给列表的竞态加了请求序号校验，并用两个 tab 快速切换验证过」「把散落各处的日期格式化收敛成一个工具函数」。关键是可验证，而不是「我很仔细」。', '["你写过最难测的代码是什么？","代码评审时你最关注什么？"]', 39);

MERGE INTO interview_question (code, job_code, kp, type, difficulty, text, reference, follow_ups, sort_order) KEY(code) VALUES ('f20', 'frontend', 'vue', '行为题', '中等', '如果让你接手一个没有文档、代码风格混乱的前端项目，你第一步做什么？', '先跑起来、再摸清边界：第一步让项目在本地能跑通（依赖、环境变量、启动命令），第二步找出「入口与数据流」（路由表、状态管理、请求封装在哪），第三步挑一条最核心的用户路径读一遍完整链路。不要一上来就重构——先把理解写成文档（哪怕只是一份 README 补丁），再按「改到哪、补到哪」的方式逐步统一风格，避免大范围重构引入新问题。', '["你怎么说服团队允许你重构？","没有测试的情况下怎么保证不乱改坏？"]', 40);
