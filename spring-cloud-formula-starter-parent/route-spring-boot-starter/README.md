# Route Spring Boot Starter
微服务服务路由提供完善的路由机制将请求分发至对应的服务实例。

route-spring-boot-starter为服务路由SDK，提供一系列的路由功能。

基于Spring Boot框架开发，目的是为Spring Cloud项目增加服务路由功能，同样在Spring Boot项目中也能正常使用。
本Starter的目前的应用场景为在Spring Cloud/Spring Boot的Web项目中引入该路由Starter。

## 主要特性
- 基于Spring Boot框架开发，方便在Spring Boot/Spring Cloud项目中使用
- 支持基于标签的请求来源
- 支持权重路由
- 支持路由时配置负载均衡策略，动态热生效

## 快速开始
使用Maven构建项目，（业务需要目前仅支持了在Spring MVC web项目中的无侵入实现）

### 引入Maven依赖
```xml
<dependency>
    <groupId>com.baidu.formula</groupId>
    <artifactId>route-spring-boot-starter</artifactId>
    <version>{version}<version>
</dependency>
```
版本号请从[maven仓库](http://maven.scm.baidu.com:8081/nexus/index.html#nexus-search)中获取最新版本