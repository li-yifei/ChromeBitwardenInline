# Chrome Autofill Bridge

[![Release](https://img.shields.io/github/v/release/li-yifei/ChromeAutofillBridge)](https://github.com/li-yifei/ChromeAutofillBridge/releases/latest)
![Android](https://img.shields.io/badge/Android-11%2B-3DDC84?logo=android&logoColor=white)
![Xposed](https://img.shields.io/badge/Xposed_API-101%2B-blue)
[![License](https://img.shields.io/badge/License-GPL--3.0--or--later-blue)](LICENSE)

**English** | [简体中文](README.zh-CN.md)

Chrome makes you choose between Google Password Manager and a third-party password manager. That restriction sucks. This module fixes it.

Keep Google's built-in autofill and use your preferred password manager's keyboard suggestions alongside it.

## Setup

1. Install the APK from [Releases](https://github.com/li-yifei/ChromeAutofillBridge/releases/latest).
2. Enable the module in LSPosed and select **Chrome** as its scope.
3. Select your third-party password manager as Android's preferred autofill service. Enable keyboard suggestions in that manager.
4. In Chrome, select **Settings → Autofill services → Autofill with Google**, then restart Chrome.

## Compatibility

| Requirement | Details |
| --- | --- |
| Android | 11+ required; tested on Android 17 |
| Framework | LSPosed or another framework supporting modern Xposed API 101+ |
| Browser | Google Chrome (`com.android.chrome`); tested on 152.0.7977.75 |
| Password managers | Bitwarden tested; 1Password confirmed working in a work profile |
| Keyboard | Gboard tested; inline suggestions require a compatible keyboard |

Other Android Autofill providers may work; compatibility depends on their implementation. Android's selected autofill provider supplies the third-party suggestions.

Currently supports top-level HTTPS forms. Nested iframes are skipped. Passkey routing is unchanged; complete passkey, card and TOTP flows need further testing. See [test results](TEST-RESULTS.md) for the scope of verification.

## Build

JDK 17, Android SDK 36 and Build Tools 36.1.0 are required. Set `ANDROID_HOME` or configure `sdk.dir` in `local.properties`.

```sh
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

See [building and release signing](docs/BUILDING.md) for release builds, or [how it works](docs/IMPLEMENTATION.md) for the bridge internals.

## License

[GPL-3.0-or-later](LICENSE).
