# Chrome Autofill Bridge

LSPosed 模块：让 Chrome 保持 Google Password Manager，同时把网页表单通过 Android Autofill 兼容桥提供给当前选中的第三方 AutofillService 的 inline 候选。Bitwarden、1Password 及其他遵循 Android Autofill/inline API 的服务都可以使用这条路径。

模块包名为 `org.lyf.chromeautofillbridge`，沿用已有 JuiceSSH Xposed 模块的 `org.lyf.*` 格式。

项目处于实验阶段。模块修复了 Chrome 兼容桥把网页错误识别为 `com.android.chrome` 的问题，并把当前 HTTPS 顶层文档来源传给第三方服务。

## 已验证环境

Pixel 11 Pro XL；Android 17 / SDK 37；Chrome 152.0.7977.75；Bitwarden 2026.8.0；Gboard；LSPosed IT 2.2.0-it (7855)，框架 API 102。模块使用 libxposed API 101。Bitwarden 已完成实机验证；1Password 和其他服务尚待实机回归。

## 安装

1. 从 Releases 下载签名后的 APK 并安装。
2. LSPosed：启用模块，勾选 Chrome (`com.android.chrome`)。更新后确认作用域仍被勾选。
3. Android“密码、通行密钥和自动填充”：选择一个第三方 AutofillService，例如 Bitwarden 或 1Password，并在该应用中打开 inline/filling suggestions。
4. Chrome → Autofill services → **Autofill with Google**，重启 Chrome。

系统同时只能选择一个第三方 AutofillService；这个模块让它与 Chrome 的 Google Password Manager 并存。

模块没有启动界面，通过 LSPosed 管理。发布 APK 使用项目持有者的 release 密钥签名，密钥不进入仓库。

## 实现与修复

- 在 Chrome 主进程的 Activity 恢复后，确认当前启用了第三方 Android AutofillService，并确认 Chrome 的 Google 模式。
- 设置进程内 `AutofillOptions.compatModeEnabled`，启用 `AutofillManager` 的兼容桥，并同步兼容状态查询。
- Android 的兼容节点采集会遗漏 `webDomain`；模块从实际 ContentView 持有的 WebContents 获取两个 GURL getter，要求 visible URL 和 last committed URL 的 HTTPS origin 一致。
- 为顶层文档及其后代节点写入 `webDomain`，使不同 AutofillService 都能按网站匹配候选。
- 跳过嵌套 iframe/文档、来源未知或来源不一致的填充请求；Chrome 原生输入框（如地址栏）也排除在第三方兼容请求之外。
- 填入时再次比较当前来源与采集时来源。

模块不访问任何密码管理器数据库，也不导出凭据。passkey 仍由 Android Credential Manager 路由，模块不修改 WebAuthn 路由。

## 当前验证与限制

详见 [TEST-RESULTS.md](TEST-RESULTS.md)。域名识别、换站更新、opaque iframe 排除和 Google Chrome 模式已通过实机测试。Bitwarden 的 inline 候选和点选填入已确认；1Password、其他 AutofillService、信用卡、TOTP 与 passkey 完整流程尚待验证。

当前范围为 **HTTPS 顶层文档**。嵌套 iframe（包括同源 iframe）暂时跳过。无痕、多 Activity、重定向中的候选生命周期及其他系统/Chrome 版本尚未全面回归。

## 回滚

在 LSPosed 关闭模块并重启 Chrome，或卸载 `org.lyf.chromeautofillbridge` 后重启 Chrome。进程内兼容状态随进程退出清除。用户手动选择的自动填充服务设置可自行恢复。

## 构建

需要 JDK 17、Python 3、Android SDK platform 36 与 build-tools 36.1.0：

```sh
ANDROID_SDK_ROOT=/path/to/android-sdk \
RELEASE_KEYSTORE=/secure/release.jks \
RELEASE_KEY_ALIAS=your-alias \
RELEASE_STORE_PASSWORD='...' \
RELEASE_KEY_PASSWORD='...' \
OUTPUT_APK=chrome-autofill-bridge.apk \
./build.sh
```

脚本从 Maven Central 获取 `io.github.libxposed:api:101.0.0` 作为编译依赖，运行时由框架提供 API。中间文件默认写入工作区 `work/chrome-bridge-build`，可通过 `BRIDGE_BUILD_DIR` 指定。密码建议通过临时环境变量注入，仓库不保存 keystore、密码或 APK。

## 上游依据

- [Android Autofill services](https://developer.android.com/identity/autofill/autofill-services)
- [Android AutofillManager / CompatibilityBridge](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/view/autofill/AutofillManager.java)
- [Android View.populateVirtualStructure](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/view/View.java)
- [Chromium WebContentsImpl 的 URL getter](https://github.com/chromium/chromium/blob/main/content/public/android/java/src/org/chromium/content/browser/webcontents/WebContentsImpl.java)
- [1Password Android Autofill](https://support.1password.com/android-autofill/)
- [Bitwarden Android AutofillParserImpl](https://github.com/bitwarden/android/blob/main/app/src/main/kotlin/com/x8bit/bitwarden/data/autofill/parser/AutofillParserImpl.kt)

研究基于 2026-09-07 获取的上游源码，兼容性以上述实机记录为准。
