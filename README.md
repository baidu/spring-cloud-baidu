# Spring Cloud Formula
基于SpringBoot兼容SpringCloud生态开发的微服务框架，是百度云CNAP(云原生微服务应用平台 Cloud-Native Application Platform)的面向客户提供的Java微服务框架设施。

Formula在官方SpringCloud功能基础上进行完善和增强，对于开发者开箱即用，对平台搭建提供关键问题的解决方案。

## 主要组件
### Spring Cloud Formula Launcher
SpringBoot, SpringCloud在不同版本有一套固定的依赖，见[Spring IO Platform](https://spring.io/projects/platform). 
不同版本也有不同的问题和特性，因此数百个应用升级SpringBoot，SpringCloud 和 starter 版本是一个无法完成的任务。
本项目可以实现服务启动时替换，实现业务项目和中间件项目分别维护。

### Spring Cloud Formula CNAP
提供开箱即用的服务治理工具集，可以高效稳定的管理线上服务。包含如下组件

##### 1. RateLimiter Spring Boot Starter
提供服务限流功能(基于[resilience4j](https://github.com/resilience4j/resilience4j)和令牌桶算法)。

##### 2. CircuitBreaker Spring Boot Starter
外部服务熔断，降级，超时，并行限制功能，来隔离故障的范围(基于[resilience4j](https://github.com/resilience4j/resilience4j)实现)

##### 3. Route Spring Boot Starter
兼容 OpenFeign，Ribbon，RestTemplate 的路由组件，支持按请求的IP，tag，路由到一个或者多个tag(按比例分配)的集群。不依赖具体的注册中心组件。

##### 4. Logging Spring Boot Starter
spring-boot-starter-logging 在配置日志时需要根据具体的日志实现，配置logback-spring.xml, log4j.properties。在企业级应用的场景会有如下问题：
* 日志配置难理解，并且很冗长。
* 各模块的日志格式很难统一。
* 不支持中间件的日志与业务日志分离。
* 不支持各环境，如测试环境和生产环境 的差异。

Logging Spring Boot Starter 基于spring-boot-starter-logging做扩展，提供统一的日志配置模型和application.yaml/properties配置，
编程的方式配置日志系统。做到与具体的日志实现解耦，相比properties/xml的配置方式可以减少90%的配置代码量。
对于嵌入式web容器, tomcat, jetty, undertow, 可以定制accesslog的存放地址，并提供自动清理功能。

##### 5. Config Client Spring Boot Starter
根据企业级配置中心的常用功能，扩展官方Spring Cloud Config Client, 提供如下功能:
* 预处理的配置表达式。支持根据环境变量的差异，加载配置之后做配置预处理，支持commons-jexl3的表达式。
* 基于定时轮询实现的配置热生效。相比Spring Cloud Bus的热更新方案更轻量。
* 中间件配置继承。Spring Boot 提供了方便的方式AutoConfiguration, 但是配置不能直接继承，在使用中需要使用者大量重复拷贝配置。
这个功能可以实现中间件使用时，相关的配置一起加载。
* config/application.yml, config/application.properties的优先加载，提供在测试时无须环境变量和启动参数即可修改应用程序配置的方式，
在测试环境编排中有较好的使用体验。

##### 6. Env Core Spring Boot Starter
提供云环境适配工具。方便更好的在云环境上使用微服务框架。现在支持百度云。

## 快速开始
### 先决条件
JDK 8+, SpringBoot 2.0.7.RELEASE, Spring Cloud Finchley.SR2

### 编译构建
```bash
./mvnw clean install -DskipTests
```

### 使用
##### 1. 引入BOM

```xml
<dependencyManagement>
 <dependencies>
    <dependency>
        <groupId>com.baidu.formula</groupId>
        <artifactId>spring-cloud-formula-parent</artifactId>
        <version>${spring-cloud-formula.version}</version>
        <type>pom</type>
        <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

##### 2. 根据需求引入依赖

```xml
<dependencies>
  <depencency>
    <groupId>com.baidu.formula</groupId>
    <artifactId>logging-spring-boot-starter</artifactId>
  </depencency>
</dependencies>
```

## 测试
```bash
./mvnw clean test
```


