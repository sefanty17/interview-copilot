# ThreadLocal 原理与内存泄漏
## ThreadLocal 底层结构
面试官常问：ThreadLocal存的数据放在哪里？不是ThreadLocal对象本身？
答题要点：每个Thread对象内部持有ThreadLocalMap，key是ThreadLocal实例（弱引用WeakReference），value是业务数据。ThreadLocal本身只是存取入口，数据存在线程的ThreadLocalMap里面，不是ThreadLocal对象。
常见误区：很多人以为ThreadLocal是一个全局map，所有线程共用；是每个线程独立一份map，互相隔离。

## 弱引用设计目的
面试官常问：ThreadLocalMap的key为什么设计成弱引用？
答题要点：如果是强引用：ThreadLocal引用被业务代码释放之后，线程还存活，map里的key强引用还持有ThreadLocal，ThreadLocal对象永远无法回收，造成泄漏。弱引用的设计：外部ThreadLocal引用置null之后，GC可以回收ThreadLocal对象。
常见误区：弱引用能彻底避免泄漏；弱引用只能回收key，**value是强引用**，只要线程存活，value还在，依然泄漏。

## 内存泄漏完整原因
面试官常问：ThreadLocal内存泄漏的完整链路是什么？
答题要点：线程是线程池复用线程，线程长期存活；ThreadLocal对象外部引用置空，key被GC回收变成null；但是Entry的value是强引用，线程还持有Entry，value无法被GC，value一直堆积，就是泄漏。
常见误区：只记“key是弱引用所以不会泄漏”，忽略value强引用；线程池场景是泄漏高发场景。

## 正确使用规范
面试官常问：ThreadLocal怎么避免内存泄漏？什么时候调用remove？
答题要点：核心规范：**用完必须remove**。try-finally模板：set放在try，finally里面remove。在线程池、异步线程场景强制remove；临时一次性线程可以不用，但不推荐。
常见误区：觉得线程结束自动回收，线程池线程不会结束，会反复复用；不要用static大对象长期存ThreadLocal。
