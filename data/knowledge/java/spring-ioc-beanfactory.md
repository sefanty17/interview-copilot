# Spring IoC 与 BeanFactory
## BeanFactory 与 ApplicationContext区别
面试官常问：BeanFactory和ApplicationContext是什么关系？区别？
答题要点：BeanFactory是顶层IoC容器根接口，定义Bean基础获取、管理方法，**延迟初始化**，getBean的时候才实例化Bean；ApplicationContext是BeanFactory的子接口，是高级容器，内置更多能力：AOP、国际化、事件发布、资源加载、自动预实例化单例Bean。
常见误区：ApplicationContext是替代BeanFactory；ApplicationContext组合持有BeanFactory，是增强包装；BeanFactory本身不支持注解、xml自动解析。

## IoC 控制反转
面试官常问：IoC控制反转到底反转了什么？依赖注入DI是什么？
答题要点：传统开发：自己new对象，主动创建依赖；IoC反转：对象的创建、依赖装配交给容器，业务代码只声明依赖，容器注入。DI是IoC的实现方式：构造注入、setter注入、字段注入。
常见误区：IoC就是DI；DI是实现手段，IoC是思想；字段注入@Autowired不推荐，单元测试难以注入mock。

## Bean定义、BeanDefinition
面试官常问：BeanDefinition是什么？作用？
答题要点：BeanDefinition是Bean的元数据，保存Bean的class、scope、初始化方法、销毁方法、依赖信息。Spring解析xml/注解之后，先把配置转成BeanDefinition，注册到注册表，之后容器根据BeanDefinition实例化Bean。
常见误区：BeanDefinition就是Bean实例；BeanDefinition是模板，实例是根据模板new出来的。

## 作用域 scope
面试官常问：Spring Bean有哪些scope？singleton和prototype区别？
答题要点：singleton：单例，容器中只创建一个实例，容器启动时默认预实例化；prototype：原型，每次getBean都新建对象；request、session、application是web专属作用域。
常见误区：singleton是JVM全局单例；是**容器内单例**，多个容器之间互不隔离；prototype不会被Spring管理销毁，需要自己清理。
