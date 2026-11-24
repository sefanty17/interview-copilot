# OOM 与堆转储排查
## OOM 常见分类
面试官常问：Java OOM有哪些不同类型？分别是什么场景？
答题要点：常见：Java heap space（堆内存不足，对象太多无法GC）、Metaspace（元空间，类加载太多、动态生成类）、Direct buffer memory（直接内存，NIO ByteBuffer，不受堆限制）、unable to create new native thread（线程数量超限，栈内存耗尽）。不同OOM的原因完全不一样，不能一概而论调大-Xmx。
常见误区：遇到OOM就直接加大堆内存；很多时候是内存泄漏，加大堆只会推迟OOM，不会解决；直接内存不属于堆，-Xmx管不到它。

## 堆转储 dump 配置
面试官常问：发生OOM怎么自动导出堆快照？常用JVM参数？
答题要点：核心参数 `-XX:+HeapDumpOnOutOfMemoryError`，OOM时自动dump；`-XX:HeapDumpPath=/xxx/heap.hprof` 指定文件路径。也可以jmap手动dump：`jmap -dump:format=b,file=xxx.hprof pid`。dump文件是堆的全量快照，包含所有对象、引用链。
常见误区：线上dump非常耗IO和内存，大堆dump会卡住进程；不要随便在生产高峰手动dump；dump文件很大，需要提前预留磁盘空间。

## 堆快照分析思路
面试官常问：拿到hprof堆转储文件，分析步骤是什么？
答题要点：优先用MAT（Memory Analyzer Tool）。第一步看支配树，找出占用内存最大的对象；第二步看泄漏嫌疑人Leak Suspects，定位最长引用链；第三步区分：是正常业务流量导致对象多，还是**无效引用持有对象导致内存泄漏**；第四步看对象的引用链，找到是谁持有这个对象没有释放。
常见误区：只看对象数量，不看引用链；很多大对象是临时正常对象，不是泄漏；不要把年轻代正常GC波动当成OOM。

## 典型OOM场景根因
面试官常问：列举几个高频OOM场景，怎么定位？
答题要点：1. 集合static全局持有对象，永不清理；2. ThreadLocal没有remove，线程池复用线程，Entry一直持有；3. 动态循环生成大量类，元空间溢出；4. NIO不断分配DirectBuffer不释放。
常见误区：ThreadLocal的key是弱引用，但是value是强引用，线程不退出，value不会被回收，这是高频踩坑点。
