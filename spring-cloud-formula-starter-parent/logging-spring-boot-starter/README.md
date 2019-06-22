# Logging Spring Boot Starter
SpringBoot对于日志系统的抽象可以很大程度上解决多种日志实现的共存问题，但是我们搭建微服务平台时遇到以下问题：
* 多模块配置格式差异太大。
* logback.xml 难理解，配置错了容易出现日志丢失或重复打。
* 多环境的差异难实现。
* 中间件日志不能独立。
* 日志清理不及时。

为解决以上问题，我们参考SpringBoot LoggingSystem, Sofa Common tools, Logback官方文档的实现，设计统一的日志配置模型，
通过编程的方式配置日志系统。可以实现在不引入新的除了新的日志API的情况下解决以上问题。

## 快速开始
##### 引入依赖
````xml
<dependency>
    <groupId>com.baidu.formula</groupId>
    <artifactId>logging-spring-boot-starter</artifactId>
</dependency>
````
##### 增加配置
受益于SpringBoot的Environment抽象，这个配置可以放在本地，配置中心，或者命令行参数等来源。
配置形式如下
````yaml
formula:
  logging:
    spaces:
      [spacename]: # 此处写日志空间名称，仅小写+中划线
        loggers: [ "com.baidu.dept.logger1", "com.baidu.dept.logger2" ]
        spec:
          file: # 默认的文件名[spacename].log, 此处配置了可以修改
          path: 日志目录，默认为WORKDIR/log/, 业务日志无需修改
          max-history: 168 # 7*24=168，默认保存7天
          max-file-size: 1GB # 默认1GB， 单个日志文件1GB， 默认值一般不需要改
          total-size-cap: 30GB # 默认30GB, 这一组日志文件最大不超过30G， 一般不需要改
          addtivity: false # 默认false, 日志无需重复打印。
          threshold: null # 默认null, 表示不增加阈过滤器。
````

## 高级特性

### 禁用此组件
````yaml
formula.logging.enabled: false
````

### 日期格式
````yaml
logging.pattern.dateformat: yyyy-MM-dd HH:mm:ss.SSS
````

### 日志级别格式
````yaml
logging.pattern.level: %5p
````

### 共用logback-spring.yml
可以继续使用，但是不能再配置console的appender, 否则会打印两次。
