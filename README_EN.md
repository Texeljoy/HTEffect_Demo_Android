[简体中文](README.md) | English

# **HTEffect Demo**
## **Instruction**
- This article introduces how to quickly get through example projects.

<br/>

## **Steps**
### **iOS**
#### **1. Download**
Execute the following commands in sequence
- git clone https://gitee.com/htai-open/HTEffect_Demo_iOS.git
- cd HTEffect_Demo_iOS
- git submodule init && git submodule update

#### **2. Configuration**
After downloading, open Project **HTEffectDemoiOS.xcodeproj**
- Replace **Bundle Display Name** and **Bundle Identifier** with your APP name and package name, respectively
- Replace **Your AppId** in **[[HTEffect shareInstance] initHTEffect:@"Your AppId" withDelegate:self]** with your AppId in **AppDelegate.m**
- Replace **HTEffect.bundle** in **HTEffect** folder with your HTEffect.bundle
- Build, Run, and search **init-status** to see relevant logs

<br/>

### **Android**
#### **1. Download**
Execute the following commands in sequence
- git clone https://gitee.com/htai-open/HTEffect_Demo_Android.git
- cd HTEffect_Demo_Android
- git submodule init && git submodule update

#### **2. Configuration**
After downloading, open Project **HTEffect_Demo_Android**
- Replace **label** in AndroidManifest.xml and **applicationId** in build.gradle with your APP name and package name, respectively
- Replace **"YOUR_APP_ID"** with your AppId in **HtApplication.java**
- Replace **assets** in **htui** folder with your assets
- Build, Run, and search **init-status** to see relevant logs