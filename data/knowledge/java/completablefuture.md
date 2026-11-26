# CompletableFuture 异步编排
## 基础回调与异步执行
面试官常问：CompletableFuture和Future有什么区别？
答题要点：Future只能阻塞get拿结果，没有回调；CompletableFuture实现Future+CompletionStage，支持链式回调、非阻塞编排，任务完成自动触发回调，不用阻塞轮询。可以supplyAsync（有返回值）、runAsync（无返回）。
常见误区：supplyAsync默认线程池是ForkJoinPool.commonPool；IO密集型任务不要用默认池，ForkJoin适合CPU密集。

## 编排API：thenApply / thenCombine / anyOf / allOf
面试官常问：thenApply、thenCombine、allOf、anyOf分别适合什么场景？
答题要点：thenApply：上一个任务完成，转换结果；thenCombine：两个独立任务都完成，合并结果；allOf：所有任务全部完成才继续；anyOf：任意一个任务完成就继续。thenApply是串行，thenCombine是并行。
常见误区：thenApply会继承上一个任务的线程；如果是thenApplyAsync，才会重新提交线程；anyOf拿到第一个完成的结果，其他任务不会自动取消。

## 异常处理
面试官常问：CompletableFuture异常怎么捕获？exceptionally、handle、whenComplete区别？
答题要点：exceptionally：只捕获异常，异常时返回兜底值；whenComplete：正常、异常都会执行，不能修改返回值；handle：正常和异常都进入回调，可以重新返回结果，功能最强。
常见误区：异常如果不捕获，get的时候会抛出ExecutionException；异常不会自动中断其他并行任务。

## 线程池选型坑
面试官常问：CompletableFuture异步任务，IO密集为什么不推荐默认ForkJoin池？
答题要点：ForkJoinPool是work-stealing，核心线程数默认CPU核心数，IO阻塞会占满工作线程，新任务排队。IO密集异步任务，必须传入自定义线程池，隔离IO和CPU任务。
常见误区：所有异步任务都直接用supplyAsync不传线程池；线上大量IO异步容易把commonPool打满，整个服务异步卡死。
