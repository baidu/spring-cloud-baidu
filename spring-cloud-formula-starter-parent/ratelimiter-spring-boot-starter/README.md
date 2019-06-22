# RateLimiter Spring Boot Starter
限流的目的为通过对并发请求进行限速以保护服务节点或数据节点，防止瞬时流量过大使服务节点或者数据崩溃，以提升微服务系统的稳定性。

ratelimiter-spring-boot-starter为服务端限流的SDK，提供单节点维度的限流功能，通过限流算法，在流量过大时保证服务端按照一定速率平滑处理请求。

基于Spring Boot框架开发，目的是为Spring Cloud项目增加限流功能，同样在Spring Boot项目中也能正常使用。
本Starter的目前的应用场景为在Spring Cloud/Spring Boot的Web项目中引入该限流Starter，配置限流规则开启限流功能。
非Spring Web项目的特性正在规划中。

**限流维度**为：节点级、方法维度、服务维度限流。
- 节点级别含义为限流SDK引入目标服务代码，限流规则针对目标服务部署的每个实例单独生效。
- 方法维度含义为可以为目标服务的每个方法单独配置限流规则，该规则针对当前方法生效，与其他方法互不影响，目前方法仅支持HttpMethod+uri。
- 服务维度含义为可针对每个服务实例配置全局规则，流入该服务实例的每个请求都将先进行服务限流判断。
服务级和方法级同时存在，将先后进过服务级、方法级两种限流器，任意一个限流器拒绝都将拒绝请求。

目前方法级只提供http方法的规则配置与生效，后续有计划支持Rpc方法的限流。

**限流结果**为：请求被限流后，限流模块将直接组装信息，立即响应/返回，对于Http调用将返回状态码为429(Too Many Request)的响应。

限流Starter目前只提供http调用的限流，后续有计划支持Rpc方式的限流。

## 主要特性
- 基于Spring Boot框架开发，方便在Spring Boot/Spring Cloud项目中使用
- 支持服务端的限流，使用令牌桶算法实现限速请求
- 支持节点级别、方法维度、服务维度限流
- Spring MVC框架构架的代码，可无侵入使用
- 支持配置规则的动态下发和热加载

## 快速开始
使用Maven构建项目，（业务需要目前仅支持了在Spring MVC web项目中的无侵入实现）

### 引入Maven依赖
```xml
<dependency>
    <groupId>com.baidu.formula</groupId>
    <artifactId>ratelimiter-spring-boot-starter</artifactId>
    <version>{version}<version>
</dependency>
```
版本号请从[maven仓库](http://maven.scm.baidu.com:8081/nexus/index.html#nexus-search)中获取最新版本

### 配置限流规则
在Spring Boot项目的application.properties/application.yml中配置限流规则

**Http方法(uri)维度限流**
```yaml
formula:
  ratelimiter:
    ratelimiters:
    # 限流生效的位置，配置具体的uri
    - effectiveLocation: /hello
      # 限流类型：1表示http，2表示rpc(暂未支持)
      effectiveType: 1
      # 该规则是否生效
      enabled: true
      httpMethod: GET
      # 限流器类型，1表示令牌桶
      limiterType: 1
      # 请求来源，当前版本不区分请求来源，区分请求来源的需求正在开发
      source: all
      # 限流的QPS值
      threshold: 123
```
**Http方式服务级(全局)限流**
```yaml
formula:
  ratelimiter:
    ratelimiters:
    # 正在等待解决/global标识问题
    - effectiveLocation: /global
      effectiveType: 1
      enabled: true
      httpMethod: '*'
      limiterType: 1
      source: all
      threshold: 1000
```
此时，http调用到服务端时，限流器将根据配置的规则，判断是否允许请求继续执行或者限流。

## 高级特性
配合config-client-spring-boot-starter和spring-cloud-config-server实现动态下发生效限流规则。
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
### 配置限流规则
按照config-server的使用方式，在其后端的存储仓库如git上配置限流规则。

项目搭建无异常即可测试项目，确认限流规则是否生效。并且可以动态修改git中限流规则，测试限流现象是否变化。

