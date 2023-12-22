简体中文 | [English](README_EN.md)

# **HTEffect集成Android教程**
## **说明**
- 本文介绍如何快速配置HTEffect模块

<br/>

## **操作步骤**
### **1. 下载源码**
依次执行以下命令
- git clone **当前仓库地址**
- cd **工程目录**
- git submodule init && git submodule update

### **2. 配置工程**
下载完成后，打开工程
- 将 AndroidManifest.xml 中的 **label** 和 build.gradle 中的 **applicationId** 分别替换为您的**应用名**和**包名**
- 在项目的Application中将 **YOUR_APP_ID** 替换成您的**AppId**
- 将htui模块中的**assets**替换为您的**assets**
- 编译，运行，日志搜索**init-status**可以查看相关日志
- 具体执行步骤可以全局搜索 **//todo --- HTEffect** 进行查看

<br/>
