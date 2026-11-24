# MyBatis 原理与缓存
## 核心组件
面试官常问：MyBatis核心组件：SqlSessionFactory、SqlSession、Mapper、Executor？
答题要点：SqlSessionFactoryBuilder读取xml/注解配置，构建SqlSessionFactory；SqlSessionFactory生产SqlSession；SqlSession是会话，和数据库交互；Mapper接口是代理，没有实现类；Executor是执行器，SIMPLE、REUSE、BATCH三种执行器。
常见误区：Mapper接口有实现类；MyBatis用JDK动态代理生成Mapper代理对象。

## 一级缓存
面试官常问：MyBatis一级缓存是什么？作用范围？什么时候失效？
答题要点：一级缓存默认开启，SqlSession级别缓存，同一个SqlSession，相同查询，第一次查库，第二次读缓存。失效场景：insert/update/delete、手动clearCache、不同SqlSession。
常见误区：一级缓存是跨会话共享；只在当前SqlSession内有效；会话关闭缓存清空。

## 二级缓存
面试官常问：二级缓存范围？开启条件？失效规则？
答题要点：二级缓存是Mapper命名空间级别，跨SqlSession共享。开启：全局settings开启cacheEnabled，Mapper.xml加<cache>标签，实体类需要序列化。增删改之后，清空当前namespace的二级缓存。
常见误区：二级缓存默认开启；默认关闭；二级缓存是namespace隔离，A mapper修改不会清空B mapper缓存。

## 执行流程与参数绑定
面试官常问：MyBatis执行SQL流程，#{}和${}区别？
答题要点：#{}预编译，参数占位符，防止SQL注入；${}直接字符串替换，有注入风险，仅适合动态表名、排序字段。流程：解析Mapper方法，生成MappedStatement，参数绑定，获取连接，执行SQL，结果集映射。
常见误区：#{}可以用来动态表名；#{}是占位符，不能替换表名结构；${}无法防止注入。
