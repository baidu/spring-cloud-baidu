# Spring cloud Formula Launcher
jar替换工具。SpringBoot, SpringCloud在不同版本有一套固定的依赖，见Spring Boot Platform. 不同版本也有不同的问题和特性，
因此数百个应用升级SpringBoot, SpringCloud和其它starter版本是一个无法完成的任务。

为了解决这类问题，我们基于maven的私服，BOM，的分析，动态下载所需要依赖，定制程序启动过程，实现不改变应用程序的情况下修改应用程序依赖的jar版本。

支持按配置规则做 springboot 应用的jar依赖启动时替换，实现基础中间件与业务逻辑分开维护。

#### 主要功能
* 基于starter来实现，过滤器链的配置方式, 方便使用和扩展。
* 支持加载Springboot应用程序, 普通java程序(DOING), WAR(DOING)。
* Classloader独立，Launcher不会浸入应用程序的Classloader。
* 基于 Maven 依赖分析工具，实现 BOM 管理应用程序的依赖版本。
* 替换过程可观察，生成加载类的实例。

## 快速开始


## 配置文件
```yaml
formula:
  launcher:
    application:
      # 加载的应用程序路径, xx.jar && main-class 没有配置的时候，是fatjar. 暂时只支持springboot格式的fatjar.
      # xx.war为部署war应用，采用undertow.
      # xx/lib,xx/classes的情况下，是普通java程序，可以
      path: xx.jar/ xx.war/ xx/lib,xx/classes
      main-class: xx.xx.Main
    rules:
    - id: xx
      order: 1
      predicates:
      - Type=springboot java war
      - HasArtifact=groupId, artifactId, version
      - HasJar=xx.jar # 支持正则表达式
      actions:
      - RemoveArtifacts=xx xx xx # 多个依赖空格分隔
      - AddArtifacts=xx xx xx
      - AlignBOM=groupId:artifactId:version groupId2:artifactId2:version 
    - id: xxf
      order: 2
      predicates:
      - Type=java
      actions:
      - RemoveArtifacts
    - id: xxf
      order: 4
      prefidcates:
      - Type=springboot
      - HasArtifact=groupId, artifactId, 2.0.0
      actions:
      - AddArtifacts=xx xx xx
    meta:
      maven-repos:
      - http://mvnrepository.com/artifact
      enable-central: true/false
      enable-third-part: true/false
```
