# ConcurrentHashMap 原理
## JDK7 和 JDK8 结构差异
面试官常问：JDK7和JDK8的ConcurrentHashMap底层结构有什么改动？
答题要点：JDK7：分段锁Segment，每个Segment是独立小HashTable，分段加锁，降低锁粒度；JDK8抛弃Segment，改成数组+链表/红黑树，**锁粒度降到Node节点**，用synchronized锁头节点，配合CAS。
常见误区：JDK8 CHM不再是分段锁；很多人还沿用老答案说Segment；JDK8的synchronized是偏向锁、轻量级锁，性能很好。

## JDK8 读写流程
面试官常问：JDK8 ConcurrentHashMap put流程？get为什么不加锁？
答题要点：put：计算hash定位桶，桶为空直接CAS插入；桶不为空，synchronized锁住头节点，遍历链表/红黑树插入；达到阈值链表转红黑树。get：volatile读Node的val，volatile保证可见性，不加锁，读到最新值。
常见误区：get不是完全无锁，依靠volatile可见性；put的时候不是锁整个数组，只锁当前桶头节点。

## 扩容、迁移机制
面试官常问：ConcurrentHashMap扩容是怎么做的？多线程协助迁移？
答题要点：扩容是多线程协助迁移，一个线程触发扩容之后，其他线程put/get的时候发现桶正在迁移，就帮忙一起搬运数据，减少单线程扩容阻塞。扩容标记forwarding node标记已经迁移完的桶。
常见误区：扩容不会阻塞全部读写；get遇到forward节点，会直接去新数组查询；put遇到forward，会加入协助迁移。

## 常见误区与API注意点
面试官常问：ConcurrentHashMap是不是线程安全就可以随便用？哪些操作不是原子？
答题要点：单个put、get是线程安全；但是复合操作，比如`if(!map.containsKey(k)) map.put(k,v)`，不是原子，会并发覆盖，需要自己加锁或者computeIfAbsent。
常见误区：误以为CHM所有操作都是原子；`size()`不是实时精准值，是累加估算，需要精确计数用mappingCount。
