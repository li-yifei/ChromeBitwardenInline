# Chrome Autofill Bridge

[![Release](https://img.shields.io/github/v/release/li-yifei/ChromeAutofillBridge)](https://github.com/li-yifei/ChromeAutofillBridge/releases/latest)
![Android](https://img.shields.io/badge/Android-11%2B-3DDC84?logo=android&logoColor=white)
![Xposed](https://img.shields.io/badge/Xposed_API-101%2B-blue)
[![License](https://img.shields.io/badge/License-GPL--3.0--or--later-blue)](LICENSE)

[English](README.md) | **简体中文**

我很讨厌 Chrome 让我在 Google Password Manager 和第三方密码管理器之间二选一。
所以做了本模块突破此限制。
保留 Google 内置自动填充，同时使用你喜欢的密码管理器提供的键盘候选。

## 安装

1. 从 [Releases](https://github.com/li-yifei/ChromeAutofillBridge/releases/latest) 下载并安装 APK。
2. 在 LSPosed 中启用模块，作用域勾选 **Chrome**。
3. 在 Android 设置中把第三方密码管理器设为首选自动填充服务，并开启该应用的键盘候选。
4. Chrome → **设置 → 自动填充服务 → 使用 Google 自动填充**，重启 Chrome。

## 兼容性

| 要求 | 说明 |
| --- | --- |
| Android | 最低 11；已测试 Android 17 |
| 框架 | LSPosed 或支持现代 Xposed API 101+ 的框架 |
| 浏览器 | Google Chrome（`com.android.chrome`）；已测试 152.0.7977.75 |
| 密码管理器 | Bitwarden 已测试；1Password 已在工作资料中确认可用 |
| 键盘 | 已测试 Gboard；inline 候选需要键盘支持 |

其他 Android Autofill 服务有望兼容，具体取决于各自实现。第三方候选来自 Android 当前选中的自动填充服务。

当前支持 HTTPS 顶层表单，跳过嵌套 iframe。Passkey 沿用原有路由；passkey、信用卡和 TOTP 的完整流程仍待测试。验证范围见[测试记录](TEST-RESULTS.md)。

## 构建

需要 JDK 17、Android SDK 36 和 Build Tools 36.1.0。设置 `ANDROID_HOME`，或在 `local.properties` 中配置 `sdk.dir`。

```sh
./gradlew :app:assembleDebug
```

APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

Release 构建见[构建与签名](docs/BUILDING.md)，桥接实现见[技术说明](docs/IMPLEMENTATION.md)。

## 许可证

[GPL-3.0-or-later](LICENSE)。
