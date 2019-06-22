# Circuitbreaker Spring Boot Starter
熔断的功能为当某个目标服务调用慢或者有大量失败时，熔断该服务的调用，对于后续调用请求，不在继续调用目标服务，直接返回，快速释放资源。后续如果目标服务情况好转则恢复调用。

circuitbreaker-spring-boot-starter为调用端熔断的SDK，提供单节点维度调用过程中服务级别的熔断功能。

基于Spring Boot框架开发，目的是为Spring Cloud项目增加熔断功能，同样在Spring Boot项目中也能正常使用。
本Starter的目前的应用场景为在Spring Cloud/Spring Boot的Web项目中引入该熔断Starter，配置熔断规则开启熔断功能。
非Spring Web项目的特性正在规划中。

**熔断维度**为：方法维度、服务维度熔断。
- 方法维度含义为可以为目标服务的具体方法配置熔断规则，该规则针对当前方法生效，与其他方法互不影响，目前方法仅支持HttpMethod+uri。
- 服务维度含义为可针对目标服务配置熔断规则，规则对向该服务发起的所有请求都有效。
方法维度和服务维度的熔断规则不能共存，一个服务只能有一条服务级别的熔断规则，或针对不同方法的多条方法级别熔断规则。

熔断Starter目前只提供http调用的熔断，后续有计划支持Rpc方式的熔断。

**熔断结果**为：熔断发生后，熔断模块将直接抛出熔断异常，对于Http调用将抛出CircuitBreakerOpenException异常。

**熔断类型**为：手动熔断、自动熔断。
手动熔断开启后，没有熔断条件，所有的请求都会被熔断。
自动熔断开启后，达到熔断条件后，熔断才会开启。

熔断器包括OPEN、HALF_OPEN和CLOSED三种状态
COPEN：当请求失败率超过阈值时，熔断器的状态由关闭状态转换到打开状态。

HALF_OPEN：打开状态的持续时间结束，熔断器的状态由打开状态转换到半开状态。这时允许一定数量的请求通过，当这些请求的失败率超过阈值，熔断器的状态由半开状态转换回打开状态。

CLOSED：如果请求失败率小于或等于阈值，则熔断器的状态由半开状态转换到关闭状态。

## 快速开始
使用Maven构建项目，（业务需要目前仅支持了在Spring MVC web项目中的无侵入实现）

### 引入Maven依赖
```xml
<dependency>
    <groupId>com.baidu.formula</groupId>
    <artifactId>circuitbreaker-spring-boot-starter</artifactId>
    <version>{version}<version>
</dependency>
```
版本号请从[maven仓库](http://maven.scm.baidu.com:8081/nexus/index.html#nexus-search)中获取最新版本

### 配置熔断规则
在Spring Boot项目的application.properties/application.yml中配置熔断规则

```yaml
formula:
  circuitBreaker:
    rules:
    # 熔断生效的具体位置
    - serviceName: provider-demo/hello
      # 熔断后降级返回值
      fallbackResult: ''
      # 熔断后降级类型  1:"抛出异常"; 2:"返回null"; 3:"返回值" ;4:"调用方法" (当前只支持1)
      fallbackType: 1
      # 是否开启手动熔断
      forceOpen: false
      # 请求异常比例
      failureRateThreshold: 1
      # 熔断条件：开启熔断前，窗口大小
      ringBufferSizeInClosedState: 20
      # 熔断半开状态，窗口大小
      ringBufferSizeInHalfOpenState: 10
      # 熔断持续时间
      waitDurationInOpenState: 60000
      # 一次请求的限定时间（暂不支持）
      timeoutDuration: -1
      # 请求超时后，是否中断本次请求（暂不支持）
      cancelRunningFuture: true
```
## 高级特性
配合config-client-spring-boot-starter和spring-cloud-config-server实现动态下发生效熔断规则。

### 部署配置config-server
参考[spring-cloud-config-server文档](https://cloud.spring.io/spring-cloud-config/multi/multi__spring_cloud_config_server.html)

### 引入config-client依赖
基于快速开始构建的项目，引入config-client-spring-boot-starter
```xml
<dependency>
    <groupId>com.baidu.formula</groupId>
    <artifactId>config-client-spring-boot-starter</artifactId>
    <version>{version}<version>
</dependency>
```
版本号请从[maven仓库](http://maven.scm.baidu.com:8081/nexus/index.html#nexus-search)中获取最新版本。
并参考[spring cloud config client文档](https://cloud.spring.io/spring-cloud-config/multi/multi__spring_cloud_config_client.html#config-first-bootstrap)，配置项目

### 配置熔断规则
按照config-server的使用方式，在其后端的存储仓库如git上配置熔断规则。

项目搭建无异常即可测试项目，确认熔断规则是否生效。并且可以动态修改git中熔断规则，测试请求结果是否变化。
