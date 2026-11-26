# MySQL MVCC、undo、redo、binlog
## MVCC原理
面试官常问：MVCC是什么？解决什么问题？
答题要点：MVCC多版本并发控制，InnoDB实现，读写不阻塞，解决RC、RR隔离级别下的快照读。通过undo log生成数据历史版本，read view判断版本可见性。快照读：select不加锁；当前读：select ... for update、update delete，加锁。
常见误区：MVCC可以解决幻读；RR级别MVCC快照读不能完全解决幻读，需要间隙锁。

## undo log 回滚日志
面试官常问：undo log作用？保存什么内容？
答题要点：undo log保存数据修改前的快照，两个作用：事务回滚、MVCC版本链。修改一行，生成undo版本，多个版本串联成版本链。事务提交不会立刻删除undo，没有快照引用才会purge清理。
常见误区：undo log是用来持久化数据；redo是持久化，undo是版本和回滚。

## redo log 重做日志
面试官常问：redo log WAL预写日志原理？
答题要点：WAL：修改先写redo log buffer，刷盘到redo log，再写内存页；不是直接刷磁盘数据页。宕机重启，回放redo log，把已经提交事务的数据恢复到磁盘，保证崩溃安全。redo是循环日志，checkpoint标记已经落盘的位置。
常见误区：redo log记录SQL语句；redo记录是物理页修改，不是SQL；binlog记录逻辑SQL。

## binlog 归档日志
面试官常问：binlog和redo log区别？
答题要点：redo是InnoDB引擎层，崩溃恢复；binlog是MySQL服务层，主从复制、数据归档。binlog三种格式：statement记录SQL，row记录行变更，mixed混合。两阶段提交：redo prepare、写binlog、redo commit。
常见误区：binlog可以用来崩溃恢复；崩溃恢复只用redo；binlog是逻辑日志，引擎无关。
