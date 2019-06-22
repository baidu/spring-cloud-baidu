# Config Client Spring Boot Starter
基于 [spring-cloud-config](https://spring.io/projects/spring-cloud-config) 二次开发的配置中心客户端，与原版配置中心完全兼容。对于以下功能点做了兼容性扩展：
* 轻量级配置热加载。
* 多应用继承。
* 配置预处理。
* 应用外配置加载。

## 快速开始
与[spring-cloud-config](https://spring.io/projects/spring-cloud-config)类似，只需添加依赖并且增加配置即可。

### 引入maven依赖
```xml
<dependency>
    <groupId>com.baidu.formula</groupId>
    <artifactId>config-client-spring-boot-starter</artifactId>
</dependency>
```

### 添加配置文件*bootstap.properties*
```properties
# 配置中心的应用名，我们是每个部门一个配置仓库，因此这个名字为[产品线名称]-[应用名称]
spring.cloud.config.name=[productLine]-[app-name]
# 配置中心服务端地址，所有人都是这个值
spring.cloud.config.uri=http://xxx
# 配置profile, 首先看命令行参数config.profile, 系统属性config.profile, 环境变量CONFIG_PROFILE, 如果没有设置，取默认值env-type-offline
spring.cloud.config.profile=${config.profile:env-type-offline}
# 远程配置是否要覆盖本地已存在的配置, 设置成true表示远程配置不会覆盖任何已经存在的值, 这样会优先使用本地的application.yml, 但是要避免application.yml提交到版本仓库;
# 如果设置成false表示远程配置可以覆盖已存在的值，配置中心有的配置项优先生效,不能通过本地配置来修改,但可以通过系统属性来修改（前面设置了远程配置不能覆盖系统属性）.
# spring.cloud.config.override-none=false ## 默认就是false
# 远程配置是否要覆盖系统属性. true: 远程配置会覆盖系统属性. 因此这里要设成false
spring.cloud.config.override-system-properties=false
# spring.cloud.config.allow-override=true ## 默认就是true
```
完整的配置项目列表见
[官方文档](https://cloud.spring.io/spring-cloud-static/spring-cloud-config/2.0.3.RELEASE/single/spring-cloud-config.html#_spring_cloud_config_client)

### 代码中使用
代码中使用方法与配置中心完全透明。
1. 使用@ConfigurationProperties, 可以支持热加载
2. 使用@Value, 不支持热加载
3. 通过Environment，不支持热加载，可以监听事件EnvironmentChangeEvent

## 高级特性

### 配置预处理
这个主要解决根据环境变量解释同一个profile的配置，主要使用场景是根据可用区应用不同的配置。
1. 添加依赖
```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-jexl3</artifactId>
</dependency>
```
2. 配置扩展
采用jexl3配置, 支持整个配置项是jexl3表达式的场景, 语法参考[官方文档](http://commons.apache.org/proper/commons-jexl/reference/syntax.html)
可以使用的变量有(按优先级排序)
    1. 测试属性源(DOING)
    2. 启动参数(DOING)
    3. 系统属性(System.getProperty)*
    4. 环境变量(System.getenv).

\* 中存在[.]分隔的变量, 会处理成驼峰. 如 系统变量 os.name, 会转成变量 osName.

### 快速配置覆盖
在应用程序工作目录下新建一个config/application.yml, 可以优先配置中心和应用程序内的配置生效。

## 最佳实践

### 开发时
开发时涉及到配置频繁修改，不需要每次都提交到GIT仓库。有如下两个方式临时修改配置，方便代码调试。
1. 新建application.yml，写在这个文件里面，开发，自测的时候会读这个配置，开发自测完成后，
把application.yml里面的配置提交到配置仓库(线下通用配置)，把代码提交到应用的git仓库. 

PS, 如果遇到application.yml与配置中心的配置发生冲突时，由下边的方式来控制优先级
```properties
# 如果设置成false表示远程配置可以覆盖已存在的值，配置中心有的配置项优先生效,不能通过本地配置来修改,但可以通过系统属性来修改（前面设置了远程配置不能覆盖系统属性）.
# spring.cloud.config.override-none=true ## 默认是false
```
2. 单测/自测时修改配置。如下代码中，properties中的配置项目是优先级最高，可以覆盖本地application.yml和配置中心的配置。
```java
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = JarvisApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "debug=true",
                "cpdinf.stargate.registration=false",
                "logging.level.org.apache.ibatis=DEBUG",
                "logging.level.org.mybatis=DEBUG",
                "logging.level.java.sql=DEBUG",
                "logging.level.com.baidu=DEBUG",
                "spring.datasource-eye.hikari.initialization-fail-timeout=-1"
        })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ApplicationTest {
    @Test
    public void testContextLoad() {
    }
}
```

3. 开发新功能的时候添加配置项，请参考@Value, @ConfigurationProperties的使用。

### 测试时
QA测试时可能需要临时修改某个配置项的配置值，采用如下方式来做
##### 启动参数
```bash
sh noah_control start --config.key1=value1 --config.key2=value2
```    
##### 环境变量
````bash
export CONFIG_KEY1=value1
sh noah_control start
````
##### config/application.yml, 具体的优先级安排见实现方案
````bash
$ ls
bin  jdk8  lib  logs
$ mkdir config
$ echo "config.key1: value1" > config/application.yml
$ sh bin/noah_control start
````

## 实现方案

### 服务端设计
扩展Spring Cloud Config Server, 一方面提供微服务系统内部模块的配置管理，另一方面提供用户的配置管理接口/config。

### 客户端设计
基于spring cloud config client 二次开发，维持SpringBoot中Environment的优先级设计
> 1. 开发工具启用时~/.spring-boot-devtools.properties
> 2. @TestPropertySource
> 3. @SpringBootTest#properties 注解上的测试
> 4. 命令行参数
> 5. 环境变量/系统属性上的SPRING_APPLICATION_JSON 属性, 行内JSON
> 6. ServletConfig init params
> 7. ServletContext init params
> 8. JNDI
> 9. Java 系统属性 System.getProperties()
> 10. 操作系统环境变量. System.getenv()
> 11. RandomValuePropertySource 随机变量 random.*
> 12. profile特定的application-{profile}.properties(jar包之外，spring.config.location的目录)
> 13. profile特定的application-{profile}.properties(自己的jar)
> 14. application.properties (jar包之外，spring.config.location的目录)
> 15. application.properties (自己的jar内)
> 16. @PropertySource 标记在@Configuration上。

在这个序列中增加远程抓取的结果, ref:
[PropertySourceBootstrapConfiguration.insertPropertySource()](https://github.com/spring-cloud/spring-cloud-commons/blob/master/spring-cloud-context/src/main/java/org/springframework/cloud/bootstrap/config/PropertySourceBootstrapConfiguration.java)
如果allow-override=false, 或者override-none=false && override system properties, 则插入到最前面. 如果override-none=true, 插入到最后面.
如果override system properties , 插入在系统属性之前, 否则插入之后.
为了优先加载本地的config/application.yml, 或者config/application.properties, 在加载cloud config client的内容时候再把这两个PropertySource 加到属性源的最开始.

### 管理端设计
基于[jGit](https://www.eclipse.org/jgit/)库，操作GIT仓库。实现配置历史，配置提交，版本DIFF等功能。
## 常见问题

##### 检查生效的配置
SpringBoot 出于安全考虑，这个功能默认不开启，通过下边的配置来开启
````yaml
management.endpoint.env.enabled: true
````
然后通过http请求查看
````bash
curl ip:port/actuator/env | python -m json.tool
````
结果中第一个出现的优先级最高。