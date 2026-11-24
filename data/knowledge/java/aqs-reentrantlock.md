# AQS 与 ReentrantLock
## AQS 核心结构
面试官常问：AQS是什么？核心state、CLH队列作用是什么？
答题要点：AQS（AbstractQueuedSynchronizer）是JUC锁的底层框架，基于volatile int state同步状态 + FIFO双向CLH等待队列。state代表资源：0=无锁，>0代表持有锁重入次数；CLH队列存放阻塞等待锁的线程，线程被包装成Node节点，自旋、park阻塞。
常见误区：AQS不是锁，是同步器框架；ReentrantLock、CountDownLatch、Semaphore底层都是AQS；state是volatile变量，保障可见性。

## 独占锁获取与释放流程
面试官常问：ReentrantLock非公平锁获取锁的完整流程？
答题要点：非公平锁上来先CAS抢state=0，抢到直接成功；抢不到调用acquire，tryAcquire尝试重入；失败则入队，自旋判断前驱节点，满足条件再抢锁，抢不到就LockSupport.park阻塞。释放锁：state减去重入次数，state归0后唤醒后继节点。
常见误区：park之后线程进入WAITING，不是BLOCKED；非公平锁不是完全无序，只是刚释放锁的时候，新来线程可以插队，队列里的线程依旧排队。

## 公平锁 vs 非公平锁
面试官常问：ReentrantLock公平锁和非公平锁区别，性能差异？
答题要点：公平锁：获取锁前先检查队列有没有等待线程，有就直接入队，不插队，严格FIFO；非公平锁允许新来线程CAS插队，减少线程唤醒切换开销，吞吐量更高，默认是非公平锁。公平锁减少饥饿，但是上下文切换更多，性能更低。
常见误区：公平锁不能保证绝对公平，只是遵守排队规则；非公平锁不会无限饥饿，队列里的节点一定会被唤醒。

## AQS共享模式与条件队列
面试官常问：AQS独占和共享模式区别？Condition条件队列原理？
答题要点：独占：资源只能一个线程拿（ReentrantLock）；共享：多个线程同时获取（Semaphore、CountDownLatch）。Condition是AQS的条件等待队列，await把线程从同步队列挪到条件队列，signal唤醒挪回CLH队列，不能直接竞争锁。
常见误区：Condition必须在持有锁的时候调用await；signal不会立刻拿到锁，只是放回同步队列排队。
