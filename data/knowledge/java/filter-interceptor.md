# 过滤器 Filter vs 拦截器 Interceptor
## 底层规范与执行位置
面试官常问：Filter和Interceptor分别属于什么规范？执行顺序？
答题要点：Filter是Servlet规范，在Tomcat容器层，在DispatcherServlet之前；Interceptor是SpringMVC组件，在DispatcherServlet内部，Controller前后。顺序：Filter → DispatcherServlet → Interceptor preHandle → Controller → Interceptor postHandle。
常见误区：拦截器比过滤器先执行；Filter更早，离容器更近；过滤器不能拿到Spring容器Bean，拦截器可以。

## 能力边界对比
面试官常问：Filter和Interceptor各自适合做什么？
答题要点：Filter适合：编码设置、跨域、请求原始流包装、黑名单限流，容器层面通用处理；Interceptor适合：登录校验、权限校验、日志埋点、请求参数处理，能拿到Spring上下文、Controller方法信息。
常见误区：拦截器可以修改request原始body；request流只能读取一次，拦截器读取之后Controller读不到，Filter配合包装器才适合。

## 异常处理差异
面试官常问：Filter和Interceptor捕获异常的区别？
答题要点：Filter只能捕获自己链内异常；Interceptor抛出异常，会进入SpringMVC异常处理器@ControllerAdvice；Filter抛出异常不会进入Spring异常体系。
常见误区：Filter抛出的异常能被@ControllerAdvice捕获；不能，Filter在SpringMVC外面。

## 注册方式
面试官常问：SpringBoot注册Filter、Interceptor方式？
答题要点：Filter：@WebFilter注解，或者RegistrationBean注册；Interceptor：实现HandlerInterceptor，在WebMvcConfigurer的addInterceptors注册，配置拦截路径、排除路径。
常见误区：Interceptor注册之后自动生效；必须实现WebMvcConfigurer添加，否则不会注册到SpringMVC。
