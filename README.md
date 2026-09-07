# Chrome + Bitwarden Inline

LSPosed 模块：Chrome 保持 Google Autofill，Bitwarden 通过 Android 兼容桥进入 Gboard 的 inline 候选。模块包名为 `org.lyf.chromebitwardeninline`，沿用已有 JuiceSSH Xposed 模块的 `org.lyf.*` 包名格式。

项目处于实验阶段。模块修复了 Chrome 兼容桥把网页错误识别为 `com.android.chrome` 的问题，并把当前 HTTPS 顶层文档来源传给 Bitwarden。

## 已验证环境

Pixel 11 Pro XL；Android 17 / SDK 37；Chrome 152.0.7977.75；Bitwarden 2026.8.0；Gboard；LSPosed IT 2.2.0-it (7855)，框架 API 102。模块使用 libxposed API 101。

## 安装

1. 从 Releases 下载签名后的 APK 并安装。
2. LSPosed：启用模块，勾选 Chrome (`com.android.chrome`)。更新后确认作用域仍被勾选。
3. Android：自动填充服务选 Bitwarden；Bitwarden：显示方式选 Inline。
4. Chrome → Autofill services → Autofill with Google，重启 Chrome。

模块没有启动界面，通过 LSPosed 管理。发布 APK 使用项目持有者的 release 密钥签名，密钥不进入仓库。

## 实现与修复

- 在 Chrome 主进程的 Activity 恢复后，确认 Chrome Google 模式以及系统 Bitwarden 服务。
- 设置进程内 `AutofillOptions.compatModeEnabled`，启用 `AutofillManager` 的兼容桥，并同步兼容状态查询。
- Android 的兼容节点采集会遗漏 `webDomain`；Bitwarden 当前也已移除 Chrome 地址栏的旧回退识别。
- 从实际 ContentView 持有的 WebContents 获取两个 GURL getter（上游分别为 visible URL、last committed URL）。通过稳定类型匹配定位混淆后的成员，要求两者 HTTPS origin 一致。
- 为顶层文档及其后代节点写入 `webDomain`。Bitwarden 只继承直接父节点域名，所以需要覆盖各层节点。
- 跳过嵌套 iframe/文档；来源未知或两个 URL 来源不一致时跳过填充请求。Chrome 原生输入框（如地址栏）也排除在第三方兼容请求之外。
- 填入时再次比较当前来源与采集时来源。

模块日志只记录生命周期与错误，诊断阶段的结构日志已移除。模块不访问 Bitwarden 数据库或导出凭据。

## 当前验证与限制

详见 [TEST-RESULTS.md](TEST-RESULTS.md)。域名识别、换站更新、opaque iframe 排除已通过实机测试。0.1 的 inline 候选和点选填入由用户确认成功；0.2 新增来源约束后的真实站点填入仍需确认。

当前范围为 **HTTPS 顶层文档**。嵌套 iframe（包括同源 iframe）暂时跳过。无痕、多 Activity、重定向中的候选生命周期及其他系统/Chrome 版本尚未全面回归。

Google 的原生填充模式保持开启，已观察到密码、卡片、地址入口及密码保存提示。信用卡完整填入、TOTP、两家 passkey 的完整登录流程尚待验证；模块没有修改 WebAuthn/Credential Manager 路由。

## 回滚

在 LSPosed 关闭模块并重启 Chrome，或卸载 `org.lyf.chromebitwardeninline` 后重启 Chrome。进程内兼容状态随进程退出清除。用户手动选择的自动填充服务设置可自行恢复。

## 构建

需要 JDK 17、Python 3、Android SDK platform 36 与 build-tools 36.1.0：

```sh
ANDROID_SDK_ROOT=/path/to/android-sdk \
RELEASE_KEYSTORE=/secure/release.jks \
RELEASE_KEY_ALIAS=your-alias \
RELEASE_STORE_PASSWORD='...' \
RELEASE_KEY_PASSWORD='...' \
OUTPUT_APK=chrome-bitwarden-inline.apk \
./build.sh
```

脚本从 Maven Central 获取 `io.github.libxposed:api:101.0.0` 作为编译依赖，运行时由框架提供 API。中间文件默认写入工作区 `work/chrome-bridge-build`，可通过 `BRIDGE_BUILD_DIR` 指定。密码建议通过临时环境变量注入，仓库不保存 keystore、密码或 APK。

## 上游依据

- [Chromium AutofillClientProvider](https://github.com/chromium/chromium/blob/main/chrome/browser/ui/autofill/autofill_client_provider.cc)
- [Android AutofillManager / CompatibilityBridge](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/view/autofill/AutofillManager.java)
- [Android View.populateVirtualStructure](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/view/View.java)
- [Chromium WebContentsImpl 的 URL getter](https://github.com/chromium/chromium/blob/main/content/public/android/java/src/org/chromium/content/browser/webcontents/WebContentsImpl.java)
- [Bitwarden AutofillParserImpl](https://github.com/bitwarden/android/blob/main/app/src/main/kotlin/com/x8bit/bitwarden/data/autofill/parser/AutofillParserImpl.kt)
- [Bitwarden ViewNodeExtensions](https://github.com/bitwarden/android/blob/main/app/src/main/kotlin/com/x8bit/bitwarden/data/autofill/util/ViewNodeExtensions.kt)
- [Chrome 兼容模式停止支持说明](https://developer.android.com/identity/autofill/autofill-services)

研究基于 2026-09-07 获取的上游源码，兼容性以上述实机记录为准。
