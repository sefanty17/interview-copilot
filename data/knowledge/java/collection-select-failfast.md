# Java集合选型与fail-fast
## 常用集合底层对比
面试官常问：ArrayList、LinkedList、ArrayDeque、HashSet适用场景？
答题要点：ArrayList：数组，随机访问O(1)，尾部添加快，中间插入删除慢；LinkedList双向链表，随机访问慢，头尾插入删除快，JDK8之后不推荐优先用；ArrayDeque数组实现双端队列，性能远高于LinkedList做栈、队列；HashSet底层就是HashMap，key存元素，value占位。
常见误区：LinkedList适合频繁增删；只有在已知头尾增删，且不需要随机访问才合适；大量随机遍历场景，LinkedList性能极差。

## 线程不安全集合与并发替代
面试官常问：ArrayList线程不安全，并发替代方案？CopyOnWriteArrayList原理？
答题要点：ArrayList并发add会数组越界、数据丢失。替代：CopyOnWriteArrayList、Vector、Collections.synchronizedList。CopyOnWrite写时复制，修改的时候复制一份新数组，修改完成替换引用；读不加锁，读旧数组。
常见误区：CopyOnWrite适合写多场景；写操作要拷贝数组，写多内存开销巨大，适合读多写极少。

## fail-fast 和 fail-safe
面试官常问：fail-fast和fail-safe区别？哪些集合是fail-safe？
答题要点：fail-fast：modCount机制，修改直接抛异常（ArrayList、HashMap）；fail-safe：迭代遍历快照，不抛异常，迭代读到的是快照，不一定是最新数据（CopyOnWriteArrayList）。
常见误区：fail-safe代表实时可见；它是快照遍历，迭代期间的修改迭代器感知不到。

## 集合选型判断思路
面试官常问：给一个业务场景，怎么选集合？
答题要点：先看是否需要有序、是否允许重复、是否需要线程安全；再看访问模式：随机访问优先ArrayList；队列栈优先ArrayDeque；去重优先HashSet；需要排序TreeSet；并发读多写少CopyOnWrite。
常见误区：不管场景一律用ArrayList；需要排序还在用ArrayList自己排序；高并发写场景盲目上CopyOnWrite。
