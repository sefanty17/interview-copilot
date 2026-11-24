# CAS 与 ABA问题
## CAS原理
面试官常问：CAS是什么？底层指令是什么？
答题要点：CAS Compare And Swap，比较并交换，无锁乐观操作：预期值A，新值B，只有当前内存值等于A，才更新成B，返回成功；否则失败。底层依赖CPU硬件指令`cmpxchg`，原子指令，不需要操作系统锁。JDK里Unsafe类提供CAS方法。
常见误区：CAS不是Java语言特性，是CPU硬件原语；CAS是乐观锁，不是无等待，会循环重试。

## 三大问题：ABA、自旋开销、只能保证一个变量
面试官常问：CAS有哪些缺陷？ABA问题怎么产生？
答题要点：ABA：线程1读取值A，线程2改成B又改回A，线程1CAS认为没有变化，成功更新，但中间数据已经变动。另外两个问题：长时间自旋CAS空转，CPU飙升；只能对单个变量CAS，多个变量无法原子操作。
常见误区：ABA一定是bug；很多场景下ABA不影响业务，比如单纯计数；只有依赖“中间有没有被修改过”的场景，ABA才是问题。

## ABA解决方案
面试官常问：怎么解决ABA？AtomicStampedReference原理？
答题要点：核心思路：除了值，额外增加版本号/时间戳，每次修改版本号自增。AtomicStampedReference保存【值+stamp版本】，CAS的时候同时对比值和版本号。AtomicMarkableReference是标记位，只标记是否修改过，不记录次数。
常见误区：AtomicInteger不能解决ABA；版本号要自增，不能复用；版本号溢出可以用long，几乎不会溢出。

## CAS适用场景
面试官常问：什么时候适合用CAS，什么时候适合用锁？
答题要点：竞争少、修改频率低的场景，CAS性能好，无上下文切换；高并发激烈竞争，CAS大量自旋重试，CPU占满，这时悲观锁synchronized反而更合适。
常见误区：盲目认为CAS一定比synchronized快；高竞争场景CAS自旋会把CPU打满。
