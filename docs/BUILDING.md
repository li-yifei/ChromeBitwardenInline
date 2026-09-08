# Building / 构建

The Gradle wrapper pins Gradle 8.13 with distribution checksum verification. Android Gradle Plugin 8.13.0 uses JDK 17. Install Android SDK Platform 36 and Build Tools 36.1.0, then set `ANDROID_HOME` or `sdk.dir` in your local `local.properties`.

项目使用带校验和的 Gradle 8.13 Wrapper、AGP 8.13.0 和 JDK 17。安装 Android SDK 36、Build Tools 36.1.0，并配置 SDK 路径。

## Debug

```sh
./gradlew :app:assembleDebug
```

Output / 产物：`app/build/outputs/apk/debug/app-debug.apk`。使用本地 Android debug 签名。

## Release

```sh
./gradlew :app:assembleRelease
```

Without signing credentials this produces `app/build/outputs/apk/release/app-release-unsigned.apk`.

默认生成未签名 APK。正式签名时，通过本地环境注入以下四个变量，再运行同一命令：

| Environment variable | Value / 含义 |
| --- | --- |
| `RELEASE_KEYSTORE` | Keystore path / 密钥库路径 |
| `RELEASE_KEY_ALIAS` | Key alias / 密钥别名 |
| `RELEASE_STORE_PASSWORD` | Keystore password / 密钥库密码 |
| `RELEASE_KEY_PASSWORD` | Key password / 私钥密码 |

Provide all four variables together. Partial configuration fails immediately. Signed output: `app/build/outputs/apk/release/app-release.apk`.

四项需完整提供，缺项会直接报错。签名产物为 `app-release.apk`。将密码保存在密码管理器或本地私密环境中。

## Checks / 检查

```sh
./gradlew :app:lintDebug :app:assembleDebug :app:assembleRelease
```

The Xposed API is a compile-only dependency supplied by the framework at runtime. Module metadata is packaged from `app/src/main/resources/META-INF/xposed/`.

Xposed API 仅用于编译，运行时由框架提供。构建后应确认 APK 包含模块入口和作用域元数据，并在设备上验证候选、域名匹配与填入。
