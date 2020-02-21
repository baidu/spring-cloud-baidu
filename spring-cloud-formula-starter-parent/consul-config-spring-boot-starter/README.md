# Consul Config Spring Boot Starter

Consul Config Spring Boot Starter 是基于 Spring Cloud Consul Config 改造而来，继承了原有的基于 HTTP Long Polling
 的配置热更新功能，扩展了基于 Consul Token 的安全认证机制，新增了基于不同 Consul KV 目录的配置优先级生效逻辑。
 
此外，结合配置中心管理端（尚未开源），还可以使用配置多版本管理、配置克隆、灰度发布、权限管理、操作审计等众多高级功能。

## 重要配置项说明

```yaml
# for consul config spring boot starter

# default to true, set false to disable all consul config func.
spring.cloud.consul.config.enabled=true
# default to true, set false to disable watch config change (disable hot update).
spring.cloud.consul.config.watch.enabled=true
# default to true, set false when token auth is disabled from remote server.
spring.cloud.consul.config.token-enabled=true
# set auth-uri when 'token-enabled=true'.
spring.cloud.consul.config.auth-uri=http://{auth-host}:{port}/{api}

# default to 'localhost'.
spring.cloud.consul.host=localhost
# default to '8500'.
spring.cloud.consul.port=8500

# default to 'config', this is the first directory.
spring.cloud.consul.config.prefix={workspace-id}
# default value is 'default', this is one of the second directory, but with lowest priority.
spring.cloud.consul.config.default-context=default
# list value, separated by ',', they are the rest of the second directory, default to null,
# priority increases when it is listed in behind.
spring.cloud.consul.config.system-labels={app-id},{env-id},{dg-id}
```

注： token-enabled、auth-uri、 system-labels 为高级功能配置项，本地开发可以不关注；
如不需要使用配置热更新机制，可以设置 spring.cloud.consul.config.watch.enabled=true 来关闭该功能。

## 快速开始

### 启动本地Consul Server

- 下载 Consul 启动包：https://www.consul.io/downloads.html

- 参照 Consul 使用指南安装: https://learn.hashicorp.com/consul/getting-started/install

- 启动 Consul Agent：https://learn.hashicorp.com/consul/getting-started/agent 
    
    - 以开发模式启动：consul agent -dev （快速测试推荐方式）
    
    - 持久化启动：consul agent -server -bootstrap-expect 1 -data-dir=/usr/local/consul/data/ -config-dir=/usr/local/consul/consul.d/

- 访问 http://localhost:8500/ui/dc1/kv , 点击 Create, 在 Key or folder 中输入 config/ , 然后点击 Save 保存；
成功后，点击进入 config 目录， 以同样的方式创建二级目录 default、三级目录 msc-1 (任意字符与数字组合)、四级目录 full，
在四级目录 full 下面创建 key：kvs。

    - 根据测试需要，可以在 key：kvs 对应的 value 里面输入 demo 应用相应的 Key/Value，以分号';'作为分隔符。
    
    - E.g. test.k1=v1;test.k2=v2;test.k3=v3
    
    - 注意，上面 value 里面的 'test' 是程序中 @ConfigurationProperties 注解指定的 'prefix'。
    
### 添加Maven依赖

```xml
        <dependency>
            <groupId>com.baidubce.formula</groupId>
            <artifactId>consul-config-spring-boot-starter</artifactId>
            <version>${consul-config-version}</version>
        </dependency>
```

从公共 Maven 仓库中找到 consul-config-spring-boot-starter 最新的版本填入 ${consul-config-version}。

### 添加必要配置项

```yaml
# for consul config spring boot starter

# disable token auth
spring.cloud.consul.config.token-enabled=false

# using default value:
spring.cloud.consul.host=localhost
spring.cloud.consul.port=8500
spring.cloud.consul.config.enabled=true
spring.cloud.consul.config.watch.enabled=true
spring.cloud.consul.config.prefix=config
spring.cloud.consul.config.default-context=default
```

### 创建配置类

在基于 Spring Boot 框架的项目中，参照下面样例创建配置类：

```java
@RefreshScope
@ConfigurationProperties(prefix = "test")
public class MyTestProperties {

    private String k1;

    private String k2;

    private String k3;

}
```

创建好后，可以在 Service 或者 Controller 中使用。

启动程序，通过 Consul UI 动态修改对应的键值可验证配置动态更新等功能。
