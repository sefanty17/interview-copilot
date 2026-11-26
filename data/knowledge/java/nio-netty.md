# NIO 与 Netty
## BIO NIO AIO区别
面试官常问：BIO、NIO、AIO模型，Java NIO三大组件？
答题要点：BIO一连接一线程；NIO同步非阻塞，Selector多路复用器、Channel、Buffer；AIO异步非阻塞，回调通知，Windows IOCP，Linux下AIO不成熟。
常见误区：NIO是异步；NIO是同步非阻塞，Selector轮询就绪事件；AIO才是异步。

## Reactor 模式
面试官常问：Reactor单线程、多线程、主从Reactor？Netty是哪一种？
答题要点：单Reactor：一个线程处理所有连接、读写；多Reactor：一个接收线程，N个IO线程；主从Reactor：主Reactor负责accept，从Reactor负责读写，Netty默认主从Reactor。
常见误区：Netty所有事件都在同一个线程；Boss线程组负责accept，Worker线程组负责IO读写。

## Netty 核心组件
面试官常问：Channel、Pipeline、Handler、EventLoop作用？
答题要点：Channel代表连接；Pipeline是责任链，一串Handler；Inbound入站，Outbound出站；EventLoop是线程，一个EventLoop绑定一个线程，串行处理一个Channel所有事件，保证线程安全。
常见误区：一个Channel会被多个EventLoop处理；Channel绑定固定EventLoop，事件串行执行，避免同步锁。

## 零拷贝
面试官常问：Netty零拷贝是什么？
答题要点：零拷贝不是没有拷贝，减少用户态内核态拷贝；FileChannel的transferTo，mmap内存映射，减少数据拷贝次数。
常见误区：零拷贝完全不拷贝；避免CPU拷贝，DMA拷贝依旧存在。
