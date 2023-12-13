简体中文 | [English](README_EN.md)

# **HTEffect Demo**
## **说明**
- 本文介绍如何快速跑通示例工程

<br/>

## **操作步骤**
### **iOS**
#### **1. 下载源码**
依次执行以下命令
- git clone https://gitee.com/htai-open/HTEffect_Demo_iOS.git
- cd HTEffect_Demo_iOS
- git submodule init && git submodule update

#### **2. 配置工程**
下载完成后，打开HTEffectDemoiOS.xcodeproj工程
- 将Bundle Display Name 和 Bundle Identifier 分别替换为您的应用名和包名
- 将AppDelegate.m中[[HTEffect shareInstance] initHTEffect:@"Your AppId" withDelegate:self]的Your AppId替换成您的AppId
- 将HTEffect文件夹下的HTEffect.bundle替换为您的HTEffect.bundle
- 编译，运行，日志搜索init-status可以查看相关日志

<br/>

### **Android**
#### **1. 下载源码**
依次执行以下命令
- git clone https://gitee.com/htai-open/HTEffect_Demo_Android.git
- cd HTEffect_Demo_Android
- git submodule init && git submodule update

#### **2. 配置工程**
下载完成后，打开HTEffect_Demo_Android工程
- 将 AndroidManifest.xml 中的 label 和 build.gradle 中的 applicationId 分别替换为您的应用名和包名
- 将HtApplication.java中的 "YOUR_APP_ID" 替换成您的AppId
- 将htui模块中的assets替换为您的assets
- 编译，运行，日志搜索init-status可以查看相关日志
