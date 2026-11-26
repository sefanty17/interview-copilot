# SpringBoot 自动配置
## @EnableAutoConfiguration 原理
面试官常问：SpringBoot自动配置核心注解原理？
答题要点：@EnableAutoConfiguration导入AutoConfigurationImportSelector，这个selector读取META-INF/spring.factories文件，拿到候选自动配置类，再根据@Conditional条件注解，判断是否满足条件，满足就注册Bean。
常见误区：启动的时候加载所有配置类；不是，全部候选加载之后，条件注解过滤，不满足条件直接跳过。

## 条件注解 @Conditional
面试官常问：常用条件注解：ConditionalOnClass、ConditionalOnMissingBean作用？
答题要点：@ConditionalOnClass：classpath存在指定类才生效；@ConditionalOnMissingBean：容器里面不存在这个Bean，才注册；@ConditionalOnProperty：配置文件存在指定属性才生效。这是自动配置按需装配的核心。
常见误区：自动配置Bean一定会注册；只要条件不满足，配置类不会生效；我们自己@Bean优先级高于自动配置。

## starter 起步依赖
面试官常问：starter是什么？和普通jar包区别？
答题要点：starter是依赖聚合包，里面不写业务代码，只管理依赖版本；真正的自动配置代码在autoconfigure包。比如spring-boot-starter-web，聚合tomcat、springmvc等依赖，自动装配web容器、DispatcherServlet。
常见误区：starter里面包含自动配置代码；自动配置代码在autoconfigure，starter只是pom依赖集合。

## 自定义starter思路
面试官常问：手写一个自定义starter步骤？
答题要点：1. 新建autoconfigure模块，写配置类加条件注解；2. 新建starter模块，依赖autoconfigure；3. autoconfigure里META-INF/spring.factories注册自动配置类；4. 业务项目引入starter，自动装配。
常见误区：直接把配置类写在starter模块；规范分开starter和autoconfigure，方便解耦。
